package com.example.bunbun.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bunbun.Config
import com.example.bunbun.data.local.CachedMessage
import com.example.bunbun.data.repository.BunbunRepository
import com.example.bunbun.ui.common.asUiError
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<CachedMessage> = emptyList(),
    val draft: String = "",
    val loading: Boolean = true,
    val saving: Boolean = false,
    val offline: Boolean = false,
    val error: String? = null,
)

class ChatViewModel(
    private val accountId: Long,
    private val chatId: Long,
    private val repository: BunbunRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = mutableState.asStateFlow()
    private var readJob: Job? = null
    private var lastReadAttempt = 0L

    init {
        viewModelScope.launch {
            repository.observeMessages(accountId, chatId).collect { messages ->
                mutableState.update { it.copy(messages = messages, loading = it.loading && messages.isEmpty()) }
                markLatestRead(messages)
            }
        }
        synchronize(initial = true)
        viewModelScope.launch {
            while (isActive) {
                delay(Config.POLL_INTERVAL_MS)
                synchronize()
            }
        }
    }

    fun setDraft(value: String) {
        if (value.length <= Config.MESSAGE_MAX_LENGTH) mutableState.update { it.copy(draft = value, error = null) }
    }

    fun retry() = synchronize()

    fun send() {
        val text = mutableState.value.draft.trim()
        if (text.isEmpty() || text.length > Config.MESSAGE_MAX_LENGTH || mutableState.value.saving) return
        mutableState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.queueMessage(accountId, chatId, text) }
                .onSuccess {
                    mutableState.update { state -> state.copy(draft = "", saving = false) }
                    repository.drainOutbox()
                }
                .onFailure { error ->
                    mutableState.update { it.copy(saving = false, error = error.asUiError("LOCAL_OUTBOX_ERROR")) }
                }
        }
    }

    fun retryMessage(localId: String) {
        viewModelScope.launch {
            if (repository.retryMessage(accountId, localId)) repository.drainOutbox()
        }
    }

    private fun synchronize(initial: Boolean = false) {
        viewModelScope.launch {
            runCatching { repository.syncMessages(accountId, chatId) }
                .onSuccess { mutableState.update { it.copy(loading = false, offline = false, error = null) } }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            loading = false,
                            offline = true,
                            error = if (it.messages.isEmpty()) error.asUiError("LOAD_MESSAGES_UNKNOWN") else null,
                        )
                    }
                }
        }
    }

    private fun markLatestRead(messages: List<CachedMessage>) {
        val latestServerId = messages.maxOfOrNull { it.serverId ?: 0L } ?: return
        if (latestServerId <= 0L || latestServerId <= lastReadAttempt || readJob?.isActive == true) return
        lastReadAttempt = latestServerId
        readJob = viewModelScope.launch {
            runCatching { repository.markRead(accountId, chatId, latestServerId) }
                .onFailure { lastReadAttempt = 0L }
        }
    }
}
