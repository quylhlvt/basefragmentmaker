package com.example.basefragment.ui.main.cosplay

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.basefragment.R
import com.example.basefragment.core.base.BaseFragment
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.popBack
import com.example.basefragment.core.extention.select
import com.example.basefragment.core.extention.setImageActionBar
import com.example.basefragment.core.extention.setTextActionBar
import com.example.basefragment.databinding.FragmentCosplayBinding
import com.example.basefragment.ui.main.customize.CustomizeFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class CosplayFragment : BaseFragment<FragmentCosplayBinding, CosplayViewModel>(
    FragmentCosplayBinding::inflate,
    CosplayViewModel::class.java
) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { popBack() }
            }
        )
    }

    override fun initView() {
        binding.setupActionBar()

        // Chỉ randomize lần đầu, nếu chưa có item nào
//        if (viewModel.randomItem.value == null) {
//            viewModel.randomize()
//        }
    }

    private fun FragmentCosplayBinding.setupActionBar() {
        actionBar.apply {
            tvCenter.select()
            setImageActionBar(btnActionBarLeft, R.drawable.back_app)
            setTextActionBar(tvCenter, getString(R.string.cosplay))
        }
    }

    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.onClick { popBack() }

        binding.random.onClick {
            viewModel.randomize()
        }

        binding.show.onClick {
            val item = viewModel.randomItem.value ?: return@onClick
            val args = CustomizeFragment.newArgs(
                templateIndex   = item.templateIndex,
                isEdit          = false,
                savedSelections = item.selections
            )
            findNavController().navigate(R.id.action_cosplay_to_show, args)
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentCosplayBinding = FragmentCosplayBinding.inflate(inflater, container, false)

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.randomItem.collectLatest { item ->
                item ?: return@collectLatest

                // Nếu đã có cache bitmap thì không render lại
                val cached = viewModel.cachedBitmap
                if (cached != null && !cached.isRecycled) {
                    showBitmap(cached)
                    return@collectLatest
                }

                renderCharacter(item)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Khi back về: dùng lại bitmap đã cache
        val cached = viewModel.cachedBitmap
        if (cached != null && !cached.isRecycled) {
            showBitmap(cached)
        }
    }

    private fun renderCharacter(item: CosplayViewModel.RandomItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            val paths = item.resolvedPaths.filterNotNull()
            if (paths.isEmpty()) return@launch

            showLoadingSafe()

            val bitmaps = withContext(Dispatchers.IO) {
                paths.map { path ->
                    async {
                        runCatching {
                            Glide.with(requireContext())
                                .asBitmap()
                                .load(path)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .override(512, 512)
                                .submit()
                                .get()
                        }.getOrNull()
                    }
                }.awaitAll().filterNotNull()
            }

            hideLoadingSafe()
            if (bitmaps.isEmpty()) return@launch

            val merged = mergeBitmaps(bitmaps)

            // Lưu vào cache
            viewModel.setCachedBitmap(merged)

            withContext(Dispatchers.Main) {
                showBitmap(merged)
            }
        }
    }

    private fun showBitmap(bitmap: Bitmap) {
        binding.imgPreview.apply {
            removeAllViews()
            addView(AppCompatImageView(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageBitmap(bitmap)
            })
        }
    }

    private fun mergeBitmaps(bitmaps: List<Bitmap>): Bitmap {
        val size   = 512
        val merged = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(merged)
        bitmaps.forEach { bmp ->
            val scaled = if (bmp.width == size && bmp.height == size) bmp
            else Bitmap.createScaledBitmap(bmp, size, size, true)
            canvas.drawBitmap(scaled, 0f, 0f, null)
            if (scaled != bmp) scaled.recycle()
        }
        return merged
    }

    override fun bindViewModel() {}
}