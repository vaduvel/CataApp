package com.emanus.lucrari

import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.domain.Rules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC §5.6. */
class RulesTest {

	@Test
	fun zi_blocata_trage_lucrarea_in_asteptare() {
		assertEquals(JobStatus.ASTEPTARE, Rules.statusAfterBlockedDay(JobStatus.IN_LUCRU))
		assertEquals(JobStatus.ASTEPTARE, Rules.statusAfterBlockedDay(JobStatus.PROGRAMAT))
		assertEquals(JobStatus.ASTEPTARE, Rules.statusAfterBlockedDay(JobStatus.DE_FINISAT))
	}

	@Test
	fun zi_blocata_nu_reinvie_o_lucrare_inchisa() {
		assertNull(Rules.statusAfterBlockedDay(JobStatus.TERMINAT))
		assertNull(Rules.statusAfterBlockedDay(JobStatus.ANULAT))
	}

	@Test
	fun zi_blocata_pe_lucrare_deja_in_asteptare_nu_schimba_nimic() {
		assertNull(Rules.statusAfterBlockedDay(JobStatus.ASTEPTARE))
	}

	@Test
	fun terminat_cu_resturi_cere_confirmare() {
		assertTrue(Rules.needsConfirmForDone(1))
		assertTrue(Rules.needsConfirmForDone(9))
	}

	@Test
	fun terminat_fara_resturi_nu_cere_nimic() {
		assertFalse(Rules.needsConfirmForDone(0))
	}

	@Test
	fun toate_etapele_bifate_cu_resturi_propun_de_finisat() {
		assertTrue(
			Rules.suggestsDeFinisat(
				JobStatus.IN_LUCRU,
				stageCount = 7,
				openStages = 0,
				openTodos = 2,
			),
		)
	}

	@Test
	fun fara_resturi_nu_se_propune_nimic() {
		assertFalse(
			Rules.suggestsDeFinisat(
				JobStatus.IN_LUCRU,
				stageCount = 7,
				openStages = 0,
				openTodos = 0,
			),
		)
	}

	@Test
	fun cu_etape_nebifate_nu_se_propune_nimic() {
		assertFalse(
			Rules.suggestsDeFinisat(
				JobStatus.IN_LUCRU,
				stageCount = 7,
				openStages = 3,
				openTodos = 2,
			),
		)
	}

	@Test
	fun lucrarea_fara_etape_nu_propune_de_finisat() {
		assertFalse(
			Rules.suggestsDeFinisat(
				JobStatus.IN_LUCRU,
				stageCount = 0,
				openStages = 0,
				openTodos = 2,
			),
		)
	}
}
