package de.kjltowinkler.msapp.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.data.task.TaskItem
import de.kjltowinkler.msapp.theme.getQuicksandFontFamily
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.Res
import merianschuleseligenstadtapp.shared.generated.resources.ic_check
import org.jetbrains.compose.resources.painterResource

/**
 * Optionen für die Sortierung der Aufgabenliste.
 */
enum class TaskSortOption(val label: String) {
    MY_ORDER("Meine Reihenfolge"),
    DATE("Datum"),
    DEADLINE("Frist"),
    RECENTLY_STARRED("Vor Kurzem markiert"),
    TITLE("Titel")
}

/**
 * Sortiert eine Liste von Aufgaben anhand der ausgewählten Sortieroption.
 */
fun List<TaskItem>.sortedByOption(option: TaskSortOption): List<TaskItem> {
    return when (option) {
        TaskSortOption.MY_ORDER -> this
        TaskSortOption.DATE -> {
            sortedWith(
                compareBy<TaskItem> { task ->
                    val date = task.dueDate.ifBlank { task.createdDate }
                    if (date.isBlank()) "zzz" else date
                }
            )
        }
        TaskSortOption.DEADLINE -> {
            sortedWith(
                compareBy<TaskItem> { task ->
                    val deadline = task.deadlineDate.ifBlank { task.dueDate }
                    if (deadline.isBlank()) "zzz" else deadline
                }
            )
        }
        TaskSortOption.RECENTLY_STARRED -> {
            sortedWith(compareByDescending { it.isStarred })
        }
        TaskSortOption.TITLE -> {
            sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
        }
    }
}

/**
 * Material You BottomSheet für die Auswahl der Sortierreihenfolge der Aufgabenliste
 * gemäß Figma-Design `FigmaDesigns/mobile/tasks#sort.png`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSortBottomSheet(
    selectedOption: TaskSortOption,
    onOptionSelected: (TaskSortOption) -> Unit,
    onDismissRequest: () -> Unit,
    useMaterialYou: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val colorScheme = MaterialTheme.colorScheme

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

    val checkmarkTint = if (useMaterialYou) {
        colorScheme.onSurface
    } else {
        Color(0xFFE4EDE0)
    }

    val options = TaskSortOption.entries

    val coroutineScope = rememberCoroutineScope()
    var isClosing by remember { mutableStateOf(false) }

    fun closeWithAnimation(action: (() -> Unit)? = null) {
        if (isClosing) return
        isClosing = true
        action?.invoke()
        coroutineScope.launch {
            try {
                sheetState.hide()
            } finally {
                onDismissRequest()
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
                .padding(bottom = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sortieren nach",
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
                options.forEachIndexed { index, option ->
                    val shape = when {
                        options.size == 1 -> RoundedCornerShape(16.dp)
                        index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        index == options.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                        else -> RoundedCornerShape(4.dp)
                    }

                    val isSelected = option == selectedOption

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(brickBackgroundColor)
                            .clickable(enabled = !isClosing) {
                                closeWithAnimation {
                                    onOptionSelected(option)
                                }
                            }
                            .defaultMinSize(minHeight = 54.dp)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_check),
                                    contentDescription = "Ausgewählt",
                                    tint = checkmarkTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Text(
                            text = option.label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}
