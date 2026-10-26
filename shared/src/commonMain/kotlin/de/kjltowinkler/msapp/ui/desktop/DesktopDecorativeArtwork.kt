package de.kjltowinkler.msapp.ui.desktop

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.kjltowinkler.msapp.theme.DesktopBackground
import de.kjltowinkler.msapp.theme.DesktopDecorativeStroke
import kotlin.math.*

/**
 * Varianten der dekorativen Grafiken für jeden Screen gemäß Figma-Designs.
 * Jede Variante enthält exakt zwei voll ausgebildete, 100% symmetrische Formen.
 */
enum class DesktopArtworkVariant {
    USERNAME,           // Ellipse (oben) + 14-blättrige Blüten-Plakette (unten rechts)
    ONE_TIME_PASSWORD,  // Blüten-Plakette (oben links) + Ellipse (unten)
    NEW_PASSWORD,       // Abgerundetes Dreieck (oben) + 4-blättriges Kleeblatt (unten links)
    TWO_FACTOR_AUTH,    // Große Ellipse (oben mittig) + Blüten-Plakette (unten rechts)
    TWO_FACTOR_CONNECT, // Abgerundetes Dreieck (oben links) + Kleeblatt (unten rechts)
    TWO_FACTOR_CONFIRM, // Abgerundetes Quadrat / Squircle (Mitte links) + Ellipse (oben rechts)
    READY,              // Abgerundetes Dreieck (oben) + Blüten-Plakette (unten rechts)
    LOGIN_PASSWORD,     // Abgerundetes Quadrat (Mitte links) + Blüten-Plakette (oben rechts)
    LOGIN_READY         // Abgerundetes Dreieck (oben) + 4-blättriges Kleeblatt (unten rechts)
}

/**
 * Definition der mathematisch vollkommen symmetrischen geometrischen Formen.
 */
sealed class DesktopShape {
    data class Ellipse(val radiusX: Dp, val radiusY: Dp) : DesktopShape()
    data class FlowerBadge(val radius: Dp, val lobes: Int = 14, val amplitudeRatio: Float = 0.12f) : DesktopShape()
    data class Clover(val radius: Dp, val lobes: Int = 4) : DesktopShape()
    data class Squircle(val size: Dp, val cornerRadius: Dp) : DesktopShape()
    data class RoundedTriangle(val radius: Dp, val cornerRadius: Dp) : DesktopShape()
}

/**
 * Platzierung und Animationskonfiguration einer Form im Canvas.
 */
data class PlacedShape(
    val shape: DesktopShape,
    val relX: Float,
    val relY: Float,
    val baseRotation: Float = 0f,
    val isRotatingContinuously: Boolean = false,
    val rotationDurationMs: Int = 45000,
    val floatDistanceDp: Float = 10f,
    val floatDurationMs: Int = 5500
)

/**
 * Liefert die beiden Formen für eine gegebene Screen-Variante.
 */
fun getArtworkShapesForVariant(variant: DesktopArtworkVariant): Pair<PlacedShape, PlacedShape> {
    return when (variant) {
        DesktopArtworkVariant.USERNAME -> Pair(
            PlacedShape(
                shape = DesktopShape.Ellipse(radiusX = 260.dp, radiusY = 200.dp),
                relX = 0.30f,
                relY = 0.28f,
                baseRotation = -24f,
                floatDistanceDp = 8f,
                floatDurationMs = 7000
            ),
            PlacedShape(
                shape = DesktopShape.FlowerBadge(radius = 160.dp, lobes = 14),
                relX = 0.72f,
                relY = 0.74f,
                isRotatingContinuously = true,
                rotationDurationMs = 45000,
                floatDistanceDp = 12f,
                floatDurationMs = 5500
            )
        )
        DesktopArtworkVariant.ONE_TIME_PASSWORD -> Pair(
            PlacedShape(
                shape = DesktopShape.FlowerBadge(radius = 170.dp, lobes = 14),
                relX = 0.32f,
                relY = 0.22f,
                isRotatingContinuously = true,
                rotationDurationMs = 50000,
                floatDistanceDp = 10f,
                floatDurationMs = 6000
            ),
            PlacedShape(
                shape = DesktopShape.Ellipse(radiusX = 280.dp, radiusY = 210.dp),
                relX = 0.65f,
                relY = 0.78f,
                baseRotation = 18f,
                floatDistanceDp = 8f,
                floatDurationMs = 7500
            )
        )
        DesktopArtworkVariant.NEW_PASSWORD -> Pair(
            PlacedShape(
                shape = DesktopShape.RoundedTriangle(radius = 200.dp, cornerRadius = 45.dp),
                relX = 0.44f,
                relY = 0.24f,
                baseRotation = 12f,
                floatDistanceDp = 9f,
                floatDurationMs = 6500
            ),
            PlacedShape(
                shape = DesktopShape.Clover(radius = 175.dp, lobes = 4),
                relX = 0.28f,
                relY = 0.76f,
                baseRotation = 15f,
                isRotatingContinuously = true,
                rotationDurationMs = 60000,
                floatDistanceDp = 12f,
                floatDurationMs = 5200
            )
        )
        DesktopArtworkVariant.TWO_FACTOR_AUTH -> Pair(
            PlacedShape(
                shape = DesktopShape.Ellipse(radiusX = 290.dp, radiusY = 230.dp),
                relX = 0.38f,
                relY = 0.28f,
                baseRotation = -32f,
                floatDistanceDp = 8f,
                floatDurationMs = 8000
            ),
            PlacedShape(
                shape = DesktopShape.FlowerBadge(radius = 165.dp, lobes = 14),
                relX = 0.75f,
                relY = 0.74f,
                isRotatingContinuously = true,
                rotationDurationMs = 42000,
                floatDistanceDp = 11f,
                floatDurationMs = 5400
            )
        )
        DesktopArtworkVariant.TWO_FACTOR_CONNECT -> Pair(
            PlacedShape(
                shape = DesktopShape.RoundedTriangle(radius = 210.dp, cornerRadius = 50.dp),
                relX = 0.25f,
                relY = 0.25f,
                baseRotation = -35f,
                floatDistanceDp = 10f,
                floatDurationMs = 6200
            ),
            PlacedShape(
                shape = DesktopShape.Clover(radius = 180.dp, lobes = 4),
                relX = 0.72f,
                relY = 0.72f,
                baseRotation = 20f,
                isRotatingContinuously = true,
                rotationDurationMs = 55000,
                floatDistanceDp = 12f,
                floatDurationMs = 5600
            )
        )
        DesktopArtworkVariant.TWO_FACTOR_CONFIRM -> Pair(
            PlacedShape(
                shape = DesktopShape.Squircle(size = 320.dp, cornerRadius = 80.dp),
                relX = 0.36f,
                relY = 0.62f,
                baseRotation = 8f,
                floatDistanceDp = 10f,
                floatDurationMs = 6400
            ),
            PlacedShape(
                shape = DesktopShape.Ellipse(radiusX = 230.dp, radiusY = 175.dp),
                relX = 0.72f,
                relY = 0.20f,
                baseRotation = -20f,
                floatDistanceDp = 8f,
                floatDurationMs = 7200
            )
        )
        DesktopArtworkVariant.READY -> Pair(
            PlacedShape(
                shape = DesktopShape.RoundedTriangle(radius = 215.dp, cornerRadius = 50.dp),
                relX = 0.30f,
                relY = 0.24f,
                baseRotation = 22f,
                floatDistanceDp = 9f,
                floatDurationMs = 6800
            ),
            PlacedShape(
                shape = DesktopShape.FlowerBadge(radius = 170.dp, lobes = 14),
                relX = 0.72f,
                relY = 0.75f,
                isRotatingContinuously = true,
                rotationDurationMs = 48000,
                floatDistanceDp = 12f,
                floatDurationMs = 5500
            )
        )
        DesktopArtworkVariant.LOGIN_PASSWORD -> Pair(
            PlacedShape(
                shape = DesktopShape.Squircle(size = 310.dp, cornerRadius = 75.dp),
                relX = 0.34f,
                relY = 0.58f,
                baseRotation = -6f,
                floatDistanceDp = 9f,
                floatDurationMs = 6500
            ),
            PlacedShape(
                shape = DesktopShape.FlowerBadge(radius = 160.dp, lobes = 14),
                relX = 0.68f,
                relY = 0.22f,
                isRotatingContinuously = true,
                rotationDurationMs = 45000,
                floatDistanceDp = 11f,
                floatDurationMs = 5300
            )
        )
        DesktopArtworkVariant.LOGIN_READY -> Pair(
            PlacedShape(
                shape = DesktopShape.RoundedTriangle(radius = 200.dp, cornerRadius = 45.dp),
                relX = 0.40f,
                relY = 0.24f,
                baseRotation = 10f,
                floatDistanceDp = 9f,
                floatDurationMs = 6600
            ),
            PlacedShape(
                shape = DesktopShape.Clover(radius = 175.dp, lobes = 4),
                relX = 0.70f,
                relY = 0.74f,
                baseRotation = 18f,
                isRotatingContinuously = true,
                rotationDurationMs = 55000,
                floatDistanceDp = 12f,
                floatDurationMs = 5400
            )
        )
    }
}

/**
 * Wiederverwendbare, animierte Hintergrundgrafik für Desktop- und Web-Screens.
 * Zeigt je nach Screen-Variante die passenden voll-symmetrischen Formen an.
 */
@Composable
fun DesktopDecorativeArtwork(
    modifier: Modifier = Modifier,
    variant: DesktopArtworkVariant = DesktopArtworkVariant.USERNAME,
    enableAnimation: Boolean = true
) {
    val (primaryShape, secondaryShape) = getArtworkShapesForVariant(variant)
    val density = LocalDensity.current

    if (!enableAnimation) {
        Canvas(
            modifier = modifier
                .clipToBounds()
                .background(DesktopBackground)
        ) {
            val strokePx = 2.dp.toPx()
            drawPlacedShape(
                shapeDef = primaryShape,
                center = Offset(size.width * primaryShape.relX, size.height * primaryShape.relY),
                rotationDeg = primaryShape.baseRotation,
                scaleVal = 1f,
                strokeColor = DesktopDecorativeStroke,
                strokeWidth = strokePx,
                density = density
            )
            drawPlacedShape(
                shapeDef = secondaryShape,
                center = Offset(size.width * secondaryShape.relX, size.height * secondaryShape.relY),
                rotationDeg = secondaryShape.baseRotation,
                scaleVal = 1f,
                strokeColor = DesktopDecorativeStroke,
                strokeWidth = strokePx,
                density = density
            )
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ArtworkTransition")

    // --- Animationen für Form 1 ---
    val rot1 by if (primaryShape.isRotatingContinuously) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = primaryShape.rotationDurationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Rot1"
        )
    } else {
        infiniteTransition.animateFloat(
            initialValue = primaryShape.baseRotation - 3f,
            targetValue = primaryShape.baseRotation + 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 14000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Rock1"
        )
    }

    val floatY1 by infiniteTransition.animateFloat(
        initialValue = -primaryShape.floatDistanceDp,
        targetValue = primaryShape.floatDistanceDp,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = primaryShape.floatDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatY1"
    )

    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.018f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale1"
    )

    // --- Animationen für Form 2 ---
    val rot2 by if (secondaryShape.isRotatingContinuously) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = secondaryShape.rotationDurationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Rot2"
        )
    } else {
        infiniteTransition.animateFloat(
            initialValue = secondaryShape.baseRotation - 3.5f,
            targetValue = secondaryShape.baseRotation + 3.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Rock2"
        )
    }

    val floatY2 by infiniteTransition.animateFloat(
        initialValue = -secondaryShape.floatDistanceDp,
        targetValue = secondaryShape.floatDistanceDp,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = secondaryShape.floatDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatY2"
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale2"
    )

    Canvas(
        modifier = modifier
            .clipToBounds()
            .background(DesktopBackground)
    ) {
        val strokePx = 2.dp.toPx()

        // 1. Primäre Form
        val center1 = Offset(
            x = size.width * primaryShape.relX,
            y = size.height * primaryShape.relY + floatY1.dp.toPx()
        )
        drawPlacedShape(
            shapeDef = primaryShape,
            center = center1,
            rotationDeg = rot1,
            scaleVal = scale1,
            strokeColor = DesktopDecorativeStroke,
            strokeWidth = strokePx,
            density = density
        )

        // 2. Sekundäre Form
        val center2 = Offset(
            x = size.width * secondaryShape.relX,
            y = size.height * secondaryShape.relY + floatY2.dp.toPx()
        )
        drawPlacedShape(
            shapeDef = secondaryShape,
            center = center2,
            rotationDeg = rot2,
            scaleVal = scale2,
            strokeColor = DesktopDecorativeStroke,
            strokeWidth = strokePx,
            density = density
        )
    }
}

/**
 * Zeichnet eine geometrisch exakte, voll-symmetrische Form mit Rotation und Skalierung.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlacedShape(
    shapeDef: PlacedShape,
    center: Offset,
    rotationDeg: Float,
    scaleVal: Float,
    strokeColor: Color,
    strokeWidth: Float,
    density: androidx.compose.ui.unit.Density
) {
    rotate(degrees = rotationDeg, pivot = center) {
        scale(scale = scaleVal, pivot = center) {
            val path = Path().apply {
                when (val shape = shapeDef.shape) {
                    is DesktopShape.Ellipse -> {
                        val rx = with(density) { shape.radiusX.toPx() }
                        val ry = with(density) { shape.radiusY.toPx() }
                        addOval(
                            Rect(
                                left = center.x - rx,
                                top = center.y - ry,
                                right = center.x + rx,
                                bottom = center.y + ry
                            )
                        )
                    }
                    is DesktopShape.FlowerBadge -> {
                        val r = with(density) { shape.radius.toPx() }
                        val amp = r * shape.amplitudeRatio
                        val steps = shape.lobes * 24
                        for (i in 0..steps) {
                            val theta = (i.toFloat() / steps) * (2f * PI.toFloat())
                            val currentR = r + amp * cos(shape.lobes * theta)
                            val x = center.x + currentR * cos(theta)
                            val y = center.y + currentR * sin(theta)
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    is DesktopShape.Clover -> {
                        val radiusPx = with(density) { shape.radius.toPx() }
                        val d = radiusPx * 0.38f
                        val r = radiusPx * 0.52f
                        val disc = 8f * r * r - 4f * d * d
                        val xInt = (2f * d + sqrt(disc)) / 4f
                        val yInt = xInt
                        val angTop = atan2(yInt, xInt - d)
                        val angBot = atan2(-yInt, xInt - d)
                        val sweep = angTop - angBot
                        val stepsPerLobe = 30
                        var isFirst = true
                        for (i in 0 until shape.lobes) {
                            val rot = i.toFloat() * (2f * PI.toFloat() / shape.lobes)
                            val lcX = center.x + d * cos(rot)
                            val lcY = center.y - d * sin(rot)
                            for (s in 0..stepsPerLobe) {
                                val a = angBot + (s.toFloat() / stepsPerLobe) * sweep
                                val totalA = a + rot
                                val px = lcX + r * cos(totalA)
                                val py = lcY - r * sin(totalA)
                                if (isFirst) {
                                    moveTo(px, py)
                                    isFirst = false
                                } else {
                                    lineTo(px, py)
                                }
                            }
                        }
                        close()
                    }
                    is DesktopShape.Squircle -> {
                        val sizePx = with(density) { shape.size.toPx() }
                        val cornerPx = with(density) { shape.cornerRadius.toPx() }
                        val half = sizePx / 2f
                        addRoundRect(
                            RoundRect(
                                left = center.x - half,
                                top = center.y - half,
                                right = center.x + half,
                                bottom = center.y + half,
                                cornerRadius = CornerRadius(cornerPx, cornerPx)
                            )
                        )
                    }
                    is DesktopShape.RoundedTriangle -> {
                        val radiusPx = with(density) { shape.radius.toPx() }
                        val cornerPx = with(density) { shape.cornerRadius.toPx() }
                        val angles = floatArrayOf(
                            -PI.toFloat() / 2f,
                            -PI.toFloat() / 2f + 2f * PI.toFloat() / 3f,
                            -PI.toFloat() / 2f + 4f * PI.toFloat() / 3f
                        )
                        val verts = angles.map { a ->
                            Offset(center.x + radiusPx * cos(a), center.y + radiusPx * sin(a))
                        }
                        val dTangent = cornerPx * sqrt(3f)
                        val dCenter = cornerPx * 2f

                        var isFirst = true
                        for (i in 0 until 3) {
                            val vCurr = verts[i]
                            val vNext = verts[(i + 1) % 3]
                            val vPrev = verts[(i - 1 + 3) % 3]

                            val dxC = center.x - vCurr.x
                            val dyC = center.y - vCurr.y
                            val distC = sqrt(dxC * dxC + dyC * dyC)
                            val bx = dxC / distC
                            val by = dyC / distC

                            val ccX = vCurr.x + bx * dCenter
                            val ccY = vCurr.y + by * dCenter

                            val dPrev = sqrt((vCurr.x - vPrev.x).pow(2) + (vCurr.y - vPrev.y).pow(2))
                            val ex1 = (vCurr.x - vPrev.x) / dPrev
                            val ey1 = (vCurr.y - vPrev.y) / dPrev
                            val pInX = vCurr.x - ex1 * dTangent
                            val pInY = vCurr.y - ey1 * dTangent

                            val dNext = sqrt((vNext.x - vCurr.x).pow(2) + (vNext.y - vCurr.y).pow(2))
                            val ex2 = (vNext.x - vCurr.x) / dNext
                            val ey2 = (vNext.y - vCurr.y) / dNext
                            val pOutX = vCurr.x + ex2 * dTangent
                            val pOutY = vCurr.y + ey2 * dTangent

                            val aIn = atan2(pInY - ccY, pInX - ccX)
                            val aOut = atan2(pOutY - ccY, pOutX - ccX)

                            var diff = aOut - aIn
                            while (diff < 0) diff += 2f * PI.toFloat()
                            while (diff > 2f * PI.toFloat()) diff -= 2f * PI.toFloat()
                            if (diff > PI.toFloat()) diff -= 2f * PI.toFloat()

                            val steps = 20
                            for (s in 0..steps) {
                                val a = aIn + (s.toFloat() / steps) * diff
                                val px = ccX + cornerPx * cos(a)
                                val py = ccY + cornerPx * sin(a)
                                if (isFirst) {
                                    moveTo(px, py)
                                    isFirst = false
                                } else {
                                    lineTo(px, py)
                                }
                            }
                        }
                        close()
                    }
                }
            }
            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}
