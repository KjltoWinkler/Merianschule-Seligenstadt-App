package de.kjltowinkler.msapp.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import de.kjltowinkler.msapp.data.task.SimpleDate
import de.kjltowinkler.msapp.ui.components.WithTooltip
import de.kjltowinkler.msapp.ui.components.handCursor
import merianschuleseligenstadtapp.shared.generated.resources.Res
import merianschuleseligenstadtapp.shared.generated.resources.ic_event_upcoming
import org.jetbrains.compose.resources.painterResource

/**
 * Material 3 DatePickerDialog mit nativem M3 DatePicker und optionalem
 * Schnell-Button "Bis zur nächsten Stunde" unten links auf Höhe von "Löschen" und "OK".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDatePickerDialog(
    selectedDate: SimpleDate?,
    onDateSelected: (SimpleDate?) -> Unit,
    onDismissRequest: () -> Unit,
    nextLessonAvailable: Boolean = false,
    isNextLessonSelected: Boolean = false,
    onSelectNextLesson: (() -> Unit)? = null
) {
    // Initiales Datum in Millisekunden umrechnen, falls vorhanden
    val initialMillis = selectedDate?.let { date ->
        val daysSince1970 = (date.year - 1970L) * 365L + ((date.year - 1968L) / 4L) + (date.monthNumber - 1L) * 30L + date.dayOfMonth.toLong()
        daysSince1970 * 86400000L
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    val pickerColors = DatePickerDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        headlineContentColor = MaterialTheme.colorScheme.onSurface,
        weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        subheadContentColor = MaterialTheme.colorScheme.onSurface,
        yearContentColor = MaterialTheme.colorScheme.onSurface,
        currentYearContentColor = MaterialTheme.colorScheme.primary,
        selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
        selectedYearContainerColor = MaterialTheme.colorScheme.primary,
        dayContentColor = MaterialTheme.colorScheme.onSurface,
        selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
        todayContentColor = MaterialTheme.colorScheme.primary,
        todayDateBorderColor = MaterialTheme.colorScheme.primary
    )

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .padding(horizontal = 16.dp),
            shape = DatePickerDefaults.shape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = DatePickerDefaults.TonalElevation
        ) {
            Column {
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    colors = pickerColors
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (onSelectNextLesson != null) {
                        WithTooltip("Bis zur nächsten Stunde") {
                            IconButton(
                                onClick = {
                                    onSelectNextLesson()
                                    onDismissRequest()
                                },
                                enabled = nextLessonAvailable,
                                modifier = Modifier.handCursor(nextLessonAvailable)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_event_upcoming),
                                    contentDescription = "Bis zur nächsten Stunde",
                                    tint = when {
                                        isNextLessonSelected -> MaterialTheme.colorScheme.primary
                                        nextLessonAvailable -> MaterialTheme.colorScheme.onSurfaceVariant
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.size(1.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                onDateSelected(null)
                                onDismissRequest()
                            },
                            modifier = Modifier.handCursor()
                        ) {
                            Text(
                                text = "Löschen",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        TextButton(
                            onClick = {
                                val millis = datePickerState.selectedDateMillis
                                if (millis != null) {
                                    val days = millis / 86400000L
                                    var l = days + 68569L + 2440588L
                                    val n = (4L * l) / 146097L
                                    l -= (146097L * n + 3L) / 4L
                                    val i = (4000L * (l + 1L)) / 1461001L
                                    l = l - (1461L * i) / 4L + 31L
                                    val j = (80L * l) / 2447L
                                    val day = (l - (2447L * j) / 80L).toInt()
                                    l = j / 11L
                                    val month = (j + 2L - 12L * l).toInt()
                                    val year = (100L * (n - 49L) + i + l).toInt()

                                    onDateSelected(SimpleDate(year, month, day))
                                } else {
                                    onDateSelected(null)
                                }
                                onDismissRequest()
                            },
                            modifier = Modifier.handCursor()
                        ) {
                            Text(
                                text = "OK",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
