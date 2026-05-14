package com.example.basefragment.ui.main.createPony

import android.graphics.drawable.Drawable
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.basefragment.core.base.BaseAdapter
import com.example.basefragment.data.model.custom.CustomModel
import com.example.basefragment.databinding.ItemChooseBinding
import kotlin.io.root

class ChoosePonyAdapter(
    private val onClick: (character: CustomModel, position: Int) -> Unit
) : BaseAdapter<CustomModel, ItemChooseBinding>(ItemChooseBinding::inflate) {

    override fun onBind(binding: ItemChooseBinding, item: CustomModel, position: Int) {
        binding.sflShimmer.startShimmer()
        binding.sflShimmer.visibility = View.VISIBLE

        Glide.with(binding.root.context)
            .load(item.avatar)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.sflShimmer.stopShimmer()
                    binding.sflShimmer.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.sflShimmer.stopShimmer()
                    binding.sflShimmer.visibility = View.GONE
                    return false
                }
            })
            .into(binding.imvImage)

        binding.root.setOnClickListener { onClick(item, position) }
    }
}