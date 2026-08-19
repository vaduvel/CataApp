package com.emanus.lucrari.data.repo

import com.emanus.lucrari.data.AppDb
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.data.today
import com.emanus.lucrari.domain.Schedule
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Programarea lucrărilor: din ce zi începe fiecare și ce se lucrează într-o zi anume.
 * Stă separat de `JobRepo`, ca `BackupRepo` sau `ReminderRepo`, pentru că are altă treabă.
 */
class ScheduleRepo(private val db: AppDb) {

	/**
	 * Pune sau schimbă data de început. Statusul se atinge doar cât timp lucrarea n-a
	 * început: una în lucru rămâne în lucru chiar dacă i se mută data în calendar.
	 */
	suspend fun setPlannedStart(jobId: String, start: LocalDate?) {
		val job = db.jobs().byId(jobId) ?: return
		val open = job.status == JobStatus.OFERTAT || job.status == JobStatus.PROGRAMAT
		db.jobs().upsert(
			job.copy(
				plannedStart = start,
				status = if (open) {
					Schedule.statusForNewJob(start, today(), job.status)
				} else {
					job.status
				},
			),
		)
	}

	/**
	 * Lucrările care ating fiecare zi din interval, pentru calendarul de lucru. Se citesc
	 * toate o dată și se împart în memorie: la câteva zeci de lucrări e mai ieftin decât o
	 * interogare pe zi. Zilele fără nimic nu apar în hartă, iar lucrările anulate nu intră.
	 */
	fun byDay(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<Job>>> =
		db.jobs().observeAll().map { all ->
			val jobs = all.filter { it.status != JobStatus.ANULAT }
			(0..ChronoUnit.DAYS.between(from, to))
				.map { offset -> from.plusDays(offset) }
				.associateWith { day ->
					jobs.filter { job -> Schedule.covers(job.plannedStart, job.estDays, day) }
				}
				.filterValues { it.isNotEmpty() }
		}
}
