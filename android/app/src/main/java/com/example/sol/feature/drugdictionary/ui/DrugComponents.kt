package com.example.sol.feature.drugdictionary.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.sol.core.ui.SolButton

internal val DrugMint = Color(0xFFE6F4EE)
internal val DrugInk = Color(0xFF205C46)

@Composable
internal fun DrugArtwork(modifier: Modifier = Modifier) {
    Canvas(modifier.size(64.dp)) {
        drawCircle(Color(0xFFD4EADF))
        rotate(-35f) {
            drawRoundRect(Color.White, Offset(size.width * .18f, size.height * .32f),
                Size(size.width * .64f, size.height * .36f), CornerRadius(size.height * .18f))
            drawRoundRect(DrugInk, Offset(size.width * .18f, size.height * .32f),
                Size(size.width * .32f, size.height * .36f), CornerRadius(size.height * .12f))
            drawLine(Color(0xFF95C4AE), Offset(size.width * .5f, size.height * .34f),
                Offset(size.width * .5f, size.height * .66f), 2.dp.toPx())
        }
    }
}

@Composable
internal fun DrugHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) { Text("‹ 뒤로") }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        Surface(color = DrugMint, shape = RoundedCornerShape(12.dp)) {
            Text("DEMO", color = DrugInk, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(10.dp))
        }
    }
}

@Composable
internal fun DrugPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, Color(0xFFE3E9E5))) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
internal fun DrugMessage(title: String, message: String, action: String? = null, onAction: () -> Unit = {}) {
    DrugPanel(title) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        if (action != null) SolButton(action, onAction)
    }
}

@Composable
internal fun DrugLoading(message: String) {
    Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        Text(message)
    }
}

/** Shared editorial banner. Decoration stays behind content and scales with text. */
@Composable
internal fun DrugGradientBanner(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = Color.Transparent, contentColor = Color.White) {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(
            Color(0xFF123F3C), Color(0xFF236C61), Color(0xFF35657B))))) {
            Canvas(Modifier.matchParentSize()) {
                val center = Offset(size.width * .94f, size.height * .12f)
                drawCircle(Color.White.copy(alpha = .06f), size.width * .37f, center)
                drawCircle(Color.White.copy(alpha = .13f), size.width * .32f, center, style = Stroke(1.dp.toPx()))
                drawCircle(Color.White.copy(alpha = .08f), size.width * .44f, center, style = Stroke(1.dp.toPx()))
            }
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }
    }
}

@Composable
fun DrugDiscoveryCard(onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(28.dp), color = Color.Transparent) {
        DrugGradientBanner {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("의약품 사전", color = Color(0xFFC1EAD9), style = MaterialTheme.typography.labelLarge)
                    Text("궁금한 약,\n여기서 찾아보세요", color = Color.White, style = MaterialTheme.typography.titleLarge)
                }
                DrugArtwork()
            }
            Text("이름으로 찾고, 상세 정보까지 한눈에", color = Color.White.copy(alpha = .9f),
                style = MaterialTheme.typography.bodyMedium)
            Surface(color = Color.White, contentColor = DrugInk, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("의약품 검색하기", style = MaterialTheme.typography.labelLarge)
                    Text("→")
                }
            }
        }
    }
}
