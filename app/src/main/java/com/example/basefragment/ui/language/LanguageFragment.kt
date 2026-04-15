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
import com.example.basefragment.core.base.BaseFragment
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.toHomeFromLanguage
import com.example.basefragment.core.extention.toIntroFromLanguage
import com.example.basefragment.core.extention.visible
import com.example.basefragment.core.helper.SharedPreferencesManager.isLanguageKey
import com.example.basefragment.core.helper.SharedPreferencesManager.isLanuageScreen
import com.example.basefragment.core.helper.SharedPreferencesManager.sharedPreferences
import com.example.basefragment.databinding.FragmentLanguageBinding
import com.example.basefragment.utils.DataLocal
import com.example.basefragment.utils.key.IntentKey
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LanguageFragment : BaseFragment<FragmentLanguageBinding, LanguageViewModel>(
    FragmentLanguageBinding::inflate, LanguageViewModel::class.java
) {
    private val languageAdapter by lazy { LanguageAdapter(requireContext()) }

    override fun viewListener() {
        binding.actionBar.btnActionBarRight.onClick {
            handleDone()
        }
        handleRcv()
    }


    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): FragmentLanguageBinding = FragmentLanguageBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.actionBar.apply {
            btnActionBarLeft.setImageResource(R.drawable.back_app)
            btnActionBarRight.setImageResource(R.drawable.select_language)
        }
        initRcv()

        val checkFirst = isLanuageScreen()
        val keyLanguage = isLanguageKey()
        val currentLang = keyLanguage

        viewModel.setFirstLanguage(isFirst = !checkFirst)
        viewModel.loadLanguages(currentLang)
//            btnActionBarRight.apply {
//                visible()
//                setImageResource(R.drawable.select_language)
//            }
//            if (!isLanuageScreen()) {
//                tvStart.visible()
//                return
//            }
//            tvCenter.visible()


//        binding.textView.text = "Home Fragment"
//        binding.btnTest.setOnClickListener {
//            showSnackbar("Xin chào từ Home!")
//
    }

    override fun observeData() {

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {


                launch {
                    viewModel.isFirstLanguage.collect { isFirst ->
                        if (isFirst) {
                            binding.actionBar.tvStart.visible()
                        } else {
                            binding.actionBar.btnActionBarLeft.visible()
                            binding.actionBar.tvCenter.visible()
                        }
                    }
                }
                launch {
                    viewModel.languageList.collect { list ->
                        Log.d("LANG", "Updating adapter with list size=${list.size}") // check log
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
//        viewModel.data.observe(viewLifecycleOwner) { text ->
//            binding.textView.text = text
//        }
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

        if (viewModel.isFirstLanguage.value) {
            sharedPreferences.setLanuageScreen(true)
            Log.d("LANGa", "Set language screen = true, navigating to Intro")

            toIntroFromLanguage()
        } else {
            Log.d("LANGa", "Navigating to Home")

            toHomeFromLanguage()
        }
    }
}