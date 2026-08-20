package com.emanus.lucrari.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.emanus.lucrari.R
import com.emanus.lucrari.ui.theme.Dimens
import com.emanus.lucrari.data.today
import com.emanus.lucrari.domain.Dates
import com.emanus.lucrari.domain.Schedule
import java.time.LocalDate

/** `DatePicker` lucrează în milisecunde UTC, iar o zi are exact atâtea. */
private const val MILLIS_PER_DAY = 86_400_000L
private const val TEMPLATE_SEPARATOR = "\u001F"

/**
 * Lucrare nouă (SPEC §7): client, adresă, ce lucrare, câte zile și când începe. Butonul de
 * salvare e activ mereu; nimic nu blochează salvarea. Data e opțională: fără ea lucrarea
 * rămâne ofertă, cu ea intră pe Programat și apare în calendar (M8).
 *
 * Tot ce a tastat se ține cu `rememberSaveable`: dacă rotește telefonul cu formularul
 * deschis, sau dacă Android îi ia procesul cât răspunde la telefon, nu rescrie nimic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewJobSheet(
	templates: List<String>,
	onDismiss: () -> Unit,
	onSave: (
		client: String,
		address: String,
		what: String,
		templates: List<String>,
		days: Int?,
		start: LocalDate?,
	) -> Unit,
) {
	var client by rememberSaveable { mutableStateOf("") }
	var address by rememberSaveable { mutableStateOf("") }
	var what by rememberSaveable { mutableStateOf("") }
	var templateKeys by rememberSaveable { mutableStateOf("") }
	var days by rememberSaveable { mutableStateOf("") }
	var startEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
	var showPicker by rememberSaveable { mutableStateOf(false) }
	val selectedTemplates = templateKeys.split(TEMPLATE_SEPARATOR).filter { it.isNotEmpty() }
	val context = LocalContext.current
	val templateLabels = templates.associateWith { name ->
		templateLabelRes(name)?.let(context::getString) ?: name
	}

	BrandFormSheet(
		title = stringResource(R.string.new_job_title),
		onDismiss = onDismiss,
	) {
			OutlinedTextField(
				value = client,
				onValueChange = { client = it },
				label = { Text(stringResource(R.string.new_job_client)) },
				singleLine = true,
				modifier = Modifier.fillMaxWidth(),
			)
			OutlinedTextField(
				value = address,
				onValueChange = { address = it },
				label = { Text(stringResource(R.string.new_job_address)) },
				supportingText = { Text(stringResource(R.string.new_job_address_hint)) },
				singleLine = true,
				modifier = Modifier.fillMaxWidth(),
			)
			OutlinedTextField(
				value = what,
				onValueChange = { what = it },
				label = { Text(stringResource(R.string.new_job_what)) },
				singleLine = true,
				modifier = Modifier.fillMaxWidth(),
			)
			Text(
				text = stringResource(R.string.new_job_templates_hint),
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.horizontalScroll(rememberScrollState()),
				horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
			) {
				templates.forEach { name ->
					FilterChip(
						selected = name in selectedTemplates,
						onClick = {
							val previousTitle = selectedTemplates.joinToString(" + ") {
								templateLabels.getValue(it)
							}
							val picked = if (name in selectedTemplates) {
								selectedTemplates - name
							} else {
								selectedTemplates + name
							}
							templateKeys = picked.joinToString(TEMPLATE_SEPARATOR)
							// Titlul urmează selecția doar cât timp nu l-a personalizat.
							if (what.isBlank() || what == previousTitle || templates.contains(what)) {
								what = picked.joinToString(" + ") { templateLabels.getValue(it) }
							}
						},
						label = { Text(templateLabels.getValue(name)) },
					)
				}
			}
			OutlinedTextField(
				value = days,
				onValueChange = { input -> days = input.filter { it.isDigit() }.take(3) },
				label = { Text(stringResource(R.string.new_job_days)) },
				singleLine = true,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
				modifier = Modifier.fillMaxWidth(),
			)

			Text(
				text = stringResource(R.string.new_job_start),
				style = MaterialTheme.typography.bodyMedium,
			)
			val todayDate = today()
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.horizontalScroll(rememberScrollState()),
				horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
			) {
				FilterChip(
					selected = startEpoch == null,
					onClick = { startEpoch = null },
					label = { Text(stringResource(R.string.new_job_start_none)) },
				)
				FilterChip(
					selected = startEpoch == todayDate.toEpochDay(),
					onClick = { startEpoch = todayDate.toEpochDay() },
					label = { Text(stringResource(R.string.new_job_start_today)) },
				)
				FilterChip(
					selected = startEpoch == todayDate.plusDays(1).toEpochDay(),
					onClick = { startEpoch = todayDate.plusDays(1).toEpochDay() },
					label = { Text(stringResource(R.string.new_job_start_tomorrow)) },
				)
				FilterChip(
					selected = startEpoch != null &&
						startEpoch != todayDate.toEpochDay() &&
						startEpoch != todayDate.plusDays(1).toEpochDay(),
					onClick = { showPicker = true },
					label = { Text(stringResource(R.string.new_job_start_pick)) },
				)
			}
			val start = startEpoch?.let { LocalDate.ofEpochDay(it) }
			if (start != null) {
				val end = Schedule.endDate(start, days.toIntOrNull())
				Text(
					text = if (end == start) {
						stringResource(R.string.new_job_start_chosen, Dates.dayMonth(start))
					} else {
						stringResource(
							R.string.new_job_period,
							Dates.dayMonth(start),
							Dates.dayMonth(end),
						)
					},
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.primary,
				)
			}

			Button(
				onClick = {
					onSave(client, address, what, selectedTemplates, days.toIntOrNull(), start)
				},
				modifier = Modifier
					.fillMaxWidth()
					.height(Dimens.primaryButtonHeight),
			) {
				Text(stringResource(R.string.save))
			}
	}

	if (showPicker) {
		val pickerState = rememberDatePickerState(
			initialSelectedDateMillis = (startEpoch ?: today().toEpochDay()) * MILLIS_PER_DAY,
		)
		DatePickerDialog(
			onDismissRequest = { showPicker = false },
			confirmButton = {
				TextButton(
					onClick = {
						pickerState.selectedDateMillis?.let { millis ->
							startEpoch = millis / MILLIS_PER_DAY
						}
						showPicker = false
					},
				) {
					Text(stringResource(R.string.date_pick_ok))
				}
			},
			dismissButton = {
				TextButton(onClick = { showPicker = false }) {
					Text(stringResource(R.string.cancel))
				}
			},
		) {
			DatePicker(state = pickerState)
		}
	}
}
