package com.emanus.lucrari.domain

import com.emanus.lucrari.data.Billing
import com.emanus.lucrari.data.Client
import com.emanus.lucrari.data.Extra
import com.emanus.lucrari.data.InvoiceRef
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.Measure
import com.emanus.lucrari.data.Stage
import com.emanus.lucrari.data.WorkDay

/**
 * Textul care ajunge pe factură (SPEC §5.5). Aplicația nu emite nimic: scrie în italiană ce
 * s-a lucrat, cât s-a măsurat și ce s-a vorbit, ca el să dea copy-paste contabilului.
 *
 * Funcția e pură și nu atinge Android, tocmai ca testul golden să poată compara șir cu șir.
 * Toate formaterele vin din `Money`, `Measures` și `Dates`, deci textul iese identic pe orice
 * telefon, indiferent de limba sistemului.
 *
 * Ce nu intră niciodată în text: TVA, cote, date fiscale, numărul facturii și banii încasați
 * în mână. Restul de încasat se vede doar în aplicație.
 */
fun descrizione(
	job: Job,
	client: Client,
	days: List<WorkDay>,
	stages: List<Stage>,
	measures: List<Measure>,
	extras: List<Extra>,
	invoices: List<InvoiceRef>,
): String {
	val out = StringBuilder()

	// 1. Cine și unde. Părțile care lipsesc se sar, fără virgule duble.
	val where = listOfNotNull(
		client.name.trim().ifEmpty { null },
		job.street?.trim()?.ifEmpty { null },
		job.city?.trim()?.ifEmpty { null },
	).joinToString(", ")
	out.append(if (where.isEmpty()) job.title else job.title + " \u2014 " + where)

	// 2. Când s-a lucrat.
	val dates = days.map { it.date }.sorted()
	if (dates.isNotEmpty()) {
		if (dates.size == 1) {
			out.append("\nData: " + Dates.full(dates.first()) + " (1 giornata)")
		} else {
			out.append(
				"\nPeriodo: " + Dates.dayMonth(dates.first()) + " \u2013 " +
					Dates.full(dates.last()) + " (" + dates.size + " giornate)",
			)
		}
	}

	// 3. Ce s-a lucrat: etapele bifate, iar dacă nu e bifată niciuna, textele zilelor.
	val doneStages = stages.filter { it.done }.sortedBy { it.sort }
	val lavorazioni = if (doneStages.isNotEmpty()) {
		doneStages.map { Dictionary.translate(it.name) }
	} else {
		days.sortedBy { it.date }
			.mapNotNull { it.what?.trim()?.ifEmpty { null } }
			.distinct()
			.map { Dictionary.translate(it) }
	}
	if (lavorazioni.isNotEmpty()) {
		out.append("\nLavorazioni eseguite:")
		lavorazioni.forEach { out.append("\n- " + it) }
	}

	// 4. Măsurătorile, exact cum le-a scris în libret.
	if (measures.isNotEmpty()) {
		out.append("\nMisure:")
		measures.forEach {
			out.append("\n- " + it.place + ": " + Measures.formatQtyWithUnit(it))
		}
	}

	// 5. La zile se scrie și manopera.
	if (job.billing == Billing.ZILE) {
		val giornate = if (dates.size == 1) "1 giornata" else dates.size.toString() + " giornate"
		out.append("\nManodopera: " + giornate + " \u00d7 " + Money.format(job.dayRateCents ?: 0L))
	}

	// 6. Extra, doar cele acceptate de client și puse pe factură.
	val extraConcordati = extras.filter { it.accepted && it.billable }
	if (extraConcordati.isNotEmpty()) {
		out.append("\nExtra concordati:")
		extraConcordati.forEach {
			out.append("\n- " + it.what + " \u2014 " + Money.format(it.priceCents))
		}
	}

	// 7. Cifrele. Aritmetica stă în Totals, aici doar se scrie.
	val baseCents = Totals.baseCents(job, dates.size, measures)
	val extrasCents = Totals.extrasCents(extras)
	val totalCents = baseCents + extrasCents
	val invoicedCents = invoices.sumOf { it.amountCents }

	out.append("\n\nConcordato: " + Money.format(baseCents))
	if (extrasCents != 0L) {
		out.append(" + extra " + Money.format(extrasCents) + " = " + Money.format(totalCents))
	}
	if (invoicedCents > 0L) {
		out.append("\nAcconti già fatturati: " + Money.format(invoicedCents))
	}
	out.append("\nDa fatturare: " + Money.format(totalCents - invoicedCents))

	return out.toString()
}
