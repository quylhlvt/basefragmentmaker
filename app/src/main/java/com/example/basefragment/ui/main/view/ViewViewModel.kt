package com.example.basefragment.ui.main.view

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basefragment.data.datalocal.manager.AppDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject



// ViewViewModel.kt
@HiltViewModel
class ViewViewModel @Inject constructor(  private val appDataManager: AppDataManager) : ViewModel() {

    fun downloadFile(context: Context, path: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = runCatching {
                val src = File(path)
                if (!src.exists()) return@runCatching false
                val pictures = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_PICTURES
                )
                pictures.mkdirs()
                val dest = File(pictures, src.name)
                src.copyTo(dest, overwrite = true)
                android.media.MediaScannerConnection.scanFile(
                    context, arrayOf(dest.absolutePath), null, null
                )
                true
            }.getOrDefault(false)
            withContext(Dispatchers.Main) { onResult(success) }
        }
    }


    fun deleteFile(
        path: String,
        isAvatar: Boolean,
        idEdit: String = "",
        onDone: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                // ✅ Nếu là avatar, xóa khỏi customizedCharacters
                if (isAvatar && idEdit.isNotEmpty()) {
                    appDataManager.deleteCustomizedCharacter(idEdit)
                }
                // ✅ Nếu là design, xóa khỏi myDesignPaths
                if (!isAvatar) {
                    val current = appDataManager.myDesignPaths.value.toMutableList()
                    current.remove(path)
                    appDataManager.saveMyDesignToJson(current)
                }
                // Xóa file vật lý
                File(path).delete()
            }
            withContext(Dispatchers.Main) { onDone() }
        }
    }
}