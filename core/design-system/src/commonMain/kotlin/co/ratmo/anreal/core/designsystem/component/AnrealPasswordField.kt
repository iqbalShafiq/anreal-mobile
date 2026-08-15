package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Visibility
import com.composables.icons.materialsymbols.rounded.Visibility_off

@Composable
fun AnrealPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    enabled: Boolean = true,
    initiallyVisible: Boolean = false,
    showPasswordDescription: String = "Show password",
    hidePasswordDescription: String = "Hide password",
) {
    var visible by rememberSaveable { mutableStateOf(initiallyVisible) }
    AnrealTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        placeholder = placeholder,
        error = error,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(
                onClick = { visible = !visible },
                enabled = enabled,
                modifier = Modifier.size(AnrealSpacing.touch),
            ) {
                Icon(
                    imageVector = if (visible) {
                        MaterialSymbols.Rounded.Visibility_off
                    } else {
                        MaterialSymbols.Rounded.Visibility
                    },
                    contentDescription = if (visible) {
                        hidePasswordDescription
                    } else {
                        showPasswordDescription
                    },
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
            }
        },
    )
}

@AnrealPreviews
@Composable
private fun AnrealPasswordFieldHiddenPreview() {
    AnrealPreview {
        AnrealAtmosphere {
            AnrealPasswordField(
                value = "password1",
                onValueChange = {},
                label = "Password",
                placeholder = "Your password",
                modifier = Modifier.padding(AnrealSpacing.md),
            )
        }
    }
}

@AnrealPreviews
@Composable
private fun AnrealPasswordFieldVisiblePreview() {
    AnrealPreview {
        AnrealAtmosphere {
            AnrealPasswordField(
                value = "password1",
                onValueChange = {},
                label = "Password",
                initiallyVisible = true,
                modifier = Modifier.padding(AnrealSpacing.md),
            )
        }
    }
}

@AnrealPreviews
@Composable
private fun AnrealPasswordFieldErrorPreview() {
    AnrealPreview {
        AnrealAtmosphere {
            AnrealPasswordField(
                value = "123",
                onValueChange = {},
                label = "Password",
                error = "Password must be at least 8 characters.",
                modifier = Modifier.padding(AnrealSpacing.md),
            )
        }
    }
}

@AnrealPreviews
@Composable
private fun AnrealPasswordFieldDisabledPreview() {
    AnrealPreview {
        AnrealAtmosphere {
            AnrealPasswordField(
                value = "password1",
                onValueChange = {},
                label = "Password",
                enabled = false,
                modifier = Modifier.padding(AnrealSpacing.md),
            )
        }
    }
}
