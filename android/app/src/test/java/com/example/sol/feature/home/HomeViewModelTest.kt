package com.example.sol.feature.home

import com.example.sol.feature.home.data.*
import com.example.sol.feature.home.viewmodel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun emptyModeHasStartStates() = runTest(dispatcher) {
        val model = HomeViewModel()
        assertEquals(SectionState.Loading, model.state.habits)
        advanceUntilIdle()
        assertEquals(SectionState.Empty, model.state.habits)
        assertEquals(SectionState.Empty, model.state.meal)
        assertEquals(SectionState.Empty, model.state.medication)
    }
    @Test fun sampleModeShowsIndependentSections() = runTest(dispatcher) {
        val model = HomeViewModel()
        model.selectMode(DemoMode.SAMPLE)
        advanceTimeBy(351)
        assertEquals(HabitSummary(1, 3), (model.state.habits as SectionState.Ready).value)
        assertEquals(SectionState.Loading, model.state.meal)
        advanceUntilIdle()
        assertTrue(model.state.meal is SectionState.Ready)
        assertTrue(model.state.medication is SectionState.Ready)
    }
    @Test fun mealFailureDoesNotHideOtherSections() = runTest(dispatcher) {
        val model = HomeViewModel()
        model.selectMode(DemoMode.PARTIAL_ERROR)
        advanceUntilIdle()
        assertTrue(model.state.habits is SectionState.Ready)
        assertEquals(SectionState.Error, model.state.meal)
        assertTrue(model.state.medication is SectionState.Ready)
    }
    @Test fun retryOnlyLoadsFailedSection() = runTest(dispatcher) {
        val model = HomeViewModel()
        model.selectMode(DemoMode.PARTIAL_ERROR)
        advanceUntilIdle()
        val habits = model.state.habits
        val medication = model.state.medication
        model.retry(HomeSection.MEAL)
        assertEquals(SectionState.Loading, model.state.meal)
        assertSame(habits, model.state.habits)
        assertSame(medication, model.state.medication)
        advanceUntilIdle()
        assertTrue(model.state.meal is SectionState.Ready)
    }
    @Test fun latestModeWinsWhenRequestsOverlap() = runTest(dispatcher) {
        val model = HomeViewModel()
        model.selectMode(DemoMode.PARTIAL_ERROR)
        advanceTimeBy(200)
        model.selectMode(DemoMode.EMPTY)
        advanceUntilIdle()
        assertEquals(SectionState.Empty, model.state.habits)
        assertEquals(SectionState.Empty, model.state.meal)
        assertEquals(SectionState.Empty, model.state.medication)
    }
    @Test fun habitChangeCannotBeOverwrittenByPendingLoad() = runTest(dispatcher) {
        val model = HomeViewModel()
        model.selectMode(DemoMode.SAMPLE)
        advanceTimeBy(100)
        model.toggleHabit(1)
        advanceUntilIdle()
        assertEquals(HabitSummary(2, 3), (model.state.habits as SectionState.Ready).value)
        model.toggleHabit(1)
        assertEquals(HabitSummary(1, 3), (model.state.habits as SectionState.Ready).value)
    }
    @Test fun repeatedRetryDoesNotDuplicateRequests() = runTest(dispatcher) {
        var calls = 0
        val repository = object : HomeRepository {
            override suspend fun habits(mode: DemoMode, completed: Set<Int>): HabitSummary? = null
            override suspend fun meal(mode: DemoMode): MealSummary? {
                calls++
                kotlinx.coroutines.delay(100)
                return null
            }
            override suspend fun medication(mode: DemoMode): MedicationSummary? = null
        }
        val model = HomeViewModel(repository)
        advanceUntilIdle()
        repeat(10) { model.retry(HomeSection.MEAL) }
        advanceUntilIdle()
        assertEquals(2, calls)
    }
    @Test fun resetClearsDemoProgress() = runTest(dispatcher) {
        val model = HomeViewModel()
        model.toggleHabit(2)
        assertEquals(HabitSummary(1, 3), (model.state.habits as SectionState.Ready).value)
        model.reset()
        advanceUntilIdle()
        assertTrue(model.completedHabits.isEmpty())
        assertEquals(DemoMode.EMPTY, model.mode)
        assertEquals(SectionState.Empty, model.state.habits)
    }
}
