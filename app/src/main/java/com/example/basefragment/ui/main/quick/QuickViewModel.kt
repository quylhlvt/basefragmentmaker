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
import kotlinx.coroutines.isActive
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

    val bitmapCache = ConcurrentHashMap<String, Bitmap>()

    private val _readyKey = MutableStateFlow<String?>(null)
    val readyKey: StateFlow<String?> = _readyKey.asStateFlow()

    // ── MỚI: khai báo đủ ──────────────────────────────────────────────────
    private val mergeDispatcher = Dispatchers.IO.limitedParallelism(12)
    private var backgroundJob: kotlinx.coroutines.Job? = null
    val keyToPosition = ConcurrentHashMap<String, Int>()
    private val _visibleRange = MutableStateFlow(0..5)

    companion object {
        const val PER_TEMPLATE = 10       // giảm từ 10 → 4
        const val PAGE_SIZE    = 6
        const val MERGE_SIZE   = 192     // giảm từ 256 → 192
    }

    fun itemKey(item: QuickMixItem) =
        "${item.templateIndex}_${item.selections.hashCode()}"

    // ── GENERATE ──────────────────────────────────────────────────────────
    fun generate() {
        if (_items.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.Default) {
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
            all.forEachIndexed { i, item -> keyToPosition[itemKey(item)] = i }

            _items.value     = all
            _isLoading.value = false

            mergeAll(all)
        }
    }

    // ── MERGE ALL (windowed) ───────────────────────────────────────────────
    private fun mergeAll(all: List<QuickMixItem>) {
        backgroundJob?.cancel()
        backgroundJob = viewModelScope.launch(mergeDispatcher) {

            // Window 1: merge PAGE_SIZE item đầu (visible ngay khi mở)
            all.take(PAGE_SIZE)
                .map { async(mergeDispatcher) { mergeAndCache(it) } }
                .awaitAll()

            // Window 2+: background tuần tự theo batch
            all.drop(PAGE_SIZE).chunked(PAGE_SIZE).forEach { batch ->
                if (!isActive) return@launch
                // ── ƯU TIÊN: bỏ qua item đã có trong visible range ──
                // (chúng đã được merge bởi updateVisibleRange rồi)
                val needed = batch.filter { !bitmapCache.containsKey(itemKey(it)) }
                if (needed.isEmpty()) return@forEach
                needed.map { async(mergeDispatcher) { mergeAndCache(it) } }.awaitAll()
            }
        }
    }


    // ── MERGE AND CACHE ───────────────────────────────────────────────────
    private suspend fun mergeAndCache(item: QuickMixItem) {
        val key = itemKey(item)
        if (bitmapCache.containsKey(key)) {
            withContext(Dispatchers.Main) { _readyKey.value = key }
            return
        }
        val merged = mergeItem(item) ?: return
        bitmapCache[key] = merged
        withContext(Dispatchers.Main) { _readyKey.value = key }
    }

    // ── VISIBLE RANGE (scroll) ────────────────────────────────────────────
    fun updateVisibleRange(first: Int, last: Int) {
        val newRange = first..last
        if (_visibleRange.value == newRange) return
        _visibleRange.value = newRange

        val all = _items.value
        if (all.isEmpty()) return

        val safeFirst = first.coerceIn(0, all.size)
        val safeLast  = (last + 1).coerceIn(0, all.size)
        if (safeFirst >= safeLast) return

        val visible = all.subList(safeFirst, safeLast)
        val missing = visible.filter { !bitmapCache.containsKey(itemKey(it)) }
        if (missing.isEmpty()) return

        // ── Hủy background job, ưu tiên visible trước ──
        backgroundJob?.cancel()

        backgroundJob = viewModelScope.launch(mergeDispatcher) {
            // 1. Merge visible ngay lập tức
            missing
                .map { async(mergeDispatcher) { mergeAndCache(it) } }
                .awaitAll()

            // 2. Tiếp tục background các item còn lại (chưa cache)
            if (!isActive) return@launch
            all.filter { !bitmapCache.containsKey(itemKey(it)) }
                .chunked(PAGE_SIZE)
                .forEach { batch ->
                    if (!isActive) return@launch
                    batch.map { async(mergeDispatcher) { mergeAndCache(it) } }.awaitAll()
                }
        }
    }

    // ── FORCE GENERATE ────────────────────────────────────────────────────
    fun forceGenerate() {
        backgroundJob?.cancel()
        bitmapCache.values.forEach { if (!it.isRecycled) it.recycle() }
        bitmapCache.clear()
        keyToPosition.clear()
        _items.value    = emptyList()
        _readyKey.value = null
        generate()
    }

    // ── REGENERATE AT ─────────────────────────────────────────────────────
    fun regenerateAt(pos: Int) {
        val current  = _items.value.toMutableList()
        val old      = current.getOrNull(pos) ?: return
        val sel      = randomSelections(old.template)
        val resolved = resolvePaths(old.template, sel)
        val newItem  = old.copy(selections = sel, resolvedPaths = resolved)
        current[pos] = newItem
        _items.value  = current

        bitmapCache.remove(itemKey(old))?.let { if (!it.isRecycled) it.recycle() }
        keyToPosition[itemKey(newItem)] = pos

        viewModelScope.launch(mergeDispatcher) {
            mergeAndCache(newItem)
        }
    }

    // ── MERGE ITEM ────────────────────────────────────────────────────────
    private suspend fun mergeItem(item: QuickMixItem): Bitmap? {
        // Dùng resolvedPaths có sẵn — không sort lại
        val paths = item.resolvedPaths.filterNotNull().filter { it.isNotBlank() }
        if (paths.isEmpty()) return null

        val bitmaps = kotlinx.coroutines.coroutineScope {
            paths.map { path ->
                async(mergeDispatcher) {
                    runCatching {
                        Glide.with(context)
                            .asBitmap()
                            .load(path)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .override(MERGE_SIZE, MERGE_SIZE)  // ✅ dùng MERGE_SIZE
                            .submit()
                            .get()
                    }.getOrNull()
                }
            }.awaitAll().filterNotNull()
        }

        if (bitmaps.isEmpty()) return null

        // Canvas merge trên Default dispatcher
        return withContext(Dispatchers.Default) {
            val merged = Bitmap.createBitmap(MERGE_SIZE, MERGE_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(merged)
            bitmaps.forEach { bmp ->
                canvas.drawBitmap(
                    if (bmp.width == MERGE_SIZE && bmp.height == MERGE_SIZE) bmp
                    else Bitmap.createScaledBitmap(bmp, MERGE_SIZE, MERGE_SIZE, true),
                    0f, 0f, null
                )
            }
            merged
        }
    }

    // ── RANDOM SELECTIONS ─────────────────────────────────────────────────
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

    // ── RESOLVE PATHS ─────────────────────────────────────────────────────
    private fun resolvePaths(template: CustomModel, sel: ArrayList<SelectionIndex>): List<String?> =
        template.listPath.mapIndexed { bpIdx, bp ->
            val s     = sel.getOrNull(bpIdx) ?: return@mapIndexed null
            val color = bp.listPath.getOrNull(s.colorIndex) ?: return@mapIndexed null
            val path  = color.listPath.getOrNull(s.pathIndex) ?: return@mapIndexed null
            if (path == "none") null else path
        }

    // ── CLEAR ─────────────────────────────────────────────────────────────
    override fun onCleared() {
        super.onCleared()
        backgroundJob?.cancel()
        bitmapCache.values.forEach { if (!it.isRecycled) it.recycle() }
        bitmapCache.clear()
    }
}