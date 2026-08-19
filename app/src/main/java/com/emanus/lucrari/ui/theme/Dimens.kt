package com.emanus.lucrari.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Dimensiunile din design system. Se folosesc în locul numerelor scrise direct
 * în ecrane, ca țintele de atingere să rămână mari peste tot.
 */
object Dimens {
	val screenPadding = 16.dp
	val cardPadding = 16.dp
	val cardSpacing = 12.dp
	val listItemMinHeight = 56.dp
	val touchTargetMin = 48.dp
	val primaryButtonHeight = 56.dp
	val iconSize = 24.dp

	/** Spațiu liber sub liste, ca butonul plutitor să nu acopere ultimul rând. */
	val listBottomSpace = 96.dp
}
