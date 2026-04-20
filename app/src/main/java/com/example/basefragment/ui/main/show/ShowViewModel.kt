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
    fun initWithSelections(templateIndex: Int, savedSelections: ArrayList<SelectionIndex>) {
        viewModelScope.launch(Dispatchers.IO) {
            val templates = appDataManager.templates.value
            val template  = templates.getOrNull(templateIndex) ?: return@launch
            val sorted    = template.listPath.sortedBy { it.zIndex }

            val target = savedSelections.toList()
            val user   = buildDefaultSelections(sorted) // ✅ giờ đã chọn layer hợp lệ đầu tiên

            _state.value = ShowState(
                template         = template,
                listData         = sorted,
                targetSelections = target,
                userSelections   = user,
                currentNavIndex  = 0,
                isLoading        = true,
                matchPercent     = calcPercent(sorted.size, target, user) // ✅ tính lại đúng
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
    ): List<SelectionIndex> = parts.mapIndexed { i, bp ->
        if (i == 0) {
            // ✅ Nav đầu tiên: chọn path hợp lệ đầu tiên
            val colorIdx = 0
            val paths = bp.listPath.getOrNull(colorIdx)?.listPath ?: emptyList()
            val pathIdx = paths.indexOfFirst { it != "none" && it != "dice" }
                .takeIf { it >= 0 } ?: 0
            SelectionIndex(i, colorIdx, pathIdx)
        } else {
            // ✅ Các nav còn lại: để none (pathIndex trỏ vào "none")
            val paths = bp.listPath.getOrNull(0)?.listPath ?: emptyList()
            val noneIdx = paths.indexOfFirst { it == "none" }.takeIf { it >= 0 } ?: 0
            SelectionIndex(i, 0, noneIdx)
        }
    }

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
        var matched = 0
        for (i in 0 until total) {  // ✅ loop theo total, không theo target.indices
            val t = target.getOrNull(i) ?: continue
            val u = user.getOrNull(i)   ?: continue
            if (t.colorIndex == u.colorIndex && t.pathIndex == u.pathIndex) matched++
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

            // ✅ Dùng listData.size thay vì totalNav
            val percent = calcPercent(state.listData.size, state.targetSelections, updated)
            state.copy(userSelections = updated, matchPercent = percent)
        }

        if (_state.value.matchPercent >= 100f) {
            viewModelScope.launch { _onComplete.emit(Unit) }
        }
    }
}