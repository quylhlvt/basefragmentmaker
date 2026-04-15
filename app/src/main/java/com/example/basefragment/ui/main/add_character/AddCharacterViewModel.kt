package com.example.basefragment.ui.main.add_character

import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.ViewModel
import com.example.basefragment.data.model.addcharacter.SelectedAddModel
import com.example.basefragment.utils.DataLocal
import com.example.basefragment.data.model.addcharacter.draw.Draw
import com.pfp.ocmaker.create.maker.data.model.draw.DrawableDraw
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AddCharacterViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Lists for adapters (converted từ ViewModelActivity data)
    var backgroundImageList: ArrayList<SelectedAddModel> = arrayListOf()
    var backgroundColorList: ArrayList<SelectedAddModel> = arrayListOf()
    var stickerList: ArrayList<SelectedAddModel> = arrayListOf()
    var speechList: ArrayList<SelectedAddModel> = arrayListOf()
    var textFontList: ArrayList<SelectedAddModel> = arrayListOf()
    var textColorList: ArrayList<SelectedAddModel> = arrayListOf()

    // StateFlows
    private val _typeNavigation = MutableStateFlow<Int>(-1)
    val typeNavigation = _typeNavigation.asStateFlow()

    private val _typeBackground = MutableStateFlow<Int>(-1)
    val typeBackground = _typeBackground.asStateFlow()

    private val _isFocusEditText = MutableStateFlow<Boolean>(false)
    val isFocusEditText = _isFocusEditText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _backgroundImagePath = MutableStateFlow<String?>(null)
    val backgroundImagePath = _backgroundImagePath

    fun setBackgroundImage(path: String?) {
        _backgroundImagePath.value = path
    }

    var currentDraw: Draw? = null
    var drawViewList: ArrayList<Draw> = arrayListOf()

    // Layout params
    lateinit var layoutParams: ViewGroup.MarginLayoutParams
    var originalMarginBottom: Int = 0

    // Path default
    var pathDefault = ""

    // ========== Functions ==========

    fun setTypeNavigation(type: Int) {
        _typeNavigation.value = type
    }

    fun setTypeBackground(type: Int) {
        _typeBackground.value = type
    }

    fun setIsFocusEditText(status: Boolean) {
        _isFocusEditText.value = status
    }

    /**
     * Load data từ ViewModelActivity (đã có sẵn backgrounds, stickers, speeches)
     * Chỉ cần convert sang SelectedAddModel và load colors/fonts từ DataLocal
     */
    // Đổi thành hàm thường, không suspend, không withContext
    fun loadDataFromMainViewModel(
        backgrounds: List<String>,
        stickers: List<String>,
        speeches: List<String>
    ) {
        // Chạy thẳng trên Main — convert list chỉ tốn < 1ms
        backgroundImageList.clear()
        backgroundImageList.addAll(backgrounds.map { SelectedAddModel(path = it) })

        backgroundColorList.clear()
        backgroundColorList.addAll(DataLocal.getBackgroundColorDefault(context))

        stickerList.clear()
        stickerList.addAll(stickers.map { SelectedAddModel(path = it) })

        speechList.clear()
        speechList.addAll(speeches.map { SelectedAddModel(path = it) })

        textFontList.clear()
        textFontList.addAll(DataLocal.getTextFontDefault())
        textFontList.firstOrNull()?.isSelected = true

        textColorList.clear()
        textColorList.addAll(DataLocal.getTextColorDefault(context))
        textColorList.getOrNull(1)?.isSelected = true
    }

    /**
     * Update background image selection
     */
    suspend fun updateBackgroundImageSelected(position: Int) {
        withContext(Dispatchers.Default) {
            backgroundColorList = backgroundColorList
                .map { it.copy(isSelected = false) }
                .toCollection(ArrayList())

            backgroundImageList.forEachIndexed { index, model ->
                model.isSelected = index == position
            }
        }
    }

    /**
     * Update background color selection
     */
    suspend fun updateBackgroundColorSelected(position: Int) {
        withContext(Dispatchers.Default) {
            backgroundImageList = backgroundImageList
                .map { it.copy(isSelected = false) }
                .toCollection(ArrayList())

            backgroundColorList.forEachIndexed { index, model ->
                model.isSelected = index == position
            }
        }
    }

    /**
     * Update text font selection
     */
    fun updateTextFontSelected(position: Int) {
        textFontList = textFontList
            .map { it.copy(isSelected = false) }
            .toCollection(ArrayList())

        textFontList.forEachIndexed { index, model ->
            model.isSelected = index == position
        }
    }

    /**
     * Update text color selection
     */
    fun updateTextColorSelected(position: Int) {
        textColorList = textColorList
            .map { it.copy(isSelected = false) }
            .toCollection(ArrayList())

        textColorList.forEachIndexed { index, model ->
            model.isSelected = index == position
        }
    }

    /**
     * Update current draw
     */
    fun updateCurrentCurrentDraw(draw: Draw) {
        currentDraw = draw
    }

    /**
     * Add draw to list
     */
    fun addDrawView(draw: Draw) {
        drawViewList.add(draw)
    }

    /**
     * Delete draw from list
     */
    fun deleteDrawView(draw: Draw) {
        drawViewList.removeIf { it == draw }
    }

    /**
     * Update default path
     */
    fun updatePathDefault(path: String) {
        pathDefault = path
    }

    /**
     * Load drawable emoji from bitmap
     */
    fun loadDrawableEmoji(
        bitmap: Bitmap,
        isCharacter: Boolean = false,
        isText: Boolean = false
    ): DrawableDraw {
        val drawable = bitmap.toDrawable(context.resources)
        val timestamp = SimpleDateFormat("dd_MM_yyyy_hh_mm_ss").format(Date())
        val drawableEmoji = DrawableDraw(drawable, "${timestamp}.png")
        drawableEmoji.isCharacter = isCharacter
        drawableEmoji.isText = isText
        return drawableEmoji
    }

    /**
     * Reset all draws
     */
    fun resetDraw() {
        drawViewList.clear()
        currentDraw = null
    }

    /**
     * Save image from view
     */
//    fun saveImageFromView(view: View): Flow<SaveState> = flow {
//        emit(SaveState.Loading)
//        try {
//            val bitmap = BitmapHelper.createBimapFromView(view)
//            MediaHelper.saveBitmapToInternalStorage(
//                context,
//                ValueKey.DOWNLOAD_ALBUM,
//                bitmap
//            ).collect { state ->
//                emit(state)
//            }
//        } catch (e: Exception) {
//            emit(SaveState.Error(e.message ?: "Unknown error"))
//        }
//    }.flowOn(Dispatchers.IO)

    /**
     * Clear all data
     */
    fun clearAllData() {
        backgroundImageList.clear()
        backgroundColorList.clear()
        stickerList.clear()
        speechList.clear()
        textFontList.clear()
        textColorList.clear()
        drawViewList.clear()
        currentDraw = null
        pathDefault = ""
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up if needed
    }
}