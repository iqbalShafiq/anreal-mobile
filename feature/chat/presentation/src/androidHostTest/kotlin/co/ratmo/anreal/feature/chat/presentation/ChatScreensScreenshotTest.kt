package co.ratmo.anreal.feature.chat.presentation

import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import co.ratmo.anreal.feature.chat.domain.stream.ChatThreadState
import co.ratmo.anreal.feature.chat.presentation.preview.chatComposerCatalogPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatPopulatedPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatStreamingPreviewState
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import org.junit.Before
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@OptIn(ExperimentalRoborazziApi::class)
class ChatScreensScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun reduceMotion() {
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
    }

    @Test
    fun populatedChatLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                ChatScreen(state = chatPopulatedPreviewState(), onAction = {})
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun streamingChatDark() {
        composeTestRule.setContent {
            AnrealPreview(dark = true) {
                ChatScreen(state = chatStreamingPreviewState(), onAction = {})
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun contextUsageSheetLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                ChatScreen(state = chatPopulatedPreviewState(), onAction = {})
            }
        }

        composeTestRule
            .onNodeWithContentDescription(AnrealCopy.get(AnrealCopy.CD_CONTEXT_USAGE))
            .performClick()
        composeTestRule.waitForIdle()
        captureScreenRoboImage()
    }

    @Test
    fun modelAndReasoningSheetLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                ChatScreen(state = chatComposerCatalogPreviewState(), onAction = {})
            }
        }

        composeTestRule
            .onNodeWithContentDescription(AnrealCopy.get(AnrealCopy.CD_MODEL))
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("GPT Luna 5.6").assertIsDisplayed()
        composeTestRule.onNodeWithText(AnrealCopy.get(AnrealCopy.LABEL_REASONING)).assertIsDisplayed()
        captureScreenRoboImage()
    }

    @Test
    fun populatedSessionStartsAtLatestMessage() {
        val messages = List(30) { index ->
            ChatMessage(
                id = "message-$index",
                role = if (index % 2 == 0) ChatRole.User else ChatRole.Assistant,
                parts = listOf(
                    ChatPart.Text(
                        id = "text-$index",
                        text = if (index == 29) "Latest loaded answer" else "Conversation message $index",
                    ),
                ),
                isComplete = true,
            )
        }
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                ChatScreen(
                    state = chatPopulatedPreviewState().copy(
                        thread = ChatThreadState(messages = messages),
                    ),
                    onAction = {},
                )
            }
        }

        composeTestRule.waitForIdle()
        val latest = composeTestRule.onNodeWithText("Latest loaded answer").assertIsDisplayed()
        assertTrue(latest.getUnclippedBoundsInRoot().bottom.value > 550f)
        captureScreenRoboImage()
    }

    @Test
    fun streamingUpdateDoesNotOverrideUserScroll() {
        val messages = List(30) { index ->
            ChatMessage(
                id = "message-$index",
                role = if (index % 2 == 0) ChatRole.User else ChatRole.Assistant,
                parts = listOf(
                    ChatPart.Text(
                        id = "text-$index",
                        text = if (index == 29) "Latest loaded answer" else "Conversation message $index",
                    ),
                ),
                isComplete = true,
            )
        }
        val screenState = mutableStateOf(
            chatPopulatedPreviewState().copy(thread = ChatThreadState(messages = messages)),
        )
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                ChatScreen(state = screenState.value, onAction = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Latest loaded answer", substring = true)
            .performTouchInput {
                repeat(3) { swipeDown() }
            }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription(AnrealCopy.get(AnrealCopy.CD_SCROLL_TO_BOTTOM))
            .assertIsDisplayed()

        val updatedText = "Latest loaded answer with streaming update"
        screenState.value = screenState.value.copy(
            thread = screenState.value.thread.copy(
                messages = messages.dropLast(1) + messages.last().copy(
                    parts = listOf(ChatPart.Text(id = "text-29", text = updatedText)),
                    isComplete = false,
                ),
            ),
        )
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription(AnrealCopy.get(AnrealCopy.CD_SCROLL_TO_BOTTOM))
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        val latest = composeTestRule.onNodeWithText(updatedText).assertIsDisplayed()
        assertTrue(latest.getUnclippedBoundsInRoot().bottom.value > 550f)
    }
}
