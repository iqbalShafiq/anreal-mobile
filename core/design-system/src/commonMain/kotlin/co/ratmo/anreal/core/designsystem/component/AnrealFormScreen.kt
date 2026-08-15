package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Arrow_back

enum class AnrealAuthLayout {
    CenteredForm,
    Docked,
}

@Composable
fun AnrealAuthScaffold(
    modifier: Modifier = Modifier,
    layout: AnrealAuthLayout = AnrealAuthLayout.CenteredForm,
    navigationIcon: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnrealAtmosphere(modifier = modifier) {
        val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        val (imeShift, shiftPx) = rememberImeFocusShift()
        ProvideImeFocusAnchor(imeShift) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                        ),
                    )
                    .clipToBounds(),
            ) {
                val columnModifier = when (layout) {
                    AnrealAuthLayout.CenteredForm -> Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .then(
                            if (imeOpen) {
                                Modifier
                            } else {
                                Modifier.verticalScroll(rememberScrollState())
                            },
                        )
                    AnrealAuthLayout.Docked -> Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
                        )
                }
                val contentPadding = when (layout) {
                    AnrealAuthLayout.CenteredForm -> Modifier.padding(
                        horizontal = AnrealSpacing.screenCompact,
                        vertical = AnrealSpacing.xl,
                    )
                    AnrealAuthLayout.Docked -> Modifier.padding(vertical = AnrealSpacing.xl)
                }
                Column(
                    modifier = columnModifier
                        .imeFocusShiftOffset(shiftPx)
                        .then(contentPadding),
                    verticalArrangement = Arrangement.spacedBy(AnrealSpacing.md),
                    content = content,
                )
                if (navigationIcon != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(AnrealSpacing.xxs),
                    ) {
                        navigationIcon()
                    }
                }
            }
        }
    }
}

@Composable
fun AnrealFormScreen(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    wordmark: String = "Anreal",
    markDescription: String? = wordmark,
    onBack: (() -> Unit)? = null,
    backDescription: String? = null,
    footer: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnrealAuthScaffold(
        modifier = modifier,
        navigationIcon = if (onBack != null && backDescription != null) {
            {
                AnrealBackButton(
                    description = backDescription,
                    onClick = onBack,
                )
            }
        } else {
            null
        },
    ) {
        AnrealMark(
            wordmark = wordmark,
            contentDescription = markDescription,
        )
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

@Composable
fun AnrealBackButton(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(AnrealSpacing.touch),
    ) {
        Icon(
            imageVector = MaterialSymbols.Rounded.Arrow_back,
            contentDescription = description,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@AnrealPreviews
@Composable
private fun AnrealFormScreenWithBackPreview() {
    AnrealPreview {
        AnrealFormScreen(
            title = "Welcome back",
            subtitle = "Sign in with the email and password for your Anreal workspace.",
            onBack = {},
            backDescription = "Back",
            footer = {
                TextButton(onClick = {}) {
                    Text("New here? Create an account")
                }
            },
        ) {
            AnrealTextField(
                value = "you@company.com",
                onValueChange = {},
                label = "Email",
            )
            AnrealPrimaryButton(label = "Continue", onClick = {})
        }
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
