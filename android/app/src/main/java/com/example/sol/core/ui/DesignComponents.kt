package com.example.sol.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sol.R

val Pine = Color(0xFF205C46)
val Ink = Color(0xFF17251E)
val Sage = Color(0xFFE7EEDC)
val Peach = Color(0xFFFBE6D6)
val Lavender = Color(0xFFEAE4F8)

@Composable
fun MorningArt(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.sol_morning), contentDescription = null,
        contentScale = ContentScale.Crop, modifier = modifier)
}

@Composable
fun SolNavIcon(kind: String, color: Color) {
    Canvas(Modifier.size(24.dp)) {
        val u = size.width / 24f
        val stroke = Stroke(1.7f * u)
        when (kind) {
            "home" -> {
                val path = Path().apply {
                    moveTo(3*u, 10*u); lineTo(12*u, 3*u); lineTo(21*u, 10*u)
                    lineTo(21*u, 21*u); lineTo(15*u, 21*u); lineTo(15*u, 14*u)
                    lineTo(9*u, 14*u); lineTo(9*u, 21*u); lineTo(3*u, 21*u); close()
                }
                drawPath(path, color, style = stroke)
            }
            "habit" -> {
                drawRoundRect(color, Offset(5*u, 2*u), Size(14*u, 20*u),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3*u), style = stroke)
                for (y in listOf(8, 12, 16)) drawLine(color, Offset(8*u,y*u), Offset(16*u,y*u), 1.5f*u)
            }
            "statistics" -> listOf(12f, 4f, 8f).forEachIndexed { i, top ->
                drawLine(color, Offset((5+i*7)*u,top*u), Offset((5+i*7)*u,21*u), 3*u)
            }
            else -> {
                drawCircle(color, 4*u, Offset(12*u,7*u), style = stroke)
                drawArc(color, 180f, 180f, false, Offset(4*u,14*u), Size(16*u,14*u), style = stroke)
            }
        }
    }
}

@Composable
fun SolBottomBar(current: String, onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        listOf("home" to "오늘", "habit" to "기록", "statistics" to "통계", "settings" to "내 정보")
            .forEach { (route, label) ->
                NavigationBarItem(selected = current == route, onClick = { onNavigate(route) },
                    icon = { SolNavIcon(route, if (current == route) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant) }, label = { Text(label) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.secondaryContainer))
            }
    }
}

@Composable
fun ActivityTile(title: String, subtitle: String, color: Color, onClick: () -> Unit,
    modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = color, contentColor = Ink)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (title.contains("식사")) "◒" else "◷", style = MaterialTheme.typography.headlineMedium)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            Text("보기 →", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AdPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Text("광고 · 예시", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MorningArt(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)))
                Column(Modifier.weight(1f)) {
                    Text("일상에 작은 휴식을", fontWeight = FontWeight.SemiBold)
                    Text("광고 영역 미리보기", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
