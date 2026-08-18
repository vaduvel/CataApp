package com.emanus.lucrari

import com.emanus.lucrari.domain.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** SPEC §5.1: banii stau în cenți, iar textul arată la fel pe orice telefon. */
class MoneyTest {

	@Test
	fun scrie_punct_la_mii_si_virgula_la_bani() {
		assertEquals("2.400,00 €", Money.format(240_000))
		assertEquals("800,00", Money.plain(80_000))
		assertEquals("180,00", Money.plain(18_000))
	}

	@Test
	fun scrie_si_sumele_mici() {
		assertEquals("0,00", Money.plain(0))
		assertEquals("0,05", Money.plain(5))
		assertEquals("0,50", Money.plain(50))
		assertEquals("1,00", Money.plain(100))
	}

	@Test
	fun scrie_milioanele_cu_două_puncte() {
		assertEquals("1.234.567,89", Money.plain(123_456_789))
	}

	@Test
	fun scrie_si_sumele_negative() {
		assertEquals("-180,00", Money.plain(-18_000))
	}

	@Test
	fun citeste_numarul_simplu_cu_sau_fara_euro() {
		assertEquals(18_000L, Money.parse("180"))
		assertEquals(18_000L, Money.parse(" 180 € "))
	}

	@Test
	fun citeste_virgula_si_punctul_ca_zecimale() {
		assertEquals(18_050L, Money.parse("180,50"))
		assertEquals(18_050L, Money.parse("180.50"))
	}

	@Test
	fun un_punct_cu_trei_cifre_dupa_el_inseamna_mie() {
		assertEquals(180_000L, Money.parse("1.800"))
		assertEquals(180_050L, Money.parse("1.800,50"))
	}

	@Test
	fun virgula_ramane_zecimala_si_rotunjeste_la_cent() {
		assertEquals(1_246L, Money.parse("12,456"))
		assertEquals(1_245L, Money.parse("12,454"))
	}

	@Test
	fun textul_stricat_nu_trece_si_dus_intors_pastreaza_suma() {
		assertNull(Money.parse(""))
		assertNull(Money.parse("abc"))
		assertNull(Money.parse("12x"))
		assertEquals(240_000L, Money.parse(Money.plain(240_000)))
		assertEquals(178_000L, Money.parse(Money.format(178_000)))
	}
}
