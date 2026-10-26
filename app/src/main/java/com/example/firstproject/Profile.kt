package com.example.firstproject

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =====================================================================
//  KİŞİSEL PROFİL
//  Aynı hava, farklı kişiye farklı öneri: kıyafet, uyarılar ve sesli özet
//  bu profile göre değişir. Seçimler telefonda saklanır.
// =====================================================================

enum class TempSense(val shift: Double) {
    Normal(0.0),   // olduğu gibi
    Cold(-3.0),    // çabuk üşürüm: hava 3° daha soğuk hissedilir
    Hot(3.0)       // çabuk terlerim: hava 3° daha sıcak hissedilir
}

data class UserProfile(
    val sense: TempSense = TempSense.Normal,
    val allergy: Boolean = false,   // polen uyarısı eşiği düşer
    val asthma: Boolean = false,    // hava kalitesi uyarı eşiği düşer
    val bike: Boolean = false       // kask, rüzgâr ve yağmur önerileri
)

@Composable
fun ProfileSection(vm: CityViewModel) {
    val th = AppTheme.colors
    val p by vm.profile.collectAsState()

    Spacer(Modifier.height(24.dp))
    Text("👤  Benim profilim", color = th.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    DarkCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Önerilerin ve uyarıların sana göre değişir.", color = th.sub, fontSize = 13.sp)

            Spacer(Modifier.height(14.dp))
            Text("Sıcaklık hassasiyeti", color = th.sub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileChip("Normal", p.sense == TempSense.Normal, Modifier.weight(1f)) {
                    vm.setSense(TempSense.Normal)
                }
                ProfileChip("🥶 Üşürüm", p.sense == TempSense.Cold, Modifier.weight(1f)) {
                    vm.setSense(TempSense.Cold)
                }
                ProfileChip("🥵 Terlerim", p.sense == TempSense.Hot, Modifier.weight(1f)) {
                    vm.setSense(TempSense.Hot)
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("Bana uyanlar", color = th.sub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileChip("🌼 Alerji", p.allergy, Modifier.weight(1f)) { vm.toggleAllergy() }
                ProfileChip("🫁 Solunum", p.asthma, Modifier.weight(1f)) { vm.toggleAsthma() }
                ProfileChip("🚲 Bisiklet", p.bike, Modifier.weight(1f)) { vm.toggleBike() }
            }
        }
    }
}

@Composable
private fun ProfileChip(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val th = AppTheme.colors
    val shape = RoundedCornerShape(50)
    val blue = Color(0xFF2563EB)
    Text(
        label,
        color = if (selected) Color.White else th.text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .clip(shape)
            .background(if (selected) blue else th.chip)
            .border(1.dp, if (selected) blue else th.border, shape)
            .clickable { onClick() }
            .padding(vertical = 9.dp)
    )
}