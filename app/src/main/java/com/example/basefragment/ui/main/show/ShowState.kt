package com.example.basefragment.ui.main.show

import com.example.basefragment.data.model.custom.ColorModel
import com.example.basefragment.data.model.custom.CustomModel
import com.example.basefragment.data.model.custom.SelectionIndex

data class ShowState(
    val template        : CustomModel?        = null,
    val listData        : List<com.example.basefragment.data.model.custom.BodyPartModel> = emptyList(),
    val targetSelections: List<SelectionIndex> = emptyList(), // nhân vật random (đáp án)
    val userSelections  : List<SelectionIndex> = emptyList(), // lựa chọn của user
    val currentNavIndex : Int     = 0,
    val isLoading       : Boolean = true,
    val matchPercent    : Float   = 0f   // 0f..100f
) {
    val totalNav: Int get() = listData.size

    val currentColors: List<ColorModel>
        get() = listData.getOrNull(currentNavIndex)?.listPath ?: emptyList()

    val currentPaths: List<String>
        get() {
            val sel = userSelections.getOrNull(currentNavIndex) ?: return emptyList()
            return listData.getOrNull(currentNavIndex)
                ?.listPath?.getOrNull(sel.colorIndex)?.listPath ?: emptyList()
        }

    val currentColorIndex: Int get() = userSelections.getOrNull(currentNavIndex)?.colorIndex ?: 0
    val currentPathIndex : Int get() = userSelections.getOrNull(currentNavIndex)?.pathIndex  ?: 0
    val hasMultipleColors: Boolean get() = (listData.getOrNull(currentNavIndex)?.listPath?.size ?: 0) > 1
}