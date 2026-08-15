package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealBrand
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.hazeEffect

private val DrawerCorner: Dp = 28.dp
internal val GlassDrawerWidth: Dp = 300.dp

@Composable
fun glassHighlightColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.surface.luminance() < 0.5f) {
        Color.White.copy(alpha = 0.055f)
    } else {
        scheme.onSurface.copy(alpha = 0.06f)
    }
}

fun glassDrawerShape(fromEnd: Boolean): Shape {
    val radius = DrawerCorner
    return if (fromEnd) {
        RoundedCornerShape(topStart = radius, bottomStart = radius)
    } else {
        RoundedCornerShape(topEnd = radius, bottomEnd = radius)
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassDrawer(
    modifier: Modifier = Modifier,
    fromEnd: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hazeState = LocalAnrealHazeState.current
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    val shape = glassDrawerShape(fromEnd)
    val tint = if (dark) {
        Color(AnrealBrand.canvasArgb).copy(alpha = 0.50f)
    } else {
        scheme.surface.copy(alpha = 0.56f)
    }
    val border = scheme.outlineVariant.copy(alpha = if (dark) 0.18f else 0.28f)
    val frost = if (hazeState != null) {
        Modifier.hazeEffect(state = hazeState, style = HazeMaterials.thin())
    } else {
        Modifier
    }
    val insetSides = WindowInsetsSides.Vertical + if (fromEnd) {
        WindowInsetsSides.End
    } else {
        WindowInsetsSides.Start
    }
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(GlassDrawerWidth)
            .then(frost)
            .border(width = 1.dp, color = border, shape = shape),
        shape = shape,
        color = if (hazeState != null) {
            tint
        } else if (dark) {
            Color(AnrealBrand.canvasArgb).copy(alpha = 0.82f)
        } else {
            scheme.surfaceContainer.copy(alpha = 0.94f)
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars.only(insetSides)),
                content = content,
            )
        },
    )
}

@AnrealPreviews
@Composable
private fun GlassDrawerStartPreview() {
    AnrealPreview {
        AnrealAtmosphere {
            GlassDrawer(fromEnd = false) {
                Text(
                    text = "Workspace",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@AnrealPreviews
@Composable
private fun GlassDrawerEndPreview() {
    AnrealPreview {
        AnrealAtmosphere {
            GlassDrawer(fromEnd = true) {
                Text(
                    text = "Documents",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
