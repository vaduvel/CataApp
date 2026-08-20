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
import com.emanus.lucrari.data.Extra
import com.emanus.lucrari.data.Measure
import com.emanus.lucrari.data.MeasureUnit
import com.emanus.lucrari.data.today
import com.emanus.lucrari.domain.Dates
import com.emanus.lucrari.domain.Measures
import com.emanus.lucrari.domain.Money
import java.time.LocalDate

/**
 * Data unei măsurători sau a unui extra: de obicei azi, uneori ieri, iar la editare rămâne
 * data scrisă atunci, arătată pe un al treilea jeton.
 */
@Composable
private fun DateChips(epochDay: Long, onPick: (Long) -> Unit) {
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
 * O măsurătoare luată pe teren (SPEC §4): unde, ce, cât și în ce unitate. Prețul pe unitate
 * se pune doar când lucrarea se plătește la măsură; altfel rămâne o cifră pentru textul
 * facturii. Cât face rândul se vede imediat sub câmpuri, ca să prindă o cifră greșită pe loc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasureSheet(
	measure: Measure?,
	title: String,
	onDismiss: () -> Unit,
	onDelete: () -> Unit,
	onSave: (
		place: String,
		work: String,
		qty: Double,
		unit: MeasureUnit,
		unitPriceCents: Long?,
		date: LocalDate,
	) -> Unit,
) {
	var place by rememberSaveable(measure?.id) { mutableStateOf(measure?.place.orEmpty()) }
	var work by rememberSaveable(measure?.id) { mutableStateOf(measure?.work.orEmpty()) }
	var qty by rememberSaveable(measure?.id) {
		mutableStateOf(measure?.qty?.let { Measures.formatQty(it) }.orEmpty())
	}
	var unitName by rememberSaveable(measure?.id) {
		mutableStateOf(measure?.unit?.name ?: MeasureUnit.M2.name)
	}
	var price by rememberSaveable(measure?.id) {
		mutableStateOf(measure?.unitPriceCents?.let { Money.plain(it) }.orEmpty())
	}
	var dateEpoch by rememberSaveable(measure?.id) {
		mutableStateOf((measure?.date ?: today()).toEpochDay())
	}

	val parsedQty = Measures.parseQty(qty)
	val parsedPrice = if (price.isBlank()) null else Money.parse(price)
	val lineCents = if (parsedQty == null) null else Measures.lineCents(parsedQty, parsedPrice)
	val ready = place.isNotBlank() && parsedQty != null && (price.isBlank() || parsedPrice != null)

	BrandFormSheet(title = title, onDismiss = onDismiss) {
			OutlinedTextField(
				value = place,
				onValueChange = { place = it },
				label = { Text(stringResource(R.string.measure_place)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
			)

			OutlinedTextField(
				value = work,
				onValueChange = { work = it },
				label = { Text(stringResource(R.string.measure_work)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
			)

			OutlinedTextField(
				value = qty,
				onValueChange = { qty = it },
				label = { Text(stringResource(R.string.measure_qty)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
			)

			Text(
				text = stringResource(R.string.measure_unit),
				style = MaterialTheme.typography.bodyMedium,
			)
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.horizontalScroll(rememberScrollState()),
				horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
			) {
				MeasureUnit.entries.forEach { unit ->
					FilterChip(
						selected = unitName == unit.name,
						onClick = { unitName = unit.name },
						label = { Text(unit.label) },
					)
				}
			}

			OutlinedTextField(
				value = price,
				onValueChange = { price = it },
				label = { Text(stringResource(R.string.measure_price)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
			)

			if (lineCents != null) {
				Text(
					text = stringResource(R.string.measure_total_line, Money.format(lineCents)),
					style = MaterialTheme.typography.titleMedium,
				)
			}

			DateChips(epochDay = dateEpoch, onPick = { dateEpoch = it })

			Button(
				onClick = {
					if (parsedQty != null) {
						onSave(
							place,
							work,
							parsedQty,
							MeasureUnit.valueOf(unitName),
							parsedPrice,
							LocalDate.ofEpochDay(dateEpoch),
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

			if (measure != null) {
				TextButton(
					onClick = onDelete,
					modifier = Modifier.align(Alignment.CenterHorizontally),
				) {
					Text(stringResource(R.string.measure_delete))
				}
			}
	}
}

/**
 * Un extra cerut de client (SPEC §4). Bifa de înțelegere și rândul de dovadă sunt partea
 * importantă: la sfârșit, ele fac diferența dintre bani încasați și discuție. Un extra făcut
 * din bunăvoință se lasă nebifat la se pune pe factură.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraSheet(
	extra: Extra?,
	title: String,
	onDismiss: () -> Unit,
	onDelete: () -> Unit,
	onSave: (
		what: String,
		priceCents: Long,
		accepted: Boolean,
		proof: String,
		billable: Boolean,
		date: LocalDate,
	) -> Unit,
) {
	var what by rememberSaveable(extra?.id) { mutableStateOf(extra?.what.orEmpty()) }
	var price by rememberSaveable(extra?.id) {
		mutableStateOf(extra?.priceCents?.let { Money.plain(it) }.orEmpty())
	}
	var accepted by rememberSaveable(extra?.id) { mutableStateOf(extra?.accepted ?: false) }
	var proof by rememberSaveable(extra?.id) { mutableStateOf(extra?.proof.orEmpty()) }
	var billable by rememberSaveable(extra?.id) { mutableStateOf(extra?.billable ?: true) }
	var dateEpoch by rememberSaveable(extra?.id) {
		mutableStateOf((extra?.date ?: today()).toEpochDay())
	}

	val parsedPrice = if (price.isBlank()) null else Money.parse(price)
	val ready = what.isNotBlank() && (price.isBlank() || parsedPrice != null)

	BrandFormSheet(title = title, onDismiss = onDismiss) {
			OutlinedTextField(
				value = what,
				onValueChange = { what = it },
				label = { Text(stringResource(R.string.extra_what)) },
				modifier = Modifier.fillMaxWidth(),
				minLines = 2,
			)

			OutlinedTextField(
				value = price,
				onValueChange = { price = it },
				label = { Text(stringResource(R.string.extra_price)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
			)

			Row(
				modifier = Modifier
					.fillMaxWidth()
					.horizontalScroll(rememberScrollState()),
				horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
			) {
				FilterChip(
					selected = accepted,
					onClick = { accepted = !accepted },
					label = { Text(stringResource(R.string.extra_accepted)) },
				)
				FilterChip(
					selected = billable,
					onClick = { billable = !billable },
					label = { Text(stringResource(R.string.extra_billable)) },
				)
			}

			OutlinedTextField(
				value = proof,
				onValueChange = { proof = it },
				label = { Text(stringResource(R.string.extra_proof)) },
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
			)

			DateChips(epochDay = dateEpoch, onPick = { dateEpoch = it })

			Button(
				onClick = {
					onSave(
						what,
						parsedPrice ?: 0L,
						accepted,
						proof,
						billable,
						LocalDate.ofEpochDay(dateEpoch),
					)
				},
				enabled = ready,
				modifier = Modifier
					.fillMaxWidth()
					.height(Dimens.primaryButtonHeight),
			) {
				Text(stringResource(R.string.save))
			}

			if (extra != null) {
				TextButton(
					onClick = onDelete,
					modifier = Modifier.align(Alignment.CenterHorizontally),
				) {
					Text(stringResource(R.string.extra_delete))
				}
			}
	}
}
