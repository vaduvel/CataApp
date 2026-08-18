package com.emanus.lucrari.data.repo

import com.emanus.lucrari.data.AppDb
import com.emanus.lucrari.data.Client
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.data.JobWithTotals
import com.emanus.lucrari.data.Stage
import com.emanus.lucrari.data.now
import com.emanus.lucrari.domain.Templates
import kotlinx.coroutines.flow.Flow

/**
 * Singurul drum dintre interfață și baza de date. Composable-urile nu ating niciodată DAO-urile.
 */
class JobRepo(private val db: AppDb) {

	fun board(query: String): Flow<List<JobWithTotals>> {
		val q = query.trim()
		return if (q.isEmpty()) db.jobs().observeBoard() else db.jobs().searchByStreetOrClient(q)
	}

	fun job(id: String): Flow<Job?> = db.jobs().observe(id)

	fun allJobs(): Flow<List<Job>> = db.jobs().observeAll()

	fun stages(jobId: String): Flow<List<Stage>> = db.stages().observeByJob(jobId)

	fun clients(): Flow<List<Client>> = db.clients().observeAll()

	fun client(id: String): Flow<Client?> = db.clients().observeById(id)

	/**
	 * Formularul de lucrare nouă are 4 câmpuri (SPEC §7). Adresa se scrie într-un singur
	 * câmp, așa cum o spune el ("Via 23, Milano"), și se sparge aici în stradă și oraș.
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
}
