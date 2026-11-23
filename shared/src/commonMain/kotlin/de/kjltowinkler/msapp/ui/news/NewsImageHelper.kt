package de.kjltowinkler.msapp.ui.news

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import de.kjltowinkler.msapp.ui.account.rememberAvatarBitmap
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun NewsImageView(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    when {
        imageUrl == "res:news_stairs" -> {
            Image(
                painter = painterResource(Res.drawable.news_stairs),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        imageUrl == "res:news_cups" -> {
            Image(
                painter = painterResource(Res.drawable.news_cups),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        imageUrl == "res:news_certificates" -> {
            Image(
                painter = painterResource(Res.drawable.news_certificates),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        imageUrl == "res:app_icon" -> {
            Image(
                painter = painterResource(Res.drawable.app_icon),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        !imageUrl.isNullOrBlank() -> {
            val bitmap = rememberAvatarBitmap(imageUrl)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = contentScale
                )
            } else {
                Box(
                    modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.bg_desktop_shapes),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(0.6f),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
        else -> {
            Box(
                modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.bg_desktop_shapes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(0.6f),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
