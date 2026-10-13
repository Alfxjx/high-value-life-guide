package com.hvlg.guide

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hvlg.guide.ui.ReaderApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        // 内容离线加载很快，开屏会一闪而过；固定停留约 2 秒再退场
        val splashReleaseAt = SystemClock.uptimeMillis() + 2000L
        splashScreen.setKeepOnScreenCondition { SystemClock.uptimeMillis() < splashReleaseAt }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { ReaderApp() }
    }
}
