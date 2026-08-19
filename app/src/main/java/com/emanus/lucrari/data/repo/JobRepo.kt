package com.emanus.lucrari.data.repo

import com.emanus.lucrari.data.AppDb
import com.emanus.lucrari.data.Billing
import com.emanus.lucrari.data.Client
import com.emanus.lucrari.data.Extra
import com.emanus.lucrari.data.InvoiceKind
import com.emanus.lucrari.data.InvoiceRef
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.data.JobToday
import com.emanus.lucrari.data.JobWithTotals
import com.emanus.lucrari.data.Material
import com.emanus.lucrari.data.Measure
import com.emanus.lucrari.data.MeasureUnit
import com.emanus.lucrari.data.Method
import com.emanus.lucrari.data.Payment
import com.emanus.lucrari.data.Reason
import com.emanus.lucrari.data.Stage
import com.emanus.lucrari.data.Todo
import com.emanus.lucrari.data.TodoWithJob
import com.emanus.lucrari.data.WorkDay
import com.emanus.lucrari.data.now
import com.emanus.lucrari.data.today
import com.emanus.lucrari.domain.JobTotals
import com.emanus.lucrari.domain.Rules
import com.emanus.lucrari.domain.Templates
import com.emanus.lucrari.domain.Totals
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** O lucrare cu banii ei, așa cum apare în lista de pe ecranul Bani. */
data class JobMoney(
	val job: Job,
	val clientName: String,
	val totals: JobTotals,
)

/**
 * Cele trei cifre mari de sus (SPEC §5.3) plus lucrările care așteaptă o factură.
 * Ofertele nu intră nicăieri: o ofertă nu e încă bani.
 */
data class MoneySummary(
	val outstandingCents: Long,
	val overdueCents: Long,
	val collectedThisMonthCents: Long,
	val toInvoice: List<JobMoney>,
)

/** Facturat și încasat, adunate separat pentru o singură lucrare. */
private data class Sums(val invoicedCents: Long, val collectedCents: Long)

/**
 * Singurul drum dintre interfață și baza de date. Composable-urile nu ating niciodată DAO-urile.
 */
class JobRepo(private val db: AppDb) {

	fun board(query: String): Flow<List<JobWithTotals>> {
		val q = query.trim()
		return if (q.isEmpty()) db.jobs().observeBoard() else db.jobs().searchByStreetOrClient(q)
	}

	/**
	 * Aceleași rânduri ca [board], dar citite o dată, la cerere. Lista de Lucrări se
	 * reîmprospătează la fiecare intrare pe ecran: pe telefon s-a văzut că un abonament
	 * lung la Room poate rămâne cu mulțimea veche, iar o lucrare abia salvată nu are voie
	 * să lipsească din listă.
	 */
	suspend fun boardOnce(query: String): List<JobWithTotals> = db.jobs().boardOnce(query.trim())

	/**
	 * Lucrările la care se poate lucra azi, în ordinea în care le atinge: întâi cele în
	 * lucru, apoi de finisat, apoi cele blocate în așteptare.
	 */
	fun todayBoard(date: LocalDate): Flow<List<JobToday>> =
		db.jobs().observeToday(date, ACTIVE_STATUSES).map { list ->
			list.sortedBy { row -> ACTIVE_STATUSES.indexOf(row.job.status) }
		}

	fun job(id: String): Flow<Job?> = db.jobs().observe(id)

	fun allJobs(): Flow<List<Job>> = db.jobs().observeAll()

	fun stages(jobId: String): Flow<List<Stage>> = db.stages().observeByJob(jobId)

	fun days(jobId: String): Flow<List<WorkDay>> = db.workDays().observeByJobDesc(jobId)

	fun todos(jobId: String): Flow<List<Todo>> = db.todos().observeByJob(jobId)

	/** Tot ce a rămas nefăcut, din toate lucrările, cel mai apropiat termen primul (SPEC §11). */
	fun openTodos(): Flow<List<TodoWithJob>> = db.todos().observeOpenAll()

	fun materials(jobId: String): Flow<List<Material>> = db.materials().observeByJob(jobId)

	fun measures(jobId: String): Flow<List<Measure>> = db.measures().observeByJob(jobId)

	fun extras(jobId: String): Flow<List<Extra>> = db.extras().observeByJob(jobId)

	fun payments(jobId: String): Flow<List<Payment>> = db.payments().observeByJob(jobId)

	fun invoices(jobId: String): Flow<List<InvoiceRef>> = db.invoices().observeByJob(jobId)

	fun clients(): Flow<List<Client>> = db.clients().observeAll()

	fun client(id: String): Flow<Client?> = db.clients().observeById(id)

	/**
	 * Formularul de lucrare nouă are 4 câmpuri (SPEC §7). Adresa se scrie într-un singur
	 * câmp, așa cum o spune el (Via 23, Milano), și se sparge aici în stradă și oraș.
	 * Clientul se refolosește dacă există deja cu același nume, altfel se creează.
	 */
	suspend fun createJob(
		clientName: String,
		address: String,
		title: String,
		type: String?,
		estDays: Int?,
	): String {
		val name = clientName.trim()
		val client = db.clients().byName(name) ?: Client(name = name).also { db.clients().upsert(it) }

		val parts = address.split(",", limit = 2)
		val street = parts.getOrNull(0)?.trim()?.takeIf { it.isNotEmpty() }
		val city = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }

		val job = Job(
			clientId = client.id,
			title = title.trim(),
			street = street,
			city = city,
			type = type,
			estDays = estDays,
		)
		db.jobs().upsert(job)

		val stageNames = Templates.stagesFor(type)
		if (stageNames.isNotEmpty()) {
			db.stages().upsertAll(
				stageNames.mapIndexed { index, stageName ->
					Stage(jobId = job.id, name = stageName, sort = index)
				},
			)
		}
		return job.id
	}

	suspend fun setStatus(job: Job, status: JobStatus) {
		val closedAt = if (status == JobStatus.TERMINAT || status == JobStatus.ANULAT) {
			job.closedAt ?: now()
		} else {
			null
		}
		db.jobs().upsert(job.copy(status = status, closedAt = closedAt))
	}

	suspend fun deleteJob(job: Job) {
		db.jobs().delete(job)
	}

	suspend fun addClient(name: String, phone: String?, note: String?) {
		db.clients().upsert(Client(name = name.trim(), phone = phone, note = note))
	}

	/**
	 * Am lucrat azi aici, dintr-o singură apăsare (SPEC §7). A doua apăsare în aceeași zi
	 * nu dublează nimic: întoarce false, iar interfața spune doar că ziua era deja trecută.
	 * Prima zi lucrată scoate lucrarea din ofertă și o pune în lucru.
	 */
	suspend fun logDay(jobId: String, date: LocalDate = today(), what: String? = null): Boolean {
		if (db.workDays().byJobAndDate(jobId, date) != null) return false
		db.workDays().upsert(
			WorkDay(jobId = jobId, date = date, what = what?.trim()?.ifBlank { null }),
		)
		val job = db.jobs().byId(jobId)
		if (job != null && (job.status == JobStatus.OFERTAT || job.status == JobStatus.PROGRAMAT)) {
			db.jobs().upsert(job.copy(status = JobStatus.IN_LUCRU))
		}
		return true
	}

	/**
	 * SPEC §5.6: o zi în care s-a stat degeaba trage lucrarea în Așteptare. Nu atinge
	 * lucrările deja închise, ca să nu reînvie una terminată.
	 */
	suspend fun saveDay(day: WorkDay) {
		val clean = day.copy(
			what = day.what?.trim()?.ifBlank { null },
			blocked = day.blocked?.trim()?.ifBlank { null },
		)
		db.workDays().upsert(clean)
		if (clean.blocked == null) return
		val job = db.jobs().byId(clean.jobId) ?: return
		val next = Rules.statusAfterBlockedDay(job.status) ?: return
		db.jobs().upsert(job.copy(status = next))
	}

	suspend fun deleteDay(day: WorkDay) {
		db.workDays().delete(day)
	}

	suspend fun toggleStage(stage: Stage) {
		val done = !stage.done
		db.stages().upsert(stage.copy(done = done, doneAt = if (done) now() else null))
	}

	suspend fun addStage(jobId: String, name: String) {
		val clean = name.trim()
		if (clean.isEmpty()) return
		val sort = (db.stages().maxSort(jobId) ?: -1) + 1
		db.stages().upsert(Stage(jobId = jobId, name = clean, sort = sort))
	}

	suspend fun deleteStage(stage: Stage) {
		db.stages().delete(stage)
	}

	/**
	 * Alege un șablon (SPEC §14) pentru o lucrare care n-a primit unul la creare. Etapele
	 * se adaugă la coadă, nu se șterge nimic din ce a bifat deja.
	 */
	suspend fun applyTemplate(jobId: String, type: String) {
		val names = Templates.stagesFor(type)
		if (names.isEmpty()) return
		val start = (db.stages().maxSort(jobId) ?: -1) + 1
		db.stages().upsertAll(
			names.mapIndexed { index, name ->
				Stage(jobId = jobId, name = name, sort = start + index)
			},
		)
		val job = db.jobs().byId(jobId) ?: return
		if (job.type.isNullOrBlank()) {
			db.jobs().upsert(job.copy(type = type))
		}
	}

	/**
	 * Rest de făcut (SPEC §11). Locul e camera sau bucata de lucrare, ca să știe unde se
	 * întoarce: baia, scara B. Motivul și termenul sunt opționale.
	 */
	suspend fun addTodo(
		jobId: String,
		what: String,
		place: String? = null,
		reason: Reason? = null,
		due: LocalDate? = null,
	) {
		val clean = what.trim()
		if (clean.isEmpty()) return
		db.todos().upsert(
			Todo(
				jobId = jobId,
				place = place?.trim()?.ifBlank { null },
				what = clean,
				reason = reason,
				due = due,
			),
		)
	}

	suspend fun saveTodo(todo: Todo) {
		val clean = todo.what.trim()
		if (clean.isEmpty()) return
		db.todos().upsert(
			todo.copy(what = clean, place = todo.place?.trim()?.ifBlank { null }),
		)
	}

	/** Bifarea e reversibilă: dacă a bifat din greșeală, o apasă din nou. */
	suspend fun toggleTodo(todo: Todo) {
		val done = !todo.done
		db.todos().upsert(todo.copy(done = done, doneAt = if (done) now() else null))
	}

	suspend fun deleteTodo(todo: Todo) {
		db.todos().delete(todo)
	}

	suspend fun addMaterial(jobId: String, what: String, qty: String? = null, shop: String? = null) {
		val clean = what.trim()
		if (clean.isEmpty()) return
		db.materials().upsert(
			Material(
				jobId = jobId,
				what = clean,
				qty = qty?.trim()?.ifBlank { null },
				shop = shop?.trim()?.ifBlank { null },
			),
		)
	}

	suspend fun toggleMaterial(material: Material) {
		db.materials().upsert(material.copy(bought = !material.bought))
	}

	suspend fun deleteMaterial(material: Material) {
		db.materials().delete(material)
	}

	/**
	 * Măsurătoare (SPEC §4). Locul e obligatoriu, prețul pe unitate nu: la lucrările la corp
	 * măsura se notează doar pentru textul facturii. Cantitatea rămâne Double.
	 */
	suspend fun addMeasure(
		jobId: String,
		place: String,
		qty: Double,
		unit: MeasureUnit,
		work: String? = null,
		unitPriceCents: Long? = null,
		date: LocalDate = today(),
	) {
		val clean = place.trim()
		if (clean.isEmpty()) return
		db.measures().upsert(
			Measure(
				jobId = jobId,
				place = clean,
				work = work?.trim()?.ifBlank { null },
				qty = qty,
				unit = unit,
				unitPriceCents = unitPriceCents,
				date = date,
			),
		)
	}

	suspend fun saveMeasure(measure: Measure) {
		val clean = measure.place.trim()
		if (clean.isEmpty()) return
		db.measures().upsert(
			measure.copy(place = clean, work = measure.work?.trim()?.ifBlank { null }),
		)
	}

	suspend fun deleteMeasure(measure: Measure) {
		db.measures().delete(measure)
	}

	/**
	 * Extra (SPEC §4 și §5.1): ce a cerut clientul peste ce s-a vorbit. Se ține minte și
	 * dovada înțelegerii, ca la sfârșit să nu fie discuție. Un extra făcut din bunăvoință
	 * rămâne nebifat la se pune pe factură, deci nu intră în bani.
	 */
	suspend fun addExtra(
		jobId: String,
		what: String,
		priceCents: Long = 0,
		accepted: Boolean = false,
		proof: String? = null,
		billable: Boolean = true,
		date: LocalDate = today(),
	) {
		val clean = what.trim()
		if (clean.isEmpty()) return
		db.extras().upsert(
			Extra(
				jobId = jobId,
				what = clean,
				date = date,
				priceCents = priceCents,
				accepted = accepted,
				proof = proof?.trim()?.ifBlank { null },
				billable = billable,
			),
		)
	}

	suspend fun saveExtra(extra: Extra) {
		val clean = extra.what.trim()
		if (clean.isEmpty()) return
		db.extras().upsert(
			extra.copy(what = clean, proof = extra.proof?.trim()?.ifBlank { null }),
		)
	}

	/** Înțelegerea se bifează și se debifează dintr-o apăsare, ca orice bifă. */
	suspend fun toggleExtraAccepted(extra: Extra) {
		db.extras().upsert(extra.copy(accepted = !extra.accepted))
	}

	suspend fun deleteExtra(extra: Extra) {
		db.extras().delete(extra)
	}

	/**
	 * Cum se plătește lucrarea și cât s-a vorbit (SPEC §5.1). Prețul care nu mai are sens
	 * se șterge: la plata pe zile nu rămâne agățat un preț la corp din greșeală.
	 */
	suspend fun setBilling(
		job: Job,
		billing: Billing,
		agreedPriceCents: Long?,
		dayRateCents: Long?,
	) {
		db.jobs().upsert(
			job.copy(
				billing = billing,
				agreedPriceCents = if (billing == Billing.CORP) agreedPriceCents else null,
				dayRateCents = if (billing == Billing.ZILE) dayRateCents else null,
			),
		)
	}

	/**
	 * Bani intrați (SPEC §5.2). Suma zero nu se salvează: n-are ce să însemne o încasare
	 * de nimic, iar o apăsare greșită nu are voie să umple lista.
	 */
	suspend fun addPayment(
		jobId: String,
		amountCents: Long,
		method: Method = Method.BONIFICO,
		date: LocalDate = today(),
		note: String? = null,
	) {
		if (amountCents == 0L) return
		db.payments().upsert(
			Payment(
				jobId = jobId,
				date = date,
				amountCents = amountCents,
				method = method,
				note = note?.trim()?.ifBlank { null },
			),
		)
	}

	suspend fun savePayment(payment: Payment) {
		if (payment.amountCents == 0L) return
		db.payments().upsert(payment.copy(note = payment.note?.trim()?.ifBlank { null }))
	}

	suspend fun deletePayment(payment: Payment) {
		db.payments().delete(payment)
	}

	/**
	 * Evidența facturilor trimise (SPEC §5.2). Aplicația nu emite nimic: ține minte doar
	 * numărul, suma și data, ca să știe ce a cerut deja. Bifa de încasat e separată,
	 * pentru că o factură trimisă nu înseamnă bani în mână.
	 */
	suspend fun addInvoice(
		jobId: String,
		amountCents: Long,
		number: String? = null,
		kind: InvoiceKind = InvoiceKind.SALDO,
		date: LocalDate? = today(),
		due: LocalDate? = null,
		paid: Boolean = false,
	) {
		if (amountCents == 0L) return
		db.invoices().upsert(
			InvoiceRef(
				jobId = jobId,
				number = number?.trim()?.ifBlank { null },
				date = date,
				amountCents = amountCents,
				kind = kind,
				due = due,
				paid = paid,
			),
		)
	}

	suspend fun saveInvoice(invoice: InvoiceRef) {
		if (invoice.amountCents == 0L) return
		db.invoices().upsert(invoice.copy(number = invoice.number?.trim()?.ifBlank { null }))
	}

	/**
	 * Bifa de încasat schimbă doar starea facturii. Nu creează o încasare: banii se trec
	 * separat, cu data lor, altfel cele două cifre s-ar amesteca (SPEC §5.2).
	 */
	suspend fun toggleInvoicePaid(invoice: InvoiceRef) {
		db.invoices().upsert(invoice.copy(paid = !invoice.paid))
	}

	suspend fun deleteInvoice(invoice: InvoiceRef) {
		db.invoices().delete(invoice)
	}

	/** Facturat și încasat pentru o lucrare, adunate din rândurile lor. */
	private fun sums(jobId: String): Flow<Sums> =
		combine(
			db.invoices().observeByJob(jobId),
			db.payments().observeByJob(jobId),
		) { invoices, payments ->
			Sums(
				invoicedCents = invoices.sumOf { it.amountCents },
				collectedCents = payments.sumOf { it.amountCents },
			)
		}

	/**
	 * Toate cifrele unei lucrări, recalculate singure la orice schimbare: o zi trecută, o
	 * măsurătoare, un extra acceptat, o factură sau o încasare.
	 */
	fun jobTotals(jobId: String): Flow<JobTotals?> =
		combine(
			db.jobs().observe(jobId),
			db.workDays().observeByJob(jobId),
			db.measures().observeByJob(jobId),
			db.extras().observeByJob(jobId),
			sums(jobId),
		) { job, days, measures, extras, sums ->
			if (job == null) {
				null
			} else {
				Totals.of(
					job = job,
					workedDays = days.size,
					measures = measures,
					extras = extras,
					invoicedCents = sums.invoicedCents,
					collectedCents = sums.collectedCents,
				)
			}
		}

	/**
	 * Banii pe toate lucrările. Măsurătorile și extra-urile se citesc o dată, dintr-o
	 * bucată, și se împart pe lucrări în memorie: la câteva zeci de lucrări e mai ieftin
	 * decât o interogare pentru fiecare. Lucrările anulate nu apar nicăieri.
	 */
	fun moneyBoard(): Flow<List<JobMoney>> =
		combine(
			db.jobs().observeBoard(),
			db.measures().observeAll(),
			db.extras().observeAll(),
		) { rows, measures, extras ->
			val measuresByJob = measures.groupBy { it.jobId }
			val extrasByJob = extras.groupBy { it.jobId }
			rows.filter { it.job.status != JobStatus.ANULAT }
				.map { row ->
					JobMoney(
						job = row.job,
						clientName = row.clientName,
						totals = Totals.of(
							job = row.job,
							workedDays = row.workedDays,
							measures = measuresByJob[row.job.id].orEmpty(),
							extras = extrasByJob[row.job.id].orEmpty(),
							invoicedCents = row.invoicedCents,
							collectedCents = row.collectedCents,
						),
					)
				}
		}

	/**
	 * Cele trei cifre mari și lista De facturat (SPEC §5.3). Data vine de afară, ca ecranul
	 * să arate corect și dacă telefonul a rămas deschis peste noapte.
	 */
	fun moneySummary(date: LocalDate): Flow<MoneySummary> =
		combine(
			moneyBoard(),
			db.payments().observeCollectedSince(date.withDayOfMonth(1)),
			db.invoices().observeOverdueBefore(date.minusDays(30)),
		) { rows, collectedThisMonth, overdue ->
			val live = rows.filter { it.job.status != JobStatus.OFERTAT }
			MoneySummary(
				// O lucrare încasată în plus nu are voie să acopere alta neîncasată.
				outstandingCents = live.sumOf { it.totals.outstandingCents.coerceAtLeast(0L) },
				overdueCents = overdue,
				collectedThisMonthCents = collectedThisMonth,
				toInvoice = live
					.filter { it.totals.toInvoiceCents > 0 }
					.sortedByDescending { it.totals.toInvoiceCents },
			)
		}

	/** Câte resturi nebifate are lucrarea. Interfața întreabă înainte să pună Terminat. */
	suspend fun openTodoCount(jobId: String): Int = db.todos().openCount(jobId)

	/**
	 * Toate etapele bifate, dar au rămas resturi: propune De finisat în loc de Terminat
	 * (SPEC §5.6). Doar propune; apasă omul.
	 */
	suspend fun suggestsDeFinisat(jobId: String): Boolean {
		val job = db.jobs().byId(jobId) ?: return false
		return Rules.suggestsDeFinisat(
			current = job.status,
			stageCount = db.stages().count(jobId),
			openStages = db.stages().openCount(jobId),
			openTodos = db.todos().openCount(jobId),
		)
	}

	companion object {
		/** Lucrările vii, cele care apar pe ecranul Azi (SPEC §6). */
		val ACTIVE_STATUSES: List<JobStatus> = listOf(
			JobStatus.IN_LUCRU,
			JobStatus.DE_FINISAT,
			JobStatus.ASTEPTARE,
		)
	}
}
