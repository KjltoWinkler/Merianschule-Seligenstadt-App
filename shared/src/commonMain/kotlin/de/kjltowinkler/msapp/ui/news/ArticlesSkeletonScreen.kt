package de.kjltowinkler.msapp.ui.news

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import de.kjltowinkler.msapp.theme.FigmaBackgroundDark
import de.kjltowinkler.msapp.theme.FigmaOnBackgroundDark
import de.kjltowinkler.msapp.ui.components.handCursor
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Skeleton-Screen (Platzhalter-Ladeansicht) für die Seite mit allen Artikeln
 * gemäß Figma-Design: `FigmaDesigns/mobile/Artikel skeleton screen.png`
 *
 * Unterstützt dynamische Material You Farben (`useMaterialYou = true`) sowie
 * die exakten Figma-Farben als Fallback (`useMaterialYou = false`).
 *
 * Beinhaltet:
 * - TopAppBar mit abgerundetem Zurück-Button und Titel-Platzhalter
 * - Oberes rechteckiges Banner/Karten-Element (Höhe 102dp, Radius 16dp)
 * - Großes Featured-Karten-Element (Höhe 320dp, Radius 20dp)
 * - Trenn-/Kategorieleiste mit horizontalem Pill-Balken (14dp Höhe) und Punkt (14dp)
 * - Zweispaltiges Raster mit 2 vertikalen Karten (Höhe 306dp, Radius 20dp)
 * - Flüssige, dezente Shimmer-Animation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticlesSkeletonScreen(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    useMaterialYou: Boolean = true
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val shimmerBrush = rememberArticlesShimmerBrush(useMaterialYou = useMaterialYou)

    val backgroundColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.background
    } else {
        if (isDark) FigmaBackgroundDark else MaterialTheme.colorScheme.background
    }

    val backButtonColor = if (useMaterialYou) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        Color.Black.copy(alpha = 0.35f)
    }

    val backIconTint = if (useMaterialYou) {
        MaterialTheme.colorScheme.onSurface
    } else {
        if (isDark) FigmaOnBackgroundDark else MaterialTheme.colorScheme.onSurface
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .width(131.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerBrush)
                    )
                },
                navigationIcon = {
                    Surface(
                        shape = CircleShape,
                        color = backButtonColor,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                    ) {
                        IconButton(
                            onClick = onBackClicked,
                            modifier = Modifier
                                .fillMaxSize()
                                .handCursor()
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_arrow_back),
                                contentDescription = "Zurück",
                                tint = backIconTint
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    scrolledContainerColor = backgroundColor
                )
            )
        },
        containerColor = backgroundColor,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 19.dp, vertical = 6.dp)
        ) {
            // 1. Oberes Karten-Element (entsprechend Box 1 aus Figma: 374 x 102 dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(102.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(shimmerBrush)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Großes Hero-/Haupt-Element (entsprechend Box 2 aus Figma: 374 x 320 dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(shimmerBrush)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Trennbalken mit Pill und rundem Punkt (entsprechend Figma: Höhe 14 dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(CircleShape)
                        .background(shimmerBrush)
                )

                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(shimmerBrush)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Zweispaltige Artikelkarten unten (entsprechend Figma: je 181-182 x 306 dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(306.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(shimmerBrush)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(306.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(shimmerBrush)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Erzeugt einen weichen, harmonischen Shimmer-Farbverlauf abgestimmt auf Material You
 * (wenn `useMaterialYou = true`) oder auf die exakten Figma-Farben (`useMaterialYou = false`).
 */
@Composable
fun rememberArticlesShimmerBrush(
    useMaterialYou: Boolean = true
): Brush {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val baseColor = when {
        useMaterialYou -> MaterialTheme.colorScheme.surfaceContainerHighest
        isDark -> Color(0xFF2C332E) // Exakte Figma-Farbe #2c332e
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }

    val highlightColor = when {
        useMaterialYou -> lerp(
            baseColor,
            MaterialTheme.colorScheme.primary,
            if (isDark) 0.12f else 0.10f
        )
        isDark -> Color(0xFF3B463E)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
    }

    val transition = rememberInfiniteTransition(label = "articles_skeleton_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1800f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "articles_skeleton_translate"
    )

    return Brush.linearGradient(
        colors = listOf(
            baseColor,
            highlightColor,
            baseColor
        ),
        start = Offset(translateAnim, translateAnim),
        end = Offset(translateAnim + 400f, translateAnim + 400f)
    )
}
