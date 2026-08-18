package com.emanus.lucrari.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
	entities = [
		Client::class,
		Job::class,
		Stage::class,
		WorkDay::class,
		Todo::class,
		Material::class,
		Measure::class,
		Extra::class,
		Payment::class,
		InvoiceRef::class,
		Photo::class,
		Reminder::class,
	],
	version = 1,
	exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {

	abstract fun clients(): ClientDao

	abstract fun jobs(): JobDao

	abstract fun stages(): StageDao

	abstract fun workDays(): WorkDayDao

	abstract fun todos(): TodoDao

	abstract fun materials(): MaterialDao

	abstract fun measures(): MeasureDao

	abstract fun extras(): ExtraDao

	abstract fun payments(): PaymentDao

	abstract fun invoices(): InvoiceDao

	abstract fun photos(): PhotoDao

	abstract fun reminders(): ReminderDao

	abstract fun backup(): BackupDao

	companion object {
		const val NAME = "lucrari.db"

		// Fără fallbackToDestructiveMigration: datele lui nu se șterg niciodată automat.
		// La schimbarea schemei se scrie migrare, iar schema exportată din app/schemas o verifică.
		fun build(context: Context): AppDb =
			Room.databaseBuilder(context.applicationContext, AppDb::class.java, NAME).build()
	}
}
