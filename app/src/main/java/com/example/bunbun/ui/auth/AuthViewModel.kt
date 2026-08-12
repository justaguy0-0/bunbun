package com.example.bunbun.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bunbun.data.model.UserDto
import com.example.bunbun.data.repository.BunbunRepository
import com.example.bunbun.ui.common.asUiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(val loading: Boolean = false, val error: String? = null)

class AuthViewModel(private val repository: BunbunRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = mutableState.asStateFlow()

    fun login(username: String, password: String, onSuccess: (UserDto) -> Unit) {
        submit({ repository.login(username, password) }, onSuccess)
    }

    fun register(username: String, displayName: String, password: String, onSuccess: (UserDto) -> Unit) {
        if (!USERNAME.matches(username.trim().lowercase())) {
            mutableState.update { it.copy(error = "LOCAL_USERNAME_INVALID") }
            return
        }
        if (displayName.trim().isEmpty() || displayName.trim().length > 64) {
            mutableState.update { it.copy(error = "LOCAL_DISPLAY_NAME_INVALID") }
            return
        }
        if (password.length !in 8..72) {
            mutableState.update { it.copy(error = "LOCAL_PASSWORD_INVALID") }
            return
        }
        submit({ repository.register(username, displayName, password) }, onSuccess)
    }

    private fun submit(request: suspend () -> UserDto, onSuccess: (UserDto) -> Unit) {
        if (mutableState.value.loading) return
        mutableState.value = AuthUiState(loading = true)
        viewModelScope.launch {
            runCatching { request() }
                .onSuccess { user -> mutableState.value = AuthUiState(); onSuccess(user) }
                .onFailure { mutableState.value = AuthUiState(error = it.asUiError("AUTH_UNKNOWN")) }
        }
    }

    private companion object { val USERNAME = Regex("^[a-z0-9_]{3,32}$") }
}
