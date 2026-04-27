package com.misfinanzas.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.misfinanzas.app.R
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animación efímera de celebración: emoji central con halo + partículas que estallan
 * desde el centro + título y subtítulo. Dura ~1.4s y se autocierra.
 *
 * Se dibuja como Popup no modal para no bloquear la navegación inferior.
 */
@Composable
fun OverlayCelebracion(
    visible: Boolean,
    emoji: String,
    titulo: String,
    onFinished: () -> Unit,
    particulas: List<String> = listOf("✨", "🌟", "💫"),
    subtitulo: String? = null,
    haloColor: Color = Color(0xFFFFD78A),
) {
    if (!visible) return

    Popup(
        alignment = Alignment.Center,
        properties = PopupProperties(
            focusable = false,
            clippingEnabled = false,
        )
    ) {
        var phase by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) {
            phase = 1
            delay(1100)
            phase = 2 // fade-out
            delay(380)
            onFinished()
        }

        val scale by animateFloatAsState(
            targetValue = if (phase >= 1) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
            label = "scale",
        )
        val particleProgress by animateFloatAsState(
            targetValue = if (phase >= 1) 1f else 0f,
            animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            label = "particles",
        )
        val titleAlpha by animateFloatAsState(
            targetValue = if (phase >= 1) 1f else 0f,
            animationSpec = tween(durationMillis = 350, delayMillis = 150),
            label = "title",
        )
        val rootAlpha by animateFloatAsState(
            targetValue = if (phase < 2) 1f else 0f,
            animationSpec = tween(durationMillis = 380),
            label = "root",
        )

        // Configuración determinista de partículas (no se reshuffle entre frames).
        val particleConfigs = remember {
            val rng = Random(System.nanoTime())
            (0 until 14).map {
                ParticleConfig(
                    angleDeg = rng.nextFloat() * 360f,
                    distance = 180f + rng.nextFloat() * 240f,
                    emoji = particulas[rng.nextInt(particulas.size)],
                    rotacionFinal = (rng.nextFloat() * 720f) - 360f,
                    sizeSp = 18f + rng.nextFloat() * 14f,
                    delay = rng.nextFloat() * 0.20f,
                )
            }
        }

        Box(
            modifier = Modifier
                .size(560.dp)
                .graphicsLayer { alpha = rootAlpha },
            contentAlignment = Alignment.Center,
        ) {
            // Halo radial
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                haloColor.copy(alpha = 0.30f * scale),
                                haloColor.copy(alpha = 0.08f * scale),
                                Color.Transparent,
                            )
                        )
                    )
            )

            // Partículas estallando
            particleConfigs.forEach { p ->
                val staggered = ((particleProgress - p.delay) / (1f - p.delay))
                    .coerceIn(0f, 1f)
                val rad = Math.toRadians(p.angleDeg.toDouble())
                val tx = (cos(rad) * p.distance * staggered).toFloat()
                val ty = (sin(rad) * p.distance * staggered).toFloat()
                Text(
                    text = p.emoji,
                    fontSize = p.sizeSp.sp,
                    modifier = Modifier.graphicsLayer {
                        translationX = tx
                        translationY = ty
                        rotationZ = p.rotacionFinal * staggered
                        alpha = ((1f - staggered * 0.85f) * scale).coerceIn(0f, 1f)
                        scaleX = scale
                        scaleY = scale
                    }
                )
            }

            // Emoji central
            Text(
                text = emoji,
                fontSize = 96.sp,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = (1f - scale) * 80f
                }
            )

            // Texto
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        alpha = titleAlpha
                        translationY = (1f - titleAlpha) * 22f
                    }
                )
                if (!subtitulo.isNullOrBlank()) {
                    Text(
                        text = subtitulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = haloColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer {
                            alpha = titleAlpha
                            translationY = (1f - titleAlpha) * 18f
                        }
                    )
                }
            }
        }
    }
}

private data class ParticleConfig(
    val angleDeg: Float,
    val distance: Float,
    val emoji: String,
    val rotacionFinal: Float,
    val sizeSp: Float,
    val delay: Float,
)

// ───────────────────────── Helpers por evento ─────────────────────────

@Composable
fun OverlayCelebracionIngreso(visible: Boolean, monto: String, onFinished: () -> Unit) {
    OverlayCelebracion(
        visible = visible,
        emoji = "💰",
        titulo = stringResource(R.string.celebracion_ingreso),
        subtitulo = "+$monto",
        particulas = listOf("💵", "💶", "✨", "💎", "🪙"),
        haloColor = Color(0xFF4CAF50),
        onFinished = onFinished,
    )
}

@Composable
fun OverlayCelebracionGasto(visible: Boolean, monto: String, onFinished: () -> Unit) {
    OverlayCelebracion(
        visible = visible,
        emoji = "💸",
        titulo = stringResource(R.string.celebracion_gasto),
        subtitulo = "-$monto",
        particulas = listOf("🛒", "🧾", "🪙", "💨"),
        haloColor = Color(0xFFE57373),
        onFinished = onFinished,
    )
}

@Composable
fun OverlayCelebracionColeccionCreada(visible: Boolean, onFinished: () -> Unit) {
    OverlayCelebracion(
        visible = visible,
        emoji = "📦",
        titulo = stringResource(R.string.celebracion_coleccion_creada),
        particulas = listOf("🎉", "✨", "🌟", "🎊", "💫"),
        haloColor = Color(0xFFB388FF),
        onFinished = onFinished,
    )
}

@Composable
fun OverlayCelebracionColeccionEliminada(visible: Boolean, onFinished: () -> Unit) {
    OverlayCelebracion(
        visible = visible,
        emoji = "🗑️",
        titulo = stringResource(R.string.celebracion_coleccion_eliminada),
        particulas = listOf("💨", "💫", "✦"),
        haloColor = Color(0xFF9E9E9E),
        onFinished = onFinished,
    )
}

@Composable
fun OverlayCelebracionItemConseguido(visible: Boolean, nombre: String, onFinished: () -> Unit) {
    OverlayCelebracion(
        visible = visible,
        emoji = "🎉",
        titulo = stringResource(R.string.celebracion_item_conseguido),
        subtitulo = nombre,
        particulas = listOf("⭐", "✨", "🌟", "💫", "🏆", "🎊"),
        haloColor = Color(0xFFFFC107),
        onFinished = onFinished,
    )
}
