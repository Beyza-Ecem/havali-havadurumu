package com.example.firstproject

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstproject.ui.theme.FirstprojectTheme
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// =====================================================================
//  ACTIVITY
// =====================================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Lifecycle", "onCreate")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            val vm: CityViewModel = viewModel()
            val weather by vm.weather.collectAsState()
            val dark by vm.darkTheme.collectAsState()
            val colors = if (dark) DarkAppColors else LightAppColors

            // Açık temada durum çubuğu simgeleri koyu, koyu temada açık olsun
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    )
                }
                this@MainActivity.enableEdgeToEdge(
                    statusBarStyle = style,
                    navigationBarStyle = style
                )
            }

            FirstprojectTheme {
                CompositionLocalProvider(LocalAppColors provides colors) {
                    val success = weather as? WeatherState.Success

                    // Arka plan havaya ve temaya göre renk değiştirir
                    val palette = success?.let {
                        weatherPalette(it.data.current.weatherCode, it.data.current.isDay == 1, dark)
                    } ?: defaultPalette(dark)
                    val top by animateColorAsState(palette.top, tween(900), label = "top")
                    val bottom by animateColorAsState(palette.bottom, tween(900), label = "bottom")
                    val glow by animateColorAsState(palette.glow, tween(900), label = "glow")

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawRect(Brush.verticalGradient(listOf(top, bottom)))
                                drawRect(
                                    Brush.radialGradient(
                                        listOf(glow.copy(alpha = 0.55f), Color.Transparent),
                                        center = Offset(size.width / 2f, size.height * 0.22f),
                                        radius = size.width * 1.1f
                                    )
                                )
                            }
                    ) {
                        // Yağmur, kar, güneş ışınları vb. (içeriğin arkasında)
                        if (success != null) {
                            WeatherEffects(
                                code = success.data.current.weatherCode,
                                isDay = success.data.current.isDay == 1,
                                dark = dark
                            )
                        }
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = Color.Transparent
                        ) { inner -> AppRoot(vm, Modifier.padding(inner)) }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("Lifecycle", "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("Lifecycle", "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d("Lifecycle", "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("Lifecycle", "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Lifecycle", "onDestroy")
    }
}

// =====================================================================
//  YARDIMCILAR
// =====================================================================

fun describeWeather(code: Int, isDay: Boolean = true): Pair<String, String> = when (code) {
    0 -> (if (isDay) "☀️" else "🌙") to "Açık"
    1, 2 -> (if (isDay) "🌤️" else "☁️") to "Az bulutlu"
    3 -> "☁️" to "Kapalı"
    45, 48 -> "🌫️" to "Sisli"
    in 51..57 -> "🌦️" to "Çisenti"
    in 61..67 -> "🌧️" to "Yağmurlu"
    in 71..77 -> "❄️" to "Karlı"
    in 80..82 -> "🌧️" to "Sağanak yağış"
    85, 86 -> "🌨️" to "Kar sağanağı"
    in 95..99 -> "⛈️" to "Gök gürültülü fırtına"
    else -> "🌡️" to "Bilinmiyor"
}

fun tempNum(celsius: Double, useF: Boolean): String =
    if (useF) (celsius * 9 / 5 + 32).roundToInt().toString() else celsius.roundToInt().toString()

fun fmtTemp(celsius: Double, useF: Boolean): String = "${tempNum(celsius, useF)}°"

fun tipFor(code: Int, temp: Double, wind: Double, uv: Double): String = when {
    code in 95..99 -> "Fırtına var, mümkünse dışarı çıkma."
    code in 51..67 || code in 80..82 -> "Şemsiyeni al, yağmur bekleniyor."
    code in 71..77 || code == 85 || code == 86 -> "Kar var, kalın giyin ve dikkatli yürü."
    wind >= 35 -> "Rüzgâr sert, hafif eşyalara dikkat."
    temp <= 5 -> "Çok soğuk, mont ve atkı şart."
    temp <= 15 -> "Serin, yanına ceket al."
    temp >= 30 -> "Çok sıcak, bol su iç, gölgede kal."
    uv >= 6 -> "UV yüksek, güneş kremi sür."
    else -> "Hava harika, dışarı çıkma zamanı!"
}

fun dayLabel(date: String, index: Int): String {
    if (index == 0) return "Bugün"
    return try {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
        SimpleDateFormat("EEEE", Locale("tr", "TR")).format(d!!)
    } catch (e: Exception) {
        date
    }
}

fun City.key() = "$latitude,$longitude"

// =====================================================================
//  ÇİZİLMİŞ 3D HAVA İKONLARI (ekstra kütüphane yok)
// =====================================================================

@Composable
fun WeatherIcon(code: Int, isDay: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        when {
            code == 0 ->
                if (isDay) drawSun(s * 0.5f, s * 0.5f, s * 0.26f)
                else drawMoon(s * 0.5f, s * 0.5f, s * 0.26f)

            code == 1 || code == 2 -> {
                if (isDay) drawSun(s * 0.64f, s * 0.34f, s * 0.2f)
                else drawMoon(s * 0.62f, s * 0.34f, s * 0.19f)
                drawCloud(s * 0.08f, s * 0.3f, s * 0.84f, s * 0.5f)
            }

            code == 45 || code == 48 -> {
                drawCloud(s * 0.08f, s * 0.2f, s * 0.84f, s * 0.5f)
                drawFog(s)
            }

            code in 51..67 || code in 80..82 -> {
                if (isDay) drawSun(s * 0.66f, s * 0.26f, s * 0.17f)
                else drawMoon(s * 0.64f, s * 0.26f, s * 0.16f)
                drawCloud(s * 0.08f, s * 0.2f, s * 0.84f, s * 0.48f)
                drawRain(s)
            }

            code in 71..77 || code == 85 || code == 86 -> {
                drawCloud(s * 0.08f, s * 0.18f, s * 0.84f, s * 0.48f)
                drawSnow(s)
            }

            code in 95..99 -> {
                drawCloud(s * 0.08f, s * 0.16f, s * 0.84f, s * 0.48f)
                drawBolt(s)
            }

            else -> drawCloud(s * 0.08f, s * 0.28f, s * 0.84f, s * 0.54f)
        }
    }
}

private fun DrawScope.drawSun(cx: Float, cy: Float, r: Float) {
    val c = Offset(cx, cy)
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xFFFFB347).copy(alpha = 0.45f), Color.Transparent),
            center = c, radius = r * 2.2f
        ),
        radius = r * 2.2f, center = c
    )
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xFFFFE29A), Color(0xFFFF9A3C)),
            center = Offset(cx - r * 0.35f, cy - r * 0.35f), radius = r * 1.7f
        ),
        radius = r, center = c
    )
}

private fun DrawScope.drawMoon(cx: Float, cy: Float, r: Float) {
    val c = Offset(cx, cy)
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xFFFFE9A8).copy(alpha = 0.25f), Color.Transparent),
            center = c, radius = r * 2f
        ),
        radius = r * 2f, center = c
    )
    val cut = Path().apply {
        addOval(Rect(center = Offset(cx + r * 0.5f, cy - r * 0.35f), radius = r * 0.85f))
    }
    clipPath(cut, clipOp = ClipOp.Difference) {
        drawCircle(
            Brush.linearGradient(
                listOf(Color(0xFFFFF1C1), Color(0xFFFFC857)),
                start = Offset(cx - r, cy - r), end = Offset(cx + r, cy + r)
            ),
            radius = r, center = c
        )
    }
}

private fun DrawScope.drawCloud(x: Float, y: Float, w: Float, h: Float) {
    // Açık temada beyaz bulut kaybolmasın diye hafif gölge
    val shadow = Color(0x26000000)
    val dy = h * 0.06f
    drawRoundRect(
        shadow,
        topLeft = Offset(x, y + h * 0.45f + dy),
        size = Size(w, h * 0.55f),
        cornerRadius = CornerRadius(h * 0.275f)
    )
    drawCircle(shadow, radius = h * 0.3f, center = Offset(x + w * 0.3f, y + h * 0.55f + dy))
    drawCircle(shadow, radius = h * 0.42f, center = Offset(x + w * 0.58f, y + h * 0.42f + dy))

    val b = Brush.verticalGradient(
        listOf(Color.White, Color(0xFFCBD5E6)),
        startY = y, endY = y + h
    )
    drawRoundRect(
        b,
        topLeft = Offset(x, y + h * 0.45f),
        size = Size(w, h * 0.55f),
        cornerRadius = CornerRadius(h * 0.275f)
    )
    drawCircle(b, radius = h * 0.3f, center = Offset(x + w * 0.3f, y + h * 0.55f))
    drawCircle(b, radius = h * 0.42f, center = Offset(x + w * 0.58f, y + h * 0.42f))
}

private fun DrawScope.drawRain(s: Float) {
    listOf(0.34f, 0.5f, 0.66f).forEachIndexed { i, fx ->
        val off = if (i == 1) 0.04f else 0f
        drawLine(
            color = Color(0xFF3B82F6),
            start = Offset(s * fx, s * (0.58f + off)),
            end = Offset(s * (fx - 0.05f), s * (0.8f + off)),
            strokeWidth = s * 0.075f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSnow(s: Float) {
    listOf(
        0.32f to 0.66f, 0.5f to 0.74f, 0.68f to 0.66f,
        0.4f to 0.84f, 0.6f to 0.86f
    ).forEach { (fx, fy) ->
        drawCircle(Color(0xFFBFE3FF), radius = s * 0.035f, center = Offset(s * fx, s * fy))
    }
}

private fun DrawScope.drawFog(s: Float) {
    listOf(0.72f to 0.7f, 0.82f to 0.5f).forEach { (fy, fw) ->
        drawLine(
            color = Color(0xFFB8C2D6),
            start = Offset(s * (0.5f - fw / 2f), s * fy),
            end = Offset(s * (0.5f + fw / 2f), s * fy),
            strokeWidth = s * 0.05f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawBolt(s: Float) {
    drawCircle(
        Color(0xFFFFD60A).copy(alpha = 0.2f),
        radius = s * 0.22f, center = Offset(s * 0.52f, s * 0.72f)
    )
    val p = Path().apply {
        moveTo(s * 0.56f, s * 0.52f)
        lineTo(s * 0.40f, s * 0.76f)
        lineTo(s * 0.51f, s * 0.76f)
        lineTo(s * 0.44f, s * 0.96f)
        lineTo(s * 0.64f, s * 0.68f)
        lineTo(s * 0.53f, s * 0.68f)
        lineTo(s * 0.62f, s * 0.52f)
        close()
    }
    drawPath(p, Color(0xFFFFD60A))
}

// =====================================================================
//  KÖK EKRAN + ALT MENÜ
// =====================================================================

@Composable
fun AppRoot(vm: CityViewModel, modifier: Modifier = Modifier) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = tab != 0) { tab = 0 }

    Box(modifier = modifier.fillMaxSize()) {
        when (tab) {
            0 -> HomeTab(vm)
            1 -> SearchTab(vm, onPicked = { tab = 0 })
            else -> FavoritesTab(vm, onPicked = { tab = 0 })
        }
        BottomNav(tab, { tab = it }, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun BottomNav(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val th = AppTheme.colors
    val items = listOf("🏠" to "Ana", "🔍" to "Ara", "⭐" to "Favoriler")
    val shape = RoundedCornerShape(32.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 14.dp)
            .clip(shape)
            .background(th.navBg)
            .border(1.dp, th.border, shape)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEachIndexed { i, (emoji, label) ->
            val sel = i == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (sel) th.chip else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(emoji, fontSize = 20.sp, modifier = Modifier.alpha(if (sel) 1f else 0.55f))
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                    color = if (sel) th.text else th.sub
                )
            }
        }
    }
}

// =====================================================================
//  ANA SEKME
// =====================================================================

@Composable
fun HomeTab(vm: CityViewModel) {
    val th = AppTheme.colors
    val city by vm.currentCity.collectAsState()
    val weather by vm.weather.collectAsState()
    val useF by vm.useFahrenheit.collectAsState()
    val dark by vm.darkTheme.collectAsState()
    val favs by vm.favorites.collectAsState()
    val isFav = favs.any { it.key() == city.key() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 110.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoundBtn(if (isFav) "⭐" else "☆") { vm.toggleFavorite(city) }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📍 ${city.name}", color = th.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                city.admin1?.let { Text(it, color = th.sub, fontSize = 13.sp) }
                if (weather is WeatherState.Loading) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "● Güncelleniyor",
                        color = th.text.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, th.border, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
            RoundBtn(if (dark) "☀️" else "🌙") { vm.toggleTheme() }
            Spacer(Modifier.width(8.dp))
            RoundBtn(if (useF) "°F" else "°C") { vm.toggleUnit() }
        }

        when (val w = weather) {
            is WeatherState.Loading -> Box(
                modifier = Modifier.fillMaxWidth().height(420.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = th.accent) }

            is WeatherState.Error -> Column(
                modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📡", fontSize = 56.sp)
                Spacer(Modifier.height(8.dp))
                Text("Hava durumu alınamadı", color = th.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("İnternetini kontrol et", color = th.sub)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { vm.retryWeather() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) { Text("Tekrar dene", color = Color.White) }
            }

            is WeatherState.Success -> {
                w.offlineSince?.let { OfflineBanner(it) { vm.retryWeather() } }
                HomeContent(w.data, useF, vm)
            }
        }
    }
}

@Composable
fun RoundBtn(text: String, onClick: () -> Unit) {
    val th = AppTheme.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(th.chip)
            .border(1.dp, th.border, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(text, color = th.text, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun HomeContent(data: WeatherData, useF: Boolean, vm: CityViewModel) {
    val th = AppTheme.colors
    val cur = data.current
    val isDay = cur.isDay == 1
    val desc = describeWeather(cur.weatherCode, isDay).second

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        WeatherIcon(cur.weatherCode, isDay, Modifier.size(210.dp))
        Row(verticalAlignment = Alignment.Top) {
            Text(
                tempNum(cur.temperature, useF),
                color = th.text,
                fontSize = 96.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                if (useF) "°F" else "°C",
                color = th.text,
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 20.dp)
            )
        }
        Text(desc, color = th.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            tipFor(cur.weatherCode, cur.temperature, cur.wind, data.uv),
            color = th.text.copy(alpha = 0.8f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Hissedilen ${fmtTemp(cur.feelsLike, useF)}  •  " +
                    "↑ ${fmtTemp(data.days.first().max, useF)}  ↓ ${fmtTemp(data.days.first().min, useF)}",
            color = th.sub,
            fontSize = 13.sp
        )
    }

    Spacer(Modifier.height(22.dp))

    // Rüzgâr / Nem / UV
    DarkCard {
        Row(
            modifier = Modifier.padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Stat("💨", "${cur.wind.roundToInt()} km/sa", "Rüzgâr", Modifier.weight(1f))
            VDivider()
            Stat("💧", "%${cur.humidity}", "Nem", Modifier.weight(1f))
            VDivider()
            Stat("🕶️", "${data.uv.roundToInt()}", "UV", Modifier.weight(1f))
        }
    }

    Spacer(Modifier.height(12.dp))

    // Gün doğumu / batımı / yağış
    DarkCard {
        Row(
            modifier = Modifier.padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Stat("🌅", data.sunrise, "Gün doğumu", Modifier.weight(1f))
            VDivider()
            Stat("🌇", data.sunset, "Gün batımı", Modifier.weight(1f))
            VDivider()
            Stat("☔", "%${data.days.first().rain}", "Yağış", Modifier.weight(1f))
        }
    }

    // Hava kalitesi ve polen
    AirSection(vm)

    // Kişisel profil
    ProfileSection(vm)

    // Kıyafet önerisi
    OutfitSection(vm, data)

    Spacer(Modifier.height(24.dp))

    // Saatlik
    Text("🕐  Saatlik tahmin", color = th.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(data.hours) { i, h -> HourCard(h, i == 0, isDay, useF) }
    }

    Spacer(Modifier.height(24.dp))

    // 7 günlük
    Text("📅  7 günlük tahmin", color = th.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    DarkCard {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            data.days.forEach { d ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(d.label, color = th.sub, fontSize = 14.sp, modifier = Modifier.width(84.dp))
                    WeatherIcon(d.code, true, Modifier.size(34.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(describeWeather(d.code).second, color = th.text, fontSize = 14.sp)
                        if (d.rain >= 20) {
                            Text("💧 %${d.rain}", color = th.accent, fontSize = 11.sp)
                        }
                    }
                    Text(fmtTemp(d.max, useF), color = th.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(fmtTemp(d.min, useF), color = th.sub, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun HourCard(h: HourItem, highlight: Boolean, isDay: Boolean, useF: Boolean) {
    val th = AppTheme.colors
    val shape = RoundedCornerShape(22.dp)
    val dayH = if (h.label == "Şimdi") isDay else (h.label.take(2).toIntOrNull() ?: 12) in 6..19
    val bg: Brush =
        if (highlight) Brush.verticalGradient(listOf(Color(0xFF4F9BFF), Color(0xFF2563EB)))
        else SolidColor(th.card)

    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, th.border, shape)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            h.label,
            color = if (highlight) Color.White else th.sub,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        WeatherIcon(h.code, dayH, Modifier.size(40.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            fmtTemp(h.temp, useF),
            color = if (highlight) Color.White else th.text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        if (h.rain >= 20) {
            Text("💧${h.rain}%", color = if (highlight) Color.White else th.accent, fontSize = 10.sp)
        } else {
            Spacer(Modifier.height(13.dp))
        }
    }
}

@Composable
fun DarkCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val th = AppTheme.colors
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(th.card)
            .border(1.dp, th.border, shape)
    ) { content() }
}

@Composable
fun Stat(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    val th = AppTheme.colors
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = th.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(label, color = th.sub, fontSize = 11.sp)
    }
}

@Composable
fun VDivider() {
    val th = AppTheme.colors
    Box(modifier = Modifier.width(1.dp).height(44.dp).background(th.border))
}

// =====================================================================
//  ARAMA SEKMESİ
// =====================================================================

@Composable
fun SearchTab(vm: CityViewModel, onPicked: () -> Unit) {
    val th = AppTheme.colors
    val query by vm.searchQuery.collectAsState()
    val state by vm.state.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val mini by vm.mini.collectAsState()
    val useF by vm.useFahrenheit.collectAsState()
    val current by vm.currentCity.collectAsState()

    val shown = (state as? LoadState.Success)?.cities.orEmpty()
    LaunchedEffect(shown) { vm.loadMini(shown) }

    Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
        Text(
            "Şehir ara",
            color = th.text,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Text(
            "Türkiye'nin il merkezleri",
            color = th.sub,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = { vm.updateSearchQuery(it) },
            placeholder = { Text("Şehir ara...", color = th.sub) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Text(
                        "✕",
                        fontSize = 18.sp,
                        color = th.text,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { vm.updateSearchQuery("") }
                            .padding(12.dp)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = th.text,
                unfocusedTextColor = th.text,
                cursorColor = th.accent,
                focusedContainerColor = th.card,
                unfocusedContainerColor = th.card,
                focusedBorderColor = th.accent,
                unfocusedBorderColor = th.border
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)
        )

        when (val s = state) {
            is LoadState.Loading -> CenterBox {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = th.accent)
                    Spacer(Modifier.height(12.dp))
                    Text("Şehirler aranıyor...", color = th.sub)
                }
            }

            is LoadState.Success -> {
                Text(
                    if (query.isBlank()) "🏙️ Popüler şehirler" else "${s.cities.size} şehir bulundu",
                    color = th.text,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 24.dp, bottom = 6.dp)
                )
                LazyColumn(contentPadding = PaddingValues(bottom = 110.dp)) {
                    itemsIndexed(s.cities) { _, c ->
                        CityRow(
                            c,
                            favorites.any { it.key() == c.key() },
                            mini[c.key()],
                            useF,
                            selected = c.key() == current.key(),
                            onFav = { vm.toggleFavorite(c) },
                            onClick = { vm.selectCity(c); onPicked() }
                        )
                    }
                }
            }

            is LoadState.Empty -> CenterBox {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 56.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Şehir bulunamadı", color = th.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Başka bir isim dene", color = th.sub)
                }
            }

            is LoadState.Error -> CenterBox {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📡", fontSize = 56.sp)
                    Text("Bir sorun oluştu", color = th.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(s.message, color = th.sub)
                    Button(
                        onClick = { vm.retry() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) { Text("Tekrar dene", color = Color.White) }
                }
            }
        }
    }
}

// =====================================================================
//  FAVORİLER SEKMESİ
// =====================================================================

@Composable
fun FavoritesTab(vm: CityViewModel, onPicked: () -> Unit) {
    val th = AppTheme.colors
    val favorites by vm.favorites.collectAsState()
    val mini by vm.mini.collectAsState()
    val useF by vm.useFahrenheit.collectAsState()
    val current by vm.currentCity.collectAsState()

    LaunchedEffect(favorites) { vm.loadMini(favorites) }

    Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
        Text(
            "Favorilerim",
            color = th.text,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Text(
            "Sık baktığın şehirler",
            color = th.sub,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 24.dp, bottom = 16.dp)
        )

        if (favorites.isEmpty()) {
            CenterBox {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⭐", fontSize = 56.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Henüz favorin yok", color = th.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Bir şehrin yanındaki ☆'ya bas", color = th.sub)
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 110.dp)) {
                itemsIndexed(favorites) { _, c ->
                    CityRow(
                        c, true, mini[c.key()], useF,
                        selected = c.key() == current.key(),
                        onFav = { vm.toggleFavorite(c) },
                        onClick = { vm.selectCity(c); onPicked() }
                    )
                }
            }
        }
    }
}

@Composable
fun CenterBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
fun CityRow(
    city: City,
    isFav: Boolean,
    mini: MiniWeather?,
    useF: Boolean,
    onFav: () -> Unit,
    onClick: () -> Unit,
    selected: Boolean = false
) {
    val th = AppTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(shape)
            .background(th.card)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) th.accent else th.border,
                shape
            )
            .clickable {
                Log.d("HavaDurumuApp", "Tıklandı: ${city.name}")
                onClick()
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (mini != null) {
                WeatherIcon(mini.code, mini.isDay, Modifier.size(48.dp))
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = th.accent
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(city.name, color = th.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (mini != null) describeWeather(mini.code, mini.isDay).second else "Yükleniyor...",
                color = th.sub,
                fontSize = 13.sp
            )
        }
        if (mini != null) {
            Text(fmtTemp(mini.temp, useF), color = th.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            if (isFav) "⭐" else "☆",
            fontSize = 22.sp,
            color = Color(0xFFFBBF24),
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onFav() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14151F)
@Composable
fun CityRowPreview() {
    CompositionLocalProvider(LocalAppColors provides DarkAppColors) {
        CityRow(
            city = City("Ankara", 39.9334, 32.8597),
            isFav = true,
            mini = MiniWeather(18.0, 2, true),
            useF = false,
            onFav = {},
            onClick = {},
            selected = true
        )
    }
}

// =====================================================================
//  DURUMLAR
// =====================================================================

sealed interface LoadState {
    data object Loading : LoadState
    data class Success(val cities: List<City>) : LoadState
    data object Empty : LoadState
    data class Error(val message: String) : LoadState
}

sealed interface WeatherState {
    data object Loading : WeatherState
    data class Success(val data: WeatherData, val offlineSince: Long? = null) : WeatherState
    data class Error(val message: String) : WeatherState
}

// =====================================================================
//  UYGULAMA MODELLERİ
// =====================================================================

data class MiniWeather(val temp: Double, val code: Int, val isDay: Boolean)

data class HourItem(val label: String, val temp: Double, val code: Int, val rain: Int)

data class DayItem(
    val label: String,
    val code: Int,
    val max: Double,
    val min: Double,
    val rain: Int
)

data class WeatherData(
    val current: CurrentWeather,
    val hours: List<HourItem>,
    val days: List<DayItem>,
    val sunrise: String,
    val sunset: String,
    val uv: Double
)

// =====================================================================
//  API MODELLERİ
// =====================================================================

data class City(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("feature_code") val featureCode: String? = null,
    val admin1: String? = null
)

data class CitySearchResponse(val results: List<City>?)

data class CurrentWeather(
    val time: String = "",
    @SerializedName("temperature_2m") val temperature: Double = 0.0,
    @SerializedName("apparent_temperature") val feelsLike: Double = 0.0,
    @SerializedName("relative_humidity_2m") val humidity: Int = 0,
    @SerializedName("wind_speed_10m") val wind: Double = 0.0,
    @SerializedName("weather_code") val weatherCode: Int = 0,
    @SerializedName("is_day") val isDay: Int = 1
)

data class HourlyDto(
    val time: List<String>?,
    @SerializedName("temperature_2m") val temp: List<Double?>?,
    @SerializedName("weather_code") val code: List<Int?>?,
    @SerializedName("precipitation_probability") val rain: List<Int?>?
)

data class DailyDto(
    val time: List<String>?,
    @SerializedName("weather_code") val code: List<Int?>?,
    @SerializedName("temperature_2m_max") val max: List<Double?>?,
    @SerializedName("temperature_2m_min") val min: List<Double?>?,
    val sunrise: List<String?>?,
    val sunset: List<String?>?,
    @SerializedName("uv_index_max") val uv: List<Double?>?,
    @SerializedName("precipitation_probability_max") val rain: List<Int?>?
)

data class ForecastResponse(
    val current: CurrentWeather?,
    val hourly: HourlyDto?,
    val daily: DailyDto?
)

// =====================================================================
//  API
// =====================================================================

interface CityApi {
    @GET("search")
    suspend fun searchCities(
        @Query("name") query: String,
        @Query("count") count: Int,
        @Query("language") language: String,
        @Query("countryCode") countryCode: String
    ): CitySearchResponse
}

interface WeatherApi {
    @GET("forecast")
    suspend fun getCurrent(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String,
        @Query("timezone") timezone: String
    ): ForecastResponse

    @GET("forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String,
        @Query("hourly") hourly: String,
        @Query("daily") daily: String,
        @Query("timezone") timezone: String,
        @Query("forecast_days") days: Int
    ): ForecastResponse
}

object ApiClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }
    private val client = OkHttpClient.Builder().addInterceptor(logging).build()

    private fun retrofit(baseUrl: String) = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val cityApi: CityApi by lazy {
        retrofit("https://geocoding-api.open-meteo.com/v1/").create(CityApi::class.java)
    }
    val weatherApi: WeatherApi by lazy {
        retrofit("https://api.open-meteo.com/v1/").create(WeatherApi::class.java)
    }
    val airApi: AirApi by lazy {
        retrofit("https://air-quality-api.open-meteo.com/v1/").create(AirApi::class.java)
    }
}

// =====================================================================
//  REPOSITORY
// =====================================================================

class CityRepository(
    private val cityApi: CityApi = ApiClient.cityApi,
    private val weatherApi: WeatherApi = ApiClient.weatherApi,
    private val airApi: AirApi = ApiClient.airApi
) {
    suspend fun air(city: City): AirData? =
        airApi.getAir(
            city.latitude, city.longitude,
            "european_aqi,pm2_5,pm10,alder_pollen,birch_pollen,grass_pollen,mugwort_pollen,olive_pollen,ragweed_pollen",
            "auto"
        ).current?.toAirData()

    suspend fun search(query: String): List<City> =
        cityApi.searchCities(query, 100, "tr", "TR")
            .results.orEmpty()
            .filter { it.featureCode == "PPLA" || it.featureCode == "PPLC" }
            .distinctBy { it.name }

    suspend fun mini(city: City): MiniWeather {
        val c = weatherApi.getCurrent(
            city.latitude, city.longitude,
            "temperature_2m,weather_code,is_day", "auto"
        ).current ?: throw IllegalStateException("Boş cevap")
        return MiniWeather(c.temperature, c.weatherCode, c.isDay == 1)
    }

    suspend fun forecast(city: City): WeatherData {
        val r = weatherApi.getForecast(
            city.latitude, city.longitude,
            "temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,is_day",
            "temperature_2m,weather_code,precipitation_probability",
            "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_probability_max",
            "auto", 7
        )
        val cur = r.current ?: throw IllegalStateException("Boş cevap")
        val h = r.hourly ?: throw IllegalStateException("Saatlik yok")
        val d = r.daily ?: throw IllegalStateException("Günlük yok")

        val hTimes = h.time.orEmpty()
        val start = hTimes.indexOfFirst { it.take(13) >= cur.time.take(13) }.coerceAtLeast(0)
        val end = minOf(start + 24, hTimes.size)
        val hours = (start until end).map { i ->
            HourItem(
                label = if (i == start) "Şimdi" else hTimes[i].substring(11, 16),
                temp = h.temp?.getOrNull(i) ?: 0.0,
                code = h.code?.getOrNull(i) ?: 0,
                rain = h.rain?.getOrNull(i) ?: 0
            )
        }

        val dTimes = d.time.orEmpty()
        val days = dTimes.indices.map { i ->
            DayItem(
                label = dayLabel(dTimes[i], i),
                code = d.code?.getOrNull(i) ?: 0,
                max = d.max?.getOrNull(i) ?: 0.0,
                min = d.min?.getOrNull(i) ?: 0.0,
                rain = d.rain?.getOrNull(i) ?: 0
            )
        }
        if (days.isEmpty()) throw IllegalStateException("Günlük yok")

        return WeatherData(
            current = cur,
            hours = hours,
            days = days,
            sunrise = d.sunrise?.getOrNull(0)?.takeLast(5) ?: "--:--",
            sunset = d.sunset?.getOrNull(0)?.takeLast(5) ?: "--:--",
            uv = d.uv?.getOrNull(0) ?: 0.0
        )
    }
}

// =====================================================================
//  VIEWMODEL
// =====================================================================

class CityViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = CityRepository()
    private val prefs = app.getSharedPreferences("hava_prefs", Context.MODE_PRIVATE)
    private val cache = WeatherCache(prefs)
    private val staleMini = mutableSetOf<String>()   // önbellekten gelen, yenilenmesi gereken kartlar

    private val defaultCities = listOf(
        City("Kayseri", 38.7312, 35.4787),
        City("Bursa", 40.1826, 29.0665),
        City("İstanbul", 41.0082, 28.9784),
        City("Ankara", 39.9334, 32.8597),
        City("İzmir", 38.4237, 27.1428),
        City("Antalya", 36.8969, 30.7133),
        City("Adana", 37.0000, 35.3213),
        City("Konya", 37.8746, 32.4932),
        City("Samsun", 41.2867, 36.3300),
        City("Trabzon", 41.0027, 39.7168)
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _state = MutableStateFlow<LoadState>(LoadState.Success(defaultCities))
    val state = _state.asStateFlow()

    // Ana ekranda gösterilen şehir (son bakılan şehir hatırlanır)
    private val _currentCity = MutableStateFlow(loadLastCity())
    val currentCity = _currentCity.asStateFlow()

    private val _weather = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val weather = _weather.asStateFlow()

    private val _mini = MutableStateFlow<Map<String, MiniWeather>>(emptyMap())
    val mini = _mini.asStateFlow()

    private val _favorites = MutableStateFlow(loadFavorites())
    val favorites = _favorites.asStateFlow()

    private val _useF = MutableStateFlow(prefs.getBoolean("useF", false))
    val useFahrenheit = _useF.asStateFlow()

    // Açık / koyu tema (varsayılan koyu, seçim hatırlanır)
    private val _dark = MutableStateFlow(prefs.getBoolean("dark", true))
    val darkTheme = _dark.asStateFlow()

    private var searchJob: Job? = null
    private var weatherJob: Job? = null

    // Kıyafet önerisi
    private val _outfit = MutableStateFlow<OutfitState>(OutfitState.Idle)
    val outfit = _outfit.asStateFlow()
    private var outfitJob: Job? = null

    // Hava kalitesi ve polen
    private val _air = MutableStateFlow<AirState>(AirState.Loading)
    val air = _air.asStateFlow()
    private var airJob: Job? = null

    // Kişisel profil (SharedPreferences'ta saklanır)
    private val _profile = MutableStateFlow(loadProfile())
    val profile = _profile.asStateFlow()
    private var lastOutfitData: WeatherData? = null

    init {
        loadWeather(_currentCity.value)
    }

    // ----- Arama -----

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            load(query)
        }
    }

    fun retry() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { load(_searchQuery.value) }
    }

    private suspend fun load(query: String) {
        if (query.isBlank()) {
            _state.value = LoadState.Success(defaultCities)
            return
        }
        _state.value = LoadState.Loading
        try {
            val cities = repository.search(query.trim())
            _state.value = if (cities.isEmpty()) LoadState.Empty else LoadState.Success(cities)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("HavaDurumuApp", "Arama hatası", e)
            _state.value = LoadState.Error("Bağlantı hatası. İnternetini kontrol et.")
        }
    }

    // ----- Kartlardaki mini hava durumu -----

    fun loadMini(cities: List<City>) {
        val missing = cities
            .filter { !_mini.value.containsKey(it.key()) || it.key() in staleMini }
            .distinctBy { it.key() }
        if (missing.isEmpty()) return
        viewModelScope.launch {
            coroutineScope {
                missing.map { c ->
                    async {
                        try {
                            val m = repository.mini(c)
                            cache.saveMini(c, m)
                            staleMini.remove(c.key())
                            _mini.value = _mini.value + (c.key() to m)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e("HavaDurumuApp", "Mini hava hatası: ${c.name}", e)
                            // İnternet yoksa son bilinen değeri göster, tekrar girince yeniden dene
                            cache.loadMini(c)?.let {
                                staleMini.add(c.key())
                                _mini.value = _mini.value + (c.key() to it)
                            }
                        }
                    }
                }.awaitAll()
            }
        }
    }

    // ----- Ana ekran hava durumu -----

    fun selectCity(city: City) {
        _currentCity.value = city
        prefs.edit()
            .putString("last", "${city.name}|${city.latitude}|${city.longitude}|${city.admin1.orEmpty()}")
            .apply()
        loadWeather(city)
    }

    fun retryWeather() {
        loadWeather(_currentCity.value)
    }

    private fun loadWeather(city: City) {
        weatherJob?.cancel()
        loadAir(city)
        weatherJob = viewModelScope.launch {
            _outfit.value = OutfitState.Idle
            _weather.value = WeatherState.Loading
            try {
                val data = repository.forecast(city)
                cache.save(city, data)
                _weather.value = WeatherState.Success(data)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("HavaDurumuApp", "Hava durumu hatası", e)
                // İnternet yoksa son kaydedilen veriyi göster
                val cached = cache.load(city)
                _weather.value =
                    if (cached != null) WeatherState.Success(cached.first, offlineSince = cached.second)
                    else WeatherState.Error("Hava durumu alınamadı")
            }
        }
    }

    private fun loadAir(city: City) {
        airJob?.cancel()
        airJob = viewModelScope.launch {
            _air.value = AirState.Loading
            _air.value = try {
                repository.air(city)?.let {
                    cache.saveAir(city, it)
                    AirState.Success(it)
                } ?: AirState.Unavailable
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("HavaDurumuApp", "Hava kalitesi hatası", e)
                // İnternet yoksa son kayıtlı değeri göster
                cache.loadAir(city)?.let { AirState.Success(it.first, cachedAt = it.second) }
                    ?: AirState.Unavailable
            }
        }
    }

    // ----- Kıyafet önerisi -----

    fun suggestOutfit(data: WeatherData) {
        lastOutfitData = data
        outfitJob?.cancel()
        outfitJob = viewModelScope.launch {
            _outfit.value = OutfitState.Loading
            _outfit.value = OutfitState.Success(
                OutfitRepository.suggest(
                    _currentCity.value.name,
                    data,
                    (_air.value as? AirState.Success)?.data,
                    _profile.value
                )
            )
        }
    }

    // ----- Kişisel profil -----

    fun setSense(sense: TempSense) = saveProfile(_profile.value.copy(sense = sense))
    fun toggleAllergy() = saveProfile(_profile.value.copy(allergy = !_profile.value.allergy))
    fun toggleAsthma() = saveProfile(_profile.value.copy(asthma = !_profile.value.asthma))
    fun toggleBike() = saveProfile(_profile.value.copy(bike = !_profile.value.bike))

    private fun saveProfile(p: UserProfile) {
        _profile.value = p
        prefs.edit()
            .putString("p_sense", p.sense.name)
            .putBoolean("p_allergy", p.allergy)
            .putBoolean("p_asthma", p.asthma)
            .putBoolean("p_bike", p.bike)
            .apply()
        // Kıyafet önerisi ekrandaysa yeni profile göre yenile
        if (_outfit.value is OutfitState.Success) {
            lastOutfitData?.let { suggestOutfit(it) }
        }
    }

    private fun loadProfile(): UserProfile = UserProfile(
        sense = runCatching {
            TempSense.valueOf(prefs.getString("p_sense", "Normal") ?: "Normal")
        }.getOrDefault(TempSense.Normal),
        allergy = prefs.getBoolean("p_allergy", false),
        asthma = prefs.getBoolean("p_asthma", false),
        bike = prefs.getBoolean("p_bike", false)
    )

    // ----- Favoriler, birim, tema (SharedPreferences) -----

    fun toggleFavorite(city: City) {
        val list = _favorites.value
        _favorites.value =
            if (list.any { it.key() == city.key() }) list.filter { it.key() != city.key() }
            else list + city
        val s = _favorites.value.joinToString(";") { "${it.name}|${it.latitude}|${it.longitude}" }
        prefs.edit().putString("favs", s).apply()
    }

    fun toggleUnit() {
        _useF.value = !_useF.value
        prefs.edit().putBoolean("useF", _useF.value).apply()
    }

    fun toggleTheme() {
        _dark.value = !_dark.value
        prefs.edit().putBoolean("dark", _dark.value).apply()
    }

    private fun loadLastCity(): City {
        val p = prefs.getString("last", "").orEmpty().split("|")
        if (p.size >= 3) {
            val lat = p[1].toDoubleOrNull()
            val lon = p[2].toDoubleOrNull()
            if (lat != null && lon != null) {
                return City(p[0], lat, lon, admin1 = p.getOrNull(3)?.takeIf { it.isNotBlank() })
            }
        }
        return City("Bursa", 40.1826, 29.0665, admin1 = "Bursa")
    }

    private fun loadFavorites(): List<City> =
        prefs.getString("favs", "").orEmpty()
            .split(";")
            .filter { it.isNotBlank() }
            .mapNotNull {
                val p = it.split("|")
                if (p.size == 3) {
                    val lat = p[1].toDoubleOrNull()
                    val lon = p[2].toDoubleOrNull()
                    if (lat != null && lon != null) City(p[0], lat, lon) else null
                } else null
            }
}