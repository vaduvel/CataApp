package com.emanus.lucrari.domain

import com.emanus.lucrari.data.JobStatus

/**
 * Regulile de status din SPEC §5.6, scrise ca funcții pure: pot fi testate fără bază
 * de date și fără telefon. Regulile propun, nu impun; schimbarea o apasă omul.
 */
object Rules {

	/** Lucrările la care mai are rost să te uiți. Restul sunt închise. */
	private val LIVE = setOf(
		JobStatus.OFERTAT,
		JobStatus.PROGRAMAT,
		JobStatus.IN_LUCRU,
		JobStatus.DE_FINISAT,
	)

	/**
	 * O zi cu blocaj trage lucrarea în Așteptare. Întoarce null când nu e nimic de
	 * schimbat: lucrarea e deja în așteptare, terminată sau anulată.
	 */
	fun statusAfterBlockedDay(current: JobStatus): JobStatus? =
		if (current in LIVE) JobStatus.ASTEPTARE else null

	/** Terminat cu resturi nebifate: se cere confirmare, nu se refuză. */
	fun needsConfirmForDone(openTodos: Int): Boolean = openTodos > 0

	/**
	 * Toate etapele bifate, dar au rămas resturi: statusul corect e De finisat,
	 * nu Terminat.
	 */
	fun suggestsDeFinisat(
		current: JobStatus,
		stageCount: Int,
		openStages: Int,
		openTodos: Int,
	): Boolean = current == JobStatus.IN_LUCRU &&
		stageCount > 0 &&
		openStages == 0 &&
		openTodos > 0
}
