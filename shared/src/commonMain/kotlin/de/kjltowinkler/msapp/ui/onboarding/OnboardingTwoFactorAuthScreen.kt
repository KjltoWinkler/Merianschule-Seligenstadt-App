package de.kjltowinkler.msapp.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.theme.AppTheme
import de.kjltowinkler.msapp.ui.components.MerianAccountBadge
import de.kjltowinkler.msapp.ui.desktop.DesktopTwoFactorAuthScreen
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Onboarding-Screen: Zwei-Faktor-Authentifizierung (2FA) einrichten?
 * Gemäß Figma-Design (onboarding#2FA.png).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingTwoFactorAuthScreen(
    accountName: String = "Sebastian Wagner",
    avatarPainter: Painter? = null,
    onBackClicked: () -> Unit = {},
    onChangeAccountClicked: () -> Unit = onBackClicked,
    onSkipClicked: () -> Unit = {},
    onNextClicked: () -> Unit = {}
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        if (maxWidth >= 840.dp) {
            DesktopTwoFactorAuthScreen(
                accountName = accountName,
                avatarPainter = avatarPainter,
                onNextClicked = onNextClicked,
                onSkipClicked = onSkipClicked,
                onChangeAccountClicked = onChangeAccountClicked
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Oberer Inhaltsbereich (ohne Scrollen)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // 1. Zurück-Button oben links (44x44 dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClicked,
                        modifier = Modifier
                            .size(44.dp)
                            .semantics { contentDescription = "Zurück" }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = "Zurück",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Headline "Zwei-Faktor-Authentifizierung einrichten?"
                Text(
                    text = "Zwei-Faktor-\nAuthentifizierung\neinrichten?",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 34.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Account-Chip ("Sebastian Wagner" mit Avatar)
                MerianAccountBadge(
                    accountName = accountName,
                    avatarPainter = avatarPainter,
                    onClick = onChangeAccountClicked
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Beschreibungstext
                Text(
                    text = "Mit der Zwei-Faktor-Authentifizierung (2FA) machst du dein Konto doppelt so sicher. Bei jeder neuen Anmeldung bestätigst du kurz deine Identität.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )

                // 5. Zentrierte Grafik - füllt flexibel den gesamten verbleibenden Platz aus
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.two_factor_auth),
                        contentDescription = "Zwei-Faktor-Authentifizierung Illustration",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }
            }

            // 6. Buttons am unteren Rand: "Nein danke" (Outlined) & "Weiter" (Primary Filled)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(69.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Outlined Button "Nein danke"
                OutlinedButton(
                    onClick = onSkipClicked,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight()
                        .semantics { contentDescription = "Nein danke" }
                ) {
                    Text(
                        text = "Nein danke",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Filled Primary Button "Weiter" mit Pfeil
                Button(
                    onClick = onNextClicked,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight()
                        .semantics { contentDescription = "Weiter" }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Weiter",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_forward),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 20.dp)
                                .size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
}
}

@Preview
@Composable
fun OnboardingTwoFactorAuthScreenPreview() {
    AppTheme(darkTheme = true) {
        OnboardingTwoFactorAuthScreen(
            accountName = "Sebastian Wagner"
        )
    }
}
