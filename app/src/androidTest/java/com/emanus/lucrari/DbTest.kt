package com.emanus.lucrari

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.emanus.lucrari.data.AppDb
import com.emanus.lucrari.data.Client
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.domain.Seed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
}
