package com.pgstorm148.heyum.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.pgstorm148.heyum.model.AmbienceConfig
import com.pgstorm148.heyum.model.AmbienceEffect
import com.pgstorm148.heyum.model.AmbienceIntensity
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val TWO_PI: Float = 6.2831855f

/**
 * A purely decorative, NON-INTERACTIVE ambience layer for the decision screen. No gesture
 * modifiers, so touches always pass through to the card below. Renders nothing when the resolved
 * effect is NONE (the default and the fallback for any bad/disabled config).
 *
 * The effect plays for [AmbienceConfig.durationSeconds] each time the screen opens, then fades out.
 * Set durationSeconds to 0 to make it persist. It replays whenever the screen is reopened or the
 * server config changes.
 */
@Composable
fun AmbienceOverlay(config: AmbienceConfig, modifier: Modifier = Modifier) {
    val effect = remember(config) { config.resolveEffect() }
    if (effect == AmbienceEffect.NONE) return

    // Auto-stop: play for durationSeconds, fade out, then stop compositing entirely.
    var visible by remember(config) { mutableStateOf(true) }
    val fade = remember(config) { Animatable(1f) }
    LaunchedEffect(config) {
        val dur = config.durationSeconds
        if (dur > 0) {
            delay(dur * 1000L)
            fade.animateTo(0f, tween(1200))
            visible = false
        }
    }
    if (!visible) return

    // Hard client cap: a bad server value can never flood the device with particles.
    val count = when (config.intensity) {
        AmbienceIntensity.LOW -> 24
        AmbienceIntensity.MEDIUM -> 48
        AmbienceIntensity.HIGH -> 80
    }.coerceIn(1, 90)

    val accent = remember(config.accentColorHex) { accentOr(config.accentColorHex, null) }
    val field = modifier.fillMaxSize().alpha(fade.value)

    when (effect) {
        AmbienceEffect.RAIN -> RainEffect(count, accent ?: Color(0xFF9EC7FF), field)
        AmbienceEffect.SNOW -> SnowEffect(count, accent ?: Color(0xFFFFFFFF), field)
        AmbienceEffect.STARS -> StarsEffect(count, accent ?: Color(0xFFFFF3B0), field)
        AmbienceEffect.SUNNY -> SunnyEffect(count, accent ?: Color(0xFFFFD08A), field)
        AmbienceEffect.CONFETTI -> ConfettiEffect(count, field)
        AmbienceEffect.HEARTS -> HeartsEffect(count, accent, field)
        AmbienceEffect.BUBBLES -> BubblesEffect(count, accent ?: Color(0xFFB3E5FC), field)
        AmbienceEffect.LEAVES -> LeavesEffect(count, field)
        AmbienceEffect.FIREFLIES -> FirefliesEffect(count, accent ?: Color(0xFFCDFF6B), field)
        AmbienceEffect.PETALS -> PetalsEffect(count, field)
        AmbienceEffect.NONE -> Unit
    }
}

// ---- Shared particle model & drivers -------------------------------------------------------------

private data class Particle(
    val x: Float,      // 0..1 horizontal start
    val y: Float,      // 0..1 (used by 2D effects like fireflies/stars)
    val speed: Float,  // fall/rise rate
    val phase: Float,  // 0..1 stagger
    val size: Float,   // 0..1 scale
    val sway: Float,   // horizontal wobble amount
    val alpha: Float,  // 0..1 base opacity
    val rot: Float,    // base rotation degrees
    val index: Int     // stable index for palette selection
)

@Composable
private fun rememberParticles(count: Int): List<Particle> = remember(count) {
    List(count) { i ->
        Particle(
            x = Random.nextFloat(),
            y = Random.nextFloat(),
            speed = Random.nextFloat() * 0.9f + 0.4f,
            phase = Random.nextFloat(),
            size = Random.nextFloat(),
            sway = Random.nextFloat() * 0.05f + 0.01f,
            alpha = Random.nextFloat() * 0.45f + 0.4f,
            rot = Random.nextFloat() * 360f,
            index = i
        )
    }
}

@Composable
private fun rememberLoop(durationMillis: Int, reverse: Boolean = false): State<Float> {
    val transition = rememberInfiniteTransition(label = "ambience")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis, easing = LinearEasing),
            if (reverse) RepeatMode.Reverse else RepeatMode.Restart
        ),
        label = "ambienceT"
    )
}

private fun frac(v: Float): Float = v - kotlin.math.floor(v)
private fun swayX(p: Particle, t: Float, width: Float, mult: Float = 1f): Float =
    (p.x + sin((t + p.phase) * TWO_PI) * p.sway * mult) * width

// ---- Effects -------------------------------------------------------------------------------------

@Composable
private fun RainEffect(count: Int, color: Color, modifier: Modifier) {
    val drops = rememberParticles(count)
    val t by rememberLoop(1300)
    Canvas(modifier) {
        drops.forEach { p ->
            val y = frac(t * (p.speed + 0.6f) + p.phase) * size.height
            val x = p.x * size.width
            val len = (0.05f + p.size * 0.05f) * size.height
            drawLine(
                color = color.copy(alpha = 0.25f + p.alpha * 0.35f),
                start = Offset(x, y),
                end = Offset(x, y + len),
                strokeWidth = 2.5f
            )
        }
    }
}

@Composable
private fun SnowEffect(count: Int, color: Color, modifier: Modifier) {
    val flakes = rememberParticles(count)
    val t by rememberLoop(6000)
    Canvas(modifier) {
        flakes.forEach { p ->
            val y = frac(t * p.speed + p.phase) * size.height
            val x = swayX(p, t, size.width)
            drawCircle(color.copy(alpha = p.alpha), radius = 2f + p.size * 4f, center = Offset(x, y))
        }
    }
}

@Composable
private fun StarsEffect(count: Int, color: Color, modifier: Modifier) {
    val stars = rememberParticles(count)
    val t by rememberLoop(2600, reverse = true)
    Canvas(modifier) {
        stars.forEach { p ->
            val twinkle = (0.3f + 0.6f * (0.5f + 0.5f * sin((t + p.phase) * TWO_PI))).coerceIn(0f, 1f)
            drawCircle(color.copy(alpha = twinkle), radius = 1f + p.size * 2.5f, center = Offset(p.x * size.width, p.y * size.height))
        }
    }
}

@Composable
private fun SunnyEffect(count: Int, color: Color, modifier: Modifier) {
    val motes = rememberParticles((count / 2).coerceIn(6, 40))
    val t by rememberLoop(9000)
    Canvas(modifier) {
        // Warm sunlight glow from the top-right, like light through a window.
        val c = Offset(size.width * 0.85f, size.height * 0.12f)
        val r = size.minDimension * 0.7f
        drawCircle(
            brush = Brush.radialGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent), center = c, radius = r),
            radius = r, center = c
        )
        motes.forEach { p ->
            val y = (1f - frac(t * (p.speed * 0.5f) + p.phase)) * size.height
            val x = swayX(p, t, size.width)
            drawCircle(color.copy(alpha = p.alpha * 0.4f), radius = 1.5f + p.size * 3f, center = Offset(x, y))
        }
    }
}

@Composable
private fun ConfettiEffect(count: Int, modifier: Modifier) {
    val pieces = rememberParticles(count)
    val t by rememberLoop(2600)
    Canvas(modifier) {
        pieces.forEach { p ->
            val y = frac(t * (p.speed + 0.5f) + p.phase) * size.height
            val x = swayX(p, t, size.width, mult = 2f)
            val w = 6f + p.size * 8f
            val h = 4f + p.size * 6f
            val color = ConfettiColors[p.index % ConfettiColors.size]
            rotate(degrees = p.rot + t * 540f, pivot = Offset(x, y)) {
                drawRect(color.copy(alpha = 0.85f), topLeft = Offset(x - w / 2f, y - h / 2f), size = Size(w, h))
            }
        }
    }
}

@Composable
private fun HeartsEffect(count: Int, accent: Color?, modifier: Modifier) {
    val hearts = rememberParticles((count * 2 / 3).coerceIn(8, 60))
    val t by rememberLoop(5000)
    Canvas(modifier) {
        hearts.forEach { p ->
            val prog = frac(t * p.speed + p.phase)
            val y = (1f - prog) * size.height
            val x = swayX(p, t, size.width, mult = 1.5f)
            val r = 6f + p.size * 10f
            val base = accent ?: HeartColors[p.index % HeartColors.size]
            drawHeart(x, y, r, base.copy(alpha = (p.alpha * (1f - prog * 0.4f)).coerceIn(0f, 1f)))
        }
    }
}

@Composable
private fun BubblesEffect(count: Int, color: Color, modifier: Modifier) {
    val bubbles = rememberParticles(count)
    val t by rememberLoop(7000)
    Canvas(modifier) {
        bubbles.forEach { p ->
            val y = (1f - frac(t * p.speed + p.phase)) * size.height
            val x = swayX(p, t, size.width, mult = 1.5f)
            val r = 4f + p.size * 14f
            drawCircle(color.copy(alpha = p.alpha * 0.5f), radius = r, center = Offset(x, y), style = Stroke(width = 2f))
            drawCircle(Color.White.copy(alpha = p.alpha * 0.5f), radius = r * 0.22f, center = Offset(x - r * 0.3f, y - r * 0.3f))
        }
    }
}

@Composable
private fun LeavesEffect(count: Int, modifier: Modifier) {
    val leaves = rememberParticles((count * 2 / 3).coerceIn(8, 60))
    val t by rememberLoop(5200)
    Canvas(modifier) {
        leaves.forEach { p ->
            val y = frac(t * p.speed + p.phase) * size.height
            val x = swayX(p, t, size.width, mult = 3f)
            val w = 8f + p.size * 10f
            val h = w * 0.55f
            val color = LeafColors[p.index % LeafColors.size]
            rotate(degrees = p.rot + t * 220f, pivot = Offset(x, y)) {
                drawOval(color.copy(alpha = p.alpha), topLeft = Offset(x - w / 2f, y - h / 2f), size = Size(w, h))
            }
        }
    }
}

@Composable
private fun PetalsEffect(count: Int, modifier: Modifier) {
    val petals = rememberParticles((count * 2 / 3).coerceIn(8, 60))
    val t by rememberLoop(6500)
    Canvas(modifier) {
        petals.forEach { p ->
            val y = frac(t * (p.speed * 0.7f) + p.phase) * size.height
            val x = swayX(p, t, size.width, mult = 3f)
            val w = 7f + p.size * 9f
            val h = w * 0.6f
            val color = PetalColors[p.index % PetalColors.size]
            rotate(degrees = p.rot + t * 160f, pivot = Offset(x, y)) {
                drawOval(color.copy(alpha = p.alpha * 0.9f), topLeft = Offset(x - w / 2f, y - h / 2f), size = Size(w, h))
            }
        }
    }
}

@Composable
private fun FirefliesEffect(count: Int, color: Color, modifier: Modifier) {
    val flies = rememberParticles((count * 2 / 3).coerceIn(8, 50))
    val t by rememberLoop(4000, reverse = true)
    Canvas(modifier) {
        flies.forEach { p ->
            val ang: Float = (t + p.phase) * TWO_PI
            val glow: Float = (0.25f + 0.7f * (0.5f + 0.5f * sin(ang))).coerceIn(0f, 1f)
            val x: Float = (p.x + sin(ang) * p.sway * 2f) * size.width
            val y: Float = (p.y + cos(ang) * p.sway * 2f) * size.height
            val r = 1.5f + p.size * 2.5f
            drawCircle(
                brush = Brush.radialGradient(listOf(color.copy(alpha = glow * 0.5f), Color.Transparent), center = Offset(x, y), radius = r * 4f),
                radius = r * 4f, center = Offset(x, y)
            )
            drawCircle(color.copy(alpha = glow), radius = r, center = Offset(x, y))
        }
    }
}

// ---- Drawing helpers -----------------------------------------------------------------------------

/** Simple heart from two lobes + a triangle — cheap and reads clearly at small sizes. */
private fun DrawScope.drawHeart(cx: Float, cy: Float, r: Float, color: Color) {
    drawCircle(color, radius = r * 0.55f, center = Offset(cx - r * 0.5f, cy))
    drawCircle(color, radius = r * 0.55f, center = Offset(cx + r * 0.5f, cy))
    val path = Path().apply {
        moveTo(cx - r * 1.02f, cy + r * 0.1f)
        lineTo(cx + r * 1.02f, cy + r * 0.1f)
        lineTo(cx, cy + r * 1.5f)
        close()
    }
    drawPath(path, color)
}

private val ConfettiColors = listOf(
    Color(0xFFFF6B35), Color(0xFFFFD23F), Color(0xFF4A90E2),
    Color(0xFF7ED957), Color(0xFFFF5DA2), Color(0xFF9B5DE5)
)
private val HeartColors = listOf(Color(0xFFFF5DA2), Color(0xFFFF2A5F), Color(0xFFFF8FB1))
private val LeafColors = listOf(Color(0xFFD2691E), Color(0xFFC0392B), Color(0xFFE67E22), Color(0xFFB7791F))
private val PetalColors = listOf(Color(0xFFFFC0CB), Color(0xFFFFB7C5), Color(0xFFFFD9E3))

/** Parses a "#RRGGBB" hex into a Color, returning [fallback] on any bad input. */
private fun accentOr(hex: String?, fallback: Color?): Color? {
    if (hex.isNullOrBlank()) return fallback
    return try {
        parseHexColor(hex)
    } catch (e: Exception) {
        fallback
    }
}
