package com.emanus.lucrari.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Citirea completă și inserarea idempotentă pentru arhivele M7. Sunt numai interogări:
 * entitățile și schema Room v1 nu se schimbă.
 */
@Dao
interface BackupDao {
	@Query("SELECT * FROM clients ORDER BY id")
	suspend fun clients(): List<Client>

	@Query("SELECT * FROM jobs ORDER BY id")
	suspend fun jobs(): List<Job>

	@Query("SELECT * FROM stages ORDER BY id")
	suspend fun stages(): List<Stage>

	@Query("SELECT * FROM work_days ORDER BY id")
	suspend fun workDays(): List<WorkDay>

	@Query("SELECT * FROM todos ORDER BY id")
	suspend fun todos(): List<Todo>

	@Query("SELECT * FROM materials ORDER BY id")
	suspend fun materials(): List<Material>

	@Query("SELECT * FROM measures ORDER BY id")
	suspend fun measures(): List<Measure>

	@Query("SELECT * FROM extras ORDER BY id")
	suspend fun extras(): List<Extra>

	@Query("SELECT * FROM payments ORDER BY id")
	suspend fun payments(): List<Payment>

	@Query("SELECT * FROM invoices ORDER BY id")
	suspend fun invoices(): List<InvoiceRef>

	@Query("SELECT * FROM photos ORDER BY id")
	suspend fun photos(): List<Photo>

	@Query("SELECT * FROM reminders ORDER BY id")
	suspend fun reminders(): List<Reminder>

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertClients(items: List<Client>)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertJobs(items: List<Job>)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertStages(items: List<Stage>)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertWorkDays(items: List<WorkDay>)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertTodos(items: List<Todo>)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertMaterials(items: List<Material>)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertMeasures(items: List<Measure>)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertExtras(items: List<Extra>)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertPayments(items: List<Payment>)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertInvoices(items: List<InvoiceRef>)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertPhotos(items: List<Photo>)

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun insertReminders(items: List<Reminder>)

	@Query("DELETE FROM reminders")
	suspend fun clearReminders()

	/** Ștergerea clienților șterge în cascadă lucrările și toate rândurile lor. */
	@Query("DELETE FROM clients")
	suspend fun clearClients()
}
