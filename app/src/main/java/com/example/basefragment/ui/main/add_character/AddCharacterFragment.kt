package com.example.basefragment.ui.main.add_character

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.basefragment.R
import com.example.basefragment.core.base.BackPressHandler
import com.example.basefragment.core.base.BaseFragment
import com.example.basefragment.core.custom.Draw
import com.example.basefragment.core.custom.DrawableDraw
import com.example.basefragment.core.custom.listener.listenerdraw.OnDrawListener
import com.example.basefragment.core.dialog.ChooseColorDialog
import com.example.basefragment.core.dialog.DialogSpeech
import com.example.basefragment.core.extention.checkPermissions
import com.example.basefragment.core.extention.dpToPx
import com.example.basefragment.core.extention.drawToBitmap
import com.example.basefragment.core.extention.goToSettings
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.hideNavigation
import com.example.basefragment.core.extention.hideSoftKeyboard
import com.example.basefragment.core.extention.loadImage
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.setFont
import com.example.basefragment.core.extention.setImageActionBar
import com.example.basefragment.core.extention.visible
import com.example.basefragment.core.helper.BitmapHelper
import com.example.basefragment.databinding.FragmentAddCharacterBinding
import com.example.basefragment.ui.main.add_character.adapter.BackgroundColorAdapter
import com.example.basefragment.ui.main.add_character.adapter.BackgroundImageAdapter
import com.example.basefragment.ui.main.add_character.adapter.StickerAdapter
import com.example.basefragment.ui.main.add_character.adapter.TextColorAdapter
import com.example.basefragment.ui.main.add_character.adapter.TextFontAdapter
import com.example.basefragment.ui.onboarding.permission.PermissionViewModel
import com.example.basefragment.utils.DataLocal
import com.example.basefragment.utils.key.ValueKey
import com.example.basefragment.data.datalocal.manager.CharacterImageManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AddCharacterFragment : BaseFragment<FragmentAddCharacterBinding, AddCharacterViewModel>(
    FragmentAddCharacterBinding::inflate,
    AddCharacterViewModel::class.java
) , BackPressHandler{
    @Inject
    lateinit var imageManager: CharacterImageManager
    private val permissionViewModel: PermissionViewModel by viewModels()

    private val backgroundImageAdapter by lazy { BackgroundImageAdapter() }
    private val backgroundColorAdapter by lazy { BackgroundColorAdapter() }
    private val stickerAdapter by lazy { StickerAdapter() }
    private val speechAdapter by lazy { StickerAdapter() }
    private val textFontAdapter by lazy { TextFontAdapter(requireContext()) }
    private val textColorAdapter by lazy { TextColorAdapter() }
    private val imagepath: String by lazy {
        arguments?.getString("imagePath") ?: ""
    }

    // XÓA 2 lazy list này
// private val buttonNavigationList by lazy { ... }
// private val layoutNavigationList by lazy { ... }

    // THAY bằng 2 function này
    private fun buttonNavigationList() = arrayListOf(
        binding.btnBackground,
        binding.btnSticker,
        binding.btnSpeech,
        binding.btnText,
    )

    private fun layoutNavigationList() = arrayListOf(
        binding.lnlBackground.root,
        binding.lnlSticker,
        binding.lnlSpeech,
        binding.lnlText.scvText,
    )

    // Image picker launcher
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult

                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                handleSetBackgroundImage(uri.toString(), 0)
            }
        }


    // Permission launcher
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                permissionViewModel.updateStorageGranted(sharedPreferences, true)
                launchImagePicker() // ✅ ĐÚNG
            } else {
                permissionViewModel.updateStorageGranted(sharedPreferences, false)
            }
        }

    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
        imagePickerLauncher.launch(intent)
    }


    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentAddCharacterBinding = FragmentAddCharacterBinding.inflate(inflater, container, false)



    override fun observeData() {
        binding.apply {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        viewModel.typeNavigation.collect { type ->
                            if (type != -1) {
                                setupTypeNavigation(type)
                            }
                            requireActivity().hideNavigation(true)

                        }

                    }
                    // Trong observeData()
                    launch {
                        viewModel.backgroundImagePath.collect { path ->
                            path?.let {
                                loadImage(requireContext(), it, binding.imvBackground)  // ✅ Dùng extension
                            }
                        }
                    }
                    launch {
                        // Type Background
                        viewModel.typeBackground.collect { type ->
                            if (type != -1) {
                                setupTypeBackground(type)
                            }
                            requireActivity().hideNavigation(true)

                        }
                    }
                    launch {
                        viewModel.isFocusEditText.collect { status ->
                            val params = binding.flFunction.layoutParams
                                    as ViewGroup.MarginLayoutParams

                            if (status) {
                                // Lên ngay — không delay
                                params.topMargin = dpToPx(requireContext(), -170)
                                binding.flFunction.layoutParams = params
                            } else {
                                // ✅ Dùng animate thay vì delay thô
                                // delay thô → coroutine bị cancel giữa chừng → state sai
                                binding.flFunction.animate()
                                    .translationY(0f)
                                    .setDuration(200)
                                    .withEndAction {
                                        params.topMargin = viewModel.originalMarginBottom
                                        binding.flFunction.layoutParams = params
                                        binding.flFunction.translationY = 0f
                                    }
                                    .start()
                            }
                            requireActivity().hideNavigation(true)
                        }
                    }

                    // isSpeechKeyboardVisible — KHÔNG đụng layout, chỉ dùng để guard
                    launch {
                        viewModel.isSpeechKeyboardVisible.collect { visible ->
                            // Không làm gì với layout ở đây
                            // Chỉ log hoặc xử lý nếu cần
                            requireActivity().hideNavigation(true)
                        }
                    }
                }
            }
        }
    }


    override fun bindViewModel() {
        // Can be used for additional bindings with viewModelActivity if needed
    }

    override fun viewListener() {
        binding.apply {
            actionBar.apply {
                btnActionBarLeft.onClick { confirmExit() }
                btnActionBarCenter1.onClick { confirmReset() }
                btnActionBarRight.onClick { handleSave() }
//                setImageActionBar(btnActionBarCenter2, R.drawable.ic_show_all_custom)

            }

            lnlBackground.btnBackgroundImage.onClick { viewModel.setTypeBackground(ValueKey.IMAGE_BACKGROUND) }
            lnlBackground.btnBackgroundColor.onClick { viewModel.setTypeBackground(ValueKey.COLOR_BACKGROUND) }
            btnBackground.onClick { viewModel.setTypeNavigation(ValueKey.BACKGROUND_NAVIGATION) }
            btnSticker.onClick { viewModel.setTypeNavigation(ValueKey.STICKER_NAVIGATION) }
            btnSpeech.onClick { viewModel.setTypeNavigation(ValueKey.SPEECH_NAVIGATION) }
            btnText.onClick { viewModel.setTypeNavigation(ValueKey.TEXT_NAVIGATION) }

            lnlText.edtText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    tvGetText.text = p0.toString()
                }

                override fun afterTextChanged(p0: Editable?) {}
            })

            lnlText.edtText.setOnEditorActionListener { _, i, _ ->
                if (i == EditorInfo.IME_ACTION_DONE) {
                    viewModel.setIsFocusEditText(false)
                      hideSoftKeyboard()
                    true
                } else {
                    false
                }
            }

            lnlText.edtText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                // Guard: chỉ set khi speech không mở VÀ value thực sự thay đổi
                if (!viewModel.isSpeechKeyboardVisible.value) {
                    viewModel.setIsFocusEditText(hasFocus)  // setter đã có guard bên trong
                }
            }
            lnlText.btnDoneText.onClick { handleDoneText() }

            main.onClick {
                viewModel.setIsFocusEditText(false)
                clearFocus()
                hideSoftKeyboard()
            }

            backgroundImageAdapter.apply {
                onAddImageClick = { checkStoragePermission() }
                onBackgroundImageClick = { path, position ->
                    handleSetBackgroundImage(path, position)
                }
            }

            backgroundColorAdapter.apply {
                onChooseColorClick = { handleChooseColor() }
                onBackgroundColorClick = { color, position ->
                    handleSetBackgroundColor(color, position)
                }
            }

            stickerAdapter.onItemClick = { path -> addDrawable(path) }
            speechAdapter.onItemClick = { path -> handleSpeech(path) }
            textFontAdapter.onTextFontClick = { font, position ->
                handleFontClick(font, position)
            }

            textColorAdapter.apply {
                onChooseColorClick = { handleChooseColor(true) }
                onTextColorClick = { color, position ->
                    handleTextColorClick(color, position)
                }
            }
        }

        initActionBar()
        requireActivity().hideNavigation(true)

    }

    private fun initActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.back_app)
            setImageActionBar(btnActionBarCenter1, R.drawable.ic_reset_all_custom)
            setImageActionBar(btnActionBarRight, R.drawable.next_app)
        }
    }

    private fun initRcv() {
        binding.apply {
            lnlBackground.rcvBackgroundImage.apply {
                adapter = backgroundImageAdapter
                itemAnimator = null
                setHasFixedSize(true)
                setItemViewCacheSize(10)
            }
            lnlBackground.rcvBackgroundColor.apply {
                adapter = backgroundColorAdapter
                itemAnimator = null
            }
            rcvSticker.apply {
                adapter = stickerAdapter
                itemAnimator = null
                setHasFixedSize(true)    // ← thêm dòng này
                setItemViewCacheSize(10)
            }
            rcvSpeech.apply {
                adapter = speechAdapter
                itemAnimator = null
                setHasFixedSize(true)    // ← thêm dòng này
                setItemViewCacheSize(10)
            }
            lnlText.rcvFont.apply {
                adapter = textFontAdapter
                itemAnimator = null
            }
            lnlText.rcvTextColor.apply {
                adapter = textColorAdapter
                itemAnimator = null
            }
        }
        requireActivity().hideNavigation(true)
    }

    override fun initView() {
        binding.lnlBackground.btnBackgroundColorTv.isSelected = true
        binding.lnlBackground.btnBackgroundImageTv.isSelected = true
        requireActivity().hideNavigation(true)
        setupKeyboardListener()
        binding.tvGetText.setTextColor(requireContext().getColor(R.color.black))
        viewModel.layoutParams = binding.flFunction.layoutParams as ViewGroup.MarginLayoutParams
        if (!viewModel.isInitialized) {
            viewModel.originalMarginBottom = viewModel.layoutParams.topMargin
            android.util.Log.d("FOCUS_DEBUG", "originalMarginBottom saved: ${viewModel.originalMarginBottom}")
        }
        initRcv()
        initDrawView()
//        setupBackPress()

        if (!viewModel.isInitialized) {
            showLoadingSafe()
            initData()
            viewModel.isInitialized = true
        } else {
            restoreUIState()
        }
    }
    private fun setupKeyboardListener() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeVisible = insets.isVisible(
                androidx.core.view.WindowInsetsCompat.Type.ime()
            )

            // Speech đang mở → bỏ qua hoàn toàn
            if (viewModel.isSpeechKeyboardVisible.value) return@setOnApplyWindowInsetsListener insets

            // Keyboard ẩn đi mà focus vẫn true → reset
            if (!imeVisible && viewModel.isFocusEditText.value) {
                viewModel.setIsFocusEditText(false)
            }

            insets
        }
    }
    private fun restoreUIState() {
        backgroundImageAdapter.submitList(viewModel.backgroundImageList)
        backgroundColorAdapter.submitList(viewModel.backgroundColorList, true)
        stickerAdapter.submitList(viewModel.stickerList, true)
        speechAdapter.submitList(viewModel.speechList)
        textFontAdapter.submitListReset(viewModel.textFontList)
        textColorAdapter.submitListReset(viewModel.textColorList)

        val currentNav = viewModel.typeNavigation.value
        if (currentNav != -1) setupTypeNavigation(currentNav)

        val currentBg = viewModel.typeBackground.value
        if (currentBg != -1) setupTypeBackground(currentBg)

        // Restore background — ưu tiên image, fallback color
        val imagePath = viewModel.backgroundImagePath.value
        val savedColor = viewModel.savedBackgroundColor

        when {
            imagePath != null -> {
                binding.imvBackground.setBackgroundColor(
                    requireContext().getColor(R.color.transparent)
                )
                loadImage(requireContext(), imagePath, binding.imvBackground)
            }
            savedColor != null -> {
                binding.imvBackground.setImageBitmap(null)
                binding.imvBackground.setBackgroundColor(savedColor)
            }
        }

        if (viewModel.drawViewList.isNotEmpty()) {
            viewModel.isRestoringDraws = true
            binding.drawView.fillData(viewModel.drawViewList)
            viewModel.isRestoringDraws = false
        }
    }

    private fun initData() {
        viewModel.loadDataFromMainViewModel(
            viewModelActivity.backgrounds.value,
            viewModelActivity.stickers.value,
            viewModelActivity.speechs.value
        )

        backgroundImageAdapter.submitList(viewModel.backgroundImageList)
        backgroundColorAdapter.submitList(viewModel.backgroundColorList, true)
        stickerAdapter.submitList(viewModel.stickerList, true)
        speechAdapter.submitList(viewModel.speechList)
        textFontAdapter.submitListReset(viewModel.textFontList)
        textColorAdapter.submitListReset(viewModel.textColorList)
        viewModel.setTypeNavigation(ValueKey.BACKGROUND_NAVIGATION)
        viewModel.setTypeBackground(ValueKey.IMAGE_BACKGROUND)
        hideLoadingSafe() // ← hide sau khi submit xong

        viewLifecycleOwner.lifecycleScope.launch {
            addDrawable(imagepath, true)
            if (viewModel.pathDefault.isNotEmpty()) {
                addDrawable(viewModel.pathDefault, true)
            }
        }
    }
    private fun addDrawable(
        path: String,
        isCharacter: Boolean = false,
        bitmapText: Bitmap? = null
    ) {
        if (bitmapText != null) {
            val drawable = viewModel.loadDrawableEmoji(bitmapText, isCharacter)
            binding.drawView.addDraw(drawable)
            return
        }

        Glide.with(this)
            .asBitmap()
            .load(path)
            .override(512, 512)
            .diskCacheStrategy(DiskCacheStrategy.ALL)  // ← cache bitmap, lần sau load ngay
            .format(com.bumptech.glide.load.DecodeFormat.PREFER_ARGB_8888)
            .disallowHardwareConfig()
            .into(object : com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                ) {
                    val drawable = viewModel.loadDrawableEmoji(resource, isCharacter)
                    binding.drawView.addDraw(drawable)
                    requireActivity().hideNavigation(true)
                }
                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                    showToast("Không thể tải sticker")
                }
            })
    }

    private fun initDrawView() {
        requireActivity().hideNavigation(true)
        binding.drawView.apply {
            setConstrained(true)
            setLocked(false)
            setOnDrawListener(object : OnDrawListener {
                override fun onAddedDraw(draw: Draw) {
                    if (!viewModel.isRestoringDraws) {
                        viewModel.updateCurrentCurrentDraw(draw)
                        viewModel.addDrawView(draw)
                    }
                    viewModel.setIsFocusEditText(false)
                }

                override fun onClickedDraw(draw: Draw) {
                    viewModel.setIsFocusEditText(false)
                }

                override fun onDeletedDraw(draw: Draw) {
                    viewModel.deleteDrawView(draw)
                    viewModel.setIsFocusEditText(false)
                }

                override fun onDragFinishedDraw(draw: Draw) {
                    viewModel.setIsFocusEditText(false)
                }

                override fun onTouchedDownDraw(draw: Draw) {
                    viewModel.updateCurrentCurrentDraw(draw)
                    viewModel.setIsFocusEditText(false)
                }

                override fun onZoomFinishedDraw(draw: Draw) {}
                override fun onFlippedDraw(draw: Draw) {
                    viewModel.setIsFocusEditText(false)
                }

                override fun onDoubleTappedDraw(draw: Draw) {}
                override fun onHideOptionIconDraw() {}
                override fun onUndoDeleteDraw(draw: List<Draw?>) {}
                override fun onUndoUpdateDraw(draw: List<Draw?>) {}
                override fun onUndoDeleteAll() {}
                override fun onRedoAll() {}
                override fun onReplaceDraw(draw: Draw) {}
                override fun onEditText(draw: DrawableDraw) {}
                override fun onReplace(draw: Draw) {}
            })
        }
    }

    private fun setupTypeBackground(type: Int) {
        binding.apply {
            when (type) {
                ValueKey.IMAGE_BACKGROUND -> {
                    requireActivity().hideNavigation(true)
                    lnlBackground.rcvBackgroundImage.visible()
                    lnlBackground.rcvBackgroundColor.gone()
                    lnlBackground.btnBackgroundImage.setBackgroundResource(R.drawable.img_bg_image_addcharacter)
                    lnlBackground.btnBackgroundColor.setBackgroundResource(R.drawable.img_bg_color_addcharacter)
                    backgroundImageAdapter.submitList(viewModel.backgroundImageList)
                }
                ValueKey.COLOR_BACKGROUND -> {
                    requireActivity().hideNavigation(true)
                    lnlBackground.rcvBackgroundImage.gone()
                    lnlBackground.rcvBackgroundColor.visible()
                    lnlBackground.btnBackgroundColor.setBackgroundResource(R.drawable.img_bg_image_addcharacter)
                    lnlBackground.btnBackgroundImage.setBackgroundResource(R.drawable.img_bg_color_addcharacter)
                    backgroundColorAdapter.submitList(viewModel.backgroundColorList)
                }
            }
        }
    }

    private fun setupTypeNavigation(type: Int) {
        buttonNavigationList().forEachIndexed { index, button ->
            val (res, status) = if (index == type) {
                DataLocal.bottomNavigationSelected[index] to true
            } else {
                DataLocal.bottomNavigationNotSelect[index] to false
            }
            button.setImageResource(res)
            layoutNavigationList()[index].isVisible = status
        }
    }

    private fun confirmExit() {
        android.util.Log.d("FOCUS_DEBUG", "confirmExit called, isAdded=$isAdded, activity=$activity")
        viewModel.setIsFocusEditText(false)
        showConfirmDialog(
            message = getString(R.string.haven_t_saved_it_yet_do_you_want_to_exit),
            title = getString(R.string.exit),
            onYes = {
                hideLoadingSafe()
                findNavController().navigateUp()
            },
            onNo = { hideLoadingSafe() }
        )
    }

    private fun confirmReset() {
        viewModel.setIsFocusEditText(false)
        showConfirmDialog(
            message = getString(R.string.do_you_want_to_reset_all),
            title = getString(R.string.reset),
            onYes = {
                showLoadingSafe()

                viewModel.loadDataFromMainViewModel(
                    viewModelActivity.backgrounds.value,
                    viewModelActivity.stickers.value,
                    viewModelActivity.speechs.value
                )
                viewModel.resetDraw()

                binding.drawView.removeAllDraw()
                binding.imvBackground.setImageBitmap(null)
                binding.imvBackground.setBackgroundColor(
                    requireContext().getColor(R.color.transparent)
                )

                // ← Reset đúng cách — clear isSelected trong list rồi mới notify
                backgroundImageAdapter.clearSelection()
                backgroundColorAdapter.clearSelection()

                hideLoadingSafe()
                addDrawable(imagepath, true)
            },
            onNo = { hideLoadingSafe() }
        )
    }

    // Trong handleSetBackgroundImage()
    private fun handleSetBackgroundImage(path: String, position: Int) {
        viewModel.setBackgroundImage(path)
        viewModel.savedBackgroundColor = null           // ← clear color khi chọn image
        binding.imvBackground.setBackgroundColor(
            requireContext().getColor(R.color.transparent)
        )
        loadImage(requireContext(), path, binding.imvBackground)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            viewModel.updateBackgroundImageSelected(position)
            withContext(Dispatchers.Main) {
                backgroundColorAdapter.clearSelection()
                backgroundImageAdapter.selectItem(position)
            }
        }
    }

    private fun checkStoragePermission() {
        val perms = permissionViewModel.getStoragePermissions()
        if (requireContext().checkPermissions(perms)) {
            launchImagePicker()
        } else if (permissionViewModel.needGoToSettings(sharedPreferences, true)) {
            requireActivity().goToSettings()
        } else {
            permissionLauncher.launch(perms)
        }
    }



    private fun handleChooseColor(isTextColor: Boolean = false) {
        val dialog = ChooseColorDialog(requireContext())
        dialog.show()

        fun dismissDialog() {
            dialog.dismiss()
        }

        dialog.onCloseEvent = { dismissDialog() }
        dialog.onDoneEvent = { color ->
            dismissDialog()
            if (!isTextColor) {
                handleSetBackgroundColor(color, 0)
            } else {
                handleTextColorClick(color, 0)
            }
        }
    }

    private fun handleSpeech(path: String) {
        // Báo hiệu speech keyboard sắp xuất hiện
        viewModel.setSpeechKeyboardVisible(true)
        viewModel.setIsFocusEditText(false)
        hideSoftKeyboard()

        val dialog = DialogSpeech(requireContext(), path)
        dialog.show()

        dialog.onDoneClick = { bitmap ->
            dialog.dismiss()
            if (bitmap != null) addDrawable("", false, bitmap)
        }

        dialog.setOnDismissListener {
            binding.root.postDelayed({
                // Reset cả 2 về false khi dialog đóng
                viewModel.setSpeechKeyboardVisible(false)
                viewModel.setIsFocusEditText(false)
                binding.main.requestFocus()
                hideSoftKeyboard()
            }, 300)
        }
    }

    private fun handleSetBackgroundColor(color: Int, position: Int) {
        binding.apply {
            imvBackground.setImageBitmap(null)
            imvBackground.setBackgroundColor(color)
            viewModel.savedBackgroundColor = color      // ← lưu lại
            viewModel.setBackgroundImage(null)          // ← clear image path khi chọn color
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                viewModel.updateBackgroundColorSelected(position)
                withContext(Dispatchers.Main) {
                    backgroundImageAdapter.clearSelection()
                    backgroundColorAdapter.selectItem(position)
                }
            }
        }
    }

    private fun handleFontClick(font: Int, position: Int) {
        binding.apply {
            lnlText.edtText.setFont(font)
            tvGetText.setFont(font)
            viewModel.updateTextFontSelected(position)
            textFontAdapter.submitItem(position, viewModel.textFontList)
        }
    }

    private fun handleTextColorClick(color: Int, position: Int) {
        binding.apply {
            lnlText.edtText.setTextColor(color)
            tvGetText.setTextColor(color)
            viewModel.updateTextColorSelected(position)
            textColorAdapter.submitItem(position, viewModel.textColorList)
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun handleDoneText() {
        hideSoftKeyboard()
        binding.apply {
            if (lnlText.edtText.text.toString().trim() == "") {
                showToast(getString(R.string.null_edt))
            } else {
                tvGetText.text = lnlText.edtText.text.toString().trim()
                val bitmap = BitmapHelper.getBitmapFromEditText(tvGetText)
                val drawableEmoji = viewModel.loadDrawableEmoji(bitmap, isText = true)
                drawView.addDraw(drawableEmoji)

                // Reset
                val font = viewModel.textFontList.first().color
                val color = viewModel.textColorList[1].color

                lnlText.edtText.text = null
                lnlText.edtText.setFont(font)
                lnlText.edtText.setTextColor(color)

                viewModel.updateTextFontSelected(0)
                viewModel.updateTextColorSelected(1)

                textFontAdapter.submitListReset(viewModel.textFontList)
                textColorAdapter.submitListReset(viewModel.textColorList)

                tvGetText.text = ""
                tvGetText.setFont(font)
                tvGetText.setTextColor(color)
            }
        }
        viewModel.setIsFocusEditText(false)
    }

    private fun clearFocus() {
        binding.drawView.hideSelect()
    }

    private fun handleSave() {
        viewModel.setIsFocusEditText(false)
        clearFocus()
        hideSoftKeyboard()

        viewLifecycleOwner.lifecycleScope.launch {
            showLoadingSafe()
            try {
                // ✅ Giờ drawToBitmap luôn đúng vì Glide không dùng hardware bitmap nữa
                val bitmap = binding.flSave.drawToBitmap()

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(java.util.Date())
                val designId = "design_$timestamp"

                val savedImagePath = withContext(Dispatchers.IO) {
                    imageManager.deleteOldImage(designId)
                    val path = imageManager.saveBitmap(bitmap, designId)
                    if (path != null) {
                        viewModelActivity.appDataManager.addMyDesignPath(path)
                    }
                    path
                }

                hideLoadingSafe()

                if (savedImagePath != null) {
                    val bundle = Bundle().apply {
                        putString("imagePath", savedImagePath)
                        putString("idEdit", "")
                        putInt("imageType", 0)
                    }
                    findNavController().navigate(
                        R.id.action_addCharacterFragment_to_viewImageFragment,
                        bundle
                    )
                } else {
                    Toast.makeText(requireContext(), "Lưu thất bại!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                hideLoadingSafe()
                Toast.makeText(requireContext(), "Có lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onBackPressed(): Boolean {
        android.util.Log.d("FOCUS_DEBUG", "onBackPressed called: isFocusEditText=${viewModel.isFocusEditText.value}")

        return if (viewModel.isFocusEditText.value) {
            binding.lnlText.edtText.clearFocus()
            hideSoftKeyboard()
            viewModel.setIsFocusEditText(false)
            true // consumed, không back
        } else {
            confirmExit()
            true // consumed, hiện dialog
        }
    }

}