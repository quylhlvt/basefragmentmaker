package com.example.basefragment.ui.onboarding.intro

import android.content.Intent
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.example.basefragment.R
import com.example.basefragment.core.base.BackPressHandler
import com.example.basefragment.core.base.BaseFragment
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.toHome
import com.example.basefragment.core.extention.toIntro
import com.example.basefragment.core.extention.toPermission
import com.example.basefragment.core.helper.SharedPreferencesManager.isPermissionScreen
import com.example.basefragment.databinding.FragmentIntroBinding
import com.example.basefragment.databinding.FragmentPermissionBinding
import com.example.basefragment.ui.onboarding.permission.PermissionViewModel
import com.example.basefragment.utils.DataLocal
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.text.compareTo

@AndroidEntryPoint
class IntroFragment : BaseFragment<FragmentIntroBinding, IntroViewModel>(
    FragmentIntroBinding::inflate,
    IntroViewModel::class.java
), BackPressHandler  {
    @Inject
    lateinit var introAdapter: IntroAdapter

    override fun viewListener() {
        binding.btnNextPager.root.setOnClickListener {
            viewModel.nextPage(binding.viewPager2.currentItem, introAdapter.itemCount)
        }

//        binding.viewPager2.registerOnPageChangeCallback(object :
//            ViewPager2.OnPageChangeCallback() {
//            override fun onPageSelected(position: Int) {
//                super.onPageSelected(position)
//                if (position == 1) {
//                    binding.nativeAds.gone()
//                } else {
//                    binding.nativeAds.visible()
//                }
//            }
//        })
    }


    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentIntroBinding = FragmentIntroBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.viewPager2.adapter = introAdapter
        binding.dotsIndicator.attachTo(binding.viewPager2)
        setOnChangeViewPager2()
//        binding.textView.text = "Home Fragment"
//        binding.btnTest.setOnClickListener {
//            showSnackbar("Xin chào từ Home!")
//        }
    }

    override fun observeData() {
//        viewModel.data.observe(viewLifecycleOwner) { text ->
//            binding.textView.text = text
//        }
    }

    override fun bindViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                introAdapter.submitList(state.pagesSplash)
                binding.apply {
                    viewPager2.currentItem = state.page
                    btnNextPager.tvButton.text = getString(state.textButtonRes)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.singleEvent.collect { event ->
                when (event) {
                    is IntroSingleEvent.NavigateToNextScreen ->
                        if (sharedPreferences.isPermissionScreen())
                            toHome()
                        else
                            toPermission()
                }
            }
        }
    }


    private fun setOnChangeViewPager2() {
        binding.viewPager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                viewModel.getPage(binding.viewPager2.currentItem, introAdapter.itemCount)
            }
        })
    }

    override fun onBackPressed(): Boolean {
        requireActivity().finish()
        return  true
    }
}