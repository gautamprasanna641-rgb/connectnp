package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins, FontWeight.Bold)
)

val BrandHeaderStyle = TextStyle(
    fontFamily = PoppinsFontFamily,
    fontWeight = FontWeight.Black,
    fontSize = 22.sp,
    letterSpacing = 1.sp
)

val BrandSubHeaderStyle = TextStyle(
    fontFamily = PoppinsFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 18.sp,
    letterSpacing = 1.sp
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = BrandHeaderStyle,
    titleMedium = BrandSubHeaderStyle
)
