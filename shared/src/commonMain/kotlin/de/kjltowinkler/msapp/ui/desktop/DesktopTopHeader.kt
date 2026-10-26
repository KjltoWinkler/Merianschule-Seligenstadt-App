package de.kjltowinkler.msapp.ui.desktop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.getCurrentFormattedDate
import de.kjltowinkler.msapp.theme.*
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Universeller Top-Header für die Desktop-Version der Merianschule App.
 *
 * Beinhaltet:
 * 1. Dynamischer Titel des aktiven Tabs & aktuelles Datum
 * 2. Elegante Desktop-Suchleiste mit Tastaturkürzel-Hinweis (Strg + K)
 * 3. Aktualisieren-Button mit Rotations-Feedback
 * 4. Kontextuelle Schnell-Aktionen (z. B. "+ Neue Aufgabe" im Aufgaben-Tab)
 */
@Composable
fun DesktopTopHeader(
    selectedTab: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRefreshClicked: () -> Unit,
    modifier: Modifier = Modifier,
    searchFocusRequester: FocusRequester = remember { FocusRequester() },
    isRefreshing: Boolean = false,
    onPrimaryActionClicked: () -> Unit = {}
) {
    val tabTitles = listOf("Startseite", "Stundenplan", "Vertretungsplan", "Aufgaben")
    val currentTitle = tabTitles.getOrElse(selectedTab) { "Merianschule" }

    val coroutineScope = rememberCoroutineScope()
    val rotationAnim = remember { Animatable(0f) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            rotationAnim.animateTo(
                targetValue = 360f,
                animationSpec = repeatable(
                    iterations = 3,
                    animation = tween(durationMillis = 800, easing = LinearEasing)
                )
            )
            rotationAnim.snapTo(0f)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(DesktopBackground) // #111114
            .padding(horizontal = 28.dp)
    ) {
        // --- 1. TITEL & DATUM ---
        Column(
            modifier = Modifier.widthIn(min = 180.dp)
        ) {
            Text(
                text = currentTitle,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesktopTextPrimary
                )
            )
            Text(
                text = getCurrentFormattedDate(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    color = DesktopTextSecondary.copy(alpha = 0.7f)
                )
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- 2. DESKTOP-SUCHLEISTE (MIT SHORTCUT PILL) ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .width(360.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(Color(0xFF191A20))
                .border(1.dp, Color(0xFF2C2F3A), RoundedCornerShape(21.dp))
                .padding(horizontal = 14.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_search),
                contentDescription = "Suchen",
                tint = DesktopInputPlaceholder,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Suchen in Plänen & Aufgaben...",
                        style = TextStyle(
                            fontSize = 13.5.sp,
                            color = DesktopInputPlaceholder
                        )
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 13.5.sp,
                        color = DesktopTextPrimary
                    ),
                    cursorBrush = SolidColor(DesktopAccentBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFocusRequester)
                )
            }

            if (searchQuery.isNotEmpty()) {
                Icon(
                    painter = painterResource(Res.drawable.ic_close),
                    contentDescription = "Suche löschen",
                    tint = DesktopTextSecondary,
                    modifier = Modifier
                        .size(16.dp)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { onSearchQueryChange("") }
                )
            } else {
                // Shortcut-Pill "Strg K"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF242732))
                        .border(1.dp, Color(0xFF383C4C), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Strg K",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DesktopTextSecondary.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // --- 3. REFRESH BUTTON MIT HOVER & DREH-EFFEKT ---
        val refreshInteraction = remember { MutableInteractionSource() }
        val isRefreshHovered by refreshInteraction.collectIsHoveredAsState()

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isRefreshHovered) Color.White.copy(alpha = 0.08f) else Color(0xFF191A20))
                .border(1.dp, Color(0xFF2C2F3A), CircleShape)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = refreshInteraction,
                    indication = null,
                    onClick = {
                        coroutineScope.launch {
                            rotationAnim.snapTo(0f)
                            rotationAnim.animateTo(
                                targetValue = 360f,
                                animationSpec = tween(durationMillis = 500, easing = LinearEasing)
                            )
                        }
                        onRefreshClicked()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_history),
                contentDescription = "Aktualisieren",
                tint = DesktopTextSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotationAnim.value)
            )
        }

        // --- 4. KONTEXTUELLE AKTION (Z.B. "+ NEUE AUFGABE") ---
        if (selectedTab == 3) {
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onPrimaryActionClicked,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DesktopAccentBlue,
                    contentColor = DesktopOnAccentBlue
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(42.dp)
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_add),
                    contentDescription = null,
                    tint = DesktopOnAccentBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Neue Aufgabe",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = DesktopOnAccentBlue
                )
            }
        }
    }
}
