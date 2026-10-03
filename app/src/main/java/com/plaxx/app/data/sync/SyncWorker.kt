package com.plaxx.app.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.plaxx.app.data.ScheduleRepository

class SyncWorker(
    ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val planIds = inputData.getStringArray("plan_ids")
            ?.mapNotNull { it.toLongOrNull() }
            ?: return Result.success()

        val since = planIds.associateWith { id ->
            inputData.getString("since_$id")?.toLongOrNull() ?: 0L
        }

        val repo = ScheduleRepository()
        return try {
            repo.sync(planIds, since).fold(
                onSuccess = { Result.success() },
                onFailure = { if (runAttemptCount < 3) Result.retry() else Result.failure() }
            )
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val UNIQUE_NAME = "plaxx-sync"
    }
}
