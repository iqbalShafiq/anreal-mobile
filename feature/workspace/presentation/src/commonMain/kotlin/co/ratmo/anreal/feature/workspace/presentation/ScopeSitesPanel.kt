package co.ratmo.anreal.feature.workspace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.component.AnrealEmpty
import co.ratmo.anreal.core.designsystem.component.AnrealError
import co.ratmo.anreal.core.designsystem.component.AnrealSkeletonCard
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Language
import com.composables.icons.materialsymbols.rounded.Visibility

/**
 * Renders a scope site preview. Android hosts a real WebView with
 * JavaScript enabled, no auth headers, and no cookie/file access. iOS is a TODO stub.
 */
internal expect @Composable fun ScopeSitePreviewWebView(url: String, modifier: Modifier = Modifier)

/**
 * Joins a relative site route against the app base URL. Kept local (instead of reusing
 * core:data `constructRoute` or chat's helper) because presentation modules must not
 * depend on core:data or on each other.
 */
internal fun resolveScopeSiteUrl(baseUrl: String, route: String): String {
    if (route.startsWith("http://") || route.startsWith("https://")) return route
    val base = baseUrl.trimEnd('/')
    if (base.isEmpty()) return route
    return base + if (route.startsWith("/")) route else "/$route"
}

@Composable
fun ScopeSitesPanel(
    sites: List<SiteUi>,
    isLoading: Boolean,
    loaded: Boolean,
    hasScope: Boolean,
    error: UiText?,
    baseUrl: String,
    previewSiteId: String?,
    onAction: (WorkspaceAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val previewSite = sites.firstOrNull { it.siteId == previewSiteId }
    if (!hasScope) {
        AnrealEmpty(
            title = AnrealCopy.get(AnrealCopy.LABEL_SITES),
            body = AnrealCopy.get(AnrealCopy.SITES_NO_SCOPE),
            icon = MaterialSymbols.Rounded.Language,
            modifier = modifier.fillMaxSize(),
        )
    } else if (isLoading && !loaded) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = WorkspaceListPadding,
            verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
        ) {
            items(4) { AnrealSkeletonCard() }
        }
    } else if (error != null && !loaded) {
        AnrealError(
            message = error.asString(),
            retryLabel = AnrealCopy.get(AnrealCopy.ACTION_RETRY),
            onRetry = { onAction(WorkspaceAction.Retry) },
            modifier = modifier.fillMaxSize(),
        )
    } else if (sites.isEmpty()) {
        AnrealEmpty(
            title = AnrealCopy.get(AnrealCopy.LABEL_SITES),
            body = AnrealCopy.get(AnrealCopy.SITES_EMPTY),
            icon = MaterialSymbols.Rounded.Language,
            modifier = modifier.fillMaxSize(),
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = WorkspaceListPadding,
            verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
        ) {
            items(sites, key = SiteUi::siteId) { site ->
                ScopeSiteCard(
                    site = site,
                    onPreview = { onAction(WorkspaceAction.OpenSitePreview(site.siteId)) },
                    onOpenOrigin = { onAction(WorkspaceAction.OnOpenSiteOrigin(site.siteId, site.sessionId)) },
                    onContinue = { onAction(WorkspaceAction.OnContinueSite(site.siteId, site.sessionId)) },
                )
            }
        }
    }
    if (previewSite?.previewUrl != null) {
        AlertDialog(
            onDismissRequest = { onAction(WorkspaceAction.CloseSitePreview) },
            title = { Text("v${previewSite.version}") },
            text = {
                ScopeSitePreviewWebView(
                    url = resolveScopeSiteUrl(baseUrl, previewSite.previewUrl),
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = { onAction(WorkspaceAction.CloseSitePreview) }) {
                    Text(AnrealCopy.get(AnrealCopy.ACTION_CLOSE))
                }
            },
        )
    }
}

@Composable
private fun ScopeSiteCard(
    site: SiteUi,
    onPreview: () -> Unit,
    onOpenOrigin: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColor = when (site.statusLabel) {
        "ready" -> MaterialTheme.colorScheme.primary
        "failed" -> MaterialTheme.colorScheme.error
        "running" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AnrealSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildAnnotatedString {
                            append("v${site.version} · ")
                            withStyle(SpanStyle(color = statusColor)) {
                                append(site.statusLabel)
                            }
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (site.stableVersion != null) {
                        Text(
                            text = "stable v${site.stableVersion}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (site.previewUrl != null) {
                    IconButton(onClick = onPreview) {
                        Icon(
                            MaterialSymbols.Rounded.Visibility,
                            contentDescription = AnrealCopy.get(AnrealCopy.SITE_PREVIEW_ACTION),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
            ) {
                TextButton(onClick = onOpenOrigin) {
                    Text(AnrealCopy.get(AnrealCopy.SITE_OPEN_CHAT))
                }
                FilledTonalButton(onClick = onContinue) {
                    Text(AnrealCopy.get(AnrealCopy.SITE_CONTINUE))
                }
            }
        }
    }
}

@AnrealPreviews
@Composable
private fun ScopeSitesPopulatedPreview() {
    AnrealPreview {
        ScopeSitesPanel(
            sites = listOf(
                SiteUi("s1", "abc", 3, 2, "ready", "/api/sites/s1/v3/preview/index.html", "/api/sites/s1/v3/download"),
                SiteUi("s2", "abc", 1, null, "failed", null, "/api/sites/s2/v1/download"),
                SiteUi("s3", "abc", 1, null, "running", null, "/api/sites/s3/v1/download"),
            ),
            isLoading = false,
            loaded = true,
            hasScope = true,
            error = null,
            baseUrl = "",
            previewSiteId = null,
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ScopeSitesLoadingPreview() {
    AnrealPreview {
        ScopeSitesPanel(
            sites = emptyList(),
            isLoading = true,
            loaded = false,
            hasScope = true,
            error = null,
            baseUrl = "",
            previewSiteId = null,
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ScopeSitesEmptyPreview() {
    AnrealPreview {
        ScopeSitesPanel(
            sites = emptyList(),
            isLoading = false,
            loaded = true,
            hasScope = true,
            error = null,
            baseUrl = "",
            previewSiteId = null,
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ScopeSitesNoScopePreview() {
    AnrealPreview {
        ScopeSitesPanel(
            sites = emptyList(),
            isLoading = false,
            loaded = false,
            hasScope = false,
            error = null,
            baseUrl = "",
            previewSiteId = null,
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ScopeSitesErrorPreview() {
    AnrealPreview {
        ScopeSitesPanel(
            sites = emptyList(),
            isLoading = false,
            loaded = false,
            hasScope = true,
            error = UiText.DynamicString("Session not found"),
            baseUrl = "",
            previewSiteId = null,
            onAction = {},
        )
    }
}
