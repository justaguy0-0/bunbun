package com.example.bunbun.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.example.bunbun.data.local.CachedMessage
import com.example.bunbun.data.local.MessageSendState
import com.example.bunbun.ui.common.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChatScreen(
    peerName: String,
    state: ChatUiState,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onRetryMessage: (String) -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    val initialScrollState = remember { ChatInitialScrollState() }
    val timeline = remember(state.messages) { buildChatTimeline(state.messages) }
    LaunchedEffect(timeline.lastOrNull()?.key) {
        val initialTarget = initialScrollState.consumeTargetIndex(timeline.size)
        if (initialTarget != null) {
            listState.scrollToItem(initialTarget)
            return@LaunchedEffect
        }
        if (timeline.isEmpty()) return@LaunchedEffect

        val layoutInfo = listState.layoutInfo
        val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
        val wasNearBottom = !listState.canScrollForward ||
            lastVisibleIndex != null && lastVisibleIndex >= (layoutInfo.totalItemsCount - 2).coerceAtLeast(0)
        if (wasNearBottom) listState.animateScrollToItem(timeline.lastIndex)
    }

    TerminalScreen {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            TerminalTopBar(title = peerName, subtitle = stringResource(R.string.chat_subtitle), onBack = onBack)
            if (state.offline) OfflineStrip()

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
                        message = localizedErrorMessage(state.error),
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
                        if (timeline.isEmpty()) {
                            item("empty") {
                                TerminalState(
                                    code = stringResource(R.string.chat_empty_code),
                                    title = stringResource(R.string.chat_empty_title),
                                    message = stringResource(R.string.chat_empty_message),
                                    modifier = Modifier.fillParentMaxSize(),
                                )
                            }
                        }
                        items(timeline, key = ChatTimelineItem::key) { item ->
                            when (item) {
                                is ChatTimelineItem.DateHeader -> DateSeparator(item.label)
                                is ChatTimelineItem.Message -> MessageBubble(item.value, onRetryMessage)
                            }
                        }
                    }
                }
            }

            ComposerDock(
                draft = state.draft,
                saving = state.saving,
                error = if (state.messages.isNotEmpty()) state.error else null,
                onDraft = onDraft,
                onSend = onSend,
            )
        }
    }
}

@Composable
private fun OfflineStrip() {
    Text(
        stringResource(R.string.common_offline_cached),
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)).padding(6.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onErrorContainer,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun DateSeparator(label: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    }
}

@Composable
private fun MessageBubble(message: CachedMessage, onRetryMessage: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.84f),
            color = if (message.isMine) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(
                1.dp,
                if (message.isMine) MaterialTheme.colorScheme.primary.copy(alpha = 0.58f)
                else MaterialTheme.colorScheme.outline,
            ),
        ) {
            Column(Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (message.isMine) stringResource(R.string.chat_you_transmit)
                        else stringResource(R.string.chat_peer_receive),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (message.isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(compactMessageTime(message.createdAtMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (message.isMine) {
                        Spacer(Modifier.width(6.dp))
                        MessageStatus(message, onRetryMessage)
                    }
                }
                Spacer(Modifier.height(7.dp))
                LinkifiedMessageText(message.text)
            }
        }
    }
}

@Composable
private fun MessageStatus(message: CachedMessage, onRetryMessage: (String) -> Unit) {
    val description = when (message.sendState) {
        MessageSendState.PENDING, MessageSendState.SENDING -> stringResource(R.string.message_status_sending)
        MessageSendState.SENT -> stringResource(R.string.message_status_sent)
        MessageSendState.READ -> stringResource(R.string.message_status_read)
        MessageSendState.FAILED -> stringResource(R.string.message_status_failed)
    }
    val symbol = messageStatusSymbol(message.sendState)
    Text(
        symbol,
        modifier = Modifier
            .semantics { contentDescription = description }
            .then(if (message.sendState == MessageSendState.FAILED) Modifier.clickable { onRetryMessage(message.localId) } else Modifier),
        style = MaterialTheme.typography.labelMedium,
        color = if (message.sendState == MessageSendState.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ComposerDock(
    draft: String,
    saving: Boolean,
    error: String?,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
) {
    val sendDescription = stringResource(R.string.chat_send_description)
    Column(
        modifier = Modifier.fillMaxWidth().imePadding().background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        error?.let { TerminalError(it); Spacer(Modifier.height(8.dp)) }
        TerminalSectionLabel(label = stringResource(R.string.chat_new_message), detail = "${draft.length}/${Config.MESSAGE_MAX_LENGTH}")
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            TerminalTextField(
                value = draft,
                onValueChange = onDraft,
                label = stringResource(R.string.chat_message),
                enabled = !saving,
                singleLine = false,
                minLines = 1,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (draft.isNotBlank() && !saving) onSend() }),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onSend,
                enabled = draft.isNotBlank() && !saving,
                modifier = Modifier.size(56.dp).semantics { contentDescription = sendDescription },
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
            ) {
                Text(if (saving) "…" else "↑", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Clip)
            }
        }
    }
}

private fun compactMessageTime(timestampMillis: Long): String = DateTimeFormatter.ofPattern("HH:mm")
    .format(Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()))
