package com.pgstorm148.heyum

import androidx.compose.ui.window.ComposeUIViewController
import com.pgstorm148.heyum.ui.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    App()
}
