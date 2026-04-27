package com.misfinanzas.app.ui.screens.configuracion

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misfinanzas.app.data.preferences.ApiPreferences
import com.misfinanzas.app.data.preferences.AppThemeMode
import com.misfinanzas.app.data.preferences.ThemePreferences
import com.misfinanzas.app.data.repository.BackupRepository
import com.misfinanzas.app.domain.model.Recordatorio
import com.misfinanzas.app.domain.model.TipoRecordatorio
import com.misfinanzas.app.domain.repository.RecordatorioRepository
import com.misfinanzas.app.notifications.NotificacionesHelper
import com.misfinanzas.app.ui.components.EstadoVacio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class ConfiguracionViewModel @Inject constructor(
    private val repo: RecordatorioRepository,
    private val backupRepository: BackupRepository,
    private val themePreferences: ThemePreferences,
    private val apiPreferences: ApiPreferences
) : ViewModel() {

    val recordatorios: StateFlow<List<Recordatorio>> = repo.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val themeMode: StateFlow<AppThemeMode> = themePreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppThemeMode.SYSTEM)
    val apifyToken: StateFlow<String> = apiPreferences.apifyToken
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje

    fun crear(context: Context, titulo: String, mensaje: String, hora: LocalTime, tipo: TipoRecordatorio) {
        viewModelScope.launch {
            val id = repo.guardar(
                Recordatorio(titulo = titulo, mensaje = mensaje, tipo = tipo, hora = hora, activo = true)
            )
            NotificacionesHelper.programar(context, id, hora)
        }
    }

    fun eliminar(context: Context, r: Recordatorio) {
        viewModelScope.launch {
            repo.eliminar(r)
            NotificacionesHelper.cancelar(context, r.id)
        }
    }

    fun toggle(context: Context, r: Recordatorio) {
        viewModelScope.launch {
            val nuevo = r.copy(activo = !r.activo)
            repo.guardar(nuevo)
            if (nuevo.activo) NotificacionesHelper.programar(context, r.id, r.hora)
            else NotificacionesHelper.cancelar(context, r.id)
        }
    }

    fun exportar(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val json = withContext(Dispatchers.IO) { backupRepository.exportarJson() }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("No se pudo abrir el archivo")
                }
            }.onSuccess {
                _mensaje.value = "Copia exportada"
            }.onFailure {
                _mensaje.value = "No se pudo exportar: ${it.message ?: "error"}"
            }
        }
    }

    fun importar(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("No se pudo abrir el archivo")
                }
                withContext(Dispatchers.IO) { backupRepository.importarJson(json) }
            }.onSuccess {
                _mensaje.value = "Copia importada"
            }.onFailure {
                _mensaje.value = "No se pudo importar: ${it.message ?: "error"}"
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }

    fun setApifyToken(token: String) {
        viewModelScope.launch {
            apiPreferences.setApifyToken(token)
            _mensaje.value = if (token.isBlank()) "Token de Apify eliminado" else "Token de Apify guardado"
        }
    }

    fun consumirMensaje() { _mensaje.value = null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(
    onVolver: () -> Unit,
    vm: ConfiguracionViewModel = hiltViewModel()
) {
    val recordatorios by vm.recordatorios.collectAsState()
    val themeMode by vm.themeMode.collectAsState()
    val apifyToken by vm.apifyToken.collectAsState()
    val mensaje by vm.mensaje.collectAsState()
    val context = LocalContext.current
    var mostrarDialogo by remember { mutableStateOf(false) }
    var uriImportacionPendiente by remember { mutableStateOf<Uri?>(null) }
    var apifyTokenEdit by remember(apifyToken) { mutableStateOf(apifyToken) }
    val snackbarHostState = remember { SnackbarHostState() }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.exportar(context, uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) uriImportacionPendiente = uri }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            vm.consumirMensaje()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogo = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo recordatorio")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Tema visual", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Elige si la app sigue al sistema, fuerza claro, oscuro o negro puro OLED.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        ) {
                            AppThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { vm.setThemeMode(mode) },
                                    label = { Text(mode.etiqueta) }
                                )
                            }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Apify Funko Pop", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Guarda tu token de Apify para buscar productos Funko desde colecciones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = apifyTokenEdit,
                            onValueChange = { apifyTokenEdit = it },
                            label = { Text("Token de Apify") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    apifyTokenEdit = ""
                                    vm.setApifyToken("")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Borrar")
                            }
                            Button(
                                onClick = { vm.setApifyToken(apifyTokenEdit) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Guardar")
                            }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Copias de seguridad", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    exportLauncher.launch("mis-finanzas-${java.time.LocalDate.now()}.json")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.FileDownload, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Exportar")
                            }
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.FileUpload, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Importar")
                            }
                        }
                    }
                }
            }
            item {
                Text("Recordatorios", style = MaterialTheme.typography.titleMedium)
            }
            if (recordatorios.isEmpty()) {
                item {
                    EstadoVacio(
                        titulo = "Sin recordatorios",
                        descripcion = "Configura recordatorios diarios para registrar gastos o revisar tus ahorros.",
                        emoji = "🔔",
                        accion = {
                            Button(onClick = { mostrarDialogo = true }) {
                                Text("Crear recordatorio")
                            }
                        }
                    )
                }
            } else {
                items(recordatorios, key = { it.id }) { r ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(r.titulo, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${r.hora} • ${r.tipo.etiqueta}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = r.activo, onCheckedChange = { vm.toggle(context, r) })
                            IconButton(onClick = { vm.eliminar(context, r) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar")
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogo) {
        DialogoRecordatorio(
            onCerrar = { mostrarDialogo = false },
            onGuardar = { titulo, mensaje, hora, tipo ->
                vm.crear(context, titulo, mensaje, hora, tipo)
                mostrarDialogo = false
            }
        )
    }

    uriImportacionPendiente?.let { uri ->
        AlertDialog(
            onDismissRequest = { uriImportacionPendiente = null },
            title = { Text("Importar copia") },
            text = {
                Text("Se importarán gastos, ingresos, metas, colecciones, presupuestos, recurrentes y recordatorios del archivo seleccionado.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.importar(context, uri)
                        uriImportacionPendiente = null
                    }
                ) {
                    Text("Importar")
                }
            },
            dismissButton = {
                TextButton(onClick = { uriImportacionPendiente = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoRecordatorio(
    onCerrar: () -> Unit,
    onGuardar: (String, String, LocalTime, TipoRecordatorio) -> Unit
) {
    var titulo by remember { mutableStateOf("Registrar gasto") }
    var mensaje by remember { mutableStateOf("¿Has anotado tus gastos de hoy?") }
    var hora by remember { mutableStateOf("21:00") }
    var tipo by remember { mutableStateOf(TipoRecordatorio.REGISTRO_GASTO) }
    var exp by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Nuevo recordatorio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titulo, onValueChange = { titulo = it },
                    label = { Text("Título") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mensaje, onValueChange = { mensaje = it },
                    label = { Text("Mensaje") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hora, onValueChange = { hora = it },
                    label = { Text("Hora (HH:mm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }) {
                    OutlinedTextField(
                        value = tipo.etiqueta, onValueChange = {}, readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exp) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                        TipoRecordatorio.entries.forEach {
                            DropdownMenuItem(text = { Text(it.etiqueta) }, onClick = { tipo = it; exp = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = titulo.isNotBlank() && hora.matches(Regex("\\d{1,2}:\\d{2}")),
                onClick = {
                    val parts = hora.split(":")
                    val lt = LocalTime.of(parts[0].toInt().coerceIn(0, 23), parts[1].toInt().coerceIn(0, 59))
                    onGuardar(titulo, mensaje, lt, tipo)
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
