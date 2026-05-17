package com.example.basefragment.ui.main.webview

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.webkit.WebViewAssetLoader
import com.example.basefragment.ViewModelActivity
import com.example.basefragment.core.base.BaseFragment
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.popBack
import com.example.basefragment.core.extention.visible
import com.example.basefragment.data.datalocal.manager.client.ConnectWebViewClient
import com.example.basefragment.data.datalocal.manager.client.OauthWebChromeClient
import com.example.basefragment.databinding.FragmentWebViewBinding
import com.example.basefragment.ui.main.createPony.ChoosePonyAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.getValue


@AndroidEntryPoint
class WebViewFragment : BaseFragment<FragmentWebViewBinding, WebViewViewModel>(
    FragmentWebViewBinding::inflate,
    WebViewViewModel::class.java
) {
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
    ): FragmentWebViewBinding = FragmentWebViewBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true

                allowFileAccess = false
                allowContentAccess = true

                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

                setSupportMultipleWindows(false)
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false

                loadWithOverviewMode = false
                useWideViewPort = false

                cacheMode = WebSettings.LOAD_DEFAULT
            }

            webViewClient = ConnectWebViewClient(assetLoader)
            webChromeClient = OauthWebChromeClient(requireContext())
            loadUrl("https://appassets.androidplatform.net/assets/cat/index.html")
        }
    }

    override fun viewListener() {
        binding.exit.onClick {
            binding.webView.apply {
                evaluateJavascript(
                    """
                document.querySelectorAll('audio, video').forEach(e => e.pause());
                """.trimIndent(),
                    null
                )
                stopLoading()
                loadUrl("about:blank")
                onPause()
                pauseTimers()
            }
            popBack()
        }
    }
    override fun onPause() {
        binding.webView.onPause()
        binding.webView.pauseTimers()
        binding.webView.evaluateJavascript(
            """
        if (window.AudioContext) {
            document.querySelectorAll('audio, video').forEach(e => {
                e.pause();
                e.currentTime = 0;
            });
        }
        """.trimIndent(),
            null
        )
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
        binding.webView.resumeTimers()
    }

    override fun onDestroyView() {
        binding.webView.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            onPause()
            pauseTimers()
            destroy()
        }
        super.onDestroyView()
    }
    fun showPlayButton() {
        if (isAdded) binding.exit.visible()  // ✅ guard tránh crash khi Fragment detach
    }
    fun hidePlayButton() {
        if (isAdded) binding.exit.gone()
    }

    override fun observeData() {}
    override fun bindViewModel() {}
}
class AndroidBridge(private val fragment: WebViewFragment) {

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