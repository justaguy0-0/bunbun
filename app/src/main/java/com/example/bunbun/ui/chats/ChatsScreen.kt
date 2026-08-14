package com.example.bunbun.ui.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bunbun.R
import com.example.bunbun.data.local.CachedChat
import com.example.bunbun.data.local.MessageSendState
import com.example.bunbun.data.model.UserDto
import com.example.bunbun.ui.common.PressableTerminalRow
import com.example.bunbun.ui.common.TerminalBadge
import com.example.bunbun.ui.common.TerminalError
import com.example.bunbun.ui.common.TerminalIconButton
import com.example.bunbun.ui.common.TerminalScreen
import com.example.bunbun.ui.common.TerminalSectionLabel
import com.example.bunbun.ui.common.TerminalState
import com.example.bunbun.ui.common.TerminalTextAction
import com.example.bunbun.ui.common.TerminalTopBar
import com.example.bunbun.ui.common.localizedErrorMessage

@Composable
fun ChatsScreen(
    currentUser: UserDto,
    state: ChatsUiState,
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onChat: (CachedChat) -> Unit,
    onLogout: () -> Unit,
) {
    TerminalScreen {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            TerminalTopBar(
                title = stringResource(R.string.chats_title),
                subtitle = stringResource(R.string.chats_subtitle, currentUser.username),
                actions = {
                    TerminalIconButton("⌕", stringResource(R.string.chats_find_people_description), onSearch)
                    TerminalIconButton("×", stringResource(R.string.chats_logout_description), onLogout)
                },
            )
            if (state.offline && state.chats.isNotEmpty()) {
                Text(
                    stringResource(R.string.common_offline_cached),
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)).padding(6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TerminalSectionLabel(
                    label = stringResource(R.string.chats_section),
                    detail = state.chats.size.toString().padStart(2, '0'),
                    modifier = Modifier.weight(1f),
                )
                TerminalTextAction(
                    if (state.refreshing) stringResource(R.string.common_syncing) else stringResource(R.string.common_sync),
                    onRefresh,
                    !state.refreshing,
                )
            }

            when {
                state.loading -> TerminalState(
                    code = stringResource(R.string.chats_loading_code),
                    title = stringResource(R.string.chats_loading_title),
                    message = stringResource(R.string.chats_loading_message),
                    loading = true,
                    modifier = Modifier.fillMaxSize(),
                )

                state.error != null && state.chats.isEmpty() -> TerminalState(
                    code = stringResource(R.string.chats_error_code),
                    title = stringResource(R.string.chats_error_title),
                    message = if (state.offline) stringResource(R.string.first_connection_required) else localizedErrorMessage(state.error),
                    actionLabel = stringResource(R.string.chats_retry),
                    onAction = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 18.dp),
                ) {
                    state.error?.let { message ->
                        item { TerminalError(message, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                    }
                    if (state.chats.isEmpty()) {
                        item {
                            TerminalState(
                                code = stringResource(R.string.chats_empty_code),
                                title = stringResource(R.string.chats_empty_title),
                                message = stringResource(R.string.chats_empty_message),
                                actionLabel = stringResource(R.string.chats_find_action),
                                onAction = onSearch,
                                modifier = Modifier.fillParentMaxSize(),
                            )
                        }
                    }
                    items(state.chats, key = { it.id }) { chat ->
                        ChatRow(chat = chat, onClick = { onChat(chat) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRow(chat: CachedChat, onClick: () -> Unit) {
    PressableTerminalRow(onClick = onClick) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.09f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        chat.peerDisplayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(
                            chat.peerDisplayName,
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (chat.unreadCount > 0) TerminalBadge(chat.unreadCount.coerceAtMost(99).toString())
                    }
                    Text(
                        chat.lastMessage?.text ?: stringResource(R.string.chats_no_messages),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (chat.lastMessage?.sendState in setOf(MessageSendState.PENDING, MessageSendState.SENDING)) {
                        Text(
                            stringResource(R.string.chats_pending_preview),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        compactTime(chat.lastMessage?.createdAtMillis) ?: stringResource(R.string.chats_new),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text("→", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)))
        }
    }
}

private fun compactTime(timestampMillis: Long?): String? = timestampMillis?.let {
    java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        .format(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()))
}
