package co.ratmo.anreal.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnrealTheme(
    settings: ThemeSettings = ThemeSettings(),
    reduceMotion: Boolean = false,
    reduceTransparency: Boolean = false,
    highContrast: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val darkTheme = settings.resolveDark(isSystemInDarkTheme())
    ApplySystemBars(darkTheme)
    val platformContrast = rememberPlatformContrast()
    val resolvedContrast = if (highContrast == null) {
        contrastLevelFor(platformContrast)
    } else {
        if (highContrast) 1f else 0f
    }
    val highContrastOn = highContrast ?: highContrastActive(platformContrast)
    val dynamicScheme = if (settings.dynamicColor) {
        platformDynamicColorScheme(darkTheme)
    } else {
        null
    }
    val brandScheme = rememberDynamicColorScheme(
        seedColor = Color(AnrealBrand.seedArgb),
        isDark = darkTheme,
        isAmoled = false,
        style = PaletteStyle.Expressive,
        contrastLevel = resolvedContrast.toDouble(),
    )

    CompositionLocalProvider(
        LocalAnrealReduceMotion provides (reduceMotion || rememberReduceMotion()),
        LocalAnrealReduceTransparency provides (reduceTransparency || highContrastOn || rememberReduceTransparency()),
        LocalAnrealHighContrast provides highContrastOn,
    ) {
        MaterialExpressiveTheme(
            colorScheme = dynamicScheme ?: brandScheme,
            motionScheme = MotionScheme.standard(),
            typography = anrealTypography(),
            content = content,
        )
    }
}
