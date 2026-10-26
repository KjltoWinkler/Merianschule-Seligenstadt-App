package de.kjltowinkler.msapp.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.kjltowinkler.msapp.theme.DesktopBackground
import de.kjltowinkler.msapp.theme.DesktopCardBackground
import de.kjltowinkler.msapp.theme.DesktopTheme

/**
 * Universelles Layout-Scaffold für alle Desktop-Screens:
 * - Linke Seite: animierte Vektorgrafiken (DesktopDecorativeArtwork)
 * - Rechte Seite: abgerundetes Panel (#0B0B0D) mit einheitlichen Abständen
 */
@Composable
fun DesktopScreenScaffold(
    modifier: Modifier = Modifier,
    artworkVariant: DesktopArtworkVariant = DesktopArtworkVariant.USERNAME,
    topSpacing: Dp = 110.dp,
    panelContent: @Composable ColumnScope.() -> Unit
) {
    DesktopTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DesktopBackground)
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // Linker Bereich: Animierte Dekoration passend zum jeweiligen Screen
                DesktopDecorativeArtwork(
                    variant = artworkVariant,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                // Rechter Bereich: Inhaltspanel
                Box(
                    modifier = Modifier
                        .widthIn(min = 380.dp, max = 430.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp))
                        .background(DesktopCardBackground)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                    ) {
                        Spacer(modifier = Modifier.height(topSpacing))
                        panelContent()
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}
