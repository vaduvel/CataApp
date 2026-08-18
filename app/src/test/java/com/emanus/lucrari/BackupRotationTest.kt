package com.emanus.lucrari

import com.emanus.lucrari.domain.BackupRotation
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupRotationTest {
	@Test
	fun pastreaza_ultimele_sapte_arhive() {
		val names = (1..8).map { day -> "lucrari-2026-08-${day.toString().padStart(2, '0')}.zip" } +
			listOf("note.txt", "lucrari-invalid.zip")
		assertEquals(listOf("lucrari-2026-08-01.zip"), BackupRotation.filesToDelete(names))
	}

	@Test
	fun aceeasi_zi_nu_schimba_rotatia() {
		val names = listOf("lucrari-2026-08-18.zip")
		assertEquals(emptyList<String>(), BackupRotation.filesToDelete(names))
	}
}
