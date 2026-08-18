package com.emanus.lucrari.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.emanus.lucrari.App
import com.emanus.lucrari.R
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.Phase
import com.emanus.lucrari.data.Photo
import com.emanus.lucrari.data.Todo
import com.emanus.lucrari.data.repo.PendingPhotoCapture
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PhotosViewModel(app: App) : ViewModel() {
	private val repo = app.repo
	private val store = app.photoStore

	val jobs: StateFlow<List<Job>> = repo.allJobs()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
	val selectedJobId = MutableStateFlow<String?>(null)

	@OptIn(ExperimentalCoroutinesApi::class)
	val photos: StateFlow<List<Photo>> = selectedJobId.flatMapLatest { id ->
		if (id == null) flowOf(emptyList()) else store.observe(id)
	}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	@OptIn(ExperimentalCoroutinesApi::class)
	val todos: StateFlow<List<Todo>> = selectedJobId.flatMapLatest { id ->
		if (id == null) flowOf(emptyList()) else repo.todos(id)
	}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	fun selectJob(id: String) {
		selectedJobId.value = id
	}

	fun createCapture(todoId: String?, phase: Phase): PendingPhotoCapture? {
		val jobId = selectedJobId.value ?: return null
		return store.createCapture(jobId, todoId, phase)
	}

	fun complete(capture: PendingPhotoCapture, success: Boolean, onDone: (Boolean) -> Unit) {
		viewModelScope.launch {
			onDone(runCatching { store.complete(capture, success) }.getOrDefault(false))
		}
	}

	fun discard(capture: PendingPhotoCapture) {
		store.discard(capture)
	}

	fun delete(photo: Photo) {
		viewModelScope.launch { store.delete(photo) }
	}

	companion object {
		val factory: ViewModelProvider.Factory = viewModelFactory {
			initializer {
				val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as App
				PhotosViewModel(app)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(onBack: () -> Unit) {
	val vm: PhotosViewModel = viewModel(factory = PhotosViewModel.factory)
	val jobs by vm.jobs.collectAsState()
	val selectedJobId by vm.selectedJobId.collectAsState()
	val photos by vm.photos.collectAsState()
	val todos by vm.todos.collectAsState()
	val openTodos = todos.filter { !it.done }
	val snackbar = remember { SnackbarHostState() }
	val scope = rememberCoroutineScope()
	var selectedTodoId by rememberSaveable { mutableStateOf<String?>(null) }
	var phase by remember { mutableStateOf(Phase.DURING) }
	var pending by remember { mutableStateOf<PendingPhotoCapture?>(null) }
	val photoSaved = stringResource(R.string.photo_saved)
	val photoFailed = stringResource(R.string.photo_failed)

	LaunchedEffect(jobs, selectedJobId) {
		if (selectedJobId == null && jobs.isNotEmpty()) vm.selectJob(jobs.first().id)
	}
	LaunchedEffect(selectedJobId) { selectedTodoId = null }

	val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
		val capture = pending
		pending = null
		if (capture != null) {
			vm.complete(capture, success) { saved ->
				scope.launch { snackbar.showSnackbar(if (saved) photoSaved else photoFailed) }
			}
		}
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.photos_title)) },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(
							Icons.AutoMirrored.Outlined.ArrowBack,
							contentDescription = stringResource(R.string.back),
						)
					}
				},
			)
		},
		snackbarHost = { SnackbarHost(snackbar) },
	) { padding ->
		LazyColumn(
			modifier = Modifier.fillMaxSize().padding(padding),
			contentPadding = PaddingValues(16.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			item {
				Text(stringResource(R.string.photos_choose_job), style = MaterialTheme.typography.titleMedium)
				Row(
					modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
					horizontalArrangement = Arrangement.spacedBy(8.dp),
				) {
					jobs.forEach { job ->
						FilterChip(
							selected = selectedJobId == job.id,
							onClick = { vm.selectJob(job.id) },
							label = { Text(job.title) },
						)
					}
				}
				if (jobs.isEmpty()) Text(stringResource(R.string.photos_no_jobs))
			}
			if (selectedJobId != null) {
				item {
					Text(stringResource(R.string.photos_phase), style = MaterialTheme.typography.titleMedium)
					Row(
						modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
						horizontalArrangement = Arrangement.spacedBy(8.dp),
					) {
						Phase.entries.forEach { item ->
							val label = when (item) {
								Phase.BEFORE -> R.string.photo_before
								Phase.DURING -> R.string.photo_during
								Phase.AFTER -> R.string.photo_after
							}
							FilterChip(
								selected = phase == item,
								onClick = { phase = item },
								label = { Text(stringResource(label)) },
							)
						}
					}
				}
				item {
					Text(stringResource(R.string.photos_link_todo), style = MaterialTheme.typography.titleMedium)
					Row(
						modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
						horizontalArrangement = Arrangement.spacedBy(8.dp),
					) {
						FilterChip(
							selected = selectedTodoId == null,
							onClick = { selectedTodoId = null },
							label = { Text(stringResource(R.string.photos_job_only)) },
						)
						openTodos.forEach { todo ->
							FilterChip(
								selected = selectedTodoId == todo.id,
								onClick = { selectedTodoId = todo.id },
								label = { Text(todo.what, maxLines = 1) },
							)
						}
					}
				}
				item {
					Button(
						onClick = {
							val capture = runCatching { vm.createCapture(selectedTodoId, phase) }.getOrNull()
							if (capture == null) {
								scope.launch { snackbar.showSnackbar(photoFailed) }
							} else {
								pending = capture
								try {
									camera.launch(capture.uri)
								} catch (_: Throwable) {
									pending = null
									vm.discard(capture)
									scope.launch { snackbar.showSnackbar(photoFailed) }
								}
							}
						},
						modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
					) {
						Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
						Spacer(Modifier.width(8.dp))
						Text(stringResource(R.string.photos_take))
					}
				}
				if (photos.isEmpty()) {
					item { Text(stringResource(R.string.photos_empty)) }
				} else {
					items(photos, key = { it.id }) { photo ->
						Card(modifier = Modifier.fillMaxWidth()) {
							Column {
								AsyncImage(
									model = File(photo.path),
									contentDescription = stringResource(R.string.photo_content_description),
									modifier = Modifier.fillMaxWidth().height(220.dp),
									contentScale = ContentScale.Crop,
								)
								Row(
									modifier = Modifier.fillMaxWidth().padding(12.dp),
									horizontalArrangement = Arrangement.SpaceBetween,
								) {
									Column(modifier = Modifier.weight(1f)) {
										val phaseText = when (photo.phase) {
											Phase.BEFORE -> R.string.photo_before
											Phase.DURING -> R.string.photo_during
											Phase.AFTER -> R.string.photo_after
										}
										Text(stringResource(phaseText), style = MaterialTheme.typography.titleSmall)
										val todo = todos.firstOrNull { it.id == photo.todoId }
										if (todo != null) Text(todo.what, style = MaterialTheme.typography.bodyMedium)
									}
									IconButton(onClick = { vm.delete(photo) }) {
										Icon(
											Icons.Outlined.Delete,
											contentDescription = stringResource(R.string.photo_delete),
										)
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
