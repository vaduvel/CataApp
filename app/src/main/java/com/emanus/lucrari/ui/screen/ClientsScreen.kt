package com.emanus.lucrari.ui.screen

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emanus.lucrari.App
import com.emanus.lucrari.R
import com.emanus.lucrari.data.Client
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.ui.component.BrandCard
import com.emanus.lucrari.ui.component.BrandEmptyState
import com.emanus.lucrari.ui.component.BrandFormSheet
import com.emanus.lucrari.ui.component.BrandTopAppBar
import com.emanus.lucrari.ui.theme.Dimens
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ClientWithJobs(val client: Client, val jobs: List<Job>)

class ClientsViewModel(app: Application) : AndroidViewModel(app) {

	private val repo = (app as App).repo

	val clients: StateFlow<List<ClientWithJobs>> =
		combine(repo.clients(), repo.allJobs()) { clients, jobs ->
			clients.map { client ->
				ClientWithJobs(client, jobs.filter { it.clientId == client.id })
			}
		}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	fun add(name: String, phone: String, note: String) {
		viewModelScope.launch {
			repo.addClient(
				name = name,
				phone = phone.trim().ifBlank { null },
				note = note.trim().ifBlank { null },
			)
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(onBack: () -> Unit, vm: ClientsViewModel = viewModel()) {
	val clients by vm.clients.collectAsState()
	var showNew by rememberSaveable { mutableStateOf(false) }
	val unnamed = stringResource(R.string.new_job_client_unnamed)

	Scaffold(
		topBar = {
			BrandTopAppBar(
				title = stringResource(R.string.clients_title),
				onBack = onBack,
				backContentDescription = stringResource(R.string.back),
			)
		},
		floatingActionButton = {
			ExtendedFloatingActionButton(
				onClick = { showNew = true },
				icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
				text = { Text(stringResource(R.string.clients_new)) },
			)
		},
	) { padding ->
		if (clients.isEmpty()) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(padding)
					.padding(Dimens.space32),
			contentAlignment = Alignment.Center,
		) {
			BrandEmptyState(
				icon = Icons.Outlined.Add,
				title = stringResource(R.string.clients_empty),
			)
			}
		} else {
			LazyColumn(
				modifier = Modifier
					.fillMaxSize()
					.padding(padding),
				contentPadding = PaddingValues(Dimens.screenPadding),
				verticalArrangement = Arrangement.spacedBy(Dimens.cardSpacing),
			) {
				items(clients, key = { it.client.id }) { row ->
					BrandCard(modifier = Modifier.fillMaxWidth()) {
						Column(
							modifier = Modifier.padding(Dimens.cardPadding),
							verticalArrangement = Arrangement.spacedBy(Dimens.space4),
						) {
							Text(
								text = row.client.name,
								style = MaterialTheme.typography.titleMedium,
							)
							val phone = row.client.phone
							if (!phone.isNullOrBlank()) {
								Text(text = phone, style = MaterialTheme.typography.bodyMedium)
							}
							val note = row.client.note
							if (!note.isNullOrBlank()) {
								Text(text = note, style = MaterialTheme.typography.bodySmall)
							}
							Text(
								text = stringResource(R.string.client_jobs, row.jobs.size),
								style = MaterialTheme.typography.bodySmall,
							)
							row.jobs.forEach { job ->
								val line = listOfNotNull(job.title, job.street).joinToString(" — ")
								Text(text = line, style = MaterialTheme.typography.bodyMedium)
							}
						}
					}
				}
			}
		}
	}

	if (showNew) {
		NewClientSheet(
			onDismiss = { showNew = false },
			onSave = { name, phone, note ->
				vm.add(name.ifBlank { unnamed }, phone, note)
				showNew = false
			},
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewClientSheet(
	onDismiss: () -> Unit,
	onSave: (name: String, phone: String, note: String) -> Unit,
) {
	var name by rememberSaveable { mutableStateOf("") }
	var phone by rememberSaveable { mutableStateOf("") }
	var note by rememberSaveable { mutableStateOf("") }

	BrandFormSheet(
		title = stringResource(R.string.clients_new),
		onDismiss = onDismiss,
	) {
			OutlinedTextField(
				value = name,
				onValueChange = { name = it },
				label = { Text(stringResource(R.string.client_name)) },
				singleLine = true,
				modifier = Modifier.fillMaxWidth(),
			)
			OutlinedTextField(
				value = phone,
				onValueChange = { phone = it },
				label = { Text(stringResource(R.string.client_phone)) },
				singleLine = true,
				modifier = Modifier.fillMaxWidth(),
			)
			OutlinedTextField(
				value = note,
				onValueChange = { note = it },
				label = { Text(stringResource(R.string.client_note)) },
				modifier = Modifier.fillMaxWidth(),
			)
			Button(
				onClick = { onSave(name, phone, note) },
				modifier = Modifier
					.fillMaxWidth()
					.height(Dimens.primaryButtonHeight),
			) {
				Text(stringResource(R.string.save))
			}
	}
}
