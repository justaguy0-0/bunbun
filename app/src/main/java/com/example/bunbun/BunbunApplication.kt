package com.example.bunbun

import android.app.Application
import com.example.bunbun.push.BunbunNotifications

class BunbunApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        BunbunNotifications.createChannel(this)
    }
}
