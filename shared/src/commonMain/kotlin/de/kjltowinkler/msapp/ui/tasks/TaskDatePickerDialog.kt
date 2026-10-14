package de.kjltowinkler.msapp.ui.tasks

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import de.kjltowinkler.msapp.data.task.SimpleDate

/**
 * Offizieller Material 3 DatePickerDialog gemäß AOSP DatePickerSamples:
 * Nutzt den nativen M3 DatePickerDialog & DatePicker mit DatePickerState.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDatePickerDialog(
    selectedDate: SimpleDate?,
    onDateSelected: (SimpleDate?) -> Unit,
    onDismissRequest: () -> Unit
) {
    // Initiales Datum in Millisekunden umrechnen, falls vorhanden
    val initialMillis = selectedDate?.let { date ->
        // Ungefähre/direkte UTC-Epoche für das ausgewählte Datum
        val daysSince1970 = (date.year - 1970L) * 365L + ((date.year - 1968L) / 4L) + (date.monthNumber - 1L) * 30L + date.dayOfMonth.toLong()
        daysSince1970 * 86400000L
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        // UTC Tage zu SimpleDate
                        val days = millis / 86400000L
                        // Konvertierung
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
                }
            ) {
                Text(
                    text = "OK",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDateSelected(null)
                    onDismissRequest()
                }
            ) {
                Text(
                    text = "Löschen",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = DatePickerDefaults.colors(
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
    ) {
        DatePicker(
            state = datePickerState,
            modifier = Modifier.verticalScroll(rememberScrollState()),
            colors = DatePickerDefaults.colors(
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
        )
    }
}
