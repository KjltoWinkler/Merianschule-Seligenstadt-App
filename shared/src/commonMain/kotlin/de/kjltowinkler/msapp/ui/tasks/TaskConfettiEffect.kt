package de.kjltowinkler.msapp.ui.tasks

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class ConfettiParticle(
    val color: Color,
    val angle: Double,
    val speed: Float,
    val rotationSpeed: Float,
    val sizeWidth: Float,
    val sizeHeight: Float,
    val isCircle: Boolean
)

data class ConfettiBurst(
    val id: Long,
    val origin: Offset
)

/**
 * Feines, elegantes Konfetti-Feuerwerk, das direkt zentriert am Kreis (Checkbox)
 * radial in alle Richtungen explodiert und sanft nach unten fällt.
 */
@Composable
fun TaskConfettiEffect(
    origin: Offset,
    onFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val density = LocalDensity.current

    val particles = remember {
        val palette = listOf(
            Color(0xFF4CAF50), // Frisches Grün
            Color(0xFF81C784), // Hellgrün
            Color(0xFFFFD54F), // Sonnengelb / Gold
            Color(0xFFFF9800), // Orange
            Color(0xFF42A5F5), // Hellblau
            Color(0xFFAB47BC), // Violett
            Color(0xFFFF4081), // Pink
            Color(0xFF26A69A), // Türkis
            Color(0xFFFFFFFF)  // Weißer Glanz
        )

        with(density) {
            List(42) {
                // Radialer Ausbruch um 360 Grad
                val angleDeg = Random.nextDouble(0.0, 360.0)
                val angleRad = angleDeg * (PI / 180.0)
                val speedPx = (Random.nextFloat() * 65f + 30f).dp.toPx()
                val widthPx = if (Random.nextBoolean()) {
                    (Random.nextFloat() * 4f + 3f).dp.toPx()
                } else {
                    (Random.nextFloat() * 6f + 5f).dp.toPx()
                }
                val heightPx = (Random.nextFloat() * 4f + 3f).dp.toPx()

                ConfettiParticle(
                    color = palette.random(),
                    angle = angleRad,
                    speed = speedPx,
                    rotationSpeed = Random.nextFloat() * 720f - 360f,
                    sizeWidth = widthPx,
                    sizeHeight = heightPx,
                    isCircle = Random.nextFloat() < 0.45f
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        onFinished()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val p = progress.value
        val gravity = with(density) { 50.dp.toPx() * p * p }

        // Feiner Schockwellen-Ring direkt um den geklickten Kreis
        if (p < 0.4f) {
            val ringProgress = p / 0.4f
            val ringRadius = with(density) { (10f + 24f * ringProgress).dp.toPx() }
            val ringAlpha = ((1f - ringProgress) * 0.65f).coerceIn(0f, 1f)
            drawCircle(
                color = Color(0xFF4CAF50).copy(alpha = ringAlpha),
                radius = ringRadius,
                center = origin,
                style = Stroke(width = with(density) { 2.dp.toPx() })
            )
        }

        // Partikel
        particles.forEach { particle ->
            val distance = particle.speed * p
            val x = origin.x + (cos(particle.angle) * distance).toFloat()
            val y = origin.y + (sin(particle.angle) * distance).toFloat() + gravity

            val currentAlpha = if (p > 0.65f) {
                ((1f - p) / 0.35f).coerceIn(0f, 1f)
            } else {
                1f
            }

            val scale = if (p > 0.75f) {
                ((1f - p) / 0.25f).coerceIn(0.2f, 1f)
            } else {
                1f
            }

            val pColor = particle.color.copy(alpha = currentAlpha)
            val currentWidth = particle.sizeWidth * scale
            val currentHeight = particle.sizeHeight * scale

            rotate(degrees = particle.rotationSpeed * p, pivot = Offset(x, y)) {
                if (particle.isCircle) {
                    drawCircle(
                        color = pColor,
                        radius = currentWidth / 2f,
                        center = Offset(x, y)
                    )
                } else {
                    drawRect(
                        color = pColor,
                        topLeft = Offset(x - currentWidth / 2f, y - currentHeight / 2f),
                        size = Size(currentWidth, currentHeight)
                    )
                }
            }
        }
    }
}
