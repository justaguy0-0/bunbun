package com.example.bunbun.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bunbun.Config
import com.example.bunbun.R
import com.example.bunbun.data.model.MessageDto
import com.example.bunbun.ui.common.LinkifiedMessageText
import com.example.bunbun.ui.common.TerminalError
import com.example.bunbun.ui.common.TerminalScreen
import com.example.bunbun.ui.common.TerminalSectionLabel
import com.example.bunbun.ui.common.TerminalState
import com.example.bunbun.ui.common.TerminalTextField
import com.example.bunbun.ui.common.TerminalTopBar

@Composable
fun ChatScreen(
    peerName: String,
    currentUserId: Long,
    state: ChatUiState,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.lastOrNull()?.id) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    TerminalScreen {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            TerminalTopBar(
                title = peerName,
                subtitle = stringResource(R.string.chat_subtitle),
                onBack = onBack,
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> TerminalState(
                        code = stringResource(R.string.chat_loading_code),
                        title = stringResource(R.string.chat_loading_title),
                        message = stringResource(R.string.chat_loading_message),
                        loading = true,
                        modifier = Modifier.fillMaxSize(),
                    )

                    state.error != null && state.messages.isEmpty() -> TerminalState(
                        code = stringResource(R.string.chat_error_code),
                        title = stringResource(R.string.chat_error_title),
                        message = com.example.bunbun.ui.common.localizedErrorMessage(state.error),
                        actionLabel = stringResource(R.string.chat_retry),
                        onAction = onRetry,
                        modifier = Modifier.fillMaxSize(),
                    )

                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (state.messages.isEmpty()) {
                            item {
                                TerminalState(
                                    code = stringResource(R.string.chat_empty_code),
                                    title = stringResource(R.string.chat_empty_title),
                                    message = stringResource(R.string.chat_empty_message),
                                    modifier = Modifier.fillParentMaxSize(),
                                )
                            }
                        }
                        items(state.messages, key = { it.id }) { message ->
                            MessageBubble(message = message, mine = message.senderId == currentUserId)
                        }
                    }
                }
            }

            ComposerDock(
                draft = state.draft,
                sending = state.sending,
                error = if (state.messages.isNotEmpty()) state.error else null,
                onDraft = onDraft,
                onSend = onSend,
            )
        }
    }
}

@Composable
private fun MessageBubble(message: MessageDto, mine: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.84f),
            color = if (mine) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(
                1.dp,
                if (mine) MaterialTheme.colorScheme.primary.copy(alpha = 0.58f) else MaterialTheme.colorScheme.outline,
            ),
        ) {
            Column(Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (mine) stringResource(R.string.chat_you_transmit)
                        else stringResource(R.string.chat_peer_receive),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        compactMessageTime(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(7.dp))
                LinkifiedMessageText(message.text)
            }
        }
    }
}

@Composable
private fun ComposerDock(
    draft: String,
    sending: Boolean,
    error: String?,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
) {
    val sendDescription = stringResource(R.string.chat_send_description)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        error?.let {
            TerminalError(it)
            Spacer(Modifier.height(8.dp))
        }
        TerminalSectionLabel(
            label = stringResource(R.string.chat_new_message),
            detail = "${draft.length}/${Config.MESSAGE_MAX_LENGTH}",
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            TerminalTextField(
                value = draft,
                onValueChange = onDraft,
                label = stringResource(R.string.chat_message),
                enabled = !sending,
                singleLine = false,
                minLines = 1,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (draft.isNotBlank() && !sending) onSend() }),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onSend,
                enabled = draft.isNotBlank() && !sending,
                modifier = Modifier
                    .size(56.dp)
                    .semantics { contentDescription = sendDescription },
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    if (sending) "…" else "↑",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

private fun compactMessageTime(raw: String): String {
    val time = raw.substringAfter(' ', missingDelimiterValue = raw)
    return time.take(5).ifBlank { "--:--" }
}
