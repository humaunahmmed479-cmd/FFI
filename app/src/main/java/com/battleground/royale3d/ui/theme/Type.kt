package com.battleground.royale3d.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.battleground.royale3d.R

val RajdhaniFontFamily = FontFamily(
    Font(R.font.rajdhani, FontWeight.Bold)
)

val ChakraPetchFontFamily = FontFamily(
    Font(R.font.chakra_petch, FontWeight.SemiBold)
)

val PlusJakartaFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal)
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 46.sp,
        letterSpacing = 1.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.75.sp
    ),
    titleLarge = TextStyle(
        fontFamily = ChakraPetchFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleMedium = TextStyle(
        fontFamily = ChakraPetchFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = ChakraPetchFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp
    ),
    labelSmall = TextStyle(
        fontFamily = ChakraPetchFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)
