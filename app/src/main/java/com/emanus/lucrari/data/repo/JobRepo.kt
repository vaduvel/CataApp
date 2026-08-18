package com.emanus.lucrari.data.repo

import com.emanus.lucrari.data.AppDb
import com.emanus.lucrari.data.Client
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.data.JobToday
import com.emanus.lucrari.data.JobWithTotals
import com.emanus.lucrari.data.Material
import com.emanus.lucrari.data.Reason
import com.emanus.lucrari.data.Stage
import com.emanus.lucrari.data.Todo
import com.emanus.lucrari.data.TodoWithJob
import com.emanus.lucrari.data.WorkDay
import com.emanus.lucrari.data.now
import com.emanus.lucrari.data.today
import com.emanus.lucrari.domain.Rules
import com.emanus.lucrari.domain.Templates
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Singurul drum dintre interfață și baza de date. Composable-urile nu ating niciodată DAO-urile.
 */
class JobRepo(private val db: AppDb) {

	fun board(query: String): Flow<List<JobWithTotals>> {
		val q = query.trim()
		return if (q.isEmpty()) db.jobs().observeBoard() else db.jobs().searchByStreetOrClient(q)
	}

	/**
	 * Lucrările la care se poate lucra azi, în ordinea în care le atinge: întâi cele în
	 * lucru, apoi de finisat, apoi cele blocate în așteptare.
	 */
	fun todayBoard(date: LocalDate): Flow<List<JobToday>> =
		db.jobs().observeToday(date, ACTIVE_STATUSES).map { list ->
			list.sortedBy { row -> ACTIVE_STATUSES.indexOf(row.job.status) }
		}

	fun job(id: String): Flow<Job?> = db.jobs().observe(id)

	fun allJobs(): Flow<List<Job>> = db.jobs().observeAll()

	fun stages(jobId: String): Flow<List<Stage>> = db.stages().observeByJob(jobId)

	fun days(jobId: String): Flow<List<WorkDay>> = db.workDays().observeByJobDesc(jobId)

	fun todos(jobId: String): Flow<List<Todo>> = db.todos().observeByJob(jobId)

	/** Tot ce a rămas nefăcut, din toate lucrările, cel mai apropiat termen primul (SPEC §11). */
	fun openTodos(): Flow<List<TodoWithJob>> = db.todos().observeOpenAll()

	fun materials(jobId: String): Flow<List<Material>> = db.materials().observeByJob(jobId)

	fun clients(): Flow<List<Client>> = db.clients().observeAll()

	fun client(id: String): Flow<Client?> = db.clients().observeById(id)

	/**
	 * Formularul de lucrare nouă are 4 câmpuri (SPEC §7). Adresa se scrie într-un singur
	 * câmp, așa cum o spune el (Via 23, Milano), și se sparge aici în stradă și oraș.
	 * Clientul se refolosește dacă există deja cu același nume, altfel se creează.
	 */
	suspend fun createJob(
		clientName: String,
		address: String,
		title: String,
		type: String?,
		estDays: Int?,
	): String {
		val name = clientName.trim()
		val client = db.clients().byName(name) ?: Client(name = name).also { db.clients().upsert(it) }

		val parts = address.split(",", limit = 2)
		val street = parts.getOrNull(0)?.trim()?.takeIf { it.isNotEmpty() }
		val city = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }

		val job = Job(
			clientId = client.id,
			title = title.trim(),
			street = street,
			city = city,
			type = type,
			estDays = estDays,
		)
		db.jobs().upsert(job)

		val stageNames = Templates.stagesFor(type)
		if (stageNames.isNotEmpty()) {
			db.stages().upsertAll(
				stageNames.mapIndexed { index, stageName ->
					Stage(jobId = job.id, name = stageName, sort = index)
				},
			)
		}
		return job.id
	}

	suspend fun setStatus(job: Job, status: JobStatus) {
		val closedAt = if (status == JobStatus.TERMINAT || status == JobStatus.ANULAT) {
			job.closedAt ?: now()
		} else {
			null
		}
		db.jobs().upsert(job.copy(status = status, closedAt = closedAt))
	}

	suspend fun deleteJob(job: Job) {
		db.jobs().delete(job)
	}

	suspend fun addClient(name: String, phone: String?, note: String?) {
		db.clients().upsert(Client(name = name.trim(), phone = phone, note = note))
	}

	/**
	 * Am lucrat azi aici, dintr-o singură apăsare (SPEC §7). A doua apăsare în aceeași zi
	 * nu dublează nimic: întoarce false, iar interfața spune doar că ziua era deja trecută.
	 * Prima zi lucrată scoate lucrarea din ofertă și o pune în lucru.
	 */
	suspend fun logDay(jobId: String, date: LocalDate = today(), what: String? = null): Boolean {
		if (db.workDays().byJobAndDate(jobId, date) != null) return false
		db.workDays().upsert(
			WorkDay(jobId = jobId, date = date, what = what?.trim()?.ifBlank { null }),
		)
		val job = db.jobs().byId(jobId)
		if (job != null && (job.status == JobStatus.OFERTAT || job.status == JobStatus.PROGRAMAT)) {
			db.jobs().upsert(job.copy(status = JobStatus.IN_LUCRU))
		}
		return true
	}

	/**
	 * SPEC §5.6: o zi în care s-a stat degeaba trage lucrarea în Așteptare. Nu atinge
	 * lucrările deja închise, ca să nu reînvie una terminată.
	 */
	suspend fun saveDay(day: WorkDay) {
		val clean = day.copy(
			what = day.what?.trim()?.ifBlank { null },
			blocked = day.blocked?.trim()?.ifBlank { null },
		)
		db.workDays().upsert(clean)
		if (clean.blocked == null) return
		val job = db.jobs().byId(clean.jobId) ?: return
		val next = Rules.statusAfterBlockedDay(job.status) ?: return
		db.jobs().upsert(job.copy(status = next))
	}

	suspend fun deleteDay(day: WorkDay) {
		db.workDays().delete(day)
	}

	suspend fun toggleStage(stage: Stage) {
		val done = !stage.done
		db.stages().upsert(stage.copy(done = done, doneAt = if (done) now() else null))
	}

	suspend fun addStage(jobId: String, name: String) {
		val clean = name.trim()
		if (clean.isEmpty()) return
		val sort = (db.stages().maxSort(jobId) ?: -1) + 1
		db.stages().upsert(Stage(jobId = jobId, name = clean, sort = sort))
	}

	suspend fun deleteStage(stage: Stage) {
		db.stages().delete(stage)
	}

	/**
	 * Alege un șablon (SPEC §14) pentru o lucrare care n-a primit unul la creare. Etapele
	 * se adaugă la coadă, nu se șterge nimic din ce a bifat deja.
	 */
	suspend fun applyTemplate(jobId: String, type: String) {
		val names = Templates.stagesFor(type)
		if (names.isEmpty()) return
		val start = (db.stages().maxSort(jobId) ?: -1) + 1
		db.stages().upsertAll(
			names.mapIndexed { index, name ->
				Stage(jobId = jobId, name = name, sort = start + index)
			},
		)
		val job = db.jobs().byId(jobId) ?: return
		if (job.type.isNullOrBlank()) {
			db.jobs().upsert(job.copy(type = type))
		}
	}

	/**
	 * Rest de făcut (SPEC §11). Locul e camera sau bucata de lucrare, ca să știe unde se
	 * întoarce: "baia", "scara B". Motivul și termenul sunt opționale.
	 */
	suspend fun addTodo(
		jobId: String,
		what: String,
		place: String? = null,
		reason: Reason? = null,
		due: LocalDate? = null,
	) {
		val clean = what.trim()
		if (clean.isEmpty()) return
		db.todos().upsert(
			Todo(
				jobId = jobId,
				place = place?.trim()?.ifBlank { null },
				what = clean,
				reason = reason,
				due = due,
			),
		)
	}

	suspend fun saveTodo(todo: Todo) {
		val clean = todo.what.trim()
		if (clean.isEmpty()) return
		db.todos().upsert(
			todo.copy(what = clean, place = todo.place?.trim()?.ifBlank { null }),
		)
	}

	/** Bifarea e reversibilă: dacă a bifat din greșeală, o apasă din nou. */
	suspend fun toggleTodo(todo: Todo) {
		val done = !todo.done
		db.todos().upsert(todo.copy(done = done, doneAt = if (done) now() else null))
	}

	suspend fun deleteTodo(todo: Todo) {
		db.todos().delete(todo)
	}

	suspend fun addMaterial(jobId: String, what: String, qty: String? = null, shop: String? = null) {
		val clean = what.trim()
		if (clean.isEmpty()) return
		db.materials().upsert(
			Material(
				jobId = jobId,
				what = clean,
				qty = qty?.trim()?.ifBlank { null },
				shop = shop?.trim()?.ifBlank { null },
			),
		)
	}

	suspend fun toggleMaterial(material: Material) {
		db.materials().upsert(material.copy(bought = !material.bought))
	}

	suspend fun deleteMaterial(material: Material) {
		db.materials().delete(material)
	}

	/** Câte resturi nebifate are lucrarea. Interfața întreabă înainte să pună Terminat. */
	suspend fun openTodoCount(jobId: String): Int = db.todos().openCount(jobId)

	/**
	 * Toate etapele bifate, dar au rămas resturi: propune De finisat în loc de Terminat
	 * (SPEC §5.6). Doar propune; apasă omul.
	 */
	suspend fun suggestsDeFinisat(jobId: String): Boolean {
		val job = db.jobs().byId(jobId) ?: return false
		return Rules.suggestsDeFinisat(
			current = job.status,
			stageCount = db.stages().count(jobId),
			openStages = db.stages().openCount(jobId),
			openTodos = db.todos().openCount(jobId),
		)
	}

	companion object {
		/** Lucrările vii, cele care apar pe ecranul Azi (SPEC §6). */
		val ACTIVE_STATUSES: List<JobStatus> = listOf(
			JobStatus.IN_LUCRU,
			JobStatus.DE_FINISAT,
			JobStatus.ASTEPTARE,
		)
	}
}
