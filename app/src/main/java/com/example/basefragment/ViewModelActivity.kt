package com.example.basefragment

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.basefragment.core.helper.NetworkMonitor
import com.example.basefragment.data.datalocal.manager.AppDataManager
import com.example.basefragment.data.model.custom.CustomModel
import com.example.basefragment.data.model.custom.SelectionIndex
import com.example.basefragment.data.usecase.GetCatalogueUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
@HiltViewModel
class ViewModelActivity @Inject constructor(
    private val getCatalogueUseCase: GetCatalogueUseCase,
    val appDataManager: AppDataManager,
    private val networkFlow: Flow<Boolean>,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ── EXPOSED FLOWS ─────────────────────────────────────────────────────────

    val characters:           StateFlow<List<CustomModel>> = appDataManager.characters
    val templates:            StateFlow<List<CustomModel>> = appDataManager.templates
    val customizedCharacters: StateFlow<List<CustomModel>> = appDataManager.customizedCharacters
    val backgrounds:          StateFlow<List<String>>      = appDataManager.backgrounds
    val backgroundTexts:      StateFlow<List<String>>      = appDataManager.backgroundTexts
    val stickers:             StateFlow<List<String>>      = appDataManager.stickers
    val speechs:              StateFlow<List<String>>      = appDataManager.speechs
    val myDesignPaths:        StateFlow<List<String>>      = appDataManager.myDesignPaths
    val isLoading:            StateFlow<Boolean>           = appDataManager.isLoading
    val error:                StateFlow<String?>           = appDataManager.error

    val networkOnline: StateFlow<Boolean> = networkFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly, // ← Eagerly để không miss network event
            initialValue = false
        )

    // Guard chống gọi fetch trùng từ nhiều fragment
    private var isFetchingOnline = false

    // ── INIT ──────────────────────────────────────────────────────────────────

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                Log.d("ViewModelActivity", "🚀 loadInitialData start")
                appDataManager.loadInitialData()
                Log.d("ViewModelActivity", "✅ local data loaded")
                // Fetch online ngay nếu có mạng
                fetchOnlineTemplatesInternal()
            } catch (e: Exception) {
                Log.e("ViewModelActivity", "❌ Init error: ${e.message}", e)
            }
        }
    }

    // ── FETCH (duy nhất 1 hàm, có guard) ─────────────────────────────────────

    /**
     * Gọi từ mọi nơi đều an toàn — guard đảm bảo chỉ 1 request chạy tại 1 thời điểm.
     * BaseFragment gọi khi vào màn + có mạng + chưa có online data.
     */
    private suspend fun fetchOnlineTemplatesInternal() {
        if (isFetchingOnline) {
            Log.d("ViewModelActivity", "⏭️ Already fetching, skip")
            return
        }
        isFetchingOnline = true
        try {
            Log.d("ViewModelActivity", "📡 fetchOnlineTemplates start")
            val result = getCatalogueUseCase()
            if (result.isSuccess) {
                val newTemplates = result.getOrNull() ?: return
                appDataManager.saveApiCache(newTemplates)
                appDataManager.mergeApiTemplates(newTemplates)
                prefetchTemplateImages(newTemplates)
                Log.d("ViewModelActivity", "✅ Online templates loaded: ${newTemplates.size}")
            } else {
                Log.e("ViewModelActivity", "❌ API failed: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.e("ViewModelActivity", "❌ fetchOnlineTemplates error: ${e.message}", e)
        } finally {
            isFetchingOnline = false
        }
    }

    /** Public — BaseFragment và pull-to-refresh gọi */
    fun fetchOnlineTemplates() {
        viewModelScope.launch { fetchOnlineTemplatesInternal() }
    }

    fun forceReloadAll() {
        viewModelScope.launch { appDataManager.forceReloadAll() }
    }

    fun refreshApiData() {
        viewModelScope.launch { appDataManager.refreshFromApi() }
    }

    // ── PREFETCH IMAGES ───────────────────────────────────────────────────────

    private suspend fun prefetchTemplateImages(templates: List<CustomModel>) {
        withContext(Dispatchers.IO) {
            templates.take(5).forEach { template ->
                template.listPath.forEach { bp ->
                    val firstColor = bp.listPath.firstOrNull() ?: return@forEach
                    val firstPath  = firstColor.listPath
                        .firstOrNull { it != "none" && it != "dice" }
                        ?: return@forEach
                    runCatching {
                        Glide.with(context)
                            .asBitmap()
                            .load(firstPath)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .override(256, 256)
                            .preload()
                    }
                }
            }
        }
    }

    // ── QUERIES ───────────────────────────────────────────────────────────────

    fun getCharacterByIndex(index: Int): CustomModel? = appDataManager.getCharacterByIndex(index)
    fun getCharacterById(id: String): CustomModel?    = appDataManager.getCharacterById(id)
    fun isTemplate(id: String): Boolean               = appDataManager.isTemplate(id)
    fun getTemplateIndexByAvt(avt: String): Int       = appDataManager.getTemplateIndexByAvt(avt)
    fun getCharacterIndexById(id: String): Int        = characters.value.indexOfFirst { it.id == id }

    fun getTemplateIndexForCustomized(customizedId: String): Int {
        val customized = customizedCharacters.value.firstOrNull { it.id == customizedId }
            ?: return -1
        val byTemplateId = customized.templateId?.let { tplId ->
            templates.value.indexOfFirst { it.id == tplId }.takeIf { it >= 0 }
        }
        if (byTemplateId != null) return byTemplateId
        return templates.value.indexOfFirst { it.avatar == customized.avatar }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    fun saveCharacterWithSelections(
        character:  CustomModel,
        selections: List<SelectionIndex>,
        imageSave:  String  = "",
        isFlipped:  Boolean = false
    ) {
        viewModelScope.launch {
            val toSave = if (isTemplate(character.id)) {
                character.copy(
                    id         = UUID.randomUUID().toString(),
                    templateId = character.id,
                    selections = ArrayList(selections),
                    imageSave  = imageSave,
                    isFlipped  = isFlipped,
                    updatedAt  = System.currentTimeMillis()
                )
            } else {
                character.copy(
                    selections = ArrayList(selections),
                    imageSave  = imageSave,
                    isFlipped  = isFlipped,
                    updatedAt  = System.currentTimeMillis()
                )
            }
            appDataManager.updateCustomizedCharacter(toSave)
        }
    }

    fun deleteCharacter(characterId: String) {
        viewModelScope.launch {
            if (isTemplate(characterId)) return@launch
            appDataManager.deleteCustomizedCharacter(characterId)
        }
    }

    // ── SELECTION HELPERS ─────────────────────────────────────────────────────

    fun resolvePath(character: CustomModel, sel: SelectionIndex): String? =
        appDataManager.resolvePathFromSelection(character, sel)

    fun resolveAllPaths(character: CustomModel, selections: List<SelectionIndex>): List<Pair<Int, String>> =
        appDataManager.resolveAllPaths(character, selections)

    // ── MY DESIGNS ────────────────────────────────────────────────────────────

    fun addMyDesign(path: String)    { viewModelScope.launch { appDataManager.addMyDesignPath(path) } }
    fun removeMyDesign(path: String) { viewModelScope.launch { appDataManager.removeMyDesignPath(path) } }

    // ── CLEAR ─────────────────────────────────────────────────────────────────

    fun clearData() { appDataManager.clearData() }
}