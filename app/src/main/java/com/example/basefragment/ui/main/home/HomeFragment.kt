package com.example.basefragment.ui.main.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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
        android.util.Log.d("PERF", "2. Home inflateBinding: ${System.currentTimeMillis()}")
        return FragmentHomeBinding.inflate(inflater, container, false)
    }
    override fun initView() {
        android.util.Log.d("PERF", "3. Home initView: ${System.currentTimeMillis()}")

        binding.apply {
            tv1.post { tv1.isSelected = true }
            tv2.post { tv2.isSelected = true }
            tv3.post { tv3.isSelected = true }
            tv4.post { tv4.isSelected = true }

            actionBar.apply {
                setImageActionBar(btnActionBarRight, R.drawable.ic_settings)
//            setImageActionBar(btnActionBarLeft, R.drawable.logo_app)
            }
        }
        deleteTempFolder()
//        binding.textView.text = "Home Fragment"
//        binding.btnTest.setOnClickListener {
//            showSnackbar("Xin chào từ Home!")
//        }
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.isLoading.collect { isLoading ->
                // Hiển thị/ẩn loading indicator
                if (isLoading) {
                    // binding.progressBar.visibility = View.VISIBLE
                } else {
                    // binding.progressBar.visibility = View.GONE
                }
            }
        }

        // Observe characters data
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.characters.collect { characters ->
                // Data đã được load, có thể dùng ở đây
                if (characters.isNotEmpty()) {
                    // Ví dụ: hiển thị số lượng characters
                    // binding.tvCharacterCount.text = "Available: ${characters.size} characters"
                    android.util.Log.d("HomeFragment", "✅ Có ${characters.size} characters")
                }
            }
        }

        // Observe backgrounds data
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.backgrounds.collect { backgrounds ->
                if (backgrounds.isNotEmpty()) {
                    android.util.Log.d("HomeFragment", "✅ Có ${backgrounds.size} backgrounds")
                }
            }
        }

        // Observe stickers data
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.stickers.collect { stickers ->
                if (stickers.isNotEmpty()) {
                    android.util.Log.d("HomeFragment", "✅ Có ${stickers.size} stickers")
                }
            }
        }

        // Observe errors
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.error.collect { error ->
                error?.let {
                    // Hiển thị error message
                    // showSnackbar("Error: $it")
                    android.util.Log.e("HomeFragment", "❌ Error: $it")
                }
            }
        }
//        viewModel.data.observe(viewLifecycleOwner) { text ->
//            binding.textView.text = text
//        }
    }

    override fun bindViewModel() {
    }

    private fun deleteTempFolder() {
        lifecycleScope.launch(Dispatchers.IO) {
//            val dataTemp =
//                MediaHelper.getImageInternal(requireContext(), ValueKey.RANDOM_TEMP_ALBUM)
//            if (dataTemp.isNotEmpty()) {
//                dataTemp.forEach {
//                    val file = File(it)
//                    file.delete()
//                }
//            }
        }
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