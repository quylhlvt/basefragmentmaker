package com.example.basefragment.ui.main.quick

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.basefragment.data.datalocal.manager.AppDataManager
import com.example.basefragment.data.model.custom.CustomModel
import com.example.basefragment.data.model.custom.QuickMixItem
import com.example.basefragment.data.model.custom.SelectionIndex
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class QuickViewModel @Inject constructor(
    private val appDataManager: AppDataManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _items     = MutableStateFlow<List<QuickMixItem>>(emptyList())
    val items: StateFlow<List<QuickMixItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Cache bitmap đã merge — sống cùng ViewModel, không bị destroy khi navigate
    val bitmapCache = ConcurrentHashMap<String, Bitmap>()

    // Signal cho adapter biết key nào vừa ready
    private val _readyKey = MutableStateFlow<String?>(null)
    val readyKey: StateFlow<String?> = _readyKey.asStateFlow()

    private val ioDispatcher = Dispatchers.IO.limitedParallelism(6)

    companion object {
        const val PER_TEMPLATE = 10
    }

    fun itemKey(item: QuickMixItem) =
        "${item.templateIndex}_${item.selections.hashCode()}"

    fun generate() {
        if (_items.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val templates = appDataManager.templates.value
            if (templates.isEmpty()) { _isLoading.value = false; return@launch }

            val all = mutableListOf<QuickMixItem>()
            templates.forEachIndexed { idx, template ->
                repeat(PER_TEMPLATE) {
                    val sel      = randomSelections(template)
                    val resolved = resolvePaths(template, sel)
                    all.add(QuickMixItem(idx, template, sel, resolved))
                }
            }
            all.shuffle()

            // Emit items trước để UI layout xong, biết visible range
            withContext(Dispatchers.Main) {
                _items.value     = all
                _isLoading.value = false
            }

            // Phase 1: merge đúng các item visible (do Fragment báo qua visibleRange)
            // Đợi 1 frame để RecyclerView layout xong
            withContext(Dispatchers.Main) { }

            val visible = _visibleRange.value
            val visibleItems = if (visible != IntRange.EMPTY) {
                all.subList(
                    visible.first.coerceIn(0, all.size),
                    (visible.last + 1).coerceIn(0, all.size)
                )
            } else {
                all.take(6) // fallback nếu chưa có visible range
            }

            // Merge visible ngay — ưu tiên cao nhất
            kotlinx.coroutines.coroutineScope {
                visibleItems.map { item ->
                    async(Dispatchers.IO) {
                        val key = itemKey(item)
                        if (bitmapCache.containsKey(key)) return@async
                        val merged = mergeItem(item) ?: return@async
                        bitmapCache[key] = merged
                        withContext(Dispatchers.Main) { _readyKey.value = key }
                    }
                }.awaitAll()
            }

            // Phase 2: merge phần còn lại ở background — limitedParallelism thấp hơn
            val remaining = all.filter { !bitmapCache.containsKey(itemKey(it)) }
            kotlinx.coroutines.coroutineScope {
                remaining.map { item ->
                    async(ioDispatcher) {
                        val key = itemKey(item)
                        if (bitmapCache.containsKey(key)) return@async
                        val merged = mergeItem(item) ?: return@async
                        bitmapCache[key] = merged
                        withContext(Dispatchers.Main) { _readyKey.value = key }
                    }
                }.awaitAll()
            }
        }
    }

    // Fragment gọi hàm này mỗi khi scroll để ViewModel biết visible range
    private val _visibleRange = MutableStateFlow(IntRange.EMPTY)

    fun updateVisibleRange(first: Int, last: Int) {
        _visibleRange.value = first..last

        // Nếu items đã có nhưng bitmap chưa có → merge ngay visible range
        val all = _items.value
        if (all.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val visibleItems = all.subList(
                first.coerceIn(0, all.size),
                (last + 1).coerceIn(0, all.size)
            )
            // Cancel background jobs nếu có — tập trung vào visible
            kotlinx.coroutines.coroutineScope {
                visibleItems.map { item ->
                    async(Dispatchers.IO) {
                        val key = itemKey(item)
                        if (bitmapCache.containsKey(key)) {
                            // Đã có — notify lại để hiển thị
                            withContext(Dispatchers.Main) { _readyKey.value = key }
                            return@async
                        }
                        val merged = mergeItem(item) ?: return@async
                        bitmapCache[key] = merged
                        withContext(Dispatchers.Main) { _readyKey.value = key }
                    }
                }.awaitAll()
            }
        }
    }

    fun forceGenerate() {
        bitmapCache.values.forEach { if (!it.isRecycled) it.recycle() }
        bitmapCache.clear()
        _items.value    = emptyList()
        _readyKey.value = null
        generate()
    }

    fun regenerateAt(pos: Int) {
        val current  = _items.value.toMutableList()
        val old      = current.getOrNull(pos) ?: return
        val sel      = randomSelections(old.template)
        val resolved = resolvePaths(old.template, sel)
        val newItem  = old.copy(selections = sel, resolvedPaths = resolved)
        current[pos] = newItem
        _items.value  = current

        // Xóa bitmap cũ, merge lại
        bitmapCache.remove(itemKey(old))?.let { if (!it.isRecycled) it.recycle() }
        viewModelScope.launch(ioDispatcher) {
            val key    = itemKey(newItem)
            val merged = mergeItem(newItem) ?: return@launch
            bitmapCache[key] = merged
            withContext(Dispatchers.Main) { _readyKey.value = key }
        }
    }

    private suspend fun mergeItem(item: QuickMixItem): Bitmap? {
        val paths = item.template.listPath
            .sortedBy { it.position }
            .mapIndexedNotNull { _, bp ->
                val idx = item.template.listPath.indexOf(bp)
                item.resolvedPaths.getOrNull(idx)
            }
        if (paths.isEmpty()) return null

        // ✅ coroutineScope để dùng async bên trong suspend fun
        val bitmaps = kotlinx.coroutines.coroutineScope {
            paths.map { path ->
                async(Dispatchers.IO) {
                    runCatching {
                        Glide.with(context)
                            .asBitmap()
                            .load(path)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .override(256, 256)
                            .submit()
                            .get()
                    }.getOrNull()
                }
            }.awaitAll().filterNotNull()
        }

        if (bitmaps.isEmpty()) return null

        val w      = bitmaps[0].width
        val h      = bitmaps[0].height
        val merged = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(merged)
        bitmaps.forEach { bmp ->
            canvas.drawBitmap(
                if (bmp.width == w && bmp.height == h) bmp
                else Bitmap.createScaledBitmap(bmp, w, h, true),
                0f, 0f, null
            )
        }
        return merged
    }

    private fun randomSelections(template: CustomModel): ArrayList<SelectionIndex> {
        val list = ArrayList<SelectionIndex>()
        template.listPath.forEachIndexed { bpIdx, bp ->
            val colorCount = bp.listPath.size
            if (colorCount == 0) { list.add(SelectionIndex(bpIdx, 0, 0)); return@forEachIndexed }
            val colorIdx = (0 until colorCount).random()
            val color    = bp.listPath[colorIdx]
            val paths    = color.listPath
            val limit    = if (paths.size > 6) paths.size / 2 else paths.size
            val limited  = paths.subList(0, limit)
            val validIdx = limited.indices.filter { limited[it] != "none" && limited[it] != "dice" }
            val pathIdx  = if (validIdx.isNotEmpty()) validIdx.random() else (0 until limit).random()
            list.add(SelectionIndex(bpIdx, colorIdx, pathIdx))
        }
        return list
    }

    private fun resolvePaths(template: CustomModel, sel: ArrayList<SelectionIndex>): List<String?> =
        template.listPath.mapIndexed { bpIdx, bp ->
            val s     = sel.getOrNull(bpIdx) ?: return@mapIndexed null
            val color = bp.listPath.getOrNull(s.colorIndex) ?: return@mapIndexed null
            val path  = color.listPath.getOrNull(s.pathIndex) ?: return@mapIndexed null
            if (path == "none") null else path
        }

    override fun onCleared() {
        super.onCleared()
        bitmapCache.values.forEach { if (!it.isRecycled) it.recycle() }
        bitmapCache.clear()
    }
}