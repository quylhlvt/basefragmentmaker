package com.example.basefragment.ui.main.createPony

import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.basefragment.core.base.BaseAdapter
import com.example.basefragment.data.model.custom.CustomModel
import com.example.basefragment.databinding.ItemChooseBinding

class ChoosePonyAdapter(
    private val onClick: (character: CustomModel, position: Int) -> Unit
) : BaseAdapter<CustomModel, ItemChooseBinding>(ItemChooseBinding::inflate) {

    override fun onBind(binding: ItemChooseBinding, item: CustomModel, position: Int) {
        Glide.with(binding.imvImage)
            .load(item.avatar)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .centerCrop()
            .dontAnimate()
            .into(binding.imvImage)

        binding.root.setOnClickListener { onClick(item, position) }
    }
}