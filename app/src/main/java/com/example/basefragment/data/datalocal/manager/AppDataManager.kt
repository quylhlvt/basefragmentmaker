package com.example.basefragment.data.datalocal.manager

import android.content.Context
import android.util.Log
import com.example.basefragment.data.model.custom.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDataManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG             = "AppDataManager"
        private const val ASSET_PREFIX    = "file:///android_asset"
        private const val TEMPLATES_FILE  = "templates.json"
        private const val CUSTOMIZED_FILE = "customized.json"
        private const val MY_DESIGNS_FILE = "my_designs.json"
        private const val API_CACHE_FILE = "api_cache.json"
        private const val KEY_TEMPLATES   = "templates"
        private const val KEY_CUSTOMIZED  = "customized"
        private const val KEY_MY_DESIGNS  = "my_designs"
        private const val KEY_API_CACHE   = "api_cache"

    }
    private val mmkv = MMKV.defaultMMKV()
    private val gson = Gson()

    // ── STATE FLOWS ──────────────────────────────────────────────────────────

    private val _templates            = MutableStateFlow<List<CustomModel>>(emptyList())
    val templates: StateFlow<List<CustomModel>> = _templates.asStateFlow()

    private val _customizedCharacters = MutableStateFlow<List<CustomModel>>(emptyList())
    val customizedCharacters: StateFlow<List<CustomModel>> = _customizedCharacters.asStateFlow()

    private val _characters           = MutableStateFlow<List<CustomModel>>(emptyList())
    val characters: StateFlow<List<CustomModel>> = _characters.asStateFlow()

    private val _backgrounds          = MutableStateFlow<List<String>>(emptyList())
    val backgrounds: StateFlow<List<String>> = _backgrounds.asStateFlow()

    private val _backgroundTexts      = MutableStateFlow<List<String>>(emptyList())
    val backgroundTexts: StateFlow<List<String>> = _backgroundTexts.asStateFlow()

    private val _stickers             = MutableStateFlow<List<String>>(emptyList())
    val stickers: StateFlow<List<String>> = _stickers.asStateFlow()

    private val _speechs              = MutableStateFlow<List<String>>(emptyList())
    val speechs: StateFlow<List<String>> = _speechs.asStateFlow()

    private val _myDesignPaths        = MutableStateFlow<List<String>>(emptyList())
    val myDesignPaths: StateFlow<List<String>> = _myDesignPaths.asStateFlow()

    private val _isLoading            = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error                = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isQuickLoading       = MutableStateFlow(false)
    val isQuickLoading: StateFlow<Boolean> = _isQuickLoading.asStateFlow()

    private val _errorQuick           = MutableStateFlow<String?>(null)
    val errorQuick: StateFlow<String?> = _errorQuick.asStateFlow()

    private var isDataLoaded      = false
    private var isDataQuickLoaded = false

    // ── INIT ─────────────────────────────────────────────────────────────────

    suspend fun loadInitialData() {
        if (isDataLoaded) { Log.d(TAG, "⚠️ Already loaded, skip"); return }
        _isLoading.value = true
        _error.value = null

        withContext(Dispatchers.IO) {
            try {
                loadTemplates()
                loadCustomizedCharacters()
                combineCharacterLists()
                coroutineScope {
                    launch { loadBackgrounds() }
                    launch { loadBackgroundTexts() }
                    launch { loadStickers() }
                    launch { loadSpeechs() }
                    launch { loadMyDesigns() }
                }
                isDataLoaded = true
                Log.d(TAG, "✅ Loaded – templates:${_templates.value.size} custom:${_customizedCharacters.value.size}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Init error: ${e.message}", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }


    /**
     * Merge API templates vào _templates:
     * - Giữ nguyên local templates (template_*)
     * - Thay thế/thêm online templates (online_*)
     */
    fun mergeApiTemplates(onlineTemplates: List<CustomModel>) {
        val localOnly = _templates.value.filter { !it.id.startsWith("online_") }
        _templates.value = onlineTemplates + localOnly
        combineCharacterLists()
        Log.d(TAG, "✅ Merged: ${onlineTemplates.size} online + ${localOnly.size} local templates")
    }


    // ── TEMPLATE LOADING ─────────────────────────────────────────────────────

    private suspend fun loadTemplates() = withContext(Dispatchers.IO) {
        val cached = loadTemplatesFromJson()
        if (cached.isNotEmpty()) {
            _templates.value = cached
            Log.d(TAG, "✅ ${cached.size} templates from cache")
            return@withContext
        }
        loadTemplatesFromAssets()
    }

    suspend fun loadTemplatesFromAssets() = withContext(Dispatchers.IO) {
        try {
            val assetManager = context.assets
            val result       = arrayListOf<CustomModel>()
            val folders      = assetManager.list("data") ?: return@withContext

            folders.forEach { folder ->
                val basePath = "data/$folder"
                val items    = (assetManager.list(basePath) ?: return@forEach)
                    .sortedBy { it.substringBefore("-").toIntOrNull() ?: 999 }  // sort theo gia tri x trong "x-y"

                val bodyParts = arrayListOf<BodyPartModel>()
                var avatar    = ""
                var position  = 0

                items.forEach { item ->
                    val fullPath = "$basePath/$item"
                    val contents = assetManager.list(fullPath)

                    if (contents.isNullOrEmpty()) {
                        avatar = "$ASSET_PREFIX/$fullPath"
                        return@forEach
                    }

                    val parts = item.split("-")
                    val x = parts.getOrNull(0)?.toIntOrNull() ?: position
                    val y = parts.getOrNull(1)?.toIntOrNull() ?: position

                    val nav = contents.firstOrNull { it.startsWith("nav.") }
                        ?.let { "$ASSET_PREFIX/$fullPath/$it" } ?: ""

                    val colors        = arrayListOf<ColorModel>()
                    val listThumbPath = arrayListOf<String>()
                    val listSinglePath = arrayListOf<String>()

                    contents.filter { !it.startsWith("nav.") }.forEach { layer ->
                        val layerPath = "$fullPath/$layer"
                        val files     = assetManager.list(layerPath)

                        if (files.isNullOrEmpty()) {
                            // File đơn (không có subfolder)
                            val fullAssetPath = "$ASSET_PREFIX/$layerPath"
                            if (layer.startsWith("thumb_")) {
                                listThumbPath.add(fullAssetPath)
                            } else {
                                listSinglePath.add(fullAssetPath)
                            }
                        } else {
                            // Có subfolder màu
                            colors.add(ColorModel(layer, ArrayList(files.map { "$ASSET_PREFIX/$layerPath/$it" })))
                        }
                    }

                    if (colors.isEmpty() && listSinglePath.isNotEmpty()) {
                        if (listThumbPath.isNotEmpty()) {
                            // Có file thumb_ thật → sort và dùng bình thường
                            listThumbPath.sortBy {
                                it.substringAfterLast("thumb_").substringBeforeLast(".").toIntOrNull() ?: 0
                            }
                            listSinglePath.sortBy {
                                it.substringAfterLast("/").substringBeforeLast(".").toIntOrNull() ?: 0
                            }
                            colors.add(ColorModel("", ArrayList(listSinglePath)))
                        } else {
                            // Không có thumb_ → flat list, tách đôi giống 226
                            val sorted = listSinglePath.sortedBy {
                                it.substringAfterLast("/").substringBeforeLast(".").toIntOrNull() ?: 0
                            }
                            val half  = sorted.size / 2
                            val real  = ArrayList(sorted.subList(0, half))
                            val thumb = ArrayList(sorted.subList(half, sorted.size))
                            listThumbPath.addAll(thumb)
                            colors.add(ColorModel("", real))
                        }
                    }

                    applySpecialPrefixes(colors, item)

                    bodyParts.add(BodyPartModel(
                        nav            = nav,
                        listPath       = colors,
                        listThumbPath  = listThumbPath,
                        listSinglePath = listSinglePath,
                        position       = x,
                        zIndex         = y
                    ))
                    position++
                }

                result.add(CustomModel(
                    id         = "template_$folder",
                    avatar     = avatar,
                    listPath   = ArrayList(bodyParts),
                    selections = arrayListOf(),
                    updatedAt  = System.currentTimeMillis()
                ))
            }

            _templates.value = result
            saveTemplatesToJson(result)
            Log.d(TAG, "✅ ${result.size} templates from assets")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Asset load error: ${e.message}", e)
        }
    }
    private fun applySpecialPrefixes(colors: ArrayList<ColorModel>, itemName: String) {
        val pos = itemName.substringAfter("-").toIntOrNull() ?: return
        colors.forEach { cm ->
            if (cm.listPath.isEmpty()) return@forEach  // ← guard: bỏ qua nếu rỗng
            when {
                pos == 1 -> {
                    // Nav0 / body chính: CHỈ thêm "dice", không "none"
                    if (cm.listPath.first() != "dice") cm.listPath.add(0, "dice")
                }
                else -> {
                    // Nav khác: thêm "none" trước, "dice" sau
                    if (cm.listPath.first() != "none") {
                        cm.listPath.add(0, "none")
                        cm.listPath.add(1, "dice")
                    }
                }
            }
        }
    }

    // ── TEMPLATE CACHE ────────────────────────────────────────────────────────

    private fun saveTemplatesToJson(templates: List<CustomModel>) = runCatching {
        mmkv.encode(KEY_TEMPLATES, gson.toJson(templates))
    }.onFailure { Log.e(TAG, "❌ Cache save error", it) }

    private suspend fun loadTemplatesFromJson(): List<CustomModel> = withContext(Dispatchers.IO) {
        runCatching {
            val json = mmkv.decodeString(KEY_TEMPLATES) ?: return@withContext emptyList()
            val type = object : TypeToken<List<CustomModel>>() {}.type
            gson.fromJson<List<CustomModel>>(json, type) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    // ── CUSTOMIZED CHARACTERS ─────────────────────────────────────────────────


// Chỉ sửa 2 hàm này trong AppDataManager

    private suspend fun saveCustomizedCharacters(characters: List<CustomModel>) = withContext(Dispatchers.IO) {
        runCatching {
            val dtos = characters.map { it.toDto() }
            mmkv.encode(KEY_CUSTOMIZED, gson.toJson(dtos))
        }.onFailure { Log.e(TAG, "❌ Save customized error", it) }
    }

    suspend fun saveApiCache(templates: List<CustomModel>) = withContext(Dispatchers.IO) {
        runCatching {
            mmkv.encode(KEY_API_CACHE, gson.toJson(templates))
            Log.d(TAG, "✅ Saved ${templates.size} API templates to cache")
        }.onFailure { Log.e(TAG, "❌ saveApiCache error", it) }
    }
    private suspend fun loadCustomizedCharacters() = withContext(Dispatchers.IO) {
        runCatching {
            val json = mmkv.decodeString(KEY_CUSTOMIZED)
            if (json.isNullOrEmpty()) { _customizedCharacters.value = emptyList(); return@withContext }

            val type = object : TypeToken<List<CustomizedCharacterDto>>() {}.type
            val dtos: List<CustomizedCharacterDto> = gson.fromJson(json, type) ?: emptyList()

            val models = dtos.map { dto ->
                val template = _templates.value.find { t ->
                    t.id == dto.templateId || t.avatar == dto.avatar
                }
                dto.toModel(templateListPath = template?.listPath ?: arrayListOf())
            }

            _customizedCharacters.value = models
            Log.d(TAG, "✅ Loaded ${models.size} customized from dto")
        }.onFailure {
            _customizedCharacters.value = emptyList()
            Log.e(TAG, "❌ Load customized error", it)
        }
    }
    suspend fun updateCustomizedCharacter(character: CustomModel) = withContext(Dispatchers.IO) {
        val list  = _customizedCharacters.value.toMutableList()
        val index = list.indexOfFirst { it.id == character.id }
        if (index >= 0) list[index] = character else list.add(character)
        _customizedCharacters.value = list
        saveCustomizedCharacters(list)
        combineCharacterLists()
    }

    suspend fun deleteCustomizedCharacter(characterId: String) = withContext(Dispatchers.IO) {
        val list    = _customizedCharacters.value.toMutableList()
        val removed = list.removeIf { it.id == characterId }
        if (removed) {
            _customizedCharacters.value = list
            saveCustomizedCharacters(list)
            combineCharacterLists()
        }
    }

    // ── COMBINE ───────────────────────────────────────────────────────────────

    private fun combineCharacterLists() {
        _characters.value = _templates.value + _customizedCharacters.value
    }

    fun prependOnlineTemplates(onlineTemplates: List<CustomModel>) {
        val existing = _templates.value.filter { !it.id.startsWith("online_") }
        _templates.value = onlineTemplates + existing
        combineCharacterLists()
    }

    // ── UTILITIES ─────────────────────────────────────────────────────────────

    fun isTemplate(id: String)             = id.startsWith("template_") || id.startsWith("online_")
    fun getCharacterByIndex(index: Int)    = _characters.value.getOrNull(index)
    fun getCharacterById(id: String)       = _characters.value.find { it.id == id }
    fun getTemplateIndexByAvt(avt: String) = _characters.value.indexOfFirst { it.avatar == avt }

    fun resolvePathFromSelection(character: CustomModel, sel: SelectionIndex): String? {
        val path = character.listPath.getOrNull(sel.bodyPartIndex)
            ?.listPath?.getOrNull(sel.colorIndex)
            ?.listPath?.getOrNull(sel.pathIndex) ?: return null
        return if (path == "none" || path == "dice") null else path
    }

    fun resolveAllPaths(character: CustomModel, selections: List<SelectionIndex>): List<Pair<Int, String>> {
        return selections.mapIndexedNotNull { i, sel ->
            val bp   = character.listPath.getOrNull(sel.bodyPartIndex) ?: return@mapIndexedNotNull null
            val path = bp.listPath.getOrNull(sel.colorIndex)?.listPath?.getOrNull(sel.pathIndex)
                ?: return@mapIndexedNotNull null
            if (path == "none" || path == "dice") null else i to path
        }
    }

    // ── ASSETS ────────────────────────────────────────────────────────────────

    private suspend fun loadBackgrounds() {
        runCatching {
            val files = context.assets.list("bg") ?: emptyArray()
            _backgrounds.value = listOf("") + files
                .sortedBy { it.substringBeforeLast(".").toIntOrNull() ?: 0 }
                .map { "$ASSET_PREFIX/bg/$it" }
        }.onFailure { Log.e(TAG, "❌ loadBackgrounds", it) }
    }

    private suspend fun loadBackgroundTexts() {
        runCatching {
            _backgroundTexts.value = (context.assets.list("BG_Text") ?: emptyArray())
                .map { "$ASSET_PREFIX/BG_Text/$it" }
        }.onFailure { Log.e(TAG, "❌ loadBGTexts", it) }
    }

    private suspend fun loadStickers() {
        runCatching {
            _stickers.value = (context.assets.list("sticker") ?: emptyArray())
                .map { "$ASSET_PREFIX/sticker/$it" }
        }.onFailure { Log.e(TAG, "❌ loadStickers", it) }
    }

    private suspend fun loadSpeechs() {
        runCatching {
            _speechs.value = (context.assets.list("speech") ?: emptyArray())
                .map { "$ASSET_PREFIX/speech/$it" }
        }.onFailure { Log.e(TAG, "❌ loadSpeechs", it) }
    }

    // ── MY DESIGNS ────────────────────────────────────────────────────────────

    private suspend fun loadMyDesigns() = withContext(Dispatchers.IO) {
        runCatching {
            val json = mmkv.decodeString(KEY_MY_DESIGNS)
            if (json.isNullOrEmpty()) { _myDesignPaths.value = emptyList(); return@withContext }
            val type = object : TypeToken<List<String>>() {}.type
            _myDesignPaths.value = gson.fromJson<List<String>>(json, type) ?: emptyList()
        }.onFailure { Log.e(TAG, "❌ loadMyDesigns", it) }
    }

    suspend fun saveMyDesignToJson(paths: List<String>) = withContext(Dispatchers.IO) {
        runCatching { mmkv.encode(KEY_MY_DESIGNS, gson.toJson(paths)) }
            .onFailure { Log.e(TAG, "❌ saveMyDesigns", it) }
    }
    suspend fun addMyDesignPath(imagePath: String) {
        val list = _myDesignPaths.value.toMutableList()
        if (!list.contains(imagePath)) {
            list.add(0, imagePath)
            _myDesignPaths.value = list
            saveMyDesignToJson(list)
        }
    }

    suspend fun removeMyDesignPath(imagePath: String) {
        val list = _myDesignPaths.value.toMutableList()
        if (list.remove(imagePath)) {
            _myDesignPaths.value = list
            saveMyDesignToJson(list)
        }
    }

    suspend fun loadMyDesignData() = loadMyDesigns()

    // ── QUICK RANDOM ──────────────────────────────────────────────────────────



    // ── REFRESH & CLEAR ───────────────────────────────────────────────────────

    suspend fun refreshFromApi() {
        _isLoading.value = true
        withContext(Dispatchers.IO) {
            try {
                loadTemplatesFromAssets()
                combineCharacterLists()
            } catch (e: Exception) {
                _error.value = "Cannot refresh: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun forceReloadAll() { isDataLoaded = false; loadInitialData() }

    fun clearData() {
        _templates.value            = emptyList()
        _customizedCharacters.value = emptyList()
        _characters.value           = emptyList()
        _backgrounds.value          = emptyList()
        _backgroundTexts.value      = emptyList()
        _stickers.value             = emptyList()
        _speechs.value              = emptyList()
        _myDesignPaths.value        = emptyList()
        isDataLoaded                = false
        isDataQuickLoaded           = false
    }
}