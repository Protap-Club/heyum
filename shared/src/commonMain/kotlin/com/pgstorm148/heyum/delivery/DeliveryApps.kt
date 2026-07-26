package com.pgstorm148.heyum.delivery

import heyum.shared.generated.resources.Res
import heyum.shared.generated.resources.logo_amazon
import heyum.shared.generated.resources.logo_bigbasket
import heyum.shared.generated.resources.logo_blinkit
import heyum.shared.generated.resources.logo_eatclub
import heyum.shared.generated.resources.logo_flipkart
import heyum.shared.generated.resources.logo_instamart
import heyum.shared.generated.resources.logo_ownly
import heyum.shared.generated.resources.logo_swiggy
import heyum.shared.generated.resources.logo_swish
import heyum.shared.generated.resources.logo_toing
import heyum.shared.generated.resources.logo_zepto
import heyum.shared.generated.resources.logo_zomato
import org.jetbrains.compose.resources.DrawableResource

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
    val packageName: String = "",
    val webFallbackUrl: String,
    val category: DeliveryCategory = DeliveryCategory.FOOD_DELIVERY,
    val emoji: String = "🛵",
    // Used only to detect installation via canOpenURL on iOS (see DeliveryApps.ios.kt) — never
    // used to actually open the app, so a wrong value just means a row is mis-shown/hidden.
    val iosUrlScheme: String? = null
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

/**
 * Real app icon (fetched from each app's official App Store listing), for use instead of
 * [DeliveryApp.emoji] wherever the UI has room for an image. Null for an id with no bundled
 * logo, in which case the caller should fall back to the emoji.
 */
fun DeliveryApp.logoResource(): DrawableResource? = when (id) {
    "swiggy" -> Res.drawable.logo_swiggy
    "zomato" -> Res.drawable.logo_zomato
    "zepto", "zeptocafe" -> Res.drawable.logo_zepto
    "blinkit" -> Res.drawable.logo_blinkit
    "instamart" -> Res.drawable.logo_instamart
    "bigbasket" -> Res.drawable.logo_bigbasket
    "flipkart" -> Res.drawable.logo_flipkart
    "amazon" -> Res.drawable.logo_amazon
    "eatclub" -> Res.drawable.logo_eatclub
    "swish" -> Res.drawable.logo_swish
    "toing" -> Res.drawable.logo_toing
    "ownly" -> Res.drawable.logo_ownly
    else -> null
}

private fun encodeQuery(text: String): String = text.trim().replace(" ", "+")

/**
 * Search deep link (https app-link) for a finished dish within a food-delivery app, or null when
 * the app has no reliable search endpoint — in that case the caller should just launch the app
 * home. These https URLs double as the apps' verified app-links / universal links, so
 * [openDeliveryApp] can route them into the app on both platforms.
 *
 * No id currently maps to a search URL here: checked against each app's real
 * apple-app-site-association file (the file iOS itself uses to decide whether a URL opens the app
 * or Safari). Swiggy and BigBasket don't allowlist `/search` at all, so it never even reaches the
 * app — straight to Safari. Zomato's AASA does allowlist it system-wide, but the app has no
 * in-app screen for that route and bounces back out to a browser once opened (reported directly:
 * "opens the zomato app but then takes to zomato.com in browser"). Landing reliably on the app's
 * home screen beats a search link that silently fails or bounces, so this returns null for
 * everyone until a per-app deep link path is found and verified to actually work end-to-end.
 */
fun DeliveryApp.dishSearchUrl(dishName: String): String? = null

/**
 * Grocery search deep link for a single ingredient within a quick-commerce app, or null when the
 * app has no reliable search endpoint (caller launches the app home instead). Deliberately one
 * ingredient at a time — a combined multi-word query (e.g. "tomato onion paneer") doesn't match a
 * real product in these apps' search, so the UI searches each missing ingredient separately.
 *
 * Every path below was checked against the app's real apple-app-site-association file (the file
 * iOS uses to decide whether a URL opens the app or Safari) to confirm it's actually allowlisted,
 * not just guessed: blinkit's "s" path with a wildcard suffix, zepto's full wildcard, instamart's
 * "instamart" path with a wildcard suffix, flipkart's full wildcard, and amazon's "s" path are all
 * covered. bigbasket's AASA does NOT allowlist "ps" (or any general search path) — that link would
 * silently fail straight to Safari — so it's omitted below and falls back to the app's
 * (allowlisted) home screen instead.
 */
fun DeliveryApp.grocerySearchUrl(ingredient: String): String? {
    val q = encodeQuery(ingredient)
    return when (id) {
        "blinkit" -> "https://blinkit.com/s/?q=$q"
        "zepto" -> "https://www.zeptonow.com/search?query=$q"
        "instamart" -> "https://www.swiggy.com/instamart/search?custom_back=true&query=$q"
        "flipkart" -> "https://www.flipkart.com/search?q=$q"
        "amazon" -> "https://www.amazon.in/s?k=$q"
        else -> null
    }
}
