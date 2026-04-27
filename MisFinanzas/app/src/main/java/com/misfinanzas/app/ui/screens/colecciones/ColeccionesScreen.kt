package com.misfinanzas.app.ui.screens.colecciones

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.misfinanzas.app.domain.model.Coleccion
import com.misfinanzas.app.domain.model.TipoColeccion
import com.misfinanzas.app.ui.components.EmojiBadge
import com.misfinanzas.app.ui.components.EstadoVacio
import com.misfinanzas.app.ui.components.OverlayCelebracionColeccionCreada
import com.misfinanzas.app.ui.components.OverlayCelebracionColeccionEliminada

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColeccionesScreen(
    onAbrirColeccion: (Long) -> Unit,
    vm: ColeccionesViewModel = hiltViewModel()
) {
    val state by vm.estado.collectAsState()
    var celebrarCreacion by remember { mutableStateOf(false) }
    var celebrarEliminacion by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mis colecciones") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.abrirNuevo() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nueva colección") }
            )
        }
    ) { padding ->
        if (state.colecciones.isEmpty()) {
            Box(Modifier.padding(padding)) {
                EstadoVacio(
                    titulo = "No tienes colecciones",
                    descripcion = "Crea una colección por cada serie que coleccionas: Stephen King, Funkos, MTG, juegos…",
                    emoji = "📦"
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.colecciones, key = { it.id }) { c ->
                    TarjetaColeccion(
                        coleccion = c,
                        onClick = { onAbrirColeccion(c.id) },
                        onLongClick = { vm.abrirEditar(c) },
                        onEliminar = {
                            vm.eliminar(c)
                            celebrarEliminacion = true
                        }
                    )
                }
            }
        }
    }

    if (state.mostrarDialogo) {
        val esNueva = state.coleccionEditando == null
        DialogoColeccion(
            inicial = state.coleccionEditando,
            onCerrar = { vm.cerrarDialogo() },
            onGuardar = { n, t, d, i ->
                vm.guardar(n, t, d, i)
                if (esNueva) celebrarCreacion = true
            }
        )
    }

    OverlayCelebracionColeccionCreada(
        visible = celebrarCreacion,
        onFinished = { celebrarCreacion = false },
    )
    OverlayCelebracionColeccionEliminada(
        visible = celebrarEliminacion,
        onFinished = { celebrarEliminacion = false },
    )
}

@Composable
private fun TarjetaColeccion(
    coleccion: Coleccion,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEliminar: () -> Unit
) {
    val oledDark = MaterialTheme.colorScheme.background.luminance() < 0.02f
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (oledDark) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                EmojiBadge(emoji = coleccion.icono, tamaño = 48)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEliminar) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                coleccion.nombre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )
            Text(
                coleccion.tipo.etiqueta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!coleccion.descripcion.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    coleccion.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoColeccion(
    inicial: Coleccion?,
    onCerrar: () -> Unit,
    onGuardar: (String, TipoColeccion, String?, String) -> Unit
) {
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var tipo by remember { mutableStateOf(inicial?.tipo ?: TipoColeccion.OTROS) }
    var descripcion by remember { mutableStateOf(inicial?.descripcion ?: "") }
    var icono by remember { mutableStateOf(inicial?.icono ?: tipo.emoji) }
    var exp by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (inicial == null) "Nueva colección" else "Editar colección") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row {
                    OutlinedTextField(
                        value = icono, onValueChange = { if (it.length <= 2) icono = it },
                        label = { Text("Icono") },
                        modifier = Modifier.width(90.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = nombre, onValueChange = { nombre = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.weight(1f)
                    )
                }
                ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }) {
                    OutlinedTextField(
                        value = "${tipo.emoji} ${tipo.etiqueta}",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exp) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                        TipoColeccion.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text("${t.emoji}  ${t.etiqueta}") },
                                onClick = {
                                    tipo = t
                                    if (icono == inicial?.icono?.takeIf { it.isNotBlank() } ?: tipo.emoji) icono = t.emoji
                                    exp = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = descripcion, onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = nombre.isNotBlank(),
                onClick = { onGuardar(nombre.trim(), tipo, descripcion.ifBlank { null }, icono.ifBlank { tipo.emoji }) }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
