package com.emanus.lucrari.ui.screen

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emanus.lucrari.App
import com.emanus.lucrari.R
import com.emanus.lucrari.data.Billing
import com.emanus.lucrari.data.InvoiceKind
import com.emanus.lucrari.data.InvoiceRef
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.Method
import com.emanus.lucrari.data.Payment
import com.emanus.lucrari.data.today
import com.emanus.lucrari.domain.Dates
import com.emanus.lucrari.domain.JobTotals
import com.emanus.lucrari.domain.Money
import com.emanus.lucrari.ui.component.InvoiceSheet
import com.emanus.lucrari.ui.component.PaymentSheet
import com.emanus.lucrari.ui.component.labelRes
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Lucrarea plus numele clientului, atât cât îi trebuie capului de ecran. */
data class JobHeader(val job: Job, val clientName: String)

class JobMoneyViewModel(app: Application) : AndroidViewModel(app) {

	private val repo = (app as App).repo

	fun header(jobId: String): Flow<JobHeader?> =
		combine(repo.job(jobId), repo.clients()) { job, clients ->
			if (job == null) {
				null
			} else {
				JobHeader(job, clients.firstOrNull { it.id == job.clientId }?.name.orEmpty())
			}
		}

	fun totals(jobId: String): Flow<JobTotals?> = repo.jobTotals(jobId)

	fun payments(jobId: String): Flow<List<Payment>> = repo.payments(jobId)

	fun invoices(jobId: String): Flow<List<InvoiceRef>> = repo.invoices(jobId)

	fun setBilling(job: Job, billing: Billing, agreedPriceCents: Long?, dayRateCents: Long?) {
		viewModelScope.launch { repo.setBilling(job, billing, agreedPriceCents, dayRateCents) }
	}

	fun addPayment(jobId: String, amountCents: Long, method: Method, date: LocalDate, note: String) {
		viewModelScope.launch { repo.addPayment(jobId, amountCents, method, date, note) }
	}

	fun savePayment(payment: Payment) {
		viewModelScope.launch { repo.savePayment(payment) }
	}

	fun deletePayment(payment: Payment) {
		viewModelScope.launch { repo.deletePayment(payment) }
	}

	fun addInvoice(
		jobId: String,
		amountCents: Long,
		number: String,
		kind: InvoiceKind,
		date: LocalDate,
		paid: Boolean,
	) {
		viewModelScope.launch {
			repo.addInvoice(
				jobId = jobId,
				amountCents = amountCents,
				number = number,
				kind = kind,
				date = date,
				paid = paid,
			)
		}
	}

	fun saveInvoice(invoice: InvoiceRef) {
		viewModelScope.launch { repo.saveInvoice(invoice) }
	}

	fun toggleInvoicePaid(invoice: InvoiceRef) {
		viewModelScope.launch { repo.toggleInvoicePaid(invoice) }
	}

	fun deleteInvoice(invoice: InvoiceRef) {
		viewModelScope.launch { repo.deleteInvoice(invoice) }
	}
}

/**
 * Banii unei lucrări (SPEC §5). Sus se spune cum se plătește și cât s-a vorbit, apoi ies
 * cifrele singure, apoi ce a intrat și ce s-a facturat. Aplicația nu emite facturi:
 * lista de facturi e doar evidența a ce a trimis el.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobMoneyScreen(
	jobId: String,
	onBack: () -> Unit,
	vm: JobMoneyViewModel = viewModel(),
) {
	val header by remember(jobId) { vm.header(jobId) }.collectAsState(initial = null)
	val totals by remember(jobId) { vm.totals(jobId) }.collectAsState(initial = null)
	val payments by remember(jobId) { vm.payments(jobId) }.collectAsState(initial = emptyList())
	val invoices by remember(jobId) { vm.invoices(jobId) }.collectAsState(initial = emptyList())

	var showNewPayment by rememberSaveable { mutableStateOf(false) }
	var editedPaymentId by rememberSaveable { mutableStateOf<String?>(null) }
	var showNewInvoice by rememberSaveable { mutableStateOf(false) }
	var editedInvoiceId by rememberSaveable { mutableStateOf<String?>(null) }

	val editedPayment = payments.firstOrNull { it.id == editedPaymentId }
	val editedInvoice = invoices.firstOrNull { it.id == editedInvoiceId }
	val overdueBefore = today().minusDays(30)

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.job_money_title)) },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(
							Icons.AutoMirrored.Outlined.ArrowBack,
							contentDescription = stringResource(R.string.back),
						)
					}
				},
			)
		},
	) { padding ->
		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding),
			contentPadding = PaddingValues(16.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			val loaded = header
			if (loaded != null) {
				item {
					Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
						Text(
							text = loaded.job.title,
							style = MaterialTheme.typography.titleLarge,
						)
						val where = listOfNotNull(
							loaded.clientName.ifBlank { null },
							loaded.job.street,
							loaded.job.city,
						).joinToString(", ")
						if (where.isNotEmpty()) {
							Text(text = where, style = MaterialTheme.typography.bodyMedium)
						}
					}
				}

				item {
					BillingCard(
						job = loaded.job,
						onSave = { billing, agreed, dayRate ->
							vm.setBilling(loaded.job, billing, agreed, dayRate)
						},
					)
				}

				val numbers = totals
				if (numbers != null) {
					item { TotalsCard(job = loaded.job, totals = numbers) }
				}
			}

			item {
				SectionHeader(
					titleRes = R.string.payments_title,
					actionRes = R.string.payment_add,
					onAction = { showNewPayment = true },
				)
			}
			if (payments.isEmpty()) {
				item {
					Text(
						text = stringResource(R.string.payments_empty),
						style = MaterialTheme.typography.bodyMedium,
					)
				}
			}
			items(payments, key = { it.id }) { payment ->
				PaymentRow(payment = payment, onOpen = { editedPaymentId = payment.id })
			}

			item {
				SectionHeader(
					titleRes = R.string.invoices_title,
					actionRes = R.string.invoice_add,
					onAction = { showNewInvoice = true },
				)
			}
			item {
				Text(
					text = stringResource(R.string.invoice_note_app),
					style = MaterialTheme.typography.bodySmall,
				)
			}
			if (invoices.isEmpty()) {
				item {
					Text(
						text = stringResource(R.string.invoices_empty),
						style = MaterialTheme.typography.bodyMedium,
					)
				}
			}
			items(invoices, key = { it.id }) { invoice ->
				InvoiceRow(
					invoice = invoice,
					overdueBefore = overdueBefore,
					onTogglePaid = { vm.toggleInvoicePaid(invoice) },
					onOpen = { editedInvoiceId = invoice.id },
				)
			}
		}
	}

	if (showNewPayment) {
		PaymentSheet(
			payment = null,
			title = stringResource(R.string.payment_new_title),
			onDismiss = { showNewPayment = false },
			onDelete = { showNewPayment = false },
			onSave = { amountCents, method, date, note ->
				vm.addPayment(jobId, amountCents, method, date, note)
				showNewPayment = false
			},
		)
	}

	if (editedPayment != null) {
		PaymentSheet(
			payment = editedPayment,
			title = stringResource(R.string.payment_edit_title),
			onDismiss = { editedPaymentId = null },
			onDelete = {
				vm.deletePayment(editedPayment)
				editedPaymentId = null
			},
			onSave = { amountCents, method, date, note ->
				vm.savePayment(
					editedPayment.copy(
						amountCents = amountCents,
						method = method,
						date = date,
						note = note,
					),
				)
				editedPaymentId = null
			},
		)
	}

	if (showNewInvoice) {
		InvoiceSheet(
			invoice = null,
			title = stringResource(R.string.invoice_new_title),
			onDismiss = { showNewInvoice = false },
			onDelete = { showNewInvoice = false },
			onSave = { number, amountCents, kind, date, paid ->
				vm.addInvoice(jobId, amountCents, number, kind, date, paid)
				showNewInvoice = false
			},
		)
	}

	if (editedInvoice != null) {
		InvoiceSheet(
			invoice = editedInvoice,
			title = stringResource(R.string.invoice_edit_title),
			onDismiss = { editedInvoiceId = null },
			onDelete = {
				vm.deleteInvoice(editedInvoice)
				editedInvoiceId = null
			},
			onSave = { number, amountCents, kind, date, paid ->
				vm.saveInvoice(
					editedInvoice.copy(
						number = number,
						amountCents = amountCents,
						kind = kind,
						date = date,
						paid = paid,
					),
				)
				editedInvoiceId = null
			},
		)
	}
}

/**
 * Felul plății și prețul vorbit. Fără cifra asta lucrarea nu are cum să arate bani, așa că
 * stă prima pe ecran.
 */
@Composable
private fun BillingCard(job: Job, onSave: (Billing, Long?, Long?) -> Unit) {
	var billingName by rememberSaveable(job.id) { mutableStateOf(job.billing.name) }
	var agreed by rememberSaveable(job.id) {
		mutableStateOf(job.agreedPriceCents?.let { Money.plain(it) }.orEmpty())
	}
	var dayRate by rememberSaveable(job.id) {
		mutableStateOf(job.dayRateCents?.let { Money.plain(it) }.orEmpty())
	}
	val billing = Billing.valueOf(billingName)

	Card(modifier = Modifier.fillMaxWidth()) {
		Column(
			modifier = Modifier.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			Text(
				text = stringResource(R.string.money_billing),
				style = MaterialTheme.typography.titleMedium,
			)
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.horizontalScroll(rememberScrollState()),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
			) {
				Billing.entries.forEach { option ->
					FilterChip(
						selected = billingName == option.name,
						onClick = { billingName = option.name },
						label = { Text(stringResource(option.labelRes)) },
					)
				}
			}

			when (billing) {
				Billing.CORP -> OutlinedTextField(
					value = agreed,
					onValueChange = { agreed = it },
					label = { Text(stringResource(R.string.money_agreed_price)) },
					modifier = Modifier.fillMaxWidth(),
					singleLine = true,
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
				)

				Billing.ZILE -> OutlinedTextField(
					value = dayRate,
					onValueChange = { dayRate = it },
					label = { Text(stringResource(R.string.money_day_rate)) },
					modifier = Modifier.fillMaxWidth(),
					singleLine = true,
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
				)

				Billing.MASURA -> Text(
					text = stringResource(R.string.money_base_masura),
					style = MaterialTheme.typography.bodyMedium,
				)
			}

			Button(
				onClick = { onSave(billing, Money.parse(agreed), Money.parse(dayRate)) },
				modifier = Modifier
					.fillMaxWidth()
					.height(56.dp),
			) {
				Text(stringResource(R.string.money_billing_save))
			}
		}
	}
}

@Composable
private fun TotalsCard(job: Job, totals: JobTotals) {
	val days = job.dayRateCents?.takeIf { it > 0 }?.let { totals.baseCents / it } ?: 0L
	val baseLabel = when (job.billing) {
		Billing.CORP -> stringResource(R.string.money_base)
		Billing.MASURA -> stringResource(R.string.money_base_masura)
		Billing.ZILE -> stringResource(
			R.string.money_base_zile,
			Money.format(job.dayRateCents ?: 0L),
			days.toInt(),
		)
	}

	Card(modifier = Modifier.fillMaxWidth()) {
		Column(
			modifier = Modifier.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp),
		) {
			MoneyLine(label = baseLabel, value = Money.format(totals.baseCents))
			MoneyLine(
				label = stringResource(R.string.money_extras),
				value = Money.format(totals.extrasCents),
			)
			HorizontalDivider()
			MoneyLine(
				label = stringResource(R.string.money_total),
				value = Money.format(totals.totalCents),
				strong = true,
			)
			MoneyLine(
				label = stringResource(R.string.money_invoiced),
				value = Money.format(totals.invoicedCents),
			)
			MoneyLine(
				label = stringResource(R.string.money_to_invoice),
				value = Money.format(totals.toInvoiceCents),
				strong = true,
			)
			HorizontalDivider()
			MoneyLine(
				label = stringResource(R.string.money_collected),
				value = Money.format(totals.collectedCents),
			)
			MoneyLine(
				label = stringResource(R.string.money_rest),
				value = Money.format(totals.outstandingCents),
				strong = true,
			)
			if (totals.toInvoiceCents < 0) {
				Text(
					text = stringResource(R.string.money_over_invoiced),
					style = MaterialTheme.typography.bodySmall,
				)
			}
			Text(
				text = stringResource(R.string.money_note_split),
				style = MaterialTheme.typography.bodySmall,
			)
		}
	}
}

@Composable
private fun MoneyLine(label: String, value: String, strong: Boolean = false) {
	val style = if (strong) {
		MaterialTheme.typography.titleMedium
	} else {
		MaterialTheme.typography.bodyMedium
	}
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.SpaceBetween,
	) {
		Text(text = label, style = style)
		Text(text = value, style = style)
	}
}

@Composable
private fun SectionHeader(titleRes: Int, actionRes: Int, onAction: () -> Unit) {
	Row(
		modifier = Modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.CenterVertically,
	) {
		Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
		TextButton(onClick = onAction) { Text(stringResource(actionRes)) }
	}
}

@Composable
private fun PaymentRow(payment: Payment, onOpen: () -> Unit) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onOpen),
	) {
		Column(
			modifier = Modifier.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(2.dp),
		) {
			Text(
				text = Money.format(payment.amountCents) + " · " +
					stringResource(payment.method.labelRes) + " · " +
					Dates.dayMonth(payment.date),
				style = MaterialTheme.typography.bodyLarge,
			)
			val note = payment.note
			if (!note.isNullOrBlank()) {
				Text(text = note, style = MaterialTheme.typography.bodySmall)
			}
		}
	}
}

/**
 * Rândul unei facturi. Bifa de încasat se schimbă doar din cerc, niciodată dintr-o atingere
 * greșită pe rând: textul deschide editarea.
 */
@Composable
private fun InvoiceRow(
	invoice: InvoiceRef,
	overdueBefore: LocalDate,
	onTogglePaid: () -> Unit,
	onOpen: () -> Unit,
) {
	val date = invoice.date
	val overdue = !invoice.paid && date != null && !date.isAfter(overdueBefore)

	Card(modifier = Modifier.fillMaxWidth()) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(8.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp),
		) {
			IconButton(onClick = onTogglePaid, modifier = Modifier.size(48.dp)) {
				Icon(
					imageVector = if (invoice.paid) {
						Icons.Outlined.CheckCircle
					} else {
						Icons.Outlined.RadioButtonUnchecked
					},
					contentDescription = stringResource(R.string.invoice_paid),
				)
			}
			Column(
				modifier = Modifier
					.weight(1f)
					.clickable(onClick = onOpen)
					.padding(vertical = 8.dp),
				verticalArrangement = Arrangement.spacedBy(2.dp),
			) {
				val head = listOf(
					Money.format(invoice.amountCents),
					invoice.number ?: stringResource(R.string.invoice_no_number),
					stringResource(invoice.kind.labelRes),
				).joinToString(" · ")
				Text(text = head, style = MaterialTheme.typography.bodyLarge)
				val status = when {
					invoice.paid -> stringResource(R.string.invoice_paid)
					overdue -> stringResource(R.string.invoice_overdue)
					else -> stringResource(R.string.invoice_not_paid)
				}
				val line = if (date != null) status + " · " + Dates.dayMonth(date) else status
				Text(text = line, style = MaterialTheme.typography.bodySmall)
			}
		}
	}
}
