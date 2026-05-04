
package com.example.basefragment.ui.main.createPony

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.basefragment.R
import com.example.basefragment.ViewModelActivity
import com.example.basefragment.core.base.BaseFragment
import com.example.basefragment.core.extention.safeNavigate
import com.example.basefragment.core.extention.setImageActionBar
import com.example.basefragment.core.extention.setTextActionBar
import com.example.basefragment.databinding.FragmentChoosePonyBinding
import com.example.basefragment.ui.main.customize.CustomizeFragment.Companion.ARG_TEMPLATE_INDEX
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChoosePonyFragment : BaseFragment<FragmentChoosePonyBinding, ChoosePonyViewModel>(
    FragmentChoosePonyBinding::inflate,
    ChoosePonyViewModel::class.java
) {
    private val mainViewModel: ViewModelActivity by activityViewModels()
    private lateinit var adapter: ChoosePonyAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentChoosePonyBinding = FragmentChoosePonyBinding.inflate(inflater, container, false)

    override fun initView() {
        setImageActionBar(binding.actionBar.btnActionBarLeft, R.drawable.back_app)
        setTextActionBar(
            binding.actionBar.tvCenter,
            getString(R.string.category)
        )

        adapter = ChoosePonyAdapter { _, position ->
            findNavController().safeNavigate(
                R.id.action_createPony_to_custom,
                bundleOf(ARG_TEMPLATE_INDEX to position)
            )
        }

        binding.recycleChoose.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter       = this@ChoosePonyFragment.adapter
            itemAnimator  = null
        }
    }

    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.setOnClickListener { findNavController().navigateUp() }
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Avatar đã được preload trong GetCatalogueUseCase trước khi vào đây
                // → submitList xong là hiện ngay, không cần preload thêm
                launch {
                    mainViewModel.templates.collect { templates ->
                        adapter.submitList(templates)
                    }
                }
                launch {
                    mainViewModel.error.collect { error ->
                        error?.let { showSnackbar(it) }
                    }
                }
            }
        }
    }

    override fun bindViewModel() {}
}
