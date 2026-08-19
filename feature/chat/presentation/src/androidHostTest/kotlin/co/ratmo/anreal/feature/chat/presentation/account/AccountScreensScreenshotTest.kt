package co.ratmo.anreal.feature.chat.presentation.account

import android.provider.Settings
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
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
class AccountScreensScreenshotTest {
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
    fun accountPopulatedLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                AccountScreen(
                    state = AccountState(
                        name = "Ada Lovelace",
                        email = "ada@analytical.engine",
                    ),
                    onAction = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun accountSigningOutDark() {
        composeTestRule.setContent {
            AnrealPreview(dark = true) {
                AccountScreen(
                    state = AccountState(
                        name = "Ada Lovelace",
                        email = "ada@analytical.engine",
                        isSigningOut = true,
                    ),
                    onAction = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun accountSignOutDialogLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                AccountScreen(
                    state = AccountState(
                        name = "Ada Lovelace",
                        email = "ada@analytical.engine",
                        showSignOutDialog = true,
                    ),
                    onAction = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun usagePopulatedDark() {
        composeTestRule.setContent {
            AnrealPreview(dark = true) {
                AccountScreen(
                    state = AccountState(
                        section = AccountSection.Usage,
                        usage = AccountUsageUi(
                            storageUsed = "12 MB",
                            storageMax = "200 MB",
                            storageFraction = 0.06f,
                            requestCount = "42",
                            totalTokens = "240,000",
                            inputTokens = "180,000",
                            outputTokens = "60,000",
                            cachedTokens = "20,000",
                            models = listOf(UsageBreakdownUi("GPT Luna 5.6", "40", "230,000")),
                            reasoning = listOf(UsageBreakdownUi("High", "10", "80,000")),
                        ),
                    ),
                    onAction = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun personalizationPopulatedLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                AccountScreen(
                    state = AccountState(
                        section = AccountSection.Personalization,
                        userProfile = ProfileUi(
                            sections = listOf(
                                ProfileSectionUi("facts", "Facts", listOf("Builds Kotlin apps")),
                                ProfileSectionUi("preferences", "Preferences", listOf("Concise answers")),
                            ),
                            explicitFacts = emptyList(),
                            updatedAt = "now",
                        ),
                    ),
                    onAction = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }
}
