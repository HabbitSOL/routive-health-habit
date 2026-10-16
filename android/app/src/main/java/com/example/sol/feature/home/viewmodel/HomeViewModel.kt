package com.example.sol.feature.home.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sol.feature.home.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface SectionState<out T> {
    data object Loading : SectionState<Nothing>
    data object Empty : SectionState<Nothing>
    data class Ready<T>(val value: T) : SectionState<T>
    data object Error : SectionState<Nothing>
}
data class HomeUiState(
    val habits: SectionState<HabitSummary> = SectionState.Loading,
    val meal: SectionState<MealSummary> = SectionState.Loading,
    val medication: SectionState<MedicationSummary> = SectionState.Loading,
)

class HomeViewModel(private val repository: HomeRepository = DemoHomeRepository()) : ViewModel() {
    var state by mutableStateOf(HomeUiState())
        private set
    var mode by mutableStateOf(DemoMode.EMPTY)
        private set
    var completedHabits by mutableStateOf(setOf<Int>())
        private set
    private val jobs = mutableMapOf<HomeSection, Job>()
    private val generations = mutableMapOf<HomeSection, Int>()

    init { reload() }
    fun selectMode(value: DemoMode) {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        mode = value
        completedHabits = if (value == DemoMode.EMPTY) emptySet() else setOf(0)
        state = HomeUiState()
        reload()
    }
    fun reset() = selectMode(DemoMode.EMPTY)
    fun reload() = HomeSection.entries.forEach { load(it) }
    fun retry(section: HomeSection) = load(section, recovering = true)

    fun toggleHabit(index: Int) {
        if (index !in 0..2) return
        if (mode == DemoMode.EMPTY) mode = DemoMode.SAMPLE
        completedHabits = if (index in completedHabits) completedHabits - index else completedHabits + index
        jobs.remove(HomeSection.HABITS)?.cancel()
        generations[HomeSection.HABITS] = (generations[HomeSection.HABITS] ?: 0) + 1
        state = state.copy(habits = SectionState.Ready(HabitSummary(completedHabits.size, 3)))
    }
    private fun load(section: HomeSection, recovering: Boolean = false) {
        if (jobs[section]?.isActive == true) return
        val generation = (generations[section] ?: 0) + 1
        generations[section] = generation
        val requestMode = if (recovering && mode == DemoMode.PARTIAL_ERROR) DemoMode.SAMPLE else mode
        val completed = completedHabits.toSet()
        setLoading(section)
        jobs[section] = viewModelScope.launch {
            when (section) {
                HomeSection.HABITS -> {
                    val result = result { repository.habits(requestMode, completed) }
                    if (generations[section] == generation) state = state.copy(habits = result)
                }
                HomeSection.MEAL -> {
                    val result = result { repository.meal(requestMode) }
                    if (generations[section] == generation) state = state.copy(meal = result)
                }
                HomeSection.MEDICATION -> {
                    val result = result { repository.medication(requestMode) }
                    if (generations[section] == generation) state = state.copy(medication = result)
                }
            }
        }
    }
    private fun setLoading(section: HomeSection) {
        state = when(section) {
            HomeSection.HABITS -> state.copy(habits = SectionState.Loading)
            HomeSection.MEAL -> state.copy(meal = SectionState.Loading)
            HomeSection.MEDICATION -> state.copy(medication = SectionState.Loading)
        }
    }
    private suspend fun <T> result(block: suspend () -> T?): SectionState<T> = try {
        block()?.let { SectionState.Ready(it) } ?: SectionState.Empty
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        SectionState.Error
    }
}
