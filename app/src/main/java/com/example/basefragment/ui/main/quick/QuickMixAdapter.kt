package com.example.basefragment.ui.main.quick

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.visible
import com.example.basefragment.data.model.custom.QuickMixItem
import com.example.basefragment.databinding.ItemQuickMixBinding

class QuickMixAdapter(
    private val viewModel: QuickViewModel
) : ListAdapter<QuickMixItem, QuickMixAdapter.VH>(DIFF) {

    var onItemClick:       ((QuickMixItem) -> Unit)? = null
    var onRegenerateClick: ((Int) -> Unit)?           = null

    // ✅ Map key → position để notifyKeyReady O(1) thay vì O(n)
    private val keyToAdapterPos = HashMap<String, Int>()

    inner class VH(val binding: ItemQuickMixBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: QuickMixItem) {
            val key = viewModel.itemKey(item)
            binding.imgPreview.tag = key
            binding.root.setOnClickListener { onItemClick?.invoke(item) }

            val cached = viewModel.bitmapCache[key]
            if (cached != null && !cached.isRecycled) {
                binding.progressLoading.gone()
                binding.imgPreview.setImageBitmap(cached)
            } else {
                binding.progressLoading.visible()
                binding.imgPreview.setImageBitmap(null)
                // ✅ KHÔNG gọi requestMergeIfMissing ở đây
                // — để updateVisibleRange xử lý ưu tiên
            }
        }
    }

    // ✅ Override submitList để build map ngay khi data về
    override fun submitList(list: List<QuickMixItem>?) {
        keyToAdapterPos.clear()
        list?.forEachIndexed { index, item ->
            keyToAdapterPos[viewModel.itemKey(item)] = index
        }
        super.submitList(list)
    }

    override fun submitList(list: List<QuickMixItem>?, commitCallback: Runnable?) {
        keyToAdapterPos.clear()
        list?.forEachIndexed { index, item ->
            keyToAdapterPos[viewModel.itemKey(item)] = index
        }
        super.submitList(list, commitCallback)
    }

    // ✅ O(1) — không loop nữa
    fun notifyKeyReady(key: String) {
        val pos = keyToAdapterPos[key] ?: return
        notifyItemChanged(pos, PAYLOAD_BITMAP_READY)
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_BITMAP_READY)) {
            val item   = runCatching { getItem(position) }.getOrNull() ?: return
            val key    = viewModel.itemKey(item)
            if (holder.binding.imgPreview.tag != key) return
            val cached = viewModel.bitmapCache[key]
            if (cached != null && !cached.isRecycled) {
                holder.binding.progressLoading.gone()
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