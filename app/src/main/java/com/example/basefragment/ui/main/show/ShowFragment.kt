package com.example.basefragment.ui.main.show

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.basefragment.R
import com.example.basefragment.core.base.BaseFragment
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.popBack
import com.example.basefragment.core.extention.setImageActionBar
import com.example.basefragment.databinding.FragmentShowBinding
import com.example.basefragment.ui.main.customize.ColorAdapter
import com.example.basefragment.ui.main.customize.NavAdapter
import com.example.basefragment.ui.main.customize.PartAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

@AndroidEntryPoint
class ShowFragment : BaseFragment<FragmentShowBinding, ShowViewModel>(
    FragmentShowBinding::inflate,
    ShowViewModel::class.java
) {
    // ── Layer views cho ảnh TARGET render lên rlCharacter ────────────────────
    private val layerViews     = arrayListOf<AppCompatImageView>()
    private val navToLayerIndex = mutableMapOf<String, Int>()

    private val adapterNav   by lazy { NavAdapter() }
    private val adapterColor by lazy { ColorAdapter() }
    private val adapterPart  by lazy { PartAdapter() }

    private val pendingLoads = AtomicInteger(0)

    // ── INFLATE ───────────────────────────────────────────────────────────────

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentShowBinding = FragmentShowBinding.inflate(inflater, container, false)

    // ── INIT ──────────────────────────────────────────────────────────────────

    override fun initView() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.back_app)
        }
        setupAdapters()
        if (viewModel.state.value.listData.isEmpty()) {
            viewModel.randomize()
        }
    }

    private fun setupAdapters() {
        binding.rcvNav.adapter   = adapterNav
        binding.rcvColor.adapter = adapterColor
        binding.rcvPart.adapter  = adapterPart
    }

    // ── LISTENERS ─────────────────────────────────────────────────────────────

    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.onClick { popBack() }

        adapterNav.onClick   = { viewModel.selectNav(it) }
        adapterColor.onClick = { viewModel.selectColor(it) }
        adapterPart.onClick  = { idx, type ->
            when (type) {
                "none" -> viewModel.selectPath(0)
                "dice" -> { /* không cho dice ở màn đoán */ }
                else   -> viewModel.selectPath(idx)
            }
        }

        // imgRandom không dùng ở màn Show → ẩn đi
        binding.imgRandom.visibility = View.GONE
    }

    // ── OBSERVE ───────────────────────────────────────────────────────────────

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collectLatest { state ->

                    // Build layer views lần đầu hoặc khi listData thay đổi
                    if (state.listData.isNotEmpty() &&
                        layerViews.size != state.listData.size) {
                        buildLayerViews(state.listData)
                    }

                    // Render nhân vật TARGET lên rlCharacter
                    renderTargetLayers(state)

                    // Update adapters (user chọn theo userSelections)
                    updateAdapters(state)

                    // Update progress
                    updateProgress(state.matchPercent)
                }
            }
        }

        // Navigate khi đạt 100%
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.onComplete.collect {
//                findNavController().navigate(R.id.action_show_to_next)
            }
        }
    }

    // ── BUILD LAYER VIEWS ─────────────────────────────────────────────────────

    private fun buildLayerViews(
        parts: List<com.example.basefragment.data.model.custom.BodyPartModel>
    ) {
        layerViews.clear()
        navToLayerIndex.clear()
        binding.rlCharacter.removeAllViews()

        parts.sortedBy { it.position }.forEachIndexed { layerIdx, bp ->
            val iv = AppCompatImageView(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            binding.rlCharacter.addView(iv)
            layerViews.add(iv)
            navToLayerIndex[bp.nav] = layerIdx
        }
    }

    // ── RENDER TARGET (nhân vật đáp án — user nhìn để đoán) ──────────────────

    private fun renderTargetLayers(state: ShowState) {
        // Dùng cache bitmap nếu đã render xong
        val cached = viewModel.cachedBitmap
        if (cached != null && !cached.isRecycled) {
            showCachedBitmap(cached)
            return
        }

        val pathsToLoad = state.listData.mapIndexedNotNull { i, bp ->
            val path       = viewModel.resolveTargetPathAt(i)
            val layerIndex = navToLayerIndex[bp.nav] ?: return@mapIndexedNotNull null
            val view       = layerViews.getOrNull(layerIndex) ?: return@mapIndexedNotNull null

            if (path == null) {
                if (view.visibility != View.GONE) {
                    view.visibility = View.GONE
                    view.tag        = null
                    Glide.with(binding.rlCharacter).clear(view)
                }
                return@mapIndexedNotNull null
            }
            if (view.tag == path && view.visibility == View.VISIBLE) return@mapIndexedNotNull null
            Triple(view, path, layerIndex)
        }

        if (pathsToLoad.isEmpty()) {
            viewModel.onLoadingComplete()
            cacheTargetBitmap()
            return
        }

        pendingLoads.set(pathsToLoad.size)
        pathsToLoad.forEach { (view, path, _) ->
            view.tag        = path
            view.visibility = View.VISIBLE
            loadTargetImage(view, path)
        }
    }

    private fun loadTargetImage(view: ImageView, path: String) {
        Glide.with(binding.rlCharacter)
            .load(path)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .priority(Priority.IMMEDIATE)
            .skipMemoryCache(false)
            .dontAnimate()
            .dontTransform()
            .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?,
                    target: Target<android.graphics.drawable.Drawable>?,
                    isFirstResource: Boolean
                ): Boolean { onTargetLoadFinished(); return false }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable?, model: Any?,
                    target: Target<android.graphics.drawable.Drawable>?,
                    dataSource: DataSource?, isFirstResource: Boolean
                ): Boolean { onTargetLoadFinished(); return false }
            })
            .into(view)
    }

    private fun onTargetLoadFinished() {
        if (pendingLoads.decrementAndGet() <= 0) {
            pendingLoads.set(0)
            view?.post {
                viewModel.onLoadingComplete()
                cacheTargetBitmap()
            }
        }
    }

    private fun cacheTargetBitmap() {
        val root = binding.rlCharacter
        if (root.width == 0 || root.height == 0) return
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        layerViews.forEach { iv ->
            if (iv.visibility != View.VISIBLE) return@forEach
            val drawable = iv.drawable ?: return@forEach
            drawable.setBounds(0, 0, root.width, root.height)
            drawable.draw(canvas)
        }
        viewModel.setCachedBitmap(bitmap)
    }

    private fun showCachedBitmap(bitmap: Bitmap) {
        // Chỉ replace nếu chưa hiển thị bitmap này
        val firstChild = binding.rlCharacter.getChildAt(0)
        if (firstChild is AppCompatImageView && firstChild.drawable != null
            && firstChild.tag == "cached") return

        binding.rlCharacter.removeAllViews()
        binding.rlCharacter.addView(AppCompatImageView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageBitmap(bitmap)
            tag = "cached"
        })
    }

    // ── ADAPTERS ──────────────────────────────────────────────────────────────

    private fun updateAdapters(state: ShowState) {
        adapterNav.setPos(state.currentNavIndex)
        adapterNav.submitList(state.listData)

        adapterColor.setPos(state.currentColorIndex)
        binding.rcvColor.isVisible = state.hasMultipleColors
        if (state.hasMultipleColors) {
            adapterColor.submitList(state.currentColors)
            binding.rcvColor.post {
                binding.rcvColor.smoothScrollToPosition(state.currentColorIndex)
            }
        }

        adapterPart.setPos(state.currentPathIndex)
        adapterPart.submitList(state.currentPaths)
        binding.rcvPart.post {
            binding.rcvPart.smoothScrollToPosition(state.currentPathIndex.coerceAtLeast(0))
        }
    }

    // ── PROGRESS ──────────────────────────────────────────────────────────────

    private fun updateProgress(percent: Float) {
        binding.progressMatch.progress = percent.toInt()
        binding.tvPercent.text = "${percent.toInt()}%"
    }

    // ── RESUME ────────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        val cached = viewModel.cachedBitmap
        if (cached != null && !cached.isRecycled) {
            showCachedBitmap(cached)
        }
    }

    override fun bindViewModel() {}
}