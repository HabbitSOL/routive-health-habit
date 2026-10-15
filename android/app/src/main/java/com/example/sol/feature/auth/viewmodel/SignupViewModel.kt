package com.example.sol.feature.auth.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class SignupStep { ACCOUNT, CONSENT, EMAIL, LOGIN }
data class SignupState(
    val step: SignupStep = SignupStep.ACCOUNT,
    val email: String = "", val password: String = "", val confirmation: String = "",
    val agreed: Set<String> = emptySet(), val errors: Map<String, String> = emptyMap(),
    val busy: Boolean = false, val error: String? = null, val message: String? = null,
    val failNext: Boolean = false, val cooldown: Int = 0,
)
data class ConsentItem(val type: String, val title: String, val required: Boolean)
val consentItems = listOf(
    ConsentItem("TERMS_OF_SERVICE", "이용약관", true),
    ConsentItem("PRIVACY", "개인정보 처리", true),
    ConsentItem("SENSITIVE_HEALTH_DATA", "민감 건강정보 처리", true),
    ConsentItem("MARKETING", "마케팅 수신", false),
)

/** Local flow simulator. No account creation, email, tokens, or durable credentials. */
class SignupViewModel : ViewModel() {
    var state by mutableStateOf(SignupState())
        private set
    private var request: Job? = null
    private var timer: Job? = null

    fun reset() { request?.cancel(); timer?.cancel(); state = SignupState() }
    fun email(value: String) { if (!state.busy) state = state.copy(email = value, errors = state.errors - "email", error = null) }
    fun password(value: String) { if (!state.busy) state = state.copy(password = value, errors = state.errors - "password", error = null) }
    fun confirmation(value: String) { if (!state.busy) state = state.copy(confirmation = value, errors = state.errors - "confirmation") }
    fun failNext(value: Boolean) { if (!state.busy) state = state.copy(failNext = value) }
    fun agree(type: String, checked: Boolean) {
        if (!state.busy) state = state.copy(agreed = if (checked) state.agreed + type else state.agreed - type, error = null)
    }
    fun agreeRequired() {
        if (!state.busy) state = state.copy(agreed = state.agreed + consentItems.filter { it.required }.map { it.type }, error = null)
    }
    private fun fieldErrors(confirm: Boolean): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        val email = state.email.trim()
        if (email.length > 320 || !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email))
            errors["email"] = "320자 이내의 올바른 이메일을 입력해 주세요."
        if (state.password.codePointCount(0, state.password.length) !in 8..16 ||
            state.password.none { it in "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~" })
            errors["password"] = "비밀번호는 특수문자를 포함해 8~16자로 입력해 주세요."
        if (confirm && state.password != state.confirmation) errors["confirmation"] = "비밀번호가 일치하지 않아요."
        return errors
    }
    fun next() {
        if (state.busy) return
        val errors = fieldErrors(true)
        state = state.copy(errors = errors, error = null)
        if (errors.isEmpty()) state = state.copy(step = SignupStep.CONSENT,
            email = state.email.trim().lowercase(Locale.ROOT))
    }
    fun back(): Boolean {
        if (state.busy) return false
        return if (state.step == SignupStep.CONSENT) {
            state = state.copy(step = SignupStep.ACCOUNT, error = null); false
        } else true
    }
    private fun simulate(onSuccess: () -> Unit) {
        if (state.busy) return
        state = state.copy(busy = true, error = null, message = null)
        request = viewModelScope.launch {
            delay(650)
            val fail = state.failNext
            state = state.copy(busy = false, failNext = false)
            if (fail) state = state.copy(error = "요청을 완료하지 못했어요. 입력은 유지되니 다시 시도해 주세요. (데모 오류)")
            else onSuccess()
        }
    }
    fun signup() {
        if (state.busy) return
        if (!consentItems.filter { it.required }.all { it.type in state.agreed }) {
            state = state.copy(error = "필수 항목 3개를 확인해 주세요."); return
        }
        simulate {
            state = state.copy(step = SignupStep.EMAIL, password = "", confirmation = "")
        }
    }
    fun resend() {
        if (state.cooldown > 0 || state.busy) return
        simulate {
            state = state.copy(message = "재발송 흐름을 체험했어요. 실제 메일은 발송되지 않습니다.", cooldown = 30)
            timer?.cancel()
            timer = viewModelScope.launch {
                while (state.cooldown > 0) { delay(1000); state = state.copy(cooldown = (state.cooldown - 1).coerceAtLeast(0)) }
            }
        }
    }
    fun verifyDemo() {
        simulate { state = state.copy(step = SignupStep.LOGIN, password = "", errors = emptyMap()) }
    }
    fun loginDemo(onSuccess: (String, Set<String>) -> Unit) {
        if (state.busy) return
        val errors = fieldErrors(false)
        state = state.copy(errors = errors)
        if (errors.isNotEmpty()) return
        simulate {
            val email = state.email
            val consent = state.agreed.toSet()
            state = state.copy(password = "", confirmation = "")
            onSuccess(email, consent)
        }
    }
}
