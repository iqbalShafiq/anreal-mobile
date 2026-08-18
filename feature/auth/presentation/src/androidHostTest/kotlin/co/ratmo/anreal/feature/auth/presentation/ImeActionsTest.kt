package co.ratmo.anreal.feature.auth.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import co.ratmo.anreal.core.designsystem.component.AnrealComposerField
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.feature.auth.presentation.boarding.BoardingScreen
import co.ratmo.anreal.feature.auth.presentation.boarding.BoardingState
import co.ratmo.anreal.feature.auth.presentation.login.LoginScreen
import co.ratmo.anreal.feature.auth.presentation.login.LoginState
import co.ratmo.anreal.feature.auth.presentation.register.RegisterScreen
import co.ratmo.anreal.feature.auth.presentation.register.RegisterState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ImeActionsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun boarding_email_uses_next_without_a_newline_or_done_action() {
        composeRule.setContent {
            AnrealPreview {
                BoardingScreen(state = BoardingState(), onAction = {})
            }
        }
        composeRule.onAllNodes(hasImeAction(ImeAction.Next)).assertCountEquals(1)
        composeRule.onAllNodes(hasImeAction(ImeAction.Done)).assertCountEquals(0)
    }

    @Test
    fun login_uses_next_then_done() {
        composeRule.setContent {
            AnrealPreview {
                LoginScreen(state = LoginState(), onAction = {})
            }
        }
        composeRule.onAllNodes(hasImeAction(ImeAction.Next)).assertCountEquals(1)
        composeRule.onAllNodes(hasImeAction(ImeAction.Done)).assertCountEquals(1)
    }

    @Test
    fun register_uses_next_until_the_final_done_action() {
        composeRule.setContent {
            AnrealPreview {
                RegisterScreen(state = RegisterState(), onAction = {})
            }
        }
        composeRule.onAllNodes(hasImeAction(ImeAction.Next)).assertCountEquals(3)
        composeRule.onAllNodes(hasImeAction(ImeAction.Done)).assertCountEquals(1)
    }

    @Test
    fun composer_accepts_newlines_without_exposing_a_send_ime_action() {
        var observedValue = ""
        composeRule.setContent {
            var value by remember { mutableStateOf("") }
            observedValue = value
            AnrealPreview {
                AnrealComposerField(
                    value = value,
                    onValueChange = { value = it },
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("First line\nSecond line")
        composeRule.runOnIdle {
            assertEquals("First line\nSecond line", observedValue)
        }
        composeRule.onAllNodes(hasImeAction(ImeAction.Send)).assertCountEquals(0)
    }

    private fun hasImeAction(action: ImeAction): SemanticsMatcher {
        return SemanticsMatcher.expectValue(SemanticsProperties.ImeAction, action)
    }
}
