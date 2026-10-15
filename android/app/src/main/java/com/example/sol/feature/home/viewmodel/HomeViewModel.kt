package com.example.sol.feature.home.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.sol.feature.home.data.*

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(val summary: HomeSummary) : HomeUiState
    data object Error : HomeUiState
}
class HomeViewModel(private val repository: HomeRepository = DemoHomeRepository()) : ViewModel() {
    var state: HomeUiState by mutableStateOf(HomeUiState.Loading)
        private set
    init { reload() }
    fun reload() { state = HomeUiState.Ready(repository.summary()) }
    fun previewLoading() { state = HomeUiState.Loading }
    fun previewError() { state = HomeUiState.Error }
}
