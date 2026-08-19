package com.emanus.lucrari.domain

import java.time.LocalDate
import java.time.YearMonth

/**
 * Harta unei luni pentru calendarul de lucru: zilele aliniate pe săptămâni care încep
 * <b>luni</b>, cu `null` pe casetele goale de la cap și de la coadă. Stă aici, în `domain`,
 * ca să fie testabilă fără Android: e locul clasic în care apar greșeli de o zi.
 */
object MonthGrid {

	private const val WEEK = 7

	fun cells(month: YearMonth): List<LocalDate?> {
		// DayOfWeek.value: luni = 1, deci ziua 1 a lunii lasă atâtea casete goale înaintea ei.
		val lead = month.atDay(1).dayOfWeek.value - 1
		val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
		val tail = (WEEK - (lead + days.size) % WEEK) % WEEK
		return List<LocalDate?>(lead) { null } + days + List<LocalDate?>(tail) { null }
	}
}
