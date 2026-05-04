package com.example.basefragment.ui.language

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
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
import kotlinx.coroutines.launch

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
        // Set isFromSetting trước khi dùng
        isFromSetting = findNavController().previousBackStackEntry?.destination?.id == R.id.setting

        binding.apply {
            actionBar.apply {
                btnActionBarRight.gone()
                btnActionBarLeft.setImageResource(R.drawable.back_app)
            }
            layoutTitle.apply {
                txtLang.isSelected =true
            }


        }
        initRcv()

        val checkFirst =
            SharedPreferencesManager.isLanuageScreen()
        val keyLanguage =
            SharedPreferencesManager.isLanguageKey()
        val currentLang = keyLanguage

        viewModel.setFirstLanguage(isFirst = !checkFirst)
        viewModel.loadLanguages(currentLang)
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isFirstLanguage.collect { isFirst ->
                        if (isFirst) {
                            binding.actionBar.apply {
//                                tvStart.visible()

                                btnActionBarRight.invisible()
                                btnActionBarRight.setImageResource(R.drawable.select_language)
                            }
                        } else {
                            binding.apply {
                                actionBar.apply {
                                    btnActionBarLeft.visible()
//                                tvCenter.visible()
                                    btnActionBarRight.setImageResource(R.drawable.select_language)
                                }
                                imageBgLang.setImageResource(R.drawable.img_bg_home)
                            }
                        }
                    }
                }
                launch {
                    viewModel.languageList.collect { list ->
                        Log.d("LANG", "Updating adapter with list size=${list.size}")
                        languageAdapter.submitList(list)
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