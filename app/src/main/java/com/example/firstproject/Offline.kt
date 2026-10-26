package com.example.firstproject

import android.content.SharedPreferences
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// =====================================================================
//  ÇEVRİMDIŞI ÖNBELLEK
//  Başarılı her hava cevabı şehir bazında telefona kaydedilir.
//  İnternet yokken boş hata ekranı yerine son kayıt gösterilir.
// =====================================================================

class WeatherCache(private val prefs: SharedPreferences) {
    private val gson = Gson()

    fun save(city: City, data: WeatherData) {
        prefs.edit()
            .putString("cache_${city.key()}", gson.toJson(data))
            .putLong("cache_t_${city.key()}", System.currentTimeMillis())
            .apply()
    }

    /** Kayıt varsa (veri, kaydedilme zamanı) döner. */
    fun load(city: City): Pair<WeatherData, Long>? {
        val json = prefs.getString("cache_${city.key()}", null) ?: return null
        val time = prefs.getLong("cache_t_${city.key()}", 0L)
        return try {
            gson.fromJson(json, WeatherData::class.java)?.let { it to time }
        } catch (e: Exception) {
            null
        }
    }

    // ----- Hava kalitesi ve polen -----

    fun saveAir(city: City, data: AirData) {
        prefs.edit()
            .putString("air_${city.key()}", gson.toJson(data))
            .putLong("air_t_${city.key()}", System.currentTimeMillis())
            .apply()
    }

    fun loadAir(city: City): Pair<AirData, Long>? {
        val json = prefs.getString("air_${city.key()}", null) ?: return null
        val time = prefs.getLong("air_t_${city.key()}", 0L)
        return try {
            gson.fromJson(json, AirData::class.java)?.let { it to time }
        } catch (e: Exception) {
            null
        }
    }

    // ----- Şehir listesindeki mini kartlar -----

    fun saveMini(city: City, mini: MiniWeather) {
        prefs.edit().putString("mini_${city.key()}", gson.toJson(mini)).apply()
    }

    fun loadMini(city: City): MiniWeather? {
        val json = prefs.getString("mini_${city.key()}", null) ?: return null
        return try {
            gson.fromJson(json, MiniWeather::class.java)
        } catch (e: Exception) {
            null
        }
    }
}

/** Bugünse "14:32", başka günse "25 Eki 14:32" */
fun fmtUpdated(timeMillis: Long): String {
    val locale = Locale("tr", "TR")
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timeMillis }
    val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    val pattern = if (sameDay) "HH:mm" else "d MMM HH:mm"
    return SimpleDateFormat(pattern, locale).format(Date(timeMillis))
}

@Composable
fun OfflineBanner(updatedAt: Long, onRetry: () -> Unit) {
    val th = AppTheme.colors
    val amber = Color(0xFFF59E0B)
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .clip(shape)
            .background(amber.copy(alpha = 0.18f))
            .border(1.dp, amber.copy(alpha = 0.45f), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "📴 Çevrimdışı · Son güncelleme ${fmtUpdated(updatedAt)}",
            color = th.text,
            fontSize = 12.sp
        )
        Text(
            "Yenile",
            color = th.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { onRetry() }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}