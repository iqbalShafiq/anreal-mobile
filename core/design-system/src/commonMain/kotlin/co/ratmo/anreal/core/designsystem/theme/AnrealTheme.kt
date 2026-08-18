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
    content: @Composable () -> Unit,
) {
    val darkTheme = settings.resolveDark(isSystemInDarkTheme())
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
    )

    CompositionLocalProvider(
        LocalAnrealReduceMotion provides (reduceMotion || rememberReduceMotion()),
        LocalAnrealReduceTransparency provides (reduceTransparency || rememberReduceTransparency()),
    ) {
        MaterialExpressiveTheme(
            colorScheme = dynamicScheme ?: brandScheme,
            motionScheme = MotionScheme.standard(),
            content = content,
        )
    }
}
