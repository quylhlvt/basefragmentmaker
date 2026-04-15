package com.example.basefragment.ui.main.add_character.adapter

import com.example.basefragment.core.base.BaseAdapter
import com.example.basefragment.core.extention.loadImage
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.data.model.addcharacter.SelectedAddModel
import com.example.basefragment.databinding.ItemStickerBinding

class StickerAdapter : BaseAdapter<SelectedAddModel, ItemStickerBinding>(ItemStickerBinding::inflate) {
    var onItemClick : ((String) -> Unit) = {}
    override fun onBind(binding: ItemStickerBinding, item: SelectedAddModel, position: Int) {
        binding.apply {
            loadImage(root, item.path, imvSticker)
            root.onClick { onItemClick.invoke(item.path) }
        }
    }
}