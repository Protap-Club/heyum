package com.pgstorm148.heyum.ui.screens

import java.util.Calendar

actual fun nowMillis(): Long = System.currentTimeMillis()

actual fun getCurrentHour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
