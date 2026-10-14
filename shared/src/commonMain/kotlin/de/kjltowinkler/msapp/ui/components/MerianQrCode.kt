package de.kjltowinkler.msapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.circle
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.options.solid
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

/**
 * Konfiguration für das optische Styling des QR-Codes.
 * Erlaubt Backend- und UI-Entwicklern flexible Anpassungen (z.B. Material 3 Styles,
 * abgerundete Pixel oder klassisches Design).
 */
data class MerianQrCodeStyle(
    val darkColor: Color = Color.Black,
    val lightColor: Color = Color.White,
    val cornerRadius: Dp = 16.dp,
    val quietZonePadding: Dp = 16.dp,
    val roundPixels: Boolean = true
)

/**
 * Standardisierte Material 3 QR-Code Komponente für die Merianschule-App.
 * 
 * Vorteile:
 * - Vektorbasiert & gestochen scharf auf allen Bildschirmauflösungen (Android & iOS).
 * - Saubere Schnittstelle für spätere Backend-Anbindung (TOTP/otpauth:// URLs).
 * - Unterstützt ansprechende Material 3 Ästhetik mit sanft abgerundeten Ecken und weichem Container.
 *
 * @param content Der kodierte String (z.B. `otpauth://totp/Merianschule:Sebastian%20Wagner?secret=JBSWY3DPEHPK3PXP&issuer=Merianschule`)
 * @param modifier Compose Modifier
 * @param style Style-Optionen für Farben, Padding und Eckradien
 */
@Composable
fun MerianQrCode(
    content: String,
    modifier: Modifier = Modifier,
    style: MerianQrCodeStyle = MerianQrCodeStyle()
) {
    val painter = rememberQrCodePainter(data = content) {
        colors {
            dark = QrBrush.solid(style.darkColor)
            light = QrBrush.solid(style.lightColor)
        }
        if (style.roundPixels) {
            shapes {
                darkPixel = QrPixelShape.roundCorners(0.25f)
                ball = QrBallShape.roundCorners(0.25f)
                frame = QrFrameShape.roundCorners(0.25f)
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(style.cornerRadius))
            .background(style.lightColor)
            .padding(style.quietZonePadding),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = "QR-Code für Zwei-Faktor-Authentifizierung",
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
        )
    }
}
