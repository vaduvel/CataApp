package com.emanus.lucrari.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.emanus.lucrari.R
import com.emanus.lucrari.ui.theme.Dimens
import com.emanus.lucrari.data.Billing
import com.emanus.lucrari.data.InvoiceKind
import com.emanus.lucrari.data.InvoiceRef
import com.emanus.lucrari.data.Method
import com.emanus.lucrari.data.Payment
import com.emanus.lucrari.data.today
import com.emanus.lucrari.domain.Dates
import com.emanus.lucrari.domain.Money
import java.time.LocalDate

/** Numele felului de plată, așa cum îl spune el: la corp, la măsură, pe zi. */
val Billing.labelRes: Int
	@StringRes get() = when (this) {
		Billing.CORP -> R.string.billing_corp
		Billing.MASURA -> R.string.billing_masura
		Billing.ZILE -> R.string.billing_zile
	}

val Method.labelRes: Int
	@StringRes get() = when (this) {
		Method.CASH -> R.string.method_cash
		Method.BONIFICO -> R.string.method_bonifico
		Method.ALTUL -> R.string.method_altul
	}

val InvoiceKind.labelRes: Int
	@StringRes get() = when (this) {
		InvoiceKind.ACONTO -> R.string.kind_aconto
		InvoiceKind.SALDO -> R.string.kind_saldo
		InvoiceKind.UNICA -> R.string.kind_unica
	}

/**
 * Data unei încasări sau a unei facturi. De obicei azi, uneori ieri, iar la editare rămâne
 * data scrisă atunci, arătată pe un al treilea jeton.
 */
@Composable
private fun MoneyDateChips(epochDay: Long, onPick: (Long) -> Unit) {
	val todayDate = today()
	val yesterday = todayDate.minusDays(1)
	val picked = LocalDate.ofEpochDay(epochDay)

	Text(
		text = stringResource(R.string.date_when),
		style = MaterialTheme.typography.bodyMedium,
	)
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.horizontalScroll(rememberScrollState()),
		horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
	) {
		FilterChip(
			selected = picked == todayDate,
			onClick = { onPick(todayDate.toEpochDay()) },
			label = { Text(stringResource(R.string.day_today)) },
		)
		FilterChip(
			selected = picked == yesterday,
			onClick = { onPick(yesterday.toEpochDay()) },
			label = { Text(stringResource(R.string.day_yesterday)) },
		)
		if (picked != todayDate && picked != yesterday) {
			FilterChip(
				selected = true,
				onClick = { onPick(epochDay) },
				label = { Text(Dates.dayMonth(picked)) },
			)
		}
	}
}

/**
 * Bani intrați (SPEC §5.2). Se scrie doar ce a intrat cu adevărat, cu data lui: cifra asta
 * nu se atinge de facturi și nu se completează singură din ele.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSheet(
	payment: Payment?,
	title: String,
	onDismiss: () -> Unit,
	onDelete: () -> Unit,
	onSave: (amountCents: Long, method: Method, date: LocalDate, note: String) -> Unit,
) {
	var amount by rememberSaveable(payment?.id) {
		mutableStateOf(payment?.amountCents?.let { Money.plain(it) }.orEmpty())
	}
	var methodName by rememberSaveable(payment?.id) {
		mutableStateOf(payment?.method?.name ?: Method.BONIFICO.name)
	}
	var note by rememberSaveable(payment?.id) { mutableStateOf(payment?.note.orEmpty()) }
	var dateEpoch by rememberSaveable(payment?.id) {
		mutableStateOf((payment?.date ?: today()).toEpochDay())
	}

	val parsedAmount = Money.parse(amount)
	val ready = parsedAmount != null && parsedAmount != 0L

	BrandFormSheet(title = title, onDismiss = onDismiss) {
			OutlinedTextField(
				value = amount,
				onValueChange = { amount = it },
				label = { Text(stringResource(R.string.payment_amount)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
			)

			Text(
				text = stringResource(R.string.payment_method),
				style = MaterialTheme.typography.bodyMedium,
			)
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.horizontalScroll(rememberScrollState()),
				horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
			) {
				Method.entries.forEach { method ->
					FilterChip(
						selected = methodName == method.name,
						onClick = { methodName = method.name },
						label = { Text(stringResource(method.labelRes)) },
					)
				}
			}

			MoneyDateChips(epochDay = dateEpoch, onPick = { dateEpoch = it })

			OutlinedTextField(
				value = note,
				onValueChange = { note = it },
				label = { Text(stringResource(R.string.payment_note)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
			)

			Button(
				onClick = {
					if (parsedAmount != null) {
						onSave(
							parsedAmount,
							Method.valueOf(methodName),
							LocalDate.ofEpochDay(dateEpoch),
							note,
						)
					}
				},
				enabled = ready,
				modifier = Modifier
					.fillMaxWidth()
					.height(Dimens.primaryButtonHeight),
			) {
				Text(stringResource(R.string.save))
			}

			if (payment != null) {
				TextButton(
					onClick = onDelete,
					modifier = Modifier.align(Alignment.CenterHorizontally),
				) {
					Text(stringResource(R.string.payment_delete))
				}
			}
	}
}

/**
 * Evidența unei facturi deja trimise (SPEC §5.2). Aplicația nu emite nimic și nu cere date
 * fiscale: numărul, suma, data și dacă a fost încasată, atât.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceSheet(
	invoice: InvoiceRef?,
	title: String,
	onDismiss: () -> Unit,
	onDelete: () -> Unit,
	onSave: (
		number: String,
		amountCents: Long,
		kind: InvoiceKind,
		date: LocalDate,
		paid: Boolean,
	) -> Unit,
) {
	var number by rememberSaveable(invoice?.id) { mutableStateOf(invoice?.number.orEmpty()) }
	var amount by rememberSaveable(invoice?.id) {
		mutableStateOf(invoice?.amountCents?.let { Money.plain(it) }.orEmpty())
	}
	var kindName by rememberSaveable(invoice?.id) {
		mutableStateOf(invoice?.kind?.name ?: InvoiceKind.ACONTO.name)
	}
	var paid by rememberSaveable(invoice?.id) { mutableStateOf(invoice?.paid ?: false) }
	var dateEpoch by rememberSaveable(invoice?.id) {
		mutableStateOf((invoice?.date ?: today()).toEpochDay())
	}

	val parsedAmount = Money.parse(amount)
	val ready = parsedAmount != null && parsedAmount != 0L

	BrandFormSheet(title = title, onDismiss = onDismiss) {
			Text(
				text = stringResource(R.string.invoice_note_app),
				style = MaterialTheme.typography.bodySmall,
			)

			OutlinedTextField(
				value = amount,
				onValueChange = { amount = it },
				label = { Text(stringResource(R.string.invoice_amount)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
			)

			OutlinedTextField(
				value = number,
				onValueChange = { number = it },
				label = { Text(stringResource(R.string.invoice_number)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
			)

			Text(
				text = stringResource(R.string.invoice_kind),
				style = MaterialTheme.typography.bodyMedium,
			)
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.horizontalScroll(rememberScrollState()),
				horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
			) {
				InvoiceKind.entries.forEach { kind ->
					FilterChip(
						selected = kindName == kind.name,
						onClick = { kindName = kind.name },
						label = { Text(stringResource(kind.labelRes)) },
					)
				}
			}

			MoneyDateChips(epochDay = dateEpoch, onPick = { dateEpoch = it })

			FilterChip(
				selected = paid,
				onClick = { paid = !paid },
				label = { Text(stringResource(R.string.invoice_paid)) },
			)

			Button(
				onClick = {
					if (parsedAmount != null) {
						onSave(
							number,
							parsedAmount,
							InvoiceKind.valueOf(kindName),
							LocalDate.ofEpochDay(dateEpoch),
							paid,
						)
					}
				},
				enabled = ready,
				modifier = Modifier
					.fillMaxWidth()
					.height(Dimens.primaryButtonHeight),
			) {
				Text(stringResource(R.string.save))
			}

			if (invoice != null) {
				TextButton(
					onClick = onDelete,
					modifier = Modifier.align(Alignment.CenterHorizontally),
				) {
					Text(stringResource(R.string.invoice_delete))
				}
			}
	}
}
