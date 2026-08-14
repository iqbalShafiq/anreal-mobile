package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing

@Composable
fun AnrealFormScreen(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    footer: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AnrealSpacing.screenCompact, vertical = AnrealSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.md),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
        footer?.invoke()
    }
}

@AnrealPreviews
@Composable
private fun AnrealFormScreenIdlePreview() {
    AnrealPreview {
        AnrealFormScreen(
            title = "Sign in",
            subtitle = "Use the email and password for your Anreal workspace.",
            footer = {
                TextButton(onClick = {}) {
                    Text("New here? Create an account")
                }
            },
        ) {
            AnrealTextField(
                value = "",
                onValueChange = {},
                label = "Email",
                placeholder = "you@company.com",
            )
            AnrealPasswordField(
                value = "",
                onValueChange = {},
                label = "Password",
                placeholder = "Your password",
            )
            AnrealPrimaryButton(label = "Continue", onClick = {})
        }
    }
}

@AnrealPreviews
@Composable
private fun AnrealFormScreenSubmittingPreview() {
    AnrealPreview {
        AnrealFormScreen(
            title = "Sign in",
            subtitle = "Use the email and password for your Anreal workspace.",
        ) {
            AnrealTextField(
                value = "you@company.com",
                onValueChange = {},
                label = "Email",
                enabled = false,
            )
            AnrealPasswordField(
                value = "password1",
                onValueChange = {},
                label = "Password",
                enabled = false,
            )
            AnrealPrimaryButton(
                label = "Continue",
                onClick = {},
                loading = true,
                loadingLabel = "Signing in…",
            )
        }
    }
}
