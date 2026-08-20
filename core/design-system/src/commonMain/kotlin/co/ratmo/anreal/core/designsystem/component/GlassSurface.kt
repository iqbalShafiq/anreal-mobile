package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.designsystem.theme.LocalAnrealReduceTransparency
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

enum class GlassTone {
    UltraThin,
    Thin,
    Regular,
    Pane,
}

@Composable
fun HazeBackdrop(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.hazeSource(state = hazeState),
        content = content,
    )
}

/**
 * Marks arbitrary screen content as a backdrop for Anreal glass chrome without
 * leaking the Haze dependency into feature modules.
 */
@Composable
fun AnrealHazeSource(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val hazeState = LocalAnrealHazeState.current
    Box(
        modifier = if (hazeState != null) {
            modifier.hazeSource(state = hazeState)
        } else {
            modifier
        },
        content = content,
    )
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = LocalAnrealHazeState.current,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    tone: GlassTone = GlassTone.Thin,
    borderColor: Color? = null,
    fallbackColor: Color? = null,
    tintColor: Color? = null,
    emphasized: Boolean = false,
    error: Boolean = false,
    effectAlpha: Float = 1f,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val reduceTransparency = LocalAnrealReduceTransparency.current
    val style = when (tone) {
        GlassTone.UltraThin -> tintColor?.let { HazeMaterials.ultraThin(containerColor = it) } ?: HazeMaterials.ultraThin()
        GlassTone.Thin -> tintColor?.let { HazeMaterials.thin(containerColor = it) } ?: HazeMaterials.thin()
        GlassTone.Regular -> tintColor?.let { HazeMaterials.regular(containerColor = it) } ?: HazeMaterials.regular()
        GlassTone.Pane -> tintColor?.let { HazeMaterials.thin(containerColor = it) } ?: HazeMaterials.thin()
    }
    val border = when {
        error -> scheme.error
        emphasized -> scheme.primary.copy(alpha = 0.38f)
        borderColor != null -> borderColor
        else -> scheme.outlineVariant.copy(alpha = 0.45f)
    }
    val clampedAlpha = effectAlpha.coerceIn(0f, 1f)
    val useHaze = hazeState != null && !reduceTransparency && clampedAlpha > 0f
    val frost = if (useHaze) {
        Modifier.hazeEffect(state = hazeState, style = style) {
            alpha = clampedAlpha
        }
    } else {
        Modifier
    }
    val fallback = fallbackColor ?: scheme.surfaceContainer
    Surface(
        modifier = modifier
            .clip(shape)
            .then(frost)
            .border(
                width = 1.dp,
                color = border.copy(alpha = border.alpha * clampedAlpha),
                shape = shape,
            ),
        shape = shape,
        // HazeMaterials already provides tint and noise. Drawing another
        // translucent Surface tint here makes the material look opaque.
        color = if (useHaze) {
            Color.Transparent
        } else {
            fallback.copy(alpha = fallback.alpha * clampedAlpha)
        },
        contentColor = scheme.onSurface,
        content = content,
    )
}

@AnrealPreviews
@Composable
private fun GlassSurfacePreview() {
    AnrealPreview {
        val hazeState = remember { HazeState() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            HazeBackdrop(
                hazeState = hazeState,
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                )
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .align(Alignment.TopEnd)
                        .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                )
            }
            GlassSurface(
                hazeState = hazeState,
                emphasized = true,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(AnrealSpacing.lg)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = "Frosted chrome",
                    modifier = Modifier.padding(AnrealSpacing.md),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
