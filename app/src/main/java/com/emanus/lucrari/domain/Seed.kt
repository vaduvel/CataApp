package com.emanus.lucrari.domain

import androidx.room.withTransaction
import com.emanus.lucrari.data.AppDb
import com.emanus.lucrari.data.Billing
import com.emanus.lucrari.data.Client
import com.emanus.lucrari.data.Extra
import com.emanus.lucrari.data.InvoiceKind
import com.emanus.lucrari.data.InvoiceRef
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.data.Measure
import com.emanus.lucrari.data.MeasureUnit
import com.emanus.lucrari.data.Method
import com.emanus.lucrari.data.Payment
import com.emanus.lucrari.data.Reason
import com.emanus.lucrari.data.Stage
import com.emanus.lucrari.data.Todo
import com.emanus.lucrari.data.WorkDay
import com.emanus.lucrari.data.now
import java.time.LocalDate

/**
 * Datele demo din scenariul de referință (SPEC §10). Cifrele sunt exact cele pe care
 * le așteaptă testul golden din M6, deci nu se schimbă fără să se schimbe și specul:
 * 2.400 € la corp, extra 180 €, acont facturat și încasat 800 €, două zile lucrate.
 */
object Seed {
	const val DEMO_CLIENT_ID = "seed-client-mario-v1"
	const val DEMO_JOB_ID = "seed-job-rifacimento-bagno-v1"

	/**
	 * Datele demo intră o singură dată, la prima pornire după instalare. Regula stă
	 * separat de inserare ca să fie testabilă fără Android, iar `alreadySeeded` se
	 * ține în afara bazei de date: altfel un import „Înlocuiește tot” cu arhivă goală
	 * sau ștergerea manuală a demo-ului l-ar readuce pe Mario la următoarea pornire.
	 */
	fun shouldSeed(alreadySeeded: Boolean, clientCount: Int): Boolean =
		!alreadySeeded && clientCount == 0

	/** @return `true` dacă datele demo au fost inserate chiar acum. */
	suspend fun ensure(db: AppDb, alreadySeeded: Boolean = false): Boolean {
		if (!shouldSeed(alreadySeeded, db.clients().count())) return false

		val client = Client(
			id = DEMO_CLIENT_ID,
			name = "Mario",
			phone = "+39 333 000 0000",
			note = "Cheia la vecin, scara B",
		)

		val job = Job(
			id = DEMO_JOB_ID,
			clientId = client.id,
			title = "Rifacimento bagno",
			street = "Via 23",
			city = "Milano",
			type = "Baie completă",
			status = JobStatus.IN_LUCRU,
			plannedStart = LocalDate.of(2026, 8, 10),
			estDays = 3,
			billing = Billing.CORP,
			agreedPriceCents = 240_000L,
		)

		// Primele două etape bifate: demolizione și tracce impianti în textul pentru contabil.
		val stages = Templates.stagesFor(job.type).mapIndexed { index, name ->
			Stage(
				jobId = job.id,
				name = name,
				sort = index,
				done = index < 2,
				doneAt = if (index < 2) now() else null,
			)
		}

		val days = listOf(
			WorkDay(jobId = job.id, date = LocalDate.of(2026, 8, 10), what = "Demolare"),
			WorkDay(jobId = job.id, date = LocalDate.of(2026, 8, 12), what = "Trasee instalații"),
		)

		val measures = listOf(
			Measure(
				jobId = job.id,
				place = "Bagno — pavimento",
				qty = 12.4,
				unit = MeasureUnit.M2,
				date = LocalDate.of(2026, 8, 12),
			),
		)

		val extras = listOf(
			Extra(
				jobId = job.id,
				what = "nicchia doccia + spostamento presa",
				date = LocalDate.of(2026, 8, 11),
				priceCents = 18_000L,
				accepted = true,
				proof = "vocală WhatsApp 11/08",
			),
		)

		val payments = listOf(
			Payment(
				jobId = job.id,
				date = LocalDate.of(2026, 8, 10),
				amountCents = 80_000L,
				method = Method.BONIFICO,
			),
		)

		val invoices = listOf(
			InvoiceRef(
				jobId = job.id,
				number = "1/2026",
				date = LocalDate.of(2026, 8, 10),
				amountCents = 80_000L,
				kind = InvoiceKind.ACONTO,
				paid = true,
			),
		)

		val todos = listOf(
			Todo(
				jobId = job.id,
				place = "Bagno",
				what = "Montat silicon la cadă",
				reason = Reason.MATERIAL,
			),
		)

		// Ordinea contează: clientul înaintea lucrării, lucrarea înaintea copiilor (chei străine).
		db.clients().upsert(client)
		db.jobs().upsert(job)
		db.stages().upsertAll(stages)
		db.workDays().upsertAll(days)
		db.measures().upsertAll(measures)
		db.extras().upsertAll(extras)
		db.payments().upsertAll(payments)
		db.invoices().upsertAll(invoices)
		db.todos().upsertAll(todos)

		return true
	}

	/**
	 * Șterge numai exemplul livrat cu aplicația. Identificatorii stabili acoperă
	 * instalările noi; amprenta strictă acoperă actualizarea peste versiunile în care
	 * seed-ul primea UUID-uri aleatoare. Orice altă lucrare a clientului rămâne intactă.
	 */
	suspend fun delete(db: AppDb): Boolean = db.withTransaction {
		val demo = findDemo(db) ?: return@withTransaction false
		val (client, job) = demo

		if (job != null) {
			db.reminders().deleteByJob(job.id)
			db.jobs().delete(job)
		}
		if (db.jobs().byClientOnce(client.id).isEmpty()) {
			db.clients().delete(client)
		}
		true
	}

	private suspend fun findDemo(db: AppDb): Pair<Client, Job?>? {
		val stableClient = db.clients().byId(DEMO_CLIENT_ID)
		if (stableClient != null) {
			val stableJob = db.jobs().byId(DEMO_JOB_ID)
			if (stableJob != null || db.jobs().byClientOnce(stableClient.id).isEmpty()) {
				return stableClient to stableJob
			}
		}

		for (client in db.clients().allOnce().filter(::isLegacyDemoClient)) {
			val jobs = db.jobs().byClientOnce(client.id)
			val demoJob = jobs.singleOrNull(::isLegacyDemoJob)
			if (demoJob != null || jobs.isEmpty()) return client to demoJob
		}
		return null
	}

	private fun isLegacyDemoClient(client: Client): Boolean =
		client.name == "Mario" &&
			client.phone == "+39 333 000 0000" &&
			client.note == "Cheia la vecin, scara B"

	private fun isLegacyDemoJob(job: Job): Boolean =
		job.title == "Rifacimento bagno" &&
			job.street == "Via 23" &&
			job.city == "Milano" &&
			job.type == "Baie completă" &&
			job.agreedPriceCents == 240_000L
}
