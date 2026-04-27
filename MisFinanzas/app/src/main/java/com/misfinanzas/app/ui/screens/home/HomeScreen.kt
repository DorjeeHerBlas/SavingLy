package com.misfinanzas.app.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.misfinanzas.app.R
import com.misfinanzas.app.ui.components.BarraProgreso
import com.misfinanzas.app.ui.theme.CollectionAccent
import com.misfinanzas.app.ui.theme.CollectionContainer
import com.misfinanzas.app.ui.theme.ExpenseColor
import com.misfinanzas.app.ui.theme.ExpenseContainer
import com.misfinanzas.app.ui.theme.IncomeColor
import com.misfinanzas.app.ui.theme.IncomeContainer
import com.misfinanzas.app.ui.theme.SavingsColor
import com.misfinanzas.app.ui.theme.SavingsContainer
import com.misfinanzas.app.ui.theme.WishlistAccent
import com.misfinanzas.app.ui.theme.WishlistContainer
import com.misfinanzas.app.utils.Formato
import java.time.LocalDate
import kotlin.math.abs

private val IncomeColorOled = Color(0xFF70E6A1)
private val ExpenseColorOled = Color(0xFFFF9B92)
private val SavingsColorOled = Color(0xFF5DE0C6)
private val CollectionColorOled = Color(0xFFC6B3FF)
private val WishlistColorOled = Color(0xFFFFC078)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onIrAGastos: () -> Unit,
    onIrAAhorros: () -> Unit,
    onIrAColecciones: () -> Unit,
    onIrAConfiguracion: () -> Unit,
    onAbrirMeta: (Long) -> Unit,
    onAbrirColeccion: (Long) -> Unit,
    onAbrirItem: (Long, Long) -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val state by vm.estado.collectAsState()
    var busqueda by rememberSaveable { mutableStateOf("") }
    val resultadosBusqueda = remember(busqueda, state.resultadosBusqueda) {
        val query = busqueda.trim()
        if (query.isBlank()) emptyList() else {
            state.resultadosBusqueda
                .filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.subtitle.contains(query, ignoreCase = true) ||
                        it.type.etiqueta.contains(query, ignoreCase = true)
                }
                .take(8)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onIrAConfiguracion) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuración")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BuscadorGlobal(
                busqueda = busqueda,
                onBusqueda = { busqueda = it },
                resultados = resultadosBusqueda,
                onAbrirResultado = { item ->
                    when (item.type) {
                        HomeSearchType.GASTO, HomeSearchType.INGRESO -> onIrAGastos()
                        HomeSearchType.META -> onAbrirMeta(item.id)
                        HomeSearchType.COLECCION -> onAbrirColeccion(item.id)
                        HomeSearchType.ITEM -> onAbrirItem(item.parentId, item.id)
                    }
                    busqueda = ""
                }
            )
            BalanceDashboard(state = state, onClick = onIrAGastos)

            Text("Actividad", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                DashboardMetric(
                    title = "Ahorrado",
                    value = Formato.importe(state.ahorroTotal),
                    subtitle = "${state.metasActivas} metas activas",
                    icon = Icons.Outlined.Savings,
                    accent = SavingsColor,
                    container = SavingsContainer,
                    modifier = Modifier.weight(1f)
                )
                DashboardMetric(
                    title = "Colecciones",
                    value = "${state.totalColecciones}",
                    subtitle = "${state.totalItemsTengo} ítems",
                    icon = Icons.Outlined.Inventory2,
                    accent = CollectionAccent,
                    container = CollectionContainer,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                DashboardMetric(
                    title = "Wishlist",
                    value = "${state.totalItemsQuiero}",
                    subtitle = Formato.importe(state.costeWishlist),
                    icon = Icons.Outlined.Inventory2,
                    accent = WishlistAccent,
                    container = WishlistContainer,
                    modifier = Modifier.weight(1f)
                )
                DashboardMetric(
                    title = "Gastado",
                    value = Formato.importe(state.gastoMes),
                    subtitle = Formato.mes(LocalDate.now()),
                    icon = Icons.Outlined.Receipt,
                    accent = ExpenseColor,
                    container = ExpenseContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Atajos", style = MaterialTheme.typography.titleLarge)
            QuickAction("Registrar gasto", Icons.Outlined.Receipt, ExpenseColor, onIrAGastos)
            QuickAction("Aportar a una meta", Icons.Outlined.Savings, SavingsColor, onIrAAhorros)
            QuickAction("Ver colecciones", Icons.Outlined.Inventory2, CollectionAccent, onIrAColecciones)
        }
    }
}

@Composable
private fun BuscadorGlobal(
    busqueda: String,
    onBusqueda: (String) -> Unit,
    resultados: List<HomeSearchItem>,
    onAbrirResultado: (HomeSearchItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = busqueda,
            onValueChange = onBusqueda,
            singleLine = true,
            label = { Text("Buscar") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (busqueda.isNotBlank()) {
                    IconButton(onClick = { onBusqueda("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Limpiar búsqueda")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        AnimatedVisibility(
            visible = busqueda.isNotBlank(),
            enter = fadeIn(animationSpec = tween(160)) + expandVertically(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(animationSpec = tween(180))
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    if (resultados.isEmpty()) {
                        Text(
                            "Sin resultados",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        resultados.forEach { item ->
                            FilaResultadoBusqueda(item = item, onClick = { onAbrirResultado(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaResultadoBusqueda(item: HomeSearchItem, onClick: () -> Unit) {
    val icon = when (item.type) {
        HomeSearchType.GASTO -> Icons.AutoMirrored.Outlined.TrendingDown
        HomeSearchType.INGRESO -> Icons.AutoMirrored.Outlined.TrendingUp
        HomeSearchType.META -> Icons.Outlined.Savings
        HomeSearchType.COLECCION, HomeSearchType.ITEM -> Icons.Outlined.Inventory2
    }
    val accent = when (item.type) {
        HomeSearchType.GASTO -> ExpenseColor
        HomeSearchType.INGRESO -> IncomeColor
        HomeSearchType.META -> SavingsColor
        HomeSearchType.COLECCION, HomeSearchType.ITEM -> CollectionAccent
    }.let { if (MaterialTheme.colorScheme.background.luminance() < 0.02f) it.oledAccent() else it }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        item.amount?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BalanceDashboard(state: HomeState, onClick: () -> Unit) {
    val oledDark = MaterialTheme.colorScheme.background.luminance() < 0.02f
    val balanceColor = when {
        state.balanceMes >= 0 && oledDark -> IncomeColorOled
        state.balanceMes >= 0 -> IncomeColor
        oledDark -> ExpenseColorOled
        else -> ExpenseColor
    }
    val incomeColor = if (oledDark) IncomeColorOled else IncomeColor
    val expenseColor = if (oledDark) ExpenseColorOled else ExpenseColor
    val gastoRatio by animateFloatAsState(
        targetValue = if (state.ingresoMes <= 0.0) 0f else (state.gastoMes / state.ingresoMes).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "gastoRatio"
    )

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (oledDark) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Balance del mes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AnimatedContent(targetState = Formato.importe(state.balanceMes), label = "balance") { balance ->
                        Text(
                            balance,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = balanceColor
                        )
                    }
                    Text(
                        if (state.balanceMes >= 0) "Disponible tras gastos" else "Por encima de ingresos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(balanceColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (state.balanceMes >= 0) Icons.AutoMirrored.Outlined.TrendingUp else Icons.AutoMirrored.Outlined.TrendingDown,
                        contentDescription = null,
                        tint = balanceColor
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                BalancePart("Ingresos", Formato.importe(state.ingresoMes), incomeColor, Modifier.weight(1f))
                BalancePart("Gastos", Formato.importe(state.gastoMes), expenseColor, Modifier.weight(1f))
                BalancePart("Margen", Formato.importe(abs(state.balanceMes)), balanceColor, Modifier.weight(1f))
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Uso de ingresos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(gastoRatio * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                BarraProgreso(
                    progreso = gastoRatio,
                    color = if (gastoRatio >= 0.9f) expenseColor else incomeColor
                )
            }
        }
    }
}

@Composable
private fun BalancePart(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun DashboardMetric(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    container: Color,
    modifier: Modifier = Modifier
) {
    val oledDark = MaterialTheme.colorScheme.background.luminance() < 0.02f
    val metricAccent = if (oledDark) accent.oledAccent() else accent
    val metricContainer = if (oledDark) MaterialTheme.colorScheme.surfaceContainerHigh else container
    val titleColor = if (oledDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    val subtitleColor = if (oledDark) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant

    ElevatedCard(
        modifier = modifier.heightIn(min = 138.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = metricContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(metricAccent.copy(alpha = if (oledDark) 0.22f else 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = metricAccent)
            }
            Text(title, style = MaterialTheme.typography.labelLarge, color = titleColor)
            AnimatedContent(targetState = value, label = "metricValue") { animatedValue ->
                Text(animatedValue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = metricAccent)
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleColor, maxLines = 1)
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    val oledDark = MaterialTheme.colorScheme.background.luminance() < 0.02f
    val actionAccent = if (oledDark) accent.oledAccent() else accent
    ElevatedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = actionAccent.copy(alpha = if (oledDark) 0.16f else 0.10f),
            contentColor = actionAccent
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text(label, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
    }
}

private fun Color.oledAccent(): Color = when (this) {
    IncomeColor -> IncomeColorOled
    ExpenseColor -> ExpenseColorOled
    SavingsColor -> SavingsColorOled
    CollectionAccent -> CollectionColorOled
    WishlistAccent -> WishlistColorOled
    else -> this
}
