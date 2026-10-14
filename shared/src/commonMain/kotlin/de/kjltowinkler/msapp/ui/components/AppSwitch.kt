/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.kjltowinkler.msapp.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import merianschuleseligenstadtapp.shared.generated.resources.Res
import merianschuleseligenstadtapp.shared.generated.resources.ic_event_upcoming
import merianschuleseligenstadtapp.shared.generated.resources.ic_today
import org.jetbrains.compose.resources.painterResource

/**
 * Farb-Definitionen für den Switch exakt abgeleitet aus Figma Designs:
 * (FigmaDesigns/other/toggleswitch.png)
 *
 * - Track: #2F402F (Dunkelgrüner Container)
 * - Thumb (Checked / An): #93DBAD (Heller Mint-Ton)
 * - Thumb (Unchecked / Aus): #1B281F (Sehr dunkles Waldgrün)
 * - Icon (Checked / An): #1B281F (Dunkles Icon auf hellem Thumb)
 * - Icon (Unchecked / Aus): #93DBAD (Helles Mint-Icon auf dunklem Thumb)
 */
object AppSwitchDefaults {
    val TrackColor = Color(0xFF2F402F)
    val CheckedThumbColor = Color(0xFF93DBAD)
    val UncheckedThumbColor = Color(0xFF1B281F)
    val CheckedIconColor = Color(0xFF1B281F)
    val UncheckedIconColor = Color(0xFF93DBAD)

    /**
     * Erstellt [SwitchColors] mit vollständiger Material You-Unterstützung:
     *
     * Wenn [useMaterialYou] = true (Standard):
     * - Checked Thumb: [MaterialTheme.colorScheme.primary] (Dynamische Akzentfarbe des Systems/Themes)
     * - Checked Track: [MaterialTheme.colorScheme.surfaceContainerHighest]
     * - Checked Icon: [MaterialTheme.colorScheme.onPrimary]
     * - Unchecked Thumb: [MaterialTheme.colorScheme.surfaceContainerHigh]
     * - Unchecked Track: [MaterialTheme.colorScheme.surfaceContainerHighest]
     * - Unchecked Icon: [MaterialTheme.colorScheme.primary]
     *
     * Wenn [useMaterialYou] = false:
     * - Exakte Figma Farben: Track: #2F402F, CheckedThumb: #93DBAD, CheckedIcon: #2A3E31,
     *   UncheckedThumb: #1B281F, UncheckedIcon: #93DBAD.
     */
    @Composable
    fun colors(
        useMaterialYou: Boolean = true,
        checkedThumbColor: Color = if (useMaterialYou) MaterialTheme.colorScheme.primary else CheckedThumbColor,
        checkedTrackColor: Color = if (useMaterialYou) MaterialTheme.colorScheme.surfaceContainerHighest else TrackColor,
        checkedBorderColor: Color = Color.Transparent,
        checkedIconColor: Color = if (useMaterialYou) MaterialTheme.colorScheme.onPrimary else CheckedIconColor,
        uncheckedThumbColor: Color = if (useMaterialYou) MaterialTheme.colorScheme.surfaceContainerHigh else UncheckedThumbColor,
        uncheckedTrackColor: Color = if (useMaterialYou) MaterialTheme.colorScheme.surfaceContainerHighest else TrackColor,
        uncheckedBorderColor: Color = Color.Transparent,
        uncheckedIconColor: Color = if (useMaterialYou) MaterialTheme.colorScheme.primary else UncheckedIconColor,
        disabledCheckedThumbColor: Color = (if (useMaterialYou) MaterialTheme.colorScheme.primary else CheckedThumbColor).copy(alpha = 0.38f),
        disabledCheckedTrackColor: Color = (if (useMaterialYou) MaterialTheme.colorScheme.surfaceContainerHighest else TrackColor).copy(alpha = 0.38f),
        disabledUncheckedThumbColor: Color = (if (useMaterialYou) MaterialTheme.colorScheme.surfaceContainerHigh else UncheckedThumbColor).copy(alpha = 0.38f),
        disabledUncheckedTrackColor: Color = (if (useMaterialYou) MaterialTheme.colorScheme.surfaceContainerHighest else TrackColor).copy(alpha = 0.38f),
    ): SwitchColors = SwitchDefaults.colors(
        checkedThumbColor = checkedThumbColor,
        checkedTrackColor = checkedTrackColor,
        checkedBorderColor = checkedBorderColor,
        checkedIconColor = checkedIconColor,
        uncheckedThumbColor = uncheckedThumbColor,
        uncheckedTrackColor = uncheckedTrackColor,
        uncheckedBorderColor = uncheckedBorderColor,
        uncheckedIconColor = uncheckedIconColor,
        disabledCheckedThumbColor = disabledCheckedThumbColor,
        disabledCheckedTrackColor = disabledCheckedTrackColor,
        disabledUncheckedThumbColor = disabledUncheckedThumbColor,
        disabledUncheckedTrackColor = disabledUncheckedTrackColor,
    )

    /**
     * Google Material Symbol: 'event_upcoming' (für den AN-Zustand).
     */
    val EventUpcomingIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "EventUpcoming",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 4f)
                horizontalLineToRelative(-1f)
                verticalLineTo(2f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(2f)
                horizontalLineTo(8f)
                verticalLineTo(2f)
                horizontalLineTo(6f)
                verticalLineToRelative(2f)
                horizontalLineTo(5f)
                curveToRelative(-1.11f, 0f, -1.99f, 0.9f, -1.99f, 2f)
                lineTo(3f, 20f)
                curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
                horizontalLineToRelative(7f)
                verticalLineToRelative(-2f)
                horizontalLineTo(5f)
                verticalLineTo(10f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(2f)
                verticalLineTo(6f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(19f, 18f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                close()
                moveTo(19f, 14f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                close()
                moveTo(5f, 8f)
                verticalLineTo(6f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(2f)
                horizontalLineTo(5f)
                close()
                moveTo(16.25f, 23f)
                lineTo(14f, 20.75f)
                lineToRelative(1.41f, -1.41f)
                lineToRelative(2.09f, 2.08f)
                lineToRelative(5.09f, -5.09f)
                lineToRelative(1.41f, 1.41f)
                lineTo(16.25f, 23f)
                close()
            }
        }.build()
    }

    /**
     * Google Material Symbol: 'today' (für den AUS-Zustand).
     */
    val TodayIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "Today",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 3f)
                horizontalLineToRelative(-1f)
                verticalLineTo(1f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(2f)
                horizontalLineTo(8f)
                verticalLineTo(1f)
                horizontalLineTo(6f)
                verticalLineToRelative(2f)
                horizontalLineTo(5f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(5f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(19f, 19f)
                horizontalLineTo(5f)
                verticalLineTo(8f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(11f)
                close()
                moveTo(7f, 10f)
                horizontalLineToRelative(5f)
                verticalLineToRelative(5f)
                horizontalLineTo(7f)
                verticalLineToRelative(-5f)
                close()
            }
        }.build()
    }
}

/**
 * Switch-Komponente im exakten Stil des Figma-Designs (`toggleswitch.png`):
 * - Track: Kapselform (Höhe: 32.dp, Breite: 60.dp) mit abgerundetem Container
 * - Thumb: Kreisförmiger Button (26.dp) mit zentriertem Icon (18.dp)
 * - Wenn an (checked / rechts):
 *   - Thumb: [MaterialTheme.colorScheme.primary] (z. B. leuchtendes Mint / dynamisches Blau)
 *   - Icon: [MaterialTheme.colorScheme.onPrimary]
 *   - Icon: 'ic_event_upcoming'
 * - Wenn aus (unchecked / links):
 *   - Thumb: Eigener runder Hintergrund [MaterialTheme.colorScheme.surfaceContainerHigh]
 *   - Icon: [MaterialTheme.colorScheme.primary]
 *   - Icon: 'ic_today'
 *
 * @param checked Ob der Schalter aktuell aktiv (an) ist.
 * @param onCheckedChange Callback, wenn sich der Status ändert.
 * @param modifier Optionaler Modifikator.
 * @param scale Skalierungsfaktor für die Gesamtgröße (Standard: 1.0f).
 * @param enabled Ob der Switch interaktiv ist.
 * @param colors Angepasste Farben; standardmäßig [AppSwitchDefaults.colors].
 * @param showThumbIcon Ob die Status-Icons im Thumb gerendert werden sollen (Standard: true).
 * @param interactionSource Optionaler [MutableInteractionSource].
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    scale: Float = 1.0f,
    enabled: Boolean = true,
    colors: SwitchColors = AppSwitchDefaults.colors(),
    showThumbIcon: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val trackWidth = 52.dp
    val trackHeight = 32.dp
    val thumbSize = 24.dp
    val padding = 4.dp

    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.checkedTrackColor else colors.uncheckedTrackColor,
        animationSpec = tween(durationMillis = 200),
        label = "trackColor"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) colors.checkedThumbColor else colors.uncheckedThumbColor,
        animationSpec = tween(durationMillis = 200),
        label = "thumbColor"
    )

    val iconColor by animateColorAsState(
        targetValue = if (checked) colors.checkedIconColor else colors.uncheckedIconColor,
        animationSpec = tween(durationMillis = 200),
        label = "iconColor"
    )

    val maxOffset = trackWidth - thumbSize - (padding * 2)
    val thumbOffsetFraction by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "thumbOffset"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .width(trackWidth)
            .height(trackHeight)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                enabled = enabled && onCheckedChange != null,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onClick = { onCheckedChange?.invoke(!checked) }
            )
            .padding(padding),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = maxOffset * thumbOffsetFraction)
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor),
            contentAlignment = Alignment.Center
        ) {
            if (showThumbIcon) {
                val painter = if (checked) {
                    painterResource(Res.drawable.ic_event_upcoming)
                } else {
                    painterResource(Res.drawable.ic_today)
                }
                Icon(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = iconColor
                )
            }
        }
    }
}

/**
 * M3 Sample: Einfacher Switch ohne Icons.
 */
@Preview
@Composable
fun SwitchSample() {
    var checked by rememberSaveable { mutableStateOf(true) }
    AppSwitch(
        modifier = Modifier.semantics { contentDescription = "Demo" },
        checked = checked,
        onCheckedChange = { checked = it },
        showThumbIcon = false,
    )
}

/**
 * M3 Sample: Switch mit Design-Icons (event_upcoming wenn an, today wenn aus).
 */
@Preview
@Composable
fun SwitchWithThumbIconSample() {
    var checked by rememberSaveable { mutableStateOf(true) }
    AppSwitch(
        modifier = Modifier.semantics { contentDescription = "Demo with icon" },
        checked = checked,
        onCheckedChange = { checked = it },
        showThumbIcon = true,
    )
}

/**
 * Preview zur visuellen Gegenüberstellung des AN- und AUS-Zustands entsprechend `toggleswitch.png`.
 */
@Preview
@Composable
fun FigmaToggleSwitchPreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("An (event_upcoming)", color = Color.White, fontSize = 14.sp)
            AppSwitch(
                checked = true,
                onCheckedChange = {},
                showThumbIcon = true,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Aus (today)", color = Color.White, fontSize = 14.sp)
            AppSwitch(
                checked = false,
                onCheckedChange = {},
                showThumbIcon = true,
            )
        }
    }
}
