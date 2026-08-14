package com.example.bunbun.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bunbun.data.local.CachedChat
import com.example.bunbun.data.repository.BunbunRepository
import com.example.bunbun.ui.common.asUiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatsUiState(
    val chats: List<CachedChat> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val offline: Boolean = false,
    val error: String? = null,
)

class ChatsViewModel(
    private val accountId: Long,
    private val repository: BunbunRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ChatsUiState())
    val state: StateFlow<ChatsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeChats(accountId).collect { chats ->
                mutableState.update { current ->
                    current.copy(chats = chats, loading = current.loading && chats.isEmpty())
                }
            }
        }
        refresh(initial = true)
    }

    fun refresh(initial: Boolean = false) {
        mutableState.update {
            it.copy(
                loading = initial && it.chats.isEmpty(),
                refreshing = !initial,
                error = null,
            )
        }
        viewModelScope.launch {
            runCatching { repository.syncChats(accountId) }
                .onSuccess { mutableState.update { it.copy(loading = false, refreshing = false, offline = false) } }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            offline = true,
                            error = if (it.chats.isEmpty()) error.asUiError("FIRST_SYNC_REQUIRED") else null,
                        )
                    }
                }
        }
    }
}
