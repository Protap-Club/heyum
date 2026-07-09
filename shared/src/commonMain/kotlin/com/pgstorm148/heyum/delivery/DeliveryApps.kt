package com.pgstorm148.heyum.delivery

/**
 * Whether an app is used to buy raw ingredients (quick commerce / groceries) or to order a
 * finished dish (food-delivery aggregator). The post-decision "make it" flow surfaces these
 * in two different places: missing ingredients go to quick commerce, the whole dish goes to
 * a food aggregator.
 */
enum class DeliveryCategory { QUICK_COMMERCE, FOOD_DELIVERY }

data class DeliveryApp(
    val id: String,
    val displayName: String,
    val packageName: String,
    val webFallbackUrl: String,
    val category: DeliveryCategory = DeliveryCategory.FOOD_DELIVERY,
    val emoji: String = "🛵"
)

/** Returns only the delivery apps that are actually installed on this device. */
expect fun getInstalledDeliveryApps(): List<DeliveryApp>

/**
 * Opens [app], preferring the deep link in [url] (an https app-link / universal link routed
 * straight into the installed app on both Android and iOS). When [url] is null or the app can't
 * handle it, this launches the app's home screen instead, and finally falls back to the web.
 */
expect fun openDeliveryApp(app: DeliveryApp, url: String?)

/** Installed quick-commerce (grocery) apps, for buying missing ingredients. */
fun getInstalledQuickCommerceApps(): List<DeliveryApp> =
    getInstalledDeliveryApps().filter { it.category == DeliveryCategory.QUICK_COMMERCE }

/** Installed food-delivery aggregators, for ordering the finished dish. */
fun getInstalledFoodDeliveryApps(): List<DeliveryApp> =
    getInstalledDeliveryApps().filter { it.category == DeliveryCategory.FOOD_DELIVERY }

private fun encodeQuery(text: String): String = text.trim().replace(" ", "+")

/**
 * Search deep link (https app-link) for a finished dish within a food-delivery app, or null when
 * the app has no known search endpoint — in that case the caller should just launch the app home.
 * These https URLs double as the apps' verified app-links / universal links, so [openDeliveryApp]
 * can route them into the app on both platforms.
 */
fun DeliveryApp.dishSearchUrl(dishName: String): String? {
    val q = encodeQuery(dishName)
    return when (id) {
        "swiggy" -> "https://www.swiggy.com/search?query=$q"
        "zomato" -> "https://www.zomato.com/search?q=$q"
        // Swish, Toing, Ownly, EatClub, Zepto Cafe expose no public search route — launch the app.
        else -> null
    }
}

/**
 * Grocery search deep link for the given ingredients within a quick-commerce app, or null when the
 * app has no known search endpoint (caller launches the app home instead).
 */
fun DeliveryApp.grocerySearchUrl(ingredients: List<String>): String? {
    val q = encodeQuery(ingredients.joinToString(" "))
    return when (id) {
        "blinkit" -> "https://blinkit.com/s/?q=$q"
        "zepto" -> "https://www.zeptonow.com/search?query=$q"
        "instamart" -> "https://www.swiggy.com/instamart/search?custom_back=true&query=$q"
        "bigbasket" -> "https://www.bigbasket.com/ps/?q=$q"
        "flipkart" -> "https://www.flipkart.com/search?q=$q"
        "amazon" -> "https://www.amazon.in/s?k=$q"
        else -> null
    }
}
