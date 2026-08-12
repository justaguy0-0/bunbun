package com.example.bunbun.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bunbun.Config
import com.example.bunbun.data.model.MessageDto
import com.example.bunbun.data.repository.BunbunRepository
import com.example.bunbun.ui.common.lastMessageId
import com.example.bunbun.ui.common.mergeMessages
import com.example.bunbun.ui.common.asUiError
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<MessageDto> = emptyList(),
    val draft: String = "",
    val loading: Boolean = true,
    val sending: Boolean = false,
    val error: String? = null,
)

class ChatViewModel(private val chatId: Long, private val repository: BunbunRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = mutableState.asStateFlow()

    init {
        loadHistory()
        viewModelScope.launch {
            while (isActive) {
                delay(Config.POLL_INTERVAL_MS)
                poll()
            }
        }
    }

    fun setDraft(value: String) {
        if (value.length <= Config.MESSAGE_MAX_LENGTH) mutableState.update { it.copy(draft = value, error = null) }
    }

    fun retry() { if (mutableState.value.messages.isEmpty()) loadHistory() else viewModelScope.launch { poll() } }

    fun send() {
        val text = mutableState.value.draft.trim()
        if (text.isEmpty() || text.length > Config.MESSAGE_MAX_LENGTH || mutableState.value.sending) return
        mutableState.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.sendMessage(chatId, text) }
                .onSuccess { message ->
                    mutableState.update { it.copy(messages = mergeMessages(it.messages, listOf(message)), draft = "", sending = false) }
                    markLatestRead()
                }
                .onFailure { error -> mutableState.update { it.copy(sending = false, error = error.asUiError("SEND_UNKNOWN")) } }
        }
    }

    private fun loadHistory() {
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.messages(chatId) }
                .onSuccess { messages ->
                    mutableState.update { it.copy(messages = mergeMessages(it.messages, messages), loading = false) }
                    markLatestRead()
                }
                .onFailure { error -> mutableState.update { it.copy(loading = false, error = error.asUiError("LOAD_MESSAGES_UNKNOWN")) } }
        }
    }

    private suspend fun poll() {
        val afterId = lastMessageId(mutableState.value.messages) ?: 0L
        runCatching { repository.messages(chatId, afterId) }
            .onSuccess { incoming ->
                if (incoming.isNotEmpty()) {
                    mutableState.update { it.copy(messages = mergeMessages(it.messages, incoming), error = null) }
                    markLatestRead()
                }
            }
            .onFailure { error -> mutableState.update { it.copy(error = error.asUiError("POLLING_UNKNOWN")) } }
    }

    private fun markLatestRead() {
        val id = lastMessageId(mutableState.value.messages) ?: return
        viewModelScope.launch { runCatching { repository.markRead(chatId, id) } }
    }
}
