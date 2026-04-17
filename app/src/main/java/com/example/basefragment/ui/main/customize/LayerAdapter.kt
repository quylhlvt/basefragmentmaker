package com.example.basefragment.ui.main.customize

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.basefragment.R
import com.example.basefragment.core.base.BaseAdapter
import com.example.basefragment.data.model.custom.BodyPartModel
import com.example.basefragment.data.model.custom.ColorModel
import com.example.basefragment.databinding.ItemColorBinding
import com.example.basefragment.databinding.ItemNavBinding
import com.example.basefragment.databinding.ItemPartBinding

// ── NAV ADAPTER ───────────────────────────────────────────────────────────────
// ── NAV ADAPTER ───────────────────────────────────────────────────────────────
class NavAdapter : BaseAdapter<BodyPartModel, ItemNavBinding>(ItemNavBinding::inflate) {

    var posNav  = 0
    var onClick: ((Int) -> Unit)? = null

    fun setPos(pos: Int) {
        val old = posNav; posNav = pos
        if (old != pos) { notifyItemChanged(old); notifyItemChanged(pos) }
    }

    override fun onBind(binding: ItemNavBinding, item: BodyPartModel, position: Int) {
        binding.ivSelected.isVisible = posNav == position
        Glide.with(binding.ivIcon)
            .load(item.nav)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .override(256)
            .dontAnimate()
            .into(binding.ivIcon)
        binding.root.setOnClickListener { onClick?.invoke(position) }
    }
}

// ── COLOR ADAPTER ─────────────────────────────────────────────────────────────
class ColorAdapter : BaseAdapter<ColorModel, ItemColorBinding>(ItemColorBinding::inflate) {

    var posColor = 0
    var onClick: ((Int) -> Unit)? = null

    fun setPos(pos: Int) {
        val old = posColor; posColor = pos
        if (old != pos) { notifyItemChanged(old); notifyItemChanged(pos) }
    }

    override fun onBind(binding: ItemColorBinding, item: ColorModel, position: Int) {
        binding.colorSelected.isVisible = posColor == position

        val colorInt = runCatching {
            Color.parseColor(
                if (item.color.isEmpty() || item.color == "#") "#FFFFFF"
                else "#${item.color}"
            )
        }.getOrDefault(Color.WHITE)

        DrawableCompat.setTint(binding.viewColor.background.mutate(), colorInt)
        binding.root.setOnClickListener { onClick?.invoke(position) }
    }
}

// ── PART ADAPTER ──────────────────────────────────────────────────────────────
class PartAdapter : BaseAdapter<String, ItemPartBinding>(ItemPartBinding::inflate) {

    var posPath:   Int          = 0
    var listThumb: List<String> = emptyList()
    var onClick:   ((Int, String) -> Unit)? = null

    fun setPos(pos: Int) {
        val old = posPath; posPath = pos
        if (old != pos) { notifyItemChanged(old); notifyItemChanged(pos) }
    }

    override fun onBind(binding: ItemPartBinding, item: String, position: Int) {
        val ctx = binding.root.context
        if (posPath == position) {
            binding.materialCard.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.app_color2))
            binding.materialCard.strokeColor = ContextCompat.getColor(ctx, R.color.app_color)
        } else {
            binding.materialCard.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.white))
            binding.materialCard.strokeColor = ContextCompat.getColor(ctx, R.color.white)
        }

        val thumbPath = listThumb.getOrElse(position) { item }
        when (item) {
            "none" -> {
                Glide.with(binding.imv).clear(binding.imv)
                binding.imv.setImageResource(R.drawable.ic_none)
            }
            "dice" -> {
                Glide.with(binding.imv).clear(binding.imv)
                binding.imv.setImageResource(R.drawable.ic_dice)
            }
            else -> {
                Glide.with(binding.imv)
                    .load(thumbPath)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .override(256)
                    .dontAnimate()
                    .into(binding.imv)
            }
        }
        binding.root.setOnClickListener { onClick?.invoke(position, item) }
    }
}