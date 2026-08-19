package com.emanus.lucrari

import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.domain.ReminderKey
import com.emanus.lucrari.domain.ReminderKind
import com.emanus.lucrari.domain.ReminderRules
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Memento-urile de început din M8: cu 3 zile înainte, în ajun și în dimineața zilei. */
class StartReminderTest {
	private val zone = ZoneId.of("UTC")
	private val today = LocalDate.of(2026, 8, 19)

	private fun at(hour: Int, minute: Int): Long =
		today.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()

	private fun job(status: JobStatus, start: LocalDate?) = Job(
		id = "j1",
		clientId = "c1",
		title = "Tencuială Mario",
		status = status,
		plannedStart = start,
		estDays = 5,
		createdAt = today.atStartOfDay(zone).toInstant().toEpochMilli(),
	)

	private fun run(
		job: Job,
		kinds: Set<ReminderKind> = ReminderKind.entries.toSet(),
		existing: Set<ReminderKey> = emptySet(),
	) = ReminderRules.candidates(
		today = today,
		zoneId = zone,
		jobs = listOf(job),
		workDays = emptyList(),
		measures = emptyList(),
		extras = emptyList(),
		invoices = emptyList(),
		todos = emptyList(),
		existingOpen = existing,
		kinds = kinds,
	)

	@Test
	fun programata_peste_trei_zile_cere_materialele_de_seara() {
		val result = run(job(JobStatus.PROGRAMAT, today.plusDays(3)))
		assertEquals(ReminderKind.START_SOON, result.single().kind)
		assertEquals(at(19, 0), result.single().dueAt)
	}

	@Test
	fun programata_maine_anunta_in_ajun() {
		val result = run(job(JobStatus.PROGRAMAT, today.plusDays(1)))
		assertEquals(ReminderKind.START_TOMORROW, result.single().kind)
		assertEquals(at(19, 0), result.single().dueAt)
	}

	@Test
	fun programata_azi_anunta_dimineata() {
		val result = run(job(JobStatus.PROGRAMAT, today))
		assertEquals(ReminderKind.START_TODAY, result.single().kind)
		assertEquals(at(7, 30), result.single().dueAt)
	}

	@Test
	fun zilele_dintre_praguri_nu_dau_niciun_memento() {
		for (days in listOf(2L, 4L, 10L)) {
			assertTrue(run(job(JobStatus.PROGRAMAT, today.plusDays(days))).isEmpty())
		}
		assertTrue(run(job(JobStatus.PROGRAMAT, today.minusDays(1))).isEmpty())
		assertTrue(run(job(JobStatus.PROGRAMAT, null)).isEmpty())
	}

	@Test
	fun lucrarea_inceputa_sau_anulata_nu_mai_da_memento_de_inceput() {
		assertTrue(run(job(JobStatus.IN_LUCRU, today)).isEmpty())
		assertTrue(run(job(JobStatus.ANULAT, today.plusDays(1))).isEmpty())
	}

	@Test
	fun rularea_de_seara_lasa_memento_ul_de_azi_pentru_dimineata() {
		val startsToday = job(JobStatus.PROGRAMAT, today)
		assertTrue(run(startsToday, kinds = ReminderRules.EVENING_KINDS).isEmpty())
		assertEquals(
			ReminderKind.START_TODAY,
			run(startsToday, kinds = ReminderRules.MORNING_KINDS).single().kind,
		)
	}

	@Test
	fun rularea_de_dimineata_nu_repeta_ce_a_spus_seara() {
		assertTrue(run(job(JobStatus.PROGRAMAT, today.plusDays(1)), kinds = ReminderRules.MORNING_KINDS).isEmpty())
	}

	@Test
	fun memento_ul_de_inceput_deschis_nu_se_dubleaza() {
		val existing = setOf(ReminderKey("j1", ReminderKind.START_TOMORROW))
		assertTrue(run(job(JobStatus.PROGRAMAT, today.plusDays(1)), existing = existing).isEmpty())
	}
}
