package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    AnrealAtmosphere(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AnrealSpacing.screenCompact, vertical = AnrealSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(
                AnrealSpacing.md,
                Alignment.CenterVertically,
            ),
        ) {
            AuthBrandMark()
            Text(
                text = title,
                style = MaterialTheme.typography.titleLargeEmphasized,
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
}

@Composable
private fun AuthBrandMark() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(10.dp),
            color = glassHighlightColor(),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "A",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Text(
            text = "Anreal",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
