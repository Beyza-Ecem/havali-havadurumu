package com.example.firstproject


import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =====================================================================
//  UYGULAMA RENKLERİ (açık / koyu tema)
// =====================================================================

data class AppColors(
    val text: Color,    // ana yazı
    val sub: Color,     // ikincil yazı
    val card: Color,    // kart zemini
    val border: Color,  // ince çerçeve
    val accent: Color,  // vurgu (mavi)
    val chip: Color,    // yuvarlak buton / seçili sekme zemini
    val navBg: Color    // alt menü zemini
)

val DarkAppColors = AppColors(
    text = Color(0xFFFFFFFF),
    sub = Color(0xFF8B8FA3),
    card = Color(0xFF191B28).copy(alpha = 0.92f),
    border = Color.White.copy(alpha = 0.07f),
    accent = Color(0xFF60A5FA),
    chip = Color.White.copy(alpha = 0.10f),
    navBg = Color(0xFF1B1D2A).copy(alpha = 0.96f)
)

val LightAppColors = AppColors(
    text = Color(0xFF0F172A),
    sub = Color(0xFF475569),
    card = Color.White.copy(alpha = 0.80f),
    border = Color.Black.copy(alpha = 0.07f),
    accent = Color(0xFF2563EB),
    chip = Color.Black.copy(alpha = 0.08f),
    navBg = Color.White.copy(alpha = 0.92f)
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

object AppTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
}

// =====================================================================
//  HAVAYA GÖRE ARKA PLAN RENKLERİ
// =====================================================================

data class WeatherPalette(val top: Color, val bottom: Color, val glow: Color)

fun defaultPalette(dark: Boolean) =
    if (dark) WeatherPalette(Color(0xFF0B0C14), Color(0xFF14151F), Color(0xFF3B3F8F))
    else WeatherPalette(Color(0xFFBFD9F5), Color(0xFFEAF2FB), Color(0xFFFFFFFF))

fun weatherPalette(code: Int, isDay: Boolean, dark: Boolean): WeatherPalette = when {
    // Fırtına
    code in 95..99 ->
        if (dark) WeatherPalette(Color(0xFF2E1065), Color(0xFF0F0A1F), Color(0xFF7C3AED))
        else WeatherPalette(Color(0xFF9A8CC2), Color(0xFFE6E1F1), Color(0xFFC4B5FD))

    // Kar
    code in 71..77 || code == 85 || code == 86 ->
        if (dark) WeatherPalette(Color(0xFF3B6EA5), Color(0xFF1E3A5F), Color(0xFFBAE6FD))
        else WeatherPalette(Color(0xFF9CC4EE), Color(0xFFEAF3FC), Color(0xFFFFFFFF))

    // Yağmur / çisenti / sağanak
    code in 51..67 || code in 80..82 ->
        if (dark) {
            if (isDay) WeatherPalette(Color(0xFF334155), Color(0xFF0F172A), Color(0xFF3B82F6))
            else WeatherPalette(Color(0xFF0F172A), Color(0xFF020617), Color(0xFF1D4ED8))
        } else {
            if (isDay) WeatherPalette(Color(0xFF8FA9C4), Color(0xFFE3EAF2), Color(0xFF93C5FD))
            else WeatherPalette(Color(0xFF6E86A8), Color(0xFFD5DEEA), Color(0xFF93C5FD))
        }

    // Sis
    code == 45 || code == 48 ->
        if (dark) WeatherPalette(Color(0xFF64748B), Color(0xFF1E293B), Color(0xFFCBD5E1))
        else WeatherPalette(Color(0xFFB4C0CF), Color(0xFFEEF2F6), Color(0xFFFFFFFF))

    // Kapalı
    code == 3 ->
        if (dark) WeatherPalette(Color(0xFF475569), Color(0xFF1E293B), Color(0xFF94A3B8))
        else WeatherPalette(Color(0xFFA3B1C2), Color(0xFFE8ECF1), Color(0xFFE2E8F0))

    // Açık / az bulutlu
    else ->
        if (dark) {
            if (isDay) WeatherPalette(Color(0xFF3B82F6), Color(0xFF1E3A8A), Color(0xFFFBBF24))
            else WeatherPalette(Color(0xFF0B1030), Color(0xFF1E1B4B), Color(0xFF4C1D95))
        } else {
            if (isDay) WeatherPalette(Color(0xFF6EB8FF), Color(0xFFE8F5FF), Color(0xFFFFE08A))
            else WeatherPalette(Color(0xFF7F93CC), Color(0xFFDDE4F4), Color(0xFFC7D2FE))
        }
}
