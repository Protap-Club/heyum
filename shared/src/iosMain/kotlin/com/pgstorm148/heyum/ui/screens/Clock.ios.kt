package com.pgstorm148.heyum.ui.screens

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour

actual fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun getCurrentHour(): Int {
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(NSCalendarUnitHour, fromDate = NSDate())
    return components.hour.toInt()
}
