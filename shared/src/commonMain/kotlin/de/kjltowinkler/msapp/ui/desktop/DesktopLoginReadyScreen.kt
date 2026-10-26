package de.kjltowinkler.msapp.ui.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.theme.*
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun DesktopLoginReadyScreen(
    accountName: String = "Sebastian Wagner",
    gradeAndSchoolType: String = "Klasse 9aR • Realschule",
    schoolName: String = "Merianschule-Seligenstadt",
    onNextClicked: () -> Unit = {}
) {
    DesktopScreenScaffold(
        artworkVariant = DesktopArtworkVariant.LOGIN_READY,
        topSpacing = 85.dp
    ) {
        // 1. Titel
        Text(
            text = "Willkommen zurück!",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 36.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Bold
            ),
            color = DesktopTextPrimary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Beschreibung
        Text(
            text = "Du bist erfolgreich angemeldet.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp
            ),
            color = DesktopTextSecondary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Benutzerdaten-Karte (Dunkelblau #004B76)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DesktopAccountChipBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_person),
                    contentDescription = null,
                    tint = DesktopAccountChipContent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = DesktopAccountChipContent
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_info),
                    contentDescription = null,
                    tint = DesktopAccountChipContent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = gradeAndSchoolType,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = DesktopAccountChipContent
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_school),
                    contentDescription = null,
                    tint = DesktopAccountChipContent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = schoolName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = DesktopAccountChipContent
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 4. Illustration
        Image(
            painter = painterResource(Res.drawable.welcome_back_sync),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(230.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.weight(1f))

        // 5. Los geht's Button
        Button(
            onClick = onNextClicked,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = DesktopAccentBlue,
                contentColor = DesktopOnAccentBlue
            ),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .semantics { contentDescription = "Los geht's!" }
        ) {
            Text(
                text = "Los geht's!",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = DesktopOnAccentBlue
            )
        }
    }
}

@Preview
@Composable
private fun DesktopLoginReadyScreenPreview() {
    DesktopTheme {
        Box(modifier = Modifier.size(1440.dp, 900.dp)) {
            DesktopLoginReadyScreen()
        }
    }
}
