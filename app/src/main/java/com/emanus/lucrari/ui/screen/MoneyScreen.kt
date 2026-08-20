package com.emanus.lucrari.ui.screen

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emanus.lucrari.App
import com.emanus.lucrari.R
import com.emanus.lucrari.data.repo.JobMoney
import com.emanus.lucrari.data.repo.MoneySummary
import com.emanus.lucrari.data.today
import com.emanus.lucrari.domain.Money
import com.emanus.lucrari.ui.component.BrandCard
import com.emanus.lucrari.ui.component.BrandSectionHeader
import com.emanus.lucrari.ui.component.BrandTopAppBar
import com.emanus.lucrari.ui.theme.AppColor
import com.emanus.lucrari.ui.theme.Dimens
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MoneyViewModel(app: Application) : AndroidViewModel(app) {

	private val repo = (app as App).repo

	/**
	 * Cele trei cifre mari și lista De facturat. Data se ia la deschiderea ecranului: e de
	 * ajuns, pentru că la fiecare intrare pe tab se recalculează.
	 */
	val summary: StateFlow<MoneySummary> = repo.moneySummary(today())
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EMPTY)

	val board: StateFlow<List<JobMoney>> = repo.moneyBoard()
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

	private companion object {
		val EMPTY = MoneySummary(
			outstandingCents = 0,
			overdueCents = 0,
			collectedThisMonthCents = 0,
			toInvoice = emptyList(),
		)
	}
}

/**
 * Ecranul Bani (SPEC §5.3): trei cifre sus, lucrările care așteaptă o factură dedesubt, apoi
 * restul lucrărilor. Nu se emite nimic aici, doar se vede unde stau banii.
 *
 * O lucrare apare o singură dată pe ecran: ce e în De facturat nu se mai repetă jos, ca să nu
 * pară că sunt doi bani diferiți de luat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
	onOpenJobMoney: (String) -> Unit,
	vm: MoneyViewModel = viewModel(),
) {
	val summary by vm.summary.collectAsState()
	val board by vm.board.collectAsState()

	val others = remember(board, summary) {
		val shown = summary.toInvoice.map { it.job.id }.toSet()
		board.filter { !shown.contains(it.job.id) }
	}

	Scaffold(
		containerColor = MaterialTheme.colorScheme.background,
		topBar = { BrandTopAppBar(title = stringResource(R.string.screen_money_title)) },
	) { padding ->
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
			verticalArrangement = Arrangement.spacedBy(Dimens.cardSpacing),
		) {
			item {
				BrandCard(
					modifier = Modifier.fillMaxWidth(),
					containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f),
				) {
					Column(
						modifier = Modifier.padding(Dimens.cardPadding),
						verticalArrangement = Arrangement.spacedBy(Dimens.space12),
					) {
						BigNumber(R.string.money_outstanding, summary.outstandingCents)
						HorizontalDivider()
						BigNumber(R.string.money_overdue, summary.overdueCents)
						HorizontalDivider()
						BigNumber(R.string.money_this_month, summary.collectedThisMonthCents)
					}
				}
			}

			item {
				BrandSectionHeader(title = stringResource(R.string.money_to_invoice_title))
			}
			if (summary.toInvoice.isEmpty()) {
				item {
					Text(
						text = stringResource(R.string.money_to_invoice_empty),
						style = MaterialTheme.typography.bodyMedium,
					)
				}
			}
			items(summary.toInvoice, key = { "tofact-" + it.job.id }) { row ->
				JobMoneyRow(
					row = row,
					bigCents = row.totals.toInvoiceCents,
					onClick = { onOpenJobMoney(row.job.id) },
				)
			}

			if (board.isEmpty()) {
				item {
					Text(
						text = stringResource(R.string.money_empty),
						style = MaterialTheme.typography.bodyMedium,
					)
				}
			} else if (others.isNotEmpty()) {
				item {
				BrandSectionHeader(title = stringResource(R.string.money_other_jobs))
				}
				items(others, key = { "toate-" + it.job.id }) { row ->
					JobMoneyRow(
						row = row,
						bigCents = row.totals.totalCents,
						onClick = { onOpenJobMoney(row.job.id) },
					)
				}
			}
		}
	}
}

@Composable
private fun BigNumber(labelRes: Int, cents: Long) {
	Column(verticalArrangement = Arrangement.spacedBy(Dimens.space2)) {
		Text(
			text = stringResource(labelRes),
			style = MaterialTheme.typography.labelLarge,
			color = MaterialTheme.colorScheme.onSecondaryContainer,
		)
		Text(
			text = Money.format(cents),
			style = MaterialTheme.typography.headlineMedium,
			color = MaterialTheme.colorScheme.onBackground,
		)
	}
}

@Composable
private fun JobMoneyRow(row: JobMoney, bigCents: Long, onClick: () -> Unit) {
	val rest = row.totals.outstandingCents
	BrandCard(
		modifier = Modifier.fillMaxWidth(),
		onClick = onClick,
		accent = if (rest > 0) MaterialTheme.colorScheme.primary else AppColor.Success,
	) {
		Column(
			modifier = Modifier.padding(Dimens.cardPadding),
			verticalArrangement = Arrangement.spacedBy(Dimens.space4),
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
			) {
				Text(text = row.job.title, style = MaterialTheme.typography.titleMedium)
				Text(text = Money.format(bigCents), style = MaterialTheme.typography.titleMedium)
			}
			val where = listOfNotNull(row.clientName, row.job.street).joinToString(" — ")
			if (where.isNotEmpty()) {
				Text(text = where, style = MaterialTheme.typography.bodyMedium)
			}
			Text(
				text = stringResource(
					R.string.money_row_invoiced,
					Money.format(row.totals.invoicedCents),
					Money.format(row.totals.collectedCents),
				),
				style = MaterialTheme.typography.bodySmall,
			)
			Text(
				text = if (rest > 0) {
					stringResource(R.string.money_row_rest, Money.format(rest))
				} else {
					stringResource(R.string.money_row_settled)
				},
				style = MaterialTheme.typography.bodySmall,
			)
		}
	}
}
