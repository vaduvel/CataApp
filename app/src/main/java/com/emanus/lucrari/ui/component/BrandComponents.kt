package com.emanus.lucrari.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.emanus.lucrari.ui.theme.Dimens

/**
 * Scheletul unic al formularelor: aceeași suprafață, marcaj de brand, ritm și protecție
 * pentru tastatură pe toate foile de editare.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandFormSheet(
	title: String,
	onDismiss: () -> Unit,
	content: @Composable ColumnScope.() -> Unit,
) {
	val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
	ModalBottomSheet(
		onDismissRequest = onDismiss,
		sheetState = sheetState,
		containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.verticalScroll(rememberScrollState())
				.padding(horizontal = Dimens.space20)
				.padding(bottom = Dimens.space24)
				.imePadding()
				.navigationBarsPadding(),
			verticalArrangement = Arrangement.spacedBy(Dimens.space12),
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
			) {
				Box(
					modifier = Modifier
						.width(Dimens.space4)
						.height(Dimens.space24)
						.clip(RoundedCornerShape(50))
						.background(MaterialTheme.colorScheme.tertiary),
				)
				Text(
					text = title,
					style = MaterialTheme.typography.titleLarge,
					color = MaterialTheme.colorScheme.onSurface,
				)
			}
			content()
		}
	}
}

/** Bară compactă pentru ecranele copil; marcajul vertical păstrează semnătura iconiței. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandTopAppBar(
	title: String,
	modifier: Modifier = Modifier,
	onBack: (() -> Unit)? = null,
	backContentDescription: String? = null,
	actions: @Composable RowScope.() -> Unit = {},
) {
	TopAppBar(
		modifier = modifier,
		title = {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
			) {
				Box(
					modifier = Modifier
						.width(Dimens.space4)
						.height(Dimens.space24)
						.clip(RoundedCornerShape(50))
						.background(MaterialTheme.colorScheme.tertiary),
				)
				Text(
					text = title,
					style = MaterialTheme.typography.titleLarge,
					color = MaterialTheme.colorScheme.onBackground,
				)
			}
		},
		navigationIcon = {
			if (onBack != null) {
				IconButton(onClick = onBack) {
					Icon(
						imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
						contentDescription = backContentDescription,
					)
				}
			}
		},
		actions = actions,
		colors = TopAppBarDefaults.topAppBarColors(
			containerColor = MaterialTheme.colorScheme.background,
			titleContentColor = MaterialTheme.colorScheme.onBackground,
		),
	)
}

/** Antetul de ecran CataLucrari: marcajul coral vine din bara calendarului din iconiță. */
@Composable
fun BrandPageHeader(
	title: String,
	modifier: Modifier = Modifier,
	subtitle: String? = null,
	horizontalPadding: Dp = Dimens.screenPadding,
	action: (@Composable RowScope.() -> Unit)? = null,
) {
	Row(
		modifier = modifier
			.fillMaxWidth()
			.padding(
				start = horizontalPadding,
				end = horizontalPadding,
				top = Dimens.space20,
				bottom = Dimens.space12,
			),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(Dimens.space12),
	) {
		Column(modifier = Modifier.weight(1f)) {
			Box(
				modifier = Modifier
					.width(Dimens.space32)
					.height(Dimens.space4)
					.clip(RoundedCornerShape(50))
					.background(MaterialTheme.colorScheme.tertiary),
			)
			Spacer(Modifier.height(Dimens.space8))
			Text(
				text = title,
				style = MaterialTheme.typography.headlineMedium,
				color = MaterialTheme.colorScheme.onBackground,
				maxLines = 2,
				overflow = TextOverflow.Ellipsis,
			)
			if (!subtitle.isNullOrBlank()) {
				Spacer(Modifier.height(Dimens.space4))
				Text(
					text = subtitle,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		}
		if (action != null) action()
	}
}

@Composable
fun BrandSectionHeader(
	title: String,
	modifier: Modifier = Modifier,
	supporting: String? = null,
) {
	Column(
		modifier = modifier.fillMaxWidth(),
		verticalArrangement = Arrangement.spacedBy(Dimens.space4),
	) {
		Text(
			text = title,
			style = MaterialTheme.typography.titleLarge,
			color = MaterialTheme.colorScheme.onBackground,
		)
		if (!supporting.isNullOrBlank()) {
			Text(
				text = supporting,
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
	}
}

/** Suprafața principală a sistemului: alb cald, contur discret și, opțional, șină de status. */
@Composable
fun BrandCard(
	modifier: Modifier = Modifier,
	onClick: (() -> Unit)? = null,
	accent: Color? = null,
	containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
	content: @Composable () -> Unit,
) {
	val borderColor = if (accent == null) {
		MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
	} else {
		accent.copy(alpha = 0.42f)
	}
	val cardContent: @Composable () -> Unit = {
		Row(modifier = Modifier.height(IntrinsicSize.Min)) {
			if (accent != null) {
				Box(
					modifier = Modifier
						.width(Dimens.statusRailWidth)
						.fillMaxHeight()
						.background(accent),
				)
			}
			Column(modifier = Modifier.weight(1f)) { content() }
		}
	}
	if (onClick == null) {
		Surface(
			modifier = modifier,
			shape = MaterialTheme.shapes.medium,
			color = containerColor,
			contentColor = MaterialTheme.colorScheme.onSurface,
			border = BorderStroke(Dimens.hairline, borderColor),
			shadowElevation = Dimens.space2,
			content = cardContent,
		)
	} else {
		Surface(
			onClick = onClick,
			modifier = modifier,
			shape = MaterialTheme.shapes.medium,
			color = containerColor,
			contentColor = MaterialTheme.colorScheme.onSurface,
			border = BorderStroke(Dimens.hairline, borderColor),
			shadowElevation = Dimens.space2,
			content = cardContent,
		)
	}
}

@Composable
fun BrandIconTile(
	icon: ImageVector,
	contentDescription: String?,
	modifier: Modifier = Modifier,
	containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
	contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
	Surface(
		modifier = modifier.size(Dimens.iconTileSize),
		shape = MaterialTheme.shapes.small,
		color = containerColor,
		contentColor = contentColor,
	) {
		Box(contentAlignment = Alignment.Center) {
			Icon(
				imageVector = icon,
				contentDescription = contentDescription,
				modifier = Modifier.size(Dimens.iconSize),
			)
		}
	}
}

@Composable
fun BrandIconButton(
	icon: ImageVector,
	contentDescription: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	Surface(
		onClick = onClick,
		modifier = modifier.size(Dimens.listItemMinHeight),
		shape = MaterialTheme.shapes.small,
		color = MaterialTheme.colorScheme.secondaryContainer,
		contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
		border = BorderStroke(
			Dimens.hairline,
			MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
		),
	) {
		Box(contentAlignment = Alignment.Center) {
			Icon(
				imageVector = icon,
				contentDescription = contentDescription,
				modifier = Modifier.size(Dimens.iconSize),
			)
		}
	}
}

@Composable
fun BrandProgress(progress: () -> Float, modifier: Modifier = Modifier) {
	LinearProgressIndicator(
		progress = progress,
		modifier = modifier
			.fillMaxWidth()
			.height(Dimens.progressHeight)
			.clip(RoundedCornerShape(50)),
		color = MaterialTheme.colorScheme.primary,
		trackColor = MaterialTheme.colorScheme.secondaryContainer,
		strokeCap = StrokeCap.Round,
	)
}

@Composable
fun BrandEmptyState(
	icon: ImageVector,
	title: String,
	modifier: Modifier = Modifier,
	message: String? = null,
) {
	Column(
		modifier = modifier.padding(Dimens.space32),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(Dimens.space12),
	) {
		BrandIconTile(
			icon = icon,
			contentDescription = null,
			containerColor = MaterialTheme.colorScheme.secondaryContainer,
		)
		Text(
			text = title,
			style = MaterialTheme.typography.titleLarge,
			color = MaterialTheme.colorScheme.onBackground,
		)
		if (!message.isNullOrBlank()) {
			Text(
				text = message,
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
	}
}
