package com.emanus.lucrari.domain

/**
 * Șabloanele de etape din SPEC §14, copiate exact. Cheia e și numele implicit al lucrării
 * și cheia din dicționarul RO -> IT folosit la textul de factură (M6), deci nu se rescriu
 * fără să se actualizeze și dicționarul.
 */
object Templates {
	private const val TYPE_SEPARATOR = " + "

	val all: Map<String, List<String>> = linkedMapOf(
		"Baie completă" to listOf(
			"Demolare",
			"Trasee instalații",
			"Impermeabilizare",
			"Gresie / faianță",
			"Sanitare",
			"Silicon / finisaje",
			"Curățenie",
		),
		"Tencuială" to listOf(
			"Pregătire pereți",
			"Colțare / plase",
			"Tencuială",
			"Glet",
			"Șlefuit",
		),
		"Gresie / pavaj" to listOf(
			"Pregătire suport",
			"Trasare",
			"Montaj",
			"Chituit",
			"Plinte",
		),
		"Rigips" to listOf(
			"Structură",
			"Placare",
			"Bandă / masă",
			"Șlefuit",
		),
		"Zugrăveală" to listOf(
			"Protejare",
			"Amorsă",
			"Strat 1",
			"Strat 2",
			"Retușuri",
		),
		"Șapă" to listOf(
			"Curățare suport",
			"Nivele",
			"Turnare",
			"Uscare / verificare",
		),
		"Termosistem" to listOf(
			"Schelă",
			"Lipit plăci",
			"Dibluit",
			"Plasă / masă",
			"Amorsă",
			"Decorativ",
		),
		"Gard / zid" to listOf(
			"Trasare",
			"Fundație",
			"Zidărie",
			"Rostuit / finisaj",
		),
		"Mansardă" to listOf(
			"Structură",
			"Izolație",
			"Barieră vapori",
			"Placare",
			"Finisaje",
		),
		"Reparații diverse" to listOf(
			"De văzut",
			"De reparat",
			"Verificare finală",
		),
	)

	val names: List<String> = all.keys.toList()

	/** Un singur câmp Room păstrează toate șabloanele alese, fără schimbarea schemei. */
	fun combineTypes(types: List<String>): String? = types
		.filter(all::containsKey)
		.distinct()
		.joinToString(TYPE_SEPARATOR)
		.ifBlank { null }

	fun typesFor(value: String?): List<String> = value
		?.split(TYPE_SEPARATOR)
		.orEmpty()
		.filter(all::containsKey)
		.distinct()

	fun stagesFor(type: String?): List<String> = typesFor(type)
		.flatMap { name -> all.getValue(name) }
		.distinct()
}
