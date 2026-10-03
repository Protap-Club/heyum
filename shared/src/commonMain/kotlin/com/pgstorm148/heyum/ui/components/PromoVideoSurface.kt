package com.pgstorm148.heyum.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform video surface for the promo popup: ExoPlayer (Media3) on Android, AVPlayer on iOS.
 * It only renders and reports; every control (play/pause, mute, which clip is next) is driven
 * by the caller through the parameters, so the shared UI owns all the state.
 *
 * The video is cropped to fill the surface (centre-crop), so a 9:16 clip fills a 9:16 card.
 *
 * @param loop       replay the same clip when it ends (used when there is only one clip)
 * @param onReady    first frame is on screen
 * @param onEnded    clip finished and [loop] is false
 * @param onError    the clip could not be loaded or played (offline, 404, unsupported)
 */
@Composable
expect fun PromoVideoSurface(
    url: String,
    playing: Boolean,
    muted: Boolean,
    loop: Boolean,
    onReady: () -> Unit,
    onEnded: () -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier
)
