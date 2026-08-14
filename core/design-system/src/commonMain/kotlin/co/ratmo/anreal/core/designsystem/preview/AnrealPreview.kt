package co.ratmo.anreal.core.designsystem.preview

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import co.ratmo.anreal.core.designsystem.theme.AnrealTheme
import co.ratmo.anreal.core.designsystem.theme.ThemeMode
import co.ratmo.anreal.core.designsystem.theme.ThemeSettings

/** Night-mode bit used by `@Preview(uiMode)`. Same value as `Configuration.UI_MODE_NIGHT_YES`. */
const val PreviewNightUiMode: Int = 0x20

@Preview(name = "Light", showBackground = true, group = "Anreal")
@Preview(name = "Dark", showBackground = true, group = "Anreal", uiMode = PreviewNightUiMode)
annotation class AnrealPreviews

@Composable
fun AnrealPreview(
    dark: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val isDark = dark ?: isSystemInDarkTheme()
    AnrealTheme(
        settings = ThemeSettings(
            mode = if (isDark) ThemeMode.Dark else ThemeMode.Light,
            dynamicColor = false,
        ),
        content = content,
    )
}
