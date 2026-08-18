package com.emanus.lucrari.domain

/**
 * Dicționarul RO → IT pentru textul care ajunge la client sau la contabil (SPEC §15).
 *
 * Cheile sunt exact numele etapelor din `Templates.kt`. Dacă un text nu e în dicționar, se
 * folosește exact cum l-a scris el: mai bine un cuvânt în română pe factură, pe care
 * contabilul îl întreabă, decât o traducere inventată care spune altceva decât s-a lucrat.
 */
object Dictionary {

	val roToIt: Map<String, String> = mapOf(
		"Demolare" to "demolizione",
		"Trasee instalații" to "tracce impianti",
		"Impermeabilizare" to "impermeabilizzazione",
		"Gresie / faianță" to "posa gres e rivestimento",
		"Sanitare" to "posa sanitari",
		"Silicon / finisaje" to "silicone e finiture",
		"Curățenie" to "pulizia finale",
		"Pregătire pereți" to "preparazione pareti",
		"Colțare / plase" to "paraspigoli e rete",
		"Tencuială" to "intonaco",
		"Glet" to "rasatura",
		"Șlefuit" to "carteggiatura",
		"Pregătire suport" to "preparazione sottofondo",
		"Trasare" to "tracciamento",
		"Montaj" to "posa",
		"Chituit" to "stuccatura",
		"Plinte" to "battiscopa",
		"Structură" to "struttura metallica",
		"Placare" to "lastratura",
		"Bandă / masă" to "nastro e rasatura",
		"Protejare" to "protezione superfici",
		"Amorsă" to "primer",
		"Strat 1" to "prima mano",
		"Strat 2" to "seconda mano",
		"Retușuri" to "ritocchi",
		"Curățare suport" to "pulizia sottofondo",
		"Nivele" to "quote e livelli",
		"Turnare" to "getto massetto",
		"Uscare / verificare" to "asciugatura e verifica",
		"Schelă" to "ponteggio",
		"Lipit plăci" to "incollaggio pannelli",
		"Dibluit" to "tassellatura",
		"Plasă / masă" to "rete e rasante",
		"Decorativ" to "finitura decorativa",
		"Fundație" to "fondazione",
		"Zidărie" to "muratura",
		"Rostuit / finisaj" to "stilatura giunti",
		"Izolație" to "isolamento",
		"Barieră vapori" to "barriera vapore",
		"Finisaje" to "finiture",
		"De văzut" to "sopralluogo",
		"De reparat" to "riparazione",
		"Verificare finală" to "verifica finale",
	)

	/** Același dicționar cu cheile mici, ca să prindă și etapele scrise cu altă majusculă. */
	private val lowercaseKeys: Map<String, String> =
		roToIt.entries.associate { it.key.lowercase() to it.value }

	/** Traduce un nume de etapă. Ce nu e în dicționar iese exact cum a intrat. */
	fun translate(text: String): String {
		val key = text.trim()
		if (key.isEmpty()) return key
		val exact = roToIt[key]
		if (exact != null) return exact
		return lowercaseKeys[key.lowercase()] ?: key
	}
}
