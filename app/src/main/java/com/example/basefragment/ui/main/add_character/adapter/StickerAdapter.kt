package com.example.basefragment.ui.main.add_character.adapter

import androidx.core.content.ContextCompat
import com.example.basefragment.R
import com.example.basefragment.core.base.BaseAdapter
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.loadImage
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.visible
import com.example.basefragment.data.model.addcharacter.SelectedAddModel
import com.example.basefragment.databinding.ItemStickerBinding

class StickerAdapter : BaseAdapter<SelectedAddModel, ItemStickerBinding>(ItemStickerBinding::inflate) {
    var onItemClick: ((String) -> Unit) = {}
    var currentSelected = -1

    override fun onBind(binding: ItemStickerBinding, item: SelectedAddModel, position: Int) {
        binding.apply {
            if (currentSelected == position) {
                shadown.visible()
                materialParent.apply {
                    strokeColor = ContextCompat.getColor(context, R.color.app_color)
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.app_color4))
                }
            } else {
                shadown.gone()
                materialParent.apply {
                    strokeColor = ContextCompat.getColor(context, R.color.app_color7)
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.app_color8))
                }
            }
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