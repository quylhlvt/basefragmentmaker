
package com.example.basefragment.ui.onboarding.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.basefragment.ViewModelActivity
import com.example.basefragment.core.base.BaseFragment
import com.example.basefragment.core.extention.toIntro
import com.example.basefragment.core.extention.toLanguage
import com.example.basefragment.core.helper.SharedPreferencesManager.isLanuageScreen
import com.example.basefragment.databinding.FragmentSplashBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class SplashFragment : BaseFragment<FragmentSplashBinding, SplashViewModel>(
    FragmentSplashBinding::inflate,
    SplashViewModel::class.java
) {
    private val mainViewModel: ViewModelActivity by activityViewModels()

    companion object {
        private const val MIN_SPLASH_MS  = 2_000L   // splash tối thiểu
        private const val API_TIMEOUT_MS = 8_000L   // timeout chờ API + preload
    }

    override fun initView() {
        viewLifecycleOwner.lifecycleScope.launch {
            val startTime = System.currentTimeMillis()

            if (isNetworkAvailable()) {
                // Có mạng: đợi API + preload avatar xong trong GetCatalogueUseCase
                // GetCatalogueUseCase.invoke() sẽ preloadAvatarsParallel() trước khi return
                // → templates Flow emit "online_*" khi cả API lẫn preload đều done
                withTimeoutOrNull(API_TIMEOUT_MS) {
                    mainViewModel.templates.first { list ->
                        list.any { it.id.startsWith("online_") }
                    }
                }
                // null = timeout → navigate với local data, không sao
            }

            // Đảm bảo splash tối thiểu MIN_SPLASH_MS
            val elapsed = System.currentTimeMillis() - startTime
            val remaining = MIN_SPLASH_MS - elapsed
            if (remaining > 0) kotlinx.coroutines.delay(remaining)

            goToHome()
        }
    }

    private fun isNetworkAvailable(): Boolean = try {
        val cm = requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
                as android.net.ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (e: Exception) { false }

    private fun goToHome() {
        if (!isLanuageScreen()) { toLanguage(); return }
        toIntro()
    }

    override fun viewListener() {}
    override fun observeData() {}
    override fun bindViewModel() {}

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentSplashBinding = FragmentSplashBinding.inflate(inflater, container, false)
}
 