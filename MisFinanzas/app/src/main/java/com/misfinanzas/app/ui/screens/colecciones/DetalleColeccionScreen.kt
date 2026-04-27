package com.misfinanzas.app.ui.screens.colecciones

import androidx.compose.animation.animateContentSize
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.misfinanzas.app.data.remote.FunkoProduct
import com.misfinanzas.app.data.remote.ScryfallSet
import com.misfinanzas.app.data.remote.ScryfallSetCard
import com.misfinanzas.app.domain.model.CondicionItem
import com.misfinanzas.app.domain.model.EstadoItem
import com.misfinanzas.app.domain.model.ItemColeccion
import com.misfinanzas.app.domain.model.PrioridadWishlist
import com.misfinanzas.app.domain.model.TipoColeccion
import com.misfinanzas.app.ui.components.BarcodeScannerDialog
import com.misfinanzas.app.ui.components.CampoFecha
import com.misfinanzas.app.ui.components.CartaHoloViewer
import com.misfinanzas.app.ui.components.EmojiBadge
import com.misfinanzas.app.ui.components.EstadoVacio
import com.misfinanzas.app.ui.components.OverlayCelebracionItemConseguido
import com.misfinanzas.app.utils.Formato
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleColeccionScreen(
    coleccionId: Long,
    onVolver: () -> Unit,
    onAbrirItem: (Long) -> Unit,
    vm: DetalleColeccionViewModel = hiltViewModel()
) {
    LaunchedEffect(coleccionId) { vm.cargar(coleccionId) }
    val state by vm.estado.collectAsState()
    val apifyToken by vm.apifyToken.collectAsState()
    val coleccion = state.coleccion
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var mostrarBuscadorMtg by remember { mutableStateOf(false) }
    var mostrarBuscadorFunko by remember { mutableStateOf(false) }
    var cartaHolo by remember { mutableStateOf<ItemColeccion?>(null) }
    var celebracionItem by remember { mutableStateOf<String?>(null) }

    // Click en una carta MTG con imagen → abrir visor 3D holo-tilt en lugar del detalle.
    val onClickItem: (ItemColeccion) -> Unit = { item ->
        if (coleccion?.tipo == TipoColeccion.CARTAS_MTG && !item.rutaImagen.isNullOrBlank()) {
            cartaHolo = item
        } else {
            onAbrirItem(item.id)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(coleccion?.nombre ?: "Colección") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (coleccion != null) {
                        if (coleccion.tipo == TipoColeccion.CARTAS_MTG) {
                            IconButton(onClick = { mostrarBuscadorMtg = true }) {
                                Icon(Icons.Outlined.Search, contentDescription = "Buscar set MTG")
                            }
                        }
                        if (coleccion.tipo == TipoColeccion.FUNKO_POPS) {
                            IconButton(onClick = { mostrarBuscadorFunko = true }) {
                                Icon(Icons.Outlined.Search, contentDescription = "Buscar Funko Pop")
                            }
                        }
                        IconButton(onClick = { vm.toggleVista() }) {
                            Icon(
                                if (state.vistaGrid) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                                contentDescription = "Cambiar vista"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (coleccion != null) ExtendedFloatingActionButton(
                onClick = { vm.abrirNuevoItem() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Añadir ítem") }
            )
        }
    ) { padding ->
        if (coleccion == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                ResumenColeccion(state)
                ScrollableTabRow(
                    selectedTabIndex = when (state.filtro) {
                        null -> 0
                        EstadoItem.TENGO -> 1
                        EstadoItem.QUIERO -> 2
                        EstadoItem.RESERVADO -> 3
                        EstadoItem.PRESTADO -> 4
                        EstadoItem.VENDIDO -> 5
                    },
                    edgePadding = 16.dp
                ) {
                    Tab(selected = state.filtro == null, onClick = { vm.setFiltro(null) }, text = { Text("Todos") })
                    EstadoItem.entries.forEach { estado ->
                        Tab(selected = state.filtro == estado, onClick = { vm.setFiltro(estado) }, text = { Text(estado.etiqueta) })
                    }
                }
                if (state.items.isEmpty()) {
                    EstadoVacio(
                        titulo = "Sin ítems",
                        descripcion = "Añade libros, cartas, figuras o lo que coleccionas a esta colección.",
                        emoji = coleccion.icono
                    )
                } else if (state.vistaGrid) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            TarjetaItemGrid(
                                item = item,
                                onClick = { onClickItem(item) },
                                onEditar = { vm.abrirEditarItem(item) },
                                onEliminar = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    vm.eliminarItem(item)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar("Ítem eliminado", "Deshacer")
                                        if (result == SnackbarResult.ActionPerformed) vm.restaurarItem(item)
                                    }
                                },
                                onToggleFavorito = { vm.toggleFavorito(item) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            FilaItem(
                                item = item,
                                onClick = { onClickItem(item) },
                                onEditar = { vm.abrirEditarItem(item) },
                                onEliminar = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    vm.eliminarItem(item)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar("Ítem eliminado", "Deshacer")
                                        if (result == SnackbarResult.ActionPerformed) vm.restaurarItem(item)
                                    }
                                },
                                onToggleFavorito = { vm.toggleFavorito(item) },
                                onCambiarEstado = { nuevoEstado ->
                                    val esWishlistConseguido = item.estado == EstadoItem.QUIERO &&
                                        nuevoEstado == EstadoItem.TENGO
                                    vm.cambiarEstadoRapido(item, nuevoEstado)
                                    if (esWishlistConseguido) celebracionItem = item.nombre
                                }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    if (state.mostrarDialogoItem && coleccion != null) {
        DialogoItem(
            inicial = state.itemEditando,
            tipo = coleccion.tipo,
            onCerrar = { vm.cerrarDialogoItem() },
            onGuardar = vm::guardarItem
        )
    }

    cartaHolo?.let { item ->
        CartaHoloViewer(
            imagenUrl = item.rutaImagen.orEmpty(),
            nombre = item.nombre,
            subtitulo = listOfNotNull(item.setColeccion, item.rareza).joinToString(" · ").ifBlank { null },
            onDismiss = { cartaHolo = null },
        )
    }

    OverlayCelebracionItemConseguido(
        visible = celebracionItem != null,
        nombre = celebracionItem.orEmpty(),
        onFinished = { celebracionItem = null },
    )

    if (mostrarBuscadorMtg && coleccion?.tipo == TipoColeccion.CARTAS_MTG) {
        BuscadorSetMtgSheet(
            onCerrar = { mostrarBuscadorMtg = false },
            onBuscarSets = vm::buscarSetsMtg,
            onBuscar = vm::buscarCartasMtgPorSet,
            onAgregar = vm::agregarCartaMtg,
            onAgregarTodas = vm::agregarCartasMtg
        )
    }

    if (mostrarBuscadorFunko && coleccion?.tipo == TipoColeccion.FUNKO_POPS) {
        BuscadorFunkoSheet(
            apifyToken = apifyToken,
            onCerrar = { mostrarBuscadorFunko = false },
            onBuscar = vm::buscarFunkos,
            onAgregar = vm::agregarFunko
        )
    }
}

@Composable
private fun ResumenColeccion(state: DetalleColeccionState) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                ColumnaResumen("Tengo", "${state.totalTengo}", Color(EstadoItem.TENGO.color.toInt()))
                ColumnaResumen("Quiero", "${state.totalQuiero}", Color(EstadoItem.QUIERO.color.toInt()))
                ColumnaResumen("Pagado", Formato.importe(state.valorInvertido), MaterialTheme.colorScheme.primary)
                ColumnaResumen("Estimado", Formato.importe(state.valorEstimado), MaterialTheme.colorScheme.tertiary)
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                ColumnaResumen("Diferencia", Formato.importe(state.diferenciaValor), if (state.diferenciaValor >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
                ColumnaResumen("Wishlist", Formato.importe(state.costeWishlist), Color(EstadoItem.QUIERO.color.toInt()))
                val sugerido = state.itemSugerido
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text("Siguiente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        sugerido?.nombre ?: "-",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnaResumen(label: String, valor: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TarjetaItemGrid(
    item: ItemColeccion,
    onClick: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onToggleFavorito: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.animateContentSize(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box {
                ImagenItem(item, Modifier.fillMaxWidth().aspectRatio(1f))
                AssistChip(
                    onClick = {},
                    label = { Text(item.estado.etiqueta) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(item.estado.color.toInt()).copy(alpha = 0.88f),
                        labelColor = Color.White
                    ),
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                )
                IconButton(
                    onClick = onToggleFavorito,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f))
                ) {
                    Icon(
                        if (item.esFavorito) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorito",
                        tint = if (item.esFavorito) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (item.estado == EstadoItem.QUIERO || item.estado == EstadoItem.RESERVADO) {
                        Text(
                            item.prioridad.etiqueta,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(item.estado.color.toInt()),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(item.estado.color.toInt()).copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        (item.precioPagado ?: item.precio)?.let { Formato.importe(it) } ?: item.condicion.etiqueta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onEditar) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = onEliminar) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaItem(
    item: ItemColeccion,
    onClick: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onToggleFavorito: () -> Unit,
    onCambiarEstado: (EstadoItem) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ImagenItem(item, Modifier.size(56.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.nombre, style = MaterialTheme.typography.titleMedium)
                    if (item.cantidad > 1) {
                        Spacer(Modifier.width(6.dp))
                        Text("x${item.cantidad}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val precioMostrar = item.precioPagado ?: item.precio
                val infoExtra = buildList {
                    add(item.estado.etiqueta)
                    if (item.estado == EstadoItem.QUIERO) add("Prioridad ${item.prioridad.etiqueta}")
                    if (item.rareza != null) add(item.rareza)
                    if (precioMostrar != null) add(Formato.importe(precioMostrar))
                }.joinToString(" • ")
                Text(infoExtra, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onToggleFavorito) {
                Icon(
                    if (item.esFavorito) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorito",
                    tint = if (item.esFavorito) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                AssistChip(onClick = { menuExpanded = true }, label = { Text(item.estado.etiqueta) })
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    EstadoItem.entries.forEach { e ->
                        DropdownMenuItem(text = { Text(e.etiqueta) }, onClick = { menuExpanded = false; onCambiarEstado(e) })
                    }
                }
            }
            IconButton(onClick = onEditar) {
                Icon(Icons.Outlined.Edit, contentDescription = "Editar")
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
            }
        }
    }
}

@Composable
private fun ImagenItem(item: ItemColeccion, modifier: Modifier) {
    val shape = RoundedCornerShape(8.dp)
    if (item.rutaImagen.isNullOrBlank()) {
        Box(
            modifier = modifier.clip(shape).background(Color(item.estado.color.toInt()).copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                when (item.estado) {
                    EstadoItem.TENGO -> "✅"
                    EstadoItem.QUIERO -> "💭"
                    EstadoItem.RESERVADO -> "🔖"
                    EstadoItem.PRESTADO -> "🤝"
                    EstadoItem.VENDIDO -> "💰"
                },
                style = MaterialTheme.typography.headlineMedium
            )
        }
    } else {
        SubcomposeAsyncImage(
            model = item.rutaImagen,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant),
            loading = { ImagenSkeleton(Modifier.fillMaxSize()) },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sin imagen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            success = { SubcomposeAsyncImageContent() }
        )
    }
}

@Composable
private fun ImagenSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier.size(34.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuscadorSetMtgSheet(
    onCerrar: () -> Unit,
    onBuscarSets: suspend () -> List<ScryfallSet>,
    onBuscar: suspend (String) -> List<ScryfallSetCard>,
    onAgregar: suspend (ScryfallSetCard) -> Boolean,
    onAgregarTodas: suspend (List<ScryfallSetCard>) -> Int
) {
    val scope = rememberCoroutineScope()
    var sets by remember { mutableStateOf<List<ScryfallSet>>(emptyList()) }
    var filtro by remember { mutableStateOf("") }
    var setSeleccionado by remember { mutableStateOf<ScryfallSet?>(null) }
    var cartas by remember { mutableStateOf<List<ScryfallSetCard>>(emptyList()) }
    var cargandoSets by remember { mutableStateOf(true) }
    var cargandoCartas by remember { mutableStateOf(false) }
    var agregandoTodas by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    val setsFiltrados = remember(sets, filtro) {
        val q = filtro.trim()
        if (q.isBlank()) sets else sets.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.code.contains(q, ignoreCase = true) ||
                it.setType.contains(q, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) {
        runCatching { onBuscarSets() }
            .onSuccess {
                sets = it
                mensaje = if (it.isEmpty()) "No pude cargar sets" else null
            }
            .onFailure { mensaje = "No pude cargar los sets de Scryfall" }
        cargandoSets = false
    }

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val selected = setSeleccionado
            if (selected == null) {
                Text("Sets de MTG", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = filtro,
                    onValueChange = { filtro = it },
                    label = { Text("Buscar por nombre, código o tipo") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                mensaje?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (it.startsWith("No")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                if (cargandoSets) {
                    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 620.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(setsFiltrados, key = { it.code }) { set ->
                            FilaSetMtg(
                                set = set,
                                onClick = {
                                    setSeleccionado = set
                                    cartas = emptyList()
                                    mensaje = null
                                    cargandoCartas = true
                                    scope.launch {
                                        runCatching { onBuscar(set.code) }
                                            .onSuccess {
                                                cartas = it
                                                mensaje = if (it.isEmpty()) "No encontré cartas para ${set.name}" else "${it.size} cartas encontradas"
                                            }
                                            .onFailure { mensaje = "No se pudieron cargar las cartas" }
                                        cargandoCartas = false
                                    }
                                }
                            )
                        }
                        item { Spacer(Modifier.height(12.dp)) }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = {
                        setSeleccionado = null
                        cartas = emptyList()
                        mensaje = null
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a sets")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(selected.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${selected.code} • ${selected.cardCount} cartas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                mensaje?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (it.startsWith("No")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                if (cargandoCartas) {
                    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (cartas.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = onCerrar, modifier = Modifier.weight(1f)) {
                                Text("Cerrar")
                            }
                            Button(
                                enabled = !agregandoTodas,
                                onClick = {
                                    agregandoTodas = true
                                    scope.launch {
                                        val total = onAgregarTodas(cartas)
                                        mensaje = "$total cartas añadidas"
                                        agregandoTodas = false
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (agregandoTodas) "Añadiendo..." else "Añadir todas")
                            }
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(cartas, key = { "${it.setCode}-${it.collectorNumber}-${it.nombre}" }) { carta ->
                            FilaCartaSetMtg(
                                carta = carta,
                                onAgregar = {
                                    scope.launch {
                                        val added = onAgregar(carta)
                                        mensaje = if (added) "${carta.nombre} añadida" else "${carta.nombre} ya estaba en la colección"
                                    }
                                }
                            )
                        }
                        item { Spacer(Modifier.height(12.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaSetMtg(
    set: ScryfallSet,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (set.iconSvgUri.isNullOrBlank()) {
                    Text(set.code.take(3), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                } else {
                    SubcomposeAsyncImage(
                        model = set.iconSvgUri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(30.dp),
                        loading = {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                        },
                        error = {
                            Text(set.code.take(3), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        },
                        success = { SubcomposeAsyncImageContent() }
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(set.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(
                    buildList {
                        add(set.code)
                        add("${set.cardCount} cartas")
                        set.releasedAt?.let { add(it) }
                        if (set.setType.isNotBlank()) add(set.setType.replace('_', ' '))
                    }.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun FilaCartaSetMtg(
    carta: ScryfallSetCard,
    onAgregar: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (carta.imagen.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(width = 58.dp, height = 82.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("#${carta.collectorNumber}", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                SubcomposeAsyncImage(
                    model = carta.imagen,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 58.dp, height = 82.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    loading = { ImagenSkeleton(Modifier.fillMaxSize()) },
                    success = { SubcomposeAsyncImageContent() }
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(carta.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(
                    listOfNotNull(
                        carta.setCode.takeIf { it.isNotBlank() },
                        carta.collectorNumber.takeIf { it.isNotBlank() }?.let { "#$it" },
                        carta.rareza,
                        carta.idioma
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                carta.precio?.let {
                    Text(Formato.importe(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Button(onClick = onAgregar) {
                Text("Añadir")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuscadorFunkoSheet(
    apifyToken: String,
    onCerrar: () -> Unit,
    onBuscar: suspend (String, String) -> List<FunkoProduct>,
    onAgregar: suspend (FunkoProduct) -> Boolean
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var resultados by remember { mutableStateOf<List<FunkoProduct>>(emptyList()) }
    var buscando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Buscar Funko Pop", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (apifyToken.isBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        "Añade tu token de Apify en Configuración para poder buscar Funkos.",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Nombre, licencia o serie") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    enabled = query.isNotBlank() && apifyToken.isNotBlank() && !buscando,
                    onClick = {
                        buscando = true
                        mensaje = null
                        resultados = emptyList()
                        scope.launch {
                            runCatching { onBuscar(query, apifyToken) }
                                .onSuccess {
                                    resultados = it
                                    mensaje = if (it.isEmpty()) "No encontré Funkos" else "${it.size} Funkos encontrados"
                                }
                                .onFailure {
                                    mensaje = "No se pudo buscar en Apify"
                                }
                            buscando = false
                        }
                    }
                ) {
                    if (buscando) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    }
                }
            }
            mensaje?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it.startsWith("No")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            LazyColumn(
                modifier = Modifier.heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(resultados, key = { "${it.productId}-${it.nombre}" }) { funko ->
                    FilaFunko(
                        funko = funko,
                        onAgregar = {
                            scope.launch {
                                val added = onAgregar(funko)
                                mensaje = if (added) "${funko.nombre} añadido" else "${funko.nombre} ya estaba en la colección"
                            }
                        }
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun FilaFunko(
    funko: FunkoProduct,
    onAgregar: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (funko.image.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(width = 70.dp, height = 82.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(funko.boxNumber?.let { "#$it" } ?: "POP", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                SubcomposeAsyncImage(
                    model = funko.image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 70.dp, height = 82.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    loading = { ImagenSkeleton(Modifier.fillMaxSize()) },
                    success = { SubcomposeAsyncImageContent() }
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(funko.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(
                    listOfNotNull(
                        funko.boxNumber?.let { "#$it" },
                        funko.license,
                        funko.category,
                        "Chase".takeIf { funko.chanceOfChase }
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                Text(
                    listOfNotNull(funko.salePrice, funko.originalPrice, funko.availability).joinToString(" • "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
            Button(onClick = onAgregar) {
                Text("Añadir")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoItem(
    inicial: ItemColeccion?,
    tipo: TipoColeccion,
    onCerrar: () -> Unit,
    onGuardar: (
        String, EstadoItem, Double?, Double?, Int, CondicionItem, String?, String?, String?,
        String?, PrioridadWishlist, String?, String?, String?, String?, String?, LocalDate?
    ) -> Unit
) {
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var estado by remember { mutableStateOf(inicial?.estado ?: EstadoItem.QUIERO) }
    var precio by remember { mutableStateOf(inicial?.precio?.toString() ?: "") }
    var precioPagado by remember { mutableStateOf(inicial?.precioPagado?.toString() ?: "") }
    var cantidad by remember { mutableStateOf((inicial?.cantidad ?: 1).toString()) }
    var fechaAdquisicion by remember {
        mutableStateOf(
            inicial?.fechaAdquisicion ?: if ((inicial?.estado ?: EstadoItem.QUIERO) == EstadoItem.TENGO) LocalDate.now() else null
        )
    }
    var condicion by remember { mutableStateOf(inicial?.condicion ?: CondicionItem.NUEVO) }
    var prioridad by remember { mutableStateOf(inicial?.prioridad ?: PrioridadWishlist.MEDIA) }
    var rareza by remember { mutableStateOf(inicial?.rareza ?: "") }
    var notas by remember { mutableStateOf(inicial?.notas ?: "") }
    var url by remember { mutableStateOf(inicial?.urlReferencia ?: "") }
    var rutaImagen by remember { mutableStateOf(inicial?.rutaImagen ?: "") }
    var autor by remember { mutableStateOf(inicial?.autor ?: "") }
    var plataforma by remember { mutableStateOf(inicial?.plataforma ?: "") }
    var setColeccion by remember { mutableStateOf(inicial?.setColeccion ?: "") }
    var idioma by remember { mutableStateOf(inicial?.idioma ?: "") }
    var codigoBarras by remember { mutableStateOf(inicial?.codigoBarras ?: "") }
    var expEstado by remember { mutableStateOf(false) }
    var expCondicion by remember { mutableStateOf(false) }
    var expPrioridad by remember { mutableStateOf(false) }
    var mostrarScanner by remember { mutableStateOf(false) }

    val imagenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) rutaImagen = uri.toString()
    }

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
                if (inicial == null) "Nuevo ítem" else "Editar ítem",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Column(
                modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre del ítem") }, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = expEstado, onExpandedChange = { expEstado = it }) {
                    OutlinedTextField(
                        value = estado.etiqueta,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Estado") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expEstado) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expEstado, onDismissRequest = { expEstado = false }) {
                        EstadoItem.entries.forEach {
                            DropdownMenuItem(
                                text = { Text(it.etiqueta) },
                                onClick = {
                                    estado = it
                                    if (it == EstadoItem.TENGO && fechaAdquisicion == null) fechaAdquisicion = LocalDate.now()
                                    expEstado = false
                                }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CampoNumero(precio, { precio = it }, "Precio €", Modifier.weight(1f))
                    CampoNumero(precioPagado, { precioPagado = it }, "Pagado €", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = cantidad,
                        onValueChange = { cantidad = it.filter(Char::isDigit) },
                        label = { Text("Cantidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    ExposedDropdownMenuBox(expanded = expPrioridad, onExpandedChange = { expPrioridad = it }, modifier = Modifier.weight(1.4f)) {
                        OutlinedTextField(
                            value = prioridad.etiqueta,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Prioridad") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expPrioridad) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expPrioridad, onDismissRequest = { expPrioridad = false }) {
                            PrioridadWishlist.entries.forEach {
                                DropdownMenuItem(text = { Text(it.etiqueta) }, onClick = { prioridad = it; expPrioridad = false })
                            }
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = expCondicion, onExpandedChange = { expCondicion = it }) {
                    OutlinedTextField(
                        value = condicion.etiqueta,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Condición") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expCondicion) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expCondicion, onDismissRequest = { expCondicion = false }) {
                        CondicionItem.entries.forEach {
                            DropdownMenuItem(text = { Text(it.etiqueta) }, onClick = { condicion = it; expCondicion = false })
                        }
                    }
                }
                if (estado == EstadoItem.TENGO) {
                    CampoFecha(
                        label = "Fecha de adquisición",
                        fecha = fechaAdquisicion,
                        onFecha = { fechaAdquisicion = it },
                        permitirVaciar = true
                    )
                }
                when (tipo) {
                    TipoColeccion.LIBROS, TipoColeccion.COMICS -> {
                        OutlinedTextField(autor, { autor = it }, label = { Text("Autor") }, modifier = Modifier.fillMaxWidth())
                    }
                    TipoColeccion.VIDEOJUEGOS -> {
                        OutlinedTextField(plataforma, { plataforma = it }, label = { Text("Plataforma") }, modifier = Modifier.fillMaxWidth())
                    }
                    TipoColeccion.CARTAS_MTG, TipoColeccion.CARTAS_OTRAS -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(setColeccion, { setColeccion = it }, label = { Text("Set") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(idioma, { idioma = it }, label = { Text("Idioma") }, modifier = Modifier.weight(1f))
                        }
                    }
                    else -> Unit
                }
                OutlinedTextField(rareza, { rareza = it }, label = { Text("Rareza / edición") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(rutaImagen, { rutaImagen = it }, label = { Text("Ruta o URL de imagen") }, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { imagenLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Elegir imagen")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(codigoBarras, { codigoBarras = it.filter(Char::isDigit) }, label = { Text("Código de barras") }, modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { mostrarScanner = true }
                    ) { Text("Escanear") }
                }
                OutlinedTextField(url, { url = it }, label = { Text("URL de referencia") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notas, { notas = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth())
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCerrar, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = nombre.isNotBlank(),
                    onClick = {
                        onGuardar(
                            nombre.trim(),
                            estado,
                            precio.replace(',', '.').toDoubleOrNull(),
                            precioPagado.replace(',', '.').toDoubleOrNull(),
                            cantidad.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                            condicion,
                            notas.ifBlank { null },
                            url.ifBlank { null },
                            rareza.ifBlank { null },
                            rutaImagen.ifBlank { null },
                            prioridad,
                            autor.ifBlank { null },
                            plataforma.ifBlank { null },
                            setColeccion.ifBlank { null },
                            idioma.ifBlank { null },
                            codigoBarras.ifBlank { null },
                            fechaAdquisicion
                        )
                    }
                ) { Text("Guardar") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (mostrarScanner) {
        BarcodeScannerDialog(
            onCodigo = {
                codigoBarras = it.filter(Char::isDigit)
                mostrarScanner = false
            },
            onCerrar = { mostrarScanner = false }
        )
    }
}

@Composable
private fun CampoNumero(
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
