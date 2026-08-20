package com.emanus.lucrari

import com.emanus.lucrari.domain.Templates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplatesTest {

	@Test
	fun tipurile_multiple_se_pastreaza_intr_un_singur_camp() {
		val stored = Templates.combineTypes(
			listOf("Baie completă", "Tencuială", "Baie completă"),
		)

		assertEquals("Baie completă + Tencuială", stored)
		assertEquals(listOf("Baie completă", "Tencuială"), Templates.typesFor(stored))
	}

	@Test
	fun etapele_sabloanelor_multiple_se_unesc_fara_duplicate() {
		val stages = Templates.stagesFor("Rigips + Tencuială")

		assertEquals(stages.size, stages.distinct().size)
		assertEquals(1, stages.count { stage -> stage == "Șlefuit" })
		assertTrue(stages.contains("Structură"))
		assertTrue(stages.contains("Glet"))
	}
}
