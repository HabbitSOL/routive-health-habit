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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sol.core.ui.*
import com.example.sol.feature.home.data.*
import com.example.sol.feature.home.viewmodel.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: HomeUiState, mode: DemoMode, nickname: String?, onOpen: (String) -> Unit,
    onReload: () -> Unit, onRetry: (HomeSection) -> Unit, onMode: (DemoMode) -> Unit, onExit: () -> Unit) {
    var toolsExpanded by rememberSaveable { mutableStateOf(false) }
    var showAd by rememberSaveable { mutableStateOf(true) }
    var quickRecord by rememberSaveable { mutableStateOf(false) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    val owner = LocalLifecycleOwner.current
    // Refresh the display date on return and across midnight. This is not a server timezone contract.
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) today = LocalDate.now()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { while (true) { delay(30_000); today = LocalDate.now() } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(today.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(nickname?.takeIf { it.isNotBlank() }?.let { "${it}님, 반가워요" } ?: "오늘도, 나를 위한 한 걸음",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onReload) { Text("새로고침") }
        }
        WeekStrip(today)
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(if (mode == DemoMode.EMPTY) "데모 · 기록을 시작하기 전 화면이에요" else "데모 · 아래 정보는 실제 기록이 아닌 예시예요",
                Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.labelMedium)
        }
        SolCard("오늘의 습관", outlined = true) {
            SectionContent(state.habits, "아직 등록한 습관이 없어요", "작게 시작할 습관을 골라보세요.",
                { onRetry(HomeSection.HABITS) }) { value ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${value.completed} / ${value.total}", style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary)
                    Text("실천 완료 · 예시", style = MaterialTheme.typography.labelMedium)
                }
                LinearProgressIndicator(progress = { if (value.total == 0) 0f else (value.completed.toFloat() / value.total).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)))
                Text(if (value.completed == value.total) "오늘의 예시 습관을 모두 완료했어요!" else "남은 습관도 나의 속도로 실천해요.",
                    style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = { onOpen("habit") }) { Text("습관 확인하고 기록하기 →") }
        }
        Button(onClick = { quickRecord = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(18.dp)) { Text("＋ 빠른 기록", style = MaterialTheme.typography.titleMedium) }
        Text("지금 챙길 일", style = MaterialTheme.typography.titleLarge)
        Card(onClick = { onOpen("habit") }, shape = RoundedCornerShape(24.dp)) {
            Box(Modifier.fillMaxWidth().heightIn(min = 150.dp)) {
                MorningArt(Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Pine, Pine.copy(alpha = .45f)))))
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("나만의 습관", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text("작은 실천부터 시작해요", color = Color.White)
                    Text("습관 화면 보기 →", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        HomeDetailCard("최근 식사", "NOURISH YOUR DAY · 식사", "meal", Peach,
            "식사 기록 살펴보기", { onOpen("meal") }) {
            SectionContent(state.meal, "아직 기록한 식사가 없어요", "오늘 먹은 식사부터 남겨보세요.",
                { onRetry(HomeSection.MEAL) }) { value ->
                InfoBadge(value.timeLabel, Peach)
                Text(value.title, style = MaterialTheme.typography.titleMedium)
                Text(value.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
        HomeDetailCard("다음 복약 일정", "A LITTLE CARE · 복약", "medication", Lavender,
            "복약 일정 살펴보기", { onOpen("medication") }) {
            SectionContent(state.medication, "등록한 복약 일정이 없어요", "복약 화면에서 일정을 확인할 수 있어요.",
                { onRetry(HomeSection.MEDICATION) }) { value ->
                InfoBadge(value.scheduledLabel, Lavender)
                Text(value.title, style = MaterialTheme.typography.titleMedium)
                Text(value.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
        com.example.sol.feature.drugdictionary.ui.DrugDiscoveryCard { onOpen("drugdictionary") }
        if (showAd) AdPreview()
        TextButton(onClick = { toolsExpanded = !toolsExpanded }) { Text(if (toolsExpanded) "데모 도구 닫기 −" else "데모 도구 +") }
        if (toolsExpanded) SolCard("화면 상태 체험") {
            Text("예시 모드를 바꾸면 습관 완료 상태도 초기화돼요. 실제 데이터는 저장하지 않아요.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { onMode(DemoMode.EMPTY) }) { Text("기록 없음 보기") }
            TextButton(onClick = { onMode(DemoMode.SAMPLE) }) { Text("예시 데이터 보기") }
            TextButton(onClick = { onMode(DemoMode.PARTIAL_ERROR) }) { Text("식사만 오류 보기") }
            TextButton(onClick = onReload) { Text("다시 불러오기 · 로딩 체험") }
            TextButton(onClick = { showAd = !showAd }) { Text(if (showAd) "광고 예시 숨기기" else "광고 예시 보기") }
            TextButton(onClick = onExit) { Text("데모 종료 · 로그인으로") }
        }
    }
    if (quickRecord) ModalBottomSheet(onDismissRequest = { quickRecord = false }) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("무엇을 기록할까요?", style = MaterialTheme.typography.headlineSmall)
            Text("습관은 기록 체험, 식사·복약은 준비 중인 안내 화면으로 이동해요.", style = MaterialTheme.typography.bodyMedium)
            listOf("habit" to "습관 실천 기록", "meal" to "식사 기록 · 준비 중", "medication" to "복약 기록 · 준비 중").forEach { (route, title) ->
                OutlinedButton(onClick = { quickRecord = false; onOpen(route) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text(title, Modifier.weight(1f)); Text("→")
                }
            }
        }
    }
}

@Composable
private fun <T> ColumnScope.SectionContent(state: SectionState<T>, emptyTitle: String, emptyHint: String,
    onRetry: () -> Unit, ready: @Composable ColumnScope.(T) -> Unit) {
    when (state) {
        SectionState.Loading -> {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("정보를 불러오고 있어요", style = MaterialTheme.typography.bodyMedium)
        }
        SectionState.Empty -> {
            Text(emptyTitle, style = MaterialTheme.typography.titleMedium)
            Text(emptyHint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SectionState.Error -> {
            Surface(shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = .35f)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("잠시 연결을 쉬고 있어요", style = MaterialTheme.typography.titleMedium)
                    Text("정보를 불러오지 못했어요.\n다른 카드는 계속 이용할 수 있어요.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(12.dp)) { Text("↻  이 항목만 다시 시도") }
                }
            }
        }
        is SectionState.Ready -> ready(state.value)
    }
}

@Composable
private fun InfoBadge(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = color, contentColor = Ink) {
        Text(text, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun WeekStrip(today: LocalDate) {
    val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        repeat(7) { offset ->
            val date = start.plusDays(offset.toLong())
            val selected = date == today
            val color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent).padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(listOf("일", "월", "화", "수", "목", "금", "토")[offset], color = color, style = MaterialTheme.typography.labelMedium)
                Text(date.dayOfMonth.toString(), color = color, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

