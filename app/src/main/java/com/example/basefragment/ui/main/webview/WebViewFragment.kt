package com.example.basefragment.ui.main.webview

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.basefragment.ViewModelActivity
import com.example.basefragment.core.base.BaseFragment
import com.example.basefragment.databinding.FragmentWebViewBinding
import com.example.basefragment.ui.main.createPony.ChoosePonyAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


@AndroidEntryPoint
class WebViewFragment : BaseFragment<FragmentWebViewBinding, WebViewViewModel>(
    FragmentWebViewBinding::inflate,
    WebViewViewModel::class.java
) {
    private val mainViewModel: ViewModelActivity by activityViewModels()

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentWebViewBinding = FragmentWebViewBinding.inflate(inflater, container, false)

    @SuppressLint("SetJavaScriptEnabled")
    override fun initView() {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_NO_CACHE
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    // 1. Set language
                    val langCode = when (sharedPreferences.isLanguageKey()) {
                        "vi" -> "vi"
                        "en" -> "en"
                        "de" -> "de"
                        "es" -> "es"
                        "fr" -> "fr"
                        "pt" -> "pt"
                        "hi" -> "hi"
                        "in" -> "in"
                        else -> "en"
                    }
                    view?.evaluateJavascript("setLanguage('$langCode');", null)

                    // 2. Truyền ảnh lên sau khi page load xong
                    loadPhotosToWebView(view)
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) {
                    request.grant(request.resources)
                }
            }

            addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun onGameStarted() {
                    requireActivity().runOnUiThread {
                        binding.exit.visibility = View.VISIBLE
                    }
                }
            }, "Android")

            loadUrl("file:///android_asset/website/index.html")
        }


    }

    private fun loadPhotosToWebView(view: WebView?) {
        // Lấy danh sách ảnh từ ViewModel (characters/avatars)
        val characters = mainViewModel.characters.value
        if (characters.isEmpty()) return


        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val base64List = mutableListOf<String>()

                characters.take(8).forEach { character ->
                    // character.imagePath là đường dẫn ảnh - đổi theo model của bạn
                    val path = character.avatar ?: return@forEach
                    val file = File(path)
                    if (!file.exists()) return@forEach

                    val bytes = file.readBytes()
                    val base64 = android.util.Base64.encodeToString(
                        bytes,
                        android.util.Base64.NO_WRAP
                    )
                    // Detect mime type
                    val mime = when {
                        path.endsWith(".png", true) -> "image/png"
                        path.endsWith(".jpg", true) || path.endsWith(".jpeg", true) -> "image/jpeg"
                        path.endsWith(".webp", true) -> "image/webp"
                        else -> "image/jpeg"
                    }
                    base64List.add("data:$mime;base64,$base64")
                }

                if (base64List.isEmpty()) return@launch

                // Tạo JS array string
                val jsArray = base64List.joinToString(",") { "\"$it\"" }
                val jsCall = "setPhotos([$jsArray]);"

                withContext(Dispatchers.Main) {
                    view?.evaluateJavascript(jsCall, null)
                }
            } catch (e: Exception) {
                android.util.Log.e("WebViewFragment", "Error loading photos: ${e.message}")
            }
        }
    }

    override fun viewListener() {
        binding.exit.setOnClickListener {
            findNavController().popBackStack()
        }
    }
    override fun observeData() {}
    override fun bindViewModel() {}
}