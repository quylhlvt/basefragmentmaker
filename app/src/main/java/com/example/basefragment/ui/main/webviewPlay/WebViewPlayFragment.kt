package com.example.basefragment.ui.main.webviewPlay

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import androidx.activity.OnBackPressedCallback
import androidx.webkit.WebViewAssetLoader
import com.example.basefragment.MainActivity
import com.example.basefragment.R
import com.example.basefragment.core.base.BaseFragment
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.popBack
import com.example.basefragment.core.extention.select
import com.example.basefragment.core.extention.setImageActionBar
import com.example.basefragment.core.extention.setTextActionBar
import com.example.basefragment.core.extention.visible
import com.example.basefragment.data.datalocal.manager.client.ConnectWebViewClient
import com.example.basefragment.data.datalocal.manager.client.OauthWebChromeClient
import com.example.basefragment.databinding.FragmentSuccessCosplayBinding
import com.example.basefragment.databinding.FragmentWebViewPlayBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WebViewPlayFragment : BaseFragment<FragmentWebViewPlayBinding, WebViewViewModel>(
    FragmentWebViewPlayBinding::inflate, WebViewViewModel::class.java
) {
    // ✅ Fix: lazy để tránh gọi requireActivity() trước khi attach
    private val assetLoader by lazy {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(requireContext()))
            .build()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { popBack() }
            }
        )
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentWebViewPlayBinding = FragmentWebViewPlayBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.webView.apply {
            settings.apply {
                userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Mobile Safari/537.36"
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true

                // ✅ THÊM 3 dòng này - quan trọng cho WASM
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = true
                @Suppress("DEPRECATION")
                allowUniversalAccessFromFileURLs = true
                mediaPlaybackRequiresUserGesture = false

                setSupportMultipleWindows(true)
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                loadWithOverviewMode = true
                useWideViewPort = true
                setInitialScale(1)
            }

            addJavascriptInterface(AndroidBridge(this@WebViewPlayFragment), "AndroidBridge")
            webViewClient = ConnectWebViewClient(assetLoader)
            webChromeClient = OauthWebChromeClient(requireContext())
            loadUrl("https://appassets.androidplatform.net/assets/cat/index.html")
        }
    }

    override fun viewListener() {
        binding.home.onClick { popBack() }
    }

    fun showPlayButton() {
        if (isAdded) binding.home.visible()  // ✅ guard tránh crash khi Fragment detach
    }
    fun hidePlayButton() {
        if (isAdded) binding.home.gone()
    }

    override fun observeData() {}
    override fun bindViewModel() {}
}

class AndroidBridge(private val fragment: WebViewPlayFragment) {

    @JavascriptInterface
    fun onGameReady() {
        fragment.activity?.runOnUiThread {  // ✅ dùng activity? thay vì requireActivity() tránh crash
            fragment.showPlayButton()
        }
    }

    @JavascriptInterface
    fun onScreenChanged(screen: String) {
        fragment.activity?.runOnUiThread {
            when (screen) {
                "game"  -> fragment.showPlayButton()
                "other" -> fragment.hidePlayButton()
            }
        }
    }
}