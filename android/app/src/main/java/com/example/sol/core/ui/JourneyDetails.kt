package com.example.sol.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Uses SOL's original artwork with a readable, separate text area. */
@Composable
fun JourneyBanner(eyebrow: String, title: String, subtitle: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Pine)) {
        MorningArt(Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            listOf(Pine, Pine.copy(alpha = .88f), Pine.copy(alpha = .20f)))))
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(eyebrow, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .8f))
            Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(subtitle, modifier = Modifier.fillMaxWidth(.75f),
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .9f))
        }
    }
}

@Composable
fun JourneySteps(current: Int) {
    val labels = listOf("계정", "동의", "인증", "로그인", "프로필")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        labels.forEachIndexed { index, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(
                    if (index <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center) {
                    Text(if (index < current) "✓" else "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (index <= current) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = if (index == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
