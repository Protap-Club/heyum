package com.pgstorm148.heyum.firebase

/**
 * Turns whatever the user typed into the E.164 form Firebase phone auth requires ("+919876543210").
 *
 * Firebase rejects any number without a leading "+countrycode", so a user who just types their
 * 10-digit number used to hit an error. Numbers typed without a country code are treated as
 * Indian (+91); anything that already carries a code ("+44 ...", "0044 ...") is kept as is.
 */
object PhoneNumberFormatter {
    private const val DEFAULT_CC = "91"

    /** Returns the E.164 number, or null if [raw] can't be a valid phone number. */
    fun toE164(raw: String): String? {
        val trimmed = raw.trim()
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return null

        val full = when {
            trimmed.startsWith("+") -> digits
            trimmed.startsWith("00") -> digits.removePrefix("00")
            digits.length == 10 -> DEFAULT_CC + digits
            digits.length == 11 && digits.startsWith("0") -> DEFAULT_CC + digits.drop(1)
            digits.length == 12 && digits.startsWith(DEFAULT_CC) -> digits
            else -> return null
        }
        if (full.length !in 8..15) return null
        // Indian numbers are always 91 + 10 digits; catch typos like a missing digit early.
        if (full.startsWith(DEFAULT_CC) && full.length != 12) return null
        return "+$full"
    }
}
