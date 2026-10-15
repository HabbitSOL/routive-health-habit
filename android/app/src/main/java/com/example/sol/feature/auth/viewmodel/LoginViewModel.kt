package com.example.sol.feature.auth.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class LoginUiState(
    val email: String = "", val password: String = "",
    val emailError: String? = null, val passwordError: String? = null,
    val message: String? = null,
)

class LoginViewModel : ViewModel() {
    // Memory only: passwords are never saved to disk, SavedStateHandle or logs.
    var state by mutableStateOf(LoginUiState())
        private set

    fun updateEmail(value: String) { state = state.copy(email = value, emailError = null, message = null) }
    fun updatePassword(value: String) { state = state.copy(password = value, passwordError = null, message = null) }
    fun clear() { state = LoginUiState() }

    fun previewValidation() {
        val email = state.email.trim()
        val emailError = when {
            email.isEmpty() -> "이메일을 입력해 주세요."
            !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email) -> "이메일 형식을 확인해 주세요."
            else -> null
        }
        val passwordError = if (state.password.isEmpty()) "비밀번호를 입력해 주세요." else null
        state = state.copy(emailError = emailError, passwordError = passwordError,
            message = if (emailError == null && passwordError == null)
                "입력 형식 확인 완료. 실제 로그인은 아직 연결되지 않았어요. 아래에서 데모를 둘러보세요." else null)
    }
}
