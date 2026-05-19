package com.chibi.avatar.chibimaker.ui.main.add_character.adapter

import androidx.core.content.ContextCompat
import com.chibi.avatar.chibimaker.R
import com.chibi.avatar.chibimaker.core.base.BaseAdapter
import com.chibi.avatar.chibimaker.core.extention.gone
import com.chibi.avatar.chibimaker.core.extention.loadImage
import com.chibi.avatar.chibimaker.core.extention.onClick
import com.chibi.avatar.chibimaker.core.extention.visible
import com.chibi.avatar.chibimaker.data.model.addcharacter.SelectedAddModel
import com.chibi.avatar.chibimaker.databinding.ItemStickerBinding

class StickerAdapter : BaseAdapter<SelectedAddModel, ItemStickerBinding>(ItemStickerBinding::inflate) {
    var onItemClick: ((String) -> Unit) = {}
    var currentSelected = -1

    override fun onBind(binding: ItemStickerBinding, item: SelectedAddModel, position: Int) {
        binding.apply {

            loadImage(root, item.path, imageView)
            root.onClick {
                selectItem(position)          // ← was missing entirely
                onItemClick.invoke(item.path)
            }
        }
    }

    fun selectItem(position: Int) {           // ← changed private → public
        if (position == currentSelected) return
        val old = currentSelected
        currentSelected = position
        if (old >= 0) notifyItemChanged(old)
        notifyItemChanged(position)
    }
}