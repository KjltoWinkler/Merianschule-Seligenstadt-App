package de.kjltowinkler.msapp.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.theme.AppTheme
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Login-Screen: "Willkommen zurück!"
 * Gemäß Figma-Design (login#fertig.png).
 * Wird angezeigt, wenn der Nutzer sich erfolgreich eingeloggt hat und Stundenpläne/Vertretungen synchronisiert werden.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoginReadyScreen(
    accountName: String = "Sebastian Wagner",
    gradeAndSchoolType: String = "Klasse 9aR • Realschule",
    schoolName: String = "Merianschule-Seligenstadt",
    onNextClicked: () -> Unit = {}
) {
    val cardBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    val cardContentColor = MaterialTheme.colorScheme.onSurface

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
            // Oberer Inhaltsbereich (ohne Scrollen, füllt den Screen flexibel aus)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // 1. Headline "Willkommen zurück!"
                Text(
                    text = "Willkommen zurück!",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 36.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Untertitel
                Text(
                    text = "Deine Stundenpläne und Vertretungen werden synchronisiert.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 3. User- & Schuldaten Karte
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Zeile 1: Name
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_person),
                                contentDescription = null,
                                tint = cardContentColor.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = accountName,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = cardContentColor
                            )
                        }

                        // Zeile 2: Klasse & Schulform
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_info),
                                contentDescription = null,
                                tint = cardContentColor.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = gradeAndSchoolType,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = cardContentColor
                            )
                        }

                        // Zeile 3: Schule
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_school),
                                contentDescription = null,
                                tint = cardContentColor.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = schoolName,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = cardContentColor
                            )
                        }
                    }
                }

                // 4. Zentrierte Synchronisations-Grafik (füllt den freien Raum flexibel aus)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.welcome_back_sync),
                        contentDescription = "Synchronisation Illustration - Willkommen zurück",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }
            }

            // 5. "Weiter" Button am unteren Rand (69 dp hoch)
            Button(
                onClick = onNextClicked,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(69.dp)
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
                            .padding(end = 28.dp)
                            .size(31.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun LoginReadyScreenPreview() {
    AppTheme(darkTheme = true) {
        LoginReadyScreen(
            accountName = "Sebastian Wagner",
            gradeAndSchoolType = "Klasse 9aR • Realschule",
            schoolName = "Merianschule-Seligenstadt"
        )
    }
}
