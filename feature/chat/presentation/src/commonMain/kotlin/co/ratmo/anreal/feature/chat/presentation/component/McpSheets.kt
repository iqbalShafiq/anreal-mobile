package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import co.ratmo.anreal.core.designsystem.component.AnrealBottomSheet
import co.ratmo.anreal.core.designsystem.component.AnrealEmpty
import co.ratmo.anreal.core.designsystem.component.AnrealError
import co.ratmo.anreal.core.designsystem.component.AnrealPrimaryButton
import co.ratmo.anreal.core.designsystem.component.AnrealSheetTitle
import co.ratmo.anreal.core.designsystem.component.AnrealSkeletonList
import co.ratmo.anreal.core.designsystem.component.AnrealTextField
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealMotion
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.designsystem.theme.LocalAnrealReduceMotion
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.ObserveAsEvents
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.core.presentation.toUiText
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.McpAuthType
import co.ratmo.anreal.feature.chat.domain.McpRemoteDataSource
import co.ratmo.anreal.feature.chat.domain.McpServer
import co.ratmo.anreal.feature.chat.domain.McpStatus
import co.ratmo.anreal.feature.chat.domain.McpTool
import co.ratmo.anreal.feature.chat.domain.validateMcpInput
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add
import com.composables.icons.materialsymbols.rounded.Close
import com.composables.icons.materialsymbols.rounded.Delete
import com.composables.icons.materialsymbols.rounded.Edit
import com.composables.icons.materialsymbols.rounded.Expand_more
import com.composables.icons.materialsymbols.rounded.Science
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

private const val EDITOR_NAME_KEY = "mcpEditorName"
private const val EDITOR_URL_KEY = "mcpEditorUrl"

internal data class McpHeaderUi(
    val id: Int,
    val name: String = "",
    val value: String = "",
)

internal sealed interface McpTestState {
    data object Untested : McpTestState
    data object Testing : McpTestState
    data class Tested(
        val ok: Boolean,
        val tools: List<McpTool> = emptyList(),
        val error: String? = null,
        val testedUrl: String = "",
    ) : McpTestState
}

internal data class McpEditorState(
    val isNew: Boolean = true,
    val sourceId: String? = null,
    val name: String = "",
    val url: String = "",
    val authType: McpAuthType = McpAuthType.None,
    val token: String = "",
    val hasSavedToken: Boolean = false,
    val headers: List<McpHeaderUi> = emptyList(),
    val nextHeaderId: Int = 0,
    val testState: McpTestState = McpTestState.Untested,
    val checkedTools: Set<String> = emptySet(),
    val fieldErrors: Map<String, String> = emptyMap(),
    val editorError: UiText? = null,
    val saving: Boolean = false,
)

internal data class McpState(
    val servers: List<McpServer> = emptyList(),
    val loading: Boolean = true,
    val error: UiText? = null,
    val editor: McpEditorState? = null,
    val rowBusyId: String? = null,
    val deleteTargetId: String? = null,
    val deleteBusy: Boolean = false,
    val deleteError: UiText? = null,
)

internal sealed interface McpAction {
    data object OnRetry : McpAction
    data object OnNewServer : McpAction
    data class OnEditServer(val id: String) : McpAction
    data object OnCloseEditor : McpAction
    data class OnEditorNameChange(val name: String) : McpAction
    data class OnEditorUrlChange(val url: String) : McpAction
    data class OnEditorAuthChange(val authType: McpAuthType) : McpAction
    data class OnEditorTokenChange(val token: String) : McpAction
    data object OnAddHeader : McpAction
    data class OnRemoveHeader(val headerId: Int) : McpAction
    data class OnHeaderNameChange(val headerId: Int, val name: String) : McpAction
    data class OnHeaderValueChange(val headerId: Int, val value: String) : McpAction
    data object OnTestConnection : McpAction
    data class OnToggleTool(val toolName: String) : McpAction
    data object OnSaveEditor : McpAction
    data class OnToggleEnabled(val id: String, val enabled: Boolean) : McpAction
    data class OnRequestDelete(val id: String) : McpAction
    data object OnCancelDelete : McpAction
    data object OnConfirmDelete : McpAction
}

internal sealed interface McpEvent {
    data class ShowMessage(val message: UiText) : McpEvent
}

internal class McpViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val mcpSource: McpRemoteDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(McpState())
    val state = _state.asStateFlow()

    private val _events = Channel<McpEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch { loadServers() }
    }

    fun onAction(action: McpAction) {
        when (action) {
            McpAction.OnRetry -> viewModelScope.launch { loadServers() }
            McpAction.OnNewServer -> openNewEditor()
            is McpAction.OnEditServer -> openEditEditor(action.id)
            McpAction.OnCloseEditor -> _state.update { it.copy(editor = null) }
            is McpAction.OnEditorNameChange -> updateEditor { editor ->
                editor.copy(name = action.name, fieldErrors = editor.fieldErrors - "name")
                    .also { savedStateHandle[EDITOR_NAME_KEY] = action.name }
            }
            is McpAction.OnEditorUrlChange -> updateEditor { editor ->
                editor.copy(url = action.url, fieldErrors = editor.fieldErrors - "url")
                    .also { savedStateHandle[EDITOR_URL_KEY] = action.url }
            }
            is McpAction.OnEditorAuthChange -> updateEditor { editor ->
                editor.copy(authType = action.authType)
            }
            is McpAction.OnEditorTokenChange -> updateEditor { editor ->
                editor.copy(token = action.token)
            }
            McpAction.OnAddHeader -> updateEditor { editor ->
                if (editor.headers.size >= MAX_HEADERS) {
                    editor
                } else {
                    editor.copy(
                        headers = editor.headers + McpHeaderUi(id = editor.nextHeaderId),
                        nextHeaderId = editor.nextHeaderId + 1,
                        fieldErrors = editor.fieldErrors - "headers",
                    )
                }
            }
            is McpAction.OnRemoveHeader -> updateEditor { editor ->
                editor.copy(headers = editor.headers.filterNot { it.id == action.headerId })
            }
            is McpAction.OnHeaderNameChange -> updateEditor { editor ->
                editor.copy(
                    headers = editor.headers.map { header ->
                        if (header.id == action.headerId) header.copy(name = action.name) else header
                    },
                    fieldErrors = editor.fieldErrors - "headers",
                )
            }
            is McpAction.OnHeaderValueChange -> updateEditor { editor ->
                editor.copy(
                    headers = editor.headers.map { header ->
                        if (header.id == action.headerId) header.copy(value = action.value) else header
                    },
                    fieldErrors = editor.fieldErrors - "headers",
                )
            }
            McpAction.OnTestConnection -> viewModelScope.launch { testConnection() }
            is McpAction.OnToggleTool -> updateEditor { editor ->
                val checked = if (action.toolName in editor.checkedTools) {
                    editor.checkedTools - action.toolName
                } else {
                    editor.checkedTools + action.toolName
                }
                editor.copy(checkedTools = checked, editorError = null)
            }
            McpAction.OnSaveEditor -> viewModelScope.launch { saveEditor() }
            is McpAction.OnToggleEnabled -> viewModelScope.launch {
                toggleEnabled(action.id, action.enabled)
            }
            is McpAction.OnRequestDelete -> _state.update {
                it.copy(deleteTargetId = action.id, deleteError = null)
            }
            McpAction.OnCancelDelete -> if (!_state.value.deleteBusy) {
                _state.update { it.copy(deleteTargetId = null, deleteError = null) }
            }
            McpAction.OnConfirmDelete -> viewModelScope.launch { confirmDelete() }
        }
    }

    private suspend fun loadServers() {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = mcpSource.listServers()) {
            is Result.Success -> {
                _state.update { it.copy(loading = false, servers = result.data) }
            }
            is Result.Error -> {
                _state.update {
                    it.copy(
                        loading = false,
                        error = result.error.serverMessage?.let(UiText::DynamicString)
                            ?: result.error.toUiText(),
                    )
                }
            }
        }
    }

    private fun openNewEditor() {
        _state.update {
            it.copy(
                editor = McpEditorState(
                    isNew = true,
                    name = savedStateHandle.get<String>(EDITOR_NAME_KEY).orEmpty(),
                    url = savedStateHandle.get<String>(EDITOR_URL_KEY).orEmpty(),
                ),
            )
        }
    }

    private fun openEditEditor(id: String) {
        val server = _state.value.servers.firstOrNull { it.id == id } ?: return
        _state.update {
            it.copy(
                editor = McpEditorState(
                    isNew = false,
                    sourceId = server.id,
                    name = server.name,
                    url = server.url,
                    authType = server.authType,
                    hasSavedToken = server.hasCredentials,
                    headers = emptyList(),
                    checkedTools = server.allowedTools.toSet(),
                ),
            )
        }
    }

    private fun updateEditor(transform: (McpEditorState) -> McpEditorState) {
        _state.update { current ->
            val editor = current.editor ?: return@update current
            current.copy(editor = transform(editor).copy(editorError = null))
        }
    }

    private suspend fun testConnection() {
        val editor = _state.value.editor ?: return
        val errors = validateMcpInput(
            name = editor.name,
            url = editor.url,
            authType = editor.authType.name.lowercase(),
            headers = editor.headers.map { it.name.trim() to it.value },
        )
        if (errors.isNotEmpty()) {
            _state.update { it.copy(editor = editor.copy(fieldErrors = errors)) }
            return
        }
        _state.update { it.copy(editor = editor.copy(testState = McpTestState.Testing, editorError = null)) }
        val trimmedUrl = editor.url.trim()
        when (val result = mcpSource.testConnection(
            url = trimmedUrl,
            authType = editor.authType,
            token = editor.token.ifBlank { null },
            headers = editor.headers.map { it.name.trim() to it.value },
            serverId = editor.sourceId,
        )) {
            is Result.Success -> {
                _state.update { current ->
                    val currentEditor = current.editor ?: return@update current
                    current.copy(
                        editor = currentEditor.copy(
                            testState = McpTestState.Tested(
                                ok = result.data.ok,
                                tools = result.data.tools,
                                error = result.data.error,
                                testedUrl = trimmedUrl,
                            ),
                            checkedTools = if (result.data.ok) {
                                result.data.tools.map { it.name }.toSet()
                            } else {
                                currentEditor.checkedTools
                            },
                        ),
                    )
                }
            }
            is Result.Error -> {
                _state.update { current ->
                    current.copy(
                        editor = current.editor?.copy(
                            testState = McpTestState.Untested,
                            editorError = result.error.serverMessage?.let(UiText::DynamicString)
                                ?: result.error.toUiText(),
                        ),
                    )
                }
            }
        }
    }

    private suspend fun saveEditor() {
        val editor = _state.value.editor ?: return
        val errors = validateMcpInput(
            name = editor.name,
            url = editor.url,
            authType = editor.authType.name.lowercase(),
            headers = editor.headers.map { it.name.trim() to it.value },
        )
        if (errors.isNotEmpty()) {
            _state.update { it.copy(editor = editor.copy(fieldErrors = errors)) }
            return
        }
        val tested = editor.testState as? McpTestState.Tested
        if (tested == null || !tested.ok) {
            _state.update {
                it.copy(editor = editor.copy(editorError = UiText.StringResource(AnrealCopy.MCP_TEST_FIRST)))
            }
            return
        }
        if (tested.testedUrl != editor.url.trim()) {
            _state.update {
                it.copy(editor = editor.copy(editorError = UiText.StringResource(AnrealCopy.MCP_URL_RETEST)))
            }
            return
        }
        if (editor.checkedTools.isEmpty()) {
            _state.update {
                it.copy(editor = editor.copy(editorError = UiText.StringResource(AnrealCopy.MCP_TOOLS_MIN)))
            }
            return
        }
        _state.update { it.copy(editor = editor.copy(saving = true, editorError = null)) }
        val headers = editor.headers.map { it.name.trim() to it.value }
        val result = if (editor.isNew) {
            mcpSource.createServer(
                name = editor.name.trim(),
                url = editor.url.trim(),
                authType = editor.authType,
                token = editor.token.ifBlank { null },
                headers = headers,
            )
        } else {
            mcpSource.updateServer(
                id = editor.sourceId.orEmpty(),
                name = editor.name.trim(),
                url = editor.url.trim(),
                authType = editor.authType,
                token = editor.token.ifBlank { null },
                headers = headers,
            )
        }
        when (result) {
            is Result.Success -> {
                savedStateHandle[EDITOR_NAME_KEY] = ""
                savedStateHandle[EDITOR_URL_KEY] = ""
                _state.update { current ->
                    val others = current.servers.filterNot { it.id == result.data.id }
                    current.copy(
                        servers = (others + result.data).sortedBy { it.name },
                        editor = null,
                    )
                }
            }
            is Result.Error -> {
                _state.update { current ->
                    current.copy(
                        editor = current.editor?.copy(
                            saving = false,
                            editorError = result.error.serverMessage?.let(UiText::DynamicString)
                                ?: result.error.toUiText(),
                        ),
                    )
                }
            }
        }
    }

    private suspend fun toggleEnabled(id: String, enabled: Boolean) {
        if (_state.value.rowBusyId != null) return
        _state.update { it.copy(rowBusyId = id) }
        when (val result = mcpSource.setServerEnabled(id, enabled)) {
            is Result.Success -> {
                _state.update { current ->
                    current.copy(
                        rowBusyId = null,
                        servers = current.servers.map { server ->
                            if (server.id == id) result.data else server
                        },
                    )
                }
            }
            is Result.Error -> {
                _state.update { it.copy(rowBusyId = null) }
                _events.send(McpEvent.ShowMessage(result.error.toUiText()))
            }
        }
    }

    private suspend fun confirmDelete() {
        val id = _state.value.deleteTargetId ?: return
        _state.update { it.copy(deleteBusy = true, deleteError = null) }
        when (val result = mcpSource.deleteServer(id)) {
            is Result.Success -> {
                _state.update { current ->
                    current.copy(
                        deleteBusy = false,
                        deleteTargetId = null,
                        servers = current.servers.filterNot { it.id == id },
                    )
                }
            }
            is Result.Error -> {
                _state.update {
                    it.copy(deleteBusy = false, deleteError = result.error.toUiText())
                }
            }
        }
    }

    private companion object {
        const val MAX_HEADERS = 16
    }
}

@Composable
internal fun McpManagementRoot(
    onDismiss: () -> Unit,
    viewModel: McpViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is McpEvent.ShowMessage -> snackbarScope.launch {
                snackbarHostState.showSnackbar(event.message.asString())
            }
        }
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        McpManagementScreen(state = state, onAction = viewModel::onAction)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
    @Suppress("UNUSED_EXPRESSION")
    onDismiss
}

@Composable
internal fun McpManagementScreen(
    state: McpState,
    onAction: (McpAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AnrealSheetTitle(AnrealCopy.get(AnrealCopy.LABEL_MCP))
        val reduceMotion = LocalAnrealReduceMotion.current
        val slideSpec: FiniteAnimationSpec<IntOffset> = if (reduceMotion) {
            snap()
        } else {
            tween(
                durationMillis = AnrealMotion.durationFast.inWholeMilliseconds.toInt(),
                easing = AnrealMotion.easeOut,
            )
        }
        AnimatedContent(
            targetState = state.editor != null,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally(slideSpec) { it } + fadeIn()) togetherWith
                        (slideOutHorizontally(slideSpec) { -it } + fadeOut())
                } else {
                    (slideInHorizontally(slideSpec) { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally(slideSpec) { it } + fadeOut())
                }.using(SizeTransform(clip = false))
            },
            label = "mcpDrillDown",
        ) { editing ->
            if (editing) {
                McpEditor(
                    editor = state.editor ?: McpEditorState(),
                    onAction = onAction,
                )
            } else {
                McpList(state = state, onAction = onAction)
            }
        }
    }
    val deleteTarget = state.servers.firstOrNull { it.id == state.deleteTargetId }
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { if (!state.deleteBusy) onAction(McpAction.OnCancelDelete) },
            icon = {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Delete,
                    contentDescription = null,
                )
            },
            title = { Text(AnrealCopy.get(AnrealCopy.DIALOG_DELETE_MCP_TITLE)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
                    Text(
                        text = UiText.StringResource(
                            AnrealCopy.DIALOG_DELETE_MCP_BODY,
                            listOf(deleteTarget.name),
                        ).asString(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    state.deleteError?.let { error ->
                        Text(
                            text = error.asString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onAction(McpAction.OnConfirmDelete) },
                    enabled = !state.deleteBusy,
                ) {
                    Text(
                        text = if (state.deleteBusy) {
                            AnrealCopy.get(AnrealCopy.ACTION_DELETING)
                        } else {
                            AnrealCopy.get(AnrealCopy.ACTION_DELETE)
                        },
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onAction(McpAction.OnCancelDelete) },
                    enabled = !state.deleteBusy,
                ) {
                    Text(AnrealCopy.get(AnrealCopy.ACTION_CANCEL))
                }
            },
        )
    }
}

@Composable
private fun McpList(
    state: McpState,
    onAction: (McpAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AnrealSpacing.lg),
    ) {
        when {
            state.loading -> {
                Column(
                    modifier = Modifier.padding(horizontal = AnrealSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
                ) {
                    AnrealSkeletonList(count = 2, itemHeight = 64.dp)
                }
            }
            state.error != null && state.servers.isEmpty() -> {
                AnrealError(
                    message = state.error.asString(),
                    retryLabel = AnrealCopy.get(AnrealCopy.ACTION_RETRY),
                    onRetry = { onAction(McpAction.OnRetry) },
                )
            }
            state.servers.isEmpty() -> {
                AnrealEmpty(
                    title = AnrealCopy.get(AnrealCopy.MCP_LIST_EMPTY_TITLE),
                    body = AnrealCopy.get(AnrealCopy.MCP_LIST_EMPTY_BODY),
                    actionLabel = AnrealCopy.get(AnrealCopy.ACTION_NEW),
                    onAction = { onAction(McpAction.OnNewServer) },
                )
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = { onAction(McpAction.OnNewServer) },
                        modifier = Modifier
                            .heightIn(min = AnrealSpacing.touch)
                            .padding(end = AnrealSpacing.sm),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Add,
                            contentDescription = null,
                        )
                        Text(
                            text = AnrealCopy.get(AnrealCopy.ACTION_NEW),
                            modifier = Modifier.padding(start = AnrealSpacing.xs),
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm)) {
                    state.servers.forEach { server ->
                        McpServerRow(
                            server = server,
                            busy = state.rowBusyId == server.id,
                            onAction = onAction,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun McpServerRow(
    server: McpServer,
    busy: Boolean,
    onAction: (McpAction) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AnrealSpacing.md),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AnrealSpacing.md, end = AnrealSpacing.xs)
                .heightIn(min = AnrealSpacing.menuRow),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        when (server.status) {
                            McpStatus.Ok -> MaterialTheme.colorScheme.primary
                            McpStatus.Error -> MaterialTheme.colorScheme.error
                            McpStatus.Untested -> MaterialTheme.colorScheme.outline
                        },
                    ),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = server.lastError
                    ?: if (server.status == McpStatus.Untested) {
                        UiText.StringResource(
                            AnrealCopy.MCP_SERVER_UNTESTED,
                            listOf(server.tools.size.toString()),
                        ).asString()
                    } else {
                        UiText.StringResource(
                            AnrealCopy.MCP_TOOLS_COUNT,
                            listOf(server.tools.size.toString()),
                        ).asString()
                    }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (server.lastError != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Switch(
                checked = server.isEnabled,
                onCheckedChange = { onAction(McpAction.OnToggleEnabled(server.id, it)) },
                enabled = !busy,
            )
            IconButton(
                onClick = { onAction(McpAction.OnEditServer(server.id)) },
                enabled = !busy,
                modifier = Modifier.size(AnrealSpacing.touch),
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Edit,
                    contentDescription = AnrealCopy.get(AnrealCopy.CD_EDIT_SERVER),
                )
            }
            IconButton(
                onClick = { onAction(McpAction.OnRequestDelete(server.id)) },
                enabled = !busy,
                modifier = Modifier.size(AnrealSpacing.touch),
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Delete,
                    contentDescription = AnrealCopy.get(AnrealCopy.CD_DELETE_SERVER),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun McpEditor(
    editor: McpEditorState,
    onAction: (McpAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AnrealSpacing.md)
            .padding(bottom = AnrealSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
    ) {
        Text(
            text = AnrealCopy.get(
                if (editor.isNew) {
                    AnrealCopy.MCP_EDITOR_NEW_TITLE
                } else {
                    AnrealCopy.MCP_EDITOR_EDIT_TITLE
                },
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        AnrealTextField(
            value = editor.name,
            onValueChange = { onAction(McpAction.OnEditorNameChange(it)) },
            label = AnrealCopy.get(AnrealCopy.LABEL_NAME),
            placeholder = AnrealCopy.get(AnrealCopy.LABEL_MCP),
            error = editor.fieldErrors["name"],
            enabled = !editor.saving,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        AnrealTextField(
            value = editor.url,
            onValueChange = { onAction(McpAction.OnEditorUrlChange(it)) },
            label = AnrealCopy.get(AnrealCopy.MCP_URL_LABEL),
            placeholder = AnrealCopy.get(AnrealCopy.MCP_URL_PLACEHOLDER),
            error = editor.fieldErrors["url"],
            enabled = !editor.saving,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
            ),
        )
        AuthTypeDropdown(editor = editor, onAction = onAction)
        if (editor.authType == McpAuthType.Bearer) {
            AnrealTextField(
                value = editor.token,
                onValueChange = { onAction(McpAction.OnEditorTokenChange(it)) },
                label = AnrealCopy.get(AnrealCopy.MCP_TOKEN_LABEL),
                placeholder = if (editor.hasSavedToken && !editor.isNew) {
                    AnrealCopy.get(AnrealCopy.MCP_TOKEN_KEEP)
                } else {
                    AnrealCopy.get(AnrealCopy.MCP_TOKEN_PLACEHOLDER)
                },
                error = null,
                enabled = !editor.saving,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                visualTransformation = PasswordVisualTransformation(),
            )
        }
        HeadersEditor(editor = editor, onAction = onAction)
        TestSection(editor = editor, onAction = onAction)
        editor.editorError?.let { error ->
            Text(
                text = error.asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { onAction(McpAction.OnCloseEditor) },
                modifier = Modifier.heightIn(min = AnrealSpacing.touch),
                enabled = !editor.saving,
            ) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_CANCEL))
            }
            val tested = editor.testState as? McpTestState.Tested
            val canSave = tested != null &&
                tested.ok &&
                tested.testedUrl == editor.url.trim() &&
                editor.checkedTools.isNotEmpty() &&
                !editor.saving
            AnrealPrimaryButton(
                label = AnrealCopy.get(
                    if (editor.isNew) AnrealCopy.ACTION_CREATE else AnrealCopy.ACTION_SAVE,
                ),
                loadingLabel = AnrealCopy.get(AnrealCopy.ACTION_SAVING),
                loading = editor.saving,
                enabled = canSave,
                onClick = { onAction(McpAction.OnSaveEditor) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthTypeDropdown(
    editor: McpEditorState,
    onAction: (McpAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
        Text(
            text = AnrealCopy.get(AnrealCopy.MCP_AUTH_LABEL).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (!editor.saving) expanded = it },
        ) {
            AnrealTextField(
                value = if (editor.authType == McpAuthType.Bearer) {
                    AnrealCopy.get(AnrealCopy.MCP_AUTH_BEARER)
                } else {
                    AnrealCopy.get(AnrealCopy.MCP_AUTH_NONE)
                },
                onValueChange = {},
                label = AnrealCopy.get(AnrealCopy.MCP_AUTH_LABEL),
                placeholder = "",
                showLabel = false,
                enabled = !editor.saving,
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                trailingIcon = {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Expand_more,
                        contentDescription = null,
                    )
                },
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(AnrealCopy.get(AnrealCopy.MCP_AUTH_NONE)) },
                    onClick = {
                        onAction(McpAction.OnEditorAuthChange(McpAuthType.None))
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
                DropdownMenuItem(
                    text = { Text(AnrealCopy.get(AnrealCopy.MCP_AUTH_BEARER)) },
                    onClick = {
                        onAction(McpAction.OnEditorAuthChange(McpAuthType.Bearer))
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun HeadersEditor(
    editor: McpEditorState,
    onAction: (McpAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
        Text(
            text = AnrealCopy.get(AnrealCopy.MCP_HEADERS_LABEL).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        editor.headers.forEach { header ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnrealTextField(
                    value = header.name,
                    onValueChange = { onAction(McpAction.OnHeaderNameChange(header.id, it)) },
                    label = AnrealCopy.get(AnrealCopy.LABEL_HEADER_NAME),
                    placeholder = "X-Api-Key",
                    error = null,
                    enabled = !editor.saving,
                    showLabel = false,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                AnrealTextField(
                    value = header.value,
                    onValueChange = { onAction(McpAction.OnHeaderValueChange(header.id, it)) },
                    label = AnrealCopy.get(AnrealCopy.LABEL_HEADER_VALUE),
                    placeholder = "…",
                    error = null,
                    enabled = !editor.saving,
                    showLabel = false,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                IconButton(
                    onClick = { onAction(McpAction.OnRemoveHeader(header.id)) },
                    enabled = !editor.saving,
                    modifier = Modifier.size(AnrealSpacing.touch),
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Close,
                        contentDescription = AnrealCopy.get(AnrealCopy.CD_REMOVE_HEADER),
                    )
                }
            }
        }
        editor.fieldErrors["headers"]?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        OutlinedButton(
            onClick = { onAction(McpAction.OnAddHeader) },
            modifier = Modifier.heightIn(min = AnrealSpacing.touch),
            enabled = !editor.saving && editor.headers.size < MAX_EDITOR_HEADERS,
        ) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Add,
                contentDescription = null,
            )
            Text(
                text = AnrealCopy.get(AnrealCopy.ACTION_ADD_HEADER),
                modifier = Modifier.padding(start = AnrealSpacing.xs),
            )
        }
    }
}

@Composable
private fun TestSection(
    editor: McpEditorState,
    onAction: (McpAction) -> Unit,
) {
    val testing = editor.testState == McpTestState.Testing
    val tested = editor.testState as? McpTestState.Tested
    Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
        OutlinedButton(
            onClick = { onAction(McpAction.OnTestConnection) },
            modifier = Modifier.heightIn(min = AnrealSpacing.touch),
            enabled = !editor.saving && !testing,
        ) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Science,
                contentDescription = null,
            )
            Text(
                text = AnrealCopy.get(
                    when {
                        testing -> AnrealCopy.MCP_TESTING_ACTION
                        tested != null -> AnrealCopy.MCP_RETEST_ACTION
                        else -> AnrealCopy.MCP_TEST_ACTION
                    },
                ),
                modifier = Modifier.padding(start = AnrealSpacing.xs),
            )
        }
        when {
            tested != null && tested.ok -> {
                Text(
                    text = AnrealCopy.get(AnrealCopy.MCP_TEST_OK),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (tested.testedUrl != editor.url.trim()) {
                    Text(
                        text = AnrealCopy.get(AnrealCopy.MCP_URL_RETEST),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text = UiText.StringResource(
                        AnrealCopy.MCP_TOOLS_TITLE,
                        listOf(
                            editor.checkedTools.size.toString(),
                            tested.tools.size.toString(),
                        ),
                    ).asString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                tested.tools.forEach { tool ->
                    val checked = tool.name in editor.checkedTools
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = AnrealSpacing.touch)
                            .clickable(
                                role = Role.Checkbox,
                                enabled = !editor.saving,
                                onClick = { onAction(McpAction.OnToggleTool(tool.name)) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onAction(McpAction.OnToggleTool(tool.name)) },
                            enabled = !editor.saving,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tool.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (tool.description.isNotBlank()) {
                                Text(
                                    text = tool.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            tested != null -> {
                Text(
                    text = tested.error ?: AnrealCopy.get(AnrealCopy.ERROR_UNKNOWN),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            else -> {
                Text(
                    text = AnrealCopy.get(AnrealCopy.MCP_TEST_FIRST),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private const val MAX_EDITOR_HEADERS = 16

internal fun mcpLoadingState(): McpState = McpState(loading = true)

internal fun mcpEmptyState(): McpState = McpState(loading = false)

internal fun mcpErrorState(): McpState = McpState(
    loading = false,
    error = UiText.DynamicString(AnrealCopy.get(AnrealCopy.MCP_LOAD_ERROR)),
)

internal fun mcpPopulatedState(): McpState = McpState(
    loading = false,
    servers = listOf(
        McpServer(
            id = "m1",
            name = "docs",
            url = "https://mcp.example.com/mcp",
            authType = McpAuthType.None,
            tools = listOf(McpTool("search_docs", "Search the docs")),
            isEnabled = true,
            status = McpStatus.Ok,
        ),
        McpServer(
            id = "m2",
            name = "staging",
            url = "https://staging.example.com/mcp",
            authType = McpAuthType.Bearer,
            tools = emptyList(),
            isEnabled = true,
            status = McpStatus.Untested,
            hasCredentials = true,
        ),
        McpServer(
            id = "m3",
            name = "broken",
            url = "https://gone.example.com/mcp",
            tools = emptyList(),
            isEnabled = false,
            status = McpStatus.Error,
            lastError = "Connection refused",
        ),
    ),
)

internal fun mcpEditorUntestedState(): McpState = mcpPopulatedState().copy(
    editor = McpEditorState(
        isNew = true,
        name = "docs",
        url = "https://mcp.example.com/mcp",
    ),
)

internal fun mcpEditorTestedState(): McpState = mcpPopulatedState().copy(
    editor = McpEditorState(
        isNew = false,
        sourceId = "m1",
        name = "docs",
        url = "https://mcp.example.com/mcp",
        testState = McpTestState.Tested(
            ok = true,
            tools = listOf(
                McpTool("search_docs", "Search the docs"),
                McpTool("fetch_page", "Fetch a page"),
            ),
            testedUrl = "https://mcp.example.com/mcp",
        ),
        checkedTools = setOf("search_docs", "fetch_page"),
    ),
)

internal fun mcpEditorErrorState(): McpState = mcpPopulatedState().copy(
    editor = McpEditorState(
        isNew = true,
        name = "docs",
        url = "http://mcp.example.com/mcp",
        testState = McpTestState.Tested(
            ok = false,
            error = "MCP URL must use public https (http and private hosts are blocked)",
            testedUrl = "http://mcp.example.com/mcp",
        ),
        fieldErrors = mapOf("url" to "Public https Streamable HTTP endpoint."),
    ),
)

@AnrealPreviews
@Composable
private fun McpListLoadingPreview() {
    AnrealPreview {
        McpManagementScreen(state = mcpLoadingState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun McpListEmptyPreview() {
    AnrealPreview {
        McpManagementScreen(state = mcpEmptyState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun McpListErrorPreview() {
    AnrealPreview {
        McpManagementScreen(state = mcpErrorState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun McpListPopulatedPreview() {
    AnrealPreview {
        McpManagementScreen(state = mcpPopulatedState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun McpEditorUntestedPreview() {
    AnrealPreview {
        McpManagementScreen(state = mcpEditorUntestedState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun McpEditorTestedPreview() {
    AnrealPreview {
        McpManagementScreen(state = mcpEditorTestedState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun McpEditorErrorPreview() {
    AnrealPreview {
        McpManagementScreen(state = mcpEditorErrorState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun McpDeleteConfirmPreview() {
    AnrealPreview {
        AnrealBottomSheet(onDismiss = {}) {
            McpManagementScreen(
                state = mcpPopulatedState().copy(deleteTargetId = "m1"),
                onAction = {},
            )
        }
    }
}
