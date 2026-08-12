package com.example.bunbun.ui.common

import com.example.bunbun.data.model.MessageDto

fun mergeMessages(existing: List<MessageDto>, incoming: List<MessageDto>): List<MessageDto> =
    (existing + incoming).associateBy(MessageDto::id).values.sortedBy(MessageDto::id)

fun lastMessageId(messages: List<MessageDto>): Long? = messages.maxOfOrNull(MessageDto::id)

