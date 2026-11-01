package de.kjltowinkler.msapp.wear.presentation.screens

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.FailureConfirmationDialogContent
import androidx.wear.compose.material3.OpenOnPhoneDialogContent
import androidx.wear.compose.material3.OpenOnPhoneDialogDefaults
import androidx.wear.compose.material3.SuccessConfirmationDialogContent
import androidx.wear.compose.material3.confirmationDialogCurvedText
import androidx.wear.compose.material3.openOnPhoneDialogCurvedText

/**
 * Onboarding Screen #2: Smartphone prüfen
 * Verwendet die offizielle Wear OS M3 Compose Komponente [OpenOnPhoneDialogContent]
 * mit animiertem Smartphone-Icon, rotierendem Fortschrittsring und gekrümmtem Text.
 */
@Composable
fun OpenOnPhoneScreen() {
    val curvedTextStyle = OpenOnPhoneDialogDefaults.curvedTextStyle

    OpenOnPhoneDialogContent(
        curvedText = {
            openOnPhoneDialogCurvedText(
                text = "Smartphone prüfen",
                style = curvedTextStyle
            )
        },
        durationMillis = OpenOnPhoneDialogDefaults.DurationMillis,
        content = {
            OpenOnPhoneDialogDefaults.Icon()
        }
    )
}

/**
 * Onboarding Screen Erfolgreich
 * Verwendet die offizielle Wear OS M3 Compose Komponente [SuccessConfirmationDialogContent]
 * mit animiertem Erfolgs-Pebble, Checkmark-Animation und gekrümmtem Text.
 */
@Composable
fun SuccessConfirmationScreen() {
    val curvedTextStyle = ConfirmationDialogDefaults.curvedTextStyle

    SuccessConfirmationDialogContent(
        curvedText = {
            confirmationDialogCurvedText(
                text = "Erfolgreich",
                style = curvedTextStyle
            )
        },
        content = {
            ConfirmationDialogDefaults.SuccessIcon()
        }
    )
}

/**
 * Onboarding Screen Fehlgeschlagen
 * Verwendet die offizielle Wear OS M3 Compose Komponente [FailureConfirmationDialogContent]
 * mit animiertem Fehler-Squircle, Verbindungsfehler-Icon und gekrümmtem Text.
 */
@Composable
fun FailureConfirmationScreen() {
    val curvedTextStyle = ConfirmationDialogDefaults.curvedTextStyle

    FailureConfirmationDialogContent(
        curvedText = {
            confirmationDialogCurvedText(
                text = "Fehlgeschlagen",
                style = curvedTextStyle
            )
        },
        content = {
            ConfirmationDialogDefaults.ConnectionFailureIcon()
        }
    )
}
