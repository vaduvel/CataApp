package com.emanus.lucrari.domain

import com.emanus.lucrari.data.JobStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Ce fel de memento de început se dă pentru o lucrare programată. */
enum class StartReminder { IN_DAYS, TOMORROW, TODAY }

/**
 * Programarea unei lucrări: din ce zi începe și cât ține.
 *
 * Intervalul se calculează din data de început și zilele estimate, deci nu are nevoie
 * de nicio coloană nouă: `plannedStart` și `estDays` există în schema v1 din M1. Regulile
 * stau aici, fără Android și fără bază de date, ca să poată fi testate direct.
 */
object Schedule {

	/** Cu câte zile înainte de început se dă primul memento. */
	const val DAYS_BEFORE: Long = 3

	/**
	 * Ultima zi din interval. O lucrare de o zi începe și se termină în aceeași zi, iar
	 * fără zile estimate se presupune tot o zi: mai bine o zi corectă decât un interval
	 * inventat.
	 */
	fun endDate(start: LocalDate, estDays: Int?): LocalDate =
		start.plusDays(((estDays ?: 1).coerceAtLeast(1) - 1).toLong())

	/** Ziua `date` cade în intervalul programat al lucrării. Fără dată, nicăieri. */
	fun covers(start: LocalDate?, estDays: Int?, date: LocalDate): Boolean {
		if (start == null) return false
		return !date.isBefore(start) && !date.isAfter(endDate(start, estDays))
	}

	/**
	 * Statusul unei lucrări nou create, după data aleasă: orice dată o ține în Programat,
	 * inclusiv dacă începutul a trecut, ca să rămână vizibilă drept restantă pe Azi. Fără
	 * dată rămâne cum era (Ofertat). Prima „Am lucrat azi aici” o mută în lucru.
	 */
	fun statusForNewJob(start: LocalDate?, fallback: JobStatus): JobStatus =
		if (start == null) fallback else JobStatus.PROGRAMAT

	/** Câte zile mai sunt până la început. Negativ dacă data a trecut. */
	fun daysUntilStart(start: LocalDate, today: LocalDate): Long =
		ChronoUnit.DAYS.between(today, start)

	/**
	 * Mementoul de început. Se dă doar pentru lucrările programate și doar în trei zile
	 * exacte — cu trei zile înainte, în ajun și în ziua respectivă — ca să nu se repete
	 * în fiecare zi din așteptare. O lucrare deja în lucru nu mai are ce să anunțe.
	 */
	fun startReminder(status: JobStatus, start: LocalDate?, today: LocalDate): StartReminder? {
		if (status != JobStatus.PROGRAMAT || start == null) return null
		return when (daysUntilStart(start, today)) {
			0L -> StartReminder.TODAY
			1L -> StartReminder.TOMORROW
			DAYS_BEFORE -> StartReminder.IN_DAYS
			else -> null
		}
	}
}
