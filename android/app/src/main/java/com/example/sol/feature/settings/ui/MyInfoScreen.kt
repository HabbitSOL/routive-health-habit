package com.example.sol.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sol.core.ui.*
import com.example.sol.feature.auth.viewmodel.consentItems
import com.example.sol.feature.settings.viewmodel.DemoAccountViewModel

@Composable
fun MyInfoScreen(account: DemoAccountViewModel, onEdit: () -> Unit, onLogout: () -> Unit,
    onOpen: (String) -> Unit) {
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    val profile = account.profile
    val name = profile?.nickname ?: "반가워요"
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MY WELLNESS SPACE", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text("내 정보", style = MaterialTheme.typography.headlineLarge)
                }
                FilledTonalIconButton(onClick = onEdit) { SolNavIcon("settings", MaterialTheme.colorScheme.primary) }
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.surface)))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f), RoundedCornerShape(28.dp))
                .padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(80.dp).border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(6.dp).clip(CircleShape).background(Pine), contentAlignment = Alignment.Center) {
                    Text(profile?.nickname?.let { String(Character.toChars(it.codePointAt(0))) } ?: "S",
                        color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.headlineLarge)
                }
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(account.email.ifEmpty { "계정 없이 둘러보는 중" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = .1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("나만의 건강 프로필", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onEdit) { Text(if (profile == null) "설정하기  ↗" else "수정하기  ↗") }
            }
            }
            JourneyBanner("A MOMENT FOR YOURSELF", "나의 속도로, 한 걸음씩", "완벽한 하루보다 나를 돌보는 작은 습관을 쌓아요.")
        }
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("데모 프로필 · 앱을 종료하면 초기화될 수 있어요", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            SolCard("나의 건강 정보", outlined = true) {
                InfoLine("생년월일", profile?.birthDate?.ifBlank { null } ?: "미입력")
                InfoLine("성별", when (profile?.sex) {
                    "MALE" -> "남성"; "FEMALE" -> "여성"; "OTHER" -> "기타"
                    "UNSPECIFIED" -> "응답하지 않음"; else -> "미입력"
                })
                InfoLine("키", profile?.heightCm?.ifBlank { null }?.let { "$it cm" } ?: "미입력")
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("나의 기록", style = MaterialTheme.typography.titleLarge)
                Text("오늘의 작은 실천을 모아보세요.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RecordShortcut("습관", "작은 실천", Sage, Modifier.weight(1f)) { onOpen("habit") }
                    RecordShortcut("식사", "하루의 식사", Peach, Modifier.weight(1f)) { onOpen("meal") }
                    RecordShortcut("복약", "챙기는 건강", Lavender, Modifier.weight(1f)) { onOpen("medication") }
                }
            }
            SolCard("계정과 개인정보", outlined = true) {
                MenuRow("가입 시 선택한 동의", { dialog = "consents" })
                MenuRow("알림 설정", { dialog = "notifications" })
                MenuRow("계정 관리 안내", { dialog = "account" })
            }
            OutlinedButton(onClick = { dialog = "logout" }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)) { Text("데모 종료 · 로그아웃") }
            Text("SOL · 나를 돌보는 작은 습관", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        }
    }
    dialog?.let { kind ->
        AlertDialog(onDismissRequest = { dialog = null }, title = { Text(when(kind) {
            "consents" -> "동의 선택 내역 · 데모"
            "notifications" -> "알림 설정"
            "logout" -> "데모를 종료할까요?"
            else -> "계정 관리"
        }) }, text = {
            if (kind == "consents") Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("가입 체험에서 선택한 내역이며 실제 법적 동의 이력이 아닙니다.")
                consentItems.forEach { Text("${it.title}: ${if (it.type in account.consents) "선택함" else "선택 안 함"}") }
            } else Text(when(kind) {
                "notifications" -> "현재 알림 권한을 요청하거나 설정을 변경하지 않습니다. 알림 기능 구현 시 연결됩니다."
                "logout" -> "이 기기의 데모 프로필과 가입 입력을 지우고 로그인 화면으로 돌아갑니다."
                else -> "실제 계정은 생성되지 않았습니다. 비밀번호 재설정·회원 탈퇴는 서버 연결 후 제공됩니다."
            })
        }, confirmButton = { TextButton(onClick = {
            dialog = null
            if (kind == "logout") onLogout()
        }) { Text(if (kind == "logout") "종료" else "확인") } },
            dismissButton = { if (kind == "logout") TextButton(onClick = { dialog = null }) { Text("취소") } })
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1f), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(label, Modifier.weight(1f))
        Text("›")
    }
}

@Composable
private fun RecordShortcut(title: String, caption: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(22.dp), color = tint,
        contentColor = Ink) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = .7f)),
                contentAlignment = Alignment.Center) {
                Text(when(title) { "습관" -> "✓"; "식사" -> "◒"; else -> "+" },
                    style = MaterialTheme.typography.titleLarge, color = Pine)
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(caption, style = MaterialTheme.typography.labelSmall)
            Text("바로가기 ›", style = MaterialTheme.typography.labelSmall, color = Pine)
        }
    }
}
