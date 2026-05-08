package com.example.basefragment.ui.main.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.basefragment.R
import com.example.basefragment.ViewModelActivity
import com.example.basefragment.core.base.BackPressHandler
import com.example.basefragment.core.base.BaseFragment
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.setImageActionBar
import com.example.basefragment.core.extention.toSettingFromHome
import com.example.basefragment.core.helper.RateHelper
import com.example.basefragment.core.helper.RateHelper.showRateDialog
import com.example.basefragment.databinding.FragmentHomeBinding
import com.example.basefragment.utils.state.RateState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.lang.System.exit

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>(
    FragmentHomeBinding::inflate, HomeViewModel::class.java
), BackPressHandler {
    private val mainViewModel: ViewModelActivity by activityViewModels()
    private var countRate =0
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            findNavController().navigate(R.id.action_home_to_web)
        } else {
            showToast(R.string.permission_camera_denied)
        }
    }
    override fun setupPreViews() {
        binding.root.post {
            binding.tv1.isSelected = true
            binding.tv2.isSelected = true
            binding.tv3.isSelected = true
            binding.tv4.isSelected = true
        }
        // ✅ Preload tất cả ảnh dùng trong màn home
        Glide.with(this).load(R.drawable.img_bg_home)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE).preload()
        binding.apply {
            actionBar.apply {
                setImageActionBar(btnActionBarRight, R.drawable.ic_settings)
            }


            // ✅ Setup actionbar sớm


            // ✅ Fetch online data sớm nhất có thể
            if (mainViewModel.networkOnline.value) {
                mainViewModel.fetchOnlineTemplates()
            }
        }

    }
    override fun viewListener() {
        binding.apply {
            // Click vào "Choose Character"
            btnCreate.onClick {
                // Navigate tới CategoryFragment
                 findNavController().navigate(R.id.action_home_to_createPony)
            }

//            // Click vào "Quick Mix"
//            btnQuickMaker.onClick {
//                // Navigate tới QuickMixFragment
//                 findNavController().navigate(R.id.action_home_to_quick)
//            }
            btnMyAlbum.onClick {
                findNavController().navigate(R.id.action_home_to_myPony)
            }
            btnRandom.onClick {
                findNavController().navigate(R.id.action_home_to_random)
            }
            btnCosPlay.onClick {
                findNavController().navigate(R.id.action_home_to_cosplay)
            }
//            btnWeb.onClick {
//                when {
//                    ContextCompat.checkSelfPermission(
//                        requireContext(),
//                        Manifest.permission.CAMERA
//                    ) == PackageManager.PERMISSION_GRANTED -> {
//                        // Đã có quyền → navigate luôn
//                        findNavController().navigate(R.id.action_home_to_web)
//                    }
//                    else -> {
//                        // Chưa có → xin quyền
//                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
//                    }
//                }
//            }
            actionBar.btnActionBarRight.onClick {
                toSettingFromHome()
            }
        }
    }


    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): FragmentHomeBinding {
        android.util.Log.d("PERF1", "1. inflateBinding: ${System.currentTimeMillis()}")
        return FragmentHomeBinding.inflate(inflater, container, false)
    }
    override fun initView() {

    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.characters.collect { characters ->
                if (characters.isNotEmpty()) {
                    Log.d("HomeFragment", "✅ ${characters.size} characters")
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.backgrounds.collect { backgrounds ->
                if (backgrounds.isNotEmpty()) {
                    Log.d("HomeFragment", "✅ ${backgrounds.size} backgrounds")
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.stickers.collect { stickers ->
                if (stickers.isNotEmpty()) {
                    Log.d("HomeFragment", "✅ ${stickers.size} stickers")
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.error.collect { error ->
                error?.let { Log.e("HomeFragment", "❌ $it") }
            }
        }
    }

    override fun bindViewModel() {
    }


    override fun onBackPressed(): Boolean {
        countRate = sharedPreferences.isBackRequest() + 1
        sharedPreferences.setBackRequest(countRate)

        android.util.Log.d("HomeFragment1", "countRate=$countRate, isRateRequest=${sharedPreferences.isRateRequest()}, check=${countRate % 2 == 0}")

        if (!sharedPreferences.isRateRequest() && countRate % 2 == 0) {
            showRateDialog(requireActivity(), sharedPreferences) { state ->
                if (state != RateState.CANCEL) {
                    showToast(R.string.have_rated)
                }
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