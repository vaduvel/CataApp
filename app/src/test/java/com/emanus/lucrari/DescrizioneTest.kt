package com.emanus.lucrari

import com.emanus.lucrari.data.Billing
import com.emanus.lucrari.data.Client
import com.emanus.lucrari.data.Extra
import com.emanus.lucrari.data.InvoiceKind
import com.emanus.lucrari.data.InvoiceRef
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.data.Measure
import com.emanus.lucrari.data.MeasureUnit
import com.emanus.lucrari.data.Stage
import com.emanus.lucrari.data.WorkDay
import com.emanus.lucrari.domain.descrizione
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Textul de factură e singurul lucru din aplicație care iese în afară, la client și la
 * contabil. De aceea se compară șir cu șir, nu pe bucăți.
 */
class DescrizioneTest {

	private val client = Client(id = "c1", name = "Mario", phone = "+39 333 000 0000")

	private val job = Job(
		id = "j1",
		clientId = "c1",
		title = "Rifacimento bagno",
		street = "Via 23",
		city = "Milano",
		status = JobStatus.IN_LUCRU,
		estDays = 3,
		billing = Billing.CORP,
		agreedPriceCents = 240_000,
	)

	private val days = listOf(
		WorkDay(id = "d1", jobId = "j1", date = LocalDate.of(2026, 8, 10), what = "Demolare"),
		WorkDay(id = "d2", jobId = "j1", date = LocalDate.of(2026, 8, 12), what = "Trasee instalații"),
	)

	private val stages = listOf(
		Stage(id = "s1", jobId = "j1", name = "Demolare", sort = 0, done = true),
		Stage(id = "s2", jobId = "j1", name = "Trasee instalații", sort = 1, done = true),
		Stage(id = "s3", jobId = "j1", name = "Impermeabilizare", sort = 2, done = false),
	)

	private val measures = listOf(
		Measure(
			id = "m1",
			jobId = "j1",
			place = "Bagno — pavimento",
			qty = 12.4,
			unit = MeasureUnit.M2,
			date = LocalDate.of(2026, 8, 12),
		),
	)

	private val extras = listOf(
		Extra(
			id = "e1",
			jobId = "j1",
			what = "nicchia doccia + spostamento presa",
			date = LocalDate.of(2026, 8, 11),
			priceCents = 18_000,
			accepted = true,
			proof = "vocală WhatsApp 11/08",
		),
	)

	private val invoices = listOf(
		InvoiceRef(
			id = "i1",
			jobId = "j1",
			number = "1/2026",
			date = LocalDate.of(2026, 8, 10),
			amountCents = 80_000,
			kind = InvoiceKind.ACONTO,
			paid = true,
		),
	)

	@Test
	fun textul_golden_pentru_lucrarea_demo() {
		val expected = listOf(
			"Rifacimento bagno — Mario, Via 23, Milano",
			"Periodo: 10/08 – 12/08/2026 (2 giornate)",
			"Lavorazioni eseguite:",
			"- demolizione",
			"- tracce impianti",
			"Misure:",
			"- Bagno — pavimento: 12,40 m²",
			"Extra concordati:",
			"- nicchia doccia + spostamento presa — 180,00 €",
			"",
			"Concordato: 2.400,00 € + extra 180,00 € = 2.580,00 €",
			"Acconti già fatturati: 800,00 €",
			"Da fatturare: 1.780,00 €",
		).joinToString("\n")

		val actual = descrizione(job, client, days, stages, measures, extras, invoices)

		assertEquals(expected, actual)
	}

	@Test
	fun o_singura_zi_scrie_data_nu_perioada() {
		val text = descrizione(
			job,
			client,
			listOf(days.first()),
			stages,
			measures,
			extras,
			invoices,
		)

		assertTrue(text.contains("Data: 10/08/2026 (1 giornata)"))
		assertTrue(!text.contains("Periodo:"))
	}

	@Test
	fun fara_etape_bifate_se_folosesc_textele_zilelor() {
		val nimicBifat = stages.map { it.copy(done = false) }

		val text = descrizione(job, client, days, nimicBifat, measures, extras, invoices)

		assertTrue(text.contains("Lavorazioni eseguite:\n- demolizione\n- tracce impianti"))
	}

	@Test
	fun ce_nu_e_in_dictionar_ramane_cum_a_scris_el() {
		val scrisDeMana = listOf(
			Stage(id = "s9", jobId = "j1", name = "Montat oglinda mare", sort = 0, done = true),
		)

		val text = descrizione(job, client, days, scrisDeMana, measures, extras, invoices)

		assertTrue(text.contains("- Montat oglinda mare"))
	}

	@Test
	fun extra_neacceptat_nu_apare_si_nu_intra_in_cifre() {
		val neacceptat = listOf(extras.first().copy(accepted = false))

		val text = descrizione(job, client, days, stages, measures, neacceptat, invoices)

		assertTrue(!text.contains("Extra concordati:"))
		assertTrue(text.contains("Concordato: 2.400,00 €\n"))
		assertTrue(text.contains("Da fatturare: 1.600,00 €"))
	}

	@Test
	fun fara_masuratori_si_fara_facturi_nu_apar_randurile_lor() {
		val text = descrizione(job, client, days, stages, emptyList(), extras, emptyList())

		assertTrue(!text.contains("Misure:"))
		assertTrue(!text.contains("Acconti"))
		assertTrue(text.contains("Da fatturare: 2.580,00 €"))
	}

	@Test
	fun la_zile_apare_manopera_cu_tariful() {
		val peZile = job.copy(
			billing = Billing.ZILE,
			agreedPriceCents = null,
			dayRateCents = 15_000,
		)

		val text = descrizione(peZile, client, days, stages, measures, extras, invoices)

		assertTrue(text.contains("Manodopera: 2 giornate × 150,00 €"))
		assertTrue(text.contains("Concordato: 300,00 € + extra 180,00 € = 480,00 €"))
	}

	@Test
	fun fara_adresa_si_fara_zile_ramane_doar_ce_exista() {
		val golHen = job.copy(street = null, city = null)

		val text = descrizione(golHen, client, emptyList(), stages, emptyList(), emptyList(), emptyList())

		assertEquals(
			listOf(
				"Rifacimento bagno — Mario",
				"Lavorazioni eseguite:",
				"- demolizione",
				"- tracce impianti",
				"",
				"Concordato: 2.400,00 €",
				"Da fatturare: 2.400,00 €",
			).joinToString("\n"),
			text,
		)
	}
}
