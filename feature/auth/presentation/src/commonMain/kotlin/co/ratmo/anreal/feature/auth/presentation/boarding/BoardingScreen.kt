package co.ratmo.anreal.feature.auth.presentation.boarding

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.ratmo.anreal.core.designsystem.component.AnrealAuthLayout
import co.ratmo.anreal.core.designsystem.component.AnrealAuthScaffold
import co.ratmo.anreal.core.designsystem.component.AnrealPrimaryButton
import co.ratmo.anreal.core.designsystem.component.AnrealTextField
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.preview.PreviewNightUiMode
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.ObserveAsEvents
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.auth.presentation.component.AuthSwitchRow
import co.ratmo.anreal.feature.auth.presentation.component.BoardingBrandHeader
import co.ratmo.anreal.feature.auth.presentation.component.BoardingCarousel
import co.ratmo.anreal.feature.auth.presentation.component.boardingSlides
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BoardingRoot(
    onNavigateRegister: (String) -> Unit,
    onNavigateLogin: (String) -> Unit,
    viewModel: BoardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is BoardingEvent.NavigateRegister -> onNavigateRegister(event.email)
            is BoardingEvent.NavigateLogin -> onNavigateLogin(event.email)
        }
    }
    BoardingScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun BoardingScreen(
    state: BoardingState,
    onAction: (BoardingAction) -> Unit,
) {
    val slides = remember { boardingSlides() }
    val pagerState = rememberPagerState(pageCount = { slides.size })
    var emailFocused by remember { mutableStateOf(false) }
    AnrealAuthScaffold(layout = AnrealAuthLayout.Docked) {
        BoardingBrandHeader()
        BoardingCarousel(
            modifier = Modifier.weight(1f),
            paused = emailFocused,
            slides = slides,
            pagerState = pagerState,
        )
        AnrealTextField(
            value = state.email,
            onValueChange = { onAction(BoardingAction.OnEmailChange(it)) },
            label = AnrealCopy.get(AnrealCopy.LABEL_EMAIL),
            placeholder = AnrealCopy.get(AnrealCopy.PLACEHOLDER_EMAIL),
            error = state.emailError?.asString(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { onAction(BoardingAction.OnRegisterClick) },
            ),
            onFocusChange = { emailFocused = it },
        )
        AnrealPrimaryButton(
            label = AnrealCopy.get(AnrealCopy.ACTION_CREATE_ACCOUNT),
            onClick = { onAction(BoardingAction.OnRegisterClick) },
        )
        AuthSwitchRow(
            prompt = AnrealCopy.get(AnrealCopy.AUTH_HAVE_ACCOUNT),
            actionLabel = AnrealCopy.get(AnrealCopy.ACTION_SIGN_IN),
            onClick = { onAction(BoardingAction.OnLoginClick) },
        )
    }
}

@AnrealPreviews
@Composable
private fun BoardingIdlePreview() {
    AnrealPreview {
        BoardingScreen(state = BoardingState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun BoardingFilledPreview() {
    AnrealPreview {
        BoardingScreen(
            state = BoardingState(email = "you@company.com"),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun BoardingEmailErrorPreview() {
    AnrealPreview {
        BoardingScreen(
            state = BoardingState(
                email = "nope",
                emailError = UiText.StringResource(AnrealCopy.ERROR_INVALID_EMAIL),
            ),
            onAction = {},
        )
    }
}

@Preview(name = "Font 1.5", showBackground = true, fontScale = 1.5f, group = "Anreal")
@Preview(
    name = "Font 1.5 dark",
    showBackground = true,
    fontScale = 1.5f,
    group = "Anreal",
    uiMode = PreviewNightUiMode,
)
@Composable
private fun BoardingFontScalePreview() {
    AnrealPreview {
        BoardingScreen(
            state = BoardingState(email = "you@company.com"),
            onAction = {},
        )
    }
}
