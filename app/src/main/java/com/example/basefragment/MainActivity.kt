package com.example.basefragment

import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.Glide
import com.example.basefragment.core.base.BackPressHandler
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.hideNavigation
import com.example.basefragment.core.extention.visible
import com.example.basefragment.core.helper.SharedPreferencesManager
import com.example.basefragment.databinding.DialogbaseBinding
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
interface LoadingController {
    fun showGlobalLoading()
    fun hideGlobalLoading()
    fun showGlobalConfirmDialog(
        message: String,
        title: String? = null,
        onYes: () -> Unit,
        onNo: (() -> Unit)? = null
    )
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() , LoadingController{
    private var globalLoadingDialog: Dialog? = null
    private var globalConfirmDialog: Dialog? = null

    override fun showGlobalLoading() {
        if (globalLoadingDialog?.isShowing == true) return
        runOnUiThread {
            globalLoadingDialog = buildDialog(
                message = getString(R.string.loading),
                showButtons = false,
                cancelable = false
            )
            globalLoadingDialog?.show()
            hideNavigation(true)
        }
    }

    override fun hideGlobalLoading() {
        val stack = Thread.currentThread().stackTrace
            .take(8).joinToString("\n") { it.toString() }
        android.util.Log.e("LOADING", "hideGlobalLoading called!\n$stack")

        runOnUiThread {
            globalLoadingDialog?.dismiss()
            globalLoadingDialog = null
            hideNavigation(true)
        }
    }

    override fun showGlobalConfirmDialog(
        message: String,
        title: String?,
        onYes: () -> Unit,
        onNo: (() -> Unit)?
    ) {
        runOnUiThread {
            globalConfirmDialog?.dismiss()
            globalConfirmDialog = buildDialog(
                message = message,
                title = title,
                showButtons = true,
                cancelable = true,
                onYes = {
                    globalConfirmDialog?.dismiss()
                    globalConfirmDialog = null
                    onYes()
                },
                onNo = {
                    globalConfirmDialog?.dismiss()
                    globalConfirmDialog = null
                    onNo?.invoke()
                }
            )
            globalConfirmDialog?.show()
            hideNavigation(true)
        }
    }

    // Builder dùng chung
    private fun buildDialog(
        message: String,
        title: String? = null,
        showButtons: Boolean = false,
        cancelable: Boolean = false,
        onYes: (() -> Unit)? = null,
        onNo: (() -> Unit)? = null
    ): Dialog {
        return Dialog(this, R.style.BaseDialog).apply {
            val binding = DialogbaseBinding.inflate(layoutInflater)
            setContentView(binding.root)

            title?.let { binding.txtTitle.text = it }
            binding.txtContent.text = message

            if (showButtons) {
                binding.btnYes.visible()
                binding.btnNo.visible()
                binding.btnYes.setOnClickListener { onYes?.invoke() }
                binding.btnNo.setOnClickListener { onNo?.invoke() }
            } else {
                binding.btnYes.gone()
                binding.btnNo.gone()
            }

            setCancelable(cancelable)
            window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                setGravity(Gravity.CENTER)
            }
        }
    }

    private fun preloadHomeDrawables() {
        val resIds = listOf(
            R.drawable.img_bg_home,
            R.drawable.img_title_home,
            R.drawable.img_bg_home1,
            R.drawable.img_avatar1,
            R.drawable.img_avatar2,
            R.drawable.img_avatar3,
            R.drawable.img_avatar4,
        )
        // Glide preload thực sự decode và cache bitmap
        // chạy background tự động, không cần Dispatchers.IO
        resIds.forEach { resId ->
            Glide.with(applicationContext)
                .load(resId)
                .preload()
        }
    }
    private lateinit var navController: NavController
    private val mainViewModel: ViewModelActivity by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        hideNavigation(true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initSharedPreferences()
        preloadHomeDrawables()
        applyLanguage()
//        val sharedPrefs = getSharedPreferences("DEFAULT", Context.MODE_PRIVATE)
//        SharedPreferencesManager.sharedPreferences = sharedPrefs
//        SharedPreferencesManager.editor = sharedPrefs.edit()

        // Lấy NavController từ NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Kiểm tra xem Fragment hiện tại có xử lý back không
                val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment

                if (currentFragment is BackPressHandler) {
                    val handled = currentFragment.onBackPressed()
                    if (handled) return
                }

                // Fragment không xử lý hoặc không implement interface
                if (!navController.popBackStack()) {
                    finish()
                }
            }
        })
        // Tuỳ chọn: ẩn thanh trạng thái hoặc làm gì đó
        // window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }
    private fun initSharedPreferences() {
        val sharedPrefs = getSharedPreferences("DEFAULT", Context.MODE_PRIVATE)
        SharedPreferencesManager.sharedPreferences = sharedPrefs
        SharedPreferencesManager.editor = sharedPrefs.edit()
    }
    private fun applyLanguage() {
        val savedLanguage = SharedPreferencesManager.isLanguageKey()
        if (savedLanguage.isNotEmpty()) {
            val locale = Locale(savedLanguage)
            Locale.setDefault(locale)

            val config = Configuration(resources.configuration)
            config.setLocale(locale)

            // ✅ QUAN TRỌNG: Update configuration
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // ✅ Re-apply language khi xoay màn hình
        applyLanguage()
    }

//    // QUAN TRỌNG: xử lý nút Back đúng cách
//    override fun onBackPressed() {
//        if (!navController.popBackStack()) {
//            // Không còn gì trong back stack → thoát app
//            super.onBackPressed()
//        }
//    }

    // Nếu bạn dùng Toolbar + NavigationIcon (mũi tên back)
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()
        hideNavigation(true)
    }
}