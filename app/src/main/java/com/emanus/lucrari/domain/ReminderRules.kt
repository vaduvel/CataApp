package com.emanus.lucrari.domain

import com.emanus.lucrari.data.Extra
import com.emanus.lucrari.data.InvoiceRef
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.data.Measure
import com.emanus.lucrari.data.Todo
import com.emanus.lucrari.data.WorkDay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class ReminderKind {
	TO_INVOICE,
	OVERDUE_INVOICE,
	OFFER_FOLLOW_UP,
	TODO_DUE,
	START_SOON,
	START_TOMORROW,
	START_TODAY,
}

data class ReminderKey(val jobId: String, val kind: ReminderKind)

data class ReminderCandidate(
	val jobId: String,
	val kind: ReminderKind,
	val jobTitle: String,
	val dueAt: Long,
	val overdueDays: Long? = null,
)

/**
 * Regulile pure din SPEC §5.4, plus memento-urile de început de lucrare (M8).
 * Worker-ul doar citește, salvează și notifică.
 *
 * Memento-urile de început se uită la [Job.plannedStart] al lucrărilor rămase pe
 * `PROGRAMAT`: cu 3 zile înainte (să aibă timp de materiale), în ajun și în
 * dimineața zilei. Primele două au sens seara, ultimul are sens dimineața, așa că
 * rularea de la 07:30 cere doar [MORNING_KINDS], iar cea de la 19:00 [EVENING_KINDS].
 *
 * Regula se uită la ziua exactă, nu la un interval: dacă telefonul e stins toată
 * ziua și worker-ul prinde abia ziua următoare, memento-ul acelei zile se pierde.
 * Următorul prag (ajunul, apoi dimineața) prinde oricum lucrarea.
 */
object ReminderRules {
	/** Rularea de seară caută tot, în afară de memento-ul care are sens dimineața. */
	val EVENING_KINDS: Set<ReminderKind> = ReminderKind.entries.toSet() - ReminderKind.START_TODAY

	/** Rularea de dimineață caută doar lucrările care încep azi. */
	val MORNING_KINDS: Set<ReminderKind> = setOf(ReminderKind.START_TODAY)

	fun candidates(
		today: LocalDate,
		zoneId: ZoneId,
		jobs: List<Job>,
		workDays: List<WorkDay>,
		measures: List<Measure>,
		extras: List<Extra>,
		invoices: List<InvoiceRef>,
		todos: List<Todo>,
		existingOpen: Set<ReminderKey>,
		kinds: Set<ReminderKind> = ReminderKind.entries.toSet(),
	): List<ReminderCandidate> {
		val daysByJob = workDays.groupBy { it.jobId }
		val measuresByJob = measures.groupBy { it.jobId }
		val extrasByJob = extras.groupBy { it.jobId }
		val invoicesByJob = invoices.groupBy { it.jobId }
		val todosByJob = todos.groupBy { it.jobId }
		val eveningAt = today.atTime(LocalTime.of(19, 0)).atZone(zoneId).toInstant().toEpochMilli()
		val morningAt = today.atTime(LocalTime.of(7, 30)).atZone(zoneId).toInstant().toEpochMilli()
		val added = existingOpen.toMutableSet()
		val result = mutableListOf<ReminderCandidate>()

		fun add(job: Job, kind: ReminderKind, overdueDays: Long? = null) {
			if (kind !in kinds) return
			val key = ReminderKey(job.id, kind)
			if (!added.add(key)) return
			val dueAt = if (kind == ReminderKind.START_TODAY) morningAt else eveningAt
			result += ReminderCandidate(job.id, kind, job.title, dueAt, overdueDays)
		}

		for (job in jobs) {
			if (job.status == JobStatus.ANULAT) continue

			val start = job.plannedStart
			if (job.status == JobStatus.PROGRAMAT && start != null) {
				when (ChronoUnit.DAYS.between(today, start)) {
					3L -> add(job, ReminderKind.START_SOON)
					1L -> add(job, ReminderKind.START_TOMORROW)
					0L -> add(job, ReminderKind.START_TODAY)
					else -> Unit
				}
			}

			val jobInvoices = invoicesByJob[job.id].orEmpty()

			if (job.status == JobStatus.TERMINAT && job.closedAt != null) {
				val closedDate = Instant.ofEpochMilli(job.closedAt).atZone(zoneId).toLocalDate()
				val age = ChronoUnit.DAYS.between(closedDate, today)
				val totals = Totals.of(
					job = job,
					workedDays = daysByJob[job.id].orEmpty().size,
					measures = measuresByJob[job.id].orEmpty(),
					extras = extrasByJob[job.id].orEmpty(),
					invoicedCents = jobInvoices.sumOf { it.amountCents },
					collectedCents = 0L,
				)
				if (age >= 3L && totals.toInvoiceCents > 0L) add(job, ReminderKind.TO_INVOICE)
			}

			val overdue = jobInvoices.filter { !it.paid && it.due != null && it.due.isBefore(today) }
			if (overdue.isNotEmpty()) {
				val oldest = overdue.minOf { invoice -> invoice.due ?: today }
				add(job, ReminderKind.OVERDUE_INVOICE, ChronoUnit.DAYS.between(oldest, today))
			}

			if (job.status == JobStatus.OFERTAT) {
				val createdDate = Instant.ofEpochMilli(job.createdAt).atZone(zoneId).toLocalDate()
				val age = ChronoUnit.DAYS.between(createdDate, today)
				if (age == 3L || age == 7L || age == 14L) add(job, ReminderKind.OFFER_FOLLOW_UP)
			}

			if (todosByJob[job.id].orEmpty().any { !it.done && it.due == today }) {
				add(job, ReminderKind.TODO_DUE)
			}
		}
		return result
	}
}
