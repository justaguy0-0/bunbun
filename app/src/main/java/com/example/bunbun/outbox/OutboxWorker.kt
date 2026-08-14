package com.example.bunbun.outbox

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.bunbun.BunbunApplication

class OutboxWorker(appContext: Context, parameters: WorkerParameters) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        val repository = (applicationContext as BunbunApplication).container.repository
        return when (repository.drainOutbox()) {
            OutboxDrainResult.RETRY -> Result.retry()
            OutboxDrainResult.COMPLETED, OutboxDrainResult.AUTH_REQUIRED -> Result.success()
        }
    }
}

enum class OutboxDrainResult { COMPLETED, RETRY, AUTH_REQUIRED }
