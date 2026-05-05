package com.example.basefragment.ui.main.add_character.adapter

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.basefragment.R
import com.example.basefragment.core.base.BaseAdapter
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.loadFromAsset
import com.example.basefragment.core.extention.loadImage
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.select
import com.example.basefragment.core.extention.visible
import com.example.basefragment.data.model.addcharacter.SelectedAddModel
import com.example.basefragment.databinding.ItemBackgroundImageBinding


class BackgroundImageAdapter : BaseAdapter<SelectedAddModel, ItemBackgroundImageBinding>(
    ItemBackgroundImageBinding::inflate
) {
    var onAddImageClick: (() -> Unit) = {}
    var onBackgroundImageClick: ((String, Int) -> Unit) = { _, _ -> }
    var currentSelected = -1

    override fun onBind(binding: ItemBackgroundImageBinding, item: SelectedAddModel, position: Int) {
        val context = binding.root.context
        binding.apply {
            if (currentSelected == position) {
                shadown.visible()
                materialParent.apply {    strokeColor = ContextCompat.getColor(context, R.color.app_color)
                    setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.app_color4)
                    )}
            } else {
                shadown.gone()
                materialParent.apply { strokeColor = ContextCompat.getColor(context, R.color.app_color7)
                    setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.app_color8)
                    )
                    // tắt elevation mặc định để dùng custom shadow
                }
            }
            if (position == 0) {
                imvAddItem.visible()
                imvImage.gone()
                imvAddItem.onClick { onAddImageClick() }
            } else {
                imvAddItem.gone()
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