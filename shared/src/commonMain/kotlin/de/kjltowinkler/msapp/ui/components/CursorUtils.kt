package de.kjltowinkler.msapp.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon

/**
 * Modifier extension to display a pointer hand cursor on hover.
 * Essential for desktop and web apps so users get immediate visual interaction hints
 * on clickable components (buttons, links, chips, cards, list items, checkboxes, switches, etc.).
 */
fun Modifier.handCursor(enabled: Boolean = true): Modifier =
    if (enabled) this.pointerHoverIcon(PointerIcon.Hand) else this

/**
 * Modifier extension to display a text selection cursor on hover.
 */
fun Modifier.textCursor(enabled: Boolean = true): Modifier =
    if (enabled) this.pointerHoverIcon(PointerIcon.Text) else this
