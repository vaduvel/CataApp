package com.emanus.lucrari.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.emanus.lucrari.App

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
	override suspend fun doWork(): Result {
		val app = applicationContext as? App ?: return Result.failure()
		return runCatching { app.backupRepo.createAutomaticBackup() }
			.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
	}
}
