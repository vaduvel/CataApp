package com.emanus.lucrari.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Text mai mare decât implicit: se citește cu mâinile murdare, la lumină puternică,
// fără ochelari de citit.
val LucrariTypography = Typography(
	headlineMedium = TextStyle(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold),
	titleLarge = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
	titleMedium = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
	bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
	bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
	labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
)
