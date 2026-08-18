package com.emanus.lucrari.data.repo

import com.emanus.lucrari.data.AppDb
import com.emanus.lucrari.data.Reminder
import kotlinx.coroutines.flow.Flow

class ReminderRepo(private val db: AppDb) {
	fun open(): Flow<List<Reminder>> = db.reminders().observeOpen()

	suspend fun toggle(reminder: Reminder) {
		db.reminders().upsert(reminder.copy(done = !reminder.done))
	}
}
