package com.example.basefragment.ui.main.myPony.adapter

import android.content.Context
import com.example.basefragment.R
import com.example.basefragment.core.base.BaseAdapter
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.loadImage
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.visible
import com.example.basefragment.data.model.mypony.MyAlbumModel
import com.example.basefragment.databinding.ItemMyAlbumBinding


class MyAvatarAdapter(val context: Context) :
    BaseAdapter<MyAlbumModel, ItemMyAlbumBinding>(ItemMyAlbumBinding::inflate) {
    var onItemClick: ((MyAlbumModel) -> Unit) = {}
    var onLongClick: ((Int) -> Unit) = {}
    var onItemTick: ((Int) -> Unit) = {}

    var onEditClick: ((String) -> Unit) = {}
    var onDeleteClick: ((String) -> Unit) = {}

    override fun onBind(binding: ItemMyAlbumBinding, item: MyAlbumModel, position: Int) {
        binding.apply {
            loadImage(root, item.path, imvImage)

            if (item.isShowSelection) {
                btnSelect.visible()
                btnEdit.gone()
                btnDelete.gone()
            } else {
                btnSelect.gone()
                btnEdit.visible()
                btnDelete.visible()
            }

            btnSelect.setImageResource(
                if (item.isSelected) R.drawable.ic_selected else R.drawable.ic_not_select
            )

            // Click luôn navigate, không check selection mode
            root.onClick { onItemClick.invoke(item) }

            root.setOnLongClickListener {
                if (items.any { it.isShowSelection }) return@setOnLongClickListener false
                onLongClick.invoke(position)
                true
            }

            btnEdit.onClick { onEditClick.invoke(item.idEdit) }
            btnDelete.onClick { onDeleteClick.invoke(item.path) }
            btnSelect.onClick { onItemTick.invoke(position) } // chỉ tick button mới toggle
        }
    }
}