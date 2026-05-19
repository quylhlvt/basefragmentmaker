package com.chibi.avatar.chibimaker.ui.main.add_character.adapter

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.chibi.avatar.chibimaker.R
import com.chibi.avatar.chibimaker.core.base.BaseAdapter
import com.chibi.avatar.chibimaker.core.extention.dp
import com.chibi.avatar.chibimaker.core.extention.gone
import com.chibi.avatar.chibimaker.core.extention.loadFromAsset
import com.chibi.avatar.chibimaker.core.extention.loadImage
import com.chibi.avatar.chibimaker.core.extention.onClick
import com.chibi.avatar.chibimaker.core.extention.select
import com.chibi.avatar.chibimaker.core.extention.visible
import com.chibi.avatar.chibimaker.data.model.addcharacter.SelectedAddModel
import com.chibi.avatar.chibimaker.databinding.ItemBackgroundImageBinding


class BackgroundImageAdapter : BaseAdapter<SelectedAddModel, ItemBackgroundImageBinding>(
    ItemBackgroundImageBinding::inflate
) {
    var onAddImageClick: (() -> Unit) = {}
    var onBackgroundImageClick: ((String, Int) -> Unit) = { _, _ -> }
    var currentSelected = -1

    override fun onBind(binding: ItemBackgroundImageBinding, item: SelectedAddModel, position: Int) {
        val context = binding.root.context
        binding.apply {
            tvAddImage.isSelected =true
            if (currentSelected == position) {
                materialParent.strokeColor = ContextCompat.getColor(context, R.color.app_color2)
            } else {
                materialParent.strokeColor = ContextCompat.getColor(context, R.color.app_color7)
            }
            if (position == 0) {
                lnlAddItem.visible()
                imvImage.gone()
                lnlAddItem.onClick { onAddImageClick() }
            } else {
                lnlAddItem.gone()
                imvImage.visible()
                if (imvImage.tag != item.path) {
                    imvImage.tag = item.path
                    imvImage.loadFromAsset(item.path)
                }
                imvImage.onClick { onBackgroundImageClick(item.path, position) }
            }
        }
    }

    fun selectItem(position: Int) {
        if (position == currentSelected) return
        val old = currentSelected
        currentSelected = position
        if (old >= 0) notifyItemChanged(old)
        if (position >= 0) notifyItemChanged(position)
    }

    fun clearSelection() {
        if (currentSelected < 0) return
        val old = currentSelected
        currentSelected = -1
        notifyItemChanged(old)
    }
}