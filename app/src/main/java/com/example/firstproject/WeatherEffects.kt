package com.example.firstproject

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

// =====================================================================
//  Arka plan hava efektleri: yağmur, kar, güneş ışınları, yıldız, sis, şimşek
//  Hepsi Canvas ile çizilir, ekstra kütüphane yok. Dokunmayı engellemez.
// =====================================================================

private val TWO_PI = (2.0 * PI).toFloat()

@Composable
fun WeatherEffects(code: Int, isDay: Boolean, dark: Boolean) {
    when {
        code in 95..99 -> { RainLayer(110, dark); StormFlash() }
        code in 80..82 -> RainLayer(100, dark)
        code in 61..67 -> RainLayer(80, dark)
        code in 51..57 -> RainLayer(35, dark)
        code in 71..77 -> SnowLayer(55, dark)
        code == 85 || code == 86 -> SnowLayer(85, dark)
        code == 45 || code == 48 -> FogLayer(dark)
        code in 0..2 -> if (isDay) SunRaysLayer(dark) else if (dark) StarsLayer()
    }
}

// Parçacıklar sabit tohumla üretilir (her açılışta aynı dağılım).
// Hepsi 0..1 aralığında normalize; k tam sayı olduğu için döngü kopmadan döner.
private class Particle(
    val x: Float,
    val phase: Float,
    val k: Int,
    val size: Float,
    val len: Float
)

private fun makeParticles(n: Int, seed: Int, kMin: Int, kMax: Int): List<Particle> {
    val r = Random(seed)
    return List(n) {
        Particle(r.nextFloat(), r.nextFloat(), r.nextInt(kMin, kMax + 1), r.nextFloat(), r.nextFloat())
    }
}

@Composable
private fun RainLayer(count: Int, dark: Boolean) {
    val drops = remember(count) { makeParticles(count, 7, 2, 4) }
    val t by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing))
    )
    val color = if (dark) Color(0xFFBFDBFE) else Color(0xFF2563EB)

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val pad = 40.dp.toPx()
        val stroke = 1.6.dp.toPx()
        drops.forEach { d ->
            val y = ((d.phase + t * d.k) % 1f) * (h + 2 * pad) - pad
            val x = d.x * w
            val len = (14f + d.len * 16f).dp.toPx()
            drawLine(
                color = color.copy(alpha = 0.18f + d.size * 0.25f),
                start = Offset(x, y),
                end = Offset(x - len * 0.25f, y + len),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun SnowLayer(count: Int, dark: Boolean) {
    val flakes = remember(count) { makeParticles(count, 11, 1, 2) }
    val t by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing))
    )
    val color = if (dark) Color.White else Color(0xFF5B9BD5)

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val pad = 12.dp.toPx()
        val sway = 14.dp.toPx()
        flakes.forEach { f ->
            val y = ((f.phase + t * f.k) % 1f) * (h + 2 * pad) - pad
            val x = f.x * w + sin(TWO_PI * (t * (f.k + 1) + f.phase)) * sway
            drawCircle(
                color = color.copy(alpha = 0.45f + f.len * 0.45f),
                radius = (1.5f + f.size * 2.5f).dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun SunRaysLayer(dark: Boolean) {
    val transition = rememberInfiniteTransition()
    val rot by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(90000, easing = LinearEasing))
    )
    val pulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(4000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        )
    )
    val color = if (dark) Color(0xFFFFE9A8) else Color.White
    val base = if (dark) 0.10f else 0.26f

    Canvas(Modifier.fillMaxSize()) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.2f
        val r = hypot(size.width, size.height) * 1.3f
        val deg = (PI / 180.0).toFloat()
        for (i in 0 until 12) {
            val a0 = (rot + i * 30f) * deg
            val a1 = a0 + 7f * deg
            val p = Path().apply {
                moveTo(cx, cy)
                lineTo(cx + r * cos(a0), cy + r * sin(a0))
                lineTo(cx + r * cos(a1), cy + r * sin(a1))
                close()
            }
            drawPath(p, color.copy(alpha = base * pulse))
        }
    }
}

@Composable
private fun StarsLayer() {
    val stars = remember { makeParticles(45, 3, 1, 3) }
    val t by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing))
    )
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        stars.forEach { s ->
            val a = 0.25f + 0.65f * (0.5f + 0.5f * sin(TWO_PI * (t * s.k + s.phase)))
            drawCircle(
                color = Color.White.copy(alpha = a),
                radius = (0.8f + s.size * 1.6f).dp.toPx(),
                center = Offset(s.x * w, s.len * h * 0.65f)
            )
        }
    }
}

@Composable
private fun FogLayer(dark: Boolean) {
    val t by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing))
    )
    val color = Color.White.copy(alpha = if (dark) 0.06f else 0.30f)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        for (i in 0 until 4) {
            val dx = sin(TWO_PI * (t + i * 0.25f)) * w * 0.12f
            drawRoundRect(
                color = color,
                topLeft = Offset(-w * 0.2f + dx, h * (0.2f + i * 0.18f)),
                size = Size(w * 1.4f, h * 0.07f),
                cornerRadius = CornerRadius(h * 0.035f)
            )
        }
    }
}

@Composable
private fun StormFlash() {
    // Çoğu zaman 0, birkaç yüz ms'lik kısa şimşek parlaması
    val flash by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 0.001f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 7000
                0f at 0
                0f at 6000
                0.35f at 6060
                0f at 6140
                0.22f at 6240
                0f at 6420
            }
        )
    )
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Color.White.copy(alpha = flash.coerceIn(0f, 1f)))
    }
}