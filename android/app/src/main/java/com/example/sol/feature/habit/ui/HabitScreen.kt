package com.example.sol.feature.habit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sol.core.ui.*
import com.example.sol.R

@Composable
fun HabitScreen(onBack: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf("전체") }
    var completed by rememberSaveable { mutableStateOf(listOf(0)) }
    var showAddInfo by rememberSaveable { mutableStateOf(false) }
    val names = listOf("아침 스트레칭", "물 마시기", "저녁 산책")
    val subtitles = listOf("가볍게 몸을 깨우는 시간", "내가 정한 목표에 맞춰", "하루를 천천히 마무리해요")
    val colors = listOf(Sage, Peach, Lavender)
    val artwork = listOf(R.drawable.habit_stretch, R.drawable.habit_water, R.drawable.habit_walk)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TextButton(onClick = onBack) { Text("← 오늘로") }
        Text("나만의 습관", style = MaterialTheme.typography.headlineLarge)
        Text("오늘의 작은 실천을 모아봐요", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("예시 습관 · 변경은 이 화면의 데모에만 반영돼요",
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("전체", "진행 중", "완료").forEach { label ->
                FilterChip(selected = filter == label, onClick = { filter = label }, label = { Text(label) })
            }
        }
        val visible = names.indices.filter { filter == "전체" || (it in completed) == (filter == "완료") }
        if (visible.isEmpty()) SolCard("아직 해당하는 습관이 없어요") {
            Text("다른 필터에서 예시 습관을 확인해 보세요.")
        }
        visible.forEach { index ->
            Card(shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors[index], contentColor = Ink)) {
                Box(Modifier.fillMaxWidth().heightIn(min = 170.dp)) {
                    Image(painter = painterResource(artwork[index]), contentDescription = null,
                        contentScale = ContentScale.Fit, alignment = Alignment.CenterEnd,
                        modifier = Modifier.matchParentSize())
                    Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
                        listOf(colors[index], colors[index].copy(alpha = .93f), colors[index].copy(alpha = .25f)))))
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(names[index], style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(subtitles[index], style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(onClick = {
                            completed = if (index in completed) completed - index else completed + index
                        }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Pine)) {
                            Text(if (index in completed) "✓ 완료 · 되돌리기" else "기록하기 →")
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = { showAddInfo = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(18.dp)) { Text("＋ 습관 추가") }
    }
    if (showAddInfo) AlertDialog(onDismissRequest = { showAddInfo = false },
        title = { Text("나만의 습관 추가") },
        text = { Text("지금은 디자인을 체험하는 화면이에요. 직접 습관을 만들고 저장하는 기능은 이후 제공할 예정입니다.") },
        confirmButton = { TextButton(onClick = { showAddInfo = false }) { Text("확인") } })
}
