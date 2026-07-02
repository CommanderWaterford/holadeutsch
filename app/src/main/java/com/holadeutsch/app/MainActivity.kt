package com.holadeutsch.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.holadeutsch.app.ui.nav.HolaNavHost
import com.holadeutsch.app.ui.theme.HolaDeutschTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HolaDeutschTheme {
                HolaNavHost()
            }
        }
    }
}
