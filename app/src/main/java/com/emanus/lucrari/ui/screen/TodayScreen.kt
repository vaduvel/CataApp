package com.emanus.lucrari.ui.screen

import android.app.Application
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
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import com.emanus.lucrari.ui.component.BrandCard
import com.emanus.lucrari.ui.component.BrandEmptyState
import com.emanus.lucrari.ui.component.BrandPageHeader
import com.emanus.lucrari.ui.component.BrandProgress
import com.emanus.lucrari.ui.component.color
import com.emanus.lucrari.ui.component.localizedStageLabel
import com.emanus.lucrari.ui.theme.Dimens
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
			BrandPageHeader(title = Dates.longDay(date))

			if (jobs.isEmpty()) {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.padding(Dimens.space32),
					contentAlignment = Alignment.Center,
				) {
					BrandEmptyState(
						icon = Icons.Outlined.Construction,
						title = stringResource(R.string.today_empty),
					)
				}
			} else {
				LazyColumn(
					contentPadding = PaddingValues(
						start = Dimens.screenPadding,
						end = Dimens.screenPadding,
						bottom = Dimens.listBottomSpace,
					),
					verticalArrangement = Arrangement.spacedBy(Dimens.cardSpacing),
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
	BrandCard(
		modifier = Modifier.fillMaxWidth(),
		onClick = onOpen,
		accent = row.job.status.color,
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(Dimens.cardPadding),
			verticalArrangement = Arrangement.spacedBy(Dimens.space8),
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
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
				Text(
					text = where,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
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
				BrandProgress(
					progress = { Progress.ofStages(row.stagesDone, row.stageCount) },
				)
				Text(
					text = stringResource(R.string.jobs_stages, row.stagesDone, row.stageCount),
					style = MaterialTheme.typography.bodySmall,
				)
			}

			val next = row.nextStage
			if (!next.isNullOrBlank()) {
				Text(
					text = stringResource(R.string.today_next, localizedStageLabel(next)),
					style = MaterialTheme.typography.bodySmall,
				)
			}
		}

		// Butonul stă în afara zonei care deschide lucrarea, ca să nu se apese din greșeală.
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.padding(
					start = Dimens.cardPadding,
					end = Dimens.cardPadding,
					bottom = Dimens.cardPadding,
				),
		) {
			if (row.loggedToday > 0) {
				OutlinedButton(
					onClick = onOpen,
					modifier = Modifier
						.fillMaxWidth()
						.height(Dimens.primaryButtonHeight),
				) {
					Icon(imageVector = Icons.Outlined.Check, contentDescription = null)
					Spacer(modifier = Modifier.width(Dimens.space8))
					Text(stringResource(R.string.today_logged_button))
				}
			} else {
				Button(
					onClick = onLog,
					modifier = Modifier
						.fillMaxWidth()
						.height(Dimens.primaryButtonHeight),
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
