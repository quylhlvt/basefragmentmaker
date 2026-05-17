package com.example.basefragment.ui.main.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.basefragment.R
import com.example.basefragment.ViewModelActivity
import com.example.basefragment.core.base.BackPressHandler
import com.example.basefragment.core.base.BaseFragment
import com.example.basefragment.core.extention.checkPermissions
import com.example.basefragment.core.extention.goToSettings
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.requestPermission
import com.example.basefragment.core.extention.setImageActionBar
import com.example.basefragment.core.extention.toSettingFromHome
import com.example.basefragment.core.helper.PermissionHelper
import com.example.basefragment.core.helper.RateHelper.showRateDialog
import com.example.basefragment.databinding.FragmentHomeBinding
import com.example.basefragment.ui.main.quick.QuickViewModel
import com.example.basefragment.ui.onboarding.permission.PermissionViewModel
import com.example.basefragment.utils.key.RequestKey
import com.example.basefragment.utils.state.RateState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.lang.System.exit

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>(
    FragmentHomeBinding::inflate, HomeViewModel::class.java
), BackPressHandler {

    private val mainViewModel: ViewModelActivity by activityViewModels()
    private val permissionsViewModel: PermissionViewModel by activityViewModels()
    private var countRate = 0

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): FragmentHomeBinding = FragmentHomeBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarRight, R.drawable.ic_settings)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("PERF2", "HomeFragment onViewCreated: ${System.currentTimeMillis()}")
    }

    override fun onResume() {
        super.onResume()
        Log.d("PERF2", "HomeFragment onResume: ${System.currentTimeMillis()}")
    }
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            findNavController().navigate(R.id.action_home_to_web)
        } else {
            showToast(R.string.granted_camera) // hoặc thông báo lỗi
        }
    }
    private fun checkCameraAndNavigate() {
        when {
            // ✅ Đã có quyền → navigate
            requireContext().checkPermissions(PermissionHelper.cameraPermission) -> {
                findNavController().navigate(R.id.action_home_to_web)
            }

            // ✅ Từ chối 2 lần → mở Settings
            permissionsViewModel.shouldCameraGoToSettings() -> {
                activity?.goToSettings()
            }

            // ✅ Chưa có → xin quyền
            else -> {
                requestPermission(
                    PermissionHelper.cameraPermission,
                    RequestKey.CAMERA_PERMISSION_CODE
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        when (requestCode) {
            RequestKey.CAMERA_PERMISSION_CODE -> {
                if (granted) {
                    viewModel.onCameraGranted()
                    findNavController().navigate(R.id.action_home_to_web)
                } else {
                    permissionsViewModel.onCameraDenied()
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            btnCreate.onClick {
                findNavController().navigate(R.id.action_home_to_createPony)
            }
            btnMyAlbum.onClick {
                findNavController().navigate(R.id.action_home_to_myPony)
            }
            btnRandom.onClick(1000) {
                findNavController().navigate(R.id.action_home_to_random)
            }
            btnQuick.onClick(1000) {
                findNavController().navigate(R.id.action_home_to_quick)
            }
            btnWeb.onClick(1000) {
                checkCameraAndNavigate()
            }
            btnCosPlay.onClick(1000) {
                findNavController().navigate(R.id.action_home_to_cosplay)
            }
            actionBar.btnActionBarRight.onClick { toSettingFromHome() }
        }
    }

    override fun observeData() {
        binding.root.post {
            Log.d("PERF2", "HomeFragment first frame: ${System.currentTimeMillis()}")
            if (!isAdded || isDetached) return@post
            binding.tv1.isSelected = true
            binding.tv2.isSelected = true
            binding.tv3.isSelected = true
            binding.tv4.isSelected = true

            viewLifecycleOwner.lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    mainViewModel.error.collect { error ->
                        error?.let { Log.e("HomeFragment", "❌ $it") }
                    }
                }
            }
        }
    }

    override fun bindViewModel() {}

    override fun onBackPressed(): Boolean {
        countRate = sharedPreferences.isBackRequest() + 1
        sharedPreferences.setBackRequest(countRate)
        if (!sharedPreferences.isRateRequest() && countRate % 2 == 0) {
            showRateDialog(requireActivity(), sharedPreferences) { state ->
                if (state != RateState.CANCEL) showToast(R.string.have_rated)
                requireActivity().finish()
                exit(0)
            }
        } else {
            requireActivity().finish()
            exit(0)
        }
        return true
    }
}