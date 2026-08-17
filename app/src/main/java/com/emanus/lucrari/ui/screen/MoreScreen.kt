package com.emanus.lucrari.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.emanus.lucrari.R
import com.emanus.lucrari.ui.component.Placeholder

@Composable
fun MoreScreen() = Placeholder(
	title = stringResource(R.string.screen_more_title),
	subtitle = stringResource(R.string.screen_more_subtitle),
)