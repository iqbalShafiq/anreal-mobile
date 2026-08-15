package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.em
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing

@Composable
fun AnrealTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    onFocusChange: (Boolean) -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val reportFocusedBottom = LocalFocusedImeAnchor.current
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        },
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.08.em),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            tone = GlassTone.Thin,
            emphasized = focused && error == null,
            error = error != null,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AnrealSpacing.field)
                    .padding(
                        start = AnrealSpacing.md,
                        end = if (trailingIcon != null) AnrealSpacing.xxs else AnrealSpacing.md,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            focused = focusState.isFocused
                            onFocusChange(focusState.isFocused)
                            if (!focusState.isFocused) {
                                reportFocusedBottom?.invoke(null)
                            }
                        }
                        .onGloballyPositioned { coordinates ->
                            if (focused) {
                                reportFocusedBottom?.invoke(
                                    coordinates.positionInWindow().y + coordinates.size.height,
                                )
                            }
                        },
                    enabled = enabled,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(
                        if (error != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    ),
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    singleLine = true,
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                    },
                )
                trailingIcon?.invoke()
            }
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@AnrealPreviews
@Composable
private fun AnrealTextFieldEmptyPreview() {
    AnrealPreview {
        AnrealAtmosphere {
            AnrealTextField(
                value = "",
                onValueChange = {},
                label = "Email",
                placeholder = "you@company.com",
                modifier = Modifier.padding(AnrealSpacing.md),
            )
        }
    }
}

@AnrealPreviews
@Composable
private fun AnrealTextFieldFilledPreview() {
    AnrealPreview {
        AnrealAtmosphere {
            AnrealTextField(
                value = "you@company.com",
                onValueChange = {},
                label = "Email",
                placeholder = "you@company.com",
                modifier = Modifier.padding(AnrealSpacing.md),
            )
        }
    }
}

@AnrealPreviews
@Composable
private fun AnrealTextFieldErrorPreview() {
    AnrealPreview {
        AnrealAtmosphere {
            AnrealTextField(
                value = "nope",
                onValueChange = {},
                label = "Email",
                error = "Enter a valid email address.",
                modifier = Modifier.padding(AnrealSpacing.md),
            )
        }
    }
}

@AnrealPreviews
@Composable
private fun AnrealTextFieldDisabledPreview() {
    AnrealPreview {
        AnrealAtmosphere {
            AnrealTextField(
                value = "you@company.com",
                onValueChange = {},
                label = "Email",
                enabled = false,
                modifier = Modifier.padding(AnrealSpacing.md),
            )
        }
    }
}
