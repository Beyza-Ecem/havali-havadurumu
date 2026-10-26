package com.example.firstproject

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.annotations.SerializedName
import kotlin.math.roundToInt
import retrofit2.http.GET
import retrofit2.http.Query

// =====================================================================
//  API (Open-Meteo Air Quality · veri: CAMS)
//  Polen verisi sadece Avrupa kapsamında gelir; yoksa alanlar null olur.
// =====================================================================

data class AirResponse(val current: AirCurrentDto?)

data class AirCurrentDto(
    @SerializedName("european_aqi") val aqi: Double? = null,
    @SerializedName("pm2_5") val pm25: Double? = null,
    @SerializedName("pm10") val pm10: Double? = null,
    @SerializedName("grass_pollen") val grass: Double? = null,
    @SerializedName("birch_pollen") val birch: Double? = null,
    @SerializedName("olive_pollen") val olive: Double? = null,
    @SerializedName("ragweed_pollen") val ragweed: Double? = null,
    @SerializedName("mugwort_pollen") val mugwort: Double? = null,
    @SerializedName("alder_pollen") val alder: Double? = null
)

interface AirApi {
    @GET("air-quality")
    suspend fun getAir(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String,
        @Query("timezone") timezone: String
    ): AirResponse
}

// =====================================================================
//  UYGULAMA MODELİ
// =====================================================================

data class PollenItem(val name: String, val emoji: String, val value: Double)

data class AirData(
    val aqi: Int?,
    val pm25: Double?,
    val pm10: Double?,
    val pollen: List<PollenItem>
)

sealed interface AirState {
    data object Loading : AirState
    data class Success(val data: AirData, val cachedAt: Long? = null) : AirState
    data object Unavailable : AirState
}

fun AirCurrentDto.toAirData(): AirData? {
    if (aqi == null && pm25 == null && pm10 == null) return null
    val pollen = listOfNotNull(
        grass?.let { PollenItem("Çimen", "🌾", it) },
        birch?.let { PollenItem("Huş", "🌳", it) },
        olive?.let { PollenItem("Zeytin", "🫒", it) },
        ragweed?.let { PollenItem("Ambrozya", "🌿", it) },
        mugwort?.let { PollenItem("Pelin otu", "🌼", it) },
        alder?.let { PollenItem("Kızılağaç", "🍂", it) }
    )
    return AirData(aqi?.roundToInt(), pm25, pm10, pollen)
}

// =====================================================================
//  SEVİYELER (Avrupa hava kalitesi indeksi; polen eşikleri yaklaşıktır)
// =====================================================================

data class Level(val label: String, val color: Color, val tip: String = "")

fun aqiLevel(aqi: Int): Level = when {
    aqi < 20 -> Level("Çok iyi", Color(0xFF22C55E), "Hava tertemiz, rahatça dışarı çıkabilirsin.")
    aqi < 40 -> Level("İyi", Color(0xFF84CC16), "Hava kalitesi iyi, normal planına devam.")
    aqi < 60 -> Level("Orta", Color(0xFFFACC15), "Hassas kişiler uzun süre dışarıda kalmasın.")
    aqi < 80 -> Level("Kötü", Color(0xFFF97316), "Yoğun egzersizi ertele, hassassan maske tak.")
    aqi < 100 -> Level("Çok kötü", Color(0xFFEF4444), "Mümkünse dışarı çıkma, pencereleri kapalı tut.")
    else -> Level("Aşırı kötü", Color(0xFFA855F7), "Dışarı çıkma, çıkacaksan maske şart.")
}

fun pollenLevel(grains: Double): Level = when {
    grains < 1 -> Level("Yok", Color(0xFF94A3B8))
    grains < 20 -> Level("Düşük", Color(0xFF22C55E))
    grains < 50 -> Level("Orta", Color(0xFFFACC15))
    grains < 200 -> Level("Yüksek", Color(0xFFF97316))
    else -> Level("Çok yüksek", Color(0xFFEF4444))
}

// =====================================================================
//  ARAYÜZ
// =====================================================================

@Composable
fun AirSection(vm: CityViewModel) {
    val th = AppTheme.colors
    val state by vm.air.collectAsState()
    val success = state as? AirState.Success ?: return
    val data = success.data

    Spacer(Modifier.height(24.dp))
    Text(
        "🌫️  Hava kalitesi ve polen",
        color = th.text,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(12.dp))
    DarkCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            data.aqi?.let { aqi ->
                val lv = aqiLevel(aqi)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(lv.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$aqi",
                            color = Color(0xFF0F172A),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(lv.label, color = th.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Avrupa hava kalitesi indeksi", color = th.sub, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(lv.tip, color = th.text.copy(alpha = 0.85f), fontSize = 14.sp)
            }

            if (data.pm25 != null && data.pm10 != null) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Stat("🔬", "${data.pm25.roundToInt()} µg", "PM2.5", Modifier.weight(1f))
                    VDivider()
                    Stat("🌫️", "${data.pm10.roundToInt()} µg", "PM10", Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("Polen", color = th.sub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            if (data.pollen.isEmpty()) {
                Text(
                    "Bu bölge için polen verisi yok (polen sadece Avrupa kapsamında).",
                    color = th.sub,
                    fontSize = 12.sp
                )
            } else {
                data.pollen.sortedByDescending { it.value }.take(4).forEach { p ->
                    val lv = pollenLevel(p.value)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p.emoji, fontSize = 18.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(p.name, color = th.text, fontSize = 14.sp)
                        }
                        Text(
                            lv.label,
                            color = Color(0xFF0F172A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(lv.color)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                if (success.cachedAt != null)
                    "📴 Çevrimdışı kayıt · ${fmtUpdated(success.cachedAt)} · Open-Meteo · CAMS"
                else
                    "Veri: Open-Meteo · CAMS",
                color = th.sub,
                fontSize = 10.sp
            )
        }
    }
}