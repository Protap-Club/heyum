package com.pgstorm148.heyum.delivery

import android.content.Intent
import android.net.Uri
import com.pgstorm148.heyum.firebase.AndroidActivityHolder

// Package ids verified against live Play Store / App Store listings (July 2026). Keep in sync with
// the <queries> block in the app's AndroidManifest.xml, which is required on Android 11+ for
// PackageManager to see these apps at all.
private val CANDIDATE_APPS = listOf(
    // Quick commerce — for buying missing ingredients.
    DeliveryApp("blinkit", "Blinkit", "com.grofers.customerapp", "https://blinkit.com", DeliveryCategory.QUICK_COMMERCE, "⚡"),
    DeliveryApp("zepto", "Zepto", "com.zeptoconsumerapp", "https://www.zeptonow.com", DeliveryCategory.QUICK_COMMERCE, "⚡"),
    // Swiggy Instamart lives inside the Swiggy app, so it shares Swiggy's package id.
    DeliveryApp("instamart", "Swiggy Instamart", "in.swiggy.android", "https://www.swiggy.com/instamart", DeliveryCategory.QUICK_COMMERCE, "🛒"),
    DeliveryApp("bigbasket", "BigBasket", "com.bigbasket.mobileapp", "https://www.bigbasket.com", DeliveryCategory.QUICK_COMMERCE, "🥦"),
    // Flipkart Minutes lives inside the Flipkart app; Amazon Now lives inside the Amazon app.
    DeliveryApp("flipkart", "Flipkart Minutes", "com.flipkart.android", "https://www.flipkart.com", DeliveryCategory.QUICK_COMMERCE, "🛍️"),
    DeliveryApp("amazon", "Amazon Now", "in.amazon.mShop.android.shopping", "https://www.amazon.in", DeliveryCategory.QUICK_COMMERCE, "📦"),
    // Food delivery — for ordering the finished dish.
    DeliveryApp("swiggy", "Swiggy", "in.swiggy.android", "https://www.swiggy.com", DeliveryCategory.FOOD_DELIVERY, "🛵"),
    DeliveryApp("zomato", "Zomato", "com.application.zomato", "https://www.zomato.com", DeliveryCategory.FOOD_DELIVERY, "🍽️"),
    // Zepto Cafe lives inside the Zepto app (limited menu), so it shares Zepto's package id.
    DeliveryApp("zeptocafe", "Zepto Cafe", "com.zeptoconsumerapp", "https://www.zeptonow.com/cafe", DeliveryCategory.FOOD_DELIVERY, "☕"),
    DeliveryApp("swish", "Swish", "com.swishapp", "https://www.justswish.in", DeliveryCategory.FOOD_DELIVERY, "⚡"),
    DeliveryApp("toing", "Toing", "in.swiggy.android.toing", "https://www.swiggy.com", DeliveryCategory.FOOD_DELIVERY, "🍱"),
    DeliveryApp("ownly", "Ownly by Rapido", "com.ownly.customer", "https://ownly.food", DeliveryCategory.FOOD_DELIVERY, "🛺"),
    DeliveryApp("eatclub", "EatClub", "com.poncho.eatclub", "https://eatclub.in", DeliveryCategory.FOOD_DELIVERY, "🍔")
)

actual fun getInstalledDeliveryApps(): List<DeliveryApp> {
    val packageManager = AndroidActivityHolder.getActivity()?.packageManager ?: return emptyList()
    return CANDIDATE_APPS.filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
}

actual fun openDeliveryApp(app: DeliveryApp, url: String?) {
    val activity = AndroidActivityHolder.getActivity() ?: return
    val packageManager = activity.packageManager

    // 1. Preferred: route the deep link straight into the target app (search page, cart, etc.).
    if (url != null) {
        val deepLink = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage(app.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (deepLink.resolveActivity(packageManager) != null) {
            try {
                activity.startActivity(deepLink)
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 2. App is installed but can't handle that link: just open the app's home screen.
    val launch = packageManager.getLaunchIntentForPackage(app.packageName)
    if (launch != null) {
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            activity.startActivity(launch)
            return
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 3. App not installed / everything failed: open the web link in a browser.
    val web = Intent(Intent.ACTION_VIEW, Uri.parse(url ?: app.webFallbackUrl))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        activity.startActivity(web)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
