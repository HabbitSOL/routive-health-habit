package com.example.sol.feature.auth.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.format.DateTimeParseException

// UI draft only. Not persisted and not an API request or an onboarding completion record.
data class ProfileDraft(
    val nickname: String = "",
    val birthDate: String = "",
    val sex: String? = null,
    val heightCm: String = "",
    val errors: Map<String, String> = emptyMap(),
    val previewComplete: Boolean = false,
)

class ProfileViewModel : ViewModel() {
    var state by mutableStateOf(ProfileDraft())
        private set

    fun nickname(value: String) { state = state.copy(nickname = value, errors = state.errors - "nickname") }
    fun birthDate(value: String) { state = state.copy(birthDate = value, errors = state.errors - "birthDate") }
    fun sex(value: String?) { state = state.copy(sex = value) }
    fun height(value: String) { state = state.copy(heightCm = value, errors = state.errors - "heightCm") }
    fun dismissPreview() { state = state.copy(previewComplete = false) }
    fun load(profile: ProfileDraft?) { state = profile?.copy(errors = emptyMap(), previewComplete = false) ?: ProfileDraft() }

    fun validate() {
        val errors = mutableMapOf<String, String>()
        val nickname = state.nickname.trim()
        if (nickname.isBlank()) errors["nickname"] = "사용할 닉네임을 입력해 주세요."
        else if (nickname.codePointCount(0, nickname.length) > 30)
            errors["nickname"] = "닉네임은 30자 이내로 입력해 주세요."
        val birth = state.birthDate.trim()
        if (birth.isNotEmpty()) {
            val date = try { LocalDate.parse(birth) } catch (_: DateTimeParseException) { null }
            if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(birth) || date == null)
                errors["birthDate"] = "실제 날짜를 YYYY-MM-DD 형식으로 입력해 주세요."
            else if (date.isAfter(LocalDate.now())) errors["birthDate"] = "미래 날짜는 입력할 수 없어요."
        }
        val height = state.heightCm.trim()
        if (height.isNotEmpty()) {
            val value = height.toBigDecimalOrNull()
            if (!Regex("\\d+(\\.\\d{1,2})?").matches(height) || value == null ||
                value < 1.toBigDecimal() || value > 300.toBigDecimal())
                errors["heightCm"] = "1~300cm 범위에서 소수 둘째 자리까지 입력해 주세요."
        }
        state = state.copy(nickname = nickname, errors = errors, previewComplete = errors.isEmpty())
    }
}
