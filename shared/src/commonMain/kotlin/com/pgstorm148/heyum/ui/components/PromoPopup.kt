package com.pgstorm148.heyum.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pgstorm148.heyum.firebase.PromoRepository
import com.pgstorm148.heyum.model.PromoConfig
import com.pgstorm148.heyum.model.PromoVideo
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/** Remembers, for this app launch only, that the user closed the popup. */
object PromoSession {
    var dismissed by mutableStateOf(false)
}

/**
 * Floating, server-driven video popup (bottom-left). Place it inside the screen's root Box.
 *
 * Clips come from Firestore `config/promo`; they play muted and in order, looping the list.
 * The card stays invisible until the first frame is ready, and removes itself for the rest of
 * the session if every clip fails to load (offline, bad URL), so users never see a broken box.
 */
@Composable
fun PromoPopup(bottomPadding: Dp = 120.dp) {
    val repo: PromoRepository = koinInject()
    val config by remember { repo.observePromo() }.collectAsState(initial = PromoConfig.Default)
    val videos = remember(config) { config.activeVideos() }

    // Wait a beat after the screen opens so the popup doesn't compete with the first card.
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1200)
        armed = true
    }
    // A load failure only hides the popup for this visit to the screen; only the close button
    // hides it for the whole session. (A transient failure must not silence it until restart.)
    var gaveUp by remember(videos) { mutableStateOf(false) }
    if (!armed || PromoSession.dismissed || gaveUp || videos.isEmpty()) return

    PromoCard(videos, bottomPadding, onGiveUp = { gaveUp = true })
}

private const val LOAD_TIMEOUT_MS = 12_000L

@Composable
private fun PromoCard(videos: List<PromoVideo>, bottomPadding: Dp, onGiveUp: () -> Unit) {
    var index by remember { mutableStateOf(0) }
    var failures by remember { mutableStateOf(0) }
    var ready by remember { mutableStateOf(false) }
    // Becomes true at the first frame and stays true, so switching clips doesn't blink the card away.
    var shown by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(true) }
    var muted by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    val video = videos[index % videos.size]

    fun next() {
        ready = false
        index = (index + 1) % videos.size
    }

    fun fail(reason: String) {
        failures += 1
        if (failures >= videos.size) onGiveUp() else next()
    }

    // A clip that never produces a frame counts as failed.
    LaunchedEffect(video.videoUrl, ready) {
        if (!ready) {
            delay(LOAD_TIMEOUT_MS)
            fail("no first frame after ${LOAD_TIMEOUT_MS / 1000}s")
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val collapsedWidth = 112.dp
        val expandedHeight = maxHeight * 0.6f
        val expandedWidth = minOf(expandedHeight * 9f / 16f, maxWidth - 24.dp)
        val width by animateDpAsState(if (expanded) expandedWidth else collapsedWidth, tween(260))
        val height = width * 16f / 9f
        val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(300))
        val shape = RoundedCornerShape(16.dp)

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = bottomPadding)
                .size(width, height)
                .alpha(alpha)
                .shadow(10.dp, shape)
                .clip(shape)
                .background(Color(0xFF161616))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    if (video.tapUrl.isNotBlank()) runCatching { uriHandler.openUri(video.tapUrl) }
                }
        ) {
            PromoVideoSurface(
                url = video.videoUrl,
                playing = playing,
                muted = muted,
                loop = videos.size == 1,
                onReady = {
                    ready = true
                    shown = true
                    failures = 0
                },
                onEnded = { next() },
                onError = { fail("player error") },
                modifier = Modifier.fillMaxSize()
            )

            // Close, top-right.
            CircleButton(Modifier.align(Alignment.TopEnd).padding(6.dp), onClick = { PromoSession.dismissed = true }) { drawClose() }
            // Mute, top-left.
            CircleButton(Modifier.align(Alignment.TopStart).padding(6.dp), onClick = { muted = !muted }) { drawSpeaker(muted) }
            // Play / pause, bottom-left.
            CircleButton(Modifier.align(Alignment.BottomStart).padding(6.dp), onClick = { playing = !playing }) {
                if (playing) drawPause() else drawPlay()
            }
            // Next video, bottom-centre (only when there is another clip to switch to).
            if (videos.size > 1) {
                CircleButton(Modifier.align(Alignment.BottomCenter).padding(6.dp), onClick = {
                    playing = true
                    next()
                }) { drawSkipNext() }
            }
            // Expand / shrink, bottom-right.
            CircleButton(Modifier.align(Alignment.BottomEnd).padding(6.dp), onClick = { expanded = !expanded }) { drawExpand(!expanded) }
        }
    }
}

@Composable
private fun CircleButton(modifier: Modifier, onClick: () -> Unit, icon: DrawScope.() -> Unit) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize().padding(7.dp)) { icon() }
    }
}

// --- icons, drawn directly so the popup needs no icon library ---------------------------------

private val IconColor = Color.White

private fun DrawScope.stroke() = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round)

private fun DrawScope.drawClose() {
    val s = stroke()
    drawLine(IconColor, Offset(0f, 0f), Offset(size.width, size.height), s.width, StrokeCap.Round)
    drawLine(IconColor, Offset(size.width, 0f), Offset(0f, size.height), s.width, StrokeCap.Round)
}

private fun DrawScope.drawPlay() {
    val p = Path().apply {
        moveTo(size.width * 0.18f, 0f)
        lineTo(size.width, size.height / 2f)
        lineTo(size.width * 0.18f, size.height)
        close()
    }
    drawPath(p, IconColor)
}

private fun DrawScope.drawSkipNext() {
    val w = size.width
    val h = size.height
    val tri = Path().apply {
        moveTo(0f, 0f)
        lineTo(w * 0.72f, h / 2f)
        lineTo(0f, h)
        close()
    }
    drawPath(tri, IconColor)
    drawRect(IconColor, Offset(w * 0.8f, 0f), Size(w * 0.2f, h))
}

private fun DrawScope.drawPause() {
    val w = size.width * 0.3f
    drawRect(IconColor, Offset(size.width * 0.08f, 0f), Size(w, size.height))
    drawRect(IconColor, Offset(size.width * 0.62f, 0f), Size(w, size.height))
}

private fun DrawScope.drawSpeaker(muted: Boolean) {
    val w = size.width
    val h = size.height
    val body = Path().apply {
        moveTo(0f, h * 0.35f)
        lineTo(w * 0.28f, h * 0.35f)
        lineTo(w * 0.56f, h * 0.08f)
        lineTo(w * 0.56f, h * 0.92f)
        lineTo(w * 0.28f, h * 0.65f)
        lineTo(0f, h * 0.65f)
        close()
    }
    drawPath(body, IconColor)
    val sw = 1.9.dp.toPx()
    if (muted) {
        drawLine(IconColor, Offset(w * 0.72f, h * 0.34f), Offset(w, h * 0.66f), sw, StrokeCap.Round)
        drawLine(IconColor, Offset(w, h * 0.34f), Offset(w * 0.72f, h * 0.66f), sw, StrokeCap.Round)
    } else {
        drawArc(IconColor, -45f, 90f, false, Offset(w * 0.46f, h * 0.28f), Size(w * 0.36f, h * 0.44f), style = Stroke(sw, cap = StrokeCap.Round))
        drawArc(IconColor, -45f, 90f, false, Offset(w * 0.46f, h * 0.06f), Size(w * 0.54f, h * 0.88f), style = Stroke(sw, cap = StrokeCap.Round))
    }
}

/** Two diagonal arrows: pointing outward to the corners (expand) or inward (shrink). */
private fun DrawScope.drawExpand(outward: Boolean) {
    val w = size.width
    val h = size.height
    val sw = 1.9.dp.toPx()
    val head = w * 0.34f
    fun arrow(tip: Offset, tail: Offset, dx: Float, dy: Float) {
        drawLine(IconColor, tail, tip, sw, StrokeCap.Round)
        drawLine(IconColor, tip, Offset(tip.x + dx * head, tip.y), sw, StrokeCap.Round)
        drawLine(IconColor, tip, Offset(tip.x, tip.y + dy * head), sw, StrokeCap.Round)
    }
    if (outward) {
        arrow(Offset(w, 0f), Offset(w * 0.55f, h * 0.45f), -1f, 1f)
        arrow(Offset(0f, h), Offset(w * 0.45f, h * 0.55f), 1f, -1f)
    } else {
        arrow(Offset(w * 0.55f, h * 0.45f), Offset(w, 0f), 1f, -1f)
        arrow(Offset(w * 0.45f, h * 0.55f), Offset(0f, h), -1f, 1f)
    }
}
