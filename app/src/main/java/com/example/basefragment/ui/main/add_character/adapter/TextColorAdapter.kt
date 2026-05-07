package com.example.basefragment.ui.main.add_character.adapter

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.basefragment.R
import com.example.basefragment.core.base.BaseAdapter
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.visible
import com.example.basefragment.data.model.addcharacter.SelectedAddModel
import com.example.basefragment.databinding.ItemTextColorBinding

class TextColorAdapter :
    BaseAdapter<SelectedAddModel, ItemTextColorBinding>(ItemTextColorBinding::inflate) {
    var onChooseColorClick: (() -> Unit) = {}
    var onTextColorClick: ((Int, Int) -> Unit) = { _, _ -> }

    private var currentSelected = 1


    override fun onBind(binding: ItemTextColorBinding, item: SelectedAddModel, position: Int) {
        binding.apply {
            if (item.isSelected) {
                frameShadown.visible()
                frame.apply {
                    strokeColor = ContextCompat.getColor(context, R.color.app_color)
                }
            } else {
                frameShadown.gone()
                frame.apply {
                    strokeColor = ContextCompat.getColor(context, R.color.transparent)
                }
                if (position == 0) {
                    imvColor.gone()
                    btnAddColor.visible()
                    root.onClick { onChooseColorClick.invoke() }
                } else {
                    imvColor.visible()
                    btnAddColor.gone()
                    imvColor.setBackgroundColor(item.color)
                    root.onClick { onTextColorClick.invoke(item.color, position) }
                }
            }
        }}

        fun submitItem(position: Int, list: ArrayList<SelectedAddModel>) {
            if (position != currentSelected) {
                items.clear()
                items.addAll(list)

                notifyItemChanged(currentSelected)
                notifyItemChanged(position)

                currentSelected = position
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        fun submitListReset(list: ArrayList<SelectedAddModel>) {
            items.clear()
            items.addAll(list)
            currentSelected = 1
            notifyDataSetChanged()
        }
    }