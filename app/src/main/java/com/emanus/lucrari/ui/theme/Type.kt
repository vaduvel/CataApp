package com.emanus.lucrari.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Scara din design system-ul aprobat. Roboto vine de la sistem: zero fonturi în APK.
// Cifrele mari cer „tnum”, ca sumele să fie aliniate una sub alta în liste.
// Totul în sp, deci scalarea de sistem îl ajută dacă vede greu.
val LucrariTypography = Typography(
	displayLarge = TextStyle(
		fontSize = 40.sp,
		lineHeight = 44.sp,
		fontWeight = FontWeight.Bold,
		fontFeatureSettings = "tnum",
	),
	headlineLarge = TextStyle(
		fontSize = 32.sp,
		lineHeight = 38.sp,
		fontWeight = FontWeight.Bold,
		fontFeatureSettings = "tnum",
	),
	headlineMedium = TextStyle(
		fontSize = 28.sp,
		lineHeight = 34.sp,
		fontWeight = FontWeight.Bold,
		fontFeatureSettings = "tnum",
	),
	headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
	titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
	titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
	titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
	bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 25.sp),
	bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
	bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
	labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
	labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
	labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
)
