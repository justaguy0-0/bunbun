package com.example.bunbun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.bunbun.navigation.BunbunApp
import com.example.bunbun.ui.theme.BunbunTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = AppContainer(applicationContext)
        setContent { BunbunTheme { BunbunApp(container.repository) } }
    }
}

