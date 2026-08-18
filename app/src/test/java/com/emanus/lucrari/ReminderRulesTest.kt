package com.emanus.lucrari

import com.emanus.lucrari.data.InvoiceRef
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.data.Todo
import com.emanus.lucrari.domain.ReminderKey
import com.emanus.lucrari.domain.ReminderKind
import com.emanus.lucrari.domain.ReminderRules
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderRulesTest {
	private val zone = ZoneId.of("UTC")
	private val today = LocalDate.of(2026, 8, 18)

	private fun epoch(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

	private fun job(
		status: JobStatus,
		created: LocalDate = today,
		closed: LocalDate? = null,
		price: Long? = 100_000L,
	) = Job(
		id = "j1",
		clientId = "c1",
		title = "Baie Mario",
		status = status,
		agreedPriceCents = price,
		createdAt = epoch(created),
		closedAt = closed?.let(::epoch),
	)

	private fun kinds(
		job: Job,
		invoices: List<InvoiceRef> = emptyList(),
		todos: List<Todo> = emptyList(),
		existing: Set<ReminderKey> = emptySet(),
	) = ReminderRules.candidates(
		today = today,
		zoneId = zone,
		jobs = listOf(job),
		workDays = emptyList(),
		measures = emptyList(),
		extras = emptyList(),
		invoices = invoices,
		todos = todos,
		existingOpen = existing,
	)

	@Test
	fun terminata_de_trei_zile_cu_rest_de_facturat_creeaza_memento() {
		val result = kinds(job(JobStatus.TERMINAT, closed = today.minusDays(3)))
		assertEquals(listOf(ReminderKind.TO_INVOICE), result.map { it.kind })
	}

	@Test
	fun complet_facturata_nu_creeaza_memento_de_facturare() {
		val invoice = InvoiceRef(id = "i1", jobId = "j1", amountCents = 100_000L)
		assertTrue(kinds(job(JobStatus.TERMINAT, closed = today.minusDays(4)), listOf(invoice)).isEmpty())
	}

	@Test
	fun factura_scadenta_spune_numarul_de_zile() {
		val invoice = InvoiceRef(
			id = "i1",
			jobId = "j1",
			amountCents = 50_000L,
			due = today.minusDays(5),
			paid = false,
		)
		val result = kinds(job(JobStatus.IN_LUCRU), listOf(invoice))
		assertEquals(ReminderKind.OVERDUE_INVOICE, result.single().kind)
		assertEquals(5L, result.single().overdueDays)
	}

	@Test
	fun oferta_se_reaminteste_la_3_7_si_14_zile() {
		for (age in listOf(3L, 7L, 14L)) {
			val result = kinds(job(JobStatus.OFERTAT, created = today.minusDays(age)))
			assertEquals(ReminderKind.OFFER_FOLLOW_UP, result.single().kind)
		}
		assertTrue(kinds(job(JobStatus.OFERTAT, created = today.minusDays(4))).isEmpty())
	}

	@Test
	fun restul_cu_termen_azi_creeaza_un_singur_memento_pe_lucrare() {
		val todos = listOf(
			Todo(id = "t1", jobId = "j1", what = "Silicon", due = today),
			Todo(id = "t2", jobId = "j1", what = "Prag", due = today),
		)
		val result = kinds(job(JobStatus.IN_LUCRU), todos = todos)
		assertEquals(listOf(ReminderKind.TODO_DUE), result.map { it.kind })
	}

	@Test
	fun rularea_repetata_nu_dubleaza_memento_ul_deschis() {
		val key = ReminderKey("j1", ReminderKind.TO_INVOICE)
		val result = kinds(
			job(JobStatus.TERMINAT, closed = today.minusDays(5)),
			existing = setOf(key),
		)
		assertTrue(result.isEmpty())
	}
}
