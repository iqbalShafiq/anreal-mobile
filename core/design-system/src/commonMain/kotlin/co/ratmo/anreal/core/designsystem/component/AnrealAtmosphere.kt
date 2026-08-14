package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
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
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    surface,
                    surface,
                ),
            ),
        ),
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-48).dp)
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.18f)),
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-36).dp, y = 28.dp)
                .clip(CircleShape)
                .background(tertiary.copy(alpha = 0.12f)),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(surface.copy(alpha = 0.55f)),
        )
    }
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
