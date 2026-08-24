package com.example.bunbun.outbox

enum class OutboxFailureDisposition { AUTH_REQUIRED, RETRY, FAILED }

fun classifyOutboxHttpFailure(status: Int): OutboxFailureDisposition = when {
    status == 401 -> OutboxFailureDisposition.AUTH_REQUIRED
    status >= 500 || status == 408 || status == 429 -> OutboxFailureDisposition.RETRY
    else -> OutboxFailureDisposition.FAILED
}

fun canDrainOutbox(capturedAccountId: Long?, activeAccountId: Long?, authenticated: Boolean): Boolean =
    authenticated && capturedAccountId != null && capturedAccountId == activeAccountId
