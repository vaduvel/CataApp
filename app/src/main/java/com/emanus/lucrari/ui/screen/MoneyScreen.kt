package com.emanus.lucrari.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.emanus.lucrari.R
import com.emanus.lucrari.ui.component.Placeholder

@Composable
fun MoneyScreen() = Placeholder(
	title = stringResource(R.string.screen_money_title),
	subtitle = stringResource(R.string.screen_money_subtitle),
)