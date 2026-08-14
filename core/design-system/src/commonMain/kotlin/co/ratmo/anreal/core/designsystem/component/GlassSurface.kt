package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

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

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassSurface(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.hazeEffect(state = hazeState, style = HazeMaterials.thin()),
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
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
