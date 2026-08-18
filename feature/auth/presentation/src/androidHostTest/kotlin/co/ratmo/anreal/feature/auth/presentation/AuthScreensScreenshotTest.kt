package co.ratmo.anreal.feature.auth.presentation

import android.provider.Settings
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.feature.auth.presentation.boarding.BoardingScreen
import co.ratmo.anreal.feature.auth.presentation.boarding.BoardingState
import co.ratmo.anreal.feature.auth.presentation.login.LoginScreen
import co.ratmo.anreal.feature.auth.presentation.login.LoginState
import co.ratmo.anreal.feature.auth.presentation.register.RegisterScreen
import co.ratmo.anreal.feature.auth.presentation.register.RegisterState
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
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
class AuthScreensScreenshotTest {
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
    fun boardingIdleLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                BoardingScreen(state = BoardingState(), onAction = {})
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun boardingErrorDark() {
        composeTestRule.setContent {
            AnrealPreview(dark = true) {
                BoardingScreen(
                    state = BoardingState(
                        email = "invalid",
                        emailError = UiText.StringResource(AnrealCopy.ERROR_INVALID_EMAIL),
                    ),
                    onAction = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun loginFilledLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                LoginScreen(
                    state = LoginState(
                        email = "ada@analytical.engine",
                        password = "password1",
                    ),
                    onAction = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun loginErrorDark() {
        composeTestRule.setContent {
            AnrealPreview(dark = true) {
                LoginScreen(
                    state = LoginState(
                        email = "ada@analytical.engine",
                        password = "password1",
                        formError = UiText.StringResource(AnrealCopy.ERROR_INVALID_CREDENTIALS),
                    ),
                    onAction = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun registerFilledLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                RegisterScreen(
                    state = RegisterState(
                        name = "Ada Lovelace",
                        email = "ada@analytical.engine",
                        password = "password1",
                        confirmPassword = "password1",
                    ),
                    onAction = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun registerErrorDark() {
        composeTestRule.setContent {
            AnrealPreview(dark = true) {
                RegisterScreen(
                    state = RegisterState(
                        name = "Ada Lovelace",
                        email = "ada@analytical.engine",
                        password = "password1",
                        confirmPassword = "password1",
                        formError = UiText.StringResource(AnrealCopy.ERROR_EMAIL_TAKEN),
                    ),
                    onAction = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }
}
