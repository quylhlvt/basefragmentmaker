package com.example.basefragment.data.datalocal.manager.client

import android.content.Context
import android.os.Message
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabsIntent

class OauthWebChromeClient(private val context: Context) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        Log.d("WebView_Progress", "Loading: $newProgress%")
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        Log.d("WebView_Console",
            "[${consoleMessage.messageLevel()}] ${consoleMessage.message()} " +
                    "-- line ${consoleMessage.lineNumber()}"
        )
        return true
    }
}