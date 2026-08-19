package com.emanus.lucrari.ui.screen

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emanus.lucrari.App
import com.emanus.lucrari.R
import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.data.JobWithTotals
import com.emanus.lucrari.domain.Dates
import com.emanus.lucrari.domain.Schedule
import com.emanus.lucrari.domain.Templates
import com.emanus.lucrari.ui.component.NewJobSheet
import com.emanus.lucrari.ui.component.StatusChip
import com.emanus.lucrari.ui.component.labelRes
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JobsViewModel(app: Application) : AndroidViewModel(app) {

	private val repo = (app as App).repo
	private val schedule = (app as App).scheduleRepo

	private val _query = MutableStateFlow("")
	val query: StateFlow<String> = _query.asStateFlow()

	private val _filter = MutableStateFlow<JobStatus?>(null)
	val filter: StateFlow<JobStatus?> = _filter.asStateFlow()

	/**
	 * Rândurile citite ultima dată din bază, la momente clare: la intrarea pe ecran, la
	 * scris în căutare și după creare.
	 *
	 * Am ajuns la citirea la cerere crezând că abonamentul lung la Room pierdea lucrarea
	 * nouă. Nu o pierdea: rândul era în rezultat, dar lista îl desena deasupra marginii de
	 * sus (vezi comentariul de la LazyColumn). Citirea scurtă rămâne fiindcă e simplă și
	 * de ajuns pentru câteva zeci de rânduri; dacă vreodată lista trebuie să se miște
	 * singură, se poate întoarce la Flow fără altă schimbare.
	 */
	private val _rows = MutableStateFlow<List<JobWithTotals>>(emptyList())

	/** Filtrul de status se aplică în memorie: apăsarea unui coș nu mai atinge baza. */
	val jobs: StateFlow<List<JobWithTotals>> =
		combine(_rows, _filter) { rows, status ->
			if (status == null) rows else rows.filter { it.job.status == status }
		}.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

	/** Recitește lista. Câteva zeci de rânduri din baza locală: nu se simte. */
	fun refresh() {
		viewModelScope.launch { _rows.value = repo.boardOnce(_query.value) }
	}

	fun setQuery(value: String) {
		_query.value = value
		refresh()
	}

	fun setFilter(value: JobStatus?) {
		_filter.value = value
	}

	fun create(
		client: String,
		address: String,
		what: String,
		template: String?,
		days: Int?,
		start: LocalDate?,
		onCreated: (String) -> Unit,
	) {
		viewModelScope.launch {
			val id = repo.createJob(
				clientName = client,
				address = address,
				title = what,
				type = template,
				estDays = days,
			)
			// Data vine după creare: pune și statusul potrivit (Programat pentru viitor).
			if (start != null) schedule.setPlannedStart(id, start)
			// Citim din nou aici, înainte să plecăm spre detaliu: la întoarcere lista o are.
			_rows.value = repo.boardOnce(_query.value)
			onCreated(id)
		}
	}
}

@Composable
fun JobsScreen(
	onOpenJob: (String) -> Unit,
	onOpenCalendar: () -> Unit,
	vm: JobsViewModel = viewModel(),
) {
	val jobs by vm.jobs.collectAsState()
	val query by vm.query.collectAsState()
	val filter by vm.filter.collectAsState()
	val listState = rememberLazyListState()
	var showNew by rememberSaveable { mutableStateOf(false) }
	val unnamedClient = stringResource(R.string.new_job_client_unnamed)
	val untitled = stringResource(R.string.new_job_untitled)

	// La fiecare intrare pe ecran, inclusiv la întoarcerea din detaliul unei lucrări.
	LaunchedEffect(Unit) { vm.refresh() }

	// LazyColumn ține poziția după cheia primului rând vizibil, ca să nu-ți sară conținutul
	// sub deget. Când o lucrare nouă intră în capul listei, poziția urmează cheia veche la
	// noul ei index, iar rândul nou rămâne desenat deasupra marginii de sus: lista pare că
	// nu l-a primit, deși îl are. Aici e invers decât vrea el: a salvat ceva și trebuie să
	// vadă ce a salvat, deci ne întoarcem în cap când se schimbă primul rând.
	val topRowId = jobs.firstOrNull()?.job?.id
	LaunchedEffect(topRowId) {
		if (topRowId != null) listState.scrollToItem(0)
	}

	Scaffold(
		floatingActionButton = {
			ExtendedFloatingActionButton(
				onClick = { showNew = true },
				icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
				text = { Text(stringResource(R.string.jobs_new)) },
			)
		},
	) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding),
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp, vertical = 8.dp),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				OutlinedTextField(
					value = query,
					onValueChange = { vm.setQuery(it) },
					label = { Text(stringResource(R.string.jobs_search_hint)) },
					leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
					singleLine = true,
					modifier = Modifier.weight(1f),
				)
				IconButton(
					onClick = onOpenCalendar,
					modifier = Modifier.size(56.dp),
				) {
					Icon(
						Icons.Outlined.CalendarMonth,
						contentDescription = stringResource(R.string.calendar_open),
					)
				}
			}
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.horizontalScroll(rememberScrollState())
					.padding(horizontal = 16.dp),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
			) {
				FilterChip(
					selected = filter == null,
					onClick = { vm.setFilter(null) },
					label = { Text(stringResource(R.string.filter_all)) },
				)
				JobStatus.entries.forEach { status ->
					FilterChip(
						selected = filter == status,
						onClick = { vm.setFilter(if (filter == status) null else status) },
						label = { Text(stringResource(status.labelRes)) },
					)
				}
			}
			if (jobs.isEmpty()) {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.padding(32.dp),
					contentAlignment = Alignment.Center,
				) {
					val empty = if (query.isBlank() && filter == null) {
						R.string.jobs_empty
					} else {
						R.string.jobs_empty_search
					}
					Text(text = stringResource(empty), style = MaterialTheme.typography.bodyLarge)
				}
			} else {
				LazyColumn(
					state = listState,
					contentPadding = PaddingValues(16.dp),
					verticalArrangement = Arrangement.spacedBy(12.dp),
				) {
					items(jobs, key = { it.job.id }) { row ->
						JobCard(row = row, onClick = { onOpenJob(row.job.id) })
					}
				}
			}
		}
	}

	if (showNew) {
		NewJobSheet(
			templates = Templates.names,
			onDismiss = { showNew = false },
			onSave = { client, address, what, template, days, start ->
				vm.create(
					client = client.ifBlank { unnamedClient },
					address = address,
					what = what.ifBlank { template ?: untitled },
					template = template,
					days = days,
					start = start,
				) { id ->
					showNew = false
					onOpenJob(id)
				}
			},
		)
	}
}

@Composable
private fun JobCard(row: JobWithTotals, onClick: () -> Unit) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.clickable { onClick() },
	) {
		Column(
			modifier = Modifier.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(6.dp),
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Text(
					text = row.job.title,
					style = MaterialTheme.typography.titleMedium,
					modifier = Modifier.weight(1f),
				)
				StatusChip(status = row.job.status)
			}
			val where = listOfNotNull(row.clientName, row.job.street, row.job.city)
			Text(text = where.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
			val planned = row.job.plannedStart
			if (planned != null && row.job.status == JobStatus.PROGRAMAT) {
				val end = Schedule.endDate(planned, row.job.estDays)
				Text(
					text = if (end == planned) {
						stringResource(R.string.jobs_planned, Dates.dayMonth(planned))
					} else {
						stringResource(
							R.string.jobs_planned_period,
							Dates.dayMonth(planned),
							Dates.dayMonth(end),
						)
					},
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.primary,
				)
			}
			if (row.stageCount > 0) {
				LinearProgressIndicator(
					progress = { row.stagesDone.toFloat() / row.stageCount },
					modifier = Modifier.fillMaxWidth(),
				)
				Text(
					text = stringResource(R.string.jobs_stages, row.stagesDone, row.stageCount),
					style = MaterialTheme.typography.bodySmall,
				)
			}
			if (row.workedDays > 0) {
				Text(
					text = stringResource(R.string.jobs_days, row.workedDays),
					style = MaterialTheme.typography.bodySmall,
				)
			}
			if (row.openTodos > 0) {
				Text(
					text = stringResource(R.string.jobs_open_todos, row.openTodos),
					style = MaterialTheme.typography.bodySmall,
				)
			}
		}
	}
}
