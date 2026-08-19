package com.emanus.lucrari.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formatarea datelor și a orelor pentru ecran. Formatul scurt cu bară (10/08) e același
 * cu cel din textul de factură (SPEC §5.5), ca să nu vadă două scrieri diferite.
 */
object Dates {

	private val romanian: Locale = Locale.forLanguageTag("ro")
	private val dayMonthFormat = DateTimeFormatter.ofPattern("dd/MM", Locale.ITALY)
	private val fullFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ITALY)
	private val longDayFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", romanian)
	private val monthYearFormat = DateTimeFormatter.ofPattern("MMMM yyyy", romanian)

	fun dayMonth(date: LocalDate): String = date.format(dayMonthFormat)

	fun full(date: LocalDate): String = date.format(fullFormat)

	/** Titlul ecranului Azi: Marti, 18 august 2026, cu majusculă la început. */
	fun longDay(date: LocalDate): String =
		date.format(longDayFormat).replaceFirstChar { it.uppercase() }

	/** Capul calendarului lunar: August 2026. */
	fun monthYear(date: LocalDate): String =
		date.format(monthYearFormat).replaceFirstChar { it.uppercase() }

	/** Orele sunt opționale și se scriu scurt: 7, 7,5. */
	fun hours(value: Double): String {
		val rounded = Math.round(value * 10.0) / 10.0
		return if (rounded == Math.floor(rounded)) {
			rounded.toInt().toString()
		} else {
			String.format(Locale.ITALY, "%.1f", rounded)
		}
	}
}
