package de.kjltowinkler.msapp.ui.desktop

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.theme.*
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

data class DesktopNavItem(
    val title: String,
    val filledIcon: DrawableResource,
    val outlinedIcon: DrawableResource,
    val badgeCount: Int = 0
)

/**
 * Permanente Navigations-Sidebar für die Desktop-Version der Merianschule App.
 *
 * Beinhaltet:
 * 1. Schullogo & Markenname
 * 2. Navigationspunkte mit Hover-Animationen, Pill-Indikatoren & Badges
 * 3. Benutzer-Chip am unteren Rand mit Profil-Status
 */
@Composable
fun DesktopNavigationSidebar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    accountName: String = "Sebastian Wagner",
    gradeAndSchool: String = "Klasse 9aR • Realschule",
    avatarType: String = "initials",
    avatarColorHex: Long = 0xFF395A3CL,
    onProfileClicked: () -> Unit = {},
    modifier: Modifier = Modifier,
    substitutionBadgeCount: Int = 0,
    tasksBadgeCount: Int = 0
) {
    val navItems = listOf(
        DesktopNavItem(
            title = "Startseite",
            filledIcon = Res.drawable.ic_nav_home_filled,
            outlinedIcon = Res.drawable.ic_nav_home_outlined
        ),
        DesktopNavItem(
            title = "Stundenplan",
            filledIcon = Res.drawable.ic_nav_schedule,
            outlinedIcon = Res.drawable.ic_nav_schedule
        ),
        DesktopNavItem(
            title = "Vertretungsplan",
            filledIcon = Res.drawable.ic_nav_event_note,
            outlinedIcon = Res.drawable.ic_nav_event_note,
            badgeCount = substitutionBadgeCount
        ),
        DesktopNavItem(
            title = "Aufgaben",
            filledIcon = Res.drawable.ic_nav_checklist_rtl,
            outlinedIcon = Res.drawable.ic_nav_checklist_rtl,
            badgeCount = tasksBadgeCount
        )
    )

    Column(
        modifier = modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(DesktopCardBackground) // #0B0B0D
            .border(
                width = 1.dp,
                color = Color(0xFF1E2127),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // --- 1. SCHUL-BRANDING & LOGO ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DesktopAccountChipBackground)
                    .border(1.dp, DesktopAccentBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.app_icon),
                    contentDescription = "Merianschule Logo",
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Merianschule",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = DesktopTextPrimary
                    )
                )
                Text(
                    text = "Seligenstadt",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = DesktopTextSecondary.copy(alpha = 0.7f),
                        letterSpacing = 0.3.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Kategorie-Titel
        Text(
            text = "HAUPTMENÜ",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = DesktopInputPlaceholder
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // --- 2. NAVIGATIONSEINTRÄGE ---
        navItems.forEachIndexed { index, item ->
            val isSelected = selectedTab == index
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()

            val targetBgColor = when {
                isSelected -> DesktopAccountChipBackground.copy(alpha = 0.85f)
                isHovered -> Color.White.copy(alpha = 0.06f)
                else -> Color.Transparent
            }
            val animatedBgColor by animateColorAsState(
                targetValue = targetBgColor,
                animationSpec = tween(durationMillis = 150)
            )

            val contentColor = when {
                isSelected -> DesktopAccentBlue
                isHovered -> DesktopTextPrimary
                else -> DesktopTextSecondary
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(animatedBgColor)
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 1.dp,
                                color = DesktopAccentBlue.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else Modifier
                    )
                    .hoverable(interactionSource)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onTabSelected(index) }
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Icon(
                    painter = painterResource(if (isSelected) item.filledIcon else item.outlinedIcon),
                    contentDescription = item.title,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = contentColor
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Badge (z. B. Anzahl Vertretungen oder offene Aufgaben)
                if (item.badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) DesktopAccentBlue else Color(0xFF1E2832)
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.badgeCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) DesktopOnAccentBlue else DesktopAccentBlue
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- 3. PROFIL- & BENUTZER-BEREICH (UNTEN) ---
        val profileInteractionSource = remember { MutableInteractionSource() }
        val isProfileHovered by profileInteractionSource.collectIsHoveredAsState()

        val profileBgColor by animateColorAsState(
            targetValue = if (isProfileHovered) Color.White.copy(alpha = 0.06f) else Color(0xFF141519),
            animationSpec = tween(durationMillis = 150)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(profileBgColor)
                .border(1.dp, Color(0xFF22242C), RoundedCornerShape(14.dp))
                .hoverable(profileInteractionSource)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = profileInteractionSource,
                    indication = null,
                    onClick = onProfileClicked
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, DesktopAccentBlue.copy(alpha = 0.5f), CircleShape)
            ) {
                de.kjltowinkler.msapp.ui.account.AccountAvatar(
                    accountName = accountName,
                    avatarType = avatarType,
                    avatarColorHex = avatarColorHex,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DesktopTextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = gradeAndSchool,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = DesktopTextSecondary.copy(alpha = 0.7f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                painter = painterResource(Res.drawable.ic_info),
                contentDescription = "Konto Details",
                tint = DesktopTextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
