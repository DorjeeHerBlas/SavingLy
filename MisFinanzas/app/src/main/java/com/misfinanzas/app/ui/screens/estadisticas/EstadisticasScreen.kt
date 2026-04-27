package com.misfinanzas.app.ui.screens.estadisticas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.misfinanzas.app.ui.components.BarraProgreso
import com.misfinanzas.app.ui.components.EstadoVacio
import com.misfinanzas.app.ui.theme.CollectionAccent
import com.misfinanzas.app.ui.theme.ExpenseColor
import com.misfinanzas.app.ui.theme.IncomeColor
import com.misfinanzas.app.ui.theme.SavingsColor
import com.misfinanzas.app.utils.Formato
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.Shape

private val ExpenseOled = Color(0xFFFF9B92)
private val IncomeOled = Color(0xFF70E6A1)
private val SavingsOled = Color(0xFF5DE0C6)
private val CollectionOled = Color(0xFFC6B3FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasScreen(vm: EstadisticasViewModel = hiltViewModel()) {
    val state by vm.estado.collectAsState()
    val producer = remember { CartesianChartModelProducer() }
    val mesesUnificados = remember(state.totalesPorMes, state.ahorroPorMes) {
        (state.totalesPorMes.map { it.first } + state.ahorroPorMes.map { it.first })
            .distinct()
            .sorted()
    }
    val mesesLabel = remember(mesesUnificados) { mesesUnificados.map { Formato.mes(it).take(3) } }
    val gastosPorMes = remember(state.totalesPorMes) { state.totalesPorMes.toMap() }
    val ahorroPorMes = remember(state.ahorroPorMes) { state.ahorroPorMes.toMap() }
    val gastoColor = MaterialTheme.colorScheme.primary
    val ahorroColor = rememberMetricColor(SavingsColor, SavingsOled)

    LaunchedEffect(mesesUnificados, gastosPorMes, ahorroPorMes) {
        if (mesesUnificados.isNotEmpty()) {
            producer.runTransaction {
                columnSeries {
                    series(mesesUnificados.map { gastosPorMes[it] ?: 0.0 })
                    series(mesesUnificados.map { ahorroPorMes[it] ?: 0.0 })
                }
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Estadísticas") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ResumenAnual(state)
            ComparativaMensual(state)

            if (mesesUnificados.isEmpty()) {
                EstadoVacio(
                    titulo = "Aún sin datos",
                    descripcion = "Cuando registres gastos o ahorros verás gráficos y desgloses aquí.",
                    emoji = "📊"
                )
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Gasto y ahorro por mes", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Evolución mensual comparada",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        LeyendaSeries(
                            entries = listOf(
                                "Gasto" to gastoColor,
                                "Ahorro" to ahorroColor,
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberColumnCartesianLayer(
                                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                        rememberLineComponent(
                                            color = gastoColor,
                                            thickness = 10.dp,
                                            shape = Shape.rounded(allPercent = 25)
                                        ),
                                        rememberLineComponent(
                                            color = ahorroColor,
                                            thickness = 10.dp,
                                            shape = Shape.rounded(allPercent = 25)
                                        )
                                    )
                                ),
                                startAxis = rememberStartAxis(label = rememberAxisLabelComponent()),
                                bottomAxis = rememberBottomAxis(
                                    label = rememberAxisLabelComponent(),
                                    valueFormatter = { v, _, _ -> mesesLabel.getOrNull(v.toInt()) ?: "" }
                                )
                            ),
                            modelProducer = producer,
                            scrollState = rememberVicoScrollState(scrollEnabled = true),
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                        )
                    }
                }
            }

            BarrasMetodoPago(state)
            TopCategorias(state)
            AhorroMensual(state)
        }
    }
}

@Composable
private fun ResumenAnual(state: EstadisticasState) {
    val expense = rememberMetricColor(ExpenseColor, ExpenseOled)
    val collections = rememberMetricColor(CollectionAccent, CollectionOled)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("Gasto anual", style = MaterialTheme.typography.titleMedium)
                    Text("Separado por tipo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(Formato.importe(state.totalAnual), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MiniDato("Esencial", Formato.importe(state.gastoEsencialAnual), expense, Modifier.weight(1f))
                MiniDato("Colecciones", Formato.importe(state.gastoColeccionesAnual), collections, Modifier.weight(1f))
                MiniDato(
                    "Otros",
                    Formato.importe((state.totalAnual - state.gastoEsencialAnual - state.gastoColeccionesAnual).coerceAtLeast(0.0)),
                    MaterialTheme.colorScheme.onSurface,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ComparativaMensual(state: EstadisticasState) {
    val income = rememberMetricColor(IncomeColor, IncomeOled)
    val expense = rememberMetricColor(ExpenseColor, ExpenseOled)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Mes actual vs anterior", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MiniDato("Actual", Formato.importe(state.totalMesActual), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                MiniDato("Anterior", Formato.importe(state.totalMesAnterior), MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
                MiniDato(
                    "Diferencia",
                    Formato.importe(state.diferenciaMes),
                    if (state.diferenciaMes <= 0) income else expense,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MiniDato(label: String, valor: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BarrasMetodoPago(state: EstadisticasState) {
    val expense = rememberMetricColor(ExpenseColor, ExpenseOled)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Gasto por método de pago", style = MaterialTheme.typography.titleMedium)
            if (state.totalesPorMetodo.isEmpty()) {
                Text("Sin datos", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val max = state.totalesPorMetodo.maxOf { it.total }.coerceAtLeast(0.01)
                state.totalesPorMetodo.forEach { total ->
                    FilaBarra("${total.metodoPago.emoji} ${total.metodoPago.etiqueta}", Formato.importe(total.total), (total.total / max).toFloat(), expense)
                }
            }
        }
    }
}

@Composable
private fun TopCategorias(state: EstadisticasState) {
    val collection = rememberMetricColor(CollectionAccent, CollectionOled)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Top 5 categorías del año", style = MaterialTheme.typography.titleMedium)
            if (state.topCategorias.isEmpty()) {
                Text("Sin gastos en este período", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val max = state.topCategorias.maxOf { it.total }.coerceAtLeast(0.01)
                state.topCategorias.forEach { tc ->
                    FilaBarra("${tc.categoria.emoji} ${tc.categoria.etiqueta}", Formato.importe(tc.total), (tc.total / max).toFloat(), collection)
                }
            }
        }
    }
}

@Composable
private fun AhorroMensual(state: EstadisticasState) {
    val savings = rememberMetricColor(SavingsColor, SavingsOled)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Evolución de ahorro por mes", style = MaterialTheme.typography.titleMedium)
            if (state.ahorroPorMes.isEmpty()) {
                Text("Sin aportaciones todavía", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val max = state.ahorroPorMes.maxOf { it.second }.coerceAtLeast(0.01)
                state.ahorroPorMes.forEach { (mes, total) ->
                    FilaBarra(Formato.mes(mes), Formato.importe(total), (total / max).toFloat(), savings)
                }
            }
        }
    }
}

@Composable
private fun rememberMetricColor(lightColor: Color, oledColor: Color): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.02f) oledColor else lightColor

@Composable
private fun LeyendaSeries(entries: List<Pair<String, Color>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        entries.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FilaBarra(label: String, valor: String, progreso: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
            Text(valor, fontWeight = FontWeight.Medium)
        }
        BarraProgreso(progreso = progreso, color = color)
    }
}
