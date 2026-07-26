package com.pgstorm148.heyum.delivery

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

// Custom URL schemes are used ONLY to detect whether an app is installed, via canOpenURL — never
// to actually open the app (openDeliveryApp below always opens an https link or the web
// fallback). So a wrong scheme here has a safe failure mode: the row is just mis-shown/hidden in
// the picker, it can never launch the wrong app.
//
// Confidence: "amazon" is verified (com.amazon.mobile.shopping.web is Amazon's documented iOS
// scheme). The rest are best-effort — these Indian apps don't publish scheme docs. If an app you
// have installed isn't showing up as available, that scheme is likely wrong; it's a quick fix
// once confirmed on-device.
//
// Instamart/Toing (inside Swiggy) and Zepto Cafe (inside Zepto) share their parent app's scheme,
// mirroring how DeliveryApps.android.kt lists them under the same package name.
private val CANDIDATE_APPS = listOf(
    // Quick commerce — for buying missing ingredients.
    DeliveryApp(id = "blinkit", displayName = "Blinkit", webFallbackUrl = "https://blinkit.com", category = DeliveryCategory.QUICK_COMMERCE, emoji = "⚡", iosUrlScheme = "blinkit"),
    DeliveryApp(id = "zepto", displayName = "Zepto", webFallbackUrl = "https://www.zeptonow.com", category = DeliveryCategory.QUICK_COMMERCE, emoji = "⚡", iosUrlScheme = "zepto"),
    DeliveryApp(id = "instamart", displayName = "Swiggy Instamart", webFallbackUrl = "https://www.swiggy.com/instamart", category = DeliveryCategory.QUICK_COMMERCE, emoji = "🛒", iosUrlScheme = "swiggy"),
    DeliveryApp(id = "bigbasket", displayName = "BigBasket", webFallbackUrl = "https://www.bigbasket.com", category = DeliveryCategory.QUICK_COMMERCE, emoji = "🥦", iosUrlScheme = "bigbasket"),
    DeliveryApp(id = "flipkart", displayName = "Flipkart Minutes", webFallbackUrl = "https://www.flipkart.com", category = DeliveryCategory.QUICK_COMMERCE, emoji = "🛍️", iosUrlScheme = "flipkart"),
    DeliveryApp(id = "amazon", displayName = "Amazon Now", webFallbackUrl = "https://www.amazon.in", category = DeliveryCategory.QUICK_COMMERCE, emoji = "📦", iosUrlScheme = "com.amazon.mobile.shopping.web"),
    // Food delivery — for ordering the finished dish.
    DeliveryApp(id = "swiggy", displayName = "Swiggy", webFallbackUrl = "https://www.swiggy.com", category = DeliveryCategory.FOOD_DELIVERY, emoji = "🛵", iosUrlScheme = "swiggy"),
    DeliveryApp(id = "zomato", displayName = "Zomato", webFallbackUrl = "https://www.zomato.com", category = DeliveryCategory.FOOD_DELIVERY, emoji = "🍽️", iosUrlScheme = "zomato"),
    DeliveryApp(id = "zeptocafe", displayName = "Zepto Cafe", webFallbackUrl = "https://www.zeptonow.com/cafe", category = DeliveryCategory.FOOD_DELIVERY, emoji = "☕", iosUrlScheme = "zepto"),
    DeliveryApp(id = "swish", displayName = "Swish", webFallbackUrl = "https://www.justswish.in", category = DeliveryCategory.FOOD_DELIVERY, emoji = "⚡", iosUrlScheme = "swish"),
    DeliveryApp(id = "toing", displayName = "Toing", webFallbackUrl = "https://www.swiggy.com", category = DeliveryCategory.FOOD_DELIVERY, emoji = "🍱", iosUrlScheme = "swiggy"),
    DeliveryApp(id = "ownly", displayName = "Ownly by Rapido", webFallbackUrl = "https://ownly.food", category = DeliveryCategory.FOOD_DELIVERY, emoji = "🛺", iosUrlScheme = "ownly"),
    DeliveryApp(id = "eatclub", displayName = "EatClub", webFallbackUrl = "https://eatclub.in", category = DeliveryCategory.FOOD_DELIVERY, emoji = "🍔", iosUrlScheme = "eatclub")
)

actual fun getInstalledDeliveryApps(): List<DeliveryApp> {
    return CANDIDATE_APPS.filter { app ->
        val scheme = app.iosUrlScheme ?: return@filter false
        val url = NSURL.URLWithString("$scheme://") ?: return@filter false
        UIApplication.sharedApplication.canOpenURL(url)
    }
}

actual fun openDeliveryApp(app: DeliveryApp, url: String?) {
    val link = url ?: app.webFallbackUrl
    // An https universal link opens the app directly when it's installed and configured for the
    // domain; otherwise iOS opens it in Safari.
    val nsUrl = NSURL.URLWithString(link) ?: return
    // openURL(_:) (no options) is the deprecated selector — modern iOS hard-blocks it at runtime
    // ("BUG IN CLIENT OF UIKIT... Force returning false"), so every tap silently did nothing.
    // open(_:options:completionHandler:) is the one that actually still works.
    UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any?>(), completionHandler = null)
}
