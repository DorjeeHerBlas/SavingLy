package com.misfinanzas.app.ui.screens.gastos

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import com.misfinanzas.app.domain.model.CategoriaGasto
import com.misfinanzas.app.domain.model.FuenteIngreso
import com.misfinanzas.app.domain.model.Gasto
import com.misfinanzas.app.domain.model.GastoRecurrente
import com.misfinanzas.app.domain.model.Ingreso
import com.misfinanzas.app.domain.model.MetodoPago
import com.misfinanzas.app.domain.model.PresupuestoMensual
import com.misfinanzas.app.ui.components.BarraProgreso
import com.misfinanzas.app.ui.components.CampoFecha
import com.misfinanzas.app.ui.components.EmojiBadge
import com.misfinanzas.app.ui.components.EstadoVacio
import com.misfinanzas.app.ui.components.OverlayCelebracionGasto
import com.misfinanzas.app.ui.components.OverlayCelebracionIngreso
import com.misfinanzas.app.ui.theme.ExpenseColor
import com.misfinanzas.app.ui.theme.IncomeColor
import com.misfinanzas.app.utils.Formato
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class GastosTab(val etiqueta: String) {
    GASTOS("Gastos"),
    INGRESOS("Ingresos"),
    PRESUPUESTOS("Presupuestos"),
    RECURRENTES("Recurrentes")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastosScreen(vm: GastosViewModel = hiltViewModel()) {
    val state by vm.estado.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var tab by remember { mutableStateOf(GastosTab.GASTOS) }
    var mostrarIngreso by remember { mutableStateOf(false) }
    var mostrarPresupuesto by remember { mutableStateOf(false) }
    var mostrarRecurrente by remember { mutableStateOf(false) }
    var celebracionGasto by remember { mutableStateOf<String?>(null) }
    var celebracionIngreso by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.mensaje) {
        state.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            vm.consumirMensaje()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Finanzas del mes") },
                actions = {
                    IconButton(onClick = { vm.cambiarMes(-1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior")
                    }
                    Text(Formato.mes(state.mes), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { vm.cambiarMes(1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente")
                    }
                }
            )
        },
        floatingActionButton = {
            when (tab) {
                GastosTab.GASTOS -> FloatingActionButton(onClick = { vm.abrirNuevo() }) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo gasto")
                }
                GastosTab.INGRESOS -> FloatingActionButton(onClick = { mostrarIngreso = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo ingreso")
                }
                GastosTab.PRESUPUESTOS -> ExtendedFloatingActionButton(
                    onClick = { mostrarPresupuesto = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Presupuesto") }
                )
                GastosTab.RECURRENTES -> ExtendedFloatingActionButton(
                    onClick = { mostrarRecurrente = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Recurrente") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ResumenMes(state)
            ScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = 16.dp) {
                GastosTab.entries.forEach { item ->
                    Tab(
                        selected = tab == item,
                        onClick = { tab = item },
                        text = { Text(item.etiqueta) }
                    )
                }
            }
            AnimatedContent(targetState = tab, label = "gastosTab") { selectedTab ->
            when (selectedTab) {
                GastosTab.GASTOS -> ListaGastos(
                    gastos = state.gastos,
                    onEditar = vm::abrirEditar,
                    onEliminar = { gasto ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.eliminar(gasto)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Gasto eliminado",
                                actionLabel = "Deshacer"
                            )
                            if (result == SnackbarResult.ActionPerformed) vm.restaurar(gasto)
                        }
                    }
                )
                GastosTab.INGRESOS -> ListaIngresos(
                    ingresos = state.ingresos,
                    onEliminar = { ingreso ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.eliminarIngreso(ingreso)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Ingreso eliminado",
                                actionLabel = "Deshacer"
                            )
                            if (result == SnackbarResult.ActionPerformed) vm.restaurarIngreso(ingreso)
                        }
                    }
                )
                GastosTab.PRESUPUESTOS -> ListaPresupuestos(
                    presupuestos = state.presupuestos,
                    gastoPorCategoria = state.gastoPorCategoria.associate { it.categoria to it.total },
                    onEliminar = vm::eliminarPresupuesto
                )
                GastosTab.RECURRENTES -> ListaRecurrentes(
                    recurrentes = state.recurrentes,
                    onAplicar = vm::aplicarRecurrentes,
                    onToggle = vm::toggleRecurrente,
                    onEliminar = vm::eliminarRecurrente
                )
            }
            }
        }
    }

    if (state.mostrarDialogo) {
        DialogoGasto(
            inicial = state.gastoEditando,
            onCerrar = { vm.cerrarDialogo() },
            onGuardar = { c, i, f, cat, m, n ->
                vm.guardar(c, i, f, cat, m, n)
                if (state.gastoEditando == null) celebracionGasto = Formato.importe(i)
            }
        )
    }
    if (mostrarIngreso) {
        DialogoIngreso(
            onCerrar = { mostrarIngreso = false },
            onGuardar = { c, i, f, fuente, n ->
                vm.guardarIngreso(c, i, f, fuente, n)
                mostrarIngreso = false
                celebracionIngreso = Formato.importe(i)
            }
        )
    }

    OverlayCelebracionGasto(
        visible = celebracionGasto != null,
        monto = celebracionGasto.orEmpty(),
        onFinished = { celebracionGasto = null },
    )
    OverlayCelebracionIngreso(
        visible = celebracionIngreso != null,
        monto = celebracionIngreso.orEmpty(),
        onFinished = { celebracionIngreso = null },
    )
    if (mostrarPresupuesto) {
        DialogoPresupuesto(
            onCerrar = { mostrarPresupuesto = false },
            onGuardar = { cat, limite ->
                vm.guardarPresupuesto(cat, limite)
                mostrarPresupuesto = false
            }
        )
    }
    if (mostrarRecurrente) {
        DialogoRecurrente(
            onCerrar = { mostrarRecurrente = false },
            onGuardar = { c, i, cat, m, dia, n ->
                vm.guardarRecurrente(c, i, cat, m, dia, n)
                mostrarRecurrente = false
            }
        )
    }
}

@Composable
private fun ResumenMes(state: GastosState) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ResumenDato("Gastos", Formato.importe(state.total), MaterialTheme.colorScheme.error, Modifier.weight(1f))
                ResumenDato("Ingresos", Formato.importe(state.totalIngresos), IncomeColor, Modifier.weight(1f))
                ResumenDato(
                    "Balance",
                    Formato.importe(state.balance),
                    if (state.balance >= 0) IncomeColor else ExpenseColor,
                    Modifier.weight(1f)
                )
            }
            Text(
                "${state.gastos.size} gastos • ${state.ingresos.size} ingresos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResumenDato(label: String, valor: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun ListaGastos(
    gastos: List<Gasto>,
    onEditar: (Gasto) -> Unit,
    onEliminar: (Gasto) -> Unit
) {
    if (gastos.isEmpty()) {
        EstadoVacio(
            titulo = "Sin gastos en este mes",
            descripcion = "Pulsa el botón + para añadir tu primer gasto",
            emoji = "🧾"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(gastos, key = { it.id }) { gasto ->
                FilaGasto(gasto, onEditar = { onEditar(gasto) }, onEliminar = { onEliminar(gasto) })
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun FilaGasto(gasto: Gasto, onEditar: () -> Unit, onEliminar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onEditar() }) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(emoji = gasto.categoria.emoji)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(gasto.concepto, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${gasto.categoria.emoji} ${gasto.categoria.etiqueta} • ${gasto.metodoPago.emoji} ${gasto.metodoPago.etiqueta} • ${Formato.fecha(gasto.fecha)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(Formato.importe(gasto.importe), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onEliminar) {
                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
            }
        }
    }
}

@Composable
private fun ListaIngresos(ingresos: List<Ingreso>, onEliminar: (Ingreso) -> Unit) {
    if (ingresos.isEmpty()) {
        EstadoVacio("Sin ingresos este mes", "Añade nóminas, ventas o ingresos extra.", "💶")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ingresos, key = { it.id }) { ingreso ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        EmojiBadge(emoji = ingreso.fuente.emoji)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ingreso.concepto, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${ingreso.fuente.etiqueta} • ${Formato.fecha(ingreso.fecha)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(Formato.importe(ingreso.importe), color = IncomeColor, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { onEliminar(ingreso) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ListaPresupuestos(
    presupuestos: List<PresupuestoMensual>,
    gastoPorCategoria: Map<CategoriaGasto, Double>,
    onEliminar: (PresupuestoMensual) -> Unit
) {
    if (presupuestos.isEmpty()) {
        EstadoVacio("Sin presupuestos", "Define límites por categoría para este mes.", "📏")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(presupuestos, key = { it.id }) { presupuesto ->
                val gastado = gastoPorCategoria[presupuesto.categoria] ?: 0.0
                val progreso = if (presupuesto.limite <= 0.0) 0f else (gastado / presupuesto.limite).toFloat()
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiBadge(emoji = presupuesto.categoria.emoji, tamaño = 36)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(presupuesto.categoria.etiqueta, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${Formato.importe(gastado)} de ${Formato.importe(presupuesto.limite)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("${(progreso * 100).toInt()}%", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onEliminar(presupuesto) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        BarraProgreso(
                            progreso = progreso.coerceAtMost(1f),
                            color = if (progreso >= 1f) ExpenseColor else IncomeColor
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ListaRecurrentes(
    recurrentes: List<GastoRecurrente>,
    onAplicar: () -> Unit,
    onToggle: (GastoRecurrente) -> Unit,
    onEliminar: (GastoRecurrente) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onAplicar,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) { Text("Aplicar recurrentes pendientes") }
        if (recurrentes.isEmpty()) {
            EstadoVacio("Sin recurrentes", "Añade alquiler, suscripciones u otros pagos mensuales.", "🔁")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recurrentes, key = { it.id }) { recurrente ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            EmojiBadge(emoji = recurrente.categoria.emoji)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(recurrente.concepto, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Día ${recurrente.diaMes} • ${Formato.importe(recurrente.importe)} • ${recurrente.categoria.emoji} ${recurrente.categoria.etiqueta} • ${recurrente.metodoPago.emoji} ${recurrente.metodoPago.etiqueta}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = recurrente.activo, onCheckedChange = { onToggle(recurrente) })
                            IconButton(onClick = { onEliminar(recurrente) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioSheet(
    titulo: String,
    onCerrar: () -> Unit,
    guardarHabilitado: Boolean,
    onGuardar: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
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
            Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            content()
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCerrar, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(
                    enabled = guardarHabilitado,
                    onClick = onGuardar,
                    modifier = Modifier.weight(1f)
                ) { Text("Guardar") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoGasto(
    inicial: Gasto?,
    onCerrar: () -> Unit,
    onGuardar: (String, Double, LocalDate, CategoriaGasto, MetodoPago, String?) -> Unit
) {
    var concepto by remember { mutableStateOf(inicial?.concepto ?: "") }
    var importe by remember { mutableStateOf(inicial?.importe?.toString() ?: "") }
    var categoria by remember { mutableStateOf(inicial?.categoria ?: CategoriaGasto.OTROS) }
    var metodoPago by remember { mutableStateOf(inicial?.metodoPago ?: MetodoPago.TARJETA) }
    var nota by remember { mutableStateOf(inicial?.nota ?: "") }
    var fecha by remember { mutableStateOf(inicial?.fecha ?: LocalDate.now()) }
    var expCategoria by remember { mutableStateOf(false) }
    var expMetodo by remember { mutableStateOf(false) }

    FormularioSheet(
        titulo = if (inicial == null) "Nuevo gasto" else "Editar gasto",
        onCerrar = onCerrar,
        guardarHabilitado = concepto.isNotBlank() && importe.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true,
        onGuardar = {
            onGuardar(
                concepto.trim(),
                importe.replace(',', '.').toDoubleOrNull() ?: 0.0,
                fecha,
                categoria,
                metodoPago,
                nota.ifBlank { null }
            )
        }
    ) {
        OutlinedTextField(concepto, { concepto = it }, label = { Text("Concepto") }, modifier = Modifier.fillMaxWidth())
        CampoImporte(importe, { importe = it }, "Importe €")
        CampoFecha("Fecha", fecha, { if (it != null) fecha = it })
        CampoCategoria(categoria, { categoria = it }, expCategoria, { expCategoria = it })
        CampoMetodo(metodoPago, { metodoPago = it }, expMetodo, { expMetodo = it })
        OutlinedTextField(nota, { nota = it }, label = { Text("Nota (opcional)") }, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoIngreso(
    onCerrar: () -> Unit,
    onGuardar: (String, Double, LocalDate, FuenteIngreso, String?) -> Unit
) {
    var concepto by remember { mutableStateOf("") }
    var importe by remember { mutableStateOf("") }
    var fuente by remember { mutableStateOf(FuenteIngreso.NOMINA) }
    var nota by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(LocalDate.now()) }
    var expFuente by remember { mutableStateOf(false) }

    FormularioSheet(
        titulo = "Nuevo ingreso",
        onCerrar = onCerrar,
        guardarHabilitado = concepto.isNotBlank() && importe.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true,
        onGuardar = {
            onGuardar(concepto.trim(), importe.replace(',', '.').toDoubleOrNull() ?: 0.0, fecha, fuente, nota.ifBlank { null })
        }
    ) {
        OutlinedTextField(concepto, { concepto = it }, label = { Text("Concepto") }, modifier = Modifier.fillMaxWidth())
        CampoImporte(importe, { importe = it }, "Importe €")
        CampoFecha("Fecha", fecha, { if (it != null) fecha = it })
        ExposedDropdownMenuBox(expanded = expFuente, onExpandedChange = { expFuente = it }) {
            OutlinedTextField(
                value = "${fuente.emoji} ${fuente.etiqueta}",
                onValueChange = {},
                readOnly = true,
                label = { Text("Fuente") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expFuente) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expFuente, onDismissRequest = { expFuente = false }) {
                FuenteIngreso.entries.forEach {
                    DropdownMenuItem(text = { Text("${it.emoji}  ${it.etiqueta}") }, onClick = { fuente = it; expFuente = false })
                }
            }
        }
        OutlinedTextField(nota, { nota = it }, label = { Text("Nota") }, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoPresupuesto(
    onCerrar: () -> Unit,
    onGuardar: (CategoriaGasto, Double) -> Unit
) {
    var categoria by remember { mutableStateOf(CategoriaGasto.ALIMENTACION) }
    var limite by remember { mutableStateOf("") }
    var expCategoria by remember { mutableStateOf(false) }

    FormularioSheet(
        titulo = "Presupuesto mensual",
        onCerrar = onCerrar,
        guardarHabilitado = limite.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true,
        onGuardar = { onGuardar(categoria, limite.replace(',', '.').toDoubleOrNull() ?: 0.0) }
    ) {
        CampoCategoria(categoria, { categoria = it }, expCategoria, { expCategoria = it })
        CampoImporte(limite, { limite = it }, "Límite €")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoRecurrente(
    onCerrar: () -> Unit,
    onGuardar: (String, Double, CategoriaGasto, MetodoPago, Int, String?) -> Unit
) {
    var concepto by remember { mutableStateOf("") }
    var importe by remember { mutableStateOf("") }
    var dia by remember { mutableStateOf("1") }
    var categoria by remember { mutableStateOf(CategoriaGasto.SUSCRIPCIONES) }
    var metodo by remember { mutableStateOf(MetodoPago.TARJETA) }
    var nota by remember { mutableStateOf("") }
    var expCategoria by remember { mutableStateOf(false) }
    var expMetodo by remember { mutableStateOf(false) }

    FormularioSheet(
        titulo = "Gasto recurrente",
        onCerrar = onCerrar,
        guardarHabilitado = concepto.isNotBlank() && importe.replace(',', '.').toDoubleOrNull()?.let { it > 0 } == true,
        onGuardar = {
            onGuardar(
                concepto.trim(),
                importe.replace(',', '.').toDoubleOrNull() ?: 0.0,
                categoria,
                metodo,
                dia.toIntOrNull()?.coerceIn(1, 31) ?: 1,
                nota.ifBlank { null }
            )
        }
    ) {
        OutlinedTextField(concepto, { concepto = it }, label = { Text("Concepto") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CampoImporte(importe, { importe = it }, "Importe €", Modifier.weight(1f))
            OutlinedTextField(
                value = dia,
                onValueChange = { dia = it.filter(Char::isDigit).take(2) },
                label = { Text("Día") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(0.7f)
            )
        }
        CampoCategoria(categoria, { categoria = it }, expCategoria, { expCategoria = it })
        CampoMetodo(metodo, { metodo = it }, expMetodo, { expMetodo = it })
        OutlinedTextField(nota, { nota = it }, label = { Text("Nota") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CampoImporte(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' || c == ',' }) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoCategoria(
    categoria: CategoriaGasto,
    onCategoria: (CategoriaGasto) -> Unit,
    expanded: Boolean,
    onExpanded: (Boolean) -> Unit
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpanded) {
        OutlinedTextField(
            value = "${categoria.emoji} ${categoria.etiqueta}",
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoría") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpanded(false) }) {
            CategoriaGasto.entries.forEach {
                DropdownMenuItem(text = { Text("${it.emoji}  ${it.etiqueta}") }, onClick = { onCategoria(it); onExpanded(false) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoMetodo(
    metodo: MetodoPago,
    onMetodo: (MetodoPago) -> Unit,
    expanded: Boolean,
    onExpanded: (Boolean) -> Unit
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpanded) {
        OutlinedTextField(
            value = "${metodo.emoji} ${metodo.etiqueta}",
            onValueChange = {},
            readOnly = true,
            label = { Text("Método de pago") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpanded(false) }) {
            MetodoPago.entries.forEach {
                DropdownMenuItem(text = { Text("${it.emoji}  ${it.etiqueta}") }, onClick = { onMetodo(it); onExpanded(false) })
            }
        }
    }
}
