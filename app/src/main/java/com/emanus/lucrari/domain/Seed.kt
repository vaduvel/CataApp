package com.emanus.lucrari.domain

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

	suspend fun ensure(db: AppDb) {
		if (db.clients().count() > 0) return

		val client = Client(
			name = "Mario",
			phone = "+39 333 000 0000",
			note = "Cheia la vecin, scara B",
		)

		val job = Job(
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
	}
}
