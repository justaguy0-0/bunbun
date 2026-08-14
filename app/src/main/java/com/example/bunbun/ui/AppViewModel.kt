package com.example.bunbun.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bunbun.data.model.UserDto
import com.example.bunbun.data.repository.BunbunRepository
import com.example.bunbun.ui.common.asUiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SessionState {
    data object Checking : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: UserDto) : SessionState
    data class Error(val message: String) : SessionState
}

class AppViewModel(private val repository: BunbunRepository) : ViewModel() {
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Checking)
    val state: StateFlow<SessionState> = mutableState.asStateFlow()

    init { restore() }

    fun restore() {
        mutableState.value = SessionState.Checking
        viewModelScope.launch {
            runCatching { repository.restoreSession() }
                .onSuccess { user ->
                    mutableState.value = user?.let(SessionState::SignedIn) ?: SessionState.SignedOut
                    if (user != null) refreshSessionInBackground()
                }
                .onFailure { mutableState.value = SessionState.Error(it.asUiError("SESSION_RESTORE_UNKNOWN")) }
        }
    }

    fun signedIn(user: UserDto) { mutableState.value = SessionState.SignedIn(user) }

    fun logout() {
        viewModelScope.launch {
            runCatching { repository.logout() }
            mutableState.value = SessionState.SignedOut
        }
    }

    private fun refreshSessionInBackground() {
        viewModelScope.launch {
            runCatching { repository.refreshSession() }
                .onSuccess { user ->
                    mutableState.value = user?.let(SessionState::SignedIn) ?: SessionState.SignedOut
                }
            // A connectivity failure deliberately leaves the cached signed-in state visible.
        }
    }
}
