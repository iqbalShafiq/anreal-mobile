package co.ratmo.anreal.feature.chat.presentation.component

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

internal actual @Composable fun SitePreviewWebView(url: String, modifier: Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.domStorageEnabled = false
                settings.mediaPlaybackRequiresUserGesture = true
            }
        },
        update = { webView ->
            // Public preview URL: never attach auth headers or cookies.
            webView.loadUrl(url)
        },
        modifier = modifier,
    )
}
