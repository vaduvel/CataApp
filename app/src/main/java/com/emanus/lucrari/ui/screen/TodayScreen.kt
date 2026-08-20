package com.emanus.lucrari.ui.screen

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emanus.lucrari.App
import com.emanus.lucrari.R
import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.data.JobToday
import com.emanus.lucrari.data.today
import com.emanus.lucrari.domain.Dates
import com.emanus.lucrari.domain.Progress
import com.emanus.lucrari.ui.component.StatusChip
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodayViewModel(app: Application) : AndroidViewModel(app) {

	private val repo = (app as App).repo

	private val _date = MutableStateFlow(today())
	val date: StateFlow<LocalDate> = _date.asStateFlow()

	@OptIn(ExperimentalCoroutinesApi::class)
	val jobs: StateFlow<List<JobToday>> = _date
		.flatMapLatest { day -> repo.todayBoard(day) }
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	/** Dacă telefonul a stat deschis peste noapte, ecranul trebuie să treacă singur la ziua nouă. */
	fun refreshDate() {
		val current = today()
		if (_date.value != current) _date.value = current
	}

	fun logToday(jobId: String, onResult: (Boolean) -> Unit) {
		viewModelScope.launch { onResult(repo.logDay(jobId, _date.value)) }
	}
}

/**
 * Ecranul de pornire (SPEC §6): lucrările vii și un buton mare pe fiecare. Aplicația se
 * deschide aici, deci trecerea unei zile înseamnă o singură apăsare.
 */
@Composable
fun TodayScreen(onOpenJob: (String) -> Unit, vm: TodayViewModel = viewModel()) {
	val jobs by vm.jobs.collectAsState()
	val date by vm.date.collectAsState()
	val snackbarHost = remember { SnackbarHostState() }
	val scope = rememberCoroutineScope()
	val loggedMessage = stringResource(R.string.today_logged_snack)
	val alreadyMessage = stringResource(R.string.today_already_snack)

	LaunchedEffect(Unit) { vm.refreshDate() }

	Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding),
		) {
			Text(
				text = Dates.longDay(date),
				style = MaterialTheme.typography.titleLarge,
				modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
			)

			if (jobs.isEmpty()) {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.padding(32.dp),
					contentAlignment = Alignment.Center,
				) {
					Text(
						text = stringResource(R.string.today_empty),
						style = MaterialTheme.typography.bodyLarge,
					)
				}
			} else {
				LazyColumn(
					contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
					verticalArrangement = Arrangement.spacedBy(12.dp),
				) {
					items(jobs, key = { row -> row.job.id }) { row ->
						TodayCard(
							row = row,
							date = date,
							onOpen = { onOpenJob(row.job.id) },
							onLog = {
								vm.logToday(row.job.id) { saved ->
									scope.launch {
										snackbarHost.showSnackbar(
											if (saved) loggedMessage else alreadyMessage,
										)
									}
								}
							},
						)
					}
				}
			}
		}
	}
}

@Composable
private fun TodayCard(row: JobToday, date: LocalDate, onOpen: () -> Unit, onLog: () -> Unit) {
	Card(modifier = Modifier.fillMaxWidth()) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.clickable { onOpen() }
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp),
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Text(
					text = row.job.title,
					style = MaterialTheme.typography.titleMedium,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.weight(1f),
				)
				StatusChip(row.job.status)
			}

			val where = listOfNotNull(row.clientName, row.job.street).joinToString(", ")
			if (where.isNotEmpty()) {
				Text(text = where, style = MaterialTheme.typography.bodyMedium)
			}

			// O lucrare programată ajunge aici în ziua în care ar trebui să înceapă, deci scrie
			// din ce zi așteaptă: altfel se vede doar eticheta Programat, care nu spune când.
			val start = row.job.plannedStart
			if (row.job.status == JobStatus.PROGRAMAT && start != null) {
				Text(
					text = if (start == date) {
						stringResource(R.string.today_starts)
					} else {
						stringResource(R.string.today_starts_late, Dates.dayMonth(start))
					},
					style = MaterialTheme.typography.bodyMedium,
				)
			}

			if (row.stageCount > 0) {
				LinearProgressIndicator(
					progress = { Progress.ofStages(row.stagesDone, row.stageCount) },
					modifier = Modifier.fillMaxWidth(),
				)
				Text(
					text = stringResource(R.string.jobs_stages, row.stagesDone, row.stageCount),
					style = MaterialTheme.typography.bodySmall,
				)
			}

			val next = row.nextStage
			if (!next.isNullOrBlank()) {
				Text(
					text = stringResource(R.string.today_next, next),
					style = MaterialTheme.typography.bodySmall,
				)
			}
		}

		// Butonul stă în afara zonei care deschide lucrarea, ca să nu se apese din greșeală.
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
		) {
			if (row.loggedToday > 0) {
				OutlinedButton(
					onClick = onOpen,
					modifier = Modifier
						.fillMaxWidth()
						.height(56.dp),
				) {
					Icon(imageVector = Icons.Outlined.Check, contentDescription = null)
					Spacer(modifier = Modifier.width(8.dp))
					Text(stringResource(R.string.today_logged_button))
				}
			} else {
				Button(
					onClick = onLog,
					modifier = Modifier
						.fillMaxWidth()
						.height(56.dp),
				) {
					Text(
						text = stringResource(R.string.today_log),
						style = MaterialTheme.typography.titleMedium,
					)
				}
			}
		}
	}
}
