package com.emanus.lucrari

import com.emanus.lucrari.domain.MonthGrid
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** SPEC §11, M8: calendarul începe săptămâna luni și nu pierde nicio zi. */
class MonthGridTest {

	@Test
	fun august_2026_incepe_sambata_deci_are_cinci_casete_goale_la_inceput() {
		val cells = MonthGrid.cells(YearMonth.of(2026, 8))
		assertEquals(5, cells.takeWhile { it == null }.size)
		assertEquals(LocalDate.of(2026, 8, 1), cells[5])
		assertEquals(42, cells.size)
	}

	@Test
	fun harta_e_mereu_multiplu_de_sapte() {
		assertEquals(0, MonthGrid.cells(YearMonth.of(2026, 8)).size % 7)
		assertEquals(0, MonthGrid.cells(YearMonth.of(2026, 12)).size % 7)
		assertEquals(0, MonthGrid.cells(YearMonth.of(2027, 3)).size % 7)
	}

	@Test
	fun februarie_2027_incepe_luni_si_intra_exact_in_patru_saptamani() {
		val cells = MonthGrid.cells(YearMonth.of(2027, 2))
		assertEquals(28, cells.size)
		assertEquals(LocalDate.of(2027, 2, 1), cells.first())
		assertEquals(LocalDate.of(2027, 2, 28), cells.last())
	}

	@Test
	fun toate_zilele_lunii_apar_o_singura_data() {
		val cells = MonthGrid.cells(YearMonth.of(2026, 9))
		val days = cells.filterNotNull()
		assertEquals(30, days.size)
		assertEquals(30, days.distinct().size)
		assertNull(cells.last())
	}
}
