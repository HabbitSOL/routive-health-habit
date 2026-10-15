package com.example.sol.feature.auth.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.example.sol.core.ui.SolButton
import com.example.sol.core.ui.SolTextField
import com.example.sol.core.ui.JourneyBanner
import com.example.sol.core.ui.JourneySteps
import com.example.sol.feature.auth.viewmodel.*

@Composable
fun SignupScreen(model: SignupViewModel, onExit: () -> Unit, onProfile: (String, Set<String>) -> Unit) {
    val s = model.state
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf<String?>(null) }
    var exitConfirm by rememberSaveable { mutableStateOf(false) }
    var tools by rememberSaveable { mutableStateOf(false) }
    val goBack = { if (model.back()) exitConfirm = true }
    BackHandler { goBack() }
    AuthBackdrop {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            TextButton(onClick = goBack, enabled = !s.busy) { Text("← 이전으로") }
            JourneyBanner("SOL · YOUR DAILY WELLNESS", "작은 시작, 나다운 하루", "나를 돌보는 일상을 함께 만들어가요.")
            Text("가입 과정 체험 · 실제 계정은 생성되지 않아요", style = MaterialTheme.typography.labelMedium)
            JourneySteps(s.step.ordinal)
            when (s.step) {
                SignupStep.ACCOUNT -> {
                    Text("나를 위한 첫걸음", style = MaterialTheme.typography.headlineLarge)
                    Text("이메일로 시작하고, 마지막에 나의 프로필을 설정해요.")
                    SolTextField(s.email, model::email, "이메일", s.errors["email"],
                        KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
                    SolTextField(s.password, model::password, "비밀번호", s.errors["password"],
                        KeyboardOptions(keyboardType = KeyboardType.Password),
                        if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        { TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "숨기기" else "보기") } })
                    Text("8~16자 · 특수문자 필수 (예: ! @ # $ %)", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val lengthOk = s.password.codePointCount(0, s.password.length) in 8..16
                        val symbolOk = s.password.any { it in "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~" }
                        Text("${if (lengthOk) "✓" else "○"} 8~16자", style = MaterialTheme.typography.labelMedium,
                            color = if (lengthOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${if (symbolOk) "✓" else "○"} 특수문자 포함", style = MaterialTheme.typography.labelMedium,
                            color = if (symbolOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SolTextField(s.confirmation, model::confirmation, "비밀번호 확인", s.errors["confirmation"],
                        KeyboardOptions(keyboardType = KeyboardType.Password), PasswordVisualTransformation())
                    SolButton("다음", model::next)
                }
                SignupStep.CONSENT -> {
                    Text("시작 전 확인해 주세요", style = MaterialTheme.typography.headlineLarge)
                    Text("아래 항목은 화면 체험용입니다. 실제 약관 원문과 게시 버전은 준비 중이며 법적 동의를 수집하지 않습니다.")
                    OutlinedButton(onClick = model::agreeRequired, enabled = !s.busy) { Text("필수 항목만 선택") }
                    consentItems.forEach { item ->
                        Surface(shape = RoundedCornerShape(20.dp),
                            color = if (item.type in s.agreed) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.weight(1f).toggleable(item.type in s.agreed, enabled = !s.busy,
                                role = Role.Checkbox, onValueChange = { model.agree(item.type, it) }).padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = item.type in s.agreed, onCheckedChange = null, enabled = !s.busy)
                                Spacer(Modifier.width(8.dp))
                                Text("[${if (item.required) "필수" else "선택"}] ${item.title}")
                            }
                            TextButton(onClick = { details = item.title }) { Text("보기") }
                        }
                        }
                    }
                    Text("마케팅 수신을 선택하지 않아도 계속할 수 있어요.", style = MaterialTheme.typography.bodySmall)
                    SolButton(if (s.busy) "처리 중…" else "가입 요청 체험", model::signup, !s.busy)
                }
                SignupStep.EMAIL -> {
                    Text("이메일을 확인하는 단계예요", style = MaterialTheme.typography.headlineLarge)
                    Text(s.email, style = MaterialTheme.typography.titleMedium)
                    Text("실제 서비스에서는 인증 메일의 링크를 확인한 뒤 로그인합니다. 지금은 메일을 보내지 않는 데모입니다.")
                    SolButton(if (s.busy) "처리 중…" else "이메일 인증 완료 체험", model::verifyDemo, !s.busy)
                    OutlinedButton(onClick = model::resend, enabled = !s.busy && s.cooldown == 0) {
                        Text(if (s.cooldown > 0) "${s.cooldown}초 후 다시 체험" else "인증 메일 재발송 체험")
                    }
                }
                SignupStep.LOGIN -> {
                    Text("이어서 로그인해 주세요", style = MaterialTheme.typography.headlineLarge)
                    Text(s.email)
                    Text("인증 다음 단계를 체험합니다. 실제 자격 증명 확인 없이, 형식에 맞는 예시 비밀번호로 진행할 수 있어요.")
                    SolTextField(s.password, model::password, "예시 비밀번호", s.errors["password"],
                        KeyboardOptions(keyboardType = KeyboardType.Password), PasswordVisualTransformation())
                    SolButton(if (s.busy) "처리 중…" else "로그인 체험 후 프로필 설정", { model.loginDemo(onProfile) }, !s.busy)
                }
            }
            if (s.busy) CircularProgressIndicator(Modifier.size(24.dp))
            s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            s.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Text("실제 이메일·비밀번호 대신 예시 정보를 사용해 주세요.", style = MaterialTheme.typography.bodySmall)
            if (s.step != SignupStep.ACCOUNT) {
                TextButton(onClick = { tools = !tools }) { Text("데모 도구") }
                if (tools) Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(s.failNext, model::failNext, enabled = !s.busy)
                    Text("다음 요청 실패 체험")
                }
            }
        }
    }
    details?.let { title -> AlertDialog(onDismissRequest = { details = null },
        title = { Text(title) }, text = { Text("원문과 게시 버전이 준비 중입니다. 이 화면의 선택은 서버에 전송되지 않으며 실제 동의로 저장되지 않습니다.") },
        confirmButton = { TextButton(onClick = { details = null }) { Text("확인") } }) }
    if (exitConfirm) AlertDialog(onDismissRequest = { exitConfirm = false },
        title = { Text("가입 체험을 나갈까요?") }, text = { Text("작성 중인 가입 정보는 지워집니다.") },
        confirmButton = { TextButton(onClick = { model.reset(); onExit() }) { Text("나가기") } },
        dismissButton = { TextButton(onClick = { exitConfirm = false }) { Text("계속 작성") } })
}
