package com.example.sol.feature.home.data

// Display-only examples; these are not API response models or medical recommendations.
data class HomeSummary(val greeting: String, val habit: String, val meal: String, val medication: String)
interface HomeRepository { fun summary(): HomeSummary }
class DemoHomeRepository : HomeRepository {
    override fun summary() = HomeSummary(
        greeting = "오늘도 나를 위한 한 걸음",
        habit = "아직 등록한 습관이 없어요.",
        meal = "아직 기록한 식사가 없어요.",
        medication = "등록한 복약 일정이 없어요.",
    )
}
