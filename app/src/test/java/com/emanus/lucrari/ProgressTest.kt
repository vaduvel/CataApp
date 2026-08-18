package com.emanus.lucrari

import com.emanus.lucrari.domain.Progress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** SPEC §5.7: bara de avansare și estimat vs. real. */
class ProgressTest {

	@Test
	fun fara_etape_avansarea_e_zero() {
		assertEquals(0f, Progress.ofStages(done = 0, total = 0), 0.0001f)
	}

	@Test
	fun doua_din_sapte_etape() {
		assertEquals(0.2857f, Progress.ofStages(done = 2, total = 7), 0.0001f)
	}

	@Test
	fun toate_etapele_bifate_inseamna_unu() {
		assertEquals(1f, Progress.ofStages(done = 7, total = 7), 0.0001f)
	}

	@Test
	fun avansarea_nu_trece_de_unu_daca_datele_sunt_stricate() {
		assertEquals(1f, Progress.ofStages(done = 9, total = 7), 0.0001f)
	}

	@Test
	fun fara_estimare_nu_exista_comparatie() {
		assertNull(Progress.daysVsEstimate(estDays = null, workedDays = 5))
		assertNull(Progress.daysVsEstimate(estDays = 0, workedDays = 5))
	}

	@Test
	fun mai_are_o_zi_pana_la_estimat() {
		assertEquals(-1, Progress.daysVsEstimate(estDays = 3, workedDays = 2))
	}

	@Test
	fun doua_zile_peste_estimat() {
		assertEquals(2, Progress.daysVsEstimate(estDays = 3, workedDays = 5))
	}
}
