package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal val LocalFocusedImeAnchor = staticCompositionLocalOf<((Float?) -> Unit)?> { null }

@Stable
internal class ImeFocusShiftState {
    var focusedBottomInWindow by mutableStateOf<Float?>(null)
        private set

    fun report(bottomInWindow: Float?) {
        focusedBottomInWindow = bottomInWindow
    }
}

internal fun imeFocusShiftPx(
    focusedBottomInWindow: Float?,
    appliedShiftPx: Int,
    imeBottomPx: Int,
    windowHeightPx: Int,
    extraGapPx: Int,
): Int {
    if (focusedBottomInWindow == null || imeBottomPx <= 0) return 0
    val keyboardTop = windowHeightPx - imeBottomPx
    val unshiftedBottom = focusedBottomInWindow + appliedShiftPx
    return (unshiftedBottom + extraGapPx - keyboardTop).roundToInt().coerceAtLeast(0)
}

@Composable
internal fun rememberImeFocusShift(extraGap: Dp = 12.dp): Pair<ImeFocusShiftState, Int> {
    val density = LocalDensity.current
    val windowHeightPx = LocalWindowInfo.current.containerSize.height
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val extraGapPx = with(density) { extraGap.roundToPx() }
    val state = remember { ImeFocusShiftState() }
    var appliedShiftPx by remember { mutableIntStateOf(0) }
    val targetShiftPx = imeFocusShiftPx(
        focusedBottomInWindow = state.focusedBottomInWindow,
        appliedShiftPx = appliedShiftPx,
        imeBottomPx = imeBottomPx,
        windowHeightPx = windowHeightPx,
        extraGapPx = extraGapPx,
    )
    SideEffect { appliedShiftPx = targetShiftPx }
    return state to targetShiftPx
}

@Composable
internal fun ProvideImeFocusAnchor(
    state: ImeFocusShiftState,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalFocusedImeAnchor provides state::report,
        content = content,
    )
}

internal fun Modifier.imeFocusShiftOffset(shiftPx: Int): Modifier {
    return offset { IntOffset(x = 0, y = -shiftPx) }
}
