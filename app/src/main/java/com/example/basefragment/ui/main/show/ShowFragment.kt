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
import com.example.basefragment.data.model.custom.SelectionIndex
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
    companion object {
        const val ARG_TEMPLATE_INDEX = "template_index"
        const val ARG_SELECTIONS     = "selections"

        fun newArgs(
            templateIndex  : Int,
            savedSelections: ArrayList<SelectionIndex>? = null
        ) = Bundle().apply {
            putInt(ARG_TEMPLATE_INDEX, templateIndex)
            savedSelections?.let { putSerializable(ARG_SELECTIONS, it) }
        }
    }

    private val layerViews      = arrayListOf<AppCompatImageView>()
    private val navToLayerIndex = mutableMapOf<String, Int>()

    private val adapterNav   by lazy { NavAdapter() }
    private val adapterColor by lazy { ColorAdapter() }
    private val adapterPart  by lazy { PartAdapter() }

    private val pendingLoads = AtomicInteger(0)

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentShowBinding = FragmentShowBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.back_app)
        }
        setupAdapters()

        val templateIndex = arguments?.getInt(ARG_TEMPLATE_INDEX, -1) ?: -1
        @Suppress("UNCHECKED_CAST")
        val savedSelections = arguments?.getSerializable(ARG_SELECTIONS) as? ArrayList<SelectionIndex>

        if (templateIndex >= 0 && savedSelections != null) {
            viewModel.initWithSelections(templateIndex, savedSelections)
        }
    }

    private fun setupAdapters() {
        binding.rcvNav.adapter   = adapterNav
        binding.rcvColor.adapter = adapterColor
        binding.rcvPart.adapter  = adapterPart
    }

    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.onClick { popBack() }

        adapterNav.onClick   = { viewModel.selectNav(it) }
        adapterColor.onClick = { viewModel.selectColor(it) }
        adapterPart.onClick  = { idx, type ->
            when (type) {
                "none" -> viewModel.selectPath(0)

                else   -> viewModel.selectPath(idx)
            }
        }
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // ✅ Tách thành 2 collector riêng
                launch {
                    viewModel.state.collectLatest { state ->
                        if (state.listData.isNotEmpty() &&
                            layerViews.size != state.listData.size) {
                            buildLayerViews(state.listData)
                        }
                        renderUserLayers(state)
                        updateAdapters(state)
                    }
                }

                launch {
                    // ✅ collect thay vì collectLatest → không bị cancel
                    viewModel.state.collect { state ->
                        updateProgress(state.matchPercent)
                    }
                }

                launch {
                    viewModel.onComplete.collect {
                        // findNavController().navigate(R.id.action_show_to_next)
                    }
                }
            }
        }
    }

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

    private fun renderUserLayers(state: ShowState) {
        val pathsToLoad = state.listData.mapIndexedNotNull { i, bp ->
            val path       = viewModel.resolveUserPathAt(i)
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

        if (pathsToLoad.isEmpty()) return

        pendingLoads.set(pathsToLoad.size)
        pathsToLoad.forEach { (view, path, _) ->
            view.tag        = path
            view.visibility = View.VISIBLE
            loadImage(view, path)
        }
    }

    private fun loadImage(view: ImageView, path: String) {
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
                ): Boolean { onLoadFinished(); return false }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable?, model: Any?,
                    target: Target<android.graphics.drawable.Drawable>?,
                    dataSource: DataSource?, isFirstResource: Boolean
                ): Boolean { onLoadFinished(); return false }
            })
            .into(view)
    }

    private fun onLoadFinished() {
        if (pendingLoads.decrementAndGet() <= 0) {
            pendingLoads.set(0)
            view?.post { viewModel.onLoadingComplete() }
        }
    }

    private fun updateAdapters(state: ShowState) {
        // ── Nav ──────────────────────────────────────────────────────────────
        adapterNav.submitList(state.listData)
        adapterNav.setPos(state.currentNavIndex)  // ✅ sau submitList

        // ── Color ─────────────────────────────────────────────────────────────
        binding.rcvColor.isVisible = state.hasMultipleColors
        if (state.hasMultipleColors) {
            adapterColor.submitList(state.currentColors)
            adapterColor.setPos(state.currentColorIndex)  // ✅ sau submitList
            binding.rcvColor.post {
                binding.rcvColor.smoothScrollToPosition(state.currentColorIndex)
            }
        }

        // ── Part ──────────────────────────────────────────────────────────────
        adapterPart.submitList(state.currentPaths)
        adapterPart.setPos(state.currentPathIndex)  // ✅ sau submitList
        binding.rcvPart.post {
            binding.rcvPart.smoothScrollToPosition(state.currentPathIndex.coerceAtLeast(0))
        }
    }

    private fun updateProgress(percent: Float) {
        binding.progressMatch.progress = percent.toInt()
        binding.tvPercent.text = "${percent.toInt()}%"
    }

    override fun onResume() {
        super.onResume()
        // không cần cache — user đang chọn liên tục


    }

    override fun bindViewModel() {}
}