package de.kjltowinkler.msapp.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import de.kjltowinkler.msapp.data.task.TaskList
import de.kjltowinkler.msapp.data.task.TasksRepository
import de.kjltowinkler.msapp.theme.getQuicksandFontFamily
import de.kjltowinkler.msapp.ui.components.handCursor
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.Res
import merianschuleseligenstadtapp.shared.generated.resources.ic_arrow_circle_right
import merianschuleseligenstadtapp.shared.generated.resources.ic_drag_indicator
import org.jetbrains.compose.resources.painterResource

/**
 * BottomSheet zum Neu-Anordnen der Aufgaben-Listen gemäß
 * Figma-Design `FigmaDesigns/mobile/tasks#listen-neu-anordnen.png`.
 *
 * Kann geöffnet werden, indem man einen der Tabs gedrückt hält.
 * Unterstützt Drag-and-Drop Neu-Anordnung mit haptischem Feedback
 * und schnelles Öffnen einer Liste über das Pfeil-Icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderListsBottomSheet(
    onDismissRequest: () -> Unit,
    onSelectList: (TaskList) -> Unit,
    useMaterialYou: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val sheetContainerColor = if (useMaterialYou) {
        colorScheme.surfaceContainerLow
    } else {
        Color(0xFF141914)
    }

    val brickBackgroundColor = if (useMaterialYou) {
        colorScheme.surfaceContainer
    } else {
        Color(0xFF1E241E)
    }

    val dragHandleColor = if (useMaterialYou) {
        colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    } else {
        Color(0xFF5E655C)
    }

    val textColor = if (useMaterialYou) {
        colorScheme.onSurface
    } else {
        Color(0xFFE4EDE0)
    }

    var lists by remember { mutableStateOf(TasksRepository.privateLists.toList()) }

    var isClosing by remember { mutableStateOf(false) }
    var draggedListId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val listHeights = remember { mutableStateMapOf<String, Float>() }

    LaunchedEffect(TasksRepository.privateLists.toList()) {
        if (draggedListId == null) {
            lists = TasksRepository.privateLists.toList()
        }
    }

    fun closeWithAction(action: () -> Unit) {
        if (isClosing) return
        isClosing = true
        coroutineScope.launch {
            try {
                sheetState.hide()
            } finally {
                onDismissRequest()
                action()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isClosing) {
                onDismissRequest()
            }
        },
        sheetState = sheetState,
        containerColor = sheetContainerColor,
        contentColor = textColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(dragHandleColor)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Listen neu anordnen",
                fontFamily = getQuicksandFontFamily(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                color = textColor
            )

            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                lists.forEachIndexed { index, list ->
                    key(list.id) {
                        val isBeingDragged = draggedListId == list.id

                        val shape = when {
                            isBeingDragged -> RoundedCornerShape(16.dp)
                            lists.size == 1 -> RoundedCornerShape(16.dp)
                            index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                            index == lists.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                            else -> RoundedCornerShape(4.dp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isBeingDragged) 10f else 1f)
                                .graphicsLayer {
                                    if (isBeingDragged) {
                                        translationY = dragOffsetY
                                        scaleX = 1.02f
                                        scaleY = 1.02f
                                    }
                                }
                                .shadow(
                                    elevation = if (isBeingDragged) 8.dp else 0.dp,
                                    shape = shape
                                )
                                .clip(shape)
                                .background(
                                    if (isBeingDragged) {
                                        brickBackgroundColor.copy(alpha = 0.95f)
                                    } else {
                                        brickBackgroundColor
                                    }
                                )
                                .onGloballyPositioned { coordinates ->
                                    listHeights[list.id] = coordinates.size.height.toFloat()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Drag Handle Icon (6 Punkte)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .handCursor()
                                    .pointerInput(list.id) {
                                        detectDragGestures(
                                            onDragStart = {
                                                draggedListId = list.id
                                                dragOffsetY = 0f
                                                try {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                } catch (_: Exception) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val currentDragId = draggedListId ?: return@detectDragGestures
                                                if (currentDragId != list.id) return@detectDragGestures
                                                dragOffsetY += dragAmount.y

                                                var currentLists = lists
                                                var currentIndex = currentLists.indexOfFirst { it.id == currentDragId }
                                                if (currentIndex == -1) return@detectDragGestures

                                                val defaultHeightPx = with(density) { 56.dp.toPx() }
                                                val spacingPx = with(density) { 2.dp.toPx() }

                                                var reordered = false
                                                while (true) {
                                                    // Nach unten ziehen
                                                    if (currentIndex < currentLists.lastIndex) {
                                                        val targetList = currentLists[currentIndex + 1]
                                                        val targetHeight = listHeights[targetList.id] ?: defaultHeightPx
                                                        val delta = targetHeight + spacingPx
                                                        if (dragOffsetY > delta * 0.6f) {
                                                            val mutableLists = currentLists.toMutableList()
                                                            val temp = mutableLists[currentIndex]
                                                            mutableLists[currentIndex] = mutableLists[currentIndex + 1]
                                                            mutableLists[currentIndex + 1] = temp
                                                            currentLists = mutableLists
                                                            currentIndex++
                                                            dragOffsetY -= delta
                                                            reordered = true
                                                            continue
                                                        }
                                                    }
                                                    // Nach oben ziehen
                                                    if (currentIndex > 0) {
                                                        val targetList = currentLists[currentIndex - 1]
                                                        val targetHeight = listHeights[targetList.id] ?: defaultHeightPx
                                                        val delta = targetHeight + spacingPx
                                                        if (dragOffsetY < -delta * 0.6f) {
                                                            val mutableLists = currentLists.toMutableList()
                                                            val temp = mutableLists[currentIndex]
                                                            mutableLists[currentIndex] = mutableLists[currentIndex - 1]
                                                            mutableLists[currentIndex - 1] = temp
                                                            currentLists = mutableLists
                                                            currentIndex--
                                                            dragOffsetY += delta
                                                            reordered = true
                                                            continue
                                                        }
                                                    }
                                                    break
                                                }

                                                if (reordered) {
                                                    lists = currentLists
                                                    TasksRepository.reorderPrivateLists(currentLists)
                                                    try {
                                                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                                    } catch (_: Exception) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                draggedListId = null
                                                dragOffsetY = 0f
                                                TasksRepository.reorderPrivateLists(lists)
                                            },
                                            onDragCancel = {
                                                draggedListId = null
                                                dragOffsetY = 0f
                                                TasksRepository.reorderPrivateLists(lists)
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_drag_indicator),
                                contentDescription = "Liste verschieben",
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // 2. Listen-Titel
                        Text(
                            text = list.title,
                            fontFamily = getQuicksandFontFamily(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    closeWithAction { onSelectList(list) }
                                }
                                .handCursor()
                        )

                        // 3. Arrow Circle Right Icon
                        IconButton(
                            onClick = {
                                closeWithAction { onSelectList(list) }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .handCursor()
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_arrow_circle_right),
                                contentDescription = "${list.title} öffnen",
                                tint = textColor.copy(alpha = 0.65f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
}
