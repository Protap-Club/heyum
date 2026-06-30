package com.pgstorm148.heyum.ui

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.pgstorm148.heyum.ui.screens.WelcomeScreen
import com.pgstorm148.heyum.ui.theme.YumTheme
import com.pgstorm148.heyum.model.NotificationPayload

@Composable
fun App(initialNotificationPayload: NotificationPayload? = null) {
    YumTheme {
        Navigator(WelcomeScreen(initialNotificationPayload)) { navigator ->
            SlideTransition(navigator)
        }
    }
}
