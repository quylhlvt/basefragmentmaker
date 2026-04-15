package com.example.basefragment.ui.language

import android.annotation.SuppressLint
import android.content.Context
import com.example.basefragment.R
import com.example.basefragment.core.base.BaseAdapter
import com.example.basefragment.core.extention.loadImage
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.data.model.language.LanguageModel
import com.example.basefragment.databinding.ItemLanguageBinding

class LanguageAdapter (val context: Context) : BaseAdapter<LanguageModel, ItemLanguageBinding>(
    ItemLanguageBinding::inflate
) {
    var onItemClick: ((String) -> Unit) = {}
    override fun onBind(
        binding: ItemLanguageBinding, item: LanguageModel, position: Int
    ) {
        binding.apply {
            loadImage(root, item.flag, imvFlag, false)
            tvLang.text = item.name

            val (ratio, color) = if (item.activate) {
                R.drawable.ic_select_lang to context.getColor(R.color.white)
            } else {
                R.drawable.ic_un_select_lang to context.getColor(R.color.app_color)
            }
            loadImage(root, ratio, btnRadio, false)

            flMain.setBackgroundResource(if (item.activate) R.drawable.frame_select_language else R.drawable.frame_unselect_language)

            tvLang.setTextColor(color)

            root.onClick {
                onItemClick.invoke(item.code)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitItem(position: Int) {
        items.forEach { it.activate = false }
        items[position].activate = true
        notifyDataSetChanged()
    }
}