package com.pgstorm148.heyum.delivery

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

// Detecting installed third-party apps on iOS requires declaring their custom URL schemes in
// Info.plist (LSApplicationQueriesSchemes). Until those are added we report none installed, so the
// UI shows its web-order fallback. Opening a universal link below still lands in the app if present.
actual fun getInstalledDeliveryApps(): List<DeliveryApp> = emptyList()

actual fun openDeliveryApp(app: DeliveryApp, url: String?) {
    val link = url ?: app.webFallbackUrl
    // An https universal link opens the app directly when it's installed and configured for the
    // domain; otherwise iOS opens it in Safari.
    val nsUrl = NSURL.URLWithString(link) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}
