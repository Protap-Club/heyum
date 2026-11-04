package com.pgstorm148.heyum.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private val ReadyGreen = Color(0xFF2E9E5B)
private val SparkColors = listOf(Color(0xFFFFD23F), Color(0xFFFF6B35), Color(0xFF7ED957), Color(0xFFFF5DA2))

/**
 * Plays once when a phone number becomes complete: a green disc pops in with a spring, its
 * check mark draws itself, and a ring of sparks bursts outward and fades.
 */
@Composable
fun PhoneReadyBadge(modifier: Modifier = Modifier) {
    val pop = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    LaunchedEffect(Unit) {
        check.animateTo(1f, tween(320, delayMillis = 120))
    }
    LaunchedEffect(Unit) {
        burst.animateTo(1f, tween(650))
    }

    Box(modifier.size(40.dp)) {
        Canvas(Modifier.size(40.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val base = size.minDimension * 0.28f

            // Sparks: 8 dots flying outward from the disc, shrinking and fading.
            val b = burst.value
            if (b > 0f && b < 1f) {
                for (i in 0 until 8) {
                    val a = i * 6.2831855f / 8f
                    val dist = base + b * size.minDimension * 0.22f
                    drawCircle(
                        SparkColors[i % SparkColors.size].copy(alpha = 1f - b),
                        radius = 2.8f * (1f - b) + 0.6f,
                        center = Offset(c.x + cos(a) * dist, c.y + sin(a) * dist)
                    )
                }
            }

            drawCircle(ReadyGreen, radius = base * pop.value, center = c)

            // Check mark, drawn progressively along its two strokes.
            val p1 = Offset(c.x - base * 0.42f, c.y + base * 0.02f)
            val p2 = Offset(c.x - base * 0.10f, c.y + base * 0.34f)
            val p3 = Offset(c.x + base * 0.46f, c.y - base * 0.30f)
            val t = check.value
            val path = Path().apply {
                moveTo(p1.x, p1.y)
                if (t <= 0.4f) {
                    val k = t / 0.4f
                    lineTo(p1.x + (p2.x - p1.x) * k, p1.y + (p2.y - p1.y) * k)
                } else {
                    lineTo(p2.x, p2.y)
                    val k = (t - 0.4f) / 0.6f
                    lineTo(p2.x + (p3.x - p2.x) * k, p2.y + (p3.y - p2.y) * k)
                }
            }
            if (t > 0f) {
                drawPath(path, Color.White, style = Stroke(width = base * 0.2f, cap = StrokeCap.Round))
            }
        }
    }
}
