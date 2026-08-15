package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
) {
    var focused by remember { mutableStateOf(false) }
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
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
                enabled = enabled,
                isError = error != null,
                placeholder = if (placeholder.isNotEmpty()) {
                    { Text(placeholder) }
                } else {
                    null
                },
                trailingIcon = trailingIcon,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = anrealFieldColors(),
            )
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

@Composable
internal fun anrealFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    errorBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    errorTextColor = MaterialTheme.colorScheme.onSurface,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
    errorCursorColor = MaterialTheme.colorScheme.error,
)

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
