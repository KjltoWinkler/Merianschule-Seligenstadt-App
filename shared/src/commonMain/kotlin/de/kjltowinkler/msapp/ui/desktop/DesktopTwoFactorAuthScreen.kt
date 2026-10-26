package de.kjltowinkler.msapp.ui.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.theme.*
import de.kjltowinkler.msapp.ui.components.MerianAccountBadge
import merianschuleseligenstadtapp.shared.generated.resources.Res
import merianschuleseligenstadtapp.shared.generated.resources.two_factor_auth
import org.jetbrains.compose.resources.painterResource

@Composable
fun DesktopTwoFactorAuthScreen(
    accountName: String = "Sebastian Wagner",
    avatarPainter: Painter? = null,
    onNextClicked: () -> Unit = {},
    onSkipClicked: () -> Unit = {},
    onChangeAccountClicked: () -> Unit = {}
) {
    DesktopScreenScaffold(
        artworkVariant = DesktopArtworkVariant.TWO_FACTOR_AUTH,
        topSpacing = 85.dp
    ) {
        // 1. Titel
        Text(
            text = "Zwei-Faktor-\nAuthentifizierung\neinrichten?",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 34.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Bold
            ),
            color = DesktopTextPrimary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Account Chip
        MerianAccountBadge(
            accountName = accountName,
            avatarPainter = avatarPainter,
            onClick = onChangeAccountClicked
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Beschreibung
        Text(
            text = "Mit der Zwei-Faktor-Authentifizierung (2FA) machst du dein Konto doppelt so sicher. Bei jeder neuen Anmeldung bestätigst du kurz deine Identität.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp
            ),
            color = DesktopTextSecondary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        // 4. Illustration
        Image(
            painter = painterResource(Res.drawable.two_factor_auth),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(230.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.weight(1f))

        // 5. Zwei Buttons: "Nein danke" und "Weiter"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSkipClicked,
                shape = CircleShape,
                border = BorderStroke(1.dp, DesktopInputBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = DesktopCardBackground,
                    contentColor = DesktopTextSecondary
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .semantics { contentDescription = "Nein danke" }
            ) {
                Text(
                    text = "Nein danke",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = DesktopTextSecondary
                )
            }

            Button(
                onClick = onNextClicked,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DesktopAccentBlue,
                    contentColor = DesktopOnAccentBlue
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .semantics { contentDescription = "Weiter" }
            ) {
                Text(
                    text = "Weiter",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = DesktopOnAccentBlue
                )
            }
        }
    }
}

@Preview
@Composable
private fun DesktopTwoFactorAuthScreenPreview() {
    DesktopTheme {
        Box(modifier = Modifier.size(1440.dp, 900.dp)) {
            DesktopTwoFactorAuthScreen()
        }
    }
}
