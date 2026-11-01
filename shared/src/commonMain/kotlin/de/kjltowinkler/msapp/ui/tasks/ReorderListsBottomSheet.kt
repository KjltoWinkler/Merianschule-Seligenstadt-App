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

    LaunchedEffect(TasksRepository.privateLists.toList()) {
        lists = TasksRepository.privateLists.toList()
    }

    var isClosing by remember { mutableStateOf(false) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var itemHeightPx by remember { mutableStateOf(0f) }

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
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                lists.forEachIndexed { index, list ->
                    val isBeingDragged = draggedIndex == index

                    val shape = when {
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
                                if (itemHeightPx == 0f) {
                                    itemHeightPx = coordinates.size.height.toFloat()
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Drag Handle Icon (6 Punkte)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .handCursor()
                                .pointerInput(list.id, lists.size) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggedIndex = index
                                            dragOffsetY = 0f
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val currentDragIndex = draggedIndex ?: return@detectDragGestures
                                            val rowHeight = if (itemHeightPx > 0f) itemHeightPx else with(density) { 56.dp.toPx() }
                                            dragOffsetY += dragAmount.y

                                            // Nach unten ziehen
                                            if (dragOffsetY > rowHeight * 0.5f && currentDragIndex < lists.lastIndex) {
                                                val mutableLists = lists.toMutableList()
                                                val temp = mutableLists[currentDragIndex]
                                                mutableLists[currentDragIndex] = mutableLists[currentDragIndex + 1]
                                                mutableLists[currentDragIndex + 1] = temp
                                                lists = mutableLists
                                                TasksRepository.reorderPrivateLists(mutableLists)
                                                draggedIndex = currentDragIndex + 1
                                                dragOffsetY -= rowHeight
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                            // Nach oben ziehen
                                            else if (dragOffsetY < -rowHeight * 0.5f && currentDragIndex > 0) {
                                                val mutableLists = lists.toMutableList()
                                                val temp = mutableLists[currentDragIndex]
                                                mutableLists[currentDragIndex] = mutableLists[currentDragIndex - 1]
                                                mutableLists[currentDragIndex - 1] = temp
                                                lists = mutableLists
                                                TasksRepository.reorderPrivateLists(mutableLists)
                                                draggedIndex = currentDragIndex - 1
                                                dragOffsetY += rowHeight
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        },
                                        onDragEnd = {
                                            draggedIndex = null
                                            dragOffsetY = 0f
                                            TasksRepository.reorderPrivateLists(lists)
                                        },
                                        onDragCancel = {
                                            draggedIndex = null
                                            dragOffsetY = 0f
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
                            fontWeight = FontWeight.SemiBold,
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
