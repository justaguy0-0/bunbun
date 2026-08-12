package com.example.bunbun

import android.content.Context
import com.example.bunbun.data.api.NetworkModule
import com.example.bunbun.data.local.EncryptedTokenStore
import com.example.bunbun.data.local.SessionManager
import com.example.bunbun.data.repository.BunbunRepository

class AppContainer(context: Context) {
    private val sessions = SessionManager(EncryptedTokenStore(context.applicationContext))
    val repository = BunbunRepository(NetworkModule.create(sessions), sessions)
}

