package com.emanus.lucrari.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.emanus.lucrari.data.Material
import com.emanus.lucrari.data.Reason
import com.emanus.lucrari.data.Stage
import com.emanus.lucrari.data.Todo
import com.emanus.lucrari.data.WorkDay
import com.emanus.lucrari.domain.Dates
import com.emanus.lucrari.domain.Progress
import com.emanus.lucrari.domain.Rules
import com.emanus.lucrari.domain.Templates
import com.emanus.lucrari.ui.component.DaySheet
import com.emanus.lucrari.ui.component.MaterialSheet
import com.emanus.lucrari.ui.component.TextSheet
import com.emanus.lucrari.ui.component.TodoSheet
import com.emanus.lucrari.ui.component.labelRes
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JobDetailViewModel(app: App, private val jobId: String) : ViewModel() {

	private val repo = app.repo

	val job: StateFlow<Job?> = repo.job(jobId)
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

	val stages: StateFlow<List<Stage>> = repo.stages(jobId)
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	val days: StateFlow<List<WorkDay>> = repo.days(jobId)
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	val todos: StateFlow<List<Todo>> = repo.todos(jobId)
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	val materials: StateFlow<List<Material>> = repo.materials(jobId)
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

	fun toggleStage(stage: Stage) {
		viewModelScope.launch { repo.toggleStage(stage) }
	}

	fun addStage(name: String) {
		viewModelScope.launch { repo.addStage(jobId, name) }
	}

	fun deleteStage(stage: Stage) {
		viewModelScope.launch { repo.deleteStage(stage) }
	}

	fun applyTemplate(type: String) {
		viewModelScope.launch { repo.applyTemplate(jobId, type) }
	}

	fun logToday(onResult: (Boolean) -> Unit) {
		viewModelScope.launch { onResult(repo.logDay(jobId)) }
	}

	/** O singură zi per dată: dacă ziua există deja, se completează, nu se dublează. */
	fun saveDay(existing: WorkDay?, date: LocalDate, what: String, hours: Double?, blocked: String) {
		viewModelScope.launch {
			val target = existing ?: days.value.firstOrNull { it.date == date }
			val day = target?.copy(date = date, what = what, hours = hours, blocked = blocked)
				?: WorkDay(
					jobId = jobId,
					date = date,
					what = what,
					hours = hours,
					blocked = blocked,
				)
			repo.saveDay(day)
		}
	}

	fun deleteDay(day: WorkDay) {
		viewModelScope.launch { repo.deleteDay(day) }
	}

	fun addTodo(what: String, place: String, reason: Reason?, due: LocalDate?) {
		viewModelScope.launch { repo.addTodo(jobId, what, place, reason, due) }
	}

	fun saveTodo(todo: Todo, what: String, place: String, reason: Reason?, due: LocalDate?) {
		viewModelScope.launch {
			repo.saveTodo(todo.copy(what = what, place = place, reason = reason, due = due))
		}
	}

	fun toggleTodo(todo: Todo) {
		viewModelScope.launch { repo.toggleTodo(todo) }
	}

	fun deleteTodo(todo: Todo) {
		viewModelScope.launch { repo.deleteTodo(todo) }
	}

	fun addMaterial(what: String, qty: String, shop: String) {
		viewModelScope.launch { repo.addMaterial(jobId, what, qty, shop) }
	}

	fun toggleMaterial(material: Material) {
		viewModelScope.launch { repo.toggleMaterial(material) }
	}

	fun deleteMaterial(material: Material) {
		viewModelScope.launch { repo.deleteMaterial(material) }
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
	val days by vm.days.collectAsState()
	val todos by vm.todos.collectAsState()
	val materials by vm.materials.collectAsState()
	val context = LocalContext.current
	val snackbarHost = remember { SnackbarHostState() }
	val scope = rememberCoroutineScope()
	val loggedMessage = stringResource(R.string.today_logged_snack)
	val alreadyMessage = stringResource(R.string.today_already_snack)
	var confirmDelete by remember { mutableStateOf(false) }
	var confirmDone by remember { mutableStateOf(false) }
	var showStageSheet by rememberSaveable { mutableStateOf(false) }
	var showNewDay by rememberSaveable { mutableStateOf(false) }
	var editingDayId by rememberSaveable { mutableStateOf<String?>(null) }
	var showNewTodo by rememberSaveable { mutableStateOf(false) }
	var editingTodoId by rememberSaveable { mutableStateOf<String?>(null) }
	var showNewMaterial by rememberSaveable { mutableStateOf(false) }
	val job = jobState
	val editingDay = days.firstOrNull { it.id == editingDayId }
	val editingTodo = todos.firstOrNull { it.id == editingTodoId }
	val stagesDone = stages.count { it.done }
	val openTodos = todos.count { !it.done }

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
		snackbarHost = { SnackbarHost(snackbarHost) },
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
							if (stages.isNotEmpty()) {
								LinearProgressIndicator(
									progress = { Progress.ofStages(stagesDone, stages.size) },
									modifier = Modifier.fillMaxWidth(),
								)
								Text(
									text = stringResource(R.string.jobs_stages, stagesDone, stages.size),
									style = MaterialTheme.typography.bodyMedium,
								)
							}
							val estimate = job.estDays
							if (estimate != null) {
								Text(
									text = stringResource(R.string.job_days_estimate, estimate, days.size),
									style = MaterialTheme.typography.bodyMedium,
								)
								val over = Progress.daysVsEstimate(estimate, days.size)
								if (over != null && over > 0) {
									Text(
										text = stringResource(R.string.job_days_over, over),
										style = MaterialTheme.typography.bodyMedium,
										color = MaterialTheme.colorScheme.error,
									)
								}
							} else if (days.isNotEmpty()) {
								Text(
									text = stringResource(R.string.jobs_days, days.size),
									style = MaterialTheme.typography.bodyMedium,
								)
							}
							if (openTodos > 0) {
								Text(
									text = stringResource(R.string.jobs_open_todos, openTodos),
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
								onClick = {
									val ask = status == JobStatus.TERMINAT &&
										Rules.needsConfirmForDone(openTodos)
									if (ask) confirmDone = true else vm.setStatus(status)
								},
								label = { Text(stringResource(status.labelRes)) },
							)
						}
					}
				}
				val suggestDeFinisat = Rules.suggestsDeFinisat(
					current = job.status,
					stageCount = stages.size,
					openStages = stages.size - stagesDone,
					openTodos = openTodos,
				)
				if (suggestDeFinisat) {
					item {
						Card(modifier = Modifier.fillMaxWidth()) {
							Column(
								modifier = Modifier.padding(16.dp),
								verticalArrangement = Arrangement.spacedBy(8.dp),
							) {
								Text(
									text = stringResource(R.string.definisat_suggest),
									style = MaterialTheme.typography.bodyMedium,
								)
								TextButton(onClick = { vm.setStatus(JobStatus.DE_FINISAT) }) {
									Text(stringResource(R.string.definisat_apply))
								}
							}
						}
					}
				}
				item {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically,
					) {
						Text(
							text = stringResource(R.string.job_stages_title),
							style = MaterialTheme.typography.titleMedium,
						)
						TextButton(onClick = { showStageSheet = true }) {
							Text(stringResource(R.string.job_stages_add))
						}
					}
				}
				if (stages.isEmpty()) {
					item {
						Text(
							text = stringResource(R.string.job_no_stages),
							style = MaterialTheme.typography.bodyMedium,
						)
					}
					item {
						Text(
							text = stringResource(R.string.job_template_title),
							style = MaterialTheme.typography.bodySmall,
						)
					}
					item {
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.horizontalScroll(rememberScrollState()),
							horizontalArrangement = Arrangement.spacedBy(8.dp),
						) {
							Templates.names.forEach { name ->
								FilterChip(
									selected = false,
									onClick = { vm.applyTemplate(name) },
									label = { Text(name) },
								)
							}
						}
					}
				} else {
					// Bifa se schimbă doar din buton. Pe șantier, o atingere greșită pe rând
					// nu are voie să debifeze tăcut o etapă terminată.
					items(stages, key = { it.id }) { stage ->
						Row(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.spacedBy(4.dp),
							verticalAlignment = Alignment.CenterVertically,
						) {
							IconButton(onClick = { vm.toggleStage(stage) }) {
								val icon = if (stage.done) {
									Icons.Outlined.Check
								} else {
									Icons.Outlined.RadioButtonUnchecked
								}
								Icon(icon, contentDescription = null)
							}
							Text(
								text = stage.name,
								style = MaterialTheme.typography.bodyLarge,
								modifier = Modifier.weight(1f),
							)
							IconButton(onClick = { vm.deleteStage(stage) }) {
								Icon(
									Icons.Outlined.Close,
									contentDescription = stringResource(R.string.stage_delete),
								)
							}
						}
					}
				}
				item {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically,
					) {
						Text(
							text = stringResource(R.string.job_days_title),
							style = MaterialTheme.typography.titleMedium,
						)
						TextButton(onClick = { showNewDay = true }) {
							Text(stringResource(R.string.job_day_add))
						}
					}
				}
				item {
					Button(
						onClick = {
							vm.logToday { saved ->
								scope.launch {
									snackbarHost.showSnackbar(
										if (saved) loggedMessage else alreadyMessage,
									)
								}
							}
						},
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
				if (days.isEmpty()) {
					item {
						Text(
							text = stringResource(R.string.job_days_empty),
							style = MaterialTheme.typography.bodyMedium,
						)
					}
				} else {
					items(days, key = { it.id }) { day ->
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.clickable { editingDayId = day.id },
							horizontalArrangement = Arrangement.spacedBy(12.dp),
							verticalAlignment = Alignment.CenterVertically,
						) {
							Text(
								text = Dates.dayMonth(day.date),
								style = MaterialTheme.typography.titleSmall,
							)
							Column(modifier = Modifier.weight(1f)) {
								val what = day.what
								if (!what.isNullOrBlank()) {
									Text(text = what, style = MaterialTheme.typography.bodyMedium)
								}
								val hours = day.hours
								if (hours != null) {
									Text(
										text = stringResource(R.string.job_day_hours, Dates.hours(hours)),
										style = MaterialTheme.typography.bodySmall,
									)
								}
								val blocked = day.blocked
								if (!blocked.isNullOrBlank()) {
									Text(
										text = blocked,
										style = MaterialTheme.typography.bodySmall,
										color = MaterialTheme.colorScheme.error,
									)
								}
							}
						}
					}
				}
				item {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically,
					) {
						Text(
							text = stringResource(R.string.job_todos_title),
							style = MaterialTheme.typography.titleMedium,
						)
						TextButton(onClick = { showNewTodo = true }) {
							Text(stringResource(R.string.job_todo_add))
						}
					}
				}
				if (todos.isEmpty()) {
					item {
						Text(
							text = stringResource(R.string.job_todos_empty),
							style = MaterialTheme.typography.bodyMedium,
						)
					}
				} else {
					items(todos, key = { it.id }) { todo ->
						Row(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.spacedBy(4.dp),
							verticalAlignment = Alignment.CenterVertically,
						) {
							IconButton(onClick = { vm.toggleTodo(todo) }) {
								val icon = if (todo.done) {
									Icons.Outlined.Check
								} else {
									Icons.Outlined.RadioButtonUnchecked
								}
								Icon(icon, contentDescription = null)
							}
							Column(
								modifier = Modifier
									.weight(1f)
									.clickable { editingTodoId = todo.id },
							) {
								val place = todo.place
								val line = if (place.isNullOrBlank()) {
									todo.what
								} else {
									place + ": " + todo.what
								}
								Text(text = line, style = MaterialTheme.typography.bodyLarge)
								val due = todo.due
								if (due != null) {
									Text(
										text = stringResource(R.string.punch_due, Dates.dayMonth(due)),
										style = MaterialTheme.typography.bodySmall,
									)
								}
								val reason = todo.reason
								if (reason != null) {
									Text(
										text = stringResource(reason.labelRes),
										style = MaterialTheme.typography.bodySmall,
										color = MaterialTheme.colorScheme.onSurfaceVariant,
									)
								}
							}
							IconButton(onClick = { vm.deleteTodo(todo) }) {
								Icon(
									Icons.Outlined.Close,
									contentDescription = stringResource(R.string.todo_delete),
								)
							}
						}
					}
				}
				item {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically,
					) {
						Text(
							text = stringResource(R.string.job_materials_title),
							style = MaterialTheme.typography.titleMedium,
						)
						TextButton(onClick = { showNewMaterial = true }) {
							Text(stringResource(R.string.job_material_add))
						}
					}
				}
				if (materials.isEmpty()) {
					item {
						Text(
							text = stringResource(R.string.job_materials_empty),
							style = MaterialTheme.typography.bodyMedium,
						)
					}
				} else {
					items(materials, key = { it.id }) { material ->
						Row(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.spacedBy(4.dp),
							verticalAlignment = Alignment.CenterVertically,
						) {
							IconButton(onClick = { vm.toggleMaterial(material) }) {
								val icon = if (material.bought) {
									Icons.Outlined.Check
								} else {
									Icons.Outlined.RadioButtonUnchecked
								}
								Icon(icon, contentDescription = null)
							}
							Column(modifier = Modifier.weight(1f)) {
								val qty = material.qty
								val line = if (qty.isNullOrBlank()) {
									material.what
								} else {
									material.what + " · " + qty
								}
								Text(text = line, style = MaterialTheme.typography.bodyLarge)
								val shop = material.shop
								if (!shop.isNullOrBlank()) {
									Text(
										text = shop,
										style = MaterialTheme.typography.bodySmall,
										color = MaterialTheme.colorScheme.onSurfaceVariant,
									)
								}
							}
							IconButton(onClick = { vm.deleteMaterial(material) }) {
								Icon(
									Icons.Outlined.Close,
									contentDescription = stringResource(R.string.material_delete),
								)
							}
						}
					}
				}
			}
		}
	}

	if (showStageSheet) {
		TextSheet(
			title = stringResource(R.string.stage_new_title),
			label = stringResource(R.string.stage_name),
			onDismiss = { showStageSheet = false },
			onSave = { name ->
				vm.addStage(name)
				showStageSheet = false
			},
		)
	}

	if (showNewDay || editingDay != null) {
		val current = editingDay
		DaySheet(
			day = current,
			title = if (current == null) {
				stringResource(R.string.day_new_title)
			} else {
				stringResource(R.string.day_edit_title, Dates.dayMonth(current.date))
			},
			onDismiss = {
				showNewDay = false
				editingDayId = null
			},
			onDelete = {
				if (current != null) vm.deleteDay(current)
				showNewDay = false
				editingDayId = null
			},
			onSave = { date, what, hours, blocked ->
				vm.saveDay(current, date, what, hours, blocked)
				showNewDay = false
				editingDayId = null
			},
		)
	}

	if (showNewTodo || editingTodo != null) {
		val current = editingTodo
		TodoSheet(
			todo = current,
			title = if (current == null) {
				stringResource(R.string.todo_new_title)
			} else {
				stringResource(R.string.todo_edit_title)
			},
			onDismiss = {
				showNewTodo = false
				editingTodoId = null
			},
			onDelete = {
				if (current != null) vm.deleteTodo(current)
				showNewTodo = false
				editingTodoId = null
			},
			onSave = { what, place, reason, due ->
				if (current == null) {
					vm.addTodo(what, place, reason, due)
				} else {
					vm.saveTodo(current, what, place, reason, due)
				}
				showNewTodo = false
				editingTodoId = null
			},
		)
	}

	if (showNewMaterial) {
		MaterialSheet(
			material = null,
			title = stringResource(R.string.material_new_title),
			onDismiss = { showNewMaterial = false },
			onDelete = { showNewMaterial = false },
			onSave = { what, qty, shop ->
				vm.addMaterial(what, qty, shop)
				showNewMaterial = false
			},
		)
	}

	if (confirmDone) {
		AlertDialog(
			onDismissRequest = { confirmDone = false },
			title = { Text(stringResource(R.string.done_confirm_title)) },
			text = { Text(stringResource(R.string.done_confirm_text, openTodos)) },
			confirmButton = {
				TextButton(
					onClick = {
						confirmDone = false
						vm.setStatus(JobStatus.TERMINAT)
					},
				) {
					Text(stringResource(R.string.done_confirm_yes))
				}
			},
			dismissButton = {
				TextButton(onClick = { confirmDone = false }) {
					Text(stringResource(R.string.cancel))
				}
			},
		)
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
