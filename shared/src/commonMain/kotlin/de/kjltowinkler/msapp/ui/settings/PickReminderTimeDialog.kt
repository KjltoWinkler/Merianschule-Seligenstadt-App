package de.kjltowinkler.msapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import de.kjltowinkler.msapp.theme.FigmaBackgroundDark

/**
 * Dialog zur Auswahl der Erinnerungszeit gemäß `picktime.png`.
 * 
 * Enthält Optionen:
 * - 5 Minuten vorher
 * - 10 Minuten vorher
 * - 15 Minuten vorher
 * - 1 Stunde vorher
 * - 1 Tag vorher
 * - Benutzerdefiniert...
 */
@Composable
fun PickReminderTimeDialog(
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onCustomClicked: () -> Unit,
    onDismissRequest: () -> Unit,
    useMaterialYou: Boolean = true
) {
    val containerColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        Color(0xFF151B15) // Exakter Figma Hex-Wert aus picktime.png
    }

    val textPrimaryColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color(0xFFDFE9DE)
    }

    val radioSelectedColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFFC7EDC6) // Exakter Figma Hex-Wert für Radio-Grün
    }

    val radioUnselectedColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color(0xFFA5ADA2)
    }

    val options = listOf(
        "5 Minuten vorher",
        "10 Minuten vorher",
        "15 Minuten vorher",
        "1 Stunde vorher",
        "1 Tag vorher",
        "Benutzerdefiniert..."
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = containerColor,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                options.forEach { option ->
                    val isCustom = option == "Benutzerdefiniert..."
                    val isSelected = if (isCustom) {
                        selectedOption !in options.dropLast(1)
                    } else {
                        selectedOption == option
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isCustom) {
                                    onCustomClicked()
                                } else {
                                    onOptionSelected(option)
                                    onDismissRequest()
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = radioSelectedColor,
                                unselectedColor = radioUnselectedColor
                            )
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimaryColor
                            )
                        )
                    }
                }
            }
        }
    }
}
