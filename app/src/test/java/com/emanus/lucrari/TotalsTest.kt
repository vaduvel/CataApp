package com.emanus.lucrari

import com.emanus.lucrari.data.Billing
import com.emanus.lucrari.data.Extra
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.Measure
import com.emanus.lucrari.data.MeasureUnit
import com.emanus.lucrari.domain.Totals
import org.junit.Assert.assertEquals
import org.junit.Test

class TotalsTest {

	private fun job(
		billing: Billing = Billing.CORP,
		agreedPriceCents: Long? = null,
		dayRateCents: Long? = null,
	) = Job(
		clientId = "c1",
		title = "Lucrare",
		billing = billing,
		agreedPriceCents = agreedPriceCents,
		dayRateCents = dayRateCents,
	)

	private fun measure(qty: Double, unitPriceCents: Long?) = Measure(
		jobId = "j1",
		place = "Baie",
		qty = qty,
		unit = MeasureUnit.M2,
		unitPriceCents = unitPriceCents,
	)

	private fun extra(priceCents: Long, accepted: Boolean, billable: Boolean = true) = Extra(
		jobId = "j1",
		what = "Extra",
		priceCents = priceCents,
		accepted = accepted,
		billable = billable,
	)

	@Test
	fun la_corp_baza_e_pretul_convenit() {
		val totals = Totals.of(
			job = job(Billing.CORP, agreedPriceCents = 240_000),
			workedDays = 5,
			measures = listOf(measure(12.4, 4_500)),
			extras = emptyList(),
			invoicedCents = 0,
			collectedCents = 0,
		)
		// Nici zilele, nici masuratorile nu misca pretul convenit la corp.
		assertEquals(240_000L, totals.baseCents)
		assertEquals(240_000L, totals.totalCents)
	}

	@Test
	fun la_corp_fara_pret_convenit_baza_e_zero() {
		val totals = Totals.of(job(), 3, emptyList(), emptyList(), 0, 0)
		assertEquals(0L, totals.baseCents)
	}

	@Test
	fun pe_zile_baza_e_tariful_ori_zilele_lucrate() {
		val totals = Totals.of(
			job = job(Billing.ZILE, dayRateCents = 15_000),
			workedDays = 3,
			measures = emptyList(),
			extras = emptyList(),
			invoicedCents = 0,
			collectedCents = 0,
		)
		assertEquals(45_000L, totals.baseCents)
	}

	@Test
	fun la_masura_baza_e_suma_randurilor_cu_pret() {
		val totals = Totals.of(
			job = job(Billing.MASURA),
			workedDays = 0,
			measures = listOf(
				measure(12.4, 4_500),
				measure(2.0, 1_000),
				measure(30.0, null),
			),
			extras = emptyList(),
			invoicedCents = 0,
			collectedCents = 0,
		)
		// 55.800 + 2.000, iar randul fara pret nu aduce nimic.
		assertEquals(57_800L, totals.baseCents)
	}

	@Test
	fun extra_intra_doar_daca_e_acceptat_si_se_pune_pe_factura() {
		val totals = Totals.of(
			job = job(Billing.CORP, agreedPriceCents = 240_000),
			workedDays = 0,
			measures = emptyList(),
			extras = listOf(
				extra(18_000, accepted = true),
				extra(5_000, accepted = true, billable = false),
				extra(12_000, accepted = false),
			),
			invoicedCents = 0,
			collectedCents = 0,
		)
		assertEquals(18_000L, totals.extrasCents)
		assertEquals(258_000L, totals.totalCents)
	}

	@Test
	fun facturat_nu_inseamna_incasat() {
		val totals = Totals.of(
			job = job(Billing.CORP, agreedPriceCents = 240_000),
			workedDays = 0,
			measures = emptyList(),
			extras = listOf(extra(18_000, accepted = true)),
			invoicedCents = 80_000,
			collectedCents = 0,
		)
		// A trimis o factura de acont, dar banii n-au intrat inca.
		assertEquals(178_000L, totals.toInvoiceCents)
		assertEquals(258_000L, totals.outstandingCents)
	}

	@Test
	fun restul_de_incasat_scade_doar_cand_intra_banii() {
		val totals = Totals.of(
			job = job(Billing.CORP, agreedPriceCents = 240_000),
			workedDays = 0,
			measures = emptyList(),
			extras = emptyList(),
			invoicedCents = 80_000,
			collectedCents = 80_000,
		)
		assertEquals(160_000L, totals.toInvoiceCents)
		assertEquals(160_000L, totals.outstandingCents)
	}

	@Test
	fun de_facturat_iese_negativ_daca_s_a_facturat_prea_mult() {
		val totals = Totals.of(
			job = job(Billing.CORP, agreedPriceCents = 100_000),
			workedDays = 0,
			measures = emptyList(),
			extras = emptyList(),
			invoicedCents = 120_000,
			collectedCents = 0,
		)
		assertEquals(-20_000L, totals.toInvoiceCents)
	}

	@Test
	fun lucrarea_fara_nimic_are_toate_cifrele_zero() {
		val totals = Totals.of(job(), 0, emptyList(), emptyList(), 0, 0)
		assertEquals(0L, totals.totalCents)
		assertEquals(0L, totals.toInvoiceCents)
		assertEquals(0L, totals.outstandingCents)
	}
}
