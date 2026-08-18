package com.emanus.lucrari.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

// Modelul din SPEC §4. Datele calendaristice sunt LocalDate (TEXT ISO prin Converters),
// timestamp-urile sunt Long epoch millis, sumele sunt cenți în Long.
// Copiii unei lucrări se șterg în cascadă odată cu ea, ca să nu rămână date orfane.

@Entity(tableName = "clients")
data class Client(
	@PrimaryKey val id: String = uuid(),
	val name: String,
	val phone: String? = null,
	val note: String? = null,
	val createdAt: Long = now(),
)

@Entity(
	tableName = "jobs",
	foreignKeys = [
		ForeignKey(
			entity = Client::class,
			parentColumns = ["id"],
			childColumns = ["clientId"],
			onDelete = ForeignKey.CASCADE,
		),
	],
	indices = [Index("clientId"), Index("status")],
)
data class Job(
	@PrimaryKey val id: String = uuid(),
	val clientId: String,
	val title: String,
	val street: String? = null,
	val city: String? = null,
	val addrNote: String? = null,
	val type: String? = null,
	val status: JobStatus = JobStatus.OFERTAT,
	val plannedStart: LocalDate? = null,
	val estDays: Int? = null,
	val billing: Billing = Billing.CORP,
	val agreedPriceCents: Long? = null,
	val dayRateCents: Long? = null,
	val note: String? = null,
	val createdAt: Long = now(),
	val closedAt: Long? = null,
)

@Entity(
	tableName = "stages",
	foreignKeys = [ForeignKey(Job::class, ["id"], ["jobId"], onDelete = ForeignKey.CASCADE)],
	indices = [Index("jobId")],
)
data class Stage(
	@PrimaryKey val id: String = uuid(),
	val jobId: String,
	val name: String,
	val sort: Int,
	val done: Boolean = false,
	val doneAt: Long? = null,
)

@Entity(
	tableName = "work_days",
	foreignKeys = [ForeignKey(Job::class, ["id"], ["jobId"], onDelete = ForeignKey.CASCADE)],
	indices = [Index("jobId")],
)
data class WorkDay(
	@PrimaryKey val id: String = uuid(),
	val jobId: String,
	val date: LocalDate,
	val hours: Double? = null,
	val what: String? = null,
	val isExtra: Boolean = false,
	val blocked: String? = null,
)

@Entity(
	tableName = "todos",
	foreignKeys = [ForeignKey(Job::class, ["id"], ["jobId"], onDelete = ForeignKey.CASCADE)],
	indices = [Index("jobId")],
)
data class Todo(
	@PrimaryKey val id: String = uuid(),
	val jobId: String,
	val place: String? = null,
	val what: String,
	val reason: Reason? = null,
	val due: LocalDate? = null,
	val done: Boolean = false,
	val doneAt: Long? = null,
)

@Entity(
	tableName = "materials",
	foreignKeys = [ForeignKey(Job::class, ["id"], ["jobId"], onDelete = ForeignKey.CASCADE)],
	indices = [Index("jobId")],
)
data class Material(
	@PrimaryKey val id: String = uuid(),
	val jobId: String,
	val what: String,
	val qty: String? = null,
	val shop: String? = null,
	val bought: Boolean = false,
)

@Entity(
	tableName = "measures",
	foreignKeys = [ForeignKey(Job::class, ["id"], ["jobId"], onDelete = ForeignKey.CASCADE)],
	indices = [Index("jobId")],
)
data class Measure(
	@PrimaryKey val id: String = uuid(),
	val jobId: String,
	val place: String,
	val work: String? = null,
	val qty: Double,
	val unit: MeasureUnit,
	val unitPriceCents: Long? = null,
	val date: LocalDate = today(),
)

@Entity(
	tableName = "extras",
	foreignKeys = [ForeignKey(Job::class, ["id"], ["jobId"], onDelete = ForeignKey.CASCADE)],
	indices = [Index("jobId")],
)
data class Extra(
	@PrimaryKey val id: String = uuid(),
	val jobId: String,
	val what: String,
	val date: LocalDate = today(),
	val priceCents: Long = 0,
	val accepted: Boolean = false,
	val proof: String? = null,
	val billable: Boolean = true,
)

@Entity(
	tableName = "payments",
	foreignKeys = [ForeignKey(Job::class, ["id"], ["jobId"], onDelete = ForeignKey.CASCADE)],
	indices = [Index("jobId")],
)
data class Payment(
	@PrimaryKey val id: String = uuid(),
	val jobId: String,
	val date: LocalDate,
	val amountCents: Long,
	val method: Method = Method.CASH,
	val note: String? = null,
)

@Entity(
	tableName = "invoices",
	foreignKeys = [ForeignKey(Job::class, ["id"], ["jobId"], onDelete = ForeignKey.CASCADE)],
	indices = [Index("jobId")],
)
data class InvoiceRef(
	@PrimaryKey val id: String = uuid(),
	val jobId: String,
	val number: String? = null,
	val date: LocalDate? = null,
	val amountCents: Long = 0,
	val kind: InvoiceKind = InvoiceKind.SALDO,
	val due: LocalDate? = null,
	val paid: Boolean = false,
)

@Entity(
	tableName = "photos",
	foreignKeys = [ForeignKey(Job::class, ["id"], ["jobId"], onDelete = ForeignKey.CASCADE)],
	indices = [Index("jobId")],
)
data class Photo(
	@PrimaryKey val id: String = uuid(),
	val jobId: String,
	val dayId: String? = null,
	val todoId: String? = null,
	val path: String,
	val phase: Phase = Phase.DURING,
	val takenAt: Long = now(),
)

// Memento-ul poate fi legat de o lucrare, de un client sau de nimic (notare liberă),
// de aceea nu are chei străine, doar indici.
@Entity(tableName = "reminders", indices = [Index("jobId"), Index("clientId")])
data class Reminder(
	@PrimaryKey val id: String = uuid(),
	val jobId: String? = null,
	val clientId: String? = null,
	val text: String,
	val dueAt: Long,
	val auto: Boolean = false,
	val done: Boolean = false,
)
