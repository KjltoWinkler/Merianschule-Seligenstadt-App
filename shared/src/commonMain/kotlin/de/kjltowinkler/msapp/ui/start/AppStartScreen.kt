package de.kjltowinkler.msapp.ui.start

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import de.kjltowinkler.msapp.theme.getGoogleSansFlexFontFamily

/**
 * App Start Screen (Splash Screen) gemäß Figma-Design:
 * FigmaDesigns/mobile/AppStartScreen.png
 *
 * Zeigt das markante "Notes Plus" App-Logo zentriert auf dunklem Hintergrund (#131314)
 * und den Text "Notes Plus" am unteren Bildschirmrand.
 */
@Composable
fun AppStartScreen(
    durationMillis: Long = 1300L,
    onFinished: () -> Unit = {}
) {
    var animationStarted by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (animationStarted) 1.0f else 0.92f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 700, delayMillis = 150, easing = FastOutSlowInEasing)
    )

    LaunchedEffect(Unit) {
        animationStarted = true
        delay(durationMillis)
        onFinished()
    }

    val googleSansFont = de.kjltowinkler.msapp.theme.getGoogleSansFlexBrandFontFamily()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF131314))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Bei Klick direkt weiternavigieren
                onFinished()
            }
    ) {
        // 1. Zentriertes Logo (Exakt 132dp x 118dp gemäß Figma)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .scale(logoScale)
                .alpha(logoAlpha)
        ) {
            NotesPlusLogo(
                modifier = Modifier.size(width = 132.dp, height = 118.dp)
            )
        }

        // 2. "Notes Plus" am unteren Bildschirmrand ("Notes" fett, "Plus" regulär gemäß Design)
        val brandText = remember {
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Notes")
                }
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                    append("Plus")
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 36.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = brandText,
                modifier = Modifier.alpha(textAlpha),
                style = TextStyle(
                    fontFamily = googleSansFont,
                    fontSize = 20.sp,
                    color = Color(0xFFC4C7C5),
                    letterSpacing = 0.2.sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

/**
 * Vektorielle Zeichnung des "Notes Plus" Logos via Compose Canvas:
 * 3 Zeilen mit abgerundeten Pillen (horizontaler Farbverlauf) und Kreisen.
 */
@Composable
fun NotesPlusLogo(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val totalHeight = size.height

        val scaleX = totalWidth / 132f
        val scaleY = totalHeight / 118f

        val rowHeight = 39.33f * scaleY
        val pillWidth = 92.5f * scaleX
        val pillCornerRadius = CornerRadius(rowHeight / 2f, rowHeight / 2f)
        val circleRadius = rowHeight / 2f
        val circleCenterX = 112.33f * scaleX

        // Zeile 1
        // Pille 1: Horizontaler Farbverlauf #585C97 -> #969DFC
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF585C97), Color(0xFF969DFC)),
                startX = 0f,
                endX = pillWidth
            ),
            topLeft = Offset(0f, 0f),
            size = Size(pillWidth, rowHeight),
            cornerRadius = pillCornerRadius
        )
        // Kreis 1: Solide #5848E2
        drawCircle(
            color = Color(0xFF5848E2),
            radius = circleRadius,
            center = Offset(circleCenterX, rowHeight / 2f)
        )

        // Zeile 2
        val row2Top = rowHeight
        // Pille 2: Horizontaler Farbverlauf #424296 -> #7171FD
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF424296), Color(0xFF7171FD)),
                startX = 0f,
                endX = pillWidth
            ),
            topLeft = Offset(0f, row2Top),
            size = Size(pillWidth, rowHeight),
            cornerRadius = pillCornerRadius
        )
        // Kreis 2: Solide #7271FE
        drawCircle(
            color = Color(0xFF7271FE),
            radius = circleRadius,
            center = Offset(circleCenterX, row2Top + rowHeight / 2f)
        )

        // Zeile 3
        val row3Top = rowHeight * 2f
        // Pille 3: Horizontaler Farbverlauf #2F267A -> #5848E0
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF2F267A), Color(0xFF5848E0)),
                startX = 0f,
                endX = pillWidth
            ),
            topLeft = Offset(0f, row3Top),
            size = Size(pillWidth, rowHeight),
            cornerRadius = pillCornerRadius
        )

        // Innere Pille in Zeile 3: Zentriert in Pille 3, solide #B1ACFF
        val innerPillWidth = 62f * scaleX
        val innerPillHeight = 10f * scaleY
        val innerPillLeft = 15.25f * scaleX
        val innerPillTop = row3Top + (rowHeight - innerPillHeight) / 2f
        drawRoundRect(
            color = Color(0xFFB1ACFF),
            topLeft = Offset(innerPillLeft, innerPillTop),
            size = Size(innerPillWidth, innerPillHeight),
            cornerRadius = CornerRadius(innerPillHeight / 2f, innerPillHeight / 2f)
        )

        // Kreis 3: Äußerer Ring/Kreis solide #B7BFFF
        drawCircle(
            color = Color(0xFFB7BFFF),
            radius = circleRadius,
            center = Offset(circleCenterX, row3Top + rowHeight / 2f)
        )
        // Kreis 3 Innerer Punkt: Solide Weiß #FFFFFF
        val innerCircleRadius = 9.7f * minOf(scaleX, scaleY)
        drawCircle(
            color = Color(0xFFFFFFFF),
            radius = innerCircleRadius,
            center = Offset(circleCenterX, row3Top + rowHeight / 2f)
        )
    }
}
