package com.pgstorm148.heyum.ui.screens

import android.content.Intent
import com.pgstorm148.heyum.firebase.AndroidActivityHolder

actual fun shareText(text: String) {
    val activity = AndroidActivityHolder.getActivity() ?: return
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    activity.startActivity(Intent.createChooser(sendIntent, "Share invite via"))
}
