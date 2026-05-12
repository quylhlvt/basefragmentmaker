package com.oc.catemoji.catoc.ui.onboarding.permission

import androidx.lifecycle.ViewModel
import com.oc.catemoji.catoc.core.helper.PermissionHelper
import com.oc.catemoji.catoc.core.helper.SharedPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
@HiltViewModel
class PermissionViewModel @Inject constructor() : ViewModel() {
    fun getStoragePermissions() = PermissionHelper.storagePermission
    fun getNotificationPermissions() = PermissionHelper.notificationPermission
}