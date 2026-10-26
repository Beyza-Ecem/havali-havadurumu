package com.example.firstproject

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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

// =====================================================================
//  MODELLER
// =====================================================================

data class OutfitItem(val emoji: String, val name: String)

data class Outfit(
    val title: String,
    val items: List<OutfitItem>,
    val note: String,
    val personalized: Boolean = false
)

sealed interface OutfitState {
    data object Idle : OutfitState
    data object Loading : OutfitState
    data class Success(val outfit: Outfit) : OutfitState
}

// =====================================================================
//  YEREL KURAL MOTORU (internet ve anahtar gerektirmez)
// =====================================================================

object OutfitEngine {
    fun suggest(
        data: WeatherData,
        air: AirData? = null,
        profile: UserProfile = UserProfile()
    ): Outfit {
        val c = data.current
        // Profil: "üşürüm" ise hava 3° daha soğuk, "terlerim" ise 3° daha sıcak hissedilir
        val t = c.feelsLike + profile.sense.shift
        val code = c.weatherCode
        val rain = data.days.first().rain
        val isDay = c.isDay == 1

        val title: String
        val items = mutableListOf<OutfitItem>()
        when {
            t <= 0 -> {
                title = "Dondurucu soğuk: kat kat giyin"
                items += OutfitItem("🧥", "Kalın mont")
                items += OutfitItem("🧶", "Kazak + termal içlik")
                items += OutfitItem("🧣", "Atkı, bere ve eldiven")
                items += OutfitItem("🥾", "Astarlı bot")
            }
            t <= 8 -> {
                title = "Soğuk bir gün"
                items += OutfitItem("🧥", "Mont")
                items += OutfitItem("🧶", "Kazak")
                items += OutfitItem("👖", "Uzun pantolon")
                items += OutfitItem("🧣", "Atkı ve bere")
            }
            t <= 15 -> {
                title = "Serin hava: ceket şart"
                items += OutfitItem("🧥", "Ceket veya trençkot")
                items += OutfitItem("👕", "Uzun kollu tişört")
                items += OutfitItem("👖", "Kot pantolon")
                items += OutfitItem("👟", "Spor ayakkabı")
            }
            t <= 22 -> {
                title = "Ilık ve rahat bir gün"
                items += OutfitItem("🧥", "İnce hırka veya sweatshirt")
                items += OutfitItem("👕", "Tişört")
                items += OutfitItem("👖", "Kot veya chino")
                items += OutfitItem("👟", "Sneaker")
            }
            t <= 28 -> {
                title = "Sıcak: hafif giyin"
                items += OutfitItem("👕", "Pamuklu tişört")
                items += OutfitItem("🩳", "Şort veya keten pantolon")
                items += OutfitItem("👟", "Hafif sneaker")
            }
            else -> {
                title = "Çok sıcak: en hafif parçalar"
                items += OutfitItem("👕", "İnce, açık renk tişört")
                items += OutfitItem("🩳", "Şort")
                items += OutfitItem("🩴", "Sandalet")
                items += OutfitItem("🧢", "Şapka")
            }
        }

        val snow = code in 71..77 || code == 85 || code == 86
        val wet = code in 51..67 || code in 80..82 || rain >= 50
        if (wet) {
            if (profile.bike) items += OutfitItem("🧥", "Yağmurluk")
            else items += OutfitItem("☂️", "Şemsiye")
            if (t > 8) items += OutfitItem("🥾", "Su geçirmez ayakkabı")
        }
        if (snow) items += OutfitItem("🥾", "Kaymayan bot")
        // Bisikletli için rüzgâr eşiği daha düşük
        if (c.wind >= (if (profile.bike) 20 else 30)) items += OutfitItem("🌬️", "Rüzgârlık")
        if (isDay && data.uv >= 6) {
            items += OutfitItem("🕶️", "Güneş gözlüğü")
            items += OutfitItem("🧴", "Güneş kremi")
        }
        if (profile.bike) {
            items += OutfitItem("🪖", "Kask")
            if (t <= 10) items += OutfitItem("🧤", "Bisiklet eldiveni")
        }

        // Hava kalitesi ve polen: eşikler profile göre değişir
        val badAir = (air?.aqi ?: 0) >= (if (profile.asthma) 40 else 60)
        val highPollen = air?.pollen?.any { it.value >= (if (profile.allergy) 20.0 else 200.0) } == true
        if (badAir) items += OutfitItem("😷", "Maske")
        if (profile.allergy && highPollen) {
            items += OutfitItem("😷", "Maske")
            items += OutfitItem("🥽", "Koruyucu gözlük")
        }

        val diff = c.feelsLike - c.temperature
        val note = when {
            code in 95..99 -> "Fırtına var, mümkünse evde kal."
            wet ->
                if (profile.bike) "Yol ıslak olacak, bisikletle yavaş git ve mesafeni koru."
                else "Yağmur ihtimali yüksek, çantana şemsiye at."
            badAir ->
                if (profile.asthma) "Solunum hassasiyetin için hava riskli, yoğun efordan kaçın."
                else "Hava kalitesi kötü, yoğun dışarı aktivitesinden kaçın."
            highPollen ->
                if (profile.allergy) "Polen yüksek ve alerjin var: maske tak, eve gelince yüzünü yıka."
                else "Polen çok yüksek, hassassan dikkat et."
            profile.sense == TempSense.Cold -> "Çabuk üşüdüğün için normalden bir kat fazla önerdim."
            profile.sense == TempSense.Hot -> "Çabuk terlediğin için biraz daha hafif önerdim."
            diff <= -3 -> "Gerçek sıcaklıktan ${(-diff).toInt()}° daha soğuk hissettiriyor, bir kat fazla giy."
            diff >= 3 -> "Nem yüzünden daha sıcak hissettiriyor, bol su iç."
            else -> "Hissedilen sıcaklık ${c.feelsLike.toInt()}°, ona göre giyin."
        }
        return Outfit(
            title = title,
            items = items.distinctBy { it.name }.take(9),
            note = note,
            personalized = profile != UserProfile()
        )
    }
}

// =====================================================================
//  REPOSITORY (şimdilik sadece yerel kural motoru)
// =====================================================================

object OutfitRepository {
    suspend fun suggest(
        city: String,
        data: WeatherData,
        air: AirData? = null,
        profile: UserProfile = UserProfile()
    ): Outfit = OutfitEngine.suggest(data, air, profile)
}

// =====================================================================
//  ARAYÜZ
// =====================================================================

@Composable
fun OutfitSection(vm: CityViewModel, data: WeatherData) {
    val state by vm.outfit.collectAsState()
    val th = AppTheme.colors

    Spacer(Modifier.height(24.dp))
    Text("👕  Ne giysem?", color = th.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    DarkCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            when (val s = state) {
                is OutfitState.Idle -> {
                    Text(
                        "Bugünkü havaya göre sana özel bir kombin önereyim.",
                        color = th.sub,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.suggestOutfit(data) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) { Text("✨ Öneri al", color = Color.White) }
                }

                is OutfitState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = th.accent
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Kombin hazırlanıyor...", color = th.sub, fontSize = 14.sp)
                }

                is OutfitState.Success -> {
                    val o = s.outfit
                    Text(o.title, color = th.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    o.items.forEach {
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(it.emoji, fontSize = 22.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(it.name, color = th.text, fontSize = 15.sp)
                        }
                    }
                    if (o.note.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("💡 ${o.note}", color = th.sub, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (o.personalized) "👤 Profiline göre kişiselleştirildi"
                            else "🌤️ Hava durumuna göre öneri",
                            color = th.accent,
                            fontSize = 11.sp
                        )
                        Text(
                            "↻ Yenile",
                            color = th.text,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { vm.suggestOutfit(data) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}