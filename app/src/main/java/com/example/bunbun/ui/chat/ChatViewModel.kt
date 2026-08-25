package com.example.bunbun.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bunbun.Config
import com.example.bunbun.data.local.CachedChat
import com.example.bunbun.data.local.CachedMessage
import com.example.bunbun.data.repository.BunbunRepository
import com.example.bunbun.ui.common.asUiError
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<CachedMessage> = emptyList(),
    val peerLastSeenAtMillis: Long? = null,
    val draft: String = "",
    val loading: Boolean = true,
    val saving: Boolean = false,
    val offline: Boolean = false,
    val error: String? = null,
    val unreadDivider: UnreadDividerUiState = UnreadDividerUiState(),
)

class ChatViewModel(
    private val accountId: Long,
    private val chatId: Long,
    private val repository: BunbunRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = mutableState.asStateFlow()
    private var readJob: Job? = null
    private var dividerHideJob: Job? = null
    private var lastReadAttempt = 0L
    private val unreadDividerSession = UnreadDividerSession()
    private var initialDataReady = false

    init {
        viewModelScope.launch {
            combine(
                repository.observeChat(accountId, chatId),
                repository.observeMessages(accountId, chatId),
                ::Pair,
            ).collect { (chat, messages) ->
                applySnapshot(chat, messages)
            }
        }
        viewModelScope.launch {
            coroutineScope {
                val chatsSync = async { runCatching { repository.syncChats(accountId) } }
                val messagesSync = async { syncMessages() }
                chatsSync.await()
                messagesSync.await()
            }
            val (chat, messages) = combine(
                repository.observeChat(accountId, chatId),
                repository.observeMessages(accountId, chatId),
                ::Pair,
            ).first()
            initialDataReady = true
            applySnapshot(chat, messages)
        }
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

    private fun synchronize() {
        viewModelScope.launch { syncMessages() }
    }

    private suspend fun syncMessages() {
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

    private fun applySnapshot(chat: CachedChat?, messages: List<CachedMessage>) {
        if (chat != null) {
            unreadDividerSession.capture(
                messages = messages,
                currentUserId = accountId,
                myLastReadMessageId = chat.myLastReadMessageId,
                initialDataReady = initialDataReady,
            )
        }
        mutableState.update {
            it.copy(
                messages = messages,
                peerLastSeenAtMillis = chat?.peerLastSeenAtMillis,
                loading = it.loading && messages.isEmpty(),
                unreadDivider = unreadDividerSession.uiState,
            )
        }
        if (unreadDividerSession.hasCaptured) markLatestRead(messages)
    }

    private fun startDividerHideTimer() {
        if (dividerHideJob?.isActive == true) return
        viewModelScope.launch {
            delay(UNREAD_DIVIDER_HOLD_MILLIS)
            mutableState.update { it.copy(unreadDivider = unreadDividerSession.onHoldTimeout()) }
            delay(UNREAD_DIVIDER_FADE_MILLIS)
            mutableState.update { it.copy(unreadDivider = unreadDividerSession.finishFade()) }
        }.also { dividerHideJob = it }
    }

    private fun markLatestRead(messages: List<CachedMessage>) {
        val latestServerId = messages.maxOfOrNull { it.serverId ?: 0L } ?: return
        if (latestServerId <= 0L || latestServerId <= lastReadAttempt || readJob?.isActive == true) return
        lastReadAttempt = latestServerId
        readJob = viewModelScope.launch {
            runCatching { repository.markRead(accountId, chatId, latestServerId) }
                .onSuccess {
                    if (unreadDividerSession.onReadConfirmed()) startDividerHideTimer()
                }
                .onFailure { lastReadAttempt = 0L }
        }
    }
}

internal const val UNREAD_DIVIDER_HOLD_MILLIS = 5_000L
internal const val UNREAD_DIVIDER_FADE_MILLIS = 300L
