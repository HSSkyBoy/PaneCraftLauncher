package com.tungsten.fcl.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mio.util.openLink
import com.tungsten.fcl.R
import com.tungsten.fcl.game.OAuthServer
import com.tungsten.fcl.setting.Accounts
import com.tungsten.fcllibrary.component.FCLActivity
import com.tungsten.fcllibrary.component.theme.ThemeEngine
import top.nkbe.pclf.theme.LauncherTheme
import top.nkbe.pclf.ui.components.LauncherButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import java.util.function.Consumer

class WebActivity : FCLActivity() {

    private var webViewRef: WebView? = null
    private var loading by mutableStateOf(true)
    private var currentUrl by mutableStateOf("")
    private var loginCompletedListener: Consumer<OAuthServer.LoginCompletedDeviceCodeEvent>? = null
    private var loginFinishedListener: Consumer<OAuthServer.LoginFinishedEvent>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialUrl = intent.extras?.getString("url").orEmpty()
        currentUrl = initialUrl

        loginCompletedListener = Consumer { runOnUiThread { finish() } }
        Accounts.OAUTH_CALLBACK.onLoginCompletedDeviceCode.register(loginCompletedListener)
        loginFinishedListener = Consumer { runOnUiThread { finish() } }
        Accounts.OAUTH_CALLBACK.onLoginFinished.register(loginFinishedListener)

        val seedColor = runCatching { ThemeEngine.getInstance().getTheme().getColor() }
            .getOrDefault(0xFF1976D2.toInt())
        val isDark = runCatching { ThemeEngine.isNightMode(this) }.getOrDefault(false)

        setContent {
            LauncherTheme(seedColorInt = seedColor, darkTheme = isDark) {
                val colors = LauncherTheme.colors
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.canvasTint)
                        .systemBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(colors.titleBarStart, colors.titleBarEnd)))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LauncherButton(
                            text = stringResource(R.string.button_back),
                            onClick = { finish() },
                            accentStyle = false,
                            height = 28.dp
                        )
                        Text(
                            text = currentUrl.ifBlank { "Web" },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        LauncherButton(
                            text = stringResource(R.string.action_refresh),
                            onClick = { webViewRef?.reload() },
                            accentStyle = false,
                            height = 28.dp
                        )
                        if (currentUrl.isNotBlank()) {
                            LauncherButton(
                                text = "Browser",
                                onClick = { openLink(this@WebActivity, currentUrl) },
                                accentStyle = false,
                                height = 28.dp
                            )
                        }
                    }

                    if (loading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            loading = true
                                            if (url != null) {
                                                currentUrl = url
                                                if (isOAuthCallback(url)) {
                                                    finish()
                                                }
                                            }
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            loading = false
                                            if (url != null) currentUrl = url
                                        }
                                    }
                                    webViewRef = this
                                    if (initialUrl.isNotBlank()) {
                                        loadUrl(initialUrl)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val url = intent.extras?.getString("url")
        if (!url.isNullOrBlank()) {
            currentUrl = url
            webViewRef?.loadUrl(url)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        loginCompletedListener?.let {
            Accounts.OAUTH_CALLBACK.onLoginCompletedDeviceCode.unregister(it)
        }
        loginFinishedListener?.let {
            Accounts.OAUTH_CALLBACK.onLoginFinished.unregister(it)
        }
        webViewRef?.destroy()
        webViewRef = null
    }

    companion object {
        private fun isOAuthCallback(url: String): Boolean {
            val uri = Uri.parse(url)
            val host = uri.host
            return "/auth-response" == uri.path && ("localhost" == host || "127.0.0.1" == host)
        }
    }
}
