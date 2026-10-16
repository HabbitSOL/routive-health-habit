package com.example.sol.feature.habit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sol.core.ui.*
import com.example.sol.R

@Composable
fun HabitScreen(completed: Set<Int>, onToggle: (Int) -> Unit, onBack: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf("전체") }
    var showAddInfo by rememberSaveable { mutableStateOf(false) }
    val names = listOf("아침 스트레칭", "물 마시기", "저녁 산책")
    val subtitles = listOf("가볍게 몸을 깨우는 시간", "내가 정한 목표에 맞춰", "하루를 천천히 마무리해요")
    val colors = listOf(Sage, Peach, Lavender)
    val artwork = listOf(R.drawable.habit_stretch, R.drawable.habit_water, R.drawable.habit_walk)
    val periods = listOf("아침 루틴", "일상 루틴", "저녁 루틴")
    val progress by animateFloatAsState(completed.size / names.size.toFloat(), label = "habitProgress")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TextButton(onClick = onBack) { Text("← 오늘로") }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("MY DAILY ROUTINE", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                Text("나만의 습관", style = MaterialTheme.typography.headlineLarge)
                Text("오늘의 작은 실천을 모아봐요", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    SolNavIcon("habit", MaterialTheme.colorScheme.primary)
                }
            }
        }
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Box(Modifier.size(76.dp).semantics {
                        contentDescription = "예시 습관 ${names.size}개 중 ${completed.size}개 완료"
                    }, contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.secondaryContainer, strokeWidth = 6.dp)
                        Text("${completed.size}/${names.size}", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("오늘의 실천 · 데모", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Text(if (completed.size == names.size) "모두 해냈어요!" else "차근차근, 나의 속도로",
                            style = MaterialTheme.typography.titleMedium)
                        Text(if (completed.size == names.size) "작은 실천으로 채운 하루예요." else "${names.size - completed.size}개의 작은 실천이 남았어요.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))
                Text("예시 습관을 눌러 기록을 체험해 보세요.\n완료 상태는 홈에도 반영돼요. 실제 저장은 하지 않아요.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("전체", "진행 중", "완료").forEach { label ->
                val count = when(label) { "완료" -> completed.size; "진행 중" -> names.size - completed.size; else -> names.size }
                FilterChip(selected = filter == label, onClick = { filter = label },
                    shape = RoundedCornerShape(50), label = { Text("$label  $count") })
            }
        }
        val visible = names.indices.filter { filter == "전체" || (it in completed) == (filter == "완료") }
        if (visible.isEmpty()) SolCard("아직 해당하는 습관이 없어요") {
            Text("다른 필터에서 예시 습관을 확인해 보세요.")
        }
        visible.forEach { index ->
            val isDone = index in completed
            Card(shape = RoundedCornerShape(24.dp),
                border = if (isDone) BorderStroke(1.dp, Pine.copy(alpha = .4f)) else null,
                colors = CardDefaults.cardColors(containerColor = colors[index], contentColor = Ink)) {
                Box(Modifier.fillMaxWidth().heightIn(min = 200.dp)) {
                    Image(painter = painterResource(artwork[index]), contentDescription = null,
                        contentScale = ContentScale.Fit, alignment = Alignment.CenterEnd,
                        modifier = Modifier.matchParentSize())
                    Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
                        listOf(colors[index], colors[index].copy(alpha = .93f), colors[index].copy(alpha = .25f)))))
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = .78f), contentColor = Pine) {
                                Text(periods[index], Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall)
                            }
                            Surface(shape = CircleShape, color = if (isDone) Pine else Color.White.copy(alpha = .8f),
                                contentColor = if (isDone) Color.White else Pine) {
                                Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                                    Text(if (isDone) "✓" else "${index + 1}", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        Text(names[index], style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(subtitles[index], style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = {
                                onToggle(index)
                            }, shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDone) Color.White.copy(alpha = .9f) else Pine,
                                    contentColor = if (isDone) Pine else Color.White)) {
                                Text(if (isDone) "✓ 완료 취소" else "실천 기록하기  →")
                            }
                            if (isDone) Text("잘했어요!", style = MaterialTheme.typography.labelSmall, color = Pine)
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = { showAddInfo = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(18.dp)) { Text("＋ 습관 추가") }
        Text("작은 실천 하나도 소중한 기록이에요.", modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
    }
    if (showAddInfo) AlertDialog(onDismissRequest = { showAddInfo = false },
        title = { Text("나만의 습관 추가") },
        text = { Text("지금은 디자인을 체험하는 화면이에요. 직접 습관을 만들고 저장하는 기능은 이후 제공할 예정입니다.") },
        confirmButton = { TextButton(onClick = { showAddInfo = false }) { Text("확인") } })
}
