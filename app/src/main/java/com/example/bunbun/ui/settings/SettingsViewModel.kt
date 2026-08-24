package com.example.bunbun.ui.settings

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

data class SettingsUiState(
    val user: UserDto,
    val showNameDialog: Boolean = false,
    val displayNameDraft: String = user.displayName,
    val savingName: Boolean = false,
    val profileError: String? = null,
    val showLogoutDialog: Boolean = false,
    val loggingOut: Boolean = false,
    val logoutError: String? = null,
    val pendingMessageCount: Int? = null,
)

class SettingsViewModel(
    currentUser: UserDto,
    private val repository: BunbunRepository,
    private val onProfileUpdated: (UserDto) -> Unit,
    private val logout: suspend () -> Unit,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState(currentUser))
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.pendingMessageCount(currentUser.id) }
                .onSuccess { count -> mutableState.update { it.copy(pendingMessageCount = count) } }
        }
    }

    fun showNameDialog() {
        mutableState.update {
            it.copy(showNameDialog = true, displayNameDraft = it.user.displayName, profileError = null)
        }
    }

    fun dismissNameDialog() {
        if (!mutableState.value.savingName) {
            mutableState.update { it.copy(showNameDialog = false, profileError = null) }
        }
    }

    fun changeDisplayName(value: String) {
        mutableState.update { it.copy(displayNameDraft = value, profileError = null) }
    }

    fun saveDisplayName() {
        val snapshot = mutableState.value
        if (snapshot.savingName) return
        val normalized = snapshot.displayNameDraft.trim()
        if (normalized.isEmpty() || normalized.codePointCount(0, normalized.length) > 64) {
            mutableState.update { it.copy(profileError = "LOCAL_DISPLAY_NAME_INVALID") }
            return
        }
        mutableState.update { it.copy(savingName = true, profileError = null) }
        viewModelScope.launch {
            runCatching { repository.updateDisplayName(normalized) }
                .onSuccess { updated ->
                    mutableState.update {
                        it.copy(
                            user = updated,
                            displayNameDraft = updated.displayName,
                            showNameDialog = false,
                            savingName = false,
                            profileError = null,
                        )
                    }
                    onProfileUpdated(updated)
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(savingName = false, profileError = error.asUiError("PROFILE_UPDATE_UNKNOWN"))
                    }
                }
        }
    }

    fun showLogoutDialog() {
        mutableState.update { it.copy(showLogoutDialog = true, logoutError = null) }
    }

    fun dismissLogoutDialog() {
        if (!mutableState.value.loggingOut) {
            mutableState.update { it.copy(showLogoutDialog = false, logoutError = null) }
        }
    }

    fun confirmLogout() {
        if (mutableState.value.loggingOut) return
        mutableState.update { it.copy(loggingOut = true, logoutError = null) }
        viewModelScope.launch {
            runCatching { logout() }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(loggingOut = false, logoutError = error.asUiError("LOCAL_LOGOUT_FAILED"))
                    }
                }
        }
    }
}

internal fun versionLabel(versionName: String): String = "Bunbun $versionName"
