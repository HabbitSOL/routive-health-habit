package com.example.sol.feature.home.data

import kotlinx.coroutines.delay

// UI-only demo models. Replace through a mapper once the server contract is supplied.
enum class HomeSection { HABITS, MEAL, MEDICATION }
enum class DemoMode { EMPTY, SAMPLE, PARTIAL_ERROR }
data class HabitSummary(val completed: Int, val total: Int)
data class MealSummary(val title: String, val timeLabel: String, val description: String)
data class MedicationSummary(val title: String, val scheduledLabel: String, val description: String)

interface HomeRepository {
    suspend fun habits(mode: DemoMode, completed: Set<Int>): HabitSummary?
    suspend fun meal(mode: DemoMode): MealSummary?
    suspend fun medication(mode: DemoMode): MedicationSummary?
}

class DemoHomeRepository : HomeRepository {
    override suspend fun habits(mode: DemoMode, completed: Set<Int>): HabitSummary? {
        delay(350)
        return if (mode == DemoMode.EMPTY) null else HabitSummary(completed.size, 3)
    }
    override suspend fun meal(mode: DemoMode): MealSummary? {
        delay(500)
        if (mode == DemoMode.PARTIAL_ERROR) error("Demo meal failure")
        return if (mode == DemoMode.EMPTY) null else MealSummary("최근 식사 · 점심", "12:30", "현미밥 · 채소 · 두부")
    }
    override suspend fun medication(mode: DemoMode): MedicationSummary? {
        delay(650)
        // Static presentation fixture only. No medical advice or schedule calculation.
        return if (mode == DemoMode.EMPTY) null else MedicationSummary("등록된 복약 일정", "오늘 18:30 · 예시", "복용할 항목과 상세 일정은 복약 화면에서 확인해요.")
    }
}
