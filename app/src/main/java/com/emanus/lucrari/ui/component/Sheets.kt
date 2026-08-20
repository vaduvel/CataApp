package com.emanus.lucrari.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import com.emanus.lucrari.data.WorkDay
import com.emanus.lucrari.data.today
import com.emanus.lucrari.domain.Dates
import java.time.LocalDate

/**
 * Ziua lucrată (SPEC §7). Data vine gata pusă pe azi; dacă a uitat să treacă ieri,
 * schimbă cu o apăsare. Orele și blocajul sunt opționale și stau ascunse până le cere.
 * O zi cu blocaj trece lucrarea în Așteptare (SPEC §5.6).
 */
@Composable
fun DaySheet(
	day: WorkDay?,
	title: String,
	onDismiss: () -> Unit,
	onDelete: () -> Unit,
	onSave: (date: LocalDate, what: String, hours: Double?, blocked: String) -> Unit,
) {
	val dayId = day?.id
	var epochDay by rememberSaveable(dayId) { mutableStateOf((day?.date ?: today()).toEpochDay()) }
	var what by rememberSaveable(dayId) { mutableStateOf(day?.what.orEmpty()) }
	var hours by rememberSaveable(dayId) {
		mutableStateOf(day?.hours?.let { Dates.hours(it) }.orEmpty())
	}
	var showHours by rememberSaveable(dayId) { mutableStateOf(day?.hours != null) }
	var blocked by rememberSaveable(dayId) { mutableStateOf(day?.blocked.orEmpty()) }
	var showBlocked by rememberSaveable(dayId) { mutableStateOf(day?.blocked != null) }
	val date = LocalDate.ofEpochDay(epochDay)

	BrandFormSheet(title = title, onDismiss = onDismiss) {
			val todayDate = today()
			val yesterday = todayDate.minusDays(1)
			Row(
				horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
				verticalAlignment = Alignment.CenterVertically,
			) {
				FilterChip(
					selected = date == todayDate,
					onClick = { epochDay = todayDate.toEpochDay() },
					label = { Text(stringResource(R.string.day_today)) },
				)
				FilterChip(
					selected = date == yesterday,
					onClick = { epochDay = yesterday.toEpochDay() },
					label = { Text(stringResource(R.string.day_yesterday)) },
				)
				if (date != todayDate && date != yesterday) {
					Text(text = Dates.dayMonth(date), style = MaterialTheme.typography.bodyMedium)
				}
			}

			OutlinedTextField(
				value = what,
				onValueChange = { what = it },
				label = { Text(stringResource(R.string.day_what)) },
				minLines = 2,
				modifier = Modifier.fillMaxWidth(),
			)

			if (showHours) {
				OutlinedTextField(
					value = hours,
					onValueChange = { hours = it },
					label = { Text(stringResource(R.string.day_hours)) },
					singleLine = true,
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
					modifier = Modifier.fillMaxWidth(),
				)
			} else {
				TextButton(onClick = { showHours = true }) {
					Text(stringResource(R.string.day_hours_add))
				}
			}

			if (showBlocked) {
				OutlinedTextField(
					value = blocked,
					onValueChange = { blocked = it },
					label = { Text(stringResource(R.string.day_blocked)) },
					minLines = 2,
					modifier = Modifier.fillMaxWidth(),
				)
				Text(
					text = stringResource(R.string.day_blocked_note),
					style = MaterialTheme.typography.bodySmall,
				)
			} else {
				TextButton(onClick = { showBlocked = true }) {
					Text(stringResource(R.string.day_blocked_add))
				}
			}

			Button(
				onClick = {
					onSave(date, what, hours.replace(',', '.').toDoubleOrNull(), blocked)
				},
				modifier = Modifier
					.fillMaxWidth()
					.height(Dimens.primaryButtonHeight),
			) {
				Text(stringResource(R.string.save))
			}

			if (day != null) {
				TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
					Text(stringResource(R.string.day_delete))
				}
			}
	}
}

/** Foaie cu un singur câmp, folosită la adăugarea unei etape. */
@Composable
fun TextSheet(
	title: String,
	label: String,
	onDismiss: () -> Unit,
	onSave: (String) -> Unit,
) {
	var text by rememberSaveable { mutableStateOf("") }

	BrandFormSheet(title = title, onDismiss = onDismiss) {
			OutlinedTextField(
				value = text,
				onValueChange = { text = it },
				label = { Text(label) },
				singleLine = true,
				modifier = Modifier.fillMaxWidth(),
			)
			Button(
				onClick = { onSave(text) },
				enabled = text.isNotBlank(),
				modifier = Modifier
					.fillMaxWidth()
					.height(Dimens.primaryButtonHeight),
			) {
				Text(stringResource(R.string.save))
			}
	}
}
