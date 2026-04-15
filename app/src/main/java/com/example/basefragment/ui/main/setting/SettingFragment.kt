package com.example.basefragment.ui.main.setting

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.basefragment.R
import com.example.basefragment.core.base.BaseFragment
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.policy
import com.example.basefragment.core.extention.select
import com.example.basefragment.core.extention.setImageActionBar
import com.example.basefragment.core.extention.setTextActionBar
import com.example.basefragment.core.extention.shareApp
import com.example.basefragment.core.extention.toHomeFromSetting
import com.example.basefragment.core.extention.visible
import com.example.basefragment.core.helper.RateHelper
import com.example.basefragment.databinding.FragmentQuickBinding
import com.example.basefragment.databinding.FragmentSettingBinding
import com.example.basefragment.ui.main.quick.QuickViewModel
import com.example.basefragment.utils.state.RateState
import kotlin.getValue

class SettingFragment : BaseFragment<FragmentSettingBinding, SettingViewModel>( FragmentSettingBinding::inflate, SettingViewModel::class.java) {
    override fun viewListener() {
        binding.apply {
            btnLang.onClick(500) { toHomeFromSetting() }

            btnShare.onClick (1500) { shareApp() }

            btnRate.onClick {
                RateHelper.showRateDialog(requireActivity(), sharedPreferences){ state ->
                    if (state != RateState.CANCEL){
                        btnRate.gone()
                        showToast(R.string.have_rated)
                    }
                }
            }
            btnPolicy.onClick(1500) { policy() }
        }
    }


    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentSettingBinding = FragmentSettingBinding.inflate(inflater, container, false)

    override fun initView() {
        if (sharedPreferences.isRateRequest()) {
            binding.btnRate.gone()
        } else {
            binding.btnRate.visible()
        }
        binding.actionBar.tvCenter.select()
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.back_app)
            setTextActionBar(tvCenter, getString(R.string.settings))
        }
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
    }
}