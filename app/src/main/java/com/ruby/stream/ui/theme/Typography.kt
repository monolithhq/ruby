package com.ruby.stream.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ruby.stream.R

// NOTE: Archivo Black is used ONLY for the logo wordmark (see ui/components/RubyWordmark.kt),
// never in body/UI typography. UI font is Inter throughout.
//
// Font files expected at: app/src/main/res/font/
//   inter_regular.ttf, inter_medium.ttf, inter_semibold.ttf, inter_bold.ttf
//   archivo_black.ttf   (wordmark only)

val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

val ArchivoBlackFontFamily = FontFamily(
    Font(R.font.archivo_black, FontWeight.Black)
)

// ── Scale (matches spec: Display/H1/H2/H3/Title/Body/Caption/Label) ──
object RubyType {
    val Display = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    )
    val H1 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp
    )
    val H2 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    )
    val H3 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    )
    val Title = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp
    )
    val Body = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )
    val Caption = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
    val Label = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
}

// Material3 Typography mapping (so default Compose components pick this up automatically)
val RubyMaterialTypography = Typography(
    displayLarge = RubyType.Display,
    headlineLarge = RubyType.H1,
    headlineMedium = RubyType.H2,
    headlineSmall = RubyType.H3,
    titleMedium = RubyType.Title,
    bodyLarge = RubyType.Body,
    bodySmall = RubyType.Caption,
    labelMedium = RubyType.Label
)
