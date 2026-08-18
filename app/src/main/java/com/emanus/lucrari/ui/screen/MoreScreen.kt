package com.emanus.lucrari.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.emanus.lucrari.R

@Composable
fun MoreScreen(onOpenClients: () -> Unit) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(16.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
	) {
		Text(
			text = stringResource(R.string.screen_more_title),
			style = MaterialTheme.typography.headlineSmall,
		)
		Card(
			modifier = Modifier
				.fillMaxWidth()
				.clickable { onOpenClients() },
		) {
			Row(
				modifier = Modifier.padding(16.dp),
				horizontalArrangement = Arrangement.spacedBy(16.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Icon(Icons.Outlined.Person, contentDescription = null)
				Column {
					Text(
						text = stringResource(R.string.more_clients),
						style = MaterialTheme.typography.titleMedium,
					)
					Text(
						text = stringResource(R.string.more_clients_hint),
						style = MaterialTheme.typography.bodyMedium,
					)
				}
			}
		}
		Text(
			text = stringResource(R.string.screen_more_subtitle),
			style = MaterialTheme.typography.bodyMedium,
		)
	}
}
