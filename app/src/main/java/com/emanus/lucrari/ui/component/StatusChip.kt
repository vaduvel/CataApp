package com.emanus.lucrari.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.emanus.lucrari.R
import com.emanus.lucrari.data.JobStatus
import com.emanus.lucrari.ui.theme.Dimens
import com.emanus.lucrari.ui.theme.StatusColor
import com.emanus.lucrari.ui.theme.StatusTone
import com.emanus.lucrari.ui.theme.StatusTones

/** Culoarea plină a statusului, aceeași peste tot în aplicație (SPEC §7). */
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

/** Fundalul și textul chip-ului, alese pentru contrast în lumină puternică. */
val JobStatus.tone: StatusTone
	get() = when (this) {
		JobStatus.OFERTAT -> StatusTones.Ofertat
		JobStatus.PROGRAMAT -> StatusTones.Programat
		JobStatus.IN_LUCRU -> StatusTones.InLucru
		JobStatus.ASTEPTARE -> StatusTones.Asteptare
		JobStatus.DE_FINISAT -> StatusTones.DeFinisat
		JobStatus.TERMINAT -> StatusTones.Terminat
		JobStatus.ANULAT -> StatusTones.Anulat
	}

/** Statusul nu se comunică doar prin culoare: fiecare are și pictogramă. */
val JobStatus.icon: ImageVector
	get() = when (this) {
		JobStatus.OFERTAT -> Icons.Outlined.Description
		JobStatus.PROGRAMAT -> Icons.Outlined.Event
		JobStatus.IN_LUCRU -> Icons.Outlined.Construction
		JobStatus.ASTEPTARE -> Icons.Outlined.PauseCircle
		JobStatus.DE_FINISAT -> Icons.AutoMirrored.Outlined.FormatListBulleted
		JobStatus.TERMINAT -> Icons.Outlined.CheckCircle
		JobStatus.ANULAT -> Icons.Outlined.Cancel
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
	val tone = status.tone
	Row(
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(Dimens.space4),
		modifier = modifier
			.clip(RoundedCornerShape(50))
			.background(tone.container)
			.padding(start = Dimens.space8, end = Dimens.space12, top = Dimens.space6, bottom = Dimens.space6),
	) {
		Icon(
			imageVector = status.icon,
			contentDescription = null,
			tint = tone.content,
			modifier = Modifier.size(Dimens.space16),
		)
		Text(
			text = stringResource(status.labelRes),
			color = tone.content,
			style = MaterialTheme.typography.labelLarge,
		)
	}
}
