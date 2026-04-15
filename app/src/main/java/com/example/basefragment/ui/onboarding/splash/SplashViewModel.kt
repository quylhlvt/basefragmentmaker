package com.example.basefragment.ui.onboarding.splash

import androidx.lifecycle.ViewModel
import com.example.basefragment.core.helper.SharedPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject
@HiltViewModel
class SplashViewModel @Inject constructor(    private val sharedPreferences: SharedPreferencesManager) : ViewModel() {
    fun isLang(): Boolean = sharedPreferences.isLanuageScreen()

}