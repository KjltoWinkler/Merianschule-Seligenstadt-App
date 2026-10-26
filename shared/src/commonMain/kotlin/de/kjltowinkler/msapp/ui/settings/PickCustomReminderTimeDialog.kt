package de.kjltowinkler.msapp.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Dialog für benutzerdefinierte Benachrichtigungszeit gemäß `picktimedefined.png`.
 * 
 * Beinhaltet:
 * 1. Titel "Benutzerdefinierte Benachrichtigung"
 * 2. Zahleneingabefeld
 * 3. Zeiteinheiten:
 *    - Minuten vorher
 *    - Stunden
 *    - Tage
 *    - Wochen
 * 4. Aktionen "Abbrechen" & "OK"
 */
@Composable
fun PickCustomReminderTimeDialog(
    initialAmount: String = "10",
    initialUnitIndex: Int = 0,
    onConfirm: (formattedResult: String) -> Unit,
    onDismissRequest: () -> Unit,
    useMaterialYou: Boolean = true
) {
    var amountText by remember { mutableStateOf(initialAmount) }
    var selectedUnitIndex by remember { mutableIntStateOf(initialUnitIndex) }

    val containerColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        Color(0xFF151B15) // Exakter Figma Hex-Wert aus picktimedefined.png
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

    val actionButtonColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFFC7EDC6)
    }

    val inputBorderColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.outlineVariant
    } else {
        Color(0xFF4A554A)
    }

    val units = listOf(
        "Minuten vorher",
        "Stunden",
        "Tage",
        "Wochen"
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
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                // Titel
                Text(
                    text = "Benutzerdefinierte Benachrichtigung",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Eingabefeld für Zahlenwert
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .border(
                            width = 1.dp,
                            color = inputBorderColor,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = amountText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 4) {
                                amountText = input
                            }
                        },
                        textStyle = TextStyle(
                            color = textPrimaryColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(radioSelectedColor),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Einheiten-RadioButtons
                units.forEachIndexed { index, unitLabel ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedUnitIndex = index }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedUnitIndex == index,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = radioSelectedColor,
                                unselectedColor = radioUnselectedColor
                            )
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = unitLabel,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimaryColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Aktions-Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismissRequest
                    ) {
                        Text(
                            text = "Abbrechen",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = actionButtonColor
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = {
                            val amount = amountText.toIntOrNull() ?: 10
                            val formatted = when (selectedUnitIndex) {
                                0 -> "$amount Minuten vorher"
                                1 -> if (amount == 1) "1 Stunde vorher" else "$amount Stunden vorher"
                                2 -> if (amount == 1) "1 Tag vorher" else "$amount Tage vorher"
                                else -> if (amount == 1) "1 Woche vorher" else "$amount Wochen vorher"
                            }
                            onConfirm(formatted)
                        }
                    ) {
                        Text(
                            text = "OK",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = actionButtonColor
                            )
                        )
                    }
                }
            }
        }
    }
}
