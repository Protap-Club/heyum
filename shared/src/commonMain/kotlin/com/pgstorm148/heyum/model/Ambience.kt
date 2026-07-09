package com.pgstorm148.heyum.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Decorative ambient effects the client can render behind the "choose what to eat" screen.
 * Purely cosmetic — never affects the decision flow. Add new effects here and in AmbienceOverlay.
 */
enum class AmbienceEffect {
    NONE, RAIN, SUNNY, SNOW, STARS, CONFETTI, HEARTS, BUBBLES, LEAVES, FIREFLIES, PETALS;

    companion object {
        /** Unknown / null names fall back to NONE, so a bad server value can never break the UI. */
        fun fromName(name: String?): AmbienceEffect =
            values().firstOrNull { it.name.equals(name?.trim(), ignoreCase = true) } ?: NONE
    }
}

enum class AmbienceIntensity {
    LOW, MEDIUM, HIGH;

    companion object {
        fun fromName(name: String?): AmbienceIntensity =
            values().firstOrNull { it.name.equals(name?.trim(), ignoreCase = true) } ?: MEDIUM
    }
}

/**
 * Server-driven ambience config. This is *config, not UI*: the server only selects and
 * parameterizes effects the client already implements. Anything unknown, missing, or disabled
 * resolves to NONE — the screen renders normally with no overlay.
 */
data class AmbienceConfig(
    val version: Int = 1,
    val enabled: Boolean = false,
    val mode: String = "forced",              // "forced" (you pick) | "auto" (client picks by daypart)
    val effect: AmbienceEffect = AmbienceEffect.NONE,
    val intensity: AmbienceIntensity = AmbienceIntensity.MEDIUM,
    val accentColorHex: String? = null,
    val message: String? = null,              // reserved for future use (banner copy)
    val startAt: Long = 0L,                   // optional schedule window, epoch millis; 0 = always
    val endAt: Long = 0L,
    val durationSeconds: Int = 8,             // how long the effect plays each time the screen opens; 0 = forever
    val auto: Map<String, AmbienceEffect> = emptyMap()
) {
    /**
     * The effect to actually render right now, honoring the enable flag, schedule window, and mode.
     * Returns NONE whenever ambience should be invisible.
     */
    fun resolveEffect(nowMillis: Long = Clock.System.now().toEpochMilliseconds()): AmbienceEffect {
        if (!enabled) return AmbienceEffect.NONE
        if (startAt > 0L && nowMillis < startAt) return AmbienceEffect.NONE
        if (endAt > 0L && nowMillis > endAt) return AmbienceEffect.NONE
        return when (mode.lowercase()) {
            "auto" -> auto[currentDaypartKey(nowMillis)] ?: AmbienceEffect.NONE
            else -> effect
        }
    }

    private fun currentDaypartKey(nowMillis: Long): String {
        val hour = try {
            Instant.fromEpochMilliseconds(nowMillis)
                .toLocalDateTime(TimeZone.currentSystemDefault()).hour
        } catch (e: Exception) {
            12
        }
        return when (hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..20 -> "evening"
            else -> "night"
        }
    }

    companion object {
        val Default = AmbienceConfig()

        /** Build from a raw Firestore document map, tolerating missing or mistyped fields. */
        fun fromMap(data: Map<String, Any?>?): AmbienceConfig {
            if (data == null) return Default
            return try {
                @Suppress("UNCHECKED_CAST")
                val autoRaw = data["auto"] as? Map<String, Any?> ?: emptyMap()
                val auto = autoRaw.mapNotNull { (key, value) ->
                    val e = AmbienceEffect.fromName(value as? String)
                    if (e == AmbienceEffect.NONE) null else key to e
                }.toMap()
                AmbienceConfig(
                    version = (data["version"] as? Number)?.toInt() ?: 1,
                    enabled = data["enabled"] as? Boolean ?: false,
                    mode = (data["mode"] as? String) ?: "forced",
                    effect = AmbienceEffect.fromName(data["effect"] as? String),
                    intensity = AmbienceIntensity.fromName(data["intensity"] as? String),
                    accentColorHex = data["accentColor"] as? String,
                    message = data["message"] as? String,
                    startAt = (data["startAt"] as? Number)?.toLong() ?: 0L,
                    endAt = (data["endAt"] as? Number)?.toLong() ?: 0L,
                    durationSeconds = (data["durationSeconds"] as? Number)?.toInt() ?: 8,
                    auto = auto
                )
            } catch (e: Exception) {
                Default
            }
        }
    }
}
