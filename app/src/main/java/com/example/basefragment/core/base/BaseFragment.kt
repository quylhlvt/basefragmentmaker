package com.example.basefragment.core.base


import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.example.basefragment.R
import com.example.basefragment.ViewModelActivity
import com.example.basefragment.core.extention.gone
import com.example.basefragment.core.extention.hideNavigation
import com.example.basefragment.core.extention.visible
import com.example.basefragment.core.helper.SharedPreferencesManager
import com.example.basefragment.databinding.DialogbaseBinding
import com.google.android.material.snackbar.Snackbar
import java.util.Locale
import javax.inject.Inject
import kotlin.getValue

abstract class BaseFragment<VB : ViewBinding, VM : ViewModel>(  private val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB,
                                                                private val viewModelClass: Class<VM>) : Fragment() {

    private lateinit var _binding: VB
    protected val binding: VB get() = _binding
    private var dialog: Dialog? = null
    private var confirmDialogBinding: DialogbaseBinding? = null
    var onYesClick: (() -> Unit)? = null
    var onNoClick: (() -> Unit)? = null
    protected val viewModelActivity: ViewModelActivity by activityViewModels()
    protected val viewModel: VM by lazy {
        ViewModelProvider(this)[viewModelClass]
    }
    open fun setupPreViews() {}
    abstract fun viewListener()
    protected var toast: Toast? = null
    @Inject
    internal lateinit var sharedPreferences: SharedPreferencesManager
    abstract fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.v(TAG, "onCreateView: $this")
        _navController = findNavController()
        _binding = inflateBinding(inflater, container, savedInstanceState)
        setupPreViews()
        return binding.root
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // ✅ Re-apply language
        val savedLanguage = sharedPreferences.isLanguageKey()
        if (savedLanguage.isNotEmpty()) {
            val locale = Locale(savedLanguage)
            Locale.setDefault(locale)

            val config = Configuration(requireContext().resources.configuration)
            config.setLocale(locale)

            requireContext().resources.updateConfiguration(
                config,
                requireContext().resources.displayMetrics
            )
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.v(TAG, "onViewCreated: $this")
        initView()
        initText()
        viewListener()
        bindViewModel()
        observeData()
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)

        Log.v(TAG, "onAttach: $this")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v(TAG, "onCreate: $this")
    }
    override fun onStart() {
        super.onStart()

        Log.v(TAG, "onStart: $this")
    }

    override fun onResume() {
        super.onResume()
        Log.v(TAG, "onResume: $this")
    }

    override fun onPause() {
        super.onPause()
        Log.v(TAG, "onPause: $this")
    }

    override fun onStop() {
        super.onStop()
        Log.v(TAG, "onStop: $this")
    }

    override fun onDestroyView() {
        hideLoading()
        super.onDestroyView()
        Log.v(TAG, "onDestroyView: $this")
        _navController = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.v(TAG, "onDestroy: $this")
    }

    override fun onDetach() {
        super.onDetach()
        Log.v(TAG, "onDetach: $this")
    }
    private var _navController: NavController? = null

    protected val navController: NavController? get() = _navController
    open fun initView() {}
    open fun initText() {}
    open fun observeData() {}

    fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
    fun showToast(content: Any) {
        if (toast != null){
            toast?.cancel()
        }
        val contentString = when (content) {
            is String -> content
            is Int -> getString(content)
            else -> {""}
        }
        toast = Toast.makeText(requireContext(), contentString, Toast.LENGTH_SHORT)
        toast?.show()
    }
    fun showLoading(
        cancelable: Boolean = false,
        select: Boolean = false,  // true = confirm dialog, false = loading
        title: String? = null,
        message: String? = getString(R.string.loading),
    ) {
        hideLoading()

        dialog = Dialog(requireContext(),R.style.BaseDialog).apply {
            val binding = DialogbaseBinding.inflate(layoutInflater)
            setContentView(binding.root)
            confirmDialogBinding = binding

            // Cập nhật text
            title?.let { binding.txtTitle.text = it } // nếu có TextView title
            binding.txtContent.text = message ?: ""

            if (select) {
                // Hiện nút Yes/No
                binding.btnNo.visible()
                binding.btnYes.visible()
                binding.btnNo.setOnClickListener {
                    onNoClick?.invoke()
                    dismiss()
                }
                binding.btnYes.setOnClickListener {
                    onYesClick?.invoke()
                    dismiss()
                }
            } else {
                // Ẩn nút Yes/No (chỉ loading)
                binding.btnNo.gone()
                binding.btnYes.gone()
            }

            setCancelable(cancelable)
            window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                // Đặt layout MATCH_PARENT cho cả width và height
                setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                setGravity(Gravity.CENTER)
            }
            show()
        }
    }
    fun hideLoading() {
        dialog?.dismiss()
        dialog = null
        dialog = null
        requireActivity().hideNavigation(true)
    }
    fun showLoadingSafe() {
        if (!isAdded || activity == null) return
        requireActivity().runOnUiThread {
            showLoading()
            requireActivity().hideNavigation(true)
        }
    }

    fun hideLoadingSafe() {
        if (!isAdded || activity == null) return
        requireActivity().runOnUiThread {
            hideLoading()
        }
    }

    // Hàm tiện ích để show confirm (dễ dùng)
    fun showConfirmDialog(
        message: String,
        title: String? = null,

        onYes: () -> Unit,
        onNo: (() -> Unit)? = null
    ) {
        onYesClick = onYes
        onNoClick = onNo

        showLoading(
            cancelable = true,
            select = true,
            title = title,
            message = message

        )
    }

    abstract fun bindViewModel()
}