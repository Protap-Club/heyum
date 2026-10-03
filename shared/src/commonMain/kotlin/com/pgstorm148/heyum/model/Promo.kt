package com.pgstorm148.heyum.model

import kotlinx.datetime.Clock

/** One promo clip. Videos are hosted in Firebase Storage and streamed, never bundled. */
data class PromoVideo(
    val title: String,
    val videoUrl: String,
    val posterUrl: String,
    /** Where a tap on the video goes (e.g. an Instagram page). Blank = tap does nothing. */
    val tapUrl: String
)

/**
 * Server-driven config for the floating promo popup, read from the Firestore document
 * `config/promo`. Like [AmbienceConfig], anything missing, mistyped or disabled resolves to
 * "show nothing", so a bad edit in the console can never break the screen.
 *
 *   enabled : Boolean
 *   startAt / endAt : epoch millis, 0 = no limit
 *   videos  : [ { title, videoUrl, posterUrl, tapUrl }, ... ]   — played in order, then looped
 */
data class PromoConfig(
    val enabled: Boolean = false,
    val startAt: Long = 0L,
    val endAt: Long = 0L,
    val videos: List<PromoVideo> = emptyList()
) {
    /** The clips to play right now, honoring the enable flag and schedule window. */
    fun activeVideos(nowMillis: Long = Clock.System.now().toEpochMilliseconds()): List<PromoVideo> {
        if (!enabled) return emptyList()
        if (startAt > 0L && nowMillis < startAt) return emptyList()
        if (endAt > 0L && nowMillis > endAt) return emptyList()
        return videos
    }

    companion object {
        val Default = PromoConfig()

        /** Build from a raw Firestore document map, tolerating missing or mistyped fields. */
        fun fromMap(data: Map<String, Any?>?): PromoConfig {
            if (data == null) return Default
            return try {
                val videos = (data["videos"] as? List<*>).orEmpty().mapNotNull { entry ->
                    val m = entry as? Map<*, *> ?: return@mapNotNull null
                    val url = (m["videoUrl"] as? String)?.trim().orEmpty()
                    if (url.isEmpty()) return@mapNotNull null
                    PromoVideo(
                        title = (m["title"] as? String).orEmpty(),
                        videoUrl = url,
                        posterUrl = (m["posterUrl"] as? String)?.trim().orEmpty(),
                        tapUrl = (m["tapUrl"] as? String)?.trim().orEmpty()
                    )
                }
                PromoConfig(
                    enabled = data["enabled"] as? Boolean ?: false,
                    startAt = (data["startAt"] as? Number)?.toLong() ?: 0L,
                    endAt = (data["endAt"] as? Number)?.toLong() ?: 0L,
                    videos = videos
                )
            } catch (e: Exception) {
                Default
            }
        }
    }
}
