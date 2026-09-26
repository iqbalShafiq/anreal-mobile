package co.ratmo.anreal.feature.workspace.presentation

import android.provider.Settings
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.onRoot
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.feature.workspace.domain.ArtifactType
import co.ratmo.anreal.feature.workspace.domain.ScheduleFreq
import co.ratmo.anreal.feature.workspace.domain.TaskStatus
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class WorkspaceScreensScreenshotTest {
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
    fun projectsPopulatedLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                WorkspaceScreen(
                    state = WorkspaceState(
                        projects = listOf(
                            ProjectUi("p1", "Quarterly review", "Board sources and research", 2, 4),
                            ProjectUi("p2", "Product launch", "Launch research", 1, 3),
                        ),
                        loadedSections = setOf(WorkspaceSection.Projects),
                    ),
                    onAction = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun documentsEmptyDark() {
        composeTestRule.setContent {
            AnrealPreview(dark = true) {
                WorkspaceScreen(
                    state = WorkspaceState(
                        section = WorkspaceSection.Documents,
                        loadedSections = setOf(WorkspaceSection.Documents),
                    ),
                    onAction = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun imagesLoadingLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                WorkspaceScreen(
                    state = WorkspaceState(
                        section = WorkspaceSection.Images,
                        isLoading = true,
                    ),
                    onAction = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun imagesPopulatedLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                WorkspaceScreen(
                    state = WorkspaceState(
                        section = WorkspaceSection.Images,
                        images = listOf(
                            ImageUi(
                                id = "i1",
                                prompt = "Generated research diagram",
                                detail = "image-model · 1024×768",
                                bytes = previewPng(),
                            ),
                        ),
                        loadedSections = setOf(WorkspaceSection.Images),
                    ),
                    onAction = {},
                )
            }
        }
        composeTestRule.waitUntilAtLeastOneExists(
            hasStateDescription("Image loaded"),
            timeoutMillis = 5_000,
        )
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun sitesHighContrastLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false, highContrast = true) {
                WorkspaceScreen(
                    state = WorkspaceState(
                        section = WorkspaceSection.Sites,
                        sites = listOf(
                            SiteUi("s1", "abc", 3, 2, "ready", "/api/sites/s1/v3/preview/index.html", "/api/sites/s1/v3/download"),
                            SiteUi("s2", "abc", 1, null, "failed", null, "/api/sites/s2/v1/download"),
                        ),
                        scopeSessionId = "abc",
                        loadedSections = setOf(WorkspaceSection.Sites),
                    ),
                    onAction = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun sitesPopulatedLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                WorkspaceScreen(
                    state = WorkspaceState(
                        section = WorkspaceSection.Sites,
                        sites = listOf(
                            SiteUi("s1", "abc", 3, 2, "ready", "/api/sites/s1/v3/preview/index.html", "/api/sites/s1/v3/download"),
                            SiteUi("s2", "abc", 1, null, "failed", null, "/api/sites/s2/v1/download"),
                        ),
                        scopeSessionId = "abc",
                        loadedSections = setOf(WorkspaceSection.Sites),
                    ),
                    onAction = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun tasksPopulatedLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                WorkspaceScreen(
                    state = WorkspaceState(
                        section = WorkspaceSection.Tasks,
                        tasks = listOf(
                            TaskUi("t1", "Fix login", TaskStatus.Doing, "Old bug", listOf(SubtaskUi("st1", "Repro", true), SubtaskUi("st2", "Fix", false)), null),
                        ),
                        scopeSessionId = "abc",
                        loadedSections = setOf(WorkspaceSection.Tasks),
                    ),
                    onAction = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun schedulesPopulatedLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                WorkspaceScreen(
                    state = WorkspaceState(
                        section = WorkspaceSection.Schedules,
                        schedules = listOf(
                            ScheduleUi("sc1", "Morning brief", "Summarize", ScheduleFreq.Daily, "2026-09-25T07:00:00+07:00", "active"),
                        ),
                        scopeSessionId = "abc",
                        loadedSections = setOf(WorkspaceSection.Schedules),
                    ),
                    onAction = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun artifactsPopulatedLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                WorkspaceScreen(
                    state = WorkspaceState(
                        section = WorkspaceSection.Artifacts,
                        artifacts = listOf(
                            ArtifactItemUi("i1", ArtifactType.Image, null, "Chart", "Draw", null, null, null, null),
                            ArtifactItemUi("s1", ArtifactType.Site, null, null, null, "ready", "/api/sites/s1/v1/preview/index.html", "/api/sites/s1/v1/download", 1),
                            ArtifactItemUi("t1", ArtifactType.Task, "Fix login", null, null, "doing", null, null, null),
                        ),
                        scopeSessionId = "abc",
                        loadedSections = setOf(WorkspaceSection.Artifacts),
                    ),
                    onAction = {},
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    private fun previewPng(): ByteArray {
        val output = ByteArrayOutputStream()
        Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(121, 79, 149))
        }.compress(Bitmap.CompressFormat.PNG, 100, output)
        return output.toByteArray()
    }
}
