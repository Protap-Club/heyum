package com.pgstorm148.heyum

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pgstorm148.heyum.firebase.AndroidActivityHolder
import com.pgstorm148.heyum.ui.App
import com.pgstorm148.heyum.model.NotificationPayload

class MainActivity : ComponentActivity() {
    private var activePayload by mutableStateOf<NotificationPayload?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AndroidActivityHolder.setActivity(this)

        handleIntent(intent)

        setContent {
            App(activePayload)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        AndroidActivityHolder.setActivity(this)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        AndroidActivityHolder.clearActivity(this)
        super.onDestroy()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val type = intent.getStringExtra("notification_type")
        if (type != null) {
            activePayload = NotificationPayload(
                type = type,
                dishId = intent.getStringExtra("dish_id"),
                groupId = intent.getStringExtra("group_id"),
                ingredient = intent.getStringExtra("ingredient")
            )
            // Clear extras to avoid re-triggering on rotation/recreation
            intent.removeExtra("notification_type")
            intent.removeExtra("dish_id")
            intent.removeExtra("group_id")
            intent.removeExtra("ingredient")
        }
    }
}