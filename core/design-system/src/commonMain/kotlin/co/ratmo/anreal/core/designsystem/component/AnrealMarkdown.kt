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
) {
    Markdown(
        content = content,
        modifier = modifier,
        colors = markdownColor(
            text = MaterialTheme.colorScheme.onSurface,
        ),
        typography = markdownTypography(
            h1 = MaterialTheme.typography.headlineSmall,
            h2 = MaterialTheme.typography.titleLarge,
            h3 = MaterialTheme.typography.titleMedium,
            text = MaterialTheme.typography.bodyLarge,
            paragraph = MaterialTheme.typography.bodyLarge,
            code = MaterialTheme.typography.bodyMedium,
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
