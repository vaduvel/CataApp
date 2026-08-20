package com.emanus.lucrari.ui.component

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.emanus.lucrari.R

@StringRes
fun templateLabelRes(value: String): Int? = when (value) {
	"Baie completă" -> R.string.template_baie_completa
	"Tencuială" -> R.string.template_tencuiala
	"Gresie / pavaj" -> R.string.template_gresie_pavaj
	"Rigips" -> R.string.template_rigips
	"Zugrăveală" -> R.string.template_zugraveala
	"Șapă" -> R.string.template_sapa
	"Termosistem" -> R.string.template_termosistem
	"Gard / zid" -> R.string.template_gard_zid
	"Mansardă" -> R.string.template_mansarda
	"Reparații diverse" -> R.string.template_reparatii_diverse
	else -> null
}

@StringRes
fun stageLabelRes(value: String): Int? = when (value) {
	"Demolare" -> R.string.stage_demolare
	"Trasee instalații" -> R.string.stage_trasee_instalatii
	"Impermeabilizare" -> R.string.stage_impermeabilizare
	"Gresie / faianță" -> R.string.stage_gresie_faianta
	"Sanitare" -> R.string.stage_sanitare
	"Silicon / finisaje" -> R.string.stage_silicon_finisaje
	"Curățenie" -> R.string.stage_curatenie
	"Pregătire pereți" -> R.string.stage_pregatire_pereti
	"Colțare / plase" -> R.string.stage_coltare_plase
	"Tencuială" -> R.string.stage_tencuiala
	"Glet" -> R.string.stage_glet
	"Șlefuit" -> R.string.stage_slefuit
	"Pregătire suport" -> R.string.stage_pregatire_suport
	"Trasare" -> R.string.stage_trasare
	"Montaj" -> R.string.stage_montaj
	"Chituit" -> R.string.stage_chituit
	"Plinte" -> R.string.stage_plinte
	"Structură" -> R.string.stage_structura
	"Placare" -> R.string.stage_placare
	"Bandă / masă" -> R.string.stage_banda_masa
	"Protejare" -> R.string.stage_protejare
	"Amorsă" -> R.string.stage_amorsa
	"Strat 1" -> R.string.stage_strat_1
	"Strat 2" -> R.string.stage_strat_2
	"Retușuri" -> R.string.stage_retusuri
	"Curățare suport" -> R.string.stage_curatare_suport
	"Nivele" -> R.string.stage_nivele
	"Turnare" -> R.string.stage_turnare
	"Uscare / verificare" -> R.string.stage_uscare_verificare
	"Schelă" -> R.string.stage_schela
	"Lipit plăci" -> R.string.stage_lipit_placi
	"Dibluit" -> R.string.stage_dibluit
	"Plasă / masă" -> R.string.stage_plasa_masa
	"Decorativ" -> R.string.stage_decorativ
	"Fundație" -> R.string.stage_fundatie
	"Zidărie" -> R.string.stage_zidarie
	"Rostuit / finisaj" -> R.string.stage_rostuit_finisaj
	"Izolație" -> R.string.stage_izolatie
	"Barieră vapori" -> R.string.stage_bariera_vapori
	"Finisaje" -> R.string.stage_finisaje
	"De văzut" -> R.string.stage_de_vazut
	"De reparat" -> R.string.stage_de_reparat
	"Verificare finală" -> R.string.stage_verificare_finala
	else -> null
}

@Composable
fun localizedTemplateLabel(value: String): String =
	templateLabelRes(value)?.let { stringResource(it) } ?: value

@Composable
fun localizedStageLabel(value: String): String =
	stageLabelRes(value)?.let { stringResource(it) } ?: value
