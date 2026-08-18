package com.emanus.lucrari.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emanus.lucrari.R

/**
 * Lucrare nouă în 4 câmpuri (SPEC §7): client, adresă, ce lucrare, câte zile.
 * Butonul de salvare e activ mereu; nimic nu blochează salvarea.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewJobSheet(
	templates: List<String>,
	onDismiss: () -> Unit,
	onSave: (client: String, address: String, what: String, template: String?, days: Int?) -> Unit,
) {
	val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
	var client by remember { mutableStateOf("") }
	var address by remember { mutableStateOf("") }
	var what by remember { mutableStateOf("") }
	var template by remember { mutableStateOf<String?>(null) }
	var days by remember { mutableStateOf("") }

	ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 20.dp)
				.padding(bottom = 32.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			Text(
				text = stringResource(R.string.new_job_title),
				style = MaterialTheme.typography.titleLarge,
			)
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
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.horizontalScroll(rememberScrollState()),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
			) {
				templates.forEach { name ->
					FilterChip(
						selected = template == name,
						onClick = {
							val picked = if (template == name) null else name
							template = picked
							// Titlul urmează șablonul cât timp nu a scris el altceva.
							if (what.isBlank() || templates.contains(what)) {
								what = picked.orEmpty()
							}
						},
						label = { Text(name) },
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
			Button(
				onClick = { onSave(client, address, what, template, days.toIntOrNull()) },
				modifier = Modifier
					.fillMaxWidth()
					.height(56.dp),
			) {
				Text(stringResource(R.string.save))
			}
		}
	}
}
