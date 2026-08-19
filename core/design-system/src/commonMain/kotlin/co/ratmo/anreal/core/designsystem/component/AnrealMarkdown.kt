package co.ratmo.anreal.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

@Composable
fun AnrealMarkdown(
    content: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val textStyle = if (compact) {
        MaterialTheme.typography.bodySmall
    } else {
        MaterialTheme.typography.bodyLarge
    }
    Markdown(
        content = content,
        modifier = modifier,
        colors = markdownColor(
            text = if (compact) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ),
        typography = markdownTypography(
            h1 = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.headlineSmall,
            h2 = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
            h3 = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
            text = textStyle,
            paragraph = textStyle,
            code = MaterialTheme.typography.bodySmall,
        ),
    )
}

@AnrealPreviews
@Composable
private fun AnrealMarkdownHeadingsPreview() {
    AnrealPreview {
        AnrealMarkdown(
            """
            ## Revenue
            - Grew **12%**
            - Costs were *flat*
            """.trimIndent(),
        )
    }
}

@AnrealPreviews
@Composable
private fun AnrealMarkdownCodePreview() {
    AnrealPreview {
        AnrealMarkdown(
            """
            Use `sum()` on the table.

            ```
            select year, revenue from q3
            ```
            """.trimIndent(),
        )
    }
}

@AnrealPreviews
@Composable
private fun AnrealMarkdownTablePreview() {
    AnrealPreview {
        AnrealMarkdown(
            """
            | Year | Revenue |
            | --- | --- |
            | 2024 | 12 |
            | 2025 | 13.4 |
            """.trimIndent(),
        )
    }
}

@AnrealPreviews
@Composable
private fun AnrealMarkdownLinkPreview() {
    AnrealPreview {
        AnrealMarkdown("See the [Q3 report](https://example.com) for details.")
    }
}

@AnrealPreviews
@Composable
private fun AnrealMarkdownEmptyPreview() {
    AnrealPreview {
        AnrealMarkdown("")
    }
}
