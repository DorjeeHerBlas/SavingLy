package com.misfinanzas.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.misfinanzas.app.utils.Formato
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun TarjetaResumen(
    titulo: String,
    valorPrincipal: String,
    subtitulo: String? = null,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    onColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelLarge,
                color = onColor.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = valorPrincipal,
                style = MaterialTheme.typography.headlineMedium,
                color = onColor,
                fontWeight = FontWeight.Bold
            )
            if (subtitulo != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = onColor.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
fun EmojiBadge(
    emoji: String,
    fondo: Color = MaterialTheme.colorScheme.primaryContainer,
    tamaño: Int = 40,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(tamaño.dp)
            .clip(CircleShape)
            .background(fondo),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun EstadoVacio(
    titulo: String,
    descripcion: String,
    emoji: String = "✨",
    accion: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(titulo, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            descripcion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (accion != null) {
            Spacer(Modifier.height(20.dp))
            accion()
        }
    }
}

@Composable
fun BarraProgreso(
    progreso: Float,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val progresoAnimado by animateFloatAsState(
        targetValue = progreso.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "progreso"
    )
    LinearProgressIndicator(
        progress = { progresoAnimado },
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp)),
        color = color,
        trackColor = color.copy(alpha = 0.18f)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    label: String,
    fecha: LocalDate?,
    onFecha: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    permitirVaciar: Boolean = false
) {
    var mostrar by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { mostrar = true }, modifier = modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                fecha?.let(Formato::fecha) ?: "Sin fecha",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
    if (mostrar) {
        val initialMillis = fecha
            ?.atStartOfDay(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { mostrar = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        onFecha(
                            millis?.let {
                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                            }
                        )
                        mostrar = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                Row {
                    if (permitirVaciar) {
                        TextButton(onClick = { onFecha(null); mostrar = false }) { Text("Quitar") }
                    }
                    TextButton(onClick = { mostrar = false }) { Text("Cancelar") }
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
