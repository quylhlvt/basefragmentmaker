package com.example.basefragment.ui.main.quick

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.basefragment.R
import com.example.basefragment.core.base.BaseFragment
import com.example.basefragment.core.extention.onClick
import com.example.basefragment.core.extention.setImageActionBar
import com.example.basefragment.databinding.FragmentQuickBinding
import com.example.basefragment.ui.main.customize.CustomizeFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QuickFragment : BaseFragment<FragmentQuickBinding, QuickViewModel>(
    FragmentQuickBinding::inflate,
    QuickViewModel::class.java
) {

    private val adapter by lazy {
        QuickMixAdapter(viewModel).apply {   // ← truyền viewModel thay vì context
            onItemClick = { item ->
                val args = CustomizeFragment.newArgs(
                    templateIndex   = item.templateIndex,
                    isEdit          = false,
                    savedSelections = item.selections
                )
                findNavController().navigate(R.id.action_quick_to_custom, args)
            }
            onRegenerateClick = { pos -> viewModel.regenerateAt(pos) }
        }
    }

    override fun initView() {
        setImageActionBar(binding.actionBar.btnActionBarLeft, R.drawable.back_app)
        binding.recyclerViewQuick.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter       = this@QuickFragment.adapter
            itemAnimator  = null
        }

        binding.recyclerViewQuick.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                notifyVisibleRange()
            }
        })

        binding.btnRefreshAll.setOnClickListener {
            viewModel.forceGenerate()
        }

        viewModel.generate()
    }

    private fun notifyVisibleRange() {
        val lm    = binding.recyclerViewQuick.layoutManager as? GridLayoutManager ?: return
        val first = lm.findFirstVisibleItemPosition()
        val last  = lm.findLastVisibleItemPosition()
        if (first == RecyclerView.NO_POSITION) return
        viewModel.updateVisibleRange(first, last)
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.items.collect { list ->
                        adapter.submitList(list) {
                            // RecyclerView layout xong → báo visible range ngay
                            binding.recyclerViewQuick.post { notifyVisibleRange() }
                        }
                    }
                }
                launch {
                    viewModel.readyKey.collect { key ->
                        if (key != null) adapter.notifyKeyReady(key)
                    }
                }
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.btnRefreshAll?.isEnabled = !loading
                        if (loading) showLoadingSafe() else hideLoadingSafe()
                    }
                }
            }
        }
    }
    override fun viewListener() {
        binding.actionBar.btnActionBarLeft.onClick {
            findNavController().navigateUp()
        }
    }

    override fun bindViewModel() {}

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentQuickBinding = FragmentQuickBinding.inflate(inflater, container, false)

    override fun onResume() {
        super.onResume()
        hideLoadingSafe()
    }
}