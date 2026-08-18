package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape

@Composable
fun GlassTopBar(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val border = glassDrawerBorderColor()
    val fallback = glassDrawerFallbackColor()
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        tone = GlassTone.Thin,
        borderColor = border,
        fallbackColor = fallback,
    ) {
        Box(content = content)
    }
}
