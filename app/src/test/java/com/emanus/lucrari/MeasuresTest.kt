package com.emanus.lucrari

import com.emanus.lucrari.data.Measure
import com.emanus.lucrari.data.MeasureUnit
import com.emanus.lucrari.domain.Measures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasuresTest {

	private fun measure(
		qty: Double,
		price: Long? = null,
		unit: MeasureUnit = MeasureUnit.M2,
	) = Measure(jobId = "job", place = "Bagno", qty = qty, unit = unit, unitPriceCents = price)

	@Test
	fun randul_fara_pret_nu_face_bani() {
		assertNull(Measures.lineCents(12.4, null))
		assertFalse(Measures.anyPriced(listOf(measure(12.4))))
	}

	@Test
	fun randul_cu_pret_se_rotunjeste_la_cent() {
		assertEquals(55_800L, Measures.lineCents(12.4, 4_500))
		assertEquals(333L, Measures.lineCents(3.333, 100))
	}

	@Test
	fun totalul_sare_peste_randurile_fara_pret() {
		val lines = listOf(measure(12.4, 4_500), measure(3.0), measure(2.0, 1_000))
		assertEquals(57_800L, Measures.totalCents(lines))
		assertTrue(Measures.anyPriced(lines))
	}

	@Test
	fun cantitatile_rotunde_se_scriu_fara_zecimale() {
		assertEquals("3", Measures.formatQty(3.0))
		assertEquals("12,40", Measures.formatQty(12.4))
		assertEquals("1.234,50", Measures.formatQty(1_234.5))
	}

	@Test
	fun cantitatea_are_unitatea_langa_ea() {
		assertEquals("12,40 m²", Measures.formatQtyWithUnit(measure(12.4)))
		assertEquals("3 buc", Measures.formatQtyWithUnit(measure(3.0, unit = MeasureUnit.BUC)))
	}

	@Test
	fun citeste_cantitatea_scrisa_de_mana() {
		assertEquals(12.4, Measures.parseQty("12,4") ?: 0.0, 0.0001)
		assertEquals(12.4, Measures.parseQty("12.4") ?: 0.0, 0.0001)
		assertEquals(3.0, Measures.parseQty("3") ?: 0.0, 0.0001)
		assertNull(Measures.parseQty("cam mult"))
	}
}
