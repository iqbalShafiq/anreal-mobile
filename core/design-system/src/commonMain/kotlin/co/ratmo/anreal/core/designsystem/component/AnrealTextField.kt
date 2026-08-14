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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
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
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                errorContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        )
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
        AnrealTextField(
            value = "",
            onValueChange = {},
            label = "Email",
            placeholder = "you@company.com",
            modifier = Modifier.padding(AnrealSpacing.md),
        )
    }
}

@AnrealPreviews
@Composable
private fun AnrealTextFieldFilledPreview() {
    AnrealPreview {
        AnrealTextField(
            value = "you@company.com",
            onValueChange = {},
            label = "Email",
            placeholder = "you@company.com",
            modifier = Modifier.padding(AnrealSpacing.md),
        )
    }
}

@AnrealPreviews
@Composable
private fun AnrealTextFieldErrorPreview() {
    AnrealPreview {
        AnrealTextField(
            value = "nope",
            onValueChange = {},
            label = "Email",
            error = "Enter a valid email address.",
            modifier = Modifier.padding(AnrealSpacing.md),
        )
    }
}

@AnrealPreviews
@Composable
private fun AnrealTextFieldDisabledPreview() {
    AnrealPreview {
        AnrealTextField(
            value = "you@company.com",
            onValueChange = {},
            label = "Email",
            enabled = false,
            modifier = Modifier.padding(AnrealSpacing.md),
        )
    }
}
