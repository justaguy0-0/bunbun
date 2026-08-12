package com.example.bunbun.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bunbun.data.model.ChatDto
import com.example.bunbun.data.repository.BunbunRepository
import com.example.bunbun.ui.common.asUiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatsUiState(
    val chats: List<ChatDto> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
)

class ChatsViewModel(private val repository: BunbunRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ChatsUiState())
    val state: StateFlow<ChatsUiState> = mutableState.asStateFlow()

    init { refresh(initial = true) }

    fun refresh(initial: Boolean = false) {
        mutableState.value = mutableState.value.copy(
            loading = initial && mutableState.value.chats.isEmpty(),
            refreshing = !initial,
            error = null,
        )
        viewModelScope.launch {
            runCatching { repository.chats() }
                .onSuccess { mutableState.value = ChatsUiState(chats = it, loading = false) }
                .onFailure { mutableState.value = mutableState.value.copy(loading = false, refreshing = false, error = it.asUiError("CHATS_UNKNOWN")) }
        }
    }
}
