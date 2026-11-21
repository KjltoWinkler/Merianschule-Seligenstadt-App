package de.kjltowinkler.msapp.ui.account

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.kjltowinkler.msapp.theme.getQuicksandFontFamily
import de.kjltowinkler.msapp.ui.components.handCursor
import de.kjltowinkler.msapp.util.NormalizedCropRect
import de.kjltowinkler.msapp.util.cropAndCompressImage
import kotlinx.coroutines.launch
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource
import kotlin.math.max
import kotlin.math.min

/**
 * Ein interaktiver Vollbild-Zuschneide-Screen im Stil von Google Fotos / Google Konten.
 * Ermöglicht freies Verschieben (Pan), Zoomen (Pinch-to-Zoom), Drehen (90°) und
 * die visuelle Ausrichtung mit Drittel-Raster innerhalb eines kreisrunden Bildausschnitts.
 */
@Composable
fun AvatarCropScreen(
    imageBytes: ByteArray,
    onDismissRequest: () -> Unit,
    onCropConfirmed: (croppedBytes: ByteArray) -> Unit,
    useMaterialYou: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()

    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var decodeError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(imageBytes) {
        try {
            imageBitmap = imageBytes.decodeToImageBitmap()
        } catch (t: Throwable) {
            decodeError = "Bild konnte nicht geöffnet werden: ${t.message}"
        }
    }

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var userScale by remember { mutableStateOf(1f) }
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }
    var rotationDegrees by remember { mutableStateOf(0) }
    var isInteracting by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {
            if (!isProcessing) onDismissRequest()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isProcessing,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F1410)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // --- TOP BAR (Google Fotos Style) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismissRequest,
                        enabled = !isProcessing,
                        modifier = Modifier.handCursor()
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_close),
                            contentDescription = "Abbrechen",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Foto anpassen",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = getQuicksandFontFamily(),
                            color = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // 90° Drehen Button
                    IconButton(
                        onClick = {
                            rotationDegrees = (rotationDegrees + 90) % 360
                            userScale = 1f
                            panX = 0f
                            panY = 0f
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.handCursor()
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_history),
                            contentDescription = "90° drehen",
                            tint = Color.White
                        )
                    }

                    // Zurücksetzen Button
                    TextButton(
                        onClick = {
                            userScale = 1f
                            panX = 0f
                            panY = 0f
                            rotationDegrees = 0
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.handCursor()
                    ) {
                        Text(
                            text = "Zurücksetzen",
                            color = Color(0xFFDFE9DE),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // --- MITTLERER VIEWPORT MIT INTERAKTIVEM BILD UND KREIS-SCHABLONE ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onSizeChanged { viewportSize = it },
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = imageBitmap
                    if (bmp != null && viewportSize.width > 0 && viewportSize.height > 0) {
                        val density = androidx.compose.ui.platform.LocalDensity.current
                        val viewW = viewportSize.width.toFloat()
                        val viewH = viewportSize.height.toFloat()

                        // Kreis-Ausschnitt: Radius beträgt 38% der kleineren Kante
                        val cropRadius = min(viewW, viewH) * 0.38f
                        val cropDiameter = cropRadius * 2f

                        // Dimensionen nach möglicher 90°/270° Drehung
                        val isQuarterTurn = rotationDegrees == 90 || rotationDegrees == 270
                        val effectiveImgW = if (isQuarterTurn) bmp.height.toFloat() else bmp.width.toFloat()
                        val effectiveImgH = if (isQuarterTurn) bmp.width.toFloat() else bmp.height.toFloat()

                        // Basis-Skalierung, damit das Bild den Kreis vollständig füllt
                        val baseScale = max(cropDiameter / effectiveImgW, cropDiameter / effectiveImgH)
                        val totalScale = baseScale * userScale

                        val curW = effectiveImgW * totalScale
                        val curH = effectiveImgH * totalScale

                        // Erlaubten Pan so beschränken, dass kein Rand im Kreis sichtbar wird
                        val maxPanX = max(0f, (curW - cropDiameter) / 2f)
                        val maxPanY = max(0f, (curH - cropDiameter) / 2f)

                        panX = panX.coerceIn(-maxPanX, maxPanX)
                        panY = panY.coerceIn(-maxPanY, maxPanY)

                        val imgDisplayWidthDp = with(density) { (bmp.width.toFloat() * baseScale * userScale).toDp() }
                        val imgDisplayHeightDp = with(density) { (bmp.height.toFloat() * baseScale * userScale).toDp() }

                        // 1. Das bewegliche Bild
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(imageBitmap, rotationDegrees) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        isInteracting = true
                                        userScale = (userScale * zoom).coerceIn(1f, 4f)
                                        panX += pan.x
                                        panY += pan.y
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bmp,
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .size(
                                        width = imgDisplayWidthDp,
                                        height = imgDisplayHeightDp
                                    )
                                    .graphicsLayer {
                                        translationX = panX
                                        translationY = panY
                                        rotationZ = rotationDegrees.toFloat()
                                    }
                            )
                        }

                        // 2. Halbdunkle Maske mit transparentem Kreis & Drittel-Raster
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val center = Offset(size.width / 2f, size.height / 2f)

                            val screenPath = Path().apply {
                                addRect(Rect(Offset.Zero, size))
                            }
                            val circlePath = Path().apply {
                                addOval(Rect(center, cropRadius))
                            }
                            val overlayPath = Path.combine(PathOperation.Difference, screenPath, circlePath)

                            // Abgedunkelter Hintergrund außerhalb des Kreises
                            drawPath(overlayPath, color = Color.Black.copy(alpha = 0.72f))

                            // Weißer Kreisrahmen
                            drawCircle(
                                color = Color.White.copy(alpha = 0.9f),
                                radius = cropRadius,
                                center = center,
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // Feines 3x3 Drittel-Raster innerhalb des Kreises (wie Google Fotos)
                            val gridColor = Color.White.copy(alpha = 0.3f)
                            val strokeW = 1.dp.toPx()
                            val left = center.x - cropRadius
                            val top = center.y - cropRadius

                            clipPath(circlePath) {
                                val oneThird = cropDiameter / 3f
                                val twoThirds = cropDiameter * 2f / 3f

                                // Horizontale Linien
                                drawLine(gridColor, Offset(left, top + oneThird), Offset(left + cropDiameter, top + oneThird), strokeW)
                                drawLine(gridColor, Offset(left, top + twoThirds), Offset(left + cropDiameter, top + twoThirds), strokeW)

                                // Vertikale Linien
                                drawLine(gridColor, Offset(left + oneThird, top), Offset(left + oneThird, top + cropDiameter), strokeW)
                                drawLine(gridColor, Offset(left + twoThirds, top), Offset(left + twoThirds, top + cropDiameter), strokeW)
                            }
                        }
                    } else if (decodeError != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = decodeError!!,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // --- UNTERE LEISTE MIT HINWEIS & BESTÄTIGEN-BUTTON ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Verschieben und zoomen, um das Gesicht zu zentrieren",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismissRequest,
                            enabled = !isProcessing,
                            modifier = Modifier
                                .weight(1f)
                                .handCursor(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Text(text = "Abbrechen", fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = {
                                val bmp = imageBitmap ?: return@Button
                                if (viewportSize.width <= 0 || viewportSize.height <= 0 || isProcessing) return@Button

                                isProcessing = true
                                coroutineScope.launch {
                                    try {
                                        val viewW = viewportSize.width.toFloat()
                                        val viewH = viewportSize.height.toFloat()
                                        val cropRadius = min(viewW, viewH) * 0.38f
                                        val cropDiameter = cropRadius * 2f

                                        val isQuarterTurn = rotationDegrees == 90 || rotationDegrees == 270
                                        val effectiveImgW = if (isQuarterTurn) bmp.height.toFloat() else bmp.width.toFloat()
                                        val effectiveImgH = if (isQuarterTurn) bmp.width.toFloat() else bmp.height.toFloat()

                                        val baseScale = max(cropDiameter / effectiveImgW, cropDiameter / effectiveImgH)
                                        val totalScale = baseScale * userScale

                                        val curW = effectiveImgW * totalScale
                                        val curH = effectiveImgH * totalScale

                                        val cx = viewW / 2f
                                        val cy = viewH / 2f

                                        val imgLeftOnCanvas = (cx + panX) - curW / 2f
                                        val imgTopOnCanvas = (cy + panY) - curH / 2f

                                        val cropBoxLeft = cx - cropRadius
                                        val cropBoxTop = cy - cropRadius

                                        val normLeft = ((cropBoxLeft - imgLeftOnCanvas) / curW).coerceIn(0f, 1f)
                                        val normTop = ((cropBoxTop - imgTopOnCanvas) / curH).coerceIn(0f, 1f)
                                        val normWidth = (cropDiameter / curW).coerceIn(0f, 1f - normLeft)
                                        val normHeight = (cropDiameter / curH).coerceIn(0f, 1f - normTop)

                                        val cropRect = NormalizedCropRect(
                                            left = normLeft,
                                            top = normTop,
                                            width = normWidth,
                                            height = normHeight
                                        )

                                        val croppedBytes = cropAndCompressImage(
                                            sourceBytes = imageBytes,
                                            cropRect = cropRect,
                                            rotationDegrees = rotationDegrees,
                                            targetSize = 512,
                                            quality = 90
                                        )

                                        isProcessing = false
                                        onCropConfirmed(croppedBytes)
                                    } catch (e: Exception) {
                                        isProcessing = false
                                        decodeError = "Zuschneiden fehlgeschlagen: ${e.message}"
                                    }
                                }
                            },
                            enabled = !isProcessing && imageBitmap != null,
                            modifier = Modifier
                                .weight(1.5f)
                                .handCursor(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (useMaterialYou) MaterialTheme.colorScheme.primary else Color(0xFF8CD88C)
                            )
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = if (useMaterialYou) MaterialTheme.colorScheme.onPrimary else Color(0xFF00390E),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Zuschneiden...",
                                    fontWeight = FontWeight.Bold,
                                    color = if (useMaterialYou) MaterialTheme.colorScheme.onPrimary else Color(0xFF00390E)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (useMaterialYou) MaterialTheme.colorScheme.onPrimary else Color(0xFF00390E)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Zuschneiden & Fertig",
                                    fontWeight = FontWeight.Bold,
                                    color = if (useMaterialYou) MaterialTheme.colorScheme.onPrimary else Color(0xFF00390E)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
