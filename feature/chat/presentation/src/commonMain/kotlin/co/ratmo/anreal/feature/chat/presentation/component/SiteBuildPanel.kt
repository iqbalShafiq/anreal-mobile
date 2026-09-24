package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.designsystem.theme.LocalAnrealReduceMotion
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.domain.SiteBuildPhase
import co.ratmo.anreal.feature.chat.domain.SiteBuildState
import co.ratmo.anreal.feature.chat.domain.SiteVersionEntry
import co.ratmo.anreal.feature.chat.domain.stableSiteUrls
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Check
import com.composables.icons.materialsymbols.rounded.Expand_more
import com.composables.icons.materialsymbols.rounded.History
import com.composables.icons.materialsymbols.rounded.Refresh
import com.composables.icons.materialsymbols.rounded.Visibility

/**
 * Renders a WebView preview of a built site. Android hosts a real WebView with
 * JavaScript enabled, no auth headers, and no cookie/file access. iOS is a TODO stub.
 */
internal expect @Composable fun SitePreviewWebView(url: String, modifier: Modifier = Modifier)

/**
 * Joins a relative site route against the app base URL. Kept local (instead of reusing
 * core:data `constructRoute`) because presentation modules must not depend on core:data.
 */
internal fun resolveSiteUrl(baseUrl: String, route: String): String {
    if (route.startsWith("http://") || route.startsWith("https://")) return route
    val base = baseUrl.trimEnd('/')
    if (base.isEmpty()) return route
    return base + if (route.startsWith("/")) route else "/$route"
}

private val BUILD_STEPS = listOf(
    SiteBuildPhase.Starting to AnrealCopy.SITE_STEP_STARTING,
    SiteBuildPhase.Planning to AnrealCopy.SITE_STEP_PLANNING,
    SiteBuildPhase.Building to AnrealCopy.SITE_STEP_BUILDING,
    SiteBuildPhase.Bundling to AnrealCopy.SITE_STEP_BUNDLING,
    SiteBuildPhase.Preview to AnrealCopy.SITE_STEP_PREVIEW,
    SiteBuildPhase.Ready to AnrealCopy.SITE_STEP_READY,
)

@Composable
internal fun SiteBuildPanel(
    build: SiteBuildState?,
    versions: List<SiteVersionEntry> = emptyList(),
    baseUrl: String,
    onRetry: (siteId: String) -> Unit = {},
    onRollback: (siteId: String, version: Int) -> Unit = { _, _ -> },
    onDownloadZip: (siteId: String, version: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    if (build == null) return
    var expanded by remember(build.siteId, build.version) { mutableStateOf(true) }
    var previewOpen by remember(build.siteId, build.version) { mutableStateOf(false) }
    val failed = build.phase == SiteBuildPhase.Failed
    val ready = build.phase == SiteBuildPhase.Ready
    val siteVersions = versions.filter { it.siteId == build.siteId }
    val stableVersion = siteVersions.firstOrNull { it.stable }?.version
    var selectedVersion by remember(build.siteId, siteVersions) {
        mutableStateOf(stableVersion ?: siteVersions.maxOfOrNull { it.version } ?: build.version)
    }
    val selectedEntry = siteVersions.firstOrNull { it.version == selectedVersion }
    val canRollback = ready && selectedEntry != null && selectedEntry.ready && !selectedEntry.stable

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (failed) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (failed) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = AnrealSpacing.md,
                vertical = AnrealSpacing.sm,
            ),
            verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
            ) {
                Text(
                    text = "v${build.version} · ${build.message}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.heightIn(min = AnrealSpacing.touch),
                ) {
                    Text(
                        text = AnrealCopy.get(
                            if (expanded) AnrealCopy.SITE_HIDE else AnrealCopy.SITE_SHOW,
                        ),
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm)) {
                    BuildSteps(build = build)
                    if (ready) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = { previewOpen = true },
                                modifier = Modifier.heightIn(min = AnrealSpacing.touch),
                            ) {
                                Icon(
                                    imageVector = MaterialSymbols.Rounded.Visibility,
                                    contentDescription = null,
                                )
                                Text(
                                    text = AnrealCopy.get(AnrealCopy.SITE_PREVIEW_ACTION),
                                    modifier = Modifier.padding(start = AnrealSpacing.xs),
                                )
                            }
                            OutlinedButton(
                                onClick = { onDownloadZip(build.siteId, build.version) },
                                modifier = Modifier.heightIn(min = AnrealSpacing.touch),
                            ) {
                                Text(AnrealCopy.get(AnrealCopy.SITE_DOWNLOAD_ACTION))
                            }
                        }
                    }
                    if (failed) {
                        Button(
                            onClick = { onRetry(build.siteId) },
                            modifier = Modifier.heightIn(min = AnrealSpacing.touch),
                        ) {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Refresh,
                                contentDescription = null,
                            )
                            Text(
                                text = AnrealCopy.get(AnrealCopy.SITE_RETRY_ACTION),
                                modifier = Modifier.padding(start = AnrealSpacing.xs),
                            )
                        }
                    }
                    if (siteVersions.size > 1) {
                        VersionRow(
                            versions = siteVersions,
                            selectedVersion = selectedVersion,
                            onSelect = { selectedVersion = it },
                            canRollback = canRollback,
                            onRollback = { onRollback(build.siteId, selectedVersion) },
                        )
                    }
                }
            }
        }
    }
    if (previewOpen) {
        val (fallbackPreview, _) = stableSiteUrls(build.siteId, build.version)
        val previewRoute = build.previewUrl ?: fallbackPreview
        SitePreviewDialog(
            url = resolveSiteUrl(baseUrl, previewRoute),
            onDownload = { onDownloadZip(build.siteId, build.version) },
            onDismiss = { previewOpen = false },
        )
    }
}

@Composable
private fun BuildSteps(build: SiteBuildState) {
    val reduceMotion = LocalAnrealReduceMotion.current
    val currentIndex = when (build.phase) {
        SiteBuildPhase.Starting -> 0
        SiteBuildPhase.Planning -> 1
        SiteBuildPhase.Building -> 2
        SiteBuildPhase.Bundling -> 3
        SiteBuildPhase.Preview -> 4
        SiteBuildPhase.Ready -> BUILD_STEPS.size
        SiteBuildPhase.Failed -> BUILD_STEPS.indexOfFirst { it.first == SiteBuildPhase.Building }
            .takeIf { it >= 0 } ?: 2
    }
    Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
        BUILD_STEPS.forEachIndexed { index, (_, labelKey) ->
            val done = index < currentIndex
            val active = index == currentIndex && build.phase != SiteBuildPhase.Ready
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            ) {
                when {
                    done -> {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    active && !reduceMotion -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                }
                Text(
                    text = AnrealCopy.get(labelKey),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (done || active) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun VersionRow(
    versions: List<SiteVersionEntry>,
    selectedVersion: Int,
    onSelect: (Int) -> Unit,
    canRollback: Boolean,
    onRollback: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
    ) {
        Box {
            OutlinedButton(
                onClick = { menuOpen = true },
                modifier = Modifier.heightIn(min = AnrealSpacing.touch),
            ) {
                Text(text = versions.versionLabel(selectedVersion))
                Icon(
                    imageVector = MaterialSymbols.Rounded.Expand_more,
                    contentDescription = null,
                    modifier = Modifier.padding(start = AnrealSpacing.xs),
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                versions.sortedByDescending { it.version }.forEach { entry ->
                    DropdownMenuItem(
                        text = { Text(versions.versionLabel(entry.version)) },
                        onClick = {
                            onSelect(entry.version)
                            menuOpen = false
                        },
                    )
                }
            }
        }
        OutlinedButton(
            onClick = onRollback,
            enabled = canRollback,
            modifier = Modifier.heightIn(min = AnrealSpacing.touch),
        ) {
            Icon(
                imageVector = MaterialSymbols.Rounded.History,
                contentDescription = null,
            )
            Text(
                text = AnrealCopy.get(AnrealCopy.SITE_ROLLBACK),
                modifier = Modifier.padding(start = AnrealSpacing.xs),
            )
        }
    }
}

private fun List<SiteVersionEntry>.versionLabel(version: Int): String {
    val entry = firstOrNull { it.version == version }
    return when {
        entry == null -> UiText.StringResource(AnrealCopy.SITE_VERSION_PLAIN, listOf(version.toString())).asString()
        entry.stable -> UiText.StringResource(AnrealCopy.SITE_VERSION_STABLE, listOf(version.toString())).asString()
        entry.failed -> UiText.StringResource(AnrealCopy.SITE_VERSION_FAILED, listOf(version.toString())).asString()
        else -> UiText.StringResource(AnrealCopy.SITE_VERSION_PLAIN, listOf(version.toString())).asString()
    }
}

@Composable
private fun SitePreviewDialog(
    url: String,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AnrealCopy.get(AnrealCopy.SITE_PREVIEW_TITLE)) },
        text = {
            SitePreviewWebView(
                url = url,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
            )
        },
        confirmButton = {
            TextButton(onClick = onDownload) {
                Text(AnrealCopy.get(AnrealCopy.SITE_DOWNLOAD_ACTION))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_CLOSE))
            }
        },
    )
}

internal fun siteBuildingState(): SiteBuildState = SiteBuildState(
    siteId = "s1",
    version = 2,
    phase = SiteBuildPhase.Building,
    message = "Membangun halaman.",
)

internal fun siteReadyState(): SiteBuildState = SiteBuildState(
    siteId = "s1",
    version = 3,
    phase = SiteBuildPhase.Ready,
    message = "Situs siap diunduh.",
    previewUrl = "/api/sites/s1/v3/preview/index.html",
    downloadUrl = "/api/sites/s1/v3/download",
)

internal fun siteFailedState(): SiteBuildState = SiteBuildState(
    siteId = "s1",
    version = 2,
    phase = SiteBuildPhase.Failed,
    message = "Build gagal.",
    downloadUrl = "/api/sites/s1/v2/download",
)

internal fun siteVersionEntries(): List<SiteVersionEntry> = listOf(
    SiteVersionEntry(
        siteId = "s1",
        version = 1,
        ready = false,
        failed = true,
        stable = false,
        previewUrl = null,
        downloadUrl = "/api/sites/s1/v1/download",
    ),
    SiteVersionEntry(
        siteId = "s1",
        version = 2,
        ready = true,
        failed = false,
        stable = true,
        previewUrl = "/api/sites/s1/v2/preview/index.html",
        downloadUrl = "/api/sites/s1/v2/download",
    ),
    SiteVersionEntry(
        siteId = "s1",
        version = 3,
        ready = true,
        failed = false,
        stable = false,
        previewUrl = "/api/sites/s1/v3/preview/index.html",
        downloadUrl = "/api/sites/s1/v3/download",
    ),
)

@AnrealPreviews
@Composable
private fun SitePanelBuildingPreview() {
    AnrealPreview {
        SiteBuildPanel(
            build = siteBuildingState(),
            versions = siteVersionEntries(),
            baseUrl = PREVIEW_BASE_URL,
        )
    }
}

@AnrealPreviews
@Composable
private fun SitePanelReadyPreview() {
    AnrealPreview {
        SiteBuildPanel(
            build = siteReadyState(),
            versions = siteVersionEntries(),
            baseUrl = PREVIEW_BASE_URL,
        )
    }
}

@AnrealPreviews
@Composable
private fun SitePanelFailedPreview() {
    AnrealPreview {
        SiteBuildPanel(
            build = siteFailedState(),
            versions = siteVersionEntries(),
            baseUrl = PREVIEW_BASE_URL,
        )
    }
}

@AnrealPreviews
@Composable
private fun SitePanelHiddenPreview() {
    AnrealPreview {
        SiteBuildPanel(build = null, baseUrl = PREVIEW_BASE_URL)
    }
}

private const val PREVIEW_BASE_URL = "http://127.0.0.1:3001"
