package de.kjltowinkler.msapp.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kjltowinkler.msapp.theme.AppTheme
import de.kjltowinkler.msapp.theme.FigmaInputBorderDark
import de.kjltowinkler.msapp.theme.FigmaInputBackgroundDark
import de.kjltowinkler.msapp.theme.FigmaPlaceholderDark
import merianschuleseligenstadtapp.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Standardfarben für alle Outlined-Textfelder der Merianschule-App gemäß Figma-Design.
 */
@Composable
fun defaultMerianOutlinedTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = FigmaInputBackgroundDark,
    unfocusedContainerColor = FigmaInputBackgroundDark,
    disabledContainerColor = FigmaInputBackgroundDark,
    errorContainerColor = FigmaInputBackgroundDark,
    focusedBorderColor = MaterialTheme.colorScheme.outline,
    unfocusedBorderColor = FigmaInputBorderDark,
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedPlaceholderColor = FigmaPlaceholderDark,
    unfocusedPlaceholderColor = FigmaPlaceholderDark
)

/**
 * Standardfarben für gefüllte Textfelder der Merianschule-App.
 */
@Composable
fun defaultMerianTextFieldColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = FigmaInputBackgroundDark,
    unfocusedContainerColor = FigmaInputBackgroundDark,
    disabledContainerColor = FigmaInputBackgroundDark,
    errorContainerColor = FigmaInputBackgroundDark,
    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    unfocusedIndicatorColor = FigmaInputBorderDark,
    errorIndicatorColor = MaterialTheme.colorScheme.error,
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedPlaceholderColor = FigmaPlaceholderDark,
    unfocusedPlaceholderColor = FigmaPlaceholderDark
)

/**
 * Custom Outlined TextField basierend auf den AOSP Material 3 Samples:
 * - `CustomOutlinedTextFieldUsingDecorator` (Decorator + Container-Architektur)
 * - `SimpleOutlinedTextFieldSample` (Label, SingleLine Limits)
 * - `TextFieldWithPlaceholder` (Placeholder-Unterstützung)
 * - `TextFieldWithPrefixAndSuffix` (Prefix & Suffix Slots)
 * - `TextFieldWithErrorState` (Barrierefreie Error-Semantik)
 * - `TextFieldWithTransformations` (Input- & OutputTransformation)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerianOutlinedTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholderText: String? = null,
    placeholder: @Composable (() -> Unit)? = null,
    label: @Composable (TextFieldLabelScope.() -> Unit)? = null,
    labelPosition: TextFieldLabelPosition = TextFieldLabelPosition.Cutout(),
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = RoundedCornerShape(4.dp),
    unfocusedBorderThickness: Dp = 1.dp,
    focusedBorderThickness: Dp = 2.dp,
    colors: TextFieldColors = defaultMerianOutlinedTextFieldColors(),
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = Color.Unspecified
    )
) {
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val textColor = textStyle.color.takeOrElse {
        colors.textColor(
            enabled = enabled,
            isError = isError,
            focused = isFocused
        )
    }

    val semanticsModifier = if (isError && errorMessage != null) {
        modifier.semantics { error(errorMessage) }
    } else {
        modifier
    }

    val effectivePlaceholder: @Composable (() -> Unit)? = when {
        placeholder != null -> placeholder
        placeholderText != null -> {
            {
                Text(
                    text = placeholderText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = FigmaPlaceholderDark
                )
            }
        }
        else -> null
    }

    BasicTextField(
        state = state,
        modifier = semanticsModifier
            .fillMaxWidth()
            .height(56.dp),
        interactionSource = interactionSource,
        enabled = enabled,
        readOnly = readOnly,
        lineLimits = lineLimits,
        inputTransformation = inputTransformation,
        outputTransformation = outputTransformation,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        textStyle = textStyle.merge(TextStyle(color = textColor)),
        cursorBrush = SolidColor(colors.cursorColor(isError)),
        decorator = OutlinedTextFieldDefaults.decorator(
            state = state,
            outputTransformation = outputTransformation,
            lineLimits = lineLimits,
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            label = label,
            labelPosition = labelPosition,
            placeholder = effectivePlaceholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            colors = colors,
            container = {
                OutlinedTextFieldDefaults.Container(
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = colors,
                    shape = shape,
                    unfocusedBorderThickness = unfocusedBorderThickness,
                    focusedBorderThickness = focusedBorderThickness
                )
            }
        )
    )
}

/**
 * Custom Outlined Password Field basierend auf den AOSP Material 3 Samples:
 * - `PasswordTextField` (TooltipBox + PlainTooltip, LiveRegionMode.Assertive paneTitle,
 *   dynamische KeyboardType & TextObfuscationMode Umschaltung)
 * - `CustomOutlinedTextFieldUsingDecorator` (Pixelgenaues Container-Styling via Figma)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerianOutlinedPasswordField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholderText: String? = null,
    placeholder: @Composable (() -> Unit)? = null,
    label: @Composable (TextFieldLabelScope.() -> Unit)? = null,
    labelPosition: TextFieldLabelPosition = TextFieldLabelPosition.Cutout(),
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    inputTransformation: InputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        autoCorrectEnabled = false,
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done
    ),
    onKeyboardAction: KeyboardActionHandler? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = RoundedCornerShape(4.dp),
    unfocusedBorderThickness: Dp = 1.dp,
    focusedBorderThickness: Dp = 2.dp,
    colors: TextFieldColors = defaultMerianOutlinedTextFieldColors(),
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = Color.Unspecified
    ),
    showVisibilityToggle: Boolean = true,
    textObfuscationMode: TextObfuscationMode = TextObfuscationMode.RevealLastTyped
) {
    var passwordHidden by rememberSaveable { mutableStateOf(true) }
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val textColor = textStyle.color.takeOrElse {
        colors.textColor(
            enabled = enabled,
            isError = isError,
            focused = isFocused
        )
    }

    val effectiveKeyboardOptions = keyboardOptions.copy(
        keyboardType = if (passwordHidden) KeyboardType.Password else KeyboardType.PasswordVisible
    )

    val effectiveTrailingIcon: @Composable (() -> Unit)? = when {
        trailingIcon != null -> trailingIcon
        showVisibilityToggle -> {
            {
                val description = if (passwordHidden) "Passwort anzeigen" else "Passwort verbergen"
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = {
                        PlainTooltip(
                            modifier = Modifier.semantics {
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
                        Icon(
                            painter = painterResource(
                                if (passwordHidden) Res.drawable.ic_visibility
                                else Res.drawable.ic_visibility_off
                            ),
                            contentDescription = description,
                            tint = FigmaPlaceholderDark
                        )
                    }
                }
            }
        }
        else -> null
    }

    val effectivePlaceholder: @Composable (() -> Unit)? = when {
        placeholder != null -> placeholder
        placeholderText != null -> {
            {
                Text(
                    text = placeholderText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = FigmaPlaceholderDark
                )
            }
        }
        else -> null
    }

    val semanticsModifier = if (isError && errorMessage != null) {
        modifier.semantics { error(errorMessage) }
    } else {
        modifier
    }

    BasicSecureTextField(
        state = state,
        modifier = semanticsModifier
            .fillMaxWidth()
            .height(56.dp),
        interactionSource = interactionSource,
        enabled = enabled,
        inputTransformation = inputTransformation,
        keyboardOptions = effectiveKeyboardOptions,
        onKeyboardAction = onKeyboardAction,
        textObfuscationMode = if (passwordHidden) textObfuscationMode else TextObfuscationMode.Visible,
        textStyle = textStyle.merge(TextStyle(color = textColor)),
        cursorBrush = SolidColor(colors.cursorColor(isError)),
        decorator = OutlinedTextFieldDefaults.decorator(
            state = state,
            outputTransformation = null,
            lineLimits = TextFieldLineLimits.SingleLine,
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            label = label,
            labelPosition = labelPosition,
            placeholder = effectivePlaceholder,
            leadingIcon = leadingIcon,
            trailingIcon = effectiveTrailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            colors = colors,
            container = {
                OutlinedTextFieldDefaults.Container(
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = colors,
                    shape = shape,
                    unfocusedBorderThickness = unfocusedBorderThickness,
                    focusedBorderThickness = focusedBorderThickness
                )
            }
        )
    )
}

/**
 * Custom Filled TextField basierend auf AOSP `CustomTextFieldUsingDecorator`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerianTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholderText: String? = null,
    placeholder: @Composable (() -> Unit)? = null,
    label: @Composable (TextFieldLabelScope.() -> Unit)? = null,
    labelPosition: TextFieldLabelPosition = TextFieldLabelPosition.Inside(),
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
    unfocusedIndicatorLineThickness: Dp = 1.dp,
    focusedIndicatorLineThickness: Dp = 2.dp,
    colors: TextFieldColors = defaultMerianTextFieldColors(),
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = Color.Unspecified
    )
) {
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val textColor = textStyle.color.takeOrElse {
        colors.textColor(
            enabled = enabled,
            isError = isError,
            focused = isFocused
        )
    }

    val semanticsModifier = if (isError && errorMessage != null) {
        modifier.semantics { error(errorMessage) }
    } else {
        modifier
    }

    val effectivePlaceholder: @Composable (() -> Unit)? = when {
        placeholder != null -> placeholder
        placeholderText != null -> {
            {
                Text(
                    text = placeholderText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = FigmaPlaceholderDark
                )
            }
        }
        else -> null
    }

    BasicTextField(
        state = state,
        modifier = semanticsModifier
            .fillMaxWidth()
            .height(56.dp),
        interactionSource = interactionSource,
        enabled = enabled,
        readOnly = readOnly,
        lineLimits = lineLimits,
        inputTransformation = inputTransformation,
        outputTransformation = outputTransformation,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        textStyle = textStyle.merge(TextStyle(color = textColor)),
        cursorBrush = SolidColor(colors.cursorColor(isError)),
        decorator = TextFieldDefaults.decorator(
            state = state,
            outputTransformation = outputTransformation,
            lineLimits = lineLimits,
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            label = label,
            labelPosition = labelPosition,
            placeholder = effectivePlaceholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            colors = colors,
            container = {
                TextFieldDefaults.Container(
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = colors,
                    shape = shape,
                    unfocusedIndicatorLineThickness = unfocusedIndicatorLineThickness,
                    focusedIndicatorLineThickness = focusedIndicatorLineThickness
                )
            }
        )
    )
}

@Preview
@Composable
fun MerianOutlinedTextFieldPreview() {
    val state = rememberTextFieldState()
    AppTheme(darkTheme = true) {
        MerianOutlinedTextField(
            state = state,
            placeholderText = "Nutzername oder Schüler-ID"
        )
    }
}

@Preview
@Composable
fun MerianOutlinedPasswordFieldPreview() {
    val state = rememberTextFieldState()
    AppTheme(darkTheme = true) {
        MerianOutlinedPasswordField(
            state = state,
            placeholderText = "Einmalpasswort"
        )
    }
}
