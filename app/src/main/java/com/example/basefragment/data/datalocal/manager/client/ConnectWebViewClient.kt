package com.example.basefragment.data.datalocal.manager.client

import android.content.Context
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader

class ConnectWebViewClient(
    private val assetLoader: WebViewAssetLoader
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val response = assetLoader.shouldInterceptRequest(request.url)
            ?: return null

        // ✅ Fix MIME type - đây là nguyên nhân chính WASM bị dừng
        val url = request.url.toString()
        return when {
            url.endsWith(".wasm") -> WebResourceResponse(
                "application/wasm",
                "binary",
                response.data
            )
            url.endsWith(".pck") -> WebResourceResponse(
                "application/octet-stream",
                "binary",
                response.data
            )
            url.endsWith(".js") -> WebResourceResponse(
                "application/javascript",
                "utf-8",
                response.data
            )
            url.endsWith(".data") -> WebResourceResponse(
                "application/octet-stream",
                "binary",
                response.data
            )
            else -> response
        }
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        super.onReceivedError(view, request, error)
        Log.e("WebViewClient", "Error: ${error.description} | URL: ${request.url}")
    }
}