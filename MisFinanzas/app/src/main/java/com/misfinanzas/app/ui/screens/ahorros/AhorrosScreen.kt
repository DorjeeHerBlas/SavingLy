package com.misfinanzas.app.ui.screens.ahorros

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.misfinanzas.app.R
import com.misfinanzas.app.domain.model.MetaAhorro
import com.misfinanzas.app.ui.components.BarraProgreso
import com.misfinanzas.app.ui.components.CampoFecha
import com.misfinanzas.app.ui.components.EmojiBadge
import com.misfinanzas.app.ui.components.EstadoVacio
import com.misfinanzas.app.utils.Formato
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AhorrosScreen(
    onAbrirMeta: (Long) -> Unit,
    vm: AhorrosViewModel = hiltViewModel()
) {
    val state by vm.estado.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ahorros") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.abrirNuevo() }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva meta")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card de la cuenta de ahorro siempre arriba
            item(key = "cuenta_ahorro") {
                TarjetaCuentaAhorro(
                    state = state,
                    onEditarSaldo = { vm.abrirDialogoSaldo() },
                )
            }

            if (state.metas.isEmpty()) {
                item(key = "vacio_metas") {
                    EstadoVacio(
                        titulo = "Aún no tienes metas",
                        descripcion = "Crea metas para seguir tu progreso de ahorro hacia algo concreto",
                        emoji = "🎯",
                        accion = {
                            Button(onClick = { vm.abrirNuevo() }) {
                                Text("Nueva meta")
                            }
                        }
                    )
                }
            } else {
                items(state.metas, key = { it.id }) { meta ->
                    TarjetaMeta(
                        meta = meta,
                        onClick = { onAbrirMeta(meta.id) },
                        onEliminar = { vm.eliminar(meta) }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (state.mostrarDialogo) {
        DialogoMeta(
            inicial = state.metaEditando,
            onCerrar = { vm.cerrarDialogo() },
            onGuardar = { n, o, i, f, no -> vm.guardar(n, o, i, f, no) }
        )
    }

    if (state.mostrarDialogoSaldo) {
        DialogoSaldoInicial(
            saldoActual = state.saldoInicialCuenta,
            onCerrar = { vm.cerrarDialogoSaldo() },
            onGuardar = { vm.setSaldoInicial(it) },
        )
    }
}

@Composable
private fun TarjetaMeta(meta: MetaAhorro, onClick: () -> Unit, onEliminar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(emoji = meta.icono)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(meta.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${Formato.importe(meta.importeActual)} de ${Formato.importe(meta.importeObjetivo)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${(meta.progreso * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (meta.completada) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onEliminar) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
                }
            }
            Spacer(Modifier.height(12.dp))
            BarraProgreso(
                progreso = meta.progreso,
                color = if (meta.completada) MaterialTheme.colorScheme.primary
                        else Color(meta.color.toInt())
            )
            if (meta.fechaLimite != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Fecha límite: ${Formato.fecha(meta.fechaLimite)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoMeta(
    inicial: MetaAhorro?,
    onCerrar: () -> Unit,
    onGuardar: (String, Double, String, LocalDate?, String?) -> Unit
) {
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var objetivo by remember { mutableStateOf(inicial?.importeObjetivo?.toString() ?: "") }
    var icono by remember { mutableStateOf(inicial?.icono ?: "🎯") }
    var fechaLimite by remember { mutableStateOf(inicial?.fechaLimite) }
    var notas by remember { mutableStateOf(inicial?.notas ?: "") }

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
            Text(
                if (inicial == null) "Nueva meta" else "Editar meta",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row {
                    OutlinedTextField(
                        value = icono, onValueChange = { if (it.length <= 2) icono = it },
                        label = { Text("Icono") },
                        modifier = Modifier.width(90.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = nombre, onValueChange = { nombre = it },
                        label = { Text("Nombre de la meta") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = objetivo,
                    onValueChange = { objetivo = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Importe objetivo €") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                CampoFecha(
                    label = "Fecha límite",
                    fecha = fechaLimite,
                    onFecha = { fechaLimite = it },
                    permitirVaciar = true
                )
                OutlinedTextField(
                    value = notas, onValueChange = { notas = it },
                    label = { Text("Notas (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCerrar, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
                Button(
                    enabled = nombre.isNotBlank() && objetivo.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true,
                    onClick = {
                        val obj = objetivo.replace(',', '.').toDoubleOrNull() ?: 0.0
                        onGuardar(nombre.trim(), obj, icono.ifBlank { "🎯" }, fechaLimite, notas.ifBlank { null })
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

@Composable
private fun TarjetaCuentaAhorro(
    state: AhorrosState,
    onEditarSaldo: () -> Unit,
) {
    val acumulado = state.acumuladoApp
    val acumuladoPositivo = acumulado >= 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("💰", style = MaterialTheme.typography.headlineSmall)
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.cuenta_ahorro_titulo),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.cuenta_ahorro_explicacion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEditarSaldo) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.cuenta_ahorro_editar),
                    )
                }
            }

            // Saldo total destacado
            Column {
                Text(
                    stringResource(R.string.cuenta_ahorro_total),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = Formato.importe(state.saldoCuentaAhorro),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.cuenta_ahorro_saldo_inicial),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        Formato.importe(state.saldoInicialCuenta),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.cuenta_ahorro_acumulado),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = (if (acumuladoPositivo) "+" else "") + Formato.importe(acumulado),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (acumuladoPositivo) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogoSaldoInicial(
    saldoActual: Double,
    onCerrar: () -> Unit,
    onGuardar: (Double) -> Unit,
) {
    var texto by remember(saldoActual) {
        mutableStateOf(if (saldoActual == 0.0) "" else saldoActual.toString())
    }
    val valorValido = texto.replace(',', '.').toDoubleOrNull()?.let { it >= 0.0 } == true

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(stringResource(R.string.cuenta_ahorro_dialogo_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.cuenta_ahorro_dialogo_descripcion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text(stringResource(R.string.cuenta_ahorro_saldo_inicial)) },
                    suffix = { Text("€") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valorValido,
                onClick = {
                    val v = texto.replace(',', '.').toDoubleOrNull() ?: 0.0
                    onGuardar(v)
                },
            ) {
                Text(stringResource(R.string.accion_guardar))
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text(stringResource(R.string.accion_cancelar))
            }
        },
    )
}
