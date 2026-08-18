package com.emanus.lucrari.domain

import com.emanus.lucrari.data.Billing
import com.emanus.lucrari.data.Extra
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.Measure

/**
 * Cifrele unei lucrari, toate in centi (SPEC 5.1). Facturat si incasat sunt doua numere
 * separate si raman separate: unul spune cat a cerut pe hartie, celalalt cat a intrat in
 * mana. Nu se deduce niciodata unul din celalalt.
 */
data class JobTotals(
	val baseCents: Long,
	val extrasCents: Long,
	val totalCents: Long,
	val invoicedCents: Long,
	val collectedCents: Long,
	val toInvoiceCents: Long,
	val outstandingCents: Long,
)

/** Toata aritmetica banilor, pura si testabila fara Android (SPEC 5.1). */
object Totals {

	/**
	 * Cat valoreaza lucrarea in sine, dupa felul in care s-a inteles la plata:
	 * la corp pretul convenit, pe zile tariful ori zilele trecute, la masura suma randurilor
	 * de masuratoare care au pret pe unitate.
	 */
	fun baseCents(job: Job, workedDays: Int, measures: List<Measure>): Long = when (job.billing) {
		Billing.CORP -> job.agreedPriceCents ?: 0L
		Billing.ZILE -> (job.dayRateCents ?: 0L) * workedDays
		Billing.MASURA -> Measures.totalCents(measures)
	}

	/**
	 * Un extra intra in bani doar daca sunt adevarate doua lucruri deodata: clientul a fost
	 * de acord si extra-ul se pune pe factura. Cel facut din buna vointa ramane scris in
	 * aplicatie, ca sa existe dovada muncii, dar nu se cere pe factura.
	 */
	fun extrasCents(extras: List<Extra>): Long =
		extras.filter { it.accepted && it.billable }.sumOf { it.priceCents }

	fun of(
		job: Job,
		workedDays: Int,
		measures: List<Measure>,
		extras: List<Extra>,
		invoicedCents: Long,
		collectedCents: Long,
	): JobTotals {
		val base = baseCents(job, workedDays, measures)
		val extra = extrasCents(extras)
		val total = base + extra
		return JobTotals(
			baseCents = base,
			extrasCents = extra,
			totalCents = total,
			invoicedCents = invoicedCents,
			collectedCents = collectedCents,
			// Cifrele pot iesi negative si asa raman: daca a facturat mai mult decat valoreaza
			// lucrarea, vrem sa se vada, nu sa fie ascuns cu un zero.
			toInvoiceCents = total - invoicedCents,
			outstandingCents = total - collectedCents,
		)
	}
}
