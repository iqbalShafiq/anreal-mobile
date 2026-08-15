package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

internal val LocalAnrealHazeState = staticCompositionLocalOf<HazeState?> { null }

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
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .background(surface)
            .drawBehind {
                val width = size.width
                val height = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(width * 0.52f, height * 0.38f),
                        radius = size.maxDimension * 0.62f,
                    ),
                    radius = size.maxDimension * 0.62f,
                    center = Offset(width * 0.52f, height * 0.38f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tertiary.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(width * 0.18f, height * 0.72f),
                        radius = size.maxDimension * 0.42f,
                    ),
                    radius = size.maxDimension * 0.42f,
                    center = Offset(width * 0.18f, height * 0.72f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(width * 0.88f, height * 0.12f),
                        radius = size.maxDimension * 0.34f,
                    ),
                    radius = size.maxDimension * 0.34f,
                    center = Offset(width * 0.88f, height * 0.12f),
                )
            },
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
