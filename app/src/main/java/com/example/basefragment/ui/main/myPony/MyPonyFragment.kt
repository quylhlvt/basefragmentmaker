package com.example.basefragment.ui.main.myPony

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.basefragment.R
import com.example.basefragment.core.base.BaseFragment
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.invisible
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.setImageActionBar
import com.example.basefragment.core.extention.setTextActionBar
import com.example.basefragment.core.extention.visible
import com.example.basefragment.data.model.mypony.MyAlbumModel
import com.example.basefragment.databinding.FragmentMyPonyBinding
import com.example.basefragment.ui.main.customize.CustomizeFragment
import com.example.basefragment.ui.main.myPony.adapter.MyAvatarAdapter
import com.example.basefragment.ui.main.myPony.adapter.MyDesignAdapter
import com.example.basefragment.utils.share.whatsapp.StickerPack
import com.example.basefragment.utils.share.whatsapp.WhatsappSharingFragment
import com.example.basefragment.utils.share.whatsapp.WhitelistCheck
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class MyPonyFragment : WhatsappSharingFragment<FragmentMyPonyBinding, MyPonyViewModel>(
    FragmentMyPonyBinding::inflate,
    MyPonyViewModel::class.java
) {

    private lateinit var myAvatarAdapter: MyAvatarAdapter
    private lateinit var myDesignAdapter: MyDesignAdapter

    private val isAvatarTab = MutableStateFlow(true)

    companion object {
        private const val ADD_PACK_REQUEST              = 200
        private const val MIN_STICKERS_WHATSAPP         = 3
        private const val MAX_STICKERS_WHATSAPP         = 30
    }

    // ── INIT ──────────────────────────────────────────────────────────────────

    override fun initView() {
        setupActionBar()
        setupTabs()
        setupRecyclerViews()
        setupBottomButtons()
        setupTouchListenerForResetSelection()
        loadAvatarData()
    }

    private fun setupActionBar() {
        binding.actionBar.apply {
            setImageActionBar(btnActionBarLeft, R.drawable.back_app)
            setTextActionBar(tvCenter, getString(R.string.my_work))
            setImageActionBar(btnActionBarNextToRight, R.drawable.ic_delete_all)
            setImageActionBar(btnActionBarRight, R.drawable.ic_select_all)
            btnActionBarNextToRight.invisible()
            btnActionBarRight.invisible()
        }
    }

    private fun setupTabs() {
        binding.btnMyAvatar.onClick { switchTab(true) }
        binding.btnMyDesign.onClick { switchTab(false) }
    }

    private fun switchTab(isAvatar: Boolean) {
        isAvatarTab.value = isAvatar
        binding.apply {
            if (isAvatar) {
                imvFocusMyDesign.setImageResource(R.drawable.bg_btn_type_unselected)
                imvFocusMyAvatar.setImageResource(R.drawable.bg_btn_type_selected)
                recycleAvatar.visible()
                recycleDesign.gone()
//                tvMyAvatar.setTextColor(requireContext().getColor(R.color.white))
//                tvMyDesign.setTextColor(requireContext().getColor(R.color.app_color3))
                loadAvatarData()
            } else {
                imvFocusMyDesign.setImageResource(R.drawable.bg_btn_type_selected)
                imvFocusMyAvatar.setImageResource(R.drawable.bg_btn_type_unselected)
                recycleAvatar.gone()
                recycleDesign.visible()
//                tvMyAvatar.setTextColor(requireContext().getColor(R.color.app_color3))
//                tvMyDesign.setTextColor(requireContext().getColor(R.color.white))
                loadDesignData()
            }
        }
        resetSelection()
    }

    private fun setupRecyclerViews() {
        myAvatarAdapter = MyAvatarAdapter(requireContext()).apply {
            onItemClick   = { item -> handleItemClick(item.path, true, 1, item.idEdit) }
            onLongClick   = { position -> handleLongClick(position, true) }
            onItemTick    = { position -> toggleSelection(position, true) }
            onEditClick   = { idEdit -> navigateToEdit(idEdit) }
            onDeleteClick = { path -> confirmDelete(arrayListOf(path), true) }
        }
        binding.recycleAvatar.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter       = myAvatarAdapter
            itemAnimator  = null
        }

        myDesignAdapter = MyDesignAdapter().apply {
            onItemClick   = { path -> handleItemClick(path, false, 2, "0") }
            onLongClick   = { position -> handleLongClick(position, false) }
            onItemTick    = { position -> toggleSelection(position, false) }
            onDeleteClick = { path -> confirmDelete(arrayListOf(path), false) }
        }
        binding.recycleDesign.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter       = myDesignAdapter
            itemAnimator  = null
        }
    }

    private fun setupBottomButtons() {
        binding.apply {
            btnWhatsapp.onClick { handleWhatsAppShare() }
            btnTelegram.onClick { handleTelegramShare() }
            btnDownload.onClick { handleDownload() }
            btnShare.onClick   { handleShare() }
            actionBar.btnActionBarRight.onClick { handleSelectAll() }
            actionBar.btnActionBarNextToRight.onClick { handleDeleteSelected() }  // ✅ thêm

        }
    }
    private fun handleShare() {
        val selected = getSelectedItems()
        if (selected.isEmpty()) { showToast(R.string.please_select_an_image); return }
        val paths = selected.map { it.path }.filter { it.isNotEmpty() }
        if (paths.isEmpty()) { showToast(R.string.please_select_an_image); return }

        val uris = ArrayList(paths.map { path ->
            androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                java.io.File(path)
            )
        })

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type  = "image/*"
                putExtra(Intent.EXTRA_STREAM, uris[0])
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type  = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        startActivity(Intent.createChooser(intent, getString(R.string.share)))
        resetSelection()
    }
    private fun setupTouchListenerForResetSelection() {
        val touchListener = object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_UP && rv.findChildViewUnder(e.x, e.y) == null) {
                    resetSelection()
                    return true
                }
                return false
            }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallow: Boolean) {}
        }
        binding.recycleAvatar.addOnItemTouchListener(touchListener)
        binding.recycleDesign.addOnItemTouchListener(touchListener)
    }
    // ── OBSERVE ───────────────────────────────────────────────────────────────
// MyPonyFragment.kt — observeData()
    override fun observeData() {
        // ✅ Avatar: collect từ viewModelActivity (source of truth)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModelActivity.customizedCharacters.collect { customized ->
                val list = customized
                    .filter { it.imageSave.isNotEmpty() && File(it.imageSave).exists() }
//                    .sortedByDescending { it.updatedAt }
                    .map { MyAlbumModel(path = it.imageSave, idEdit = it.id, type = 1) }

                myAvatarAdapter.submitList(list)
                if (isAvatarTab.value) updateEmptyState(list.isEmpty())
                updateSelectionUI()
            }
        }

        // ✅ Design: giữ nguyên collect từ viewModel.myDesignList
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.myDesignList.collect { list ->
                myDesignAdapter.submitList(list)
                if (!isAvatarTab.value) updateEmptyState(list.isEmpty())
                updateSelectionUI()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.downloadState.collect { state ->
                when (state) {
                    MyPonyViewModel.DownloadState.SUCCESS -> showToast(getString(R.string.download_success, getString(R.string.app_name)))
                    MyPonyViewModel.DownloadState.ERROR   -> showToast(R.string.download_failed_please_try_again_later)
                    else -> {}
                }
            }
        }
    }

    // ── UI HELPERS ────────────────────────────────────────────────────────────

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.noItem.isVisible = isEmpty
    }

    private fun updateSelectionUI() {
        val currentList   = if (isAvatarTab.value) myAvatarAdapter.items else myDesignAdapter.items
        val hasSelection  = currentList.any { it.isShowSelection }
        val allSelected   = currentList.isNotEmpty() && currentList.all { it.isSelected }
        val selectedCount = currentList.count { it.isSelected }

        binding.actionBar.apply {
            if (hasSelection) {
              btnActionBarNextToRight.visible()
                btnActionBarRight.visible()
                tvCenter.text = "Selected $selectedCount/${currentList.size}"
                btnActionBarRight.setImageResource(
                    if (allSelected) R.drawable.ic_select_all else R.drawable.ic_not_select_all
                )
            } else {
                btnActionBarNextToRight.invisible()
                btnActionBarRight.invisible()
                tvCenter.text = getString(R.string.my_work)
            }
        }

        if (hasSelection) {
            binding.lnlBottom.visible()
            if (isAvatarTab.value) {
                binding.lnlBottomTop.visible()   // WhatsApp + Telegram
                binding.btnDownload.gone()
                binding.btnShare.visible()        // ← THÊM
            } else {
                binding.lnlBottomTop.gone()       // Ẩn WhatsApp + Telegram cho Design tab
                binding.btnDownload.visible()
                binding.btnShare.visible()        // ← THÊM
            }
        } else {
            binding.lnlBottom.gone()
        }
    }

    // ── DATA LOADING ──────────────────────────────────────────────────────────

    private fun loadAvatarData() = viewModel.loadMyAvatar(requireContext(), true)
    private fun loadDesignData() = viewModel.loadMyDesign(requireContext())

    // ── SELECTION ─────────────────────────────────────────────────────────────

    private fun handleItemClick(path: String, isAvatar: Boolean, type: Int, idEdit: String) {
        val currentList = if (isAvatar) myAvatarAdapter.items else myDesignAdapter.items
        if (currentList.any { it.isShowSelection }) {
            val position = currentList.indexOfFirst { it.path == path }
            if (position >= 0) toggleSelection(position, isAvatar)
        } else {
            navigateToView(path, type, idEdit)
        }
    }
// MyAvatarAdapter — long click gọi về Fragment


    // Fragment nhận và gọi ViewModel

    private fun handleLongClick(position: Int, isAvatar: Boolean) {
        val currentList = if (isAvatar) myAvatarAdapter.items else myDesignAdapter.items
        val updatedList = currentList.mapIndexed { index, item ->
            if (index == position) {
                item.copy(isSelected = true, isShowSelection = true)
            } else {
                item.copy(isShowSelection = true)
            }
        }
        if (isAvatar) {
            myAvatarAdapter.submitList(updatedList)
        } else {
            myDesignAdapter.submitList(updatedList)
        }
        updateSelectionUI()
    }

    private fun toggleSelection(position: Int, isAvatar: Boolean) {
        val currentList = if (isAvatar) myAvatarAdapter.items else myDesignAdapter.items
        val updatedList = currentList.mapIndexed { index, item ->
            if (index == position) item.copy(isSelected = !item.isSelected) else item
        }
        if (isAvatar) myAvatarAdapter.submitList(updatedList)
        else myDesignAdapter.submitList(updatedList)

        updateSelectionUI()
        if (updatedList.none { it.isSelected }) resetSelection()
    }
    private fun handleDeleteSelected() {
        val selected = getSelectedItems()
        if (selected.isEmpty()) { showToast(R.string.please_select_an_image); return }
        val paths = ArrayList(selected.map { it.path })
        confirmDelete(paths, isAvatarTab.value)
    }
    private fun handleSelectAll() {
        val currentList     = if (isAvatarTab.value) myAvatarAdapter.items else myDesignAdapter.items
        val shouldSelectAll = !currentList.all { it.isSelected }
        val updatedList     = currentList.map { it.copy(isSelected = shouldSelectAll, isShowSelection = true) }
        if (isAvatarTab.value) myAvatarAdapter.submitList(updatedList)
        else myDesignAdapter.submitList(updatedList)
        updateSelectionUI()
    }

    private fun resetSelection() {
        val avatarReset = myAvatarAdapter.items.map { it.copy(isSelected = false, isShowSelection = false) }
        val designReset = myDesignAdapter.items.map { it.copy(isSelected = false, isShowSelection = false) }
        myAvatarAdapter.submitList(avatarReset)
        myDesignAdapter.submitList(designReset)
        updateSelectionUI()
    }

    private fun getSelectedItems(): List<MyAlbumModel> =
        if (isAvatarTab.value) myAvatarAdapter.items.filter { it.isSelected }
        else myDesignAdapter.items.filter { it.isSelected }

    // ── NAVIGATION ────────────────────────────────────────────────────────────

    private fun navigateToView(path: String, type: Int, idEdit: String) {
        val action = MyPonyFragmentDirections.actionMyponyToView(path, idEdit, type)
        findNavController().navigate(action)
    }

    /**
     * Navigate sang CustomizeFragment ở chế độ Edit.
     *
     * Vấn đề: khi save từ template, ViewModelActivity.saveCharacterWithSelections() copy
     * character với id = UUID mới. Không có field "templateId" nào được lưu lại.
     *
     * Giải pháp: dùng [CustomModel.avatar] của customized character để tìm template gốc
     * có cùng avatar (template gốc KHÔNG thay đổi avatar, chỉ customized mới có imageSave riêng).
     *
     * Nếu project có field templateId trong CustomModel thì dùng trực tiếp field đó thay thế.
     */
    private fun navigateToEdit(idEdit: String) {
        val customized = viewModelActivity.customizedCharacters.value
            .firstOrNull { it.id == idEdit }
            ?: run { showToast("Character not found"); return }

        val templateIndex = viewModelActivity.getTemplateIndexForCustomized(idEdit)
            .takeIf { it >= 0 }
            ?: run { showToast("Template not found"); return }

        val args = CustomizeFragment.newArgs(
            templateIndex   = templateIndex,
            isEdit          = true,
            customizedId    = idEdit,            // ← THÊM
            savedSelections = customized.selections,
            isFlipped       = customized.isFlipped
        )
        findNavController().navigate(R.id.action_mypony_to_custom, args)
    }

    // ── ACTIONS ───────────────────────────────────────────────────────────────

    private fun confirmDelete(paths: ArrayList<String>, isAvatar: Boolean) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(R.string.are_you_sure_want_to_delete_this_item)
            .setPositiveButton("Delete") { _, _ ->
                if (isAvatar) viewModel.deleteItem(requireContext(), paths)
                else          viewModel.deleteItemDesign(paths, requireContext())
                resetSelection()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleDownload() {
        val selected = getSelectedItems()
        if (selected.isEmpty()) { showToast(R.string.please_select_an_image); return }
        viewModel.downloadFiles(requireContext(), ArrayList(selected.map { it.path }))
        resetSelection()
    }

    // ── SHARE: chỉ dùng imageSave (ảnh render) ───────────────────────────────

    /**
     * Lấy đúng path để share.
     * - Avatar tab: dùng [MyAlbumModel.path] = customized.imageSave (ảnh render đã lưu)
     * - Design tab: dùng path trực tiếp
     * KHÔNG dùng customized.avatar (đó là thumbnail template gốc từ assets)
     */
    private fun getSharePaths(): List<String> =
        getSelectedItems().map { it.path }.filter { it.isNotEmpty() }

    // ── WHATSAPP ──────────────────────────────────────────────────────────────

    private fun handleWhatsAppShare() {
        val paths = getSharePaths()
        when {
            paths.isEmpty()                    -> { showToast(R.string.please_select_an_image); return }
            paths.size < MIN_STICKERS_WHATSAPP -> { showToast(R.string.limit_3_items); return }
            paths.size > MAX_STICKERS_WHATSAPP -> { showToast(R.string.limit_30_items); return }
        }
        showPackNameDialog { packName ->
            viewModel.addToWhatsapp(requireContext(), packName, ArrayList(paths)) { pack ->
                if (pack != null) { addToWhatsapp(pack); resetSelection() }
                else showToast("Failed to create sticker pack")
            }
        }
    }

    private fun showPackNameDialog(onConfirm: (String) -> Unit) {
        val editText = android.widget.EditText(requireContext()).apply {
            hint = "Enter sticker pack name"
            setText("My Ponies ${System.currentTimeMillis() % 1000}")
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Create Sticker Pack")
            .setView(editText)
            .setPositiveButton("Create") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) onConfirm(name) else showToast("Please enter a pack name")
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }




    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != ADD_PACK_REQUEST) return
        when (resultCode) {
            android.app.Activity.RESULT_OK       -> showToast("Sticker pack added successfully")
            android.app.Activity.RESULT_CANCELED -> {
                val err = data?.getStringExtra("validation_error")
                if (err != null) { Log.e("MyPonyFragment", "Validation: $err"); showToast("Failed: $err") }
                else showToast("Cancelled")
            }
        }
    }

    // ── TELEGRAM ──────────────────────────────────────────────────────────────

    private fun handleTelegramShare() {
        val paths = getSharePaths()
        if (paths.isEmpty()) { showToast(R.string.please_select_an_image); return }
        viewModel.addToTelegram(requireContext(), ArrayList(paths))
        resetSelection()
    }

    // ── UTILITY ───────────────────────────────────────────────────────────────

    private fun showToast(resId: Int)  = android.widget.Toast.makeText(requireContext(), resId, android.widget.Toast.LENGTH_SHORT).show()
    private fun showToast(msg: String) = android.widget.Toast.makeText(requireContext(), msg,   android.widget.Toast.LENGTH_SHORT).show()

    // ── BASE OVERRIDES ────────────────────────────────────────────────────────

    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.setOnClickListener {
            if (myAvatarAdapter.items.any { it.isShowSelection } ||
                myDesignAdapter.items.any { it.isShowSelection }
            ) {
                resetSelection()  // Thoát selection mode, KHÔNG navigate
            } else {
                findNavController().navigateUp()
            }
        }
    }

    override fun bindViewModel() {}

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentMyPonyBinding = FragmentMyPonyBinding.inflate(inflater, container, false)

    override fun onResume() {
        super.onResume()
        if (isAvatarTab.value) loadAvatarData() else loadDesignData()
    }
}