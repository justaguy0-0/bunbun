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
import com.example.bunbun.data.model.ChatDto
import com.example.bunbun.data.model.MessageDto
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
                    "chats" -> ChatsScreen(
                        currentUser = currentUser,
                        state = ChatsUiState(chats = previewChats, loading = false),
                        onRefresh = {},
                        onSearch = {},
                        onChat = {},
                        onLogout = {},
                    )
                    "search" -> SearchScreen(SearchUiState(), {}, { _, _, _ -> }, {}, {})
                    "chat" -> PreviewChatScreen()
                    else -> LoginScreen(AuthUiState(), { _, _, _ -> }, {}, {})
                }
            }
        }
    }

    @Composable
    private fun PreviewChatScreen() {
        var state by remember { mutableStateOf(ChatUiState(messages = previewMessages, loading = false)) }
        ChatScreen(
            peerName = peer.displayName,
            currentUserId = currentUser.id,
            state = state,
            onDraft = { state = state.copy(draft = it) },
            onSend = {
                val text = state.draft.trim()
                if (text.isNotEmpty()) {
                    val nextId = (state.messages.maxOfOrNull(MessageDto::id) ?: 0L) + 1L
                    state = state.copy(
                        messages = state.messages + MessageDto(
                            id = nextId,
                            chatId = 10,
                            senderId = currentUser.id,
                            text = text,
                            createdAt = "2026-08-08 18:45:00",
                        ),
                        draft = "",
                    )
                }
            },
            onRetry = {},
            onBack = {},
        )
    }

    private companion object {
        val currentUser = UserDto(1, "bunbun_operator", "Илья", "2026-08-08 12:00:00")
        val peer = UserDto(2, "quiet_signal", "Мира Соль", "2026-08-08 12:00:00")
        val secondPeer = UserDto(3, "north_node", "Алексей Север", "2026-08-08 12:00:00")
        val previewMessages = listOf(
            MessageDto(1, 10, peer.id, "Обычное сообщение без ссылки. Линия открыта.", "2026-08-08 18:40:00"),
            MessageDto(2, 10, peer.id, "Посмотри вот это https://picnic-bk.ru/ — прикольная штука.", "2026-08-08 18:41:00"),
            MessageDto(3, 10, currentUser.id, "Вот две ссылки: https://example.com и https://picnic-bk.ru", "2026-08-08 18:42:00"),
            MessageDto(4, 10, peer.id, "Ссылка в скобках: (https://example.com). Без схемы тоже работает: picnic-bk.ru", "2026-08-08 18:43:00"),
            MessageDto(5, 10, currentUser.id, "Длинный маршрут: https://example.com/some/really/long/path?utm_source=test&something=verylong", "2026-08-08 18:44:00"),
        )
        val previewChats = listOf(
            ChatDto(10, "direct", peer, previewMessages.last(), 2),
            ChatDto(
                11,
                "direct",
                secondPeer,
                MessageDto(4, 11, secondPeer.id, "Отправлю заметки после вечерней сверки.", "2026-08-08 17:18:00"),
            ),
            ChatDto(12, "direct", UserDto(4, "long_identity_name", "Очень длинное отображаемое имя для проверки вёрстки", "2026-08-08 12:00:00")),
        )
    }
}
