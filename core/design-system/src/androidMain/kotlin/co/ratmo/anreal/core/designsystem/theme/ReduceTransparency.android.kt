package co.ratmo.anreal.core.designsystem.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberReduceTransparency(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Secure.getInt(
            context.contentResolver,
            ACCESSIBILITY_REDUCE_TRANSPARENCY,
            0,
        ) == 1
    }
}

private const val ACCESSIBILITY_REDUCE_TRANSPARENCY = "accessibility_reduce_transparency"
