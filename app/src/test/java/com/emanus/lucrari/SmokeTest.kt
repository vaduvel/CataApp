package com.emanus.lucrari

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * M0: confirmă doar că lanțul de teste rulează.
 * Testele reale (Money, Descrizione, Rules) intră la M4–M6, conform SPEC §10.
 */
class SmokeTest {
	@Test
	fun testChainRuns() {
		assertEquals(4, 2 + 2)
	}
}
