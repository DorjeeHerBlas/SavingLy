package com.misfinanzas.app.ui.screens.ahorros

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.misfinanzas.app.domain.model.Aportacion
import com.misfinanzas.app.ui.components.BarraProgreso
import com.misfinanzas.app.ui.components.CampoFecha
import com.misfinanzas.app.ui.components.EmojiBadge
import com.misfinanzas.app.ui.components.EstadoVacio
import com.misfinanzas.app.utils.Formato
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleMetaScreen(
    metaId: Long,
    onVolver: () -> Unit,
    vm: DetalleMetaViewModel = hiltViewModel()
) {
    LaunchedEffect(metaId) { vm.cargar(metaId) }
    val state by vm.estado.collectAsState()
    val meta = state.meta
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(meta?.nombre ?: "Meta") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            if (meta != null) ExtendedFloatingActionButton(
                onClick = { vm.abrirDialogoAporte() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Aportar") }
            )
        }
    ) { padding ->
        if (meta == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiBadge(emoji = meta.icono, tamaño = 56)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(meta.nombre, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Faltan ${Formato.importe(meta.restante)}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "${Formato.importe(meta.importeActual)} / ${Formato.importe(meta.importeObjetivo)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        BarraProgreso(progreso = meta.progreso, color = Color(meta.color.toInt()))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${(meta.progreso * 100).toInt()}% completado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        state.proyeccionMeses?.let { meses ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (meses == 1) "A este ritmo llegarás en 1 mes"
                                else "A este ritmo llegarás en $meses meses",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Aportaciones", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (state.aportaciones.isEmpty()) {
                    EstadoVacio(
                        titulo = "Sin aportaciones",
                        descripcion = "Registra el primer avance para ver el ritmo hacia tu meta.",
                        emoji = "💰",
                        accion = {
                            Button(onClick = { vm.abrirDialogoAporte() }) {
                                Text("Aportar")
                            }
                        }
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.aportaciones, key = { it.id }) { ap ->
                            FilaAporte(ap, onEliminar = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.eliminarAporte(ap)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar("Aportación eliminada", "Deshacer")
                                    if (result == SnackbarResult.ActionPerformed) vm.restaurarAporte(ap)
                                }
                            })
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    if (state.mostrarDialogoAporte) {
        DialogoAportar(
            onCerrar = { vm.cerrarDialogoAporte() },
            onGuardar = { i, f, n -> vm.añadirAporte(i, f, n) }
        )
    }
}

@Composable
private fun FilaAporte(a: Aportacion, onEliminar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(Formato.importe(a.importe), fontWeight = FontWeight.SemiBold)
                Text(
                    Formato.fecha(a.fecha) + (a.nota?.let { " • $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoAportar(onCerrar: () -> Unit, onGuardar: (Double, LocalDate, String?) -> Unit) {
    var importe by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(LocalDate.now()) }

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Nueva aportación", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = importe,
                    onValueChange = { importe = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Importe €") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                CampoFecha("Fecha", fecha, { if (it != null) fecha = it })
                OutlinedTextField(
                    value = nota, onValueChange = { nota = it },
                    label = { Text("Nota") }, modifier = Modifier.fillMaxWidth()
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCerrar, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
                Button(
                    enabled = importe.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true,
                    onClick = {
                        val v = importe.replace(',', '.').toDoubleOrNull() ?: 0.0
                        if (v > 0) onGuardar(v, fecha, nota.ifBlank { null })
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Guardar")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
