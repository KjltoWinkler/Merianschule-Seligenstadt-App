package de.kjltowinkler.msapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.ui.account.AccountAvatar
import de.kjltowinkler.msapp.ui.account.getAvatarColorForName

/**
 * Wiederverwendbarer Account-/Profil-Badge gemäß Material 3 Designrichtlinien.
 * Zeigt standardmäßig das schlichte Buchstaben-Profilbild passend zum Accountnamen.
 * Passt sich dynamisch an Material You Farbschemata (z. B. Blau) an
 * und verwendet im Figma-Fallback die abgestimmten Grüntöne.
 */
@Composable
fun MerianAccountBadge(
    accountName: String,
    modifier: Modifier = Modifier,
    avatarPainter: Painter? = null,
    avatarType: String = "initials",
    avatarColorHex: Long = getAvatarColorForName(accountName),
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (avatarPainter != null) {
                Image(
                    painter = avatarPainter,
                    contentDescription = "Profilbild von $accountName",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                )
            } else {
                AccountAvatar(
                    accountName = accountName,
                    avatarType = avatarType,
                    avatarColorHex = avatarColorHex,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = accountName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
