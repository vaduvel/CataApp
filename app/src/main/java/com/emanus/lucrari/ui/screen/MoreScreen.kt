package com.emanus.lucrari.ui.screen

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.emanus.lucrari.App
import com.emanus.lucrari.R
import com.emanus.lucrari.data.Reminder
import com.emanus.lucrari.data.repo.ImportMode
import com.emanus.lucrari.data.repo.ImportResult
import com.emanus.lucrari.domain.Seed
import com.emanus.lucrari.ui.component.BrandCard
import com.emanus.lucrari.ui.component.BrandIconTile
import com.emanus.lucrari.ui.component.BrandPageHeader
import com.emanus.lucrari.ui.component.BrandSectionHeader
import com.emanus.lucrari.ui.theme.Dimens
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MoreViewModel(app: App) : ViewModel() {
	private val backup = app.backupRepo
	private val db = app.db
	private val remindersRepo = app.reminderRepo
	val reminders: StateFlow<List<Reminder>> = remindersRepo.open()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
	val busy = MutableStateFlow(false)

	fun export(uri: Uri, onDone: (Boolean) -> Unit) {
		viewModelScope.launch {
			busy.value = true
			onDone(runCatching { backup.exportTo(uri) }.isSuccess)
			busy.value = false
		}
	}

	fun import(uri: Uri, mode: ImportMode, onDone: (ImportResult?) -> Unit) {
		viewModelScope.launch {
			busy.value = true
			onDone(runCatching { backup.importFrom(uri, mode) }.getOrNull())
			busy.value = false
		}
	}

	fun share(onDone: (Uri?) -> Unit) {
		viewModelScope.launch {
			busy.value = true
			val uri = runCatching {
				val file = backup.latestOrCreate()
				backup.shareUri(file)
			}.getOrNull()
			onDone(uri)
			busy.value = false
		}
	}

	fun toggle(reminder: Reminder) {
		viewModelScope.launch { remindersRepo.toggle(reminder) }
	}

	fun deleteDemo(onDone: (Boolean) -> Unit) {
		viewModelScope.launch {
			busy.value = true
			onDone(Seed.delete(db))
			busy.value = false
		}
	}

	companion object {
		val factory: ViewModelProvider.Factory = viewModelFactory {
			initializer {
				val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as App
				MoreViewModel(app)
			}
		}
	}
}

@Composable
fun MoreScreen(
	onOpenCalendar: () -> Unit,
	onOpenClients: () -> Unit,
	onOpenPhotos: () -> Unit,
) {
	val vm: MoreViewModel = viewModel(factory = MoreViewModel.factory)
	val reminders by vm.reminders.collectAsState()
	val busy by vm.busy.collectAsState()
	val context = LocalContext.current
	val snackbar = remember { SnackbarHostState() }
	val scope = rememberCoroutineScope()
	var importUri by remember { mutableStateOf<Uri?>(null) }
	var notificationGranted by remember {
		mutableStateOf(
			Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
				ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
				PackageManager.PERMISSION_GRANTED,
		)
	}
	val exportDone = stringResource(R.string.backup_export_done)
	val importDone = stringResource(R.string.backup_import_done)
	val backupFailed = stringResource(R.string.backup_failed)
	val demoDeleted = stringResource(R.string.demo_deleted)
	val demoAlreadyDeleted = stringResource(R.string.demo_already_deleted)
	val appVersion = remember(context) { installedVersionName(context) }

	val exportLauncher = rememberLauncherForActivityResult(
		ActivityResultContracts.CreateDocument("application/zip"),
	) { uri ->
		if (uri != null) vm.export(uri) { ok ->
			scope.launch { snackbar.showSnackbar(if (ok) exportDone else backupFailed) }
		}
	}
	val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
		importUri = uri
	}
	val notificationLauncher = rememberLauncherForActivityResult(
		ActivityResultContracts.RequestPermission(),
	) { granted -> notificationGranted = granted }

	Column(modifier = Modifier.fillMaxSize()) {
		SnackbarHost(snackbar)
		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(
				start = Dimens.screenPadding,
				end = Dimens.screenPadding,
				bottom = Dimens.listBottomSpace,
			),
			verticalArrangement = Arrangement.spacedBy(Dimens.cardSpacing),
		) {
			item {
				BrandPageHeader(
					title = stringResource(R.string.screen_more_title),
					horizontalPadding = Dimens.space0,
				)
			}
			// Calendarul are si o iconita pe ecranul Lucrari, dar acolo e usor de ratat.
			// Randul de aici e singurul loc in care se vede scris ce face.
			item {
				MoreLinkCard(
					title = stringResource(R.string.calendar_title),
					hint = stringResource(R.string.more_calendar_hint),
					onClick = onOpenCalendar,
					icon = Icons.Outlined.CalendarMonth,
				)
			}
			item {
				MoreLinkCard(
					title = stringResource(R.string.more_clients),
					hint = stringResource(R.string.more_clients_hint),
					onClick = onOpenClients,
					icon = Icons.Outlined.Person,
				)
			}
			item {
				MoreLinkCard(
					title = stringResource(R.string.more_photos),
					hint = stringResource(R.string.more_photos_hint),
					onClick = onOpenPhotos,
					icon = Icons.Outlined.PhotoCamera,
				)
			}
			item {
				BrandCard(modifier = Modifier.fillMaxWidth()) {
					Column(
						modifier = Modifier.padding(Dimens.cardPadding),
						verticalArrangement = Arrangement.spacedBy(Dimens.space12),
					) {
						Text(stringResource(R.string.demo_title), style = MaterialTheme.typography.titleMedium)
						Text(stringResource(R.string.demo_hint))
						OutlinedButton(
							onClick = {
								vm.deleteDemo { deleted ->
									scope.launch {
										snackbar.showSnackbar(
											if (deleted) demoDeleted else demoAlreadyDeleted,
										)
									}
								}
							},
							enabled = !busy,
							modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
						) { Text(stringResource(R.string.demo_delete)) }
					}
				}
			}
			item {
				BrandCard(
					modifier = Modifier.fillMaxWidth(),
					containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
				) {
					Column(
						modifier = Modifier.padding(Dimens.cardPadding),
						verticalArrangement = Arrangement.spacedBy(Dimens.space12),
					) {
						Row(
							horizontalArrangement = Arrangement.spacedBy(Dimens.space12),
							verticalAlignment = Alignment.CenterVertically,
						) {
							BrandIconTile(Icons.Outlined.Backup, contentDescription = null)
							Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleMedium)
						}
						Text(stringResource(R.string.backup_hint))
						Button(
							onClick = {
								exportLauncher.launch("lucrari-${LocalDate.now()}.zip")
							},
							enabled = !busy,
							modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
						) { Text(stringResource(R.string.backup_export)) }
						OutlinedButton(
							onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
							enabled = !busy,
							modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
						) { Text(stringResource(R.string.backup_import)) }
						OutlinedButton(
							onClick = {
								vm.share { uri ->
									if (uri == null) {
										scope.launch { snackbar.showSnackbar(backupFailed) }
									} else {
										val intent = Intent(Intent.ACTION_SEND).apply {
											type = "application/zip"
											putExtra(Intent.EXTRA_STREAM, uri)
											clipData = ClipData.newUri(context.contentResolver, "backup", uri)
											addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
										}
										context.startActivity(
											Intent.createChooser(intent, context.getString(R.string.backup_share)),
										)
									}
								}
							},
							enabled = !busy,
							modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
						) { Text(stringResource(R.string.backup_share)) }
					}
				}
			}
			if (!notificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				item {
					OutlinedButton(
						onClick = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
						modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
					) {
						Icon(Icons.Outlined.Notifications, contentDescription = null)
						Text(stringResource(R.string.reminders_enable_notifications))
					}
				}
			}
			item {
				BrandSectionHeader(title = stringResource(R.string.reminders_title))
			}
			if (reminders.isEmpty()) {
				item { Text(stringResource(R.string.reminders_empty)) }
			} else {
				items(reminders, key = { it.id }) { reminder ->
					BrandCard(modifier = Modifier.fillMaxWidth()) {
						Row(
							modifier = Modifier.fillMaxWidth().padding(Dimens.space12),
							verticalAlignment = Alignment.CenterVertically,
						) {
							Text(reminder.text, modifier = Modifier.weight(1f))
							IconButton(onClick = { vm.toggle(reminder) }) {
								Icon(
									Icons.Outlined.CheckCircle,
									contentDescription = stringResource(R.string.reminder_done),
								)
							}
						}
					}
				}
			}
			item {
				Text(
					text = stringResource(R.string.app_version, appVersion),
					style = MaterialTheme.typography.bodySmall,
				)
			}
		}
	}

	val selectedUri = importUri
	if (selectedUri != null) {
		AlertDialog(
			onDismissRequest = { importUri = null },
			title = { Text(stringResource(R.string.backup_import_mode_title)) },
			text = { Text(stringResource(R.string.backup_import_mode_text)) },
			confirmButton = {
				TextButton(
					onClick = {
						importUri = null
						vm.import(selectedUri, ImportMode.REPLACE) { result ->
							scope.launch { snackbar.showSnackbar(if (result != null) importDone else backupFailed) }
						}
					},
				) { Text(stringResource(R.string.backup_replace)) }
			},
			dismissButton = {
				Row {
					TextButton(onClick = { importUri = null }) {
						Text(stringResource(R.string.cancel))
					}
					TextButton(
						onClick = {
							importUri = null
							vm.import(selectedUri, ImportMode.MERGE) { result ->
								scope.launch {
									snackbar.showSnackbar(if (result != null) importDone else backupFailed)
								}
							}
						},
					) { Text(stringResource(R.string.backup_merge)) }
				}
			},
		)
	}
}

@Suppress("DEPRECATION")
private fun installedVersionName(context: Context): String {
	val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
		context.packageManager.getPackageInfo(
			context.packageName,
			PackageManager.PackageInfoFlags.of(0),
		)
	} else {
		context.packageManager.getPackageInfo(context.packageName, 0)
	}
	return info.versionName.orEmpty()
}

@Composable
private fun MoreLinkCard(
	title: String,
	hint: String,
	onClick: () -> Unit,
	icon: ImageVector,
) {
	BrandCard(
		modifier = Modifier.fillMaxWidth(),
		onClick = onClick,
	) {
		Row(
			modifier = Modifier.padding(Dimens.cardPadding),
			horizontalArrangement = Arrangement.spacedBy(Dimens.space12),
			verticalAlignment = Alignment.CenterVertically,
		) {
			BrandIconTile(icon = icon, contentDescription = null)
			Column {
				Text(title, style = MaterialTheme.typography.titleMedium)
				Text(hint, style = MaterialTheme.typography.bodyMedium)
			}
		}
	}
}
