package com.emanus.lucrari

import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.domain.Schedule
import com.emanus.lucrari.domain.StartReminder
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Programarea lucrărilor: interval, status la creare, memento de început. */
class ScheduleTest {

	private val luni = LocalDate.of(2026, 8, 24)

	@Test
	fun cinci_zile_incepand_de_luni_se_termina_vineri() {
		assertEquals(LocalDate.of(2026, 8, 28), Schedule.endDate(luni, 5))
	}

	@Test
	fun o_lucrare_de_o_zi_incepe_si_se_termina_in_aceeasi_zi() {
		assertEquals(luni, Schedule.endDate(luni, 1))
		assertEquals(luni, Schedule.endDate(luni, null))
		assertEquals(luni, Schedule.endDate(luni, 0))
	}

	@Test
	fun intervalul_acopera_prima_si_ultima_zi() {
		assertTrue(Schedule.covers(luni, 5, luni))
		assertTrue(Schedule.covers(luni, 5, LocalDate.of(2026, 8, 26)))
		assertTrue(Schedule.covers(luni, 5, LocalDate.of(2026, 8, 28)))
	}

	@Test
	fun in_afara_intervalului_nu_apare_nimic() {
		assertFalse(Schedule.covers(luni, 5, LocalDate.of(2026, 8, 23)))
		assertFalse(Schedule.covers(luni, 5, LocalDate.of(2026, 8, 29)))
		assertFalse(Schedule.covers(null, 5, luni))
	}

	@Test
	fun o_data_din_viitor_face_lucrarea_programata() {
		assertEquals(
			JobStatus.PROGRAMAT,
			Schedule.statusForNewJob(luni, JobStatus.OFERTAT),
		)
	}

	@Test
	fun data_de_azi_este_tot_programat_pana_apasa_am_lucrat_azi_aici() {
		assertEquals(JobStatus.PROGRAMAT, Schedule.statusForNewJob(luni, JobStatus.OFERTAT))
	}

	@Test
	fun o_data_din_trecut_ramane_programat_pana_se_trece_prima_zi() {
		assertEquals(
			JobStatus.PROGRAMAT,
			Schedule.statusForNewJob(luni, JobStatus.OFERTAT),
		)
	}

	@Test
	fun fara_data_statusul_ramane_cel_implicit() {
		assertEquals(
			JobStatus.OFERTAT,
			Schedule.statusForNewJob(null, JobStatus.OFERTAT),
		)
	}

	@Test
	fun mementoul_vine_cu_trei_zile_inainte_in_ajun_si_in_ziua_respectiva() {
		assertEquals(
			StartReminder.IN_DAYS,
			Schedule.startReminder(JobStatus.PROGRAMAT, luni, LocalDate.of(2026, 8, 21)),
		)
		assertEquals(
			StartReminder.TOMORROW,
			Schedule.startReminder(JobStatus.PROGRAMAT, luni, LocalDate.of(2026, 8, 23)),
		)
		assertEquals(
			StartReminder.TODAY,
			Schedule.startReminder(JobStatus.PROGRAMAT, luni, luni),
		)
	}

	@Test
	fun in_celelalte_zile_nu_se_repeta_niciun_memento() {
		assertNull(Schedule.startReminder(JobStatus.PROGRAMAT, luni, LocalDate.of(2026, 8, 20)))
		assertNull(Schedule.startReminder(JobStatus.PROGRAMAT, luni, LocalDate.of(2026, 8, 22)))
		assertNull(Schedule.startReminder(JobStatus.PROGRAMAT, luni, LocalDate.of(2026, 8, 25)))
	}

	@Test
	fun o_lucrare_care_nu_e_programata_nu_anunta_nimic() {
		assertNull(Schedule.startReminder(JobStatus.IN_LUCRU, luni, luni))
		assertNull(Schedule.startReminder(JobStatus.TERMINAT, luni, luni))
		assertNull(Schedule.startReminder(JobStatus.PROGRAMAT, null, luni))
	}
}
