package de.kjltowinkler.msapp.wear.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import de.kjltowinkler.msapp.wear.R

/**
 * Onboarding Screen #1: Startbildschirm / Frage "Mit deinem Konto anmelden?"
 * Exakt nach Design onboarding#1.png.
 */
@Composable
fun OnboardingPromptScreen(
    onConfirm: () -> Unit
) {
    val scrollState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onConfirm,
                buttonSize = EdgeButtonSize.Large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE6DCFA),
                    contentColor = Color(0xFF231846)
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_forward),
                    contentDescription = "Weiter",
                    tint = Color(0xFF231846),
                    modifier = Modifier.size(38.dp)
                )
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                // Wear OS Compose M3 FilledTonalIconButton als Badge
                androidx.wear.compose.material3.FilledTonalIconButton(
                    onClick = {},
                    modifier = Modifier.size(36.dp),
                    colors = androidx.wear.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color(0xFFA8B8EE),
                        contentColor = Color(0xFF1B1B1F)
                    )
                ) {
                    Text(
                        text = "!",
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                // Hauptfrage in Google Sans Flex Medium
                Text(
                    text = "Mit deinem Konto\nanmelden?",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    fontSize = 17.sp,
                    lineHeight = 23.sp,
                    color = Color.White
                )
            }
        }
    }
}
