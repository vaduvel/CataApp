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

enum class ReminderKind { TO_INVOICE, OVERDUE_INVOICE, OFFER_FOLLOW_UP, TODO_DUE }

data class ReminderKey(val jobId: String, val kind: ReminderKind)

data class ReminderCandidate(
	val jobId: String,
	val kind: ReminderKind,
	val jobTitle: String,
	val dueAt: Long,
	val overdueDays: Long? = null,
)

/** Regulile pure din SPEC §5.4. Worker-ul doar citește, salvează și notifică. */
object ReminderRules {
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
	): List<ReminderCandidate> {
		val daysByJob = workDays.groupBy { it.jobId }
		val measuresByJob = measures.groupBy { it.jobId }
		val extrasByJob = extras.groupBy { it.jobId }
		val invoicesByJob = invoices.groupBy { it.jobId }
		val todosByJob = todos.groupBy { it.jobId }
		val dueAt = today.atTime(LocalTime.of(19, 0)).atZone(zoneId).toInstant().toEpochMilli()
		val added = existingOpen.toMutableSet()
		val result = mutableListOf<ReminderCandidate>()

		fun add(job: Job, kind: ReminderKind, overdueDays: Long? = null) {
			val key = ReminderKey(job.id, kind)
			if (!added.add(key)) return
			result += ReminderCandidate(job.id, kind, job.title, dueAt, overdueDays)
		}

		for (job in jobs) {
			if (job.status == JobStatus.ANULAT) continue
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
