package com.emanus.lucrari

import com.emanus.lucrari.domain.Seed
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Datele demo intră o singură dată (SPEC §10). */
class SeedRuleTest {

	@Test
	fun la_prima_pornire_pe_baza_goala_se_pun_datele_demo() {
		assertTrue(Seed.shouldSeed(alreadySeeded = false, clientCount = 0))
	}

	@Test
	fun dupa_prima_pornire_demo_ul_nu_mai_revine_pe_baza_goala() {
		// Cazul din teren: import „Înlocuiește tot” cu arhivă goală, apoi repornire.
		assertFalse(Seed.shouldSeed(alreadySeeded = true, clientCount = 0))
	}

	@Test
	fun stergerea_manuala_a_demo_ului_este_definitiva() {
		assertFalse(Seed.shouldSeed(alreadySeeded = true, clientCount = 0))
	}

	@Test
	fun o_baza_cu_date_nu_se_atinge_niciodata() {
		assertFalse(Seed.shouldSeed(alreadySeeded = false, clientCount = 1))
		assertFalse(Seed.shouldSeed(alreadySeeded = true, clientCount = 1))
	}
}
