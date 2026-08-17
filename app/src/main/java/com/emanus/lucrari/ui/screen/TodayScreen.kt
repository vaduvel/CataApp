package com.emanus.lucrari.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.emanus.lucrari.R
import com.emanus.lucrari.ui.component.Placeholder

@Composable
fun TodayScreen() = Placeholder(
	title = stringResource(R.string.screen_today_title),
	subtitle = stringResource(R.string.screen_today_subtitle),
)