package com.emanus.lucrari.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object WorkScheduler {
	const val REMINDER_WORK_NAME = "daily-reminders-19"
	const val BACKUP_WORK_NAME = "daily-backup"

	fun schedule(context: Context) {
		val manager = WorkManager.getInstance(context)
		val reminder = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
			.setInitialDelay(initialDelayMillis(19), TimeUnit.MILLISECONDS)
			.addTag(REMINDER_WORK_NAME)
			.build()
		val backup = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
			.setInitialDelay(initialDelayMillis(2), TimeUnit.MILLISECONDS)
			.addTag(BACKUP_WORK_NAME)
			.build()
		manager.enqueueUniquePeriodicWork(
			REMINDER_WORK_NAME,
			ExistingPeriodicWorkPolicy.KEEP,
			reminder,
		)
		manager.enqueueUniquePeriodicWork(
			BACKUP_WORK_NAME,
			ExistingPeriodicWorkPolicy.KEEP,
			backup,
		)
	}

	fun initialDelayMillis(hour: Int, now: ZonedDateTime = ZonedDateTime.now()): Long {
		require(hour in 0..23)
		var target = now.toLocalDate().atTime(hour, 0).atZone(now.zone)
		if (!target.isAfter(now)) target = target.plusDays(1)
		return Duration.between(now, target).toMillis()
	}
}
