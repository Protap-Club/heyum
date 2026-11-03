package com.pgstorm148.heyum.ui.screens

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Dhanteras through Bhai Dooj, 2026.
private val FestiveStart = LocalDate(2026, 11, 5)
private val FestiveEnd = LocalDate(2026, 11, 12)

/** Message shared (WhatsApp, etc.) to invite people into a group session. */
fun groupInviteMessage(link: String, today: LocalDate = indiaToday()): String =
    if (today in FestiveStart..FestiveEnd) {
        "🪔 Happy Diwali! Let's settle what the family eats tonight, everyone votes in 30 seconds on Heyum: $link"
    } else {
        "Join my Heyum group and let's decide what to eat! $link"
    }

private fun indiaToday(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Kolkata")).date
