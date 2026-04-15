package com.example.basefragment.ui.main.createPony

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.basefragment.R
import com.example.basefragment.ViewModelActivity
import com.example.basefragment.core.base.BaseFragment
import com.example.basefragment.core.extention.setImageActionBar
import com.example.basefragment.data.model.custom.CustomModel
import com.example.basefragment.databinding.FragmentChoosePonyBinding
import com.example.basefragment.databinding.ItemChooseBinding
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── VIEWMODEL ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ChoosePonyViewModel @Inject constructor() : ViewModel()
// Template data đến từ shared ViewModelActivity – không cần logic riêng ở đây.

// ── ADAPTER ───────────────────────────────────────────────────────────────────


// ── FRAGMENT ──────────────────────────────────────────────────────────────────
