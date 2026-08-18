package co.ratmo.anreal.feature.chat.presentation.account.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.component.AnrealAtmosphere
import co.ratmo.anreal.core.designsystem.component.AnrealSegmentedTabs
import co.ratmo.anreal.core.designsystem.component.GlassExtendedFloatingActionButton
import co.ratmo.anreal.core.designsystem.component.GlassSurface
import co.ratmo.anreal.core.designsystem.component.GlassTone
import co.ratmo.anreal.core.designsystem.component.GlassTopBar
import co.ratmo.anreal.core.designsystem.component.glassFaintTextColor
import co.ratmo.anreal.core.designsystem.component.glassMutedTextColor
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealMotion
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.designsystem.theme.LocalAnrealReduceMotion
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.core.domain.model.AppThemeMode
import co.ratmo.anreal.feature.chat.presentation.account.AccountSection
import co.ratmo.anreal.feature.chat.presentation.account.AccountState
import co.ratmo.anreal.feature.chat.presentation.account.AccountUsageUi
import co.ratmo.anreal.feature.chat.presentation.account.ProfileUi
import co.ratmo.anreal.feature.chat.presentation.account.ProjectProfileUi
import co.ratmo.anreal.feature.chat.presentation.account.UsageBreakdownUi
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Arrow_back
import com.composables.icons.materialsymbols.rounded.Auto_awesome
import com.composables.icons.materialsymbols.rounded.Bolt
import com.composables.icons.materialsymbols.rounded.Logout
import com.composables.icons.materialsymbols.rounded.Person

private val SettingsContentMaxWidth = 640.dp
private val LogoutDockMaxWidth = 480.dp
private val LogoutDockClearance = 104.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountSettingsLayout(
    state: AccountState,
    onBack: () -> Unit,
    onSelectSection: (AccountSection) -> Unit,
    onRetryUsage: () -> Unit = {},
    onRetryHealth: () -> Unit = {},
    onRetryPersonalization: () -> Unit = {},
    onRequestResetUserProfile: () -> Unit = {},
    onRequestResetProjectProfile: (String, String) -> Unit = { _, _ -> },
    onConfirmResetProfile: () -> Unit = {},
    onDismissResetProfile: () -> Unit = {},
    onThemeModeChange: (AppThemeMode) -> Unit = {},
    onToggleDynamicColor: () -> Unit = {},
    onToggleReduceMotion: () -> Unit = {},
    onToggleReduceTransparency: () -> Unit = {},
    onSignOut: () -> Unit,
) {
    AnrealAtmosphere {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GlassTopBar {
                    TopAppBar(
                        title = {
                            Text(
                                text = AnrealCopy.get(AnrealCopy.ACTION_SETTINGS),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onBack,
                                enabled = !state.isSigningOut,
                            ) {
                                Icon(
                                    imageVector = MaterialSymbols.Rounded.Arrow_back,
                                    contentDescription = AnrealCopy.get(AnrealCopy.CD_BACK),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                        ),
                    )
                }
            },
        ) { scaffoldPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding),
            ) {
                AnrealSegmentedTabs(
                    items = AccountSection.entries,
                    selected = state.section,
                    label = AccountSection::label,
                    enabled = !state.isSigningOut,
                    onSelect = onSelectSection,
                    modifier = Modifier
                        .widthIn(max = SettingsContentMaxWidth)
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                        .padding(
                            horizontal = AnrealSpacing.screenCompact,
                            vertical = AnrealSpacing.xs,
                        ),
                )
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    LazyColumn(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .widthIn(max = SettingsContentMaxWidth)
                            .fillMaxHeight()
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = AnrealSpacing.screenCompact,
                            top = AnrealSpacing.md,
                            end = AnrealSpacing.screenCompact,
                            bottom = LogoutDockClearance,
                        ),
                        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.lg),
                    ) {
                        item {
                            AnimatedAccountSection(
                                state = state,
                                onRetryUsage = onRetryUsage,
                                onRetryHealth = onRetryHealth,
                                onRetryPersonalization = onRetryPersonalization,
                                onRequestResetUserProfile = onRequestResetUserProfile,
                                onRequestResetProjectProfile = onRequestResetProjectProfile,
                                onThemeModeChange = onThemeModeChange,
                                onToggleDynamicColor = onToggleDynamicColor,
                                onToggleReduceMotion = onToggleReduceMotion,
                                onToggleReduceTransparency = onToggleReduceTransparency,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    GlassExtendedFloatingActionButton(
                        label = AnrealCopy.get(
                            if (state.isSigningOut) {
                                AnrealCopy.ACTION_LOGGING_OUT
                            } else {
                                AnrealCopy.ACTION_LOG_OUT
                            },
                        ),
                        icon = MaterialSymbols.Rounded.Logout,
                        onClick = onSignOut,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .widthIn(max = LogoutDockMaxWidth)
                            .navigationBarsPadding()
                            .padding(
                                horizontal = AnrealSpacing.screenCompact,
                                vertical = AnrealSpacing.sm,
                            ),
                        loading = state.isSigningOut,
                        destructive = true,
                    )
                }
            }
        }
    }
    state.resetTarget?.let {
        AlertDialog(
            onDismissRequest = onDismissResetProfile,
            title = { Text(AnrealCopy.get(AnrealCopy.DIALOG_RESET_PROFILE_TITLE)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
                    Text(AnrealCopy.get(AnrealCopy.DIALOG_RESET_PROFILE_BODY))
                    state.resetError?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmResetProfile, enabled = !state.isResettingProfile) {
                    if (state.isResettingProfile) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.size(AnrealSpacing.xs))
                    }
                    Text(
                        if (state.isResettingProfile) {
                            AnrealCopy.get(AnrealCopy.ACTION_RESETTING)
                        } else {
                            AnrealCopy.get(AnrealCopy.ACTION_RESET)
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissResetProfile, enabled = !state.isResettingProfile) {
                    Text(AnrealCopy.get(AnrealCopy.ACTION_CANCEL))
                }
            },
        )
    }
}

@Composable
private fun AnimatedAccountSection(
    state: AccountState,
    onRetryUsage: () -> Unit,
    onRetryHealth: () -> Unit,
    onRetryPersonalization: () -> Unit,
    onRequestResetUserProfile: () -> Unit,
    onRequestResetProjectProfile: (String, String) -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onToggleDynamicColor: () -> Unit,
    onToggleReduceMotion: () -> Unit,
    onToggleReduceTransparency: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalAnrealReduceMotion.current
    val offsetPx = with(LocalDensity.current) { AnrealSpacing.xs.roundToPx() }
    AnimatedContent(
        targetState = state.section,
        modifier = modifier,
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            val contentTransform = if (reduceMotion) {
                fadeIn(AnrealMotion.fadeSpec()) togetherWith fadeOut(AnrealMotion.fadeSpec())
            } else {
                (
                    slideInHorizontally(AnrealMotion.selectionSpec()) { direction * offsetPx } +
                        fadeIn(AnrealMotion.fadeSpec())
                    ) togetherWith (
                    slideOutHorizontally(AnrealMotion.selectionSpec()) { -direction * offsetPx } +
                        fadeOut(AnrealMotion.fadeSpec())
                    )
            }
            contentTransform.using(
                SizeTransform(clip = false) { _, _ -> snap() },
            )
        },
        contentKey = { it },
        label = "accountSection",
    ) { section ->
        when (section) {
            AccountSection.Account -> AccountDetails(
                state,
                onRetryHealth,
                onThemeModeChange,
                onToggleDynamicColor,
                onToggleReduceMotion,
                onToggleReduceTransparency,
            )
            AccountSection.Usage -> UsageSection(
                usage = state.usage,
                loading = state.isUsageLoading,
                error = state.usageError,
                onRetry = onRetryUsage,
            )
            AccountSection.Personalization -> PersonalizationSection(
                profile = state.userProfile,
                projects = state.projectProfiles,
                loading = state.isPersonalizationLoading,
                error = state.personalizationError,
                onRetry = onRetryPersonalization,
                onResetUser = onRequestResetUserProfile,
                onResetProject = onRequestResetProjectProfile,
            )
        }
    }
}

@Composable
private fun AccountDetails(
    state: AccountState,
    onRetryHealth: () -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onToggleDynamicColor: () -> Unit,
    onToggleReduceMotion: () -> Unit,
    onToggleReduceTransparency: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.lg),
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            tone = GlassTone.Pane,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AnrealSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs),
                ) {
                    Text(
                        text = state.resolvedName(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.resolvedEmail(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = glassMutedTextColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        SettingsSectionHeader(
            title = AnrealCopy.get(AnrealCopy.LABEL_ACCOUNT),
            body = AnrealCopy.get(AnrealCopy.ACCOUNT_SECTION_BODY),
        )

        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            tone = GlassTone.Regular,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                AccountValueRow(
                    label = AnrealCopy.get(AnrealCopy.LABEL_NAME),
                    value = state.resolvedName(),
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = AnrealSpacing.md),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                )
                AccountValueRow(
                    label = AnrealCopy.get(AnrealCopy.LABEL_EMAIL),
                    value = state.resolvedEmail(),
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = AnrealSpacing.md),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                )
                AccountValueRow(
                    label = AnrealCopy.get(AnrealCopy.LABEL_API_STATUS),
                    value = AnrealCopy.get(
                        when {
                            state.isHealthLoading -> AnrealCopy.STATUS_CHECKING
                            state.isApiHealthy == true -> AnrealCopy.STATUS_CONNECTED
                            else -> AnrealCopy.STATUS_UNAVAILABLE
                        },
                    ),
                    actionLabel = if (!state.isHealthLoading && state.isApiHealthy != true) {
                        AnrealCopy.get(AnrealCopy.ACTION_RETRY)
                    } else {
                        null
                    },
                    onAction = onRetryHealth,
                )
            }
        }

        SettingsSectionHeader(
            title = AnrealCopy.get(AnrealCopy.LABEL_APPEARANCE),
            body = AnrealCopy.get(AnrealCopy.APPEARANCE_SECTION_BODY),
        )
        GlassSurface(modifier = Modifier.fillMaxWidth(), tone = GlassTone.Regular) {
            Column(modifier = Modifier.fillMaxWidth().padding(AnrealSpacing.md)) {
                Text(
                    AnrealCopy.get(AnrealCopy.LABEL_THEME),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    AppThemeMode.entries.forEach { mode ->
                        TextButton(
                            onClick = { onThemeModeChange(mode) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                mode.label(),
                                color = if (mode == state.themeMode) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
                PreferenceSwitchRow(
                    AnrealCopy.get(AnrealCopy.LABEL_DYNAMIC_COLOR),
                    state.dynamicColor,
                    onToggleDynamicColor,
                )
                PreferenceSwitchRow(
                    AnrealCopy.get(AnrealCopy.LABEL_REDUCE_MOTION),
                    state.reduceMotion,
                    onToggleReduceMotion,
                )
                PreferenceSwitchRow(
                    AnrealCopy.get(AnrealCopy.LABEL_REDUCE_TRANSPARENCY),
                    state.reduceTransparency,
                    onToggleReduceTransparency,
                )
            }
        }
    }
}

@Composable
private fun SettingsStatus(
    icon: ImageVector,
    emptyMessage: String,
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        tone = GlassTone.Regular,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AnrealSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = glassMutedTextColor(),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Text(
                text = emptyMessage,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = glassFaintTextColor(),
            )
        }

    }
}

@Composable
private fun PreferenceSwitchRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = AnrealSpacing.touch),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

private fun AppThemeMode.label(): String = when (this) {
    AppThemeMode.System -> AnrealCopy.get(AnrealCopy.LABEL_THEME_SYSTEM)
    AppThemeMode.Light -> AnrealCopy.get(AnrealCopy.LABEL_THEME_LIGHT)
    AppThemeMode.Dark -> AnrealCopy.get(AnrealCopy.LABEL_THEME_DARK)
}

@Composable
private fun UsageSection(
    usage: AccountUsageUi?,
    loading: Boolean,
    error: UiText?,
    onRetry: () -> Unit,
) {
    SettingsSectionContainer(
        title = AnrealCopy.get(AnrealCopy.LABEL_USAGE),
        body = AnrealCopy.get(AnrealCopy.USAGE_SECTION_BODY),
    ) {
        when {
            loading && usage == null -> LoadingSettingsCard()
            error != null && usage == null -> ErrorSettingsCard(error, onRetry)
            usage == null -> SettingsStatus(
                icon = MaterialSymbols.Rounded.Bolt,
                emptyMessage = AnrealCopy.get(AnrealCopy.USAGE_EMPTY),
            )
            else -> {
                GlassSurface(modifier = Modifier.fillMaxWidth(), tone = GlassTone.Regular) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(AnrealSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
                    ) {
                        Text(AnrealCopy.get(AnrealCopy.LABEL_STORAGE), style = MaterialTheme.typography.titleMedium)
                        LinearProgressIndicator(
                            progress = { usage.storageFraction },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "${usage.storageUsed} / ${usage.storageMax}",
                            style = MaterialTheme.typography.bodySmall,
                            color = glassMutedTextColor(),
                        )
                    }
                }
                MetricGrid(usage)
                if (usage.models.isNotEmpty()) UsageBreakdownCard(
                    AnrealCopy.get(AnrealCopy.LABEL_BY_MODEL),
                    usage.models,
                )
                if (usage.reasoning.isNotEmpty()) UsageBreakdownCard(
                    AnrealCopy.get(AnrealCopy.LABEL_BY_REASONING),
                    usage.reasoning,
                )
                if (usage.models.isEmpty() && usage.requestCount == "0") {
                    Text(
                        AnrealCopy.get(AnrealCopy.USAGE_EMPTY),
                        style = MaterialTheme.typography.bodyMedium,
                        color = glassFaintTextColor(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricGrid(usage: AccountUsageUi) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), tone = GlassTone.Regular) {
        Column(modifier = Modifier.fillMaxWidth()) {
            listOf(
                AnrealCopy.get(AnrealCopy.LABEL_REQUESTS) to usage.requestCount,
                AnrealCopy.get(AnrealCopy.LABEL_TOTAL_TOKENS) to usage.totalTokens,
                AnrealCopy.get(AnrealCopy.LABEL_INPUT_TOKENS) to usage.inputTokens,
                AnrealCopy.get(AnrealCopy.LABEL_OUTPUT_TOKENS) to usage.outputTokens,
                AnrealCopy.get(AnrealCopy.LABEL_CACHED_TOKENS) to usage.cachedTokens,
            ).forEachIndexed { index, metric ->
                AccountValueRow(metric.first, metric.second)
                if (index < 4) HorizontalDivider(
                    modifier = Modifier.padding(horizontal = AnrealSpacing.md),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
private fun UsageBreakdownCard(title: String, rows: List<UsageBreakdownUi>) {
    Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        GlassSurface(modifier = Modifier.fillMaxWidth(), tone = GlassTone.Regular) {
            Column(modifier = Modifier.fillMaxWidth()) {
                rows.forEachIndexed { index, row ->
                    AccountValueRow(row.label, "${row.requests} requests · ${row.tokens} tokens")
                    if (index < rows.lastIndex) HorizontalDivider(
                        modifier = Modifier.padding(horizontal = AnrealSpacing.md),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalizationSection(
    profile: ProfileUi?,
    projects: List<ProjectProfileUi>,
    loading: Boolean,
    error: UiText?,
    onRetry: () -> Unit,
    onResetUser: () -> Unit,
    onResetProject: (String, String) -> Unit,
) {
    SettingsSectionContainer(
        title = AnrealCopy.get(AnrealCopy.LABEL_PERSONALIZATION),
        body = AnrealCopy.get(AnrealCopy.PERSONALIZATION_SECTION_BODY),
    ) {
        when {
            loading && profile == null && projects.isEmpty() -> LoadingSettingsCard()
            error != null && profile == null && projects.isEmpty() -> ErrorSettingsCard(error, onRetry)
            profile == null && projects.isEmpty() -> SettingsStatus(
                icon = MaterialSymbols.Rounded.Auto_awesome,
                emptyMessage = AnrealCopy.get(AnrealCopy.PERSONALIZATION_EMPTY),
            )
            else -> {
                ProfileCard(
                    title = AnrealCopy.get(AnrealCopy.LABEL_PROFILE_FACTS),
                    profile = profile,
                    onReset = onResetUser,
                )
                if (projects.isNotEmpty()) {
                    Text(
                        AnrealCopy.get(AnrealCopy.LABEL_PROJECT_PROFILES),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    projects.forEach { project ->
                        ProfileCard(
                            title = project.name,
                            profile = project.profile,
                            onReset = { onResetProject(project.id, project.name) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(title: String, profile: ProfileUi?, onReset: () -> Unit) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), tone = GlassTone.Regular) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AnrealSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onReset, enabled = profile != null && !profile.isEmpty) {
                    Text(AnrealCopy.get(AnrealCopy.ACTION_RESET))
                }
            }
            if (profile == null || profile.isEmpty) {
                Text(
                    AnrealCopy.get(AnrealCopy.PERSONALIZATION_EMPTY),
                    style = MaterialTheme.typography.bodyMedium,
                    color = glassFaintTextColor(),
                )
            } else {
                profile.sections.filter { it.bullets.isNotEmpty() }.forEach { section ->
                    Text(section.label, style = MaterialTheme.typography.labelLarge)
                    section.bullets.forEach { bullet ->
                        Text("• $bullet", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                profile.explicitFacts.forEach { fact ->
                    Text("• $fact", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionContainer(
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.lg),
    ) {
        SettingsSectionHeader(title, body)
        content()
    }
}

@Composable
private fun LoadingSettingsCard() {
    GlassSurface(modifier = Modifier.fillMaxWidth(), tone = GlassTone.Regular) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AnrealSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Text(AnrealCopy.get(AnrealCopy.STATUS_LOADING))
        }
    }
}

@Composable
private fun ErrorSettingsCard(error: UiText, onRetry: () -> Unit) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), tone = GlassTone.Regular) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AnrealSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
        ) {
            Text(error.asString(), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text(AnrealCopy.get(AnrealCopy.ACTION_RETRY)) }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = glassMutedTextColor(),
        )
    }
}

@Composable
private fun AccountValueRow(
    label: String,
    value: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AnrealSpacing.md, vertical = AnrealSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.36f),
            style = MaterialTheme.typography.labelMedium,
            color = glassFaintTextColor(),
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.64f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        actionLabel?.let { action ->
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

private fun AccountSection.label(): String = when (this) {
    AccountSection.Account -> AnrealCopy.get(AnrealCopy.LABEL_ACCOUNT)
    AccountSection.Usage -> AnrealCopy.get(AnrealCopy.LABEL_USAGE)
    AccountSection.Personalization -> AnrealCopy.get(AnrealCopy.LABEL_PERSONALIZATION)
}

private fun AccountState.resolvedName(): String =
    name.ifBlank { AnrealCopy.get(AnrealCopy.LABEL_APP_NAME) }

private fun AccountState.resolvedEmail(): String =
    email.ifBlank { AnrealCopy.get(AnrealCopy.ACCOUNT_EMAIL_EMPTY) }

@AnrealPreviews
@Composable
private fun AccountSettingsLayoutPreview() {
    AnrealPreview {
        AccountSettingsLayout(
            state = AccountState(name = "shafiq", email = "shafiq@testing.com"),
            onBack = {},
            onSelectSection = {},
            onSignOut = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun AccountSettingsSigningOutPreview() {
    AnrealPreview {
        AccountSettingsLayout(
            state = AccountState(
                name = "shafiq",
                email = "shafiq@testing.com",
                isSigningOut = true,
            ),
            onBack = {},
            onSelectSection = {},
            onSignOut = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun AccountSettingsUsagePreview() {
    AnrealPreview {
        AccountSettingsLayout(
            state = AccountState(section = AccountSection.Usage),
            onBack = {},
            onSelectSection = {},
            onSignOut = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun AccountSettingsPersonalizationPreview() {
    AnrealPreview {
        AccountSettingsLayout(
            state = AccountState(section = AccountSection.Personalization),
            onBack = {},
            onSelectSection = {},
            onSignOut = {},
        )
    }
}
