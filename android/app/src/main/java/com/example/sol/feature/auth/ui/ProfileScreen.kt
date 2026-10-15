package com.example.sol.feature.auth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sol.core.ui.*
import com.example.sol.feature.auth.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(model: ProfileViewModel, onBack: () -> Unit,
    editing: Boolean = false, onSave: (() -> Unit)? = null) {
    val state = model.state
    var showOptional by rememberSaveable { mutableStateOf(false) }
    AuthBackdrop {
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TextButton(onClick = onBack) { Text("← 이전으로") }
        JourneyBanner("MY SOL", "나를 알아가는 시간", "건강한 일상의 주인공은 바로 나예요.")
        if (!editing) JourneySteps(4)
        if (!editing) Text("마지막 단계 · 나의 프로필", color = MaterialTheme.colorScheme.primary)
        Text("나를 어떻게\n불러드릴까요?", style = MaterialTheme.typography.headlineLarge)
        Text("닉네임 하나로 시작해요.\n나머지 정보는 나중에 입력해도 괜찮아요.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        SolCard("나의 이름", outlined = true) {
            SolTextField(state.nickname, model::nickname, "닉네임 · 필수", state.errors["nickname"],
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))
            Text("최대 30자 · 앞뒤 공백은 자동으로 정리해요.", style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick = { showOptional = !showOptional }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showOptional) "선택 정보 접기 −" else "선택 정보 입력하기 ＋")
        }
        if (showOptional) {
            SolCard("선택 정보", outlined = true) {
                Text("입력하지 않아도 계속할 수 있어요.", style = MaterialTheme.typography.bodyMedium)
                SolTextField(state.birthDate, model::birthDate, "생년월일 · YYYY-MM-DD",
                    state.errors["birthDate"], keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
                Text("성별", style = MaterialTheme.typography.titleMedium)
                listOf("MALE" to "남성", "FEMALE" to "여성", "OTHER" to "기타",
                    "UNSPECIFIED" to "응답하지 않음").chunked(2).forEach { options ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        options.forEach { (value, label) ->
                            FilterChip(selected = state.sex == value,
                                onClick = { model.sex(if (state.sex == value) null else value) },
                                label = { Text(label) })
                        }
                    }
                }
                SolTextField(state.heightCm, model::height, "키 · cm", state.errors["heightCm"],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        } else if (state.errors.keys.any { it != "nickname" }) {
            Text("선택 정보를 펼쳐 입력 내용을 확인해 주세요.", color = MaterialTheme.colorScheme.error)
        }
        SolButton(if (editing) "변경 반영 · 데모" else "프로필 설정하고 시작하기 · 데모", {
            model.validate()
            if (model.state.errors.keys.any { it != "nickname" }) showOptional = true
            if (model.state.errors.isEmpty() && onSave != null) {
                model.dismissPreview()
                onSave()
            }
        })
        Text("프로필 화면 미리보기입니다. 입력은 서버에 저장되지 않으며 실제 개인정보 대신 예시를 사용해 주세요.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    }
    if (state.previewComplete) AlertDialog(onDismissRequest = model::dismissPreview,
        title = { Text("입력 내용을 확인했어요") },
        text = { Text("프로필 입력 형식이 올바릅니다. 실제 저장과 초기 설정 완료는 서버 연결 후 진행됩니다.") },
        confirmButton = { TextButton(onClick = model::dismissPreview) { Text("확인") } })
}
