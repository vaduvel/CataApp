package com.emanus.lucrari

import com.emanus.lucrari.data.backup.BackupCodec
import com.emanus.lucrari.data.backup.BackupPayload
import com.emanus.lucrari.data.backup.ClientRecord
import com.emanus.lucrari.data.backup.IncompatibleBackupException
import com.emanus.lucrari.data.backup.PhotoRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupCodecTest {
	@Test
	fun json_round_trip_pastreaza_datele_si_fotografiile() {
		val original = BackupPayload(
			exportedAt = "2026-08-18T18:00:00Z",
			clients = listOf(ClientRecord("c1", "Mario", null, "Cheia la vecin", 10L)),
			photos = listOf(PhotoRecord("p1", "j1", null, "t1", "p1.jpg", "AFTER", 20L)),
		)
		val decoded = BackupCodec.decode(BackupCodec.encode(original))
		assertEquals(original, decoded)
	}

	@Test(expected = IncompatibleBackupException::class)
	fun versiunea_necunoscuta_e_refuzata_inainte_de_import() {
		BackupCodec.decode("""{"schemaVersion":99,"exportedAt":"2026-08-18T18:00:00Z"}""")
	}
}
