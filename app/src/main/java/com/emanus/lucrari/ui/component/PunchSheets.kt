package com.emanus.lucrari.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.emanus.lucrari.R
import com.emanus.lucrari.ui.theme.Dimens
import com.emanus.lucrari.data.Material
import com.emanus.lucrari.data.Reason
import com.emanus.lucrari.data.Todo
import com.emanus.lucrari.data.today
import java.time.LocalDate

/** Motivul pentru care a rămas ceva nefăcut, scris pe înțelesul lui. */
@get:StringRes
val Reason.labelRes: Int
	get() = when (this) {
		Reason.MATERIAL -> R.string.reason_material
		Reason.DECIZIE_CLIENT -> R.string.reason_decizie_client
		Reason.ALT_MESERIAS -> R.string.reason_alt_meserias
		Reason.VREMEA -> R.string.reason_vremea
		Reason.LIPSA_TIMP -> R.string.reason_lipsa_timp
		Reason.ALTUL -> R.string.reason_altul
	}

/**
 * Un rest de făcut: ce a rămas, unde, de ce și până când. Doar primul câmp e obligatoriu,
 * ca notarea de pe șantier să dureze câteva secunde (SPEC §11).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoSheet(
	todo: Todo?,
	title: String,
	onDismiss: () -> Unit,
	onDelete: () -> Unit,
	onSave: (what: String, place: String, reason: Reason?, due: LocalDate?) -> Unit,
) {
	var what by rememberSaveable(todo?.id) { mutableStateOf(todo?.what.orEmpty()) }
	var place by rememberSaveable(todo?.id) { mutableStateOf(todo?.place.orEmpty()) }
	var reasonName by rememberSaveable(todo?.id) { mutableStateOf(todo?.reason?.name) }
	var dueEpoch by rememberSaveable(todo?.id) { mutableStateOf(todo?.due?.toEpochDay()) }

	BrandFormSheet(title = title, onDismiss = onDismiss) {
			OutlinedTextField(
				value = what,
				onValueChange = { what = it },
				label = { Text(stringResource(R.string.todo_what)) },
				modifier = Modifier.fillMaxWidth(),
				minLines = 2,
			)

			OutlinedTextField(
				value = place,
				onValueChange = { place = it },
				label = { Text(stringResource(R.string.todo_place)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
			)

			Text(
				text = stringResource(R.string.todo_reason),
				style = MaterialTheme.typography.bodyMedium,
			)
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.horizontalScroll(rememberScrollState()),
				horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
			) {
				Reason.entries.forEach { reason ->
					FilterChip(
						selected = reasonName == reason.name,
						onClick = {
							reasonName = if (reasonName == reason.name) null else reason.name
						},
						label = { Text(stringResource(reason.labelRes)) },
					)
				}
			}

			Text(
				text = stringResource(R.string.todo_due),
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
					selected = dueEpoch == null,
					onClick = { dueEpoch = null },
					label = { Text(stringResource(R.string.todo_due_none)) },
				)
				FilterChip(
					selected = dueEpoch == todayDate.toEpochDay(),
					onClick = { dueEpoch = todayDate.toEpochDay() },
					label = { Text(stringResource(R.string.todo_due_today)) },
				)
				FilterChip(
					selected = dueEpoch == todayDate.plusDays(1).toEpochDay(),
					onClick = { dueEpoch = todayDate.plusDays(1).toEpochDay() },
					label = { Text(stringResource(R.string.todo_due_tomorrow)) },
				)
				FilterChip(
					selected = dueEpoch == todayDate.plusDays(7).toEpochDay(),
					onClick = { dueEpoch = todayDate.plusDays(7).toEpochDay() },
					label = { Text(stringResource(R.string.todo_due_week)) },
				)
			}

			Button(
				onClick = {
					onSave(
						what,
						place,
						reasonName?.let { Reason.valueOf(it) },
						dueEpoch?.let { LocalDate.ofEpochDay(it) },
					)
				},
				enabled = what.isNotBlank(),
				modifier = Modifier
					.fillMaxWidth()
					.height(Dimens.primaryButtonHeight),
			) {
				Text(stringResource(R.string.save))
			}

			if (todo != null) {
				TextButton(
					onClick = onDelete,
					modifier = Modifier.align(Alignment.CenterHorizontally),
				) {
					Text(stringResource(R.string.todo_delete))
				}
			}
	}
}

/** Material de cumpărat sau deja cumpărat. Cantitatea rămâne text liber: "2 saci", "vreo 10 ml". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialSheet(
	material: Material?,
	title: String,
	onDismiss: () -> Unit,
	onDelete: () -> Unit,
	onSave: (what: String, qty: String, shop: String) -> Unit,
) {
	var what by rememberSaveable(material?.id) { mutableStateOf(material?.what.orEmpty()) }
	var qty by rememberSaveable(material?.id) { mutableStateOf(material?.qty.orEmpty()) }
	var shop by rememberSaveable(material?.id) { mutableStateOf(material?.shop.orEmpty()) }

	BrandFormSheet(title = title, onDismiss = onDismiss) {
			OutlinedTextField(
				value = what,
				onValueChange = { what = it },
				label = { Text(stringResource(R.string.material_what)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
			)

			OutlinedTextField(
				value = qty,
				onValueChange = { qty = it },
				label = { Text(stringResource(R.string.material_qty)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
			)

			OutlinedTextField(
				value = shop,
				onValueChange = { shop = it },
				label = { Text(stringResource(R.string.material_shop)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
			)

			Button(
				onClick = { onSave(what, qty, shop) },
				enabled = what.isNotBlank(),
				modifier = Modifier
					.fillMaxWidth()
					.height(Dimens.primaryButtonHeight),
			) {
				Text(stringResource(R.string.save))
			}

			if (material != null) {
				TextButton(
					onClick = onDelete,
					modifier = Modifier.align(Alignment.CenterHorizontally),
				) {
					Text(stringResource(R.string.material_delete))
				}
			}
	}
}
