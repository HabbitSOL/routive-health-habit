package com.example.sol.feature.home.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.sol.core.ui.*

@Composable
fun HomeDetailCard(title: String, caption: String, kind: String, tint: Color,
    action: String, onOpen: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))) {
        Column {
            Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(tint, tint.copy(alpha = .3f))))
                .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(caption, style = MaterialTheme.typography.labelSmall, color = Pine)
                    Text(title, style = MaterialTheme.typography.titleLarge, color = Ink)
                }
                WellnessDrawing(kind, Modifier.size(76.dp))
            }
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                content()
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
                TextButton(onClick = onOpen, modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)) {
                    Text(action, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    Surface(shape = CircleShape, color = tint, contentColor = Pine) {
                        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { Text("↗") }
                    }
                }
            }
        }
    }
}

/** Small original vector illustrations: no external assets or medical instructions. */
@Composable
fun WellnessDrawing(kind: String, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension / 80f
        drawCircle(Color.White.copy(alpha = .65f), 34*u, Offset(40*u, 40*u))
        drawOval(Pine.copy(alpha = .08f), Offset(13*u, 62*u), Size(55*u, 7*u))
        if (kind == "meal") {
            drawCircle(Color.White, 25*u, Offset(40*u, 40*u))
            drawCircle(Color(0xFFE6CDB9), 24*u, Offset(40*u, 40*u), style = Stroke(1.5f*u))
            drawCircle(Color(0xFFF8F1E7), 18*u, Offset(40*u, 40*u))
            drawOval(Color(0xFF74A486), Offset(23*u, 27*u), Size(20*u, 13*u))
            drawOval(Pine, Offset(28*u, 35*u), Size(16*u, 11*u))
            drawCircle(Color(0xFFDC8A66), 7*u, Offset(48*u, 34*u))
            drawCircle(Color(0xFFF0BB72), 6*u, Offset(47*u, 48*u))
            drawLine(Pine, Offset(9*u, 24*u), Offset(9*u, 57*u), 2*u)
            drawLine(Pine, Offset(69*u, 24*u), Offset(69*u, 57*u), 2*u)
            drawRoundRect(Pine, Offset(66*u, 21*u), Size(6*u, 17*u), CornerRadius(3*u))
        } else {
            rotate(-28f, Offset(35*u, 40*u)) {
                drawRoundRect(Color.White, Offset(23*u, 17*u), Size(24*u, 44*u), CornerRadius(12*u))
                drawRoundRect(Color(0xFF9786C7), Offset(23*u, 17*u), Size(24*u, 25*u), CornerRadius(12*u))
                drawRect(Color(0xFF9786C7), Offset(23*u, 30*u), Size(24*u, 10*u))
                drawLine(Color.White.copy(alpha = .65f), Offset(29*u, 25*u), Offset(29*u, 32*u), 3*u)
            }
            drawCircle(Color.White, 15*u, Offset(58*u, 55*u))
            drawCircle(Pine, 13*u, Offset(58*u, 55*u), style = Stroke(1.5f*u))
            drawLine(Pine, Offset(58*u, 47*u), Offset(58*u, 55*u), 2*u)
            drawLine(Pine, Offset(58*u, 55*u), Offset(64*u, 58*u), 2*u)
        }
    }
}
