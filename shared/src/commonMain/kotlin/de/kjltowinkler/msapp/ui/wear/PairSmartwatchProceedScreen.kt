package de.kjltowinkler.msapp.ui.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import merianschuleseligenstadtapp.shared.generated.resources.Res
import merianschuleseligenstadtapp.shared.generated.resources.ic_notes_plus_logo
import org.jetbrains.compose.resources.painterResource

/**
 * Mobile Screen #1 für das Smartwatch-Pairing (Figma: pairwithsmartwatch#proceed.png).
 * Fordert den Nutzer auf, seine Smartwatch mit der Notes+ App zu verbinden.
 */
@Composable
fun PairSmartwatchProceedScreen(
    onProceedClicked: () -> Unit,
    onBackClicked: () -> Unit = {}
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                // Oben zentriertes Notes+ Smartwatch Icon
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_notes_plus_logo),
                        contentDescription = "Notes+ Smartwatch",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Großer Titel
                Text(
                    text = "Verbinde deine\nSmartwatch mit der\nNotes+ App",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 34.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Untertitel
                Text(
                    text = "Verwalte die Notes+ App von deiner Smartwatch aus.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Unterstrichener Info-Link
                Text(
                    text = "Weitere Informationen zur Nutzung der Smartwatch-App",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        textDecoration = TextDecoration.Underline
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showInfoDialog = true }
                        .padding(vertical = 4.dp)
                )
            }

            // Voller Pill-Button unten ("Weiter")
            Button(
                onClick = onProceedClicked,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Weiter",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text(
                    text = "Smartwatch-App einrichten",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "Öffne die Notes+ App auf deiner Wear OS Smartwatch und tippe auf „Verbinden“. " +
                        "Anschließend werden deine Aufgaben, dein Stundenplan und deine Account-Sitzung sicher synchronisiert."
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Verstanden")
                }
            }
        )
    }
}
