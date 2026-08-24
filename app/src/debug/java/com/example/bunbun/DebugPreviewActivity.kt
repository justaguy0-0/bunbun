package com.example.bunbun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.bunbun.data.local.CachedChat
import com.example.bunbun.data.local.CachedChatPreview
import com.example.bunbun.data.local.CachedMessage
import com.example.bunbun.data.local.MessageSendState
import com.example.bunbun.data.model.UserDto
import com.example.bunbun.ui.auth.AuthUiState
import com.example.bunbun.ui.auth.LoginScreen
import com.example.bunbun.ui.auth.RegisterScreen
import com.example.bunbun.ui.chat.ChatScreen
import com.example.bunbun.ui.chat.ChatUiState
import com.example.bunbun.ui.chats.ChatsScreen
import com.example.bunbun.ui.chats.ChatsUiState
import com.example.bunbun.ui.search.SearchScreen
import com.example.bunbun.ui.search.SearchUiState
import com.example.bunbun.ui.theme.BunbunTheme
import java.time.Instant

/** Debug-only visual gallery entry point; never merged into release builds. */
class DebugPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val screen = intent.getStringExtra("screen") ?: "login"
        setContent {
            BunbunTheme {
                when (screen) {
                    "register" -> RegisterScreen(AuthUiState(), { _, _, _, _ -> }, {}, {})
                    "chats" -> ChatsScreen(currentUser, ChatsUiState(previewChats, loading = false), {}, {}, {}, {})
                    "search" -> SearchScreen(SearchUiState(), {}, { _, _, _ -> }, {}, {})
                    "chat" -> PreviewChatScreen()
                    else -> LoginScreen(AuthUiState(), { _, _, _ -> }, {}, {})
                }
            }
        }
    }

    @Composable
    private fun PreviewChatScreen() {
        var state by remember {
            mutableStateOf(
                ChatUiState(
                    messages = previewMessages,
                    peerLastSeenAtMillis = Instant.parse("2026-08-24T07:42:00Z").toEpochMilli(),
                    loading = false,
                ),
            )
        }
        ChatScreen(
            peerName = peer.displayName,
            state = state,
            onDraft = { state = state.copy(draft = it) },
            onSend = {
                val text = state.draft.trim()
                if (text.isNotEmpty()) {
                    val localId = "preview-${state.messages.size + 1}"
                    state = state.copy(
                        messages = state.messages + CachedMessage(
                            localId, null, localId, 10, currentUser.id, text,
                            System.currentTimeMillis(), true, MessageSendState.PENDING, false, null,
                        ),
                        draft = "",
                    )
                }
            },
            onRetry = {},
            onRetryMessage = {},
            onBack = {},
        )
    }

    private companion object {
        val currentUser = UserDto(1, "bunbun_operator", "Илья", "2026-08-08T12:00:00Z")
        val peer = UserDto(2, "quiet_signal", "Мира Соль", "2026-08-08T12:00:00Z")
        val previewMessages = listOf(
            cached("m1", 1, peer.id, "Обычное сообщение без ссылки.", "2026-08-07T18:40:00Z", false),
            cached("m2", 2, peer.id, "Посмотри https://picnic-bk.ru/", "2026-08-08T18:41:00Z", false),
            cached("m3", 3, currentUser.id, "Сообщение прочитано", "2026-08-08T18:42:00Z", true, MessageSendState.READ),
            cached("m4", null, currentUser.id, "Сообщение пока в очереди", "2026-08-08T18:44:00Z", true, MessageSendState.PENDING),
        )
        val previewChats = listOf(
            CachedChat(
                id = 10,
                type = "direct",
                peerUserId = peer.id,
                peerUsername = peer.username,
                peerDisplayName = peer.displayName,
                peerLastSeenAtMillis = Instant.parse("2026-08-24T07:42:00Z").toEpochMilli(),
                lastMessage = CachedChatPreview(null, "m4", "Сообщение пока в очереди", currentUser.id, previewMessages.last().createdAtMillis, MessageSendState.PENDING),
                unreadCount = 2,
            ),
        )

        fun cached(
            localId: String,
            serverId: Long?,
            senderId: Long,
            text: String,
            instant: String,
            mine: Boolean,
            state: MessageSendState = MessageSendState.SENT,
        ) = CachedMessage(
            localId, serverId, if (mine) localId else null, 10, senderId, text,
            Instant.parse(instant).toEpochMilli(), mine, state, state == MessageSendState.READ, null,
        )
    }
}
