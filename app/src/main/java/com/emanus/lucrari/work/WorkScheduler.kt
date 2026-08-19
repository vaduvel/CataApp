package com.emanus.lucrari.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object WorkScheduler {
	const val REMINDER_WORK_NAME = "daily-reminders-19"
	const val START_WORK_NAME = "daily-start-reminders-0730"
	const val BACKUP_WORK_NAME = "daily-backup"

	/**
	 * Cheia din datele de intrare care spune worker-ului că e rularea de dimineață.
	 * Lipsa ei înseamnă rularea de seară, ca să rămână valabile și lucrările deja
	 * programate pe telefon înainte de M8.
	 */
	const val KEY_MORNING = "morning"

	fun schedule(context: Context) {
		val manager = WorkManager.getInstance(context)
		val reminder = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
			.setInitialDelay(initialDelayMillis(19), TimeUnit.MILLISECONDS)
			.addTag(REMINDER_WORK_NAME)
			.build()
		val start = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
			.setInitialDelay(initialDelayMillis(7, minute = 30), TimeUnit.MILLISECONDS)
			.setInputData(workDataOf(KEY_MORNING to true))
			.addTag(START_WORK_NAME)
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
			START_WORK_NAME,
			ExistingPeriodicWorkPolicy.KEEP,
			start,
		)
		manager.enqueueUniquePeriodicWork(
			BACKUP_WORK_NAME,
			ExistingPeriodicWorkPolicy.KEEP,
			backup,
		)
	}

	/**
	 * Câți milisecunde sunt până la următoarea oră fixă cerută. [minute] e la sfârșit,
	 * cu valoare implicită, ca apelurile pe poziții de dinainte de M8 să rămână valabile.
	 */
	fun initialDelayMillis(hour: Int, now: ZonedDateTime = ZonedDateTime.now(), minute: Int = 0): Long {
		require(hour in 0..23)
		require(minute in 0..59)
		var target = now.toLocalDate().atTime(hour, minute).atZone(now.zone)
		if (!target.isAfter(now)) target = target.plusDays(1)
		return Duration.between(now, target).toMillis()
	}
}
