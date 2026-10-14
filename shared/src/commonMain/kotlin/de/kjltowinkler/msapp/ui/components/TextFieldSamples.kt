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
@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("DEPRECATION") // b/552879150

package de.kjltowinkler.msapp.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.DefaultFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.maxTextLength
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Annotation für Dokumentations-Samples gemäß AOSP.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Sampled

private fun InputTransformation.maxLengthTrim(maxLength: Int): InputTransformation =
    InputTransformation {
        if (length > maxLength) {
            revertAllChanges()
        }
    }

private fun CharSequence.isDigitsOnly(): Boolean = all { it.isDigit() }

/**
 * Vektor-Icons für die Samples (Clear, Favorite, Visibility, VisibilityOff).
 */
internal object SampleIcons {
    object Filled {
        val Clear: ImageVector by lazy {
            createSampleIcon("Clear") {
                moveTo(19.0f, 6.41f)
                lineTo(17.59f, 5.0f)
                lineTo(12.0f, 10.59f)
                lineTo(6.41f, 5.0f)
                lineTo(5.0f, 6.41f)
                lineTo(10.59f, 12.0f)
                lineTo(5.0f, 17.59f)
                lineTo(6.41f, 19.0f)
                lineTo(12.0f, 13.41f)
                lineTo(17.59f, 19.0f)
                lineTo(19.0f, 17.59f)
                lineTo(13.41f, 12.0f)
                close()
            }
        }

        val Favorite: ImageVector by lazy {
            createSampleIcon("Favorite") {
                moveTo(12.0f, 21.35f)
                lineToRelative(-1.45f, -1.32f)
                curveTo(5.4f, 15.36f, 2.0f, 12.28f, 2.0f, 8.5f)
                curveTo(2.0f, 5.42f, 4.42f, 3.0f, 7.5f, 3.0f)
                curveToRelative(1.74f, 0.0f, 3.41f, 0.81f, 4.5f, 2.09f)
                curveTo(13.09f, 3.81f, 14.76f, 3.0f, 16.5f, 3.0f)
                curveTo(19.58f, 3.0f, 22.0f, 5.42f, 22.0f, 8.5f)
                curveToRelative(0.0f, 3.78f, -3.4f, 6.86f, -8.55f, 11.54f)
                lineTo(12.0f, 21.35f)
                close()
            }
        }

        val Visibility: ImageVector by lazy {
            createSampleIcon("Visibility") {
                moveTo(12.0f, 4.5f)
                curveTo(7.0f, 4.5f, 2.73f, 7.61f, 1.0f, 12.0f)
                curveToRelative(1.73f, 4.39f, 6.0f, 7.5f, 11.0f, 7.5f)
                reflectiveCurveToRelative(9.27f, -3.11f, 11.0f, -7.5f)
                curveToRelative(-1.73f, -4.39f, -6.0f, -7.5f, -11.0f, -7.5f)
                close()
                moveTo(12.0f, 17.0f)
                curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
                reflectiveCurveToRelative(2.24f, -5.0f, 5.0f, -5.0f)
                reflectiveCurveToRelative(5.0f, 2.24f, 5.0f, 5.0f)
                reflectiveCurveToRelative(-2.24f, 5.0f, -5.0f, 5.0f)
                close()
                moveTo(12.0f, 9.0f)
                curveToRelative(-1.66f, 0.0f, -3.0f, 1.34f, -3.0f, 3.0f)
                reflectiveCurveToRelative(1.34f, 3.0f, 3.0f, 3.0f)
                reflectiveCurveToRelative(3.0f, -1.34f, 3.0f, -3.0f)
                reflectiveCurveToRelative(-1.34f, -3.0f, -3.0f, -3.0f)
                close()
            }
        }

        val VisibilityOff: ImageVector by lazy {
            createSampleIcon("VisibilityOff") {
                moveTo(12.0f, 7.0f)
                curveToRelative(2.76f, 0.0f, 5.0f, 2.24f, 5.0f, 5.0f)
                curveToRelative(0.0f, 0.65f, -0.13f, 1.26f, -0.36f, 1.82f)
                lineToRelative(2.92f, 2.92f)
                curveToRelative(1.51f, -1.26f, 2.7f, -2.89f, 3.44f, -4.74f)
                curveToRelative(-1.73f, -4.39f, -6.0f, -7.5f, -11.0f, -7.5f)
                curveToRelative(-1.4f, 0.0f, -2.74f, 0.25f, -3.98f, 0.7f)
                lineToRelative(2.16f, 2.16f)
                curveTo(10.74f, 7.13f, 11.35f, 7.0f, 12.0f, 7.0f)
                close()
                moveTo(2.0f, 4.27f)
                lineToRelative(2.28f, 2.28f)
                lineToRelative(0.46f, 0.46f)
                curveTo(3.08f, 8.3f, 1.78f, 10.02f, 1.0f, 12.0f)
                curveToRelative(1.73f, 4.39f, 6.0f, 7.5f, 11.0f, 7.5f)
                curveToRelative(1.55f, 0.0f, 3.03f, -0.3f, 4.38f, -0.84f)
                lineToRelative(0.42f, 0.42f)
                lineTo(19.73f, 22.0f)
                lineTo(21.0f, 20.73f)
                lineTo(3.27f, 3.0f)
                lineTo(2.0f, 4.27f)
                close()
            }
        }
    }
}

private inline fun createSampleIcon(name: String, pathBuilder: PathBuilder.() -> Unit): ImageVector {
    return ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.White),
        fillAlpha = 1f,
        stroke = null,
        strokeAlpha = 1f,
        strokeLineWidth = 1f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Bevel,
        strokeLineMiter = 1f,
        pathFillType = DefaultFillType,
        pathBuilder = pathBuilder
    ).build()
}

@Preview
@Sampled
@Composable
fun SimpleTextFieldSample() {
    TextField(
        state = rememberTextFieldState(),
        lineLimits = TextFieldLineLimits.SingleLine,
        label = { Text("Label") },
    )
}

@Preview
@Sampled
@Composable
fun SimpleOutlinedTextFieldSample() {
    OutlinedTextField(
        state = rememberTextFieldState(),
        lineLimits = TextFieldLineLimits.SingleLine,
        label = { Text("Label") },
    )
}

@Preview
@Sampled
@Composable
fun TextFieldWithTransformations() {
    TextField(
        state = rememberTextFieldState(),
        lineLimits = TextFieldLineLimits.SingleLine,
        label = { Text("Phone number") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        // Input transformation to limit user input to 10 digits
        inputTransformation =
            InputTransformation.maxLengthTrim(10).then {
                if (!this.asCharSequence().isDigitsOnly()) {
                    revertAllChanges()
                }
            },
        outputTransformation = {
            // Output transformation to format as a phone number: (XXX) XXX-XXXX
            if (length > 0) insert(0, "(")
            if (length > 4) insert(4, ") ")
            if (length > 9) insert(9, "-")
        },
    )
}

@Preview
@Sampled
@Composable
fun TextFieldWithIcons() {
    val state = rememberTextFieldState()
    TextField(
        state = state,
        lineLimits = TextFieldLineLimits.SingleLine,
        label = { Text("Label") },
        leadingIcon = { Icon(SampleIcons.Filled.Favorite, contentDescription = null) },
        trailingIcon = {
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = {
                    PlainTooltip(
                        modifier =
                            Modifier.semantics {
                                // TODO(b/496338253): Remove this modifier once bug where tooltip
                                // text is not announced by a11y screen readers is resolved.
                                liveRegion = LiveRegionMode.Assertive
                                paneTitle = "Clear text"
                            }
                    ) {
                        Text("Clear text")
                    }
                },
                state = rememberTooltipState(),
            ) {
                IconButton(onClick = { state.clearText() }) {
                    Icon(SampleIcons.Filled.Clear, contentDescription = "Clear text")
                }
            }
        },
    )
}

@Preview
@Sampled
@Composable
fun TextFieldWithPlaceholder() {
    var alwaysMinimizeLabel by remember { mutableStateOf(false) }
    Column {
        Row {
            Checkbox(checked = alwaysMinimizeLabel, onCheckedChange = { alwaysMinimizeLabel = it })
            Text("Show placeholder even when unfocused")
        }
        Spacer(Modifier.height(16.dp))
        TextField(
            state = rememberTextFieldState(),
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text("Email") },
            labelPosition = TextFieldLabelPosition.Inside(isAlwaysMinimized = alwaysMinimizeLabel),
            placeholder = { Text("example@gmail.com") },
        )
    }
}

@Preview
@Sampled
@Composable
fun TextFieldWithPrefixAndSuffix() {
    var alwaysMinimizeLabel by remember { mutableStateOf(false) }
    Column {
        Row {
            Checkbox(checked = alwaysMinimizeLabel, onCheckedChange = { alwaysMinimizeLabel = it })
            Text("Show placeholder even when unfocused")
        }
        Spacer(Modifier.height(16.dp))
        TextField(
            state = rememberTextFieldState(),
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text("Label") },
            labelPosition = TextFieldLabelPosition.Inside(isAlwaysMinimized = alwaysMinimizeLabel),
            prefix = { Text("www.") },
            suffix = { Text(".com") },
            placeholder = { Text("google") },
        )
    }
}

@Preview
@Sampled
@Composable
fun TextFieldWithErrorState() {
    val errorMessage = "Text input too long"
    val state = rememberTextFieldState()
    var isError by rememberSaveable { mutableStateOf(false) }
    val charLimit = 10
    fun validate(text: CharSequence) {
        isError = text.length > charLimit
    }
    LaunchedEffect(Unit) {
        // Run validation whenever text value changes
        snapshotFlow { state.text }.collect { validate(it) }
    }
    TextField(
        state = state,
        lineLimits = TextFieldLineLimits.SingleLine,
        label = { Text(if (isError) "Username*" else "Username") },
        supportingText = {
            Row {
                Text(if (isError) errorMessage else "", Modifier.weight(1f).clearAndSetSemantics {})
                Text("Limit: ${state.text.length}/$charLimit")
            }
        },
        isError = isError,
        onKeyboardAction = { validate(state.text) },
        modifier =
            Modifier.semantics {
                maxTextLength = charLimit
                // Provide localized description of the error
                if (isError) error(errorMessage)
            },
    )
}

@Preview
@Sampled
@Composable
fun TextFieldWithSupportingText() {
    TextField(
        state = rememberTextFieldState(),
        lineLimits = TextFieldLineLimits.SingleLine,
        label = { Text("Label") },
        supportingText = {
            Text("Supporting text that is long and perhaps goes onto another line.")
        },
    )
}

@Preview
@Sampled
@Composable
fun PasswordTextField() {
    var passwordHidden by rememberSaveable { mutableStateOf(true) }
    SecureTextField(
        state = rememberTextFieldState(),
        label = { Text("Enter password") },
        keyboardOptions =
            KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType =
                    if (passwordHidden) KeyboardType.Password else KeyboardType.PasswordVisible,
            ),
        textObfuscationMode =
            if (passwordHidden) TextObfuscationMode.System else TextObfuscationMode.Visible,
        trailingIcon = {
            // Provide localized description for accessibility services
            val description = if (passwordHidden) "Show password" else "Hide password"
            TooltipBox(
                positionProvider =
                    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = {
                    PlainTooltip(
                        modifier =
                            Modifier.semantics {
                                // TODO(b/496338253): Remove this modifier once bug where tooltip
                                // text is not announced by a11y screen readers is resolved.
                                liveRegion = LiveRegionMode.Assertive
                                paneTitle = description
                            }
                    ) {
                        Text(description)
                    }
                },
                state = rememberTooltipState(),
            ) {
                IconButton(onClick = { passwordHidden = !passwordHidden }) {
                    val visibilityIcon =
                        if (passwordHidden) SampleIcons.Filled.Visibility else SampleIcons.Filled.VisibilityOff
                    Icon(imageVector = visibilityIcon, contentDescription = description)
                }
            }
        },
    )
}

@Preview
@Sampled
@Composable
fun TextFieldWithInitialValueAndSelection() {
    val state = rememberTextFieldState("Initial text", TextRange(0, 12))
    TextField(state = state, lineLimits = TextFieldLineLimits.SingleLine, label = { Text("Label") })
}

@Preview
@Sampled
@Composable
fun OutlinedTextFieldWithInitialValueAndSelection() {
    val state = rememberTextFieldState("Initial text", TextRange(0, 12))
    OutlinedTextField(
        state = state,
        lineLimits = TextFieldLineLimits.SingleLine,
        label = { Text("Label") },
    )
}

@Preview
@Sampled
@Composable
fun DenseTextFieldContentPadding() {
    TextField(
        state = rememberTextFieldState(),
        lineLimits = TextFieldLineLimits.SingleLine,
        label = { Text("Label") },
        // Need to set a min height using `heightIn` to override the default
        modifier = Modifier.heightIn(min = 48.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp, start = 12.dp, end = 12.dp),
    )
}

@Preview
@Sampled
@Composable
fun TextFieldWithHideKeyboardOnImeAction() {
    val keyboardController = LocalSoftwareKeyboardController.current
    TextField(
        state = rememberTextFieldState(),
        label = { Text("Label") },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        onKeyboardAction = { keyboardController?.hide() },
    )
}

@Composable
fun TextArea() {
    val state =
        rememberTextFieldState(
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor " +
                "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quisque " +
                "nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
                "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu " +
                "fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in " +
                "culpa qui officia deserunt mollit anim id est laborum."
        )
    TextField(state = state, modifier = Modifier.height(120.dp), label = { Text("Label") })
}

@Preview
@Sampled
@Composable
fun CustomTextFieldUsingDecorator() {
    val state = rememberTextFieldState()
    val interactionSource = remember { MutableInteractionSource() }
    val customColors =
        TextFieldDefaults.colors(
            focusedTextColor = Color.DarkGray,
            unfocusedTextColor = Color.Gray,
            focusedIndicatorColor = Color.Blue,
            cursorColor = Color.Green,
        )
    val enabled = true
    val isError = false
    val lineLimits = TextFieldLineLimits.SingleLine
    val textColor =
        LocalTextStyle.current.color.takeOrElse {
            customColors.textColor(
                enabled = enabled,
                isError = isError,
                focused = interactionSource.collectIsFocusedAsState().value,
            )
        }
    BasicTextField(
        state = state,
        modifier = Modifier,
        interactionSource = interactionSource,
        enabled = enabled,
        lineLimits = lineLimits,
        // Colors of non-decorator elements (such as text color or cursor color)
        // must be passed to BasicTextField
        textStyle = LocalTextStyle.current.merge(TextStyle(color = textColor)),
        cursorBrush = SolidColor(customColors.cursorColor),
        decorator =
            TextFieldDefaults.decorator(
                state = state,
                outputTransformation = null,
                lineLimits = lineLimits,
                enabled = enabled,
                isError = isError,
                interactionSource = interactionSource,
                container = {
                    TextFieldDefaults.Container(
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = customColors,
                        // Update indicator line thickness
                        unfocusedIndicatorLineThickness = 2.dp,
                        focusedIndicatorLineThickness = 4.dp,
                    )
                },
            ),
    )
}

@Preview
@Sampled
@Composable
fun CustomOutlinedTextFieldUsingDecorator() {
    val state = rememberTextFieldState()
    val interactionSource = remember { MutableInteractionSource() }
    val customColors =
        OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.DarkGray,
            unfocusedTextColor = Color.Gray,
            focusedBorderColor = Color.Blue,
            cursorColor = Color.Green,
        )
    val enabled = true
    val isError = false
    val lineLimits = TextFieldLineLimits.SingleLine
    val textColor =
        LocalTextStyle.current.color.takeOrElse {
            customColors.textColor(
                enabled = enabled,
                isError = isError,
                focused = interactionSource.collectIsFocusedAsState().value,
            )
        }
    BasicTextField(
        state = state,
        modifier = Modifier,
        interactionSource = interactionSource,
        enabled = enabled,
        lineLimits = lineLimits,
        // Colors of non-decorator elements (such as text color or cursor color)
        // must be passed to BasicTextField
        textStyle = LocalTextStyle.current.merge(TextStyle(color = textColor)),
        cursorBrush = SolidColor(customColors.cursorColor),
        decorator =
            OutlinedTextFieldDefaults.decorator(
                state = state,
                outputTransformation = null,
                lineLimits = lineLimits,
                enabled = enabled,
                isError = isError,
                interactionSource = interactionSource,
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = customColors,
                        // Update border thickness and shape
                        shape = RectangleShape,
                        unfocusedBorderThickness = 2.dp,
                        focusedBorderThickness = 4.dp,
                    )
                },
            ),
    )
}

@Preview
@Sampled
@Composable
fun CustomTextFieldBasedOnDecorationBox() {
    var text by remember { mutableStateOf("") }
    val interactionSource = remember { MutableInteractionSource() }
    val customColors =
        TextFieldDefaults.colors(
            focusedTextColor = Color.DarkGray,
            unfocusedTextColor = Color.Gray,
            focusedIndicatorColor = Color.Blue,
            cursorColor = Color.Green,
        )
    val enabled = true
    val isError = false
    val singleLine = true
    val textColor =
        LocalTextStyle.current.color.takeOrElse {
            customColors.textColor(
                enabled = enabled,
                isError = isError,
                focused = interactionSource.collectIsFocusedAsState().value,
            )
        }
    BasicTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier,
        interactionSource = interactionSource,
        enabled = enabled,
        singleLine = singleLine,
        // Colors of non-decoration-box elements (such as text color or cursor color)
        // must be passed to BasicTextField
        textStyle = LocalTextStyle.current.merge(TextStyle(color = textColor)),
        cursorBrush = SolidColor(customColors.cursorColor),
        decorationBox = { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = text,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = singleLine,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                colors = customColors,
            )
        },
    )
}
