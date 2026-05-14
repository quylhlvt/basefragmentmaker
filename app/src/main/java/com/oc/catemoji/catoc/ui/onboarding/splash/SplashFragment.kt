
package com.oc.catemoji.catoc.ui.onboarding.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.oc.catemoji.catoc.R
import com.oc.catemoji.catoc.ViewModelActivity
import com.oc.catemoji.catoc.core.base.BackPressHandler
import com.oc.catemoji.catoc.core.base.BaseFragment
import com.oc.catemoji.catoc.core.extention.OuterStrokeShadownTextView
import com.oc.catemoji.catoc.core.extention.dpToPx
import com.oc.catemoji.catoc.core.extention.toIntro
import com.oc.catemoji.catoc.core.extention.toLanguage
import com.oc.catemoji.catoc.core.helper.SharedPreferencesManager.isLanuageScreen
import com.oc.catemoji.catoc.databinding.FragmentSplashBinding
import com.tencent.mmkv.MMKV
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class SplashFragment : BaseFragment<FragmentSplashBinding, SplashViewModel>(
    FragmentSplashBinding::inflate,
    SplashViewModel::class.java
) , BackPressHandler {
    private val mainViewModel: ViewModelActivity by activityViewModels()

    private var progressAnimator: ValueAnimator? = null
    private var currentOverlayFraction = 1f
    private var hasNavigated = false

    companion object {
        private const val MIN_SPLASH_MS  = 3_000L
        private const val API_TIMEOUT_MS = 8_000L
    }

    // ── INIT ──────────────────────────────────────────────────────────────────

    override fun initView() {
        // ✅ Warm up font — giữ nguyên, nhẹ
        ResourcesCompat.getFont(requireContext(), R.font.baloo2_extrabold)


        checkAndClearDataIfNewVersion()


    }
    private fun checkAndClearDataIfNewVersion() {
        val context = requireContext()
        val currentVersion = context.packageManager
            .getPackageInfo(context.packageName, 0).versionCode
        val savedVersion = sharedPreferences.getVersionCode()

        if (savedVersion != currentVersion) {
            // ✅ Xóa MMKV
            MMKV.defaultMMKV().clearAll()

            // ✅ Xóa SharedPreferences (giữ lại language)
            sharedPreferences.clearAll()

            // ✅ Xóa file cache
            context.filesDir.deleteRecursively()
            context.cacheDir.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()

            // ✅ Set lại version SAU khi clear
            sharedPreferences.setVersionCode(currentVersion)

            viewLifecycleOwner.lifecycleScope.launch {
                mainViewModel.forceReloadAll()
            }
        }
    }
    override fun viewListener() {}

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val startTime = System.currentTimeMillis()

            if (isNetworkAvailable()) {
                // Có mạng: đợi imagesReady (set sau khi fetch + prefetch xong)
                withTimeoutOrNull(API_TIMEOUT_MS) {
                    mainViewModel.imagesReady.first { it }
                } ?: Log.e("Splash", "⏰ timeout, dùng local data")
            } else {
                // Không mạng: chỉ đợi local templates
                withTimeoutOrNull(5_000L) {
                    mainViewModel.templates.first { it.isNotEmpty() }
                } ?: Log.e("Splash", "⏰ local timeout")
            }

            val elapsed = System.currentTimeMillis() - startTime
            val remaining = MIN_SPLASH_MS - elapsed
            if (remaining > 0) delay(remaining)

            goToHome()
        }
    }

    override fun bindViewModel() {}

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentSplashBinding = FragmentSplashBinding.inflate(inflater, container, false)

    // ── PROGRESS ──────────────────────────────────────────────────────────────

    // ── NAVIGATE ──────────────────────────────────────────────────────────────

    private fun goToHome() {
        if (hasNavigated) return
        if (!isAdded || isDetached || isRemoving) return
        hasNavigated = true

        if (!isLanuageScreen()) { toLanguage(); return }
        toIntro()
    }

    // ── NETWORK ───────────────────────────────────────────────────────────────

    private fun isNetworkAvailable(): Boolean = try {
        val cm = requireContext().getSystemService(ConnectivityManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    } catch (e: Exception) { false }

    // ── LIFECYCLE ─────────────────────────────────────────────────────────────
    override fun onPause() {
        super.onPause()
        // Dừng animator, giữ nguyên currentOverlayFraction
        progressAnimator?.pause()
    }

    override fun onResume() {
        super.onResume()
        // Chạy tiếp từ chỗ dừng
        progressAnimator?.resume()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        progressAnimator?.cancel()
        progressAnimator = null
    }

    override fun onBackPressed(): Boolean {
        return true
    }
}
