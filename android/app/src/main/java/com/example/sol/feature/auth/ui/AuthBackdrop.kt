package com.example.sol.feature.auth.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke

/** Original subtle botanical pattern; decorative and excluded from accessibility semantics. */
@Composable
fun AuthBackdrop(content: @Composable BoxScope.() -> Unit) {
    val ink = MaterialTheme.colorScheme.primary.copy(alpha = .045f)
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            drawArc(ink, 130f, 190f, false, Offset(w * .55f, h * .45f),
                Size(w * .8f, h * .75f), style = Stroke(5f))
            for (i in 0..5) {
                drawOval(ink, Offset(w * (.74f + (i % 2) * .10f), h * (.48f + i * .07f)),
                    Size(w * .15f, h * .025f))
            }
            drawCircle(ink, w * .15f, Offset(w * .02f, h * .15f), style = Stroke(3f))
        }
        content()
    }
}
