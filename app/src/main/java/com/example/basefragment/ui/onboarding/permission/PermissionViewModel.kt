package com.example.basefragment.ui.onboarding.permission

import androidx.lifecycle.ViewModel
import com.example.basefragment.core.helper.PermissionHelper
import com.example.basefragment.core.helper.SharedPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor() : ViewModel() {

    private val _storageGranted = MutableStateFlow(false)
    val storageGranted: StateFlow<Boolean> = _storageGranted.asStateFlow()

    private val _notificationGranted = MutableStateFlow(false)
    val notificationGranted: StateFlow<Boolean> = _notificationGranted.asStateFlow()

    fun updateStorageGranted(sharePrefer: SharedPreferencesManager, granted: Boolean) {
        // ✅ Update SharedPreferences trước
        sharePrefer.setPermissionStorRequest(if (granted) 0 else sharePrefer.isPermissionStorRequest() + 1)

        // ✅ Force update bằng cách reset rồi set lại
        if (_storageGranted.value == granted) {
            _storageGranted.value = !granted // Toggle
        }
        _storageGranted.value = granted
    }

    fun updateNotificationGranted(sharePrefer: SharedPreferencesManager, granted: Boolean) {
        // ✅ Update SharedPreferences trước
        sharePrefer.setPermissionNotiRequest(if (granted) 0 else sharePrefer.isPermissionNotiRequest() + 1)

        // ✅ Force update bằng cách reset rồi set lại
        if (_notificationGranted.value == granted) {
            _notificationGranted.value = !granted // Toggle
        }
        _notificationGranted.value = granted
    }

    fun needGoToSettings(sharePrefer: SharedPreferencesManager, storage: Boolean): Boolean {
        return if (storage) {
            sharePrefer.isPermissionStorRequest() > 2 && !_storageGranted.value
        } else {
            sharePrefer.isPermissionNotiRequest() > 2 && !_notificationGranted.value
        }
    }

    fun getStoragePermissions() = PermissionHelper.storagePermission
    fun getNotificationPermissions() = PermissionHelper.notificationPermission
}