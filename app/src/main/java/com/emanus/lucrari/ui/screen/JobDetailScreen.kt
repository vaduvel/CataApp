package com.emanus.lucrari.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.emanus.lucrari.App
import com.emanus.lucrari.R
import com.emanus.lucrari.data.Client
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.data.Stage
import com.emanus.lucrari.ui.component.labelRes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JobDetailViewModel(app: App, jobId: String) : ViewModel() {

	private val repo = app.repo

	val job: StateFlow<Job?> = repo.job(jobId)
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

	val stages: StateFlow<List<Stage>> = repo.stages(jobId)
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	@OptIn(ExperimentalCoroutinesApi::class)
	val client: StateFlow<Client?> = job
		.flatMapLatest { current -> if (current == null) flowOf(null) else repo.client(current.clientId) }
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

	fun setStatus(status: JobStatus) {
		val current = job.value ?: return
		viewModelScope.launch { repo.setStatus(current, status) }
	}

	fun delete(onDone: () -> Unit) {
		val current = job.value ?: return
		viewModelScope.launch {
			repo.deleteJob(current)
			onDone()
		}
	}

	companion object {
		fun factory(jobId: String): ViewModelProvider.Factory = viewModelFactory {
			initializer {
				val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as App
				JobDetailViewModel(app, jobId)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(jobId: String, onBack: () -> Unit) {
	val vm: JobDetailViewModel = viewModel(factory = JobDetailViewModel.factory(jobId))
	val jobState by vm.job.collectAsState()
	val client by vm.client.collectAsState()
	val stages by vm.stages.collectAsState()
	val context = LocalContext.current
	var confirmDelete by remember { mutableStateOf(false) }
	val job = jobState

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(text = job?.title.orEmpty(), maxLines = 1) },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(
							Icons.AutoMirrored.Outlined.ArrowBack,
							contentDescription = stringResource(R.string.back),
						)
					}
				},
				actions = {
					IconButton(onClick = { confirmDelete = true }) {
						Icon(
							Icons.Outlined.Delete,
							contentDescription = stringResource(R.string.job_delete),
						)
					}
				},
			)
		},
	) { padding ->
		if (job == null) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(padding),
				contentAlignment = Alignment.Center,
			) {
				CircularProgressIndicator()
			}
		} else {
			val where = listOfNotNull(job.street, job.city).joinToString(", ")
			LazyColumn(
				modifier = Modifier
					.fillMaxSize()
					.padding(padding),
				contentPadding = PaddingValues(16.dp),
				verticalArrangement = Arrangement.spacedBy(12.dp),
			) {
				item {
					Card(modifier = Modifier.fillMaxWidth()) {
						Column(
							modifier = Modifier.padding(16.dp),
							verticalArrangement = Arrangement.spacedBy(8.dp),
						) {
							Text(
								text = client?.name.orEmpty(),
								style = MaterialTheme.typography.titleMedium,
							)
							if (where.isNotEmpty()) {
								Text(text = where, style = MaterialTheme.typography.bodyLarge)
							}
							val estimate = job.estDays
							if (estimate != null) {
								Text(
									text = stringResource(R.string.job_estimate, estimate),
									style = MaterialTheme.typography.bodyMedium,
								)
							}
							Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
								val phone = client?.phone
								if (!phone.isNullOrBlank()) {
									OutlinedButton(
										onClick = {
											val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone))
											context.startActivity(intent)
										},
									) {
										Icon(Icons.Outlined.Call, contentDescription = null)
										Spacer(Modifier.width(8.dp))
										Text(stringResource(R.string.job_call))
									}
								}
								if (where.isNotEmpty()) {
									OutlinedButton(
										onClick = {
											val uri = Uri.parse("geo:0,0?q=" + Uri.encode(where))
											context.startActivity(Intent(Intent.ACTION_VIEW, uri))
										},
									) {
										Icon(Icons.Outlined.Map, contentDescription = null)
										Spacer(Modifier.width(8.dp))
										Text(stringResource(R.string.job_map))
									}
								}
							}
						}
					}
				}
				item {
					Text(
						text = stringResource(R.string.job_status),
						style = MaterialTheme.typography.titleMedium,
					)
				}
				item {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.horizontalScroll(rememberScrollState()),
						horizontalArrangement = Arrangement.spacedBy(8.dp),
					) {
						JobStatus.entries.forEach { status ->
							FilterChip(
								selected = job.status == status,
								onClick = { vm.setStatus(status) },
								label = { Text(stringResource(status.labelRes)) },
							)
						}
					}
				}
				item {
					Text(
						text = stringResource(R.string.job_stages_title),
						style = MaterialTheme.typography.titleMedium,
					)
				}
				if (stages.isEmpty()) {
					item {
						Text(
							text = stringResource(R.string.job_no_stages),
							style = MaterialTheme.typography.bodyMedium,
						)
					}
				} else {
					items(stages, key = { it.id }) { stage ->
						Row(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.spacedBy(12.dp),
							verticalAlignment = Alignment.CenterVertically,
						) {
							val icon = if (stage.done) {
								Icons.Outlined.Check
							} else {
								Icons.Outlined.RadioButtonUnchecked
							}
							Icon(icon, contentDescription = null)
							Text(text = stage.name, style = MaterialTheme.typography.bodyLarge)
						}
					}
				}
			}
		}
	}

	if (confirmDelete) {
		AlertDialog(
			onDismissRequest = { confirmDelete = false },
			title = { Text(stringResource(R.string.job_delete)) },
			text = { Text(stringResource(R.string.job_delete_confirm)) },
			confirmButton = {
				TextButton(
					onClick = {
						confirmDelete = false
						vm.delete(onBack)
					},
				) {
					Text(stringResource(R.string.delete))
				}
			},
			dismissButton = {
				TextButton(onClick = { confirmDelete = false }) {
					Text(stringResource(R.string.cancel))
				}
			},
		)
	}
}
