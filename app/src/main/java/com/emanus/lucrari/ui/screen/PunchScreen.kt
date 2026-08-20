package com.emanus.lucrari.ui.screen

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emanus.lucrari.App
import com.emanus.lucrari.R
import com.emanus.lucrari.data.Todo
import com.emanus.lucrari.data.TodoWithJob
import com.emanus.lucrari.data.today
import com.emanus.lucrari.domain.Dates
import com.emanus.lucrari.ui.component.labelRes
import com.emanus.lucrari.ui.component.BrandCard
import com.emanus.lucrari.ui.component.BrandEmptyState
import com.emanus.lucrari.ui.component.BrandTopAppBar
import com.emanus.lucrari.ui.theme.Dimens
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PunchViewModel(app: Application) : AndroidViewModel(app) {

	private val repo = (app as App).repo

	val todos: StateFlow<List<TodoWithJob>> = repo.openTodos()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	fun toggle(todo: Todo) {
		viewModelScope.launch { repo.toggleTodo(todo) }
	}
}

/**
 * Tot ce a rămas nefăcut, din toate lucrările, grupat pe lucrare (SPEC §11). Se bifează
 * de aici, fără să intre în lucrare; apăsarea pe capul de listă deschide lucrarea.
 */
@Composable
fun PunchScreen(
	onOpenJob: (String) -> Unit,
	vm: PunchViewModel = viewModel(),
) {
	val rows by vm.todos.collectAsState()

	Scaffold(
		containerColor = MaterialTheme.colorScheme.background,
		topBar = {
			BrandTopAppBar(title = stringResource(R.string.screen_punch_title))
		},
	) { padding ->
		if (rows.isEmpty()) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.padding(padding)
					.padding(Dimens.space32),
				contentAlignment = Alignment.Center,
			) {
				BrandEmptyState(
					icon = Icons.Outlined.Checklist,
					title = stringResource(R.string.punch_empty),
				)
			}
		} else {
			LazyColumn(
				modifier = Modifier
					.fillMaxSize()
					.padding(padding),
				contentPadding = PaddingValues(
					start = Dimens.screenPadding,
					top = Dimens.space8,
					end = Dimens.screenPadding,
					bottom = Dimens.listBottomSpace,
				),
				verticalArrangement = Arrangement.spacedBy(Dimens.space8),
			) {
				rows.groupBy { it.todo.jobId }.forEach { (jobId, group) ->
					val head = group.first()
					item(key = "cap-" + jobId) {
						Column(
							modifier = Modifier
								.fillMaxWidth()
								.clickable { onOpenJob(jobId) }
								.padding(top = Dimens.space8),
						) {
							Text(
								text = head.clientName,
								style = MaterialTheme.typography.titleMedium,
							)
							val where = listOfNotNull(head.street, head.jobTitle)
								.joinToString(" · ")
							if (where.isNotEmpty()) {
								Text(
									text = where,
									style = MaterialTheme.typography.bodyMedium,
									color = MaterialTheme.colorScheme.onSurfaceVariant,
								)
							}
						}
					}
					items(group, key = { it.todo.id }) { row ->
						TodoRow(row = row, onToggle = { vm.toggle(row.todo) })
					}
				}
			}
		}
	}
}

@Composable
private fun TodoRow(row: TodoWithJob, onToggle: () -> Unit) {
	BrandCard(modifier = Modifier.fillMaxWidth()) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(Dimens.space8),
			horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
			verticalAlignment = Alignment.CenterVertically,
		) {
			IconButton(onClick = onToggle) {
				Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = null)
			}
			Column(modifier = Modifier.weight(1f)) {
			val place = row.todo.place
			val line = if (place.isNullOrBlank()) row.todo.what else place + ": " + row.todo.what
			Text(text = line, style = MaterialTheme.typography.bodyLarge)
			val due = row.todo.due
			if (due != null) {
				val late = due.isBefore(today())
				Text(
					text = if (late) {
						stringResource(R.string.punch_overdue, Dates.dayMonth(due))
					} else {
						stringResource(R.string.punch_due, Dates.dayMonth(due))
					},
					style = MaterialTheme.typography.bodySmall,
					color = if (late) {
						MaterialTheme.colorScheme.error
					} else {
						MaterialTheme.colorScheme.onSurfaceVariant
					},
				)
			}
			val reason = row.todo.reason
			if (reason != null) {
				Text(
					text = stringResource(reason.labelRes),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
			}
		}
	}
}
