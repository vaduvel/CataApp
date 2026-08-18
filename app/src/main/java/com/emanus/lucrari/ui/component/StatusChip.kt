package com.emanus.lucrari.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.emanus.lucrari.R
import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.ui.theme.StatusColor

/** Culoarea statusului, aceeași peste tot în aplicație (SPEC §7). */
val JobStatus.color: Color
	get() = when (this) {
		JobStatus.OFERTAT -> StatusColor.Ofertat
		JobStatus.PROGRAMAT -> StatusColor.Programat
		JobStatus.IN_LUCRU -> StatusColor.InLucru
		JobStatus.ASTEPTARE -> StatusColor.Asteptare
		JobStatus.DE_FINISAT -> StatusColor.DeFinisat
		JobStatus.TERMINAT -> StatusColor.Terminat
		JobStatus.ANULAT -> StatusColor.Anulat
	}

val JobStatus.labelRes: Int
	get() = when (this) {
		JobStatus.OFERTAT -> R.string.status_ofertat
		JobStatus.PROGRAMAT -> R.string.status_programat
		JobStatus.IN_LUCRU -> R.string.status_in_lucru
		JobStatus.ASTEPTARE -> R.string.status_asteptare
		JobStatus.DE_FINISAT -> R.string.status_de_finisat
		JobStatus.TERMINAT -> R.string.status_terminat
		JobStatus.ANULAT -> R.string.status_anulat
	}

@Composable
fun StatusChip(status: JobStatus, modifier: Modifier = Modifier) {
	Text(
		text = stringResource(status.labelRes),
		color = Color.White,
		style = MaterialTheme.typography.labelLarge,
		modifier = modifier
			.clip(RoundedCornerShape(50))
			.background(status.color)
			.padding(horizontal = 12.dp, vertical = 6.dp),
	)
}
