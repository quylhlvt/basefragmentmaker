package com.example.basefragment.ui.onboarding.intro

import com.example.basefragment.data.model.intro.IntroModel

class IntroContact {
}
data class IntroUiState(
    val pagesSplash: List<IntroModel>? = emptyList(),
    val page: Int = 0,
    val textButton: String? = null
)

sealed class IntroSingleEvent {
    data object NavigateToNextScreen : IntroSingleEvent()
}