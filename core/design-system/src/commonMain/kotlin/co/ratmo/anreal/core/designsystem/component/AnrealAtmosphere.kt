package co.ratmo.anreal.core.designsystem.component

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealMotion
import co.ratmo.anreal.core.designsystem.theme.LocalAnrealReduceMotion
import co.ratmo.anreal.core.designsystem.theme.LocalAnrealReduceTransparency
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlin.math.PI
import kotlin.math.sin

internal val LocalAnrealHazeState = staticCompositionLocalOf<HazeState?> { null }

private const val TwoPi = (PI * 2.0).toFloat()
private const val AuroraPulseFloor = 0.62f

@Composable
fun AnrealAtmosphere(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    if (LocalAnrealHazeState.current != null) {
        Box(modifier = modifier.fillMaxSize(), content = content)
        return
    }
    val hazeState = remember { HazeState() }
    CompositionLocalProvider(LocalAnrealHazeState provides hazeState) {
        Box(modifier = modifier.fillMaxSize()) {
            AnrealAurora(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState),
            )
            content()
        }
    }
}

@Composable
fun AnrealAurora(
    modifier: Modifier = Modifier,
) {
    val hideAurora = LocalAnrealReduceMotion.current ||
        LocalAnrealReduceTransparency.current
    val surface = MaterialTheme.colorScheme.surface
    val canvas = modifier
        .background(surface)
        .semantics { hideFromAccessibility() }
    if (hideAurora) {
        Box(modifier = canvas)
        return
    }
    val animate = !LocalInspectionMode.current
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val seconds = rememberAuroraSeconds(animate)
    val periodMin = AnrealMotion.durationAuroraMin.inWholeSeconds.toFloat()
    val periodMid = AnrealMotion.durationAurora.inWholeSeconds.toFloat()
    val periodMax = AnrealMotion.durationAuroraMax.inWholeSeconds.toFloat()
    Box(
        modifier = canvas.drawBehind {
            val t = seconds.floatValue
            drawAuroraOrb(
                color = primary,
                restX = 0.52f,
                restY = 0.38f,
                radiusFraction = 0.68f,
                peakAlpha = 0.42f,
                driftX = 0.22f,
                driftY = 0.14f,
                periodX = periodMid,
                periodY = periodMax,
                periodPulse = periodMin,
                seconds = t,
            )
            drawAuroraOrb(
                color = tertiary,
                restX = 0.16f,
                restY = 0.74f,
                radiusFraction = 0.50f,
                peakAlpha = 0.28f,
                driftX = 0.18f,
                driftY = 0.16f,
                periodX = periodMax,
                periodY = periodMin,
                periodPulse = periodMid,
                seconds = t,
            )
            drawAuroraOrb(
                color = primary,
                restX = 0.90f,
                restY = 0.12f,
                radiusFraction = 0.40f,
                peakAlpha = 0.22f,
                driftX = 0.16f,
                driftY = 0.12f,
                periodX = periodMin,
                periodY = periodMid,
                periodPulse = periodMax,
                seconds = t,
            )
        },
    )
}

@Composable
private fun rememberAuroraSeconds(animate: Boolean): MutableFloatState {
    val seconds = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animate) {
        if (!animate) {
            seconds.floatValue = 0f
            return@LaunchedEffect
        }
        val startNs = withInfiniteAnimationFrameNanos { it }
        while (true) {
            withInfiniteAnimationFrameNanos { now ->
                seconds.floatValue = (now - startNs) / 1_000_000_000f
            }
        }
    }
    return seconds
}

internal fun auroraWave(seconds: Float, periodSeconds: Float): Float {
    if (periodSeconds <= 0f) return 0f
    return sin((seconds / periodSeconds) * TwoPi)
}

internal fun auroraPulse(seconds: Float, periodSeconds: Float): Float {
    val wave = 0.5f + 0.5f * auroraWave(seconds, periodSeconds)
    return AuroraPulseFloor + (1f - AuroraPulseFloor) * wave
}

private fun DrawScope.drawAuroraOrb(
    color: Color,
    restX: Float,
    restY: Float,
    radiusFraction: Float,
    peakAlpha: Float,
    driftX: Float,
    driftY: Float,
    periodX: Float,
    periodY: Float,
    periodPulse: Float,
    seconds: Float,
) {
    val center = Offset(
        x = size.width * (restX + driftX * auroraWave(seconds, periodX)),
        y = size.height * (restY + driftY * auroraWave(seconds, periodY)),
    )
    val radius = size.maxDimension * radiusFraction
    val alpha = peakAlpha * auroraPulse(seconds, periodPulse)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

@AnrealPreviews
@Composable
private fun AnrealAtmospherePreview() {
    AnrealPreview {
        AnrealAtmosphere {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
