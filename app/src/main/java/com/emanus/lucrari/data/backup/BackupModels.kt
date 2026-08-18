package com.emanus.lucrari.data.backup

import com.emanus.lucrari.data.Billing
import com.emanus.lucrari.data.Client
import com.emanus.lucrari.data.Extra
import com.emanus.lucrari.data.InvoiceKind
import com.emanus.lucrari.data.InvoiceRef
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.data.Material
import com.emanus.lucrari.data.Measure
import com.emanus.lucrari.data.MeasureUnit
import com.emanus.lucrari.data.Method
import com.emanus.lucrari.data.Payment
import com.emanus.lucrari.data.Phase
import com.emanus.lucrari.data.Photo
import com.emanus.lucrari.data.Reason
import com.emanus.lucrari.data.Reminder
import com.emanus.lucrari.data.Stage
import com.emanus.lucrari.data.Todo
import com.emanus.lucrari.data.WorkDay
import java.io.File
import java.time.LocalDate
import kotlinx.serialization.Serializable

const val BACKUP_SCHEMA_VERSION = 1

@Serializable
data class BackupPayload(
	val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
	val exportedAt: String,
	val clients: List<ClientRecord> = emptyList(),
	val jobs: List<JobRecord> = emptyList(),
	val stages: List<StageRecord> = emptyList(),
	val workDays: List<WorkDayRecord> = emptyList(),
	val todos: List<TodoRecord> = emptyList(),
	val materials: List<MaterialRecord> = emptyList(),
	val measures: List<MeasureRecord> = emptyList(),
	val extras: List<ExtraRecord> = emptyList(),
	val payments: List<PaymentRecord> = emptyList(),
	val invoices: List<InvoiceRecord> = emptyList(),
	val photos: List<PhotoRecord> = emptyList(),
	val reminders: List<ReminderRecord> = emptyList(),
)

@Serializable
data class ClientRecord(
	val id: String,
	val name: String,
	val phone: String?,
	val note: String?,
	val createdAt: Long,
)

@Serializable
data class JobRecord(
	val id: String,
	val clientId: String,
	val title: String,
	val street: String?,
	val city: String?,
	val addrNote: String?,
	val type: String?,
	val status: String,
	val plannedStart: String?,
	val estDays: Int?,
	val billing: String,
	val agreedPriceCents: Long?,
	val dayRateCents: Long?,
	val note: String?,
	val createdAt: Long,
	val closedAt: Long?,
)

@Serializable
data class StageRecord(
	val id: String,
	val jobId: String,
	val name: String,
	val sort: Int,
	val done: Boolean,
	val doneAt: Long?,
)

@Serializable
data class WorkDayRecord(
	val id: String,
	val jobId: String,
	val date: String,
	val hours: Double?,
	val what: String?,
	val isExtra: Boolean,
	val blocked: String?,
)

@Serializable
data class TodoRecord(
	val id: String,
	val jobId: String,
	val place: String?,
	val what: String,
	val reason: String?,
	val due: String?,
	val done: Boolean,
	val doneAt: Long?,
)

@Serializable
data class MaterialRecord(
	val id: String,
	val jobId: String,
	val what: String,
	val qty: String?,
	val shop: String?,
	val bought: Boolean,
)

@Serializable
data class MeasureRecord(
	val id: String,
	val jobId: String,
	val place: String,
	val work: String?,
	val qty: Double,
	val unit: String,
	val unitPriceCents: Long?,
	val date: String,
)

@Serializable
data class ExtraRecord(
	val id: String,
	val jobId: String,
	val what: String,
	val date: String,
	val priceCents: Long,
	val accepted: Boolean,
	val proof: String?,
	val billable: Boolean,
)

@Serializable
data class PaymentRecord(
	val id: String,
	val jobId: String,
	val date: String,
	val amountCents: Long,
	val method: String,
	val note: String?,
)

@Serializable
data class InvoiceRecord(
	val id: String,
	val jobId: String,
	val number: String?,
	val date: String?,
	val amountCents: Long,
	val kind: String,
	val due: String?,
	val paid: Boolean,
)

@Serializable
data class PhotoRecord(
	val id: String,
	val jobId: String,
	val dayId: String?,
	val todoId: String?,
	val fileName: String,
	val phase: String,
	val takenAt: Long,
)

@Serializable
data class ReminderRecord(
	val id: String,
	val jobId: String?,
	val clientId: String?,
	val text: String,
	val dueAt: Long,
	val auto: Boolean,
	val done: Boolean,
)

data class BackupSnapshot(
	val clients: List<Client>,
	val jobs: List<Job>,
	val stages: List<Stage>,
	val workDays: List<WorkDay>,
	val todos: List<Todo>,
	val materials: List<Material>,
	val measures: List<Measure>,
	val extras: List<Extra>,
	val payments: List<Payment>,
	val invoices: List<InvoiceRef>,
	val photos: List<Photo>,
	val reminders: List<Reminder>,
) {
	val recordCount: Int
		get() = clients.size + jobs.size + stages.size + workDays.size + todos.size +
			materials.size + measures.size + extras.size + payments.size + invoices.size +
			photos.size + reminders.size
}

fun BackupSnapshot.toPayload(exportedAt: String): BackupPayload = BackupPayload(
	exportedAt = exportedAt,
	clients = clients.map { ClientRecord(it.id, it.name, it.phone, it.note, it.createdAt) },
	jobs = jobs.map {
		JobRecord(
			it.id, it.clientId, it.title, it.street, it.city, it.addrNote, it.type,
			it.status.name, it.plannedStart?.toString(), it.estDays, it.billing.name,
			it.agreedPriceCents, it.dayRateCents, it.note, it.createdAt, it.closedAt,
		)
	},
	stages = stages.map { StageRecord(it.id, it.jobId, it.name, it.sort, it.done, it.doneAt) },
	workDays = workDays.map {
		WorkDayRecord(it.id, it.jobId, it.date.toString(), it.hours, it.what, it.isExtra, it.blocked)
	},
	todos = todos.map {
		TodoRecord(
			it.id, it.jobId, it.place, it.what, it.reason?.name, it.due?.toString(), it.done,
			it.doneAt,
		)
	},
	materials = materials.map { MaterialRecord(it.id, it.jobId, it.what, it.qty, it.shop, it.bought) },
	measures = measures.map {
		MeasureRecord(
			it.id, it.jobId, it.place, it.work, it.qty, it.unit.name, it.unitPriceCents,
			it.date.toString(),
		)
	},
	extras = extras.map {
		ExtraRecord(
			it.id, it.jobId, it.what, it.date.toString(), it.priceCents, it.accepted, it.proof,
			it.billable,
		)
	},
	payments = payments.map {
		PaymentRecord(it.id, it.jobId, it.date.toString(), it.amountCents, it.method.name, it.note)
	},
	invoices = invoices.map {
		InvoiceRecord(
			it.id, it.jobId, it.number, it.date?.toString(), it.amountCents, it.kind.name,
			it.due?.toString(), it.paid,
		)
	},
	photos = photos.map {
		PhotoRecord(it.id, it.jobId, it.dayId, it.todoId, "${it.id}.jpg", it.phase.name, it.takenAt)
	},
	reminders = reminders.map {
		ReminderRecord(it.id, it.jobId, it.clientId, it.text, it.dueAt, it.auto, it.done)
	},
)

fun BackupPayload.toSnapshot(photoRoot: File): BackupSnapshot = BackupSnapshot(
	clients = clients.map { Client(it.id, it.name, it.phone, it.note, it.createdAt) },
	jobs = jobs.map {
		Job(
			id = it.id,
			clientId = it.clientId,
			title = it.title,
			street = it.street,
			city = it.city,
			addrNote = it.addrNote,
			type = it.type,
			status = JobStatus.valueOf(it.status),
			plannedStart = it.plannedStart?.let(LocalDate::parse),
			estDays = it.estDays,
			billing = Billing.valueOf(it.billing),
			agreedPriceCents = it.agreedPriceCents,
			dayRateCents = it.dayRateCents,
			note = it.note,
			createdAt = it.createdAt,
			closedAt = it.closedAt,
		)
	},
	stages = stages.map { Stage(it.id, it.jobId, it.name, it.sort, it.done, it.doneAt) },
	workDays = workDays.map {
		WorkDay(it.id, it.jobId, LocalDate.parse(it.date), it.hours, it.what, it.isExtra, it.blocked)
	},
	todos = todos.map {
		Todo(
			it.id, it.jobId, it.place, it.what, it.reason?.let(Reason::valueOf),
			it.due?.let(LocalDate::parse), it.done, it.doneAt,
		)
	},
	materials = materials.map { Material(it.id, it.jobId, it.what, it.qty, it.shop, it.bought) },
	measures = measures.map {
		Measure(
			it.id, it.jobId, it.place, it.work, it.qty, MeasureUnit.valueOf(it.unit),
			it.unitPriceCents, LocalDate.parse(it.date),
		)
	},
	extras = extras.map {
		Extra(
			it.id, it.jobId, it.what, LocalDate.parse(it.date), it.priceCents, it.accepted,
			it.proof, it.billable,
		)
	},
	payments = payments.map {
		Payment(
			it.id, it.jobId, LocalDate.parse(it.date), it.amountCents, Method.valueOf(it.method),
			it.note,
		)
	},
	invoices = invoices.map {
		InvoiceRef(
			it.id, it.jobId, it.number, it.date?.let(LocalDate::parse), it.amountCents,
			InvoiceKind.valueOf(it.kind), it.due?.let(LocalDate::parse), it.paid,
		)
	},
	photos = photos.map {
		Photo(
			it.id, it.jobId, it.dayId, it.todoId, File(photoRoot, it.fileName).absolutePath,
			Phase.valueOf(it.phase), it.takenAt,
		)
	},
	reminders = reminders.map {
		Reminder(it.id, it.jobId, it.clientId, it.text, it.dueAt, it.auto, it.done)
	},
)
