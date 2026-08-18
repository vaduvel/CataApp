package com.emanus.lucrari.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Lucrarea plus cifrele de care are nevoie lista: zile lucrate, etape, resturi deschise,
 * facturat și încasat. Totul într-o singură interogare, fără N+1 (SPEC §4).
 */
data class JobWithTotals(
	@Embedded val job: Job,
	val clientName: String,
	val clientPhone: String?,
	val workedDays: Int,
	val stageCount: Int,
	val stagesDone: Int,
	val openTodos: Int,
	val invoicedCents: Long,
	val collectedCents: Long,
)

/**
 * Lucrarea așa cum apare pe ecranul Azi: cât a avansat, dacă ziua de azi e deja trecută
 * și care e următoarea etapă nebifată.
 */
data class JobToday(
	@Embedded val job: Job,
	val clientName: String,
	val workedDays: Int,
	val stageCount: Int,
	val stagesDone: Int,
	val loggedToday: Int,
	val nextStage: String?,
)

/** Un rest de făcut, cu lucrarea și adresa lui, pentru ecranul global din M3. */
data class TodoWithJob(
	@Embedded val todo: Todo,
	val jobTitle: String,
	val clientName: String,
	val street: String?,
)

@Dao
interface ClientDao {
	@Upsert
	suspend fun upsert(client: Client)

	@Delete
	suspend fun delete(client: Client)

	@Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE")
	fun observeAll(): Flow<List<Client>>

	@Query("SELECT * FROM clients WHERE id = :id")
	fun observeById(id: String): Flow<Client?>

	@Query("SELECT * FROM clients WHERE id = :id")
	suspend fun byId(id: String): Client?

	@Query("SELECT * FROM clients WHERE name = :name COLLATE NOCASE LIMIT 1")
	suspend fun byName(name: String): Client?

	@Query("SELECT COUNT(*) FROM clients")
	suspend fun count(): Int
}

@Dao
interface JobDao {
	@Upsert
	suspend fun upsert(job: Job)

	@Delete
	suspend fun delete(job: Job)

	@Query("SELECT * FROM jobs WHERE id = :id")
	fun observe(id: String): Flow<Job?>

	@Query("SELECT * FROM jobs WHERE id = :id")
	suspend fun byId(id: String): Job?

	@Query("SELECT * FROM jobs ORDER BY createdAt DESC")
	fun observeAll(): Flow<List<Job>>

	@Query("SELECT * FROM jobs WHERE clientId = :clientId ORDER BY createdAt DESC")
	fun observeByClient(clientId: String): Flow<List<Job>>

	@Query(
		"""
		SELECT j.*, c.name AS clientName, c.phone AS clientPhone,
			(SELECT COUNT(*) FROM work_days w WHERE w.jobId = j.id) AS workedDays,
			(SELECT COUNT(*) FROM stages s WHERE s.jobId = j.id) AS stageCount,
			(SELECT COUNT(*) FROM stages s WHERE s.jobId = j.id AND s.done = 1) AS stagesDone,
			(SELECT COUNT(*) FROM todos t WHERE t.jobId = j.id AND t.done = 0) AS openTodos,
			(SELECT IFNULL(SUM(i.amountCents), 0) FROM invoices i WHERE i.jobId = j.id) AS invoicedCents,
			(SELECT IFNULL(SUM(p.amountCents), 0) FROM payments p WHERE p.jobId = j.id) AS collectedCents
		FROM jobs j
		JOIN clients c ON c.id = j.clientId
		ORDER BY j.createdAt DESC
		"""
	)
	fun observeBoard(): Flow<List<JobWithTotals>>

	@Query(
		"""
		SELECT j.*, c.name AS clientName, c.phone AS clientPhone,
			(SELECT COUNT(*) FROM work_days w WHERE w.jobId = j.id) AS workedDays,
			(SELECT COUNT(*) FROM stages s WHERE s.jobId = j.id) AS stageCount,
			(SELECT COUNT(*) FROM stages s WHERE s.jobId = j.id AND s.done = 1) AS stagesDone,
			(SELECT COUNT(*) FROM todos t WHERE t.jobId = j.id AND t.done = 0) AS openTodos,
			(SELECT IFNULL(SUM(i.amountCents), 0) FROM invoices i WHERE i.jobId = j.id) AS invoicedCents,
			(SELECT IFNULL(SUM(p.amountCents), 0) FROM payments p WHERE p.jobId = j.id) AS collectedCents
		FROM jobs j
		JOIN clients c ON c.id = j.clientId
		WHERE j.title LIKE '%' || :q || '%'
			OR IFNULL(j.street, '') LIKE '%' || :q || '%'
			OR IFNULL(j.city, '') LIKE '%' || :q || '%'
			OR c.name LIKE '%' || :q || '%'
		ORDER BY j.createdAt DESC
		"""
	)
	fun searchByStreetOrClient(q: String): Flow<List<JobWithTotals>>

	/**
	 * Ecranul Azi (SPEC §6). Statusurile vin ca parametru, nu ca text în SQL, ca să nu
	 * depindă de felul în care sunt scrise enum-urile în baza de date.
	 */
	@Query(
		"""
		SELECT j.*, c.name AS clientName,
			(SELECT COUNT(*) FROM work_days w WHERE w.jobId = j.id) AS workedDays,
			(SELECT COUNT(*) FROM stages s WHERE s.jobId = j.id) AS stageCount,
			(SELECT COUNT(*) FROM stages s WHERE s.jobId = j.id AND s.done = 1) AS stagesDone,
			(SELECT COUNT(*) FROM work_days w WHERE w.jobId = j.id AND w.date = :date) AS loggedToday,
			(SELECT s.name FROM stages s WHERE s.jobId = j.id AND s.done = 0 ORDER BY s.sort LIMIT 1) AS nextStage
		FROM jobs j
		JOIN clients c ON c.id = j.clientId
		WHERE j.status IN (:statuses)
		ORDER BY j.createdAt DESC
		"""
	)
	fun observeToday(date: LocalDate, statuses: List<JobStatus>): Flow<List<JobToday>>
}

@Dao
interface StageDao {
	@Upsert
	suspend fun upsert(stage: Stage)

	@Upsert
	suspend fun upsertAll(stages: List<Stage>)

	@Delete
	suspend fun delete(stage: Stage)

	@Query("SELECT * FROM stages WHERE jobId = :jobId ORDER BY sort")
	fun observeByJob(jobId: String): Flow<List<Stage>>

	/** Ultima poziție folosită, ca o etapă nouă să ajungă la coadă. */
	@Query("SELECT MAX(sort) FROM stages WHERE jobId = :jobId")
	suspend fun maxSort(jobId: String): Int?

	@Query("SELECT COUNT(*) FROM stages WHERE jobId = :jobId")
	suspend fun count(jobId: String): Int

	/** Câte etape au rămas nebifate, pentru regulile din SPEC §5.6. */
	@Query("SELECT COUNT(*) FROM stages WHERE jobId = :jobId AND done = 0")
	suspend fun openCount(jobId: String): Int
}

@Dao
interface WorkDayDao {
	@Upsert
	suspend fun upsert(day: WorkDay)

	@Upsert
	suspend fun upsertAll(days: List<WorkDay>)

	@Delete
	suspend fun delete(day: WorkDay)

	@Query("SELECT * FROM work_days WHERE jobId = :jobId ORDER BY date")
	fun observeByJob(jobId: String): Flow<List<WorkDay>>

	@Query("SELECT * FROM work_days WHERE jobId = :jobId ORDER BY date DESC")
	fun observeByJobDesc(jobId: String): Flow<List<WorkDay>>

	/** O singură zi per lucrare per dată: a doua apăsare nu dublează nimic. */
	@Query("SELECT * FROM work_days WHERE jobId = :jobId AND date = :date LIMIT 1")
	suspend fun byJobAndDate(jobId: String, date: LocalDate): WorkDay?
}

@Dao
interface TodoDao {
	@Upsert
	suspend fun upsert(todo: Todo)

	@Upsert
	suspend fun upsertAll(todos: List<Todo>)

	@Delete
	suspend fun delete(todo: Todo)

	@Query("SELECT * FROM todos WHERE jobId = :jobId ORDER BY done, due")
	fun observeByJob(jobId: String): Flow<List<Todo>>

	@Query(
		"""
		SELECT t.*, j.title AS jobTitle, j.street AS street, c.name AS clientName
		FROM todos t
		JOIN jobs j ON j.id = t.jobId
		JOIN clients c ON c.id = j.clientId
		WHERE t.done = 0
		ORDER BY (t.due IS NULL), t.due
		"""
	)
	fun observeOpenAll(): Flow<List<TodoWithJob>>

	/** Câte resturi nebifate are lucrarea: se întreabă înainte de Terminat (SPEC §5.6). */
	@Query("SELECT COUNT(*) FROM todos WHERE jobId = :jobId AND done = 0")
	suspend fun openCount(jobId: String): Int
}

@Dao
interface MaterialDao {
	@Upsert
	suspend fun upsert(material: Material)

	@Upsert
	suspend fun upsertAll(materials: List<Material>)

	@Delete
	suspend fun delete(material: Material)

	@Query("SELECT * FROM materials WHERE jobId = :jobId ORDER BY bought, what")
	fun observeByJob(jobId: String): Flow<List<Material>>
}

@Dao
interface MeasureDao {
	@Upsert
	suspend fun upsert(measure: Measure)

	@Upsert
	suspend fun upsertAll(measures: List<Measure>)

	@Delete
	suspend fun delete(measure: Measure)

	@Query("SELECT * FROM measures WHERE jobId = :jobId ORDER BY date, place")
	fun observeByJob(jobId: String): Flow<List<Measure>>

	/**
	 * Toate măsurătorile deodată. Ecranul Bani are nevoie de ele ca să calculeze baza
	 * lucrărilor plătite la măsură, iar o casă cu câteva zeci de lucrări încape lejer
	 * în memorie: mai ieftin decât o interogare pe fiecare lucrare.
	 */
	@Query("SELECT * FROM measures")
	fun observeAll(): Flow<List<Measure>>
}

@Dao
interface ExtraDao {
	@Upsert
	suspend fun upsert(extra: Extra)

	@Upsert
	suspend fun upsertAll(extras: List<Extra>)

	@Delete
	suspend fun delete(extra: Extra)

	@Query("SELECT * FROM extras WHERE jobId = :jobId ORDER BY date")
	fun observeByJob(jobId: String): Flow<List<Extra>>

	/** Toate extra-urile deodată, pentru cifrele de pe ecranul Bani. */
	@Query("SELECT * FROM extras")
	fun observeAll(): Flow<List<Extra>>
}

@Dao
interface PaymentDao {
	@Upsert
	suspend fun upsert(payment: Payment)

	@Upsert
	suspend fun upsertAll(payments: List<Payment>)

	@Delete
	suspend fun delete(payment: Payment)

	@Query("SELECT * FROM payments WHERE jobId = :jobId ORDER BY date DESC")
	fun observeByJob(jobId: String): Flow<List<Payment>>

	/** Cât a intrat în mână de la o dată încoace: cifra Încasat luna asta (SPEC §5.3). */
	@Query("SELECT IFNULL(SUM(amountCents), 0) FROM payments WHERE date >= :from")
	fun observeCollectedSince(from: LocalDate): Flow<Long>
}

@Dao
interface InvoiceDao {
	@Upsert
	suspend fun upsert(invoice: InvoiceRef)

	@Upsert
	suspend fun upsertAll(invoices: List<InvoiceRef>)

	@Delete
	suspend fun delete(invoice: InvoiceRef)

	@Query("SELECT * FROM invoices WHERE jobId = :jobId ORDER BY date DESC")
	fun observeByJob(jobId: String): Flow<List<InvoiceRef>>

	/**
	 * Facturi trimise, neîncasate și mai vechi decât data dată: restanțele de pe ecranul
	 * Bani (SPEC §5.3). Facturile fără dată nu intră, pentru că nu se știe de când curge
	 * termenul.
	 */
	@Query("SELECT IFNULL(SUM(amountCents), 0) FROM invoices WHERE paid = 0 AND date IS NOT NULL AND date <= :before")
	fun observeOverdueBefore(before: LocalDate): Flow<Long>
}

@Dao
interface PhotoDao {
	@Upsert
	suspend fun upsert(photo: Photo)

	@Upsert
	suspend fun upsertAll(photos: List<Photo>)

	@Delete
	suspend fun delete(photo: Photo)

	@Query("SELECT * FROM photos WHERE jobId = :jobId ORDER BY takenAt DESC")
	fun observeByJob(jobId: String): Flow<List<Photo>>
}

@Dao
interface ReminderDao {
	@Upsert
	suspend fun upsert(reminder: Reminder)

	@Upsert
	suspend fun upsertAll(reminders: List<Reminder>)

	@Delete
	suspend fun delete(reminder: Reminder)

	@Query("SELECT * FROM reminders WHERE jobId = :jobId ORDER BY dueAt")
	fun observeByJob(jobId: String): Flow<List<Reminder>>

	@Query("SELECT * FROM reminders WHERE done = 0 ORDER BY dueAt")
	fun observeOpen(): Flow<List<Reminder>>
}
