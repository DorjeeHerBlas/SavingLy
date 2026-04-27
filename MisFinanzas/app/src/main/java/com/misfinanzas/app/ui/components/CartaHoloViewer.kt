package com.misfinanzas.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.misfinanzas.app.R
import kotlin.math.abs

/**
 * Visor 3D holo-tilt para una carta (MTG u otra).
 *
 * Características:
 *  - Drag para inclinar la carta con perspectiva 3D, vuelve a la posición neutra al soltar.
 *  - Swipe horizontal (≥ ~80dp) para voltear y ver el reverso.
 *  - Toggle "Foil" para activar el efecto holográfico (por defecto desactivado).
 *  - Toca fuera de la carta o pulsa atrás para cerrar.
 */
@Composable
fun CartaHoloViewer(
    imagenUrl: String,
    nombre: String,
    onDismiss: () -> Unit,
    subtitulo: String? = null,
) {
    var foilEnabled by remember { mutableStateOf(false) }
    var flipped by remember { mutableStateOf(false) }
    val flipAngle by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "flip",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp),
            ) {
                HoloCard(
                    imagenUrl = imagenUrl,
                    descripcion = nombre,
                    foilEnabled = foilEnabled,
                    flipAngle = flipAngle,
                    onFlipRequest = { flipped = !flipped },
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .pointerInput(Unit) { detectTapGestures(onTap = { /* consumir */ }) },
                ) {
                    FilterChip(
                        selected = foilEnabled,
                        onClick = { foilEnabled = !foilEnabled },
                        leadingIcon = {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                        },
                        label = {
                            Text(
                                stringResource(
                                    if (foilEnabled) R.string.holo_foil_on
                                    else R.string.holo_foil_off
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White.copy(alpha = 0.08f),
                            labelColor = Color.White.copy(alpha = 0.85f),
                            iconColor = Color.White.copy(alpha = 0.85f),
                            selectedContainerColor = Color(0xFFB388FF).copy(alpha = 0.35f),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                        ),
                    )
                }

                Text(
                    text = nombre,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 18.dp),
                )
                if (!subtitulo.isNullOrBlank()) {
                    Text(
                        text = subtitulo,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.holo_hint),
                    color = Color.White.copy(alpha = 0.45f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun HoloCard(
    imagenUrl: String,
    descripcion: String,
    foilEnabled: Boolean,
    flipAngle: Float,
    onFlipRequest: () -> Unit,
) {
    var targetRotX by remember { mutableFloatStateOf(0f) }
    var targetRotY by remember { mutableFloatStateOf(0f) }

    val animSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
    val rotX by animateFloatAsState(targetValue = targetRotX, animationSpec = animSpec, label = "rotX")
    val rotY by animateFloatAsState(targetValue = targetRotY, animationSpec = animSpec, label = "rotY")

    val angleNorm = ((flipAngle % 360f) + 360f) % 360f
    val showFront = angleNorm < 90f || angleNorm > 270f

    val density = LocalDensity.current
    val flipThresholdPx = with(density) { 80.dp.toPx() }
    // Drag acumulado durante el gesto en curso, para decidir si lanzamos flip al soltar.
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.82f)
            .aspectRatio(0.716f) // Ratio carta MTG estándar 63 × 88 mm
            .pointerInput(Unit) { detectTapGestures(onTap = { /* consumir */ }) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDragEnd = {
                        // Swipe horizontal predominante y > threshold → flip.
                        if (abs(totalDragX) > flipThresholdPx &&
                            abs(totalDragX) > abs(totalDragY)
                        ) {
                            onFlipRequest()
                        }
                        targetRotX = 0f
                        targetRotY = 0f
                    },
                    onDragCancel = {
                        targetRotX = 0f
                        targetRotY = 0f
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        totalDragX += drag.x
                        totalDragY += drag.y
                        targetRotY = (targetRotY + drag.x * 0.35f).coerceIn(-28f, 28f)
                        targetRotX = (targetRotX - drag.y * 0.35f).coerceIn(-28f, 28f)
                    }
                )
            }
            .graphicsLayer {
                rotationX = rotX
                rotationY = rotY + flipAngle
                cameraDistance = 14f * density.density
            }
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black)
    ) {
        if (showFront) {
            SubcomposeAsyncImage(
                model = imagenUrl,
                contentDescription = descripcion,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                loading = { CardLoadingState() },
                error = { CardErrorState() },
                success = { SubcomposeAsyncImageContent() },
            )
            if (foilEnabled) {
                HoloOverlay(rotX = rotX, rotY = rotY, modifier = Modifier.fillMaxSize())
                SpecularHighlight(rotX = rotX, rotY = rotY, modifier = Modifier.fillMaxSize())
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
            ) {
                CardBack()
                if (foilEnabled) {
                    SpecularHighlight(rotX = rotX, rotY = rotY, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun CardLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.holo_loading), color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun CardErrorState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.holo_no_image), color = Color.White.copy(alpha = 0.6f))
    }
}

/** Reverso decorativo de carta: gradiente cálido + anillos + texto. */
@Composable
private fun CardBack() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF8A4A1F),
                        Color(0xFF3A1A07),
                        Color(0xFF120700),
                    ),
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .border(
                    width = 1.5.dp,
                    color = Color(0xFFD4A86A).copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(2.dp)
                .border(
                    width = 0.8.dp,
                    color = Color(0xFFD4A86A).copy(alpha = 0.25f),
                    shape = RoundedCornerShape(10.dp),
                )
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD78A),
                                Color(0xFF8A4A1F),
                                Color(0xFF2A1308),
                            )
                        )
                    )
                    .border(2.dp, Color(0xFFD4A86A), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "💰", fontSize = 56.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.app_name).uppercase(),
                color = Color(0xFFD4A86A),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.holo_collection_label),
                color = Color(0xFFAA8855),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun HoloOverlay(rotX: Float, rotY: Float, modifier: Modifier) {
    val holoColors = listOf(
        Color(0xCCFF4D8B),
        Color(0xCCFFB347),
        Color(0xCCFFF36B),
        Color(0xCC6BFFB1),
        Color(0xCC55D9FF),
        Color(0xCCB388FF),
        Color(0xCCFF4D8B),
    )

    Canvas(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
            alpha = 0.55f
        }
    ) {
        val w = size.width
        val h = size.height
        val tiltX = (rotY / 28f).coerceIn(-1f, 1f)
        val tiltY = (rotX / 28f).coerceIn(-1f, 1f)

        val start = Offset(
            x = w * (0.5f + tiltX * 0.6f) - w,
            y = h * (0.5f - tiltY * 0.6f) - h,
        )
        val end = Offset(
            x = w * (0.5f + tiltX * 0.6f) + w,
            y = h * (0.5f - tiltY * 0.6f) + h,
        )

        drawRect(
            brush = Brush.linearGradient(colors = holoColors, start = start, end = end),
            blendMode = BlendMode.Plus,
        )
    }
}

@Composable
private fun SpecularHighlight(rotX: Float, rotY: Float, modifier: Modifier) {
    Canvas(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
            alpha = 0.45f
        }
    ) {
        val w = size.width
        val h = size.height
        val cx = w * (0.5f + (rotY / 28f).coerceIn(-1f, 1f) * 0.45f)
        val cy = h * (0.5f - (rotX / 28f).coerceIn(-1f, 1f) * 0.45f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.0f),
                ),
                center = Offset(cx, cy),
                radius = w * 0.6f,
            ),
            radius = w * 0.6f,
            center = Offset(cx, cy),
            blendMode = BlendMode.Plus,
        )
    }
}
