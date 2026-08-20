package com.emanus.lucrari

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.emanus.lucrari.data.AppDb
import com.emanus.lucrari.data.Client
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.data.Reminder
import com.emanus.lucrari.domain.Seed
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Criteriul de acceptare al lui M1 (SPEC §11). Se rulează cu telefon sau emulator conectat:
 * ./gradlew :app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class DbTest {

	private lateinit var db: AppDb

	@Before
	fun setUp() {
		val context = ApplicationProvider.getApplicationContext<Context>()
		db = Room.inMemoryDatabaseBuilder(context, AppDb::class.java).build()
	}

	@After
	fun tearDown() {
		db.close()
	}

	@Test
	fun seed_scrie_lucrarea_demo_cu_cifrele_din_spec() = runBlocking {
		Seed.ensure(db)

		val board = db.jobs().observeBoard().first()
		assertEquals(1, board.size)

		val row = board[0]
		assertEquals("Rifacimento bagno", row.job.title)
		assertEquals("Mario", row.clientName)
		assertEquals(240_000L, row.job.agreedPriceCents)
		assertEquals(2, row.workedDays)
		assertEquals(7, row.stageCount)
		assertEquals(2, row.stagesDone)
		assertEquals(1, row.openTodos)
		assertEquals(80_000L, row.invoicedCents)
		assertEquals(80_000L, row.collectedCents)
	}

	@Test
	fun seed_nu_se_dubleaza_la_a_doua_pornire() = runBlocking {
		Seed.ensure(db)
		Seed.ensure(db)

		assertEquals(1, db.clients().count())
		assertEquals(1, db.jobs().observeAll().first().size)
	}

	@Test
	fun stergerea_demo_ului_nu_atinge_datele_utilizatorului() = runBlocking {
		Seed.ensure(db)
		val userClient = Client(name = "Luigi")
		db.clients().upsert(userClient)
		db.jobs().upsert(Job(clientId = userClient.id, title = "Bucatarie"))
		db.reminders().upsert(
			Reminder(jobId = Seed.DEMO_JOB_ID, text = "Demo", dueAt = 1L),
		)

		assertTrue(Seed.delete(db))
		assertFalse(Seed.delete(db))

		// Upgrade peste versiunile în care seed-ul folosea UUID-uri aleatoare.
		val legacyClient = Client(
			name = "Mario",
			phone = "+39 333 000 0000",
			note = "Cheia la vecin, scara B",
		)
		db.clients().upsert(legacyClient)
		db.jobs().upsert(
			Job(
				clientId = legacyClient.id,
				title = "Rifacimento bagno",
				street = "Via 23",
				city = "Milano",
				type = "Baie completă",
				agreedPriceCents = 240_000L,
			),
		)
		assertTrue(Seed.delete(db))

		assertEquals(listOf("Luigi"), db.clients().allOnce().map { it.name })
		assertEquals(listOf("Bucatarie"), db.jobs().observeAll().first().map { it.title })
		assertTrue(db.reminders().observeByJob(Seed.DEMO_JOB_ID).first().isEmpty())
	}

	@Test
	fun cautarea_gaseste_lucrarea_dupa_strada_si_dupa_client() = runBlocking {
		Seed.ensure(db)

		assertEquals(1, db.jobs().searchByStreetOrClient("via 23").first().size)
		assertEquals(1, db.jobs().searchByStreetOrClient("mario").first().size)
		assertEquals(1, db.jobs().searchByStreetOrClient("Milano").first().size)
		assertTrue(db.jobs().searchByStreetOrClient("Via 99").first().isEmpty())
	}

	@Test
	fun stergerea_clientului_sterge_lucrarile_si_etapele() = runBlocking {
		val client = Client(name = "Test")
		db.clients().upsert(client)
		val job = Job(clientId = client.id, title = "Zid grădină", street = "Via Roma")
		db.jobs().upsert(job)

		assertEquals(1, db.jobs().observeAll().first().size)

		db.clients().delete(client)

		assertTrue(db.jobs().observeAll().first().isEmpty())
		assertTrue(db.stages().observeByJob(job.id).first().isEmpty())
	}

	/**
	 * M8: lucrarea programată ajunge pe ecranul Azi în ziua în care ar trebui să înceapă.
	 * Nu mai devreme, ca să nu încarce ziua de azi cu ce e săptămâna viitoare, dar nici nu
	 * dispare dacă ziua a trecut și tot n-a început-o.
	 */
	@Test
	fun ecranul_azi_ia_lucrarea_programata_din_ziua_de_inceput() = runBlocking {
		val client = Client(name = "Test")
		db.clients().upsert(client)
		val azi = LocalDate.of(2026, 8, 21)

		db.jobs().upsert(
			Job(
				clientId = client.id,
				title = "Incepe azi",
				status = JobStatus.PROGRAMAT,
				plannedStart = azi,
			),
		)
		db.jobs().upsert(
			Job(
				clientId = client.id,
				title = "A ramas in urma",
				status = JobStatus.PROGRAMAT,
				plannedStart = azi.minusDays(3),
			),
		)
		db.jobs().upsert(
			Job(
				clientId = client.id,
				title = "Abia peste doua zile",
				status = JobStatus.PROGRAMAT,
				plannedStart = azi.plusDays(2),
			),
		)
		db.jobs().upsert(
			Job(clientId = client.id, title = "Fara data", status = JobStatus.PROGRAMAT),
		)
		db.jobs().upsert(
			Job(clientId = client.id, title = "In lucru", status = JobStatus.IN_LUCRU),
		)

		val titluri = db.jobs()
			.observeToday(azi, listOf(JobStatus.IN_LUCRU), JobStatus.PROGRAMAT)
			.first()
			.map { row -> row.job.title }

		assertEquals(3, titluri.size)
		assertTrue(titluri.contains("Incepe azi"))
		assertTrue(titluri.contains("A ramas in urma"))
		assertTrue(titluri.contains("In lucru"))
		assertFalse(titluri.contains("Abia peste doua zile"))
		assertFalse(titluri.contains("Fara data"))
	}
}
