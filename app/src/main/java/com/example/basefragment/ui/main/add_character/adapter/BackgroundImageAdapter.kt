package com.example.basefragment.ui.main.add_character.adapter

import androidx.core.view.isVisible
import com.example.basefragment.core.base.BaseAdapter
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.loadFromAsset
import com.example.basefragment.core.extention.loadImage
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.select
import com.example.basefragment.core.extention.visible
import com.example.basefragment.data.model.addcharacter.SelectedAddModel
import com.example.basefragment.databinding.ItemBackgroundImageBinding


class BackgroundImageAdapter : BaseAdapter<SelectedAddModel, ItemBackgroundImageBinding>(ItemBackgroundImageBinding::inflate) {
    var onAddImageClick: (() -> Unit) = {}
    var onBackgroundImageClick: ((String, Int) -> Unit) = {_,_ ->}
    var currentSelected = -1

    override fun onBind(binding: ItemBackgroundImageBinding, item: SelectedAddModel, position: Int) {
        binding.apply {
            tvAddImage.isSelected = true
            vFocus.isVisible = item.isSelected
            if (position == 0) {
                lnlAddItem.visible()
                imvImage.gone()
                lnlAddItem.onClick { onAddImageClick.invoke() }
            } else {
                lnlAddItem.gone()
                imvImage.visible()

                // ✅ Skip load nếu đã load path này rồi
                if (imvImage.tag != item.path) {
                    imvImage.tag = item.path
                    imvImage.loadFromAsset(item.path)
                }

                imvImage.onClick { onBackgroundImageClick.invoke(item.path, position) }
            }
        }
    }

    fun submitItem(position: Int, list: ArrayList<SelectedAddModel>){
        if (position == currentSelected) return
        items.clear()
        items.addAll(list)
        notifyItemChanged(currentSelected)
        notifyItemChanged(position)
        currentSelected = position
    }
}