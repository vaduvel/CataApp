package com.emanus.lucrari.domain

/**
 * Avansarea unei lucrări (SPEC §5.7). Pură, fără Android, ca să se poată testa în
 * `testDebugUnitTest`. Composable-urile doar afișează ce întoarce de aici.
 */
object Progress {

	/** Fracție 0f..1f pentru bara de avansare. Fără etape, avansarea e 0. */
	fun ofStages(done: Int, total: Int): Float =
		if (total <= 0) 0f else (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)

	/**
	 * Zilele lucrate față de cele estimate: negativ = mai are zile, 0 = fix pe estimat,
	 * pozitiv = a depășit. Întoarce null când nu a estimat nimic, caz în care nu are rost
	 * să se afișeze vreo comparație.
	 */
	fun daysVsEstimate(estDays: Int?, workedDays: Int): Int? =
		if (estDays == null || estDays <= 0) null else workedDays - estDays
}
