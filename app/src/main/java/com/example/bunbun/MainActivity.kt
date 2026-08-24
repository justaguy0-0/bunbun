package com.example.bunbun

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.example.bunbun.navigation.BunbunApp
import com.example.bunbun.ui.theme.BunbunTheme

class MainActivity : ComponentActivity() {
    private val container get() = (application as BunbunApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handlePushIntent(intent)
        setContent {
            BunbunTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    BunbunApp(container)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        container.foregroundChatTracker.setForeground(true)
        container.presenceSynchronizer.start()
    }

    override fun onStop() {
        container.presenceSynchronizer.stop()
        container.foregroundChatTracker.setForeground(false)
        super.onStop()
    }

    private fun handlePushIntent(intent: Intent?) {
        com.example.bunbun.push.ChatNavigationTarget.fromIntent(intent)?.let(container.pendingChatNavigation::post)
    }
}
