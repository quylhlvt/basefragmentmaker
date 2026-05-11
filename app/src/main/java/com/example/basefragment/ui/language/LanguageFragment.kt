package com.example.basefragment.ui.language

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.basefragment.R
import com.example.basefragment.core.base.BackPressHandler
import com.example.basefragment.core.base.BaseFragment
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.invisible
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.popBack
import com.example.basefragment.core.extention.toHomeFromLanguage
import com.example.basefragment.core.extention.toIntroFromLanguage
import com.example.basefragment.core.extention.toSettingFromLang
import com.example.basefragment.core.extention.visible
import com.example.basefragment.core.helper.LanguageHelper.setLocale
import com.example.basefragment.core.helper.SharedPreferencesManager
import com.example.basefragment.core.helper.SharedPreferencesManager.isLanguageKey
import com.example.basefragment.core.helper.SharedPreferencesManager.isLanuageScreen
import com.example.basefragment.core.helper.SharedPreferencesManager.sharedPreferences
import com.example.basefragment.databinding.FragmentLanguageBinding
import com.example.basefragment.utils.DataLocal
import com.example.basefragment.utils.LanguageManager.updateLanguage
import com.example.basefragment.utils.key.IntentKey
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class LanguageFragment : BaseFragment<FragmentLanguageBinding, LanguageViewModel>(
    FragmentLanguageBinding::inflate, LanguageViewModel::class.java
), BackPressHandler {
    private val languageAdapter by lazy { LanguageAdapter(requireContext()) }
    private var isFromSetting = false

    override fun onBackPressed(): Boolean {

        when {
            isFromSetting -> {
                toSettingFromLang()
            }
            isLanuageScreen() -> {
                popBack()
            }
            else -> {
                requireActivity().finish()
            }
        }
        return true
    }
    override fun setupPreViews() {
        // ❌ Xóa Glide load background — đã set android:src trong XML rồi
        // Chỉ update nếu cần đổi bg runtime
        val isFirst = !SharedPreferencesManager.isLanuageScreen()
        if (!isFirst) {
            // Chỉ đổi khi không phải lần đầu
            binding.imageBgLang.setImageResource(R.drawable.img_bg_home)
        }

        binding.recycleLanguage.apply {
            adapter = languageAdapter
            itemAnimator = null
            background = ContextCompat.getDrawable(requireContext(), R.drawable.img_bg_rcy_lang)
        }

        val currentLang = SharedPreferencesManager.isLanguageKey()
        viewModel.setFirstLanguage(isFirst = isFirst)
        viewModel.loadLanguages(currentLang)

        val list = viewModel.languageList.value
        if (list.isNotEmpty()) {
            languageAdapter.submitList(list)
        }
    }
    private fun updateActionBar(isFirst: Boolean) {
        binding.apply {
            if (isFirst) {
                actionBar.btnActionBarRight.invisible()
                actionBar.btnActionBarRight.setImageResource(R.drawable.select_language)
            } else {
                actionBar.btnActionBarLeft.visible()
                actionBar.btnActionBarRight.setImageResource(R.drawable.select_language)
                // ❌ Xóa Glide — dùng setImageResource trực tiếp
                imageBgLang.setImageResource(R.drawable.img_bg_home)
            }
        }
    }
    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarRight.onClick {
                handleDone()
            }
            actionBar.btnActionBarLeft.onClick( 500) {
                // Dùng chung logic với onBackPressed
                onBackPressed()
            }
        }
        handleRcv()
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): FragmentLanguageBinding = FragmentLanguageBinding.inflate(inflater, container, false)

    override fun initView() {
        isFromSetting = findNavController().previousBackStackEntry?.destination?.id == R.id.setting

        binding.actionBar.apply {
            btnActionBarRight.gone()
            btnActionBarLeft.setImageResource(R.drawable.back_app)
        }
        binding.layoutTitle.txtLang.isSelected = true

        // ✅ Bỏ initRcv() — đã làm trong setupPreViews
        updateActionBar(viewModel.isFirstLanguage.value)
    }


    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isFirstLanguage.collect { isFirst ->
                        updateActionBar(isFirst)
                    }
                }
                launch {
                    viewModel.languageList.collect { list ->
                        if (list.isNotEmpty()) {
                            languageAdapter.submitList(list)
                        }
                    }
                }
                launch {
                    viewModel.codeLang.collect { code ->
                        if (code.isNotEmpty()) {
                            binding.actionBar.btnActionBarRight.visible()
                        }
                    }
                }
            }
        }
    }



    override fun bindViewModel() {
    }

    private fun initRcv() {
        binding.recycleLanguage.apply {
            adapter = languageAdapter
            itemAnimator = null
            post {
                background = ContextCompat.getDrawable(requireContext(), R.drawable.img_bg_rcy_lang)
            }
        }
    }
    private fun handleRcv() {
        binding.apply {
            languageAdapter.onItemClick = { code ->
                binding.actionBar.btnActionBarRight.visible()
                viewModel.selectLanguage(code)
            }
        }
    }

    private fun handleDone() {
        val code = viewModel.codeLang.value
        if (code.isEmpty()) {
            showToast(R.string.not_select_lang)
            return
        }

        sharedPreferences.setLanguageKey(code)
        setLocale(requireContext(), code)
       updateLanguage(code)

        if (viewModel.isFirstLanguage.value) {
            sharedPreferences.setLanuageScreen(true)
            Log.d("LANG", "Navigating to Intro")
            toIntroFromLanguage()
        } else {
            Log.d("LANG", "Navigating to Home")
            toHomeFromLanguage()
        }
    }
}