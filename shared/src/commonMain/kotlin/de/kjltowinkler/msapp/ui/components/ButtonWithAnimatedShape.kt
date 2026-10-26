package de.kjltowinkler.msapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.ButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A Hero Call-To-Action (CTA) or Flow Completion button with Material 3 Expressive shape morphing.
 *
 * Strict Architectural Guidelines:
 * - RESTRICTED exclusively to:
 *   - The single primary Hero Call-To-Action (CTA) on a screen.
 *   - Final flow completion triggers (e.g. "Fertigstellen", "Los geht's!", "Get Started").
 *   - Explicit state transitions with functional meaning.
 * - NEVER apply to secondary, tertiary, or purely navigational utility elements.
 * - Intermediate navigation ("Weiter") must use standard [Button].
 * - Back actions ("Zurück") must use standard [androidx.compose.material3.IconButton], [androidx.compose.material3.TextButton], or [androidx.compose.material3.OutlinedButton].
 */
@Composable
fun ButtonWithAnimatedShape(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ButtonShapes = ButtonDefaults.shapes(),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shapes = shapes,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}
