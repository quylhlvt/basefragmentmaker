package com.example.basefragment.ui.main.show

import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
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
import com.example.basefragment.data.model.custom.BodyPartModel
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
    // ── Layer views (giống CustomizeFragment) ─────────────────────────────────
    private val layerViews      = arrayListOf<AppCompatImageView>()
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
            setImageActionBar(btnActionBarRight, R.drawable.next_app)
        }
        setupAdapters()
        readArgsAndInit()
    }

    private fun readArgsAndInit() {
        val templateIndex = arguments?.getInt(ARG_TEMPLATE_INDEX, 0) ?: 0

        @Suppress("UNCHECKED_CAST")
        val targetSelections = arguments?.getSerializable(ARG_SELECTIONS) as? ArrayList<SelectionIndex>
            ?: return

        viewModel.init(templateIndex, targetSelections)
    }

    private fun setupAdapters() {
        binding.rcvNav.adapter   = adapterNav
        binding.rcvColor.adapter = adapterColor
        binding.rcvPart.adapter  = adapterPart
    }

    // ── LISTENERS ─────────────────────────────────────────────────────────────

    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.onClick { popBack() }
        binding.actionBar.btnActionBarRight.onClick {
            findNavController().navigate(R.id.action_show_to_successCosplay)
        }

        // imgRandom — randomize toàn bộ (giống btnDice ở ShowActivity)
        binding.imgRandom.onClick { viewModel.randomizeAll() }

        // imgChangColor — reset về default (giống btnReset)
        binding.imgChangColor.onClick { viewModel.resetAll() }

        adapterNav.onClick   = { viewModel.selectNav(it) }
        adapterColor.onClick = { viewModel.selectColor(it) }
        adapterPart.onClick  = { idx, type ->
            when (type) {
                "none" -> viewModel.selectNone()
                "dice" -> viewModel.selectDiceCurrent()
                else   -> viewModel.selectPath(idx)
            }
        }
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

                    // Render nhân vật theo userSelections (giống renderLayers ở CustomizeFragment)
                    renderLayers(state)

                    // Update adapters
                    updateAdapters(state)

                    // Flip
                    val scale = if (state.isFlipped) -1f else 1f
                    layerViews.forEach { it.scaleX = scale }

                    // Update progress
                    updateMatchUI(state.matchPercent)
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

    // ── BUILD LAYER VIEWS (giống CustomizeFragment.buildLayerViews) ───────────

    private fun buildLayerViews(parts: List<BodyPartModel>) {
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

    // ── RENDER LAYERS (giống CustomizeFragment.renderLayers) ─────────────────

    private fun renderLayers(state: ShowState) {
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

        if (pathsToLoad.isEmpty()) {
            viewModel.onLoadingComplete()
            return
        }

        pendingLoads.set(pathsToLoad.size)

        pathsToLoad.forEach { (view, path, _) ->
            view.tag        = path
            view.visibility = View.VISIBLE
            view.scaleX     = if (viewModel.state.value.isFlipped) -1f else 1f
            loadImageIntoView(view, path)
        }
    }

    private fun loadImageIntoView(view: ImageView, path: String) {
        Glide.with(binding.rlCharacter)
            .load(path)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .priority(Priority.IMMEDIATE)
            .skipMemoryCache(false)
            .dontAnimate()
            .dontTransform()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?,
                    target: Target<Drawable>?, isFirstResource: Boolean
                ): Boolean { onLoadFinished(); return false }

                override fun onResourceReady(
                    resource: Drawable?, model: Any?,
                    target: Target<Drawable>?, dataSource: DataSource?,
                    isFirstResource: Boolean
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

    // ── ADAPTERS (giống CustomizeFragment.updateAdapters) ────────────────────

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

        val bp    = state.listData.getOrNull(state.currentNavIndex)
        val thumb = buildThumbList(bp, state.currentPaths)
        adapterPart.listThumb = thumb
        adapterPart.setPos(state.currentPathIndex)
        adapterPart.submitList(state.currentPaths)
        binding.rcvPart.post {
            binding.rcvPart.smoothScrollToPosition(state.currentPathIndex.coerceAtLeast(0))
        }
    }

    private fun buildThumbList(bp: BodyPartModel?, paths: List<String>): List<String> {
        val thumbs = bp?.listThumbPath ?: return paths
        if (thumbs.isEmpty()) return paths
        var idx = 0
        return paths.map { path ->
            when (path) {
                "none", "dice" -> path
                else           -> thumbs.getOrElse(idx++) { path }
            }
        }
    }

    // ── PROGRESS (giống ShowActivity.updateMatchUI) ───────────────────────────

    private fun updateMatchUI(percent: Int) {
        binding.tvPercent.text = "$percent%"

        // Animate progress fill (scaleY từ 0→1 theo %)
        binding.progressTrack.post {
            val trackH   = binding.progressTrack.height.toFloat()
            val marginPx = 10 * resources.displayMetrics.density
            val fillH    = trackH - marginPx
            val scale    = percent / 100f

            binding.progressFill.pivotX = binding.progressFill.width / 2f
            binding.progressFill.pivotY = fillH

            ObjectAnimator.ofFloat(
                binding.progressFill, "scaleY",
                binding.progressFill.scaleY,
                scale * fillH / trackH
            ).apply {
                duration     = 400
                interpolator = DecelerateInterpolator()
                start()
            }

            // Star icon chạy theo thanh progress
            val starH = binding.imgStar.height.toFloat()
            ObjectAnimator.ofFloat(
                binding.imgStar, "translationY",
                binding.imgStar.translationY,
                -(fillH * scale) - marginPx + starH / 2f
            ).apply {
                duration     = 400
                interpolator = DecelerateInterpolator()
                start()
            }
        }
    }

    // ── RESUME ────────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        pendingLoads.set(0)

        val needRebuild = layerViews.isEmpty() ||
                layerViews.firstOrNull()?.isAttachedToWindow == false

        if (needRebuild) {
            val currentState = viewModel.state.value
            if (currentState.listData.isNotEmpty()) {
                buildLayerViews(currentState.listData)
                renderLayers(currentState)
                updateAdapters(currentState)
                val scale = if (currentState.isFlipped) -1f else 1f
                layerViews.forEach { it.scaleX = scale }
            }
        }
    }

    override fun bindViewModel() {}

    // ── COMPANION ─────────────────────────────────────────────────────────────

    companion object {
        const val ARG_TEMPLATE_INDEX = "template_index"
        const val ARG_SELECTIONS     = "selections"

        fun newArgs(
            templateIndex   : Int,
            targetSelections: ArrayList<SelectionIndex>
        ) = Bundle().apply {
            putInt(ARG_TEMPLATE_INDEX, templateIndex)
            putSerializable(ARG_SELECTIONS, targetSelections)
        }
    }
}