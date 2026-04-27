package com.misfinanzas.app.ui.screens.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.misfinanzas.app.R
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pantalla de splash animada de la app.
 *
 * Secuencia (~2.2s total):
 *   0–150ms   estado inicial vacío
 *   150ms     emoji central 💰 entra con scale + rotación 360° y aparece el halo radial
 *             los 4 emojis temáticos (Gastos, Ahorros, Colecciones, Stats) salen del centro
 *             en cascada hacia 4 puntos cardinales con stagger
 *   850ms     aparece el título "Mis Finanzas" con fade + slide-up
 *   ~1.95s    fade-out completo y se llama a [onFinished]
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    // Phase machine: 0=inicial, 1=orbitales+centro, 2=título, 3=fade-out
    var phase by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(150); phase = 1
        delay(700); phase = 2
        delay(1100); phase = 3
        delay(400); onFinished()
    }

    val centerScale by animateFloatAsState(
        targetValue = if (phase >= 1) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "centerScale",
    )
    val centerRotation by animateFloatAsState(
        targetValue = if (phase >= 1) 360f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "centerRot",
    )
    val orbitProgress by animateFloatAsState(
        targetValue = if (phase >= 1) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "orbit",
    )
    val titleProgress by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "title",
    )
    val rootAlpha by animateFloatAsState(
        targetValue = if (phase < 3) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "rootAlpha",
    )

    val density = LocalDensity.current
    val orbitRadiusPx = with(density) { 92.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .graphicsLayer { alpha = rootAlpha },
        contentAlignment = Alignment.Center,
    ) {
        // Halo radial central que pulsa
        val haloColor = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            haloColor.copy(alpha = 0.22f * centerScale),
                            haloColor.copy(alpha = 0.06f * centerScale),
                            Color.Transparent,
                        )
                    )
                )
        )

        // 4 emojis orbitando: vuelan desde el centro hacia los cuatro puntos cardinales.
        val orbitales = listOf(
            "🛒" to -90f, // Gastos     — arriba
            "🐷" to 0f,   // Ahorros    — derecha
            "🃏" to 90f,  // Colecciones — abajo
            "📊" to 180f, // Stats      — izquierda
        )
        orbitales.forEachIndexed { i, (emoji, angleDeg) ->
            // Stagger: cada emoji empieza con un pequeño retraso relativo al orbitProgress.
            val stagger = ((orbitProgress - i * 0.12f) * 1.45f).coerceIn(0f, 1f)
            val rad = Math.toRadians(angleDeg.toDouble())
            val tx = (cos(rad) * orbitRadiusPx * stagger).toFloat()
            val ty = (sin(rad) * orbitRadiusPx * stagger).toFloat()
            Text(
                text = emoji,
                fontSize = 30.sp,
                modifier = Modifier.graphicsLayer {
                    translationX = tx
                    translationY = ty
                    scaleX = stagger
                    scaleY = stagger
                    alpha = stagger
                    rotationZ = (1f - stagger) * 90f
                }
            )
        }

        // Emoji central
        Text(
            text = "💰",
            fontSize = 72.sp,
            modifier = Modifier.graphicsLayer {
                scaleX = centerScale
                scaleY = centerScale
                rotationZ = centerRotation
            }
        )

        // Título + barra de progreso en la parte inferior
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer {
                    alpha = titleProgress
                    translationY = (1f - titleProgress) * 30f
                }
            )
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { ((orbitProgress * 0.4f) + (titleProgress * 0.6f)).coerceIn(0f, 1f) },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier
                    .width(120.dp)
                    .height(3.dp)
                    .graphicsLayer { alpha = orbitProgress }
            )
        }
    }
}
