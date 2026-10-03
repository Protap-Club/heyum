package com.pgstorm148.heyum.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.currentItem
import platform.AVFoundation.muted
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVKit.AVPlayerViewController
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.UIKit.UIColor

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PromoVideoSurface(
    url: String,
    playing: Boolean,
    muted: Boolean,
    loop: Boolean,
    onReady: () -> Unit,
    onEnded: () -> Unit,
    onError: () -> Unit,
    modifier: Modifier
) {
    val player = remember { AVPlayer() }
    val controller = remember {
        AVPlayerViewController().apply {
            this.player = player
            showsPlaybackControls = false
            videoGravity = AVLayerVideoGravityResizeAspectFill
            view.backgroundColor = UIColor.clearColor
        }
    }
    val currentOnReady by rememberUpdatedState(onReady)
    val currentOnEnded by rememberUpdatedState(onEnded)
    val currentOnError by rememberUpdatedState(onError)
    val currentLoop by rememberUpdatedState(loop)
    val currentPlaying by rememberUpdatedState(playing)

    DisposableEffect(player) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { note ->
            // Only react to the item this surface is playing.
            if (note?.`object` === player.currentItem) {
                if (currentLoop) {
                    player.seekToTime(CMTimeMake(0, 1))
                    if (currentPlaying) player.play()
                } else {
                    currentOnEnded()
                }
            }
        }
        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
            player.pause()
            player.replaceCurrentItemWithPlayerItem(null)
        }
    }

    LaunchedEffect(url) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl == null) {
            currentOnError()
            return@LaunchedEffect
        }
        player.replaceCurrentItemWithPlayerItem(AVPlayerItem(uRL = nsUrl))
        if (currentPlaying) player.play()

        // AVPlayerItem.status is KVO-only; polling it is simpler than bridging an observer.
        var reported = false
        while (!reported) {
            when (player.currentItem?.status) {
                AVPlayerItemStatusReadyToPlay -> {
                    currentOnReady()
                    reported = true
                }
                AVPlayerItemStatusFailed -> {
                    currentOnError()
                    reported = true
                }
                else -> delay(250)
            }
        }
    }
    LaunchedEffect(playing) { if (playing) player.play() else player.pause() }
    LaunchedEffect(muted) {
        player.muted = muted
        // The silent switch mutes the default ambient session, so an explicit unmute has to
        // switch to a playback session (mixing, so the user's music isn't stopped).
        runCatching {
            val session = AVAudioSession.sharedInstance()
            if (muted) {
                session.setCategory(AVAudioSessionCategoryAmbient, error = null)
            } else {
                session.setCategory(AVAudioSessionCategoryPlayback, withOptions = AVAudioSessionCategoryOptionMixWithOthers, error = null)
            }
        }
    }

    UIKitViewController(
        factory = { controller },
        modifier = modifier,
        // Non-interactive: touches fall through to Compose, which owns tap and the controls.
        interactive = false
    )
}
