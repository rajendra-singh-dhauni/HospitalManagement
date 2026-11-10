package com.mobile360.healthcare.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Design tokens use Roboto Flex; the platform default sans is the closest installed face.
private val Family = FontFamily.Default

private fun style(size: Int, line: Int, weight: FontWeight) =
    TextStyle(fontFamily = Family, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

val AppTypography = Typography(
    displaySmall = style(32, 40, FontWeight.Medium),
    headlineSmall = style(24, 32, FontWeight.Medium),
    headlineMedium = style(24, 32, FontWeight.Medium),
    titleLarge = style(20, 28, FontWeight.Medium),
    titleMedium = style(16, 24, FontWeight.Medium),
    titleSmall = style(14, 20, FontWeight.Medium),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 20, FontWeight.Normal),
    bodySmall = style(12, 16, FontWeight.Normal),
    labelLarge = style(14, 20, FontWeight.Medium),
    labelMedium = style(12, 16, FontWeight.Medium),
    labelSmall = style(12, 16, FontWeight.Medium)
)
