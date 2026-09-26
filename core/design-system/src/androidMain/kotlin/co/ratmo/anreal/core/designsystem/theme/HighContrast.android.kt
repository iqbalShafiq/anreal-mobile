package co.ratmo.anreal.core.designsystem.theme

import android.app.UiModeManager
import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Settings.Secure key; not exposed in the public SDK 37 stubs. */
private const val HighTextContrastEnabledKey = "high_text_contrast_enabled"

@Composable
actual fun rememberPlatformContrast(): Float {
    val context = LocalContext.current
    val uiModeManager = remember(context) { context.getSystemService(UiModeManager::class.java) }
    var contrast by remember(context) {
        mutableFloatStateOf(
            platformContrastValue(
                sdkInt = Build.VERSION.SDK_INT,
                uiModeContrast = uiModeManager?.contrastOrNull(),
                legacyEnabled = readLegacyHighTextContrast(context),
            ),
        )
    }
    DisposableEffect(context, uiModeManager) {
        if (Build.VERSION.SDK_INT >= PlatformContrastApiLevel && uiModeManager != null) {
            val listener = UiModeManager.ContrastChangeListener { value -> contrast = value }
            uiModeManager.addContrastChangeListener(context.mainExecutor, listener)
            onDispose { uiModeManager.removeContrastChangeListener(listener) }
        } else {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    contrast = platformContrastValue(
                        sdkInt = Build.VERSION.SDK_INT,
                        uiModeContrast = null,
                        legacyEnabled = readLegacyHighTextContrast(context),
                    )
                }
            }
            context.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(HighTextContrastEnabledKey),
                false,
                observer,
            )
            onDispose { context.contentResolver.unregisterContentObserver(observer) }
        }
    }
    return contrast
}

private fun UiModeManager.contrastOrNull(): Float? =
    if (Build.VERSION.SDK_INT >= PlatformContrastApiLevel) contrast else null

private fun readLegacyHighTextContrast(context: Context): Boolean =
    Settings.Secure.getInt(context.contentResolver, HighTextContrastEnabledKey, 0) == 1
