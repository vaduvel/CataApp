package com.emanus.lucrari.data.repo

import com.emanus.lucrari.data.AppDb
import com.emanus.lucrari.data.Client
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.data.JobToday
import com.emanus.lucrari.data.JobWithTotals
import com.emanus.lucrari.data.Stage
import com.emanus.lucrari.data.WorkDay
import com.emanus.lucrari.data.now
import com.emanus.lucrari.data.today
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

	suspend fun saveDay(day: WorkDay) {
		db.workDays().upsert(day.copy(what = day.what?.trim()?.ifBlank { null }))
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

	companion object {
		/** Lucrările vii, cele care apar pe ecranul Azi (SPEC §6). */
		val ACTIVE_STATUSES: List<JobStatus> = listOf(
			JobStatus.IN_LUCRU,
			JobStatus.DE_FINISAT,
			JobStatus.ASTEPTARE,
		)
	}
}
