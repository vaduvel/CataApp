package com.emanus.lucrari

import android.content.Context
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.emanus.lucrari.data.AppDb
import com.emanus.lucrari.data.Client
import com.emanus.lucrari.data.Extra
import com.emanus.lucrari.data.InvoiceRef
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.data.Material
import com.emanus.lucrari.data.Measure
import com.emanus.lucrari.data.MeasureUnit
import com.emanus.lucrari.data.Payment
import com.emanus.lucrari.data.Phase
import com.emanus.lucrari.data.Photo
import com.emanus.lucrari.data.Reminder
import com.emanus.lucrari.data.Stage
import com.emanus.lucrari.data.Todo
import com.emanus.lucrari.data.WorkDay
import com.emanus.lucrari.data.repo.BackupRepo
import com.emanus.lucrari.data.repo.ImportMode
import java.io.File
import java.time.LocalDate
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRestoreTest {
	private lateinit var context: Context
	private lateinit var db: AppDb
	private lateinit var repo: BackupRepo
	private lateinit var workRoot: File

	@Before
	fun setUp() {
		context = ApplicationProvider.getApplicationContext()
		db = Room.inMemoryDatabaseBuilder(context, AppDb::class.java).build()
		repo = BackupRepo(context, db)
		workRoot = File(context.filesDir, "backup-test-${UUID.randomUUID()}").apply { mkdirs() }
		File(context.filesDir, "photos").deleteRecursively()
	}

	@After
	fun tearDown() {
		db.close()
		workRoot.deleteRecursively()
		File(context.filesDir, "photos").deleteRecursively()
	}

	@Test
	fun export_stergere_import_reface_toate_entitatile_si_poza() = runBlocking {
		insertCompleteFixture()
		val archive = File(context.filesDir, "backup/test-${UUID.randomUUID()}.zip").apply {
			parentFile?.mkdirs()
			createNewFile()
		}
		val uri = FileProvider.getUriForFile(context, context.packageName + ".files", archive)
		repo.exportTo(uri)

		db.backup().clearReminders()
		db.backup().clearClients()
		File(context.filesDir, "photos").deleteRecursively()
		assertTrue(db.backup().clients().isEmpty())

		val result = repo.importFrom(uri, ImportMode.REPLACE)
		assertEquals(12, result.recordsRead)
		assertEquals(1, db.backup().clients().size)
		assertEquals(1, db.backup().jobs().size)
		assertEquals(1, db.backup().stages().size)
		assertEquals(1, db.backup().workDays().size)
		assertEquals(1, db.backup().todos().size)
		assertEquals(1, db.backup().materials().size)
		assertEquals(1, db.backup().measures().size)
		assertEquals(1, db.backup().extras().size)
		assertEquals(1, db.backup().payments().size)
		assertEquals(1, db.backup().invoices().size)
		assertEquals(1, db.backup().photos().size)
		assertEquals(1, db.backup().reminders().size)
		assertTrue(File(db.backup().photos().single().path).isFile)
	}

	@Test
	fun adauga_ce_lipseste_e_idempotent_dupa_uuid() = runBlocking {
		insertCompleteFixture()
		val archive = File(context.filesDir, "backup/merge-${UUID.randomUUID()}.zip").apply {
			parentFile?.mkdirs()
			createNewFile()
		}
		val uri = FileProvider.getUriForFile(context, context.packageName + ".files", archive)
		repo.exportTo(uri)
		repo.importFrom(uri, ImportMode.MERGE)
		repo.importFrom(uri, ImportMode.MERGE)

		assertEquals(1, db.backup().clients().size)
		assertEquals(1, db.backup().jobs().size)
		assertEquals(1, db.backup().photos().size)
		assertEquals(1, db.backup().reminders().size)
	}

	@Test
	fun schema_incompatibila_nu_sterge_datele_existente() = runBlocking {
		val client = Client(id = "safe-client", name = "Rămâne")
		db.clients().upsert(client)
		val archive = File(workRoot, "bad.zip")
		ZipOutputStream(archive.outputStream()).use { zip ->
			zip.putNextEntry(ZipEntry("data.json"))
			zip.write("""{"schemaVersion":99,"exportedAt":"2026-08-18T18:00:00Z"}""".toByteArray())
			zip.closeEntry()
		}
		val uri = FileProvider.getUriForFile(
			context,
			context.packageName + ".files",
			File(context.filesDir, "backup/bad-${UUID.randomUUID()}.zip").also {
				it.parentFile?.mkdirs()
				archive.copyTo(it, overwrite = true)
			},
		)
		var rejected = false
		try {
			repo.importFrom(uri, ImportMode.REPLACE)
		} catch (_: IllegalArgumentException) {
			rejected = true
		}
		assertTrue(rejected)
		assertEquals("Rămâne", db.backup().clients().single().name)
	}

	private suspend fun insertCompleteFixture() {
		val date = LocalDate.of(2026, 8, 18)
		val client = Client(id = "c1", name = "Mario")
		val job = Job(
			id = "j1",
			clientId = client.id,
			title = "Baie",
			status = JobStatus.IN_LUCRU,
			agreedPriceCents = 100_000L,
		)
		db.clients().upsert(client)
		db.jobs().upsert(job)
		db.stages().upsert(Stage(id = "s1", jobId = job.id, name = "Demolare", sort = 0))
		db.workDays().upsert(WorkDay(id = "d1", jobId = job.id, date = date))
		db.todos().upsert(Todo(id = "t1", jobId = job.id, what = "Silicon", due = date))
		db.materials().upsert(Material(id = "m1", jobId = job.id, what = "Silicon"))
		db.measures().upsert(
			Measure(
				id = "q1",
				jobId = job.id,
				place = "Baie",
				qty = 2.0,
				unit = MeasureUnit.M2,
				date = date,
			),
		)
		db.extras().upsert(Extra(id = "e1", jobId = job.id, what = "Nișă", date = date))
		db.payments().upsert(Payment(id = "pmt1", jobId = job.id, date = date, amountCents = 10_000L))
		db.invoices().upsert(InvoiceRef(id = "i1", jobId = job.id, amountCents = 10_000L))
		val photoFile = File(context.filesDir, "photos/photo1.jpg").apply {
			parentFile?.mkdirs()
			writeBytes(byteArrayOf(1, 2, 3, 4))
		}
		db.photos().upsert(
			Photo(
				id = "photo1",
				jobId = job.id,
				todoId = "t1",
				path = photoFile.absolutePath,
				phase = Phase.AFTER,
			),
		)
		db.reminders().upsert(
			Reminder(id = "r1", jobId = job.id, text = "Rest de făcut azi: Baie", dueAt = 1L),
		)
	}
}
