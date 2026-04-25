package com.example.basefragment.ui.onboarding.permission

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.basefragment.R
import com.example.basefragment.core.base.BackPressHandler
import com.example.basefragment.core.base.BaseFragment
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.select
import com.example.basefragment.core.extention.toHomeFromPermission
import com.example.basefragment.core.extention.visible
import com.example.basefragment.core.helper.StringHelper
import com.example.basefragment.databinding.FragmentPermissionBinding
import com.example.basefragment.utils.key.RequestKey
import com.example.basefragment.core.extention.checkPermissions
import com.example.basefragment.core.extention.goToSettings
import com.example.basefragment.core.extention.requestPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PermissionFragment : BaseFragment<FragmentPermissionBinding, PermissionViewModel>(
    FragmentPermissionBinding::inflate, PermissionViewModel::class.java
), BackPressHandler  {
    override fun viewListener() {
        binding.swPermission.onClick(1000) { handlePermissionRequest(isStorage = true) }
        binding.swNotification.onClick(1000) { handlePermissionRequest(isStorage = false) }
        binding.tvContinue.onClick(1000) { handleContinue() }
    }


    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): FragmentPermissionBinding = FragmentPermissionBinding.inflate(inflater, container, false)

    override fun initView() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            binding.btnStorage.visible()
            binding.btnNotification.gone()
        } else {
            binding.btnNotification.visible()
            binding.btnStorage.gone()
        }


//        binding.textView.text = "Home Fragment"
//        binding.btnTest.setOnClickListener {
//            showSnackbar("Xin chào từ Home!")
//        }
    }
    override fun onStart() {
        super.onStart()
        viewModel.updateStorageGranted(
            sharedPreferences, requireContext().checkPermissions(viewModel.getStoragePermissions())
        )
        viewModel.updateNotificationGranted(
            sharedPreferences, requireContext().checkPermissions(viewModel.getNotificationPermissions())
        )
    }
    private fun handlePermissionRequest(isStorage: Boolean) {
        val perms = if (isStorage) viewModel.getStoragePermissions() else viewModel.getNotificationPermissions()
        if (requireContext().checkPermissions(perms)) {
            showToast(if (isStorage) R.string.granted_storage else R.string.granted_notification)
        } else if (viewModel.needGoToSettings(sharedPreferences, isStorage)) {
            activity?.goToSettings()
        } else {
            val requestCode = if (isStorage) RequestKey.STORAGE_PERMISSION_CODE else RequestKey.NOTIFICATION_PERMISSION_CODE
           requestPermission(perms, requestCode)
        }
    }

    private fun updatePermissionUI(granted: Boolean, isStorage: Boolean) {
        val imageView = if (isStorage) binding.swPermission else binding.swNotification
        imageView.setImageResource(if (granted) R.drawable.switch_on else R.drawable.switch_off)
    }
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        when (requestCode) {
            RequestKey.STORAGE_PERMISSION_CODE -> viewModel.updateStorageGranted(sharedPreferences, granted)

            RequestKey.NOTIFICATION_PERMISSION_CODE -> viewModel.updateNotificationGranted(sharedPreferences, granted)
        }
        if (granted) {
            showToast(if (requestCode == RequestKey.STORAGE_PERMISSION_CODE) R.string.granted_storage else R.string.granted_notification)
        }
    }
    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.storageGranted.collect { granted ->
                        updatePermissionUI(granted, true)
                    }
                }

                launch {
                    viewModel.notificationGranted.collect { granted ->
                        updatePermissionUI(granted, false)
                    }
                }
            }
        }
    }

    override fun initText() {
        binding.actionBar.tvCenter.select()
        val textRes =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) R.string.to_access_13 else R.string.to_access

        binding.txtPermission.text = TextUtils.concat(
            createColoredText(R.string.allow, R.color.black),
            " ",
            createColoredText(R.string.app_name, R.color.app_color3),
            " ",
            createColoredText(textRes, R.color.black)
        )
    }

    private fun handleContinue() {
        sharedPreferences.setPermissionScreen(true)
        toHomeFromPermission()
    }

    override fun bindViewModel() {
    }

    private fun createColoredText(
        @androidx.annotation.StringRes textRes: Int,
        @androidx.annotation.ColorRes colorRes: Int,
        font: Int = R.font.itim_regular
    ) = StringHelper.changeColor(requireContext(), getString(textRes), colorRes, font)

    override fun onBackPressed(): Boolean {
        requireActivity().finish()
        return true
    }

}