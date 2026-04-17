
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
                withTimeoutOrNull(API_TIMEOUT_MS) {
                    // ✅ Kiểm tra giá trị HIỆN TẠI của StateFlow trước
                    val currentList = mainViewModel.templates.value
                    if (currentList.none { it.id.startsWith("online_") }) {
                        // Chưa có data online → mới chờ
                        mainViewModel.templates.first { list ->
                            list.any { it.id.startsWith("online_") }
                        }
                    }
                    // Nếu đã có data rồi → không chờ nữa
                }
            }

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
        // ✅ Tránh navigate khi Fragment đã detach
        if (!isAdded || isDetached || isRemoving) return

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
 