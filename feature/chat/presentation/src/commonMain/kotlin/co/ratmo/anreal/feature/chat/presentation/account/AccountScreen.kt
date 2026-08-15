package co.ratmo.anreal.feature.chat.presentation.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.ratmo.anreal.core.designsystem.component.AnrealAtmosphere
import co.ratmo.anreal.core.designsystem.component.AnrealPrimaryButton
import co.ratmo.anreal.core.designsystem.component.glassFaintTextColor
import co.ratmo.anreal.core.designsystem.component.glassHighlightColor
import co.ratmo.anreal.core.designsystem.component.glassMutedTextColor
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.ObserveAsEvents
import co.ratmo.anreal.feature.chat.presentation.AccountUi
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Arrow_back
import com.composables.icons.materialsymbols.rounded.Auto_awesome
import com.composables.icons.materialsymbols.rounded.Bolt
import com.composables.icons.materialsymbols.rounded.Person
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AccountRoot(
    account: AccountUi,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: AccountViewModel = koinViewModel { parametersOf(account) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            AccountEvent.NavigateBack -> onBack()
            AccountEvent.SignOut -> onSignOut()
        }
    }
    AccountScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    state: AccountState,
    onAction: (AccountAction) -> Unit,
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
                            onClick = { onAction(AccountAction.OnBack) },
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
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                AccountSectionNav(
                    selected = state.section,
                    enabled = !state.isSigningOut,
                    onSelect = { onAction(AccountAction.OnSelectSection(it)) },
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = AnrealSpacing.screenCompact,
                            vertical = AnrealSpacing.md,
                        ),
                    verticalArrangement = Arrangement.spacedBy(AnrealSpacing.lg),
                ) {
                    when (state.section) {
                        AccountSection.Account -> AccountDetails(
                            state = state,
                            onAction = onAction,
                        )
                        AccountSection.Usage -> UsageDetails()
                        AccountSection.Personalization -> PersonalizationDetails()
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountSectionNav(
    selected: AccountSection,
    enabled: Boolean,
    onSelect: (AccountSection) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AnrealSpacing.sm, vertical = AnrealSpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs),
    ) {
        AccountSection.entries.forEach { section ->
            AccountSectionChip(
                section = section,
                selected = section == selected,
                enabled = enabled,
                onClick = { onSelect(section) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AccountSectionChip(
    section: AccountSection,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when (section) {
        AccountSection.Account -> AnrealCopy.get(AnrealCopy.LABEL_ACCOUNT)
        AccountSection.Usage -> AnrealCopy.get(AnrealCopy.LABEL_USAGE)
        AccountSection.Personalization -> AnrealCopy.get(AnrealCopy.LABEL_PERSONALIZATION)
    }
    val icon: ImageVector = when (section) {
        AccountSection.Account -> MaterialSymbols.Rounded.Person
        AccountSection.Usage -> MaterialSymbols.Rounded.Bolt
        AccountSection.Personalization -> MaterialSymbols.Rounded.Auto_awesome
    }
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onSurface
        else -> glassMutedTextColor()
    }
    Surface(
        modifier = modifier
            .heightIn(min = AnrealSpacing.touch)
            .semantics { role = Role.Tab }
            .clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (selected) glassHighlightColor() else Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AnrealSpacing.xs, vertical = AnrealSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AccountDetails(
    state: AccountState,
    onAction: (AccountAction) -> Unit,
) {
    SectionHeader(
        title = AnrealCopy.get(AnrealCopy.LABEL_ACCOUNT),
        body = AnrealCopy.get(AnrealCopy.ACCOUNT_SECTION_BODY),
    )
    AccountField(
        label = AnrealCopy.get(AnrealCopy.LABEL_NAME),
        value = state.name.ifBlank { AnrealCopy.get(AnrealCopy.LABEL_APP_NAME) },
    )
    AccountField(
        label = AnrealCopy.get(AnrealCopy.LABEL_EMAIL),
        value = state.email.ifBlank { AnrealCopy.get(AnrealCopy.ACCOUNT_EMAIL_EMPTY) },
    )
    AnrealPrimaryButton(
        label = AnrealCopy.get(AnrealCopy.ACTION_LOG_OUT),
        onClick = { onAction(AccountAction.OnSignOut) },
        loading = state.isSigningOut,
        loadingLabel = AnrealCopy.get(AnrealCopy.ACTION_LOGGING_OUT),
    )
}

@Composable
private fun UsageDetails() {
    SectionHeader(
        title = AnrealCopy.get(AnrealCopy.LABEL_USAGE),
        body = AnrealCopy.get(AnrealCopy.USAGE_SECTION_BODY),
    )
    Text(
        text = AnrealCopy.get(AnrealCopy.USAGE_EMPTY),
        style = MaterialTheme.typography.bodyMedium,
        color = glassFaintTextColor(),
    )
}

@Composable
private fun PersonalizationDetails() {
    SectionHeader(
        title = AnrealCopy.get(AnrealCopy.LABEL_PERSONALIZATION),
        body = AnrealCopy.get(AnrealCopy.PERSONALIZATION_SECTION_BODY),
    )
    Text(
        text = AnrealCopy.get(AnrealCopy.PERSONALIZATION_EMPTY),
        style = MaterialTheme.typography.bodyMedium,
        color = glassFaintTextColor(),
    )
}

@Composable
private fun SectionHeader(
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
private fun AccountField(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = glassFaintTextColor(),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@AnrealPreviews
@Composable
private fun AccountPopulatedPreview() {
    AnrealPreview {
        AccountScreen(
            state = AccountState(name = "shafiq", email = "shafiq@testing.com"),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun AccountSigningOutPreview() {
    AnrealPreview {
        AccountScreen(
            state = AccountState(
                name = "shafiq",
                email = "shafiq@testing.com",
                isSigningOut = true,
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun AccountUsageEmptyPreview() {
    AnrealPreview {
        AccountScreen(
            state = AccountState(
                name = "shafiq",
                email = "shafiq@testing.com",
                section = AccountSection.Usage,
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun AccountPersonalizationEmptyPreview() {
    AnrealPreview {
        AccountScreen(
            state = AccountState(
                name = "shafiq",
                email = "shafiq@testing.com",
                section = AccountSection.Personalization,
            ),
            onAction = {},
        )
    }
}
