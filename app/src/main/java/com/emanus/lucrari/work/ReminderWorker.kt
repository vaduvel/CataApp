package com.emanus.lucrari.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.emanus.lucrari.App
import com.emanus.lucrari.MainActivity
import com.emanus.lucrari.R
import com.emanus.lucrari.data.Extra
import com.emanus.lucrari.data.InvoiceRef
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.Measure
import com.emanus.lucrari.data.Reminder
import com.emanus.lucrari.data.Todo
import com.emanus.lucrari.data.WorkDay
import com.emanus.lucrari.domain.ReminderCandidate
import com.emanus.lucrari.domain.ReminderKey
import com.emanus.lucrari.domain.ReminderKind
import com.emanus.lucrari.domain.ReminderRules
import java.time.LocalDate
import java.time.ZoneId

private data class ReminderData(
	val jobs: List<Job>,
	val workDays: List<WorkDay>,
	val measures: List<Measure>,
	val extras: List<Extra>,
	val invoices: List<InvoiceRef>,
	val todos: List<Todo>,
	val reminders: List<Reminder>,
)

/**
 * Rulează de două ori pe zi cu aceeași clasă: seara la 19:00 pentru tot ce ține de
 * bani, oferte și resturi, dimineața la 07:30 doar pentru lucrările care încep azi.
 * Rularea de dimineață e marcată cu [WorkScheduler.KEY_MORNING].
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
	override suspend fun doWork(): Result {
		val app = applicationContext as? App ?: return Result.failure()
		return runCatching {
			val morning = inputData.getBoolean(WorkScheduler.KEY_MORNING, false)
			val kinds = if (morning) ReminderRules.MORNING_KINDS else ReminderRules.EVENING_KINDS
			val data = app.db.withTransaction {
				val dao = app.db.backup()
				ReminderData(
					dao.jobs(), dao.workDays(), dao.measures(), dao.extras(), dao.invoices(),
					dao.todos(), dao.reminders(),
				)
			}
			val text = ReminderText(applicationContext)
			val existing = data.reminders.filter { it.auto && !it.done }.mapNotNull { reminder ->
				val jobId = reminder.jobId ?: return@mapNotNull null
				val kind = text.kindOf(reminder.text) ?: return@mapNotNull null
				ReminderKey(jobId, kind)
			}.toSet()
			val candidates = ReminderRules.candidates(
				today = LocalDate.now(),
				zoneId = ZoneId.systemDefault(),
				jobs = data.jobs,
				workDays = data.workDays,
				measures = data.measures,
				extras = data.extras,
				invoices = data.invoices,
				todos = data.todos,
				existingOpen = existing,
				kinds = kinds,
			)
			for (candidate in candidates) {
				val message = text.format(candidate)
				app.db.reminders().upsert(
					Reminder(
						jobId = candidate.jobId,
						text = message,
						dueAt = candidate.dueAt,
						auto = true,
					),
				)
				ReminderNotifier.show(applicationContext, candidate, message)
			}
		}.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
	}
}

private class ReminderText(private val context: Context) {
	private val toInvoicePrefix = context.getString(R.string.reminder_prefix_to_invoice)
	private val overduePrefix = context.getString(R.string.reminder_prefix_overdue)
	private val offerPrefix = context.getString(R.string.reminder_prefix_offer)
	private val todoPrefix = context.getString(R.string.reminder_prefix_todo)
	private val startSoonPrefix = context.getString(R.string.reminder_prefix_start_soon)
	private val startTomorrowPrefix = context.getString(R.string.reminder_prefix_start_tomorrow)
	private val startTodayPrefix = context.getString(R.string.reminder_prefix_start_today)

	fun kindOf(text: String): ReminderKind? = when {
		text.startsWith(toInvoicePrefix) -> ReminderKind.TO_INVOICE
		text.startsWith(overduePrefix) -> ReminderKind.OVERDUE_INVOICE
		text.startsWith(offerPrefix) -> ReminderKind.OFFER_FOLLOW_UP
		text.startsWith(todoPrefix) -> ReminderKind.TODO_DUE
		text.startsWith(startSoonPrefix) -> ReminderKind.START_SOON
		text.startsWith(startTomorrowPrefix) -> ReminderKind.START_TOMORROW
		text.startsWith(startTodayPrefix) -> ReminderKind.START_TODAY
		else -> null
	}

	fun format(candidate: ReminderCandidate): String = when (candidate.kind) {
		ReminderKind.TO_INVOICE -> context.getString(R.string.reminder_to_invoice, candidate.jobTitle)
		ReminderKind.OVERDUE_INVOICE -> context.getString(
			R.string.reminder_overdue,
			candidate.overdueDays ?: 0L,
			candidate.jobTitle,
		)
		ReminderKind.OFFER_FOLLOW_UP -> context.getString(R.string.reminder_offer, candidate.jobTitle)
		ReminderKind.TODO_DUE -> context.getString(R.string.reminder_todo, candidate.jobTitle)
		ReminderKind.START_SOON -> context.getString(R.string.reminder_start_soon, candidate.jobTitle)
		ReminderKind.START_TOMORROW -> context.getString(
			R.string.reminder_start_tomorrow,
			candidate.jobTitle,
		)
		ReminderKind.START_TODAY -> context.getString(R.string.reminder_start_today, candidate.jobTitle)
	}
}

object ReminderNotifier {
	private const val CHANNEL_ID = "lucrari_reminders"

	fun createChannel(context: Context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
		val manager = context.getSystemService(NotificationManager::class.java)
		manager.createNotificationChannel(
			NotificationChannel(
				CHANNEL_ID,
				context.getString(R.string.reminder_channel_name),
				NotificationManager.IMPORTANCE_DEFAULT,
			).apply { description = context.getString(R.string.reminder_channel_description) },
		)
	}

	fun show(context: Context, candidate: ReminderCandidate, message: String) {
		if (
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
			ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
			PackageManager.PERMISSION_GRANTED
		) return
		createChannel(context)
		val intent = Intent(context, MainActivity::class.java)
		val pendingIntent = PendingIntent.getActivity(
			context,
			0,
			intent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
		)
		val notification = NotificationCompat.Builder(context, CHANNEL_ID)
			.setSmallIcon(android.R.drawable.ic_dialog_info)
			.setContentTitle(context.getString(R.string.reminder_notification_title))
			.setContentText(message)
			.setStyle(NotificationCompat.BigTextStyle().bigText(message))
			.setContentIntent(pendingIntent)
			.setAutoCancel(true)
			.build()
		NotificationManagerCompat.from(context).notify(
			(candidate.jobId + candidate.kind.name).hashCode(),
			notification,
		)
	}
}
