package com.emanus.lucrari

import com.emanus.lucrari.work.WorkScheduler
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkSchedulerTest {
	private val zone = ZoneId.of("Europe/Bucharest")

	@Test
	fun memento_ul_asteapta_pana_la_ora_19() {
		val now = ZonedDateTime.of(2026, 8, 18, 18, 30, 0, 0, zone)
		assertEquals(Duration.ofMinutes(30).toMillis(), WorkScheduler.initialDelayMillis(19, now))
	}

	@Test
	fun dupa_ora_tinta_programeaza_ziua_urmatoare() {
		val now = ZonedDateTime.of(2026, 8, 18, 19, 0, 0, 0, zone)
		assertEquals(Duration.ofHours(24).toMillis(), WorkScheduler.initialDelayMillis(19, now))
	}
}
