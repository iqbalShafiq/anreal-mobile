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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.component.AnrealAtmosphere
import co.ratmo.anreal.core.designsystem.component.AnrealPrimaryButton
import co.ratmo.anreal.core.designsystem.component.GlassSurface
import co.ratmo.anreal.core.designsystem.component.GlassTone
import co.ratmo.anreal.core.designsystem.component.glassFaintTextColor
import co.ratmo.anreal.core.designsystem.component.glassHighlightColor
import co.ratmo.anreal.core.designsystem.component.glassMutedTextColor
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealMotion
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.designsystem.theme.LocalAnrealReduceMotion
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.feature.chat.presentation.account.AccountSection
import co.ratmo.anreal.feature.chat.presentation.account.AccountState
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Arrow_back
import com.composables.icons.materialsymbols.rounded.Auto_awesome
import com.composables.icons.materialsymbols.rounded.Bolt
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
    onSignOut: () -> Unit,
) {
    AnrealAtmosphere {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
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
                        .fillMaxHeight()
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = AnrealSpacing.screenCompact,
                        top = AnrealSpacing.xs,
                        end = AnrealSpacing.screenCompact,
                        bottom = LogoutDockClearance,
                    ),
                    verticalArrangement = Arrangement.spacedBy(AnrealSpacing.lg),
                ) {
                    item {
                        AccountSectionSwitcher(
                            selected = state.section,
                            enabled = !state.isSigningOut,
                            onSelect = onSelectSection,
                        )
                    }
                    item {
                        AnimatedAccountSection(
                            state = state,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                AnrealPrimaryButton(
                    label = AnrealCopy.get(AnrealCopy.ACTION_LOG_OUT),
                    onClick = onSignOut,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .widthIn(max = LogoutDockMaxWidth)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(
                            horizontal = AnrealSpacing.screenCompact,
                            vertical = AnrealSpacing.sm,
                        ),
                    loading = state.isSigningOut,
                    loadingLabel = AnrealCopy.get(AnrealCopy.ACTION_LOGGING_OUT),
                    destructive = true,
                )
            }
        }
    }
}

@Composable
private fun AccountSectionSwitcher(
    selected: AccountSection,
    enabled: Boolean,
    onSelect: (AccountSection) -> Unit,
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        tone = GlassTone.Thin,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AnrealSpacing.xxs)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs),
        ) {
            AccountSection.entries.forEach { section ->
                val isSelected = section == selected
                val contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    glassMutedTextColor()
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    color = if (isSelected) glassHighlightColor() else Color.Transparent,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = AnrealSpacing.touch)
                            .selectable(
                                selected = isSelected,
                                enabled = enabled,
                                role = Role.Tab,
                                onClick = { onSelect(section) },
                            )
                            .alpha(if (enabled) 1f else 0.38f)
                            .padding(horizontal = AnrealSpacing.xs),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = section.label(),
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedAccountSection(
    state: AccountState,
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
            AccountSection.Account -> AccountDetails(state)
            AccountSection.Usage -> EmptySettingsSection(
                icon = MaterialSymbols.Rounded.Bolt,
                title = AnrealCopy.get(AnrealCopy.LABEL_USAGE),
                body = AnrealCopy.get(AnrealCopy.USAGE_SECTION_BODY),
                emptyMessage = AnrealCopy.get(AnrealCopy.USAGE_EMPTY),
            )
            AccountSection.Personalization -> EmptySettingsSection(
                icon = MaterialSymbols.Rounded.Auto_awesome,
                title = AnrealCopy.get(AnrealCopy.LABEL_PERSONALIZATION),
                body = AnrealCopy.get(AnrealCopy.PERSONALIZATION_SECTION_BODY),
                emptyMessage = AnrealCopy.get(AnrealCopy.PERSONALIZATION_EMPTY),
            )
        }
    }
}

@Composable
private fun AccountDetails(state: AccountState) {
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
            }
        }
    }
}

@Composable
private fun EmptySettingsSection(
    icon: ImageVector,
    title: String,
    body: String,
    emptyMessage: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.lg),
    ) {
        SettingsSectionHeader(title = title, body = body)
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
