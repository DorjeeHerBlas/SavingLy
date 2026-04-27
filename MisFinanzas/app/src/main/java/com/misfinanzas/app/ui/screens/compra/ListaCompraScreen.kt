package com.misfinanzas.app.ui.screens.compra

import android.content.Intent
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
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.misfinanzas.app.R
import com.misfinanzas.app.domain.model.CategoriaProducto
import com.misfinanzas.app.domain.model.ItemCompra
import com.misfinanzas.app.ui.components.EstadoVacio
import com.misfinanzas.app.utils.Formato

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaCompraScreen(vm: ListaCompraViewModel = hiltViewModel()) {
    val state by vm.estado.collectAsState()
    val context = LocalContext.current
    val tituloAppName = stringResource(R.string.app_name)
    val cabeceraComparte = stringResource(R.string.compra_compartir_cabecera, tituloAppName)
    val sectionPendientes = stringResource(R.string.compra_pendientes)
    val sectionComprados = stringResource(R.string.compra_comprados)
    val labelTotalEstimado = stringResource(R.string.compra_total_estimado)
    val sinPrecio = stringResource(R.string.compra_sin_precio)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.compra_titulo)) },
                actions = {
                    IconButton(
                        enabled = state.items.isNotEmpty(),
                        onClick = {
                            val texto = construirTextoCompartir(
                                state = state,
                                cabecera = cabeceraComparte,
                                seccionPendientes = sectionPendientes,
                                seccionComprados = sectionComprados,
                                etiquetaTotal = labelTotalEstimado,
                                sinPrecio = sinPrecio,
                            )
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, texto)
                                putExtra(Intent.EXTRA_SUBJECT, tituloAppName)
                            }
                            context.startActivity(
                                Intent.createChooser(send, cabeceraComparte)
                            )
                        }
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.compra_compartir))
                    }
                    IconButton(
                        enabled = state.comprados.isNotEmpty(),
                        onClick = { vm.limpiarComprados() },
                    ) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = stringResource(R.string.compra_limpiar_comprados))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.abrirNuevo() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.compra_nuevo_item)) }
            )
        }
    ) { padding ->
        if (state.items.isEmpty()) {
            Box(Modifier.padding(padding)) {
                EstadoVacio(
                    titulo = stringResource(R.string.compra_vacio_titulo),
                    descripcion = stringResource(R.string.compra_vacio_descripcion),
                    emoji = "🛒",
                    accion = {
                        Button(onClick = { vm.abrirNuevo() }) {
                            Text(stringResource(R.string.compra_nuevo_item))
                        }
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.totalEstimado > 0) {
                    item {
                        TarjetaResumen(
                            totalEstimado = state.totalEstimado,
                            pendientes = state.pendientes.size,
                            comprados = state.comprados.size,
                        )
                    }
                }
                state.porCategoria.toSortedMap(compareBy { it.ordinal }).forEach { (cat, lista) ->
                    item(key = "header_${cat.name}") { CabeceraCategoria(cat, lista.size) }
                    items(lista, key = { "p_${it.id}" }) { item ->
                        FilaItemCompra(
                            item = item,
                            onToggle = { vm.toggleComprado(item) },
                            onClick = { vm.abrirEditar(item) },
                            onEliminar = { vm.eliminar(item) },
                        )
                    }
                }
                if (state.comprados.isNotEmpty()) {
                    item(key = "header_comprados") {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            sectionComprados,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    items(state.comprados, key = { "c_${it.id}" }) { item ->
                        FilaItemCompra(
                            item = item,
                            onToggle = { vm.toggleComprado(item) },
                            onClick = { vm.abrirEditar(item) },
                            onEliminar = { vm.eliminar(item) },
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (state.mostrarDialogo) {
        DialogoItemCompra(
            inicial = state.itemEditando,
            onCerrar = { vm.cerrarDialogo() },
            onGuardar = { n, c, u, cat, p, urg, notas ->
                vm.guardar(n, c, u, cat, p, urg, notas)
            }
        )
    }
}

@Composable
private fun TarjetaResumen(totalEstimado: Double, pendientes: Int, comprados: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.compra_total_estimado),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Text(
                    Formato.importe(totalEstimado),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$pendientes ${stringResource(R.string.compra_pendientes_corto)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (comprados > 0) {
                    Text(
                        "$comprados ${stringResource(R.string.compra_comprados_corto)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CabeceraCategoria(cat: CategoriaProducto, conteo: Int) {
    Row(
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(cat.emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        Text(
            cat.etiqueta,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        Text(
            "$conteo",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FilaItemCompra(
    item: ItemCompra,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onEliminar: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (item.comprado) MaterialTheme.colorScheme.surfaceContainerLowest
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = item.comprado, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.urgente && !item.comprado) {
                        Icon(
                            Icons.Outlined.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = item.nombre,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (item.comprado) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.comprado) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                    )
                }
                val detalles = buildList {
                    add(formatearCantidad(item.cantidad, item.unidad))
                    item.precioEstimado?.let { add(Formato.importe(it * item.cantidad)) }
                    item.notas?.takeIf { it.isNotBlank() }?.let { add(it) }
                }.joinToString(" · ")
                if (detalles.isNotBlank()) {
                    Text(
                        detalles,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoItemCompra(
    inicial: ItemCompra?,
    onCerrar: () -> Unit,
    onGuardar: (String, Double, String?, CategoriaProducto, Double?, Boolean, String?) -> Unit,
) {
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var cantidadTxt by remember { mutableStateOf(inicial?.cantidad?.let { quitarCero(it) } ?: "1") }
    var unidad by remember { mutableStateOf(inicial?.unidad ?: "") }
    var categoria by remember { mutableStateOf(inicial?.categoria ?: CategoriaProducto.OTROS) }
    var precioTxt by remember { mutableStateOf(inicial?.precioEstimado?.let { quitarCero(it) } ?: "") }
    var urgente by remember { mutableStateOf(inicial?.urgente ?: false) }
    var notas by remember { mutableStateOf(inicial?.notas ?: "") }
    var menuCategoria by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (inicial == null) stringResource(R.string.compra_nuevo_item)
                else stringResource(R.string.compra_editar_item),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.compra_label_nombre)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cantidadTxt,
                        onValueChange = { cantidadTxt = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                        label = { Text(stringResource(R.string.compra_label_cantidad)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = unidad,
                        onValueChange = { if (it.length <= 6) unidad = it },
                        label = { Text(stringResource(R.string.compra_label_unidad)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                ExposedDropdownMenuBox(
                    expanded = menuCategoria,
                    onExpandedChange = { menuCategoria = !menuCategoria },
                ) {
                    OutlinedTextField(
                        value = "${categoria.emoji} ${categoria.etiqueta}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.compra_label_categoria)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuCategoria) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = menuCategoria,
                        onDismissRequest = { menuCategoria = false },
                    ) {
                        CategoriaProducto.entries.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.emoji} ${c.etiqueta}") },
                                onClick = { categoria = c; menuCategoria = false },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = precioTxt,
                    onValueChange = { precioTxt = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text(stringResource(R.string.compra_label_precio)) },
                    suffix = { Text("€") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = urgente, onCheckedChange = { urgente = it })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.compra_label_urgente))
                }
                OutlinedTextField(
                    value = notas, onValueChange = { notas = it },
                    label = { Text(stringResource(R.string.compra_label_notas)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCerrar, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.accion_cancelar))
                }
                Button(
                    enabled = nombre.isNotBlank() &&
                        cantidadTxt.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true,
                    onClick = {
                        val cant = cantidadTxt.replace(',', '.').toDoubleOrNull() ?: 1.0
                        val precio = precioTxt.replace(',', '.').toDoubleOrNull()
                        onGuardar(
                            nombre.trim(),
                            cant,
                            unidad.ifBlank { null },
                            categoria,
                            precio,
                            urgente,
                            notas.ifBlank { null },
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.accion_guardar))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun formatearCantidad(cantidad: Double, unidad: String?): String {
    val cant = quitarCero(cantidad)
    return if (unidad.isNullOrBlank()) cant else "$cant $unidad"
}

private fun quitarCero(d: Double): String =
    if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()

private fun construirTextoCompartir(
    state: ListaCompraState,
    cabecera: String,
    seccionPendientes: String,
    seccionComprados: String,
    etiquetaTotal: String,
    sinPrecio: String,
): String {
    val sb = StringBuilder()
    sb.appendLine("🛒 $cabecera").appendLine()
    if (state.pendientes.isNotEmpty()) {
        sb.appendLine("— $seccionPendientes —")
        state.pendientes
            .sortedWith(compareBy({ !it.urgente }, { it.categoria.ordinal }, { it.nombre }))
            .forEach { i ->
                val cant = formatearCantidad(i.cantidad, i.unidad)
                val flame = if (i.urgente) "🔥 " else ""
                val precio = i.precioEstimado?.let { " · ${Formato.importe(it * i.cantidad)}" } ?: ""
                sb.appendLine("• $flame${i.nombre} ($cant)$precio")
            }
        if (state.totalEstimado > 0) {
            sb.appendLine().appendLine("$etiquetaTotal: ${Formato.importe(state.totalEstimado)}")
        } else {
            sb.appendLine().appendLine(sinPrecio)
        }
    }
    if (state.comprados.isNotEmpty()) {
        sb.appendLine().appendLine("— $seccionComprados —")
        state.comprados.forEach { i ->
            sb.appendLine("✓ ${i.nombre} (${formatearCantidad(i.cantidad, i.unidad)})")
        }
    }
    return sb.toString().trim()
}
