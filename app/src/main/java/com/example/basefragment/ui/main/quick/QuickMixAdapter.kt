package com.example.basefragment.ui.main.quick

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.basefragment.data.model.custom.QuickMixItem
import com.example.basefragment.databinding.ItemQuickMixBinding

class QuickMixAdapter(
    private val viewModel: QuickViewModel
) : ListAdapter<QuickMixItem, QuickMixAdapter.VH>(DIFF) {

    var onItemClick:       ((QuickMixItem) -> Unit)? = null
    var onRegenerateClick: ((Int) -> Unit)?           = null

    inner class VH(val binding: ItemQuickMixBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: QuickMixItem) {
            val key = viewModel.itemKey(item)
            binding.imgPreview.tag = key
            binding.root.setOnClickListener { onItemClick?.invoke(item) }

            val cached = viewModel.bitmapCache[key]
            if (cached != null && !cached.isRecycled) {
                binding.progressLoading.visibility = android.view.View.GONE
                binding.imgPreview.setImageBitmap(cached)
            } else {
                binding.progressLoading.visibility = android.view.View.VISIBLE
                binding.imgPreview.setImageBitmap(null)
            }
        }
    }

    // Gọi khi ViewModel báo key nào ready — chỉ update đúng item đó
    fun notifyKeyReady(key: String) {
        for (i in 0 until itemCount) {
            val item = runCatching { getItem(i) }.getOrNull() ?: continue
            if (viewModel.itemKey(item) == key) {
                notifyItemChanged(i, PAYLOAD_BITMAP_READY)
                break
            }
        }
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_BITMAP_READY)) {
            val key    = viewModel.itemKey(getItem(position))
            val cached = viewModel.bitmapCache[key]
            if (cached != null && !cached.isRecycled &&
                holder.binding.imgPreview.tag == key) {
                holder.binding.progressLoading.visibility = android.view.View.GONE
                holder.binding.imgPreview.setImageBitmap(cached)
            }
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemQuickMixBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private const val PAYLOAD_BITMAP_READY = "bitmap_ready"
        val DIFF = object : DiffUtil.ItemCallback<QuickMixItem>() {
            override fun areItemsTheSame(a: QuickMixItem, b: QuickMixItem) =
                a.templateIndex == b.templateIndex && a.selections == b.selections
            override fun areContentsTheSame(a: QuickMixItem, b: QuickMixItem) = a == b
        }
    }
}