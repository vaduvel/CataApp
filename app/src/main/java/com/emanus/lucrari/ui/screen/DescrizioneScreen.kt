package com.emanus.lucrari.ui.screen

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emanus.lucrari.App
import com.emanus.lucrari.R
import com.emanus.lucrari.data.Client
import com.emanus.lucrari.data.Extra
import com.emanus.lucrari.data.InvoiceRef
import com.emanus.lucrari.data.Job
import com.emanus.lucrari.data.Measure
import com.emanus.lucrari.data.Stage
import com.emanus.lucrari.data.WorkDay
import com.emanus.lucrari.domain.descrizione
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Cine și când. Se ține separat ca să nu adunăm mai mult de cinci fluxuri într-un combine. */
private data class DescrizioneWho(
	val job: Job?,
	val client: Client?,
	val days: List<WorkDay>,
)

/** Ce s-a lucrat și ce s-a facturat. */
private data class DescrizioneParts(
	val stages: List<Stage>,
	val measures: List<Measure>,
	val extras: List<Extra>,
	val invoices: List<InvoiceRef>,
)

class DescrizioneViewModel(app: Application) : AndroidViewModel(app) {

	private val repo = (app as App).repo

	/**
	 * Textul se recompune la fiecare schimbare din lucrare. Nu se salvează nicăieri: e doar o
	 * privire asupra datelor, ca să nu ajungă în bază două adevăruri diferite.
	 */
	fun text(jobId: String): Flow<String> {
		val who = combine(
			repo.job(jobId),
			repo.clients(),
			repo.days(jobId),
		) { job, clients, days ->
			DescrizioneWho(job, clients.firstOrNull { it.id == job?.clientId }, days)
		}

		val parts = combine(
			repo.stages(jobId),
			repo.measures(jobId),
			repo.extras(jobId),
			repo.invoices(jobId),
		) { stages, measures, extras, invoices ->
			DescrizioneParts(stages, measures, extras, invoices)
		}

		return combine(who, parts) { cine, ce ->
			val job = cine.job ?: return@combine ""
			descrizione(
				job = job,
				client = cine.client ?: Client(id = job.clientId, name = ""),
				days = cine.days,
				stages = ce.stages,
				measures = ce.measures,
				extras = ce.extras,
				invoices = ce.invoices,
			)
		}
	}
}

/**
 * Descrierea pentru factură (SPEC §5.5). Se citește, se copiază și se trimite. Atât.
 * Trimiterea deschide ce are el pe telefon, de obicei WhatsApp: aplicația nu are rețea.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DescrizioneScreen(
	jobId: String,
	onBack: () -> Unit,
	vm: DescrizioneViewModel = viewModel(),
) {
	val textFlow = remember(jobId) { vm.text(jobId) }
	val text by textFlow.collectAsState(initial = "")

	val clipboard = LocalClipboardManager.current
	val context = LocalContext.current
	val snackbarState = remember { SnackbarHostState() }
	val scope = rememberCoroutineScope()
	val copiedMessage = stringResource(R.string.descrizione_copied)

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.descrizione_title)) },
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
		snackbarHost = { SnackbarHost(snackbarState) },
	) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
				.verticalScroll(rememberScrollState())
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			Text(
				text = stringResource(R.string.descrizione_hint),
				style = MaterialTheme.typography.bodyMedium,
			)

			if (text.isEmpty()) {
				Text(
					text = stringResource(R.string.descrizione_empty),
					style = MaterialTheme.typography.bodyMedium,
				)
			} else {
				Card(modifier = Modifier.fillMaxWidth()) {
					SelectionContainer {
						Text(
							text = text,
							modifier = Modifier.padding(16.dp),
							style = MaterialTheme.typography.bodyLarge,
						)
					}
				}

				Button(
					onClick = {
						clipboard.setText(AnnotatedString(text))
						scope.launch { snackbarState.showSnackbar(copiedMessage) }
					},
					modifier = Modifier
						.fillMaxWidth()
						.height(56.dp),
				) {
					Text(stringResource(R.string.descrizione_copy))
				}

				OutlinedButton(
					onClick = {
						val send = Intent(Intent.ACTION_SEND).apply {
							type = "text/plain"
							putExtra(Intent.EXTRA_TEXT, text)
						}
						context.startActivity(Intent.createChooser(send, null))
					},
					modifier = Modifier
						.fillMaxWidth()
						.height(56.dp),
				) {
					Text(stringResource(R.string.descrizione_send))
				}
			}
		}
	}
}
