package co.ratmo.anreal

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import co.ratmo.anreal.feature.auth.presentation.BoardingRoute
import co.ratmo.anreal.feature.auth.presentation.LoginRoute
import co.ratmo.anreal.feature.auth.presentation.RegisterRoute
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SharedLogicAndroidHostTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun auth_forms_replace_current_destination_without_revealing_boarding() {
        lateinit var navController: NavController
        composeRule.setContent {
            navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = BoardingRoute,
            ) {
                composable<BoardingRoute> {}
                composable<LoginRoute> {}
                composable<RegisterRoute> {}
            }
        }

        composeRule.runOnIdle {
            navController.replaceCurrentWith(LoginRoute())
        }
        composeRule.runOnIdle {
            assertTrue(navController.currentDestination?.hasRoute<LoginRoute>() == true)
            assertFalse(navController.popBackStack())
            navController.replaceCurrentWith(RegisterRoute())
        }
        composeRule.runOnIdle {
            assertTrue(navController.currentDestination?.hasRoute<RegisterRoute>() == true)
            assertFalse(navController.popBackStack())
        }
    }
}
