package com.emanus.lucrari.data

/** Statusul lucrării. Nu se amestecă niciodată cu starea banilor (SPEC §5.1). */
enum class JobStatus { OFERTAT, PROGRAMAT, IN_LUCRU, ASTEPTARE, DE_FINISAT, TERMINAT, ANULAT }

/** Cum se calculează baza de plată: la corp, la măsură, sau pe zi. */
enum class Billing { CORP, MASURA, ZILE }

/** De ce a rămas ceva nefăcut. */
enum class Reason { MATERIAL, DECIZIE_CLIENT, ALT_MESERIAS, VREMEA, LIPSA_TIMP, ALTUL }

/**
 * SPEC §4 numește acest enum `Unit`. L-am redenumit `MeasureUnit` intenționat:
 * un tip propriu numit `Unit` umbrește `kotlin.Unit` în orice fișier care îl importă,
 * iar atunci orice lambda scrisă ca `() -> Unit` nu mai compilează.
 * `label` e forma scrisă în textul italian pentru contabil (SPEC §5.5).
 */
enum class MeasureUnit(val label: String) {
	M2("m²"),
	ML("ml"),
	BUC("buc"),
	ORE("ore"),
	ZILE("zile"),
}

/** Cum a intrat banul. */
enum class Method { CASH, BONIFICO, ALTUL }

/** Tipul facturii, doar ca evidență. Aplicația nu emite facturi. */
enum class InvoiceKind { ACONTO, SALDO, UNICA }

/** Când a fost făcută poza. */
enum class Phase { BEFORE, DURING, AFTER }
