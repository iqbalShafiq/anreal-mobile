package co.ratmo.anreal

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val splashBars = SystemBarStyle.dark(Color.TRANSPARENT)
        enableEdgeToEdge(
            statusBarStyle = splashBars,
            navigationBarStyle = splashBars,
        )

        setContent {
            App(buildInfo = AppBuildInfo(versionName = BuildConfig.VERSION_NAME))
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}