package com.emanus.lucrari.ui.screen

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emanus.lucrari.App
import com.emanus.lucrari.R
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.today
import com.emanus.lucrari.domain.Dates
import com.emanus.lucrari.domain.MonthGrid
import com.emanus.lucrari.domain.Schedule
import com.emanus.lucrari.ui.component.StatusChip
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class CalendarViewModel(app: Application) : AndroidViewModel(app) {

	private val schedule = (app as App).scheduleRepo

	private val _month = MutableStateFlow(YearMonth.from(today()))
	val month: StateFlow<YearMonth> = _month.asStateFlow()

	@OptIn(ExperimentalCoroutinesApi::class)
	val byDay: StateFlow<Map<LocalDate, List<Job>>> = _month
		.flatMapLatest { m -> schedule.byDay(m.atDay(1), m.atEndOfMonth()) }
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

	fun shift(months: Long) {
		_month.value = _month.value.plusMonths(months)
	}
}

/**
 * Calendarul de lucru (SPEC §11, M8): ce e programat și când. O lucrare ocupă toate zilele
 * din intervalul ei, nu doar ziua de început, ca să vadă dintr-o privire când e liber.
 * Atinge o zi și vezi ce e în ea; atinge lucrarea și intri în ea.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
	onBack: () -> Unit,
	onOpenJob: (String) -> Unit,
	vm: CalendarViewModel = viewModel(),
) {
	val month by vm.month.collectAsState()
	val byDay by vm.byDay.collectAsState()
	var selectedEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
	val todayDate = today()

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.calendar_title)) },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(
							Icons.AutoMirrored.Outlined.ArrowBack,
							contentDescription = null,
						)
					}
				},
			)
		},
	) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
				.verticalScroll(rememberScrollState())
				.padding(horizontal = 16.dp)
				.padding(bottom = 24.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp),
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
			) {
				IconButton(
					onClick = {
						vm.shift(-1)
						selectedEpoch = null
					},
				) {
					Icon(
						Icons.Outlined.ChevronLeft,
						contentDescription = stringResource(R.string.calendar_prev),
					)
				}
				Text(
					text = Dates.monthYear(month.atDay(1)),
					style = MaterialTheme.typography.titleLarge,
					textAlign = TextAlign.Center,
					modifier = Modifier.weight(1f),
				)
				IconButton(
					onClick = {
						vm.shift(1)
						selectedEpoch = null
					},
				) {
					Icon(
						Icons.Outlined.ChevronRight,
						contentDescription = stringResource(R.string.calendar_next),
					)
				}
			}

			val initials = stringArrayResource(R.array.weekday_initials)
			Row(modifier = Modifier.fillMaxWidth()) {
				initials.forEach { label ->
					Text(
						text = label,
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						textAlign = TextAlign.Center,
						modifier = Modifier.weight(1f),
					)
				}
			}

			MonthGrid.cells(month).chunked(7).forEach { week ->
				Row(modifier = Modifier.fillMaxWidth()) {
					week.forEach { date ->
						if (date == null) {
							Spacer(modifier = Modifier.weight(1f))
						} else {
							DayCell(
								date = date,
								count = byDay[date].orEmpty().size,
								isToday = date == todayDate,
								isSelected = selectedEpoch == date.toEpochDay(),
								onClick = { selectedEpoch = date.toEpochDay() },
								modifier = Modifier.weight(1f),
							)
						}
					}
				}
			}

			val distinct = byDay.values.flatten().distinctBy { it.id }.size
			Text(
				text = stringResource(R.string.calendar_month_count, distinct),
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)

			val selected = selectedEpoch?.let { LocalDate.ofEpochDay(it) }
			if (selected == null) {
				Text(
					text = stringResource(R.string.calendar_hint),
					style = MaterialTheme.typography.bodyLarge,
				)
			} else {
				Text(
					text = Dates.longDay(selected),
					style = MaterialTheme.typography.titleMedium,
				)
				val jobs = byDay[selected].orEmpty()
				if (jobs.isEmpty()) {
					Text(
						text = stringResource(R.string.calendar_day_empty),
						style = MaterialTheme.typography.bodyLarge,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				} else {
					jobs.forEach { job ->
						DayJobRow(job = job, onClick = { onOpenJob(job.id) })
					}
				}
			}
		}
	}
}

/** O zi din calendar: numărul, câte lucrări o ating și dacă e ziua de azi. */
@Composable
private fun DayCell(
	date: LocalDate,
	count: Int,
	isToday: Boolean,
	isSelected: Boolean,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val shape = MaterialTheme.shapes.small
	val background = when {
		isSelected -> MaterialTheme.colorScheme.primary
		count > 0 -> MaterialTheme.colorScheme.primaryContainer
		else -> Color.Transparent
	}
	val content = when {
		isSelected -> MaterialTheme.colorScheme.onPrimary
		count > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
		else -> MaterialTheme.colorScheme.onSurface
	}
	// Ziua de azi se vede prin contur, ca să nu se bată cu fundalul zilelor ocupate.
	val outline = if (isToday && !isSelected) {
		MaterialTheme.colorScheme.primary
	} else {
		Color.Transparent
	}
	Box(
		modifier = modifier
			.padding(2.dp)
			.height(56.dp)
			.clip(shape)
			.background(background)
			.border(width = 2.dp, color = outline, shape = shape)
			.clickable { onClick() },
		contentAlignment = Alignment.Center,
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(2.dp),
		) {
			Text(
				text = date.dayOfMonth.toString(),
				style = MaterialTheme.typography.bodyLarge,
				color = content,
			)
			if (count > 0) {
				Text(
					text = "•".repeat(minOf(count, 3)),
					style = MaterialTheme.typography.bodySmall,
					color = content,
				)
			}
		}
	}
}

/** O lucrare din ziua atinsă: titlu, status, unde și intervalul ei. */
@Composable
private fun DayJobRow(job: Job, onClick: () -> Unit) {
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
					text = job.title,
					style = MaterialTheme.typography.titleMedium,
					modifier = Modifier.weight(1f),
				)
				StatusChip(status = job.status)
			}
			val where = listOfNotNull(job.street, job.city).joinToString(", ")
			if (where.isNotEmpty()) {
				Text(text = where, style = MaterialTheme.typography.bodyMedium)
			}
			val start = job.plannedStart
			if (start != null) {
				val end = Schedule.endDate(start, job.estDays)
				if (end != start) {
					Text(
						text = stringResource(
							R.string.jobs_planned_period,
							Dates.dayMonth(start),
							Dates.dayMonth(end),
						),
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.primary,
					)
				}
			}
		}
	}
}
