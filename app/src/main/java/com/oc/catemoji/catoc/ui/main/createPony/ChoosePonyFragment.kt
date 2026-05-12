
package com.oc.catemoji.catoc.ui.main.createPony

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
import com.oc.catemoji.catoc.R
import com.oc.catemoji.catoc.ViewModelActivity
import com.oc.catemoji.catoc.core.base.BaseFragment
import com.oc.catemoji.catoc.core.extention.InternetExtension.isInternetAvailable
import com.oc.catemoji.catoc.core.extention.InternetExtension.isNetworkConnected
import com.oc.catemoji.catoc.core.extention.safeNavigate
import com.oc.catemoji.catoc.core.extention.setImageActionBar
import com.oc.catemoji.catoc.core.extention.setTextActionBar
import com.oc.catemoji.catoc.databinding.FragmentChoosePonyBinding
import com.oc.catemoji.catoc.ui.main.customize.CustomizeFragment.Companion.ARG_TEMPLATE_INDEX
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChoosePonyFragment : BaseFragment<FragmentChoosePonyBinding, ChoosePonyViewModel>(
    FragmentChoosePonyBinding::inflate,
    ChoosePonyViewModel::class.java
) {
    private val mainViewModel: ViewModelActivity by activityViewModels()
    private lateinit var adapter: ChoosePonyAdapter
    private var isFirstLoad = true

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

        adapter = ChoosePonyAdapter { character, position ->
            // ✅ Chỉ check network với online item
            if (character.id.startsWith("online_") && !isInternetAvailable(requireContext())) {
                showNoInternetDialog()
                return@ChoosePonyAdapter
            }
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
                    kotlinx.coroutines.flow.combine(
                        mainViewModel.templates,
                        mainViewModel.isFetchingOnlineFlow
                    ) { templates, isFetching -> Pair(templates, isFetching) }
                        .collect { (templates, isFetching) ->
                            adapter.submitList(templates)
                            if (isFirstLoad && !isFetching) {
                                isFirstLoad = false
                                if (templates.size <= 1) showLoadingDataDialog()
                            }
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
