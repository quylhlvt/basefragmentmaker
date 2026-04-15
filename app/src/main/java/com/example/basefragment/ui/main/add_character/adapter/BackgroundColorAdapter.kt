package com.example.basefragment.ui.main.add_character.adapter

import androidx.core.view.isVisible
import com.example.basefragment.R
import com.example.basefragment.core.base.BaseAdapter
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.loadImage
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.data.model.addcharacter.SelectedAddModel
import com.example.basefragment.databinding.ItemBackgroundColorBinding


class BackgroundColorAdapter :
    BaseAdapter<SelectedAddModel, ItemBackgroundColorBinding>(ItemBackgroundColorBinding::inflate) {
    var onChooseColorClick: (() -> Unit) = {}
    var onBackgroundColorClick: ((Int, Int) -> Unit) = {_,_ ->}

    var currentSelected = -1
    override fun onBind(binding: ItemBackgroundColorBinding, item: SelectedAddModel, position: Int) {
        binding.apply {
            vFocus1.isVisible = item.isSelected
            if (position == 0) {
        
                vFocus1.isVisible = item.isSelected
                loadImage(root, R.drawable.ic_choose_color, imvColor)
                root.onClick { onChooseColorClick.invoke() }
            } else {
                imvColor.setBackgroundColor(item.color)
                root.onClick { onBackgroundColorClick.invoke(item.color, position) }
            }
        }
    }

    fun submitItem(position: Int, list: ArrayList<SelectedAddModel>){
        if (position == currentSelected) return
        if (position != currentSelected){
            if (currentSelected >= 0 && currentSelected < items.size) {
                items[currentSelected] = items[currentSelected].copy(isSelected = false)
                notifyItemChanged(currentSelected)
            }

            if (position >= 0 && position < items.size) {
                items[position] = items[position].copy(isSelected = true)
                notifyItemChanged(position)
            }

            currentSelected = position
        }
    }
}