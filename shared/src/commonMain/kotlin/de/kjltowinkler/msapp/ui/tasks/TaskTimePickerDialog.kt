package de.kjltowinkler.msapp.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Offizieller Material 3 TimePickerDialog mit Dial- & Input-Umschaltung
 * basierend auf dem AOSP AdvancedTimePicker Sample:
 * Verwendet native M3 TimePicker, TimeInput, TimePickerState und rememberTimePickerState
 * inklusive spürbarem haptischen Feedback (Tick) beim Drehen an der Uhr.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTimePickerDialog(
    initialHour: Int = 7,
    initialMinute: Int = 0,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    val haptic = LocalHapticFeedback.current

    // Haptisches Feedback auslösen, sobald sich Stunde oder Minute auf der Uhr ändert
    LaunchedEffect(timePickerState) {
        var isFirst = true
        snapshotFlow { timePickerState.hour to timePickerState.minute }
            .collect {
                if (isFirst) {
                    isFirst = false
                } else {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    } catch (_: Exception) {
                        try {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } catch (_: Exception) {
                            // Fallback
                        }
                    }
                }
            }
    }

    var showDial by remember { mutableStateOf(true) }

    val toggleIcon = if (showDial) {
        Res.drawable.ic_keyboard
    } else {
        Res.drawable.ic_access_time
    }

    val colorScheme = MaterialTheme.colorScheme

    val pickerColors = TimePickerDefaults.colors(
        clockDialColor = colorScheme.surfaceContainerHighest,
        clockDialSelectedContentColor = colorScheme.onPrimary,
        clockDialUnselectedContentColor = colorScheme.onSurface,
        selectorColor = colorScheme.primary,
        containerColor = colorScheme.surfaceContainerHigh,
        periodSelectorBorderColor = colorScheme.outline,
        periodSelectorSelectedContainerColor = colorScheme.primaryContainer,
        periodSelectorUnselectedContainerColor = colorScheme.surfaceContainerHighest,
        periodSelectorSelectedContentColor = colorScheme.onPrimaryContainer,
        periodSelectorUnselectedContentColor = colorScheme.onSurface,
        timeSelectorSelectedContainerColor = colorScheme.primaryContainer,
        timeSelectorUnselectedContainerColor = colorScheme.surfaceContainerHighest,
        timeSelectorSelectedContentColor = colorScheme.onPrimaryContainer,
        timeSelectorUnselectedContentColor = colorScheme.onSurface
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = colorScheme.surfaceContainerHigh
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = "Uhrzeit auswählen",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )

                if (showDial) {
                    TimePicker(
                        state = timePickerState,
                        colors = pickerColors
                    )
                } else {
                    TimeInput(
                        state = timePickerState,
                        colors = pickerColors
                    )
                }

                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showDial = !showDial }) {
                        Icon(
                            painter = painterResource(toggleIcon),
                            contentDescription = "Eingabeart wechseln",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(onClick = onDismissRequest) {
                        Text(
                            text = "Abbrechen",
                            color = colorScheme.primary
                        )
                    }

                    TextButton(
                        onClick = {
                            onTimeSelected(timePickerState.hour, timePickerState.minute)
                            onDismissRequest()
                        }
                    ) {
                        Text(
                            text = "OK",
                            color = colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
