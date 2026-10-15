package com.example.sol.feature.auth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.example.sol.core.ui.*
import com.example.sol.feature.auth.viewmodel.LoginUiState

@Composable
fun LoginScreen(state: LoginUiState, onEmail: (String) -> Unit,
    onPassword: (String) -> Unit, onValidate: () -> Unit,
    onExplore: () -> Unit, onSignup: () -> Unit) {
    var showPassword by rememberSaveable { mutableStateOf(false) }
    AuthBackdrop {
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            JourneyBanner("A LITTLE BETTER, EVERY DAY", "오늘도, 나를 돌보는 시간", "작은 실천이 모여 건강한 일상이 됩니다.")
            Text("SOL", style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("작은 습관이 만드는\n나의 건강한 하루", style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center)
            Text("오늘의 나를 가볍게 돌봐요", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            SolTextField(state.email, onEmail, "이메일", state.emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
            SolTextField(state.password, onPassword, "비밀번호", state.passwordError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                transformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { showPassword = !showPassword }) {
                    Text(if (showPassword) "숨기기" else "보기")
                } })
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            SolButton("로그인 · 데모", onValidate)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = onSignup) { Text("회원가입") }
                TextButton(onClick = onExplore) { Text("데모 둘러보기 →") }
            }
            Text("화면 체험용입니다. 실제 계정 정보를 입력하지 마세요.",
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
        }
    }
    }
}
