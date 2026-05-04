package com.example.basefragment.ui.language

import android.annotation.SuppressLint
import android.content.Context
import com.example.basefragment.R
import com.example.basefragment.core.base.BaseAdapter
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.invisible
import com.example.basefragment.core.extention.loadImage
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.visible
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

           if (item.activate) flFocus.visible() else flFocus.invisible()


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