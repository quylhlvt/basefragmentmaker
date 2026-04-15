package com.example.basefragment.ui.main.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.navigation.fragment.findNavController
import com.example.basefragment.R
import com.example.basefragment.core.base.BaseFragment
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.loadImage
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.safeNavigate
import com.example.basefragment.core.extention.setImageActionBar
import com.example.basefragment.core.extention.visible
import com.example.basefragment.databinding.FragmentViewBinding
import com.example.basefragment.ui.main.customize.CustomizeFragment
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class ViewFragment : BaseFragment<FragmentViewBinding, ViewViewModel>(
    FragmentViewBinding::inflate,
    ViewViewModel::class.java
) {
    private val imagePath: String by lazy { arguments?.getString("imagePath") ?: "" }
    private val imageType: Int    by lazy { arguments?.getInt("imageType", 0) ?: 0 }
    private val idEdit: String    by lazy { arguments?.getString("idEdit") ?: "" }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentViewBinding = FragmentViewBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.apply {
            setImageActionBar(actionBar.btnActionBarLeft, R.drawable.back_app)
            loadImage(requireContext(), imagePath, imvImage)

            when (imageType) {
                // ── Type 0: Từ AddCharacter — success screen ──────────────────
                0 -> {
                    tvSuccess.visible()
                    setImageActionBar(actionBar.btnActionBarRight, R.drawable.ic_home)
                    // ✅ Show 2 nút bottom
                    btnBottomLeft.apply  { visible(); text = getString(R.string.my_work) }
                    btnBottomRight.apply { visible(); text = getString(R.string.download) }
                }
                // ── Type 1: Avatar từ MyPony ──────────────────────────────────
                1 -> {
                    btnBottomLeft.text = getString(R.string.edit)
                    setImageActionBar(actionBar.btnActionBarRight,       R.drawable.ic_delete)
                    setImageActionBar(actionBar.btnActionBarNextToRight, R.drawable.ic_share)
                }
                // ── Type 2: Design từ MyPony ──────────────────────────────────
                2 -> {
                    btnBottomLeft.text = getString(R.string.share)
                    setImageActionBar(actionBar.btnActionBarRight, R.drawable.ic_delete)
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.onClick {
                findNavController().navigateUp()
            }

            when (imageType) {
                // ── Type 0: Từ AddCharacter ───────────────────────────────────
                0 -> {
                    // Home button
                    actionBar.btnActionBarRight.onClick {
                        findNavController().navigate(
                            R.id.homeFragment,
                            null,
                            androidx.navigation.NavOptions.Builder()
                                .setPopUpTo(R.id.homeFragment, true)
                                .build()
                        )
                    }
                    // ✅ MyPony button
                    btnBottomLeft.onClick {
                        findNavController().navigate(
                            R.id.myPony,
                            null,
                            androidx.navigation.NavOptions.Builder()
                                .setPopUpTo(R.id.homeFragment, false)
                                .build()
                        )
                    }
                    // ✅ Download button
                    btnBottomRight.onClick { downloadImage() }
                }

                // ── Type 1: Avatar từ MyPony ──────────────────────────────────
                1 -> {
                    actionBar.btnActionBarRight.onClick       { confirmDelete() }
                    actionBar.btnActionBarNextToRight.onClick { shareImage() }
                    btnBottomLeft.onClick                     { navigateToEdit() }
                    btnBottomRight.onClick                    { downloadImage() }
                }

                // ── Type 2: Design từ MyPony ──────────────────────────────────
                2 -> {
                    actionBar.btnActionBarRight.onClick       { confirmDelete() }
                    btnBottomLeft.onClick                     { shareImage() }
                    btnBottomRight.onClick                    { downloadImage() }
                }
            }
        }
    }
    private fun shareImage() {
        if (imagePath.isEmpty()) return
        val uri = androidx.core.content.FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            java.io.File(imagePath)
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(android.content.Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun downloadImage() {
        if (imagePath.isEmpty()) return
        viewModel.downloadFile(requireContext(), imagePath) { success ->
            showToast(
                if (success) getString(R.string.download_success)
                else getString(R.string.download_failed_please_try_again_later)
            )
        }
    }

    private fun confirmDelete() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(R.string.are_you_sure_want_to_delete_this_item)
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteFile(
                    path     = imagePath,
                    isAvatar = imageType == 1,
                    idEdit   = idEdit,
                    onDone   = {
                        findNavController().safeNavigate(R.id.action_view_to_myPony)
                    }
                )
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun navigateToEdit() {
        if (idEdit.isEmpty() || imageType != 1) return

        val customized = viewModelActivity.customizedCharacters.value
            .firstOrNull { it.id == idEdit }
            ?: run { showToast("Character not found"); return }

        val templateIndex = viewModelActivity.getTemplateIndexForCustomized(idEdit)
            .takeIf { it >= 0 }
            ?: run { showToast("Template not found"); return }

        val args = CustomizeFragment.newArgs(
            templateIndex   = templateIndex,
            isEdit          = true,
            customizedId    = idEdit,
            savedSelections = customized.selections,
            isFlipped       = customized.isFlipped
        )
        findNavController().safeNavigate(R.id.action_view_to_customize, args)
    }

    private fun showToast(msg: String) =
        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()

    override fun observeData() {}
    override fun bindViewModel() {}
}