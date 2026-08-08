package com.pgstorm148.heyum

import androidx.compose.ui.window.ComposeUIViewController
import com.pgstorm148.heyum.model.NotificationPayload
import com.pgstorm148.heyum.ui.App
import platform.UIKit.UIViewController

/**
 * Entry point for the iOS host app.
 *
 * [payloadType]/[payloadId] mirror Android's notification-extras route: a widget tap arrives as a
 * URL in Swift, which passes the parsed values through here so the shared navigation logic in
 * WelcomeScreen can act on it (craving_log / meal_log / group).
 */
fun MainViewController(
    payloadType: String? = null,
    payloadId: String? = null
): UIViewController = ComposeUIViewController {
    val payload = payloadType?.let {
        // "group" routes on groupId; the log panes carry no id at all.
        NotificationPayload(type = it, groupId = payloadId)
    }
    App(payload)
}
