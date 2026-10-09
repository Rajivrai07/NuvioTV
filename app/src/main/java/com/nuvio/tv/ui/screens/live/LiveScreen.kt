package com.nuvio.tv.ui.screens.live

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.ui.theme.NuvioTheme
import java.io.ByteArrayInputStream

/** Smartcric live cricket site, shown inside the app. */
private const val SMARTCRIC_URL = "https://smartcric.is/"

/** Known ad / tracker / popup domains blocked at network level. */
private val BLOCKED_DOMAINS = listOf(
    "doubleclick.net",
    "googlesyndication.com",
    "googleadservices.com",
    "google-analytics.com",
    "adservice.google.com",
    "pagead2.googlesyndication.com",
    "popads.net",
    "popcash.net",
    "adcash.com",
    "propellerads.com",
    "adsterra.com",
    "exoclick.com",
    "hilltopads.net",
    "clickadu.com",
    "mgid.com",
    "revcontent.com",
    "outbrain.com",
    "taboola.com",
    "facebook.net",
    "hotjar.com",
    "criteo.com",
)

private fun isAdUrl(url: String): Boolean {
    val lower = url.lowercase()
    return BLOCKED_DOMAINS.any { lower.contains(it) }
}

/**
 * Removes ad overlays, banners, iframes and hijacks popup attempts.
 * Re-runs on DOM mutations so dynamically injected ads are cleaned too.
 */
private const val AD_CLEANUP_JS = """(function() {
  window.open = function() { return null; };
  var selectors = [
    'iframe[src*="doubleclick"]', 'iframe[src*="googlesyndication"]',
    'iframe[src*="adsterra"]', 'iframe[src*="popads"]', 'iframe[src*="popcash"]',
    'iframe[src*="propellerads"]', 'iframe[src*="exoclick"]', 'iframe[src*="hilltopads"]',
    'div[id*="banner-ad"]', 'div[class*="banner-ad"]', 'div[id*="popup"]',
    'div[class*="popup-overlay"]', '.ad-container', '#ad-container', '.advertisement'
  ];
  function clean() {
    try {
      selectors.forEach(function(s) {
        document.querySelectorAll(s).forEach(function(el) { el.remove(); });
      });
    } catch (e) {}
  }
  clean();
  try {
    new MutationObserver(clean).observe(document.documentElement, { childList: true, subtree: true });
  } catch (e) {}
  var n = 0;
  var t = setInterval(function() { clean(); if (++n > 10) clearInterval(t); }, 1000);
})();"""

/**
 * LIVE screen: Smartcric live cricket inside the app with a full ad blocker.
 *
 * D-pad friendly: the WebView takes focus so the TV remote can navigate the
 * site; remote Back goes back inside the WebView first, then exits the screen.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveScreen(
    showBuiltInHeader: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var pageLoading by remember { mutableStateOf(true) }
    val webViewFocusRequester = remember { FocusRequester() }
    val reloadFocusRequester = remember { FocusRequester() }

    // Remote Back: go back inside the WebView when possible, otherwise let
    // navigation handle it (exit the LIVE screen).
    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NuvioTheme.colors.Background),
    ) {
        if (showBuiltInHeader) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.LiveTv,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "LIVE",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NuvioTheme.colors.TextPrimary,
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { webViewRef?.reload() },
                    colors = ButtonDefaults.colors(
                        containerColor = NuvioTheme.colors.SurfaceVariant,
                        contentColor = NuvioTheme.colors.TextPrimary,
                    ),
                    modifier = Modifier.focusRequester(reloadFocusRequester),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Reload")
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(webViewFocusRequester),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        // Keep popups inside this view and block new windows.
                        settings.setSupportMultipleWindows(false)
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        isFocusable = true
                        isFocusableInTouchMode = true

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): WebResourceResponse? {
                                val reqUrl = request?.url?.toString().orEmpty()
                                if (isAdUrl(reqUrl)) {
                                    return WebResourceResponse(
                                        "text/plain", "utf-8", 200, "OK",
                                        mutableMapOf(), ByteArrayInputStream(ByteArray(0)),
                                    )
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: android.graphics.Bitmap?,
                            ) {
                                super.onPageStarted(view, url, favicon)
                                pageLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                pageLoading = false
                                canGoBack = view?.canGoBack() == true
                                view?.evaluateJavascript(AD_CLEANUP_JS, null)
                            }

                            override fun doUpdateVisitedHistory(
                                view: WebView?,
                                url: String?,
                                isReload: Boolean,
                            ) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                canGoBack = view?.canGoBack() == true
                            }
                        }
                        webChromeClient = WebChromeClient()
                        loadUrl(SMARTCRIC_URL)
                        webViewRef = this
                    }
                },
                update = { view ->
                    webViewRef = view
                },
            )

            // Give the WebView D-pad focus once the page is ready so the
            // remote can navigate the site immediately.
            LaunchedEffect(pageLoading) {
                if (!pageLoading) {
                    try {
                        webViewFocusRequester.requestFocus()
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }
}
