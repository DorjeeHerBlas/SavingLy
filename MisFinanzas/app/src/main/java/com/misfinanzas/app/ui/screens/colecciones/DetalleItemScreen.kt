package com.misfinanzas.app.ui.screens.colecciones

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.misfinanzas.app.utils.Formato

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleItemScreen(
    coleccionId: Long,
    itemId: Long,
    onVolver: () -> Unit,
    vm: DetalleItemViewModel = hiltViewModel()
) {
    LaunchedEffect(itemId) { vm.cargar(itemId) }
    val state by vm.estado.collectAsState()
    val item = state.item

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.nombre ?: "Ítem") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        if (item == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!item.rutaImagen.isNullOrBlank()) {
                    AsyncImage(
                        model = item.rutaImagen,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                Text(item.nombre, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                AssistChip(onClick = {}, label = { Text(item.estado.etiqueta) })

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilaInfo("Cantidad", "${item.cantidad}")
                        item.precio?.let { FilaInfo("Precio referencia", Formato.importe(it)) }
                        item.precioPagado?.let { FilaInfo("Precio pagado", Formato.importe(it)) }
                        FilaInfo("Condición", item.condicion.etiqueta)
                        FilaInfo("Prioridad", item.prioridad.etiqueta)
                        item.rareza?.let { FilaInfo("Rareza", it) }
                        item.autor?.let { FilaInfo("Autor", it) }
                        item.plataforma?.let { FilaInfo("Plataforma", it) }
                        item.setColeccion?.let { FilaInfo("Set", it) }
                        item.idioma?.let { FilaInfo("Idioma", it) }
                        item.codigoBarras?.let { FilaInfo("Código de barras", it) }
                        item.fechaAdquisicion?.let { FilaInfo("Adquirido", Formato.fecha(it)) }
                        item.urlReferencia?.let { FilaInfo("URL", it) }
                    }
                }
                if (!item.notas.isNullOrBlank()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Notas", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(item.notas)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaInfo(label: String, valor: String) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, fontWeight = FontWeight.Medium)
    }
}
