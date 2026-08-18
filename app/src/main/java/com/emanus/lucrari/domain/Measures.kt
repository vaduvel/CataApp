package com.emanus.lucrari.domain

import com.emanus.lucrari.data.Measure

/**
 * Măsurătorile (SPEC §4 și §5.1): cantitate ori preț pe unitate, rotunjit la cent, adunat pe
 * lucrare. Cantitățile sunt Double, pentru că 12,4 m² e o măsură, nu bani. Banii rămân Long.
 *
 * Un rând fără preț pe unitate nu e o greșeală: la lucrările la corp măsura se notează doar ca
 * să ajungă în textul facturii, iar suma vine din prețul înțeles.
 */
object Measures {

	/** Cât face rândul. Null când n-are preț pe unitate. */
	fun lineCents(qty: Double, unitPriceCents: Long?): Long? {
		if (unitPriceCents == null) return null
		if (qty.isNaN() || qty.isInfinite()) return null
		return Math.round(qty * unitPriceCents)
	}

	fun lineCents(measure: Measure): Long? = lineCents(measure.qty, measure.unitPriceCents)

	/** Suma rândurilor cu preț. Cele fără preț rămân în evidență, dar nu intră în bani. */
	fun totalCents(measures: List<Measure>): Long = measures.sumOf { lineCents(it) ?: 0L }

	fun anyPriced(measures: List<Measure>): Boolean = measures.any { it.unitPriceCents != null }

	/** 12.4 -> 12,40 și 3.0 -> 3. Cifrele rotunde se scriu fără zecimale, ca la buc și zile. */
	fun formatQty(qty: Double): String {
		val hundredths = Math.round(qty * 100)
		return if (hundredths % 100 == 0L) {
			(hundredths / 100).toString()
		} else {
			Money.plain(hundredths)
		}
	}

	fun formatQtyWithUnit(measure: Measure): String =
		formatQty(measure.qty) + " " + measure.unit.label

	/**
	 * Citește cantitatea scrisă de mână: 12,4 sau 12.4 sau 3. Trece prin același cititor ca
	 * banii, deci și aici un punct cu trei cifre după el înseamnă mie.
	 */
	fun parseQty(input: String): Double? {
		val cents = Money.parse(input) ?: return null
		if (cents < 0) return null
		return cents / 100.0
	}
}
