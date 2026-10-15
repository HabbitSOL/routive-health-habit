package com.example.sol.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.sol.feature.home.viewmodel.HomeUiState
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(state: HomeUiState, onOpen: (String) -> Unit, onReload: () -> Unit,
    onLoading: () -> Unit, onError: () -> Unit, onExit: () -> Unit) {
    var toolsExpanded by rememberSaveable { mutableStateOf(false) }
    var showAd by rememberSaveable { mutableStateOf(true) }
    val today = LocalDate.now()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(today.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("오늘도, 나를 위한 한 걸음", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val start = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            repeat(7) { offset ->
                val date = start.plusDays(offset.toLong())
                val selected = date == today
                Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    Text(listOf("일", "월", "화", "수", "목", "금", "토")[offset], color = color,
                        style = MaterialTheme.typography.labelMedium)
                    Text(date.dayOfMonth.toString(), color = color,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        when (state) {
            HomeUiState.Loading -> SolCard("오늘의 정보를 불러오는 중") {
                CircularProgressIndicator(); SolButton("미리보기 끝내기", onReload)
            }
            HomeUiState.Error -> SolCard("잠시 연결이 어려워요") {
                Text("오류 화면 예시입니다. 다시 시도해 주세요.")
                SolButton("다시 시도", onReload)
            }
            is HomeUiState.Ready -> {
                SolCard("오늘의 기록", outlined = true) {
                    Text("데모 · 아직 기록이 없어요", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("습관", "식사", "복약 예정").forEach { label ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                                Text("—", style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }
                }
                Text("지금 챙길 일", style = MaterialTheme.typography.titleLarge)
                Card(onClick = { onOpen("habit") }, shape = RoundedCornerShape(24.dp)) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 156.dp)) {
                        MorningArt(Modifier.matchParentSize())
                        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
                            listOf(Pine, Pine.copy(alpha = .45f)))))
                        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("나만의 습관", color = Color.White, style = MaterialTheme.typography.titleLarge)
                            Text("작은 실천부터 시작해요", color = Color.White)
                            Text("습관 화면 보기 →", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActivityTile("식사 기록", "오늘의 식사를\n간편하게 기록해요", Peach,
                        { onOpen("meal") }, Modifier.weight(1f))
                    ActivityTile("복약 일정", "잊지 않고\n챙겨보세요", Lavender,
                        { onOpen("medication") }, Modifier.weight(1f))
                }
            }
        }
        TextButton(onClick = { onOpen("drugdictionary") }) { Text("의약품 사전 둘러보기 →") }
        if (showAd) AdPreview()
        TextButton(onClick = { toolsExpanded = !toolsExpanded }) {
            Text(if (toolsExpanded) "데모 도구 닫기 −" else "데모 도구 +")
        }
        if (toolsExpanded) {
            SolCard("화면 상태 미리보기") {
                TextButton(onClick = onLoading) { Text("불러오는 중 보기") }
                TextButton(onClick = onError) { Text("오류 · 재시도 보기") }
                TextButton(onClick = { showAd = !showAd }) { Text(if (showAd) "광고 예시 숨기기" else "광고 예시 보기") }
                TextButton(onClick = onExit) { Text("데모 종료 · 로그인으로") }
            }
        }
    }
}
