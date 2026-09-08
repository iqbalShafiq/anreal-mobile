package co.ratmo.anreal.feature.chat.presentation.account.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
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
import co.ratmo.anreal.core.designsystem.component.AnrealAtmosphereBackground
import co.ratmo.anreal.core.designsystem.component.AnrealLoadingIndicator
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
import co.ratmo.anreal.core.designsystem.component.AnrealSkeletonCard
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Arrow_back
import com.composables.icons.materialsymbols.rounded.Auto_awesome
import com.composables.icons.materialsymbols.rounded.Bolt
import com.composables.icons.materialsymbols.rounded.Chevron_right
import com.composables.icons.materialsymbols.rounded.Help
import androidx.compose.foundation.shape.CircleShape
import com.composables.icons.materialsymbols.rounded.Edit
import com.composables.icons.materialsymbols.rounded.Info
import com.composables.icons.materialsymbols.rounded.Logout
import com.composables.icons.materialsymbols.rounded.Palette
import com.composables.icons.materialsymbols.rounded.Person

private val SettingsContentMaxWidth = 640.dp

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.large,
    borderColor: Color? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = borderColor?.let { BorderStroke(1.dp, it) },
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountSettingsLayout(
    state: AccountState,
    onBack: () -> Unit,
    onSelectSection: (AccountSection) -> Unit,
    onRetryUsage: () -> Unit = {},
    onRetryPersonalization: () -> Unit = {},
    onRequestResetUserProfile: () -> Unit = {},
    onRequestResetProjectProfile: (String, String) -> Unit = { _, _ -> },
    onConfirmResetProfile: () -> Unit = {},
    onDismissResetProfile: () -> Unit = {},
    onRequestSignOut: () -> Unit = {},
    onDismissSignOut: () -> Unit = {},
    onThemeModeChange: (AppThemeMode) -> Unit = {},
    onToggleDynamicColor: () -> Unit = {},
    onToggleReduceMotion: () -> Unit = {},
    onToggleReduceTransparency: () -> Unit = {},
    onSignOut: () -> Unit,
) {
    AnrealAtmosphere(
        background = AnrealAtmosphereBackground.Surface,
        animateBackground = false,
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = when (state.section) {
                                        AccountSection.Account -> AnrealCopy.get(AnrealCopy.ACTION_SETTINGS)
                                        AccountSection.Appearance -> AnrealCopy.get(AnrealCopy.LABEL_APPEARANCE)
                                        AccountSection.Usage -> AnrealCopy.get(AnrealCopy.LABEL_USAGE)
                                        AccountSection.Personalization -> AnrealCopy.get(AnrealCopy.LABEL_PERSONALIZATION)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                if (state.section != AccountSection.Account) {
                                    Text(
                                        text = AnrealCopy.get(AnrealCopy.ACTION_SETTINGS),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glassMutedTextColor(),
                                    )
                                }
                            }
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
                            containerColor = MaterialTheme.colorScheme.surface,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                )
            },
        ) { scaffoldPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = SettingsContentMaxWidth)
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(
                        start = AnrealSpacing.screenCompact,
                        top = AnrealSpacing.md,
                        end = AnrealSpacing.screenCompact,
                        bottom = AnrealSpacing.xl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(AnrealSpacing.lg),
                ) {
                    item {
                        AnimatedAccountSection(
                            state = state,
                            onRetryUsage = onRetryUsage,
                            onRetryPersonalization = onRetryPersonalization,
                            onRequestResetUserProfile = onRequestResetUserProfile,
                            onRequestResetProjectProfile = onRequestResetProjectProfile,
                            onSelectSection = onSelectSection,
                            onRequestSignOut = onRequestSignOut,
                            onThemeModeChange = onThemeModeChange,
                            onToggleDynamicColor = onToggleDynamicColor,
                            onToggleReduceMotion = onToggleReduceMotion,
                            onToggleReduceTransparency = onToggleReduceTransparency,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
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
                        AnrealLoadingIndicator(modifier = Modifier.size(18.dp), size = 18.dp)
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
    if (state.showSignOutDialog) {
        AlertDialog(
            onDismissRequest = onDismissSignOut,
            title = { Text(AnrealCopy.get(AnrealCopy.DIALOG_SIGN_OUT_TITLE)) },
            text = { Text(AnrealCopy.get(AnrealCopy.DIALOG_SIGN_OUT_BODY)) },
            confirmButton = {
                TextButton(onClick = onSignOut, enabled = !state.isSigningOut) {
                    Text(AnrealCopy.get(AnrealCopy.ACTION_LOG_OUT))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSignOut, enabled = !state.isSigningOut) {
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
    onRetryPersonalization: () -> Unit,
    onRequestResetUserProfile: () -> Unit,
    onRequestResetProjectProfile: (String, String) -> Unit,
    onSelectSection: (AccountSection) -> Unit,
    onRequestSignOut: () -> Unit,
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
            AccountSection.Account -> AccountMenu(
                state = state,
                onSelectSection = onSelectSection,
                onRequestSignOut = onRequestSignOut,
            )
            AccountSection.Appearance -> AppearanceSection(
                state = state,
                onThemeModeChange = onThemeModeChange,
                onToggleDynamicColor = onToggleDynamicColor,
                onToggleReduceMotion = onToggleReduceMotion,
                onToggleReduceTransparency = onToggleReduceTransparency,
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
private fun AccountMenu(
    state: AccountState,
    onSelectSection: (AccountSection) -> Unit,
    onRequestSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.lg),
    ) {
        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AnrealSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
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
                        style = MaterialTheme.typography.titleMedium,
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
                    Text(
                        text = "● " + AnrealCopy.get(
                            when {
                                state.isApiHealthy == true -> AnrealCopy.STATUS_CONNECTED
                                state.isHealthLoading -> AnrealCopy.STATUS_CHECKING
                                else -> AnrealCopy.STATUS_UNAVAILABLE
                            }
                        ) + " · " + state.themeMode.label(),
                        style = MaterialTheme.typography.bodySmall,
                        color = glassFaintTextColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = glassMutedTextColor(),
                        )
                    }
                }
            }
        }

        Text(
            text = AnrealCopy.get(AnrealCopy.LABEL_ACCOUNT_PREFERENCES),
            style = MaterialTheme.typography.labelSmall,
            color = glassFaintTextColor(),
            modifier = Modifier.padding(start = AnrealSpacing.xxs, top = AnrealSpacing.xxs),
        )

        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                MenuRow(
                    icon = MaterialSymbols.Rounded.Palette,
                    label = AnrealCopy.get(AnrealCopy.LABEL_APPEARANCE),
                    description = AnrealCopy.get(AnrealCopy.LABEL_APPEARANCE_DESCRIPTION),
                    value = state.themeMode.label(),
                    onClick = { onSelectSection(AccountSection.Appearance) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = AnrealSpacing.md),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f),
                )
                MenuRow(
                    icon = MaterialSymbols.Rounded.Bolt,
                    label = AnrealCopy.get(AnrealCopy.LABEL_USAGE),
                    description = AnrealCopy.get(AnrealCopy.LABEL_USAGE_DESCRIPTION),
                    value = state.usage?.let { "${(it.storageFraction * 100).toInt()}% used" } ?: "",
                    onClick = { onSelectSection(AccountSection.Usage) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = AnrealSpacing.md),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f),
                )
                MenuRow(
                    icon = MaterialSymbols.Rounded.Auto_awesome,
                    label = AnrealCopy.get(AnrealCopy.LABEL_PERSONALIZATION),
                    description = AnrealCopy.get(AnrealCopy.LABEL_PERSONALIZATION_DESCRIPTION),
                    value = if (state.projectProfiles.isNotEmpty()) "${state.projectProfiles.size} profiles" else "",
                    onClick = { onSelectSection(AccountSection.Personalization) },
                )
            }
        }

        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                MenuRow(
                    icon = MaterialSymbols.Rounded.Help,
                    label = AnrealCopy.get(AnrealCopy.LABEL_HELP_FEEDBACK),
                    description = AnrealCopy.get(AnrealCopy.LABEL_HELP_FEEDBACK_DESCRIPTION),
                    onClick = {},
                    showChevron = false,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = AnrealSpacing.md),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f),
                )
                MenuRow(
                    icon = MaterialSymbols.Rounded.Info,
                    label = AnrealCopy.get(AnrealCopy.LABEL_ABOUT),
                    description = AnrealCopy.get(AnrealCopy.LABEL_ABOUT_DESCRIPTION),
                    onClick = {},
                    showChevron = false,
                )
            }
        }

        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.22f),
        ) {
            MenuRow(
                icon = MaterialSymbols.Rounded.Logout,
                label = AnrealCopy.get(AnrealCopy.ACTION_LOG_OUT),
                description = AnrealCopy.get(AnrealCopy.DIALOG_SIGN_OUT_BODY),
                onClick = onRequestSignOut,
                isDestructive = true,
            )
        }

        Text(
            text = AnrealCopy.get(AnrealCopy.LABEL_ACCOUNT_FOOTER),
            style = MaterialTheme.typography.labelSmall,
            color = glassFaintTextColor(),
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun AppearanceSection(
    state: AccountState,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onToggleDynamicColor: () -> Unit,
    onToggleReduceMotion: () -> Unit,
    onToggleReduceTransparency: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.lg),
    ) {
        SettingsSectionHeader(
            title = AnrealCopy.get(AnrealCopy.LABEL_APPEARANCE),
            body = AnrealCopy.get(AnrealCopy.APPEARANCE_SECTION_BODY),
        )
        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(AnrealSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            ) {
                Text(
                    AnrealCopy.get(AnrealCopy.LABEL_THEME),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f))
                        .padding(AnrealSpacing.xxs),
                    horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs),
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        val selected = mode == state.themeMode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                )
                                .clickable { onThemeModeChange(mode) }
                                .padding(vertical = AnrealSpacing.xs),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = mode.label(),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
                Text(
                    text = AnrealCopy.get(AnrealCopy.LABEL_THEME_CURRENT)
                        .replace("{0}", state.themeMode.label()),
                    style = MaterialTheme.typography.bodySmall,
                    color = glassFaintTextColor(),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                PreferenceSwitchRow(
                    label = AnrealCopy.get(AnrealCopy.LABEL_DYNAMIC_COLOR),
                    description = AnrealCopy.get(AnrealCopy.LABEL_DYNAMIC_COLOR_DESCRIPTION),
                    helper = AnrealCopy.get(AnrealCopy.LABEL_DYNAMIC_COLOR_HELPER),
                    checked = state.dynamicColor,
                    onToggle = onToggleDynamicColor,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                PreferenceSwitchRow(
                    label = AnrealCopy.get(AnrealCopy.LABEL_REDUCE_MOTION),
                    description = AnrealCopy.get(AnrealCopy.LABEL_REDUCE_MOTION_DESCRIPTION),
                    helper = AnrealCopy.get(AnrealCopy.LABEL_REDUCE_MOTION_HELPER),
                    checked = state.reduceMotion,
                    onToggle = onToggleReduceMotion,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                PreferenceSwitchRow(
                    label = AnrealCopy.get(AnrealCopy.LABEL_REDUCE_TRANSPARENCY),
                    description = AnrealCopy.get(AnrealCopy.LABEL_REDUCE_TRANSPARENCY_DESCRIPTION),
                    helper = AnrealCopy.get(AnrealCopy.LABEL_REDUCE_TRANSPARENCY_HELPER),
                    checked = state.reduceTransparency,
                    onToggle = onToggleReduceTransparency,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
        ) {
            SettingsCard(
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(AnrealSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
                ) {
                    Text(
                        text = AnrealCopy.get(AnrealCopy.LABEL_PREVIEW_WITH_FROST),
                        style = MaterialTheme.typography.labelSmall,
                        color = glassFaintTextColor(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AnrealSpacing.search)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f)),
                    )
                    Text(
                        text = AnrealCopy.get(AnrealCopy.LABEL_PREVIEW_GLASS_TOP_BAR),
                        style = MaterialTheme.typography.bodySmall,
                        color = glassMutedTextColor(),
                    )
                }
            }
            SettingsCard(
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(AnrealSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
                ) {
                    Text(
                        text = AnrealCopy.get(AnrealCopy.LABEL_PREVIEW_REDUCED),
                        style = MaterialTheme.typography.labelSmall,
                        color = glassFaintTextColor(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AnrealSpacing.search)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                    Text(
                        text = AnrealCopy.get(AnrealCopy.LABEL_PREVIEW_SURFACE_CONTAINER),
                        style = MaterialTheme.typography.bodySmall,
                        color = glassMutedTextColor(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    description: String,
    value: String = "",
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    showChevron: Boolean = true,
) {
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val mutedColor = if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.72f) else glassMutedTextColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AnrealSpacing.menuRow)
            .clickable(onClick = onClick)
            .padding(horizontal = AnrealSpacing.md, vertical = AnrealSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = MaterialTheme.shapes.large,
            color = if (isDestructive) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = if (isDestructive) MaterialTheme.colorScheme.error else glassMutedTextColor(),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                maxLines = 1,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = mutedColor,
                maxLines = 1,
            )
        }
        if (value.isNotBlank()) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = glassFaintTextColor(),
            )
        }
        if (showChevron) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Chevron_right,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isDestructive) MaterialTheme.colorScheme.error else glassFaintTextColor(),
            )
        }
    }
}

@Composable
private fun SettingsStatus(
    icon: ImageVector,
    emptyMessage: String,
) {
    SettingsCard(
        modifier = Modifier.fillMaxWidth(),
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
private fun PreferenceSwitchRow(
    label: String,
    description: String = "",
    helper: String = "",
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = AnrealSpacing.touch),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs),
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                if (description.isNotBlank()) {
                    Text(description, style = MaterialTheme.typography.bodySmall, color = glassMutedTextColor())
                }
                if (helper.isNotBlank()) {
                    Text(helper, style = MaterialTheme.typography.labelSmall, color = glassFaintTextColor())
                }
            }
            Switch(checked = checked, onCheckedChange = { onToggle() })
        }
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
                SettingsCard(modifier = Modifier.fillMaxWidth()) {
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
    SettingsCard(modifier = Modifier.fillMaxWidth()) {
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
        SettingsCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                rows.forEachIndexed { index, row ->
                    val value = if (row.detail.isBlank()) {
                        "${row.requests} requests · ${row.tokens} tokens"
                    } else {
                        "${row.requests} requests · ${row.tokens} tokens · ${row.detail}"
                    }
                    AccountValueRow(row.label, value)
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
    SettingsCard(modifier = Modifier.fillMaxWidth()) {
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
                    Text("• ${fact.fact}", style = MaterialTheme.typography.bodyMedium)
                    if (fact.source.isNotBlank()) {
                        Text(
                            fact.source,
                            style = MaterialTheme.typography.bodySmall,
                            color = glassMutedTextColor(),
                        )
                    }
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
    ) {
        AnrealSkeletonCard()
        AnrealSkeletonCard()
    }
}

@Composable
private fun ErrorSettingsCard(error: UiText, onRetry: () -> Unit) {
    SettingsCard(modifier = Modifier.fillMaxWidth()) {
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
    AccountSection.Appearance -> AnrealCopy.get(AnrealCopy.LABEL_APPEARANCE)
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
private fun AccountSettingsSignOutDialogPreview() {
    AnrealPreview {
        AccountSettingsLayout(
            state = AccountState(
                name = "shafiq",
                email = "shafiq@testing.com",
                showSignOutDialog = true,
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
