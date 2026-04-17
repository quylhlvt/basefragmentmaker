package com.example.basefragment.ui.main.show

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basefragment.data.datalocal.manager.AppDataManager
import com.example.basefragment.data.model.custom.ColorModel
import com.example.basefragment.data.model.custom.CustomModel
import com.example.basefragment.data.model.custom.SelectionIndex
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI STATE ──────────────────────────────────────────────────────────────────



// ── VIEWMODEL ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ShowViewModel @Inject constructor(
    private val appDataManager: AppDataManager
) : ViewModel() {

    private val _state = MutableStateFlow(ShowState())
    val state: StateFlow<ShowState> = _state.asStateFlow()

    // event navigate khi đạt 100%
    private val _onComplete = MutableSharedFlow<Unit>(replay = 0)
    val onComplete = _onComplete.asSharedFlow()

    // cache bitmap nhân vật random để không render lại
    private var _cachedBitmap: Bitmap? = null
    val cachedBitmap get() = _cachedBitmap

    fun setCachedBitmap(bmp: Bitmap) { _cachedBitmap = bmp }

    override fun onCleared() {
        super.onCleared()
        _cachedBitmap?.recycle()
        _cachedBitmap = null
    }

    // ── INIT ──────────────────────────────────────────────────────────────────

    fun randomize() {
        _cachedBitmap?.recycle()
        _cachedBitmap = null

        viewModelScope.launch(Dispatchers.IO) {
            val templates = appDataManager.templates.value
            if (templates.isEmpty()) return@launch

            val idx      = templates.indices.random()
            val template = templates[idx]
            val sorted   = template.listPath.sortedBy { it.zIndex }
            val target   = buildRandomSelections(sorted)
            val user     = buildDefaultSelections(sorted)

            _state.value = ShowState(
                template         = template,
                listData         = sorted,
                targetSelections = target,
                userSelections   = user,
                currentNavIndex  = 0,
                isLoading        = true,
                matchPercent     = calcPercent(sorted.size, target, user)
            )
        }
    }

    // ── USER SELECTION ────────────────────────────────────────────────────────

    fun selectNav(navIndex: Int) = _state.update { it.copy(currentNavIndex = navIndex) }

    fun selectColor(colorIndex: Int) {
        updateUserSelection { state, old ->
            val bp        = state.listData.getOrNull(state.currentNavIndex) ?: return@updateUserSelection old
            val safeColor = colorIndex.coerceIn(0, bp.listPath.size - 1)
            val maxPath   = (bp.listPath.getOrNull(safeColor)?.listPath?.size ?: 1) - 1
            SelectionIndex(old.bodyPartIndex, safeColor, old.pathIndex.coerceIn(0, maxPath))
        }
    }

    fun selectPath(pathIndex: Int) {
        updateUserSelection { state, old ->
            val bp      = state.listData.getOrNull(state.currentNavIndex) ?: return@updateUserSelection old
            val maxPath = (bp.listPath.getOrNull(old.colorIndex)?.listPath?.size ?: 1) - 1
            SelectionIndex(old.bodyPartIndex, old.colorIndex, pathIndex.coerceIn(0, maxPath))
        }
    }

    fun onLoadingComplete() = _state.update { it.copy(isLoading = false) }

    // ── RESOLVE PATH (để Fragment render ảnh user đang chọn) ─────────────────

    /** Path tại bodyPartIndex theo userSelections. */
    fun resolveUserPathAt(bodyPartIndex: Int): String? {
        val s    = _state.value
        val bp   = s.listData.getOrNull(bodyPartIndex) ?: return null
        val sel  = s.userSelections.getOrNull(bodyPartIndex) ?: return null
        val path = bp.listPath.getOrNull(sel.colorIndex)?.listPath?.getOrNull(sel.pathIndex) ?: return null
        return if (path == "none" || path == "dice") null else path
    }

    /** Path tại bodyPartIndex theo targetSelections (để render nhân vật đáp án). */
    fun resolveTargetPathAt(bodyPartIndex: Int): String? {
        val s    = _state.value
        val bp   = s.listData.getOrNull(bodyPartIndex) ?: return null
        val sel  = s.targetSelections.getOrNull(bodyPartIndex) ?: return null
        val path = bp.listPath.getOrNull(sel.colorIndex)?.listPath?.getOrNull(sel.pathIndex) ?: return null
        return if (path == "none" || path == "dice") null else path
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private fun buildRandomSelections(
        parts: List<com.example.basefragment.data.model.custom.BodyPartModel>
    ): List<SelectionIndex> = parts.mapIndexed { i, bp ->
        val colorCount = bp.listPath.size
        if (colorCount == 0) return@mapIndexed SelectionIndex(i, 0, 0)
        val colorIdx = (0 until colorCount).random()
        val paths    = bp.listPath[colorIdx].listPath
        val validIdx = paths.indices.filter { paths[it] != "none" && paths[it] != "dice" }
        val pathIdx  = if (validIdx.isNotEmpty()) validIdx.random() else 0
        SelectionIndex(i, colorIdx, pathIdx)
    }

    private fun buildDefaultSelections(
        parts: List<com.example.basefragment.data.model.custom.BodyPartModel>
    ): List<SelectionIndex> = parts.mapIndexed { i, _ -> SelectionIndex(i, 0, 0) }

    /**
     * Tính % số nav khớp hoàn toàn (cả colorIndex lẫn pathIndex).
     * Mỗi nav đúng = 100f / totalNav.
     */
    private fun calcPercent(
        total : Int,
        target: List<SelectionIndex>,
        user  : List<SelectionIndex>
    ): Float {
        if (total == 0) return 0f
        val matched = target.indices.count { i ->
            val t = target.getOrNull(i) ?: return@count false
            val u = user.getOrNull(i)   ?: return@count false
            t.colorIndex == u.colorIndex && t.pathIndex == u.pathIndex
        }
        return (matched.toFloat() / total.toFloat()) * 100f
    }

    private fun updateUserSelection(
        transform: (ShowState, SelectionIndex) -> SelectionIndex
    ) {
        _state.update { state ->
            val navIdx  = state.currentNavIndex
            val old     = state.userSelections.getOrElse(navIdx) { SelectionIndex(navIdx, 0, 0) }
            val new     = transform(state, old)
            val updated = state.userSelections.toMutableList()
            if (navIdx < updated.size) updated[navIdx] = new
            else {
                while (updated.size < navIdx) updated.add(SelectionIndex(updated.size, 0, 0))
                updated.add(new)
            }
            val percent = calcPercent(state.totalNav, state.targetSelections, updated)
            state.copy(userSelections = updated, matchPercent = percent)
        }

        // Kiểm tra đạt 100% sau khi update
        val currentPercent = _state.value.matchPercent
        if (currentPercent >= 100f) {
            viewModelScope.launch { _onComplete.emit(Unit) }
        }
    }
}