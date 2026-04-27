package com.misfinanzas.app.ui.screens.colecciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misfinanzas.app.data.preferences.ApiPreferences
import com.misfinanzas.app.data.remote.ApifyFunkoRepository
import com.misfinanzas.app.data.remote.FunkoProduct
import com.misfinanzas.app.data.remote.ScryfallSet
import com.misfinanzas.app.data.remote.ScryfallSetCard
import com.misfinanzas.app.data.remote.ScryfallSetRepository
import com.misfinanzas.app.domain.model.*
import com.misfinanzas.app.domain.repository.ColeccionRepository
import com.misfinanzas.app.domain.repository.GastoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ColeccionesState(
    val colecciones: List<Coleccion> = emptyList(),
    val mostrarDialogo: Boolean = false,
    val coleccionEditando: Coleccion? = null
)

@HiltViewModel
class ColeccionesViewModel @Inject constructor(
    private val repo: ColeccionRepository
) : ViewModel() {

    private val dialogo = MutableStateFlow(false)
    private val editando = MutableStateFlow<Coleccion?>(null)

    val estado: StateFlow<ColeccionesState> = combine(
        repo.observarColecciones(), dialogo, editando
    ) { cols, d, e -> ColeccionesState(cols, d, e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ColeccionesState())

    fun abrirNuevo() { editando.value = null; dialogo.value = true }
    fun abrirEditar(c: Coleccion) { editando.value = c; dialogo.value = true }
    fun cerrarDialogo() { dialogo.value = false; editando.value = null }

    fun guardar(nombre: String, tipo: TipoColeccion, descripcion: String?, icono: String) {
        viewModelScope.launch {
            val edit = editando.value
            val c = edit?.copy(nombre = nombre, tipo = tipo, descripcion = descripcion, icono = icono)
                ?: Coleccion(nombre = nombre, tipo = tipo, descripcion = descripcion, icono = icono)
            repo.guardarColeccion(c)
            cerrarDialogo()
        }
    }

    fun eliminar(c: Coleccion) { viewModelScope.launch { repo.eliminarColeccion(c) } }
}

data class DetalleColeccionState(
    val coleccion: Coleccion? = null,
    val items: List<ItemColeccion> = emptyList(),
    val filtro: EstadoItem? = null,
    val totalTengo: Int = 0,
    val totalQuiero: Int = 0,
    val valorInvertido: Double = 0.0,
    val valorEstimado: Double = 0.0,
    val costeWishlist: Double = 0.0,
    val itemSugerido: ItemColeccion? = null,
    val vistaGrid: Boolean = true,
    val mostrarDialogoItem: Boolean = false,
    val itemEditando: ItemColeccion? = null
) {
    val diferenciaValor: Double get() = valorEstimado - valorInvertido
}

@HiltViewModel
class DetalleColeccionViewModel @Inject constructor(
    private val repo: ColeccionRepository,
    private val gastosRepo: GastoRepository,
    private val scryfallRepo: ScryfallSetRepository,
    private val funkoRepo: ApifyFunkoRepository,
    apiPreferences: ApiPreferences
) : ViewModel() {

    private val coleccionId = MutableStateFlow(0L)
    private val filtroEstado = MutableStateFlow<EstadoItem?>(null)
    private val dialogoItem = MutableStateFlow(false)
    private val itemEditando = MutableStateFlow<ItemColeccion?>(null)
    private val vistaGrid = MutableStateFlow(true)
    val apifyToken: StateFlow<String> = apiPreferences.apifyToken
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val estado: StateFlow<DetalleColeccionState> = coleccionId.flatMapLatest { id ->
        if (id == 0L) flowOf(DetalleColeccionState()) else combine(
            repo.observarColeccion(id),
            repo.observarItems(id),
            filtroEstado,
            repo.observarCuenta(id, EstadoItem.TENGO),
            repo.observarCuenta(id, EstadoItem.QUIERO),
            repo.observarValorInvertido(id),
            repo.observarValorEstimado(id),
            repo.observarCosteWishlist(id),
            vistaGrid,
            dialogoItem,
            itemEditando
        ) { values ->
            val col = values[0] as Coleccion?
            @Suppress("UNCHECKED_CAST") val items = values[1] as List<ItemColeccion>
            val f = values[2] as EstadoItem?
            DetalleColeccionState(
                coleccion = col,
                items = if (f == null) items else items.filter { it.estado == f },
                filtro = f,
                totalTengo = values[3] as Int,
                totalQuiero = values[4] as Int,
                valorInvertido = values[5] as Double,
                valorEstimado = values[6] as Double,
                costeWishlist = values[7] as Double,
                itemSugerido = items
                    .filter { it.estado == EstadoItem.QUIERO || it.estado == EstadoItem.RESERVADO }
                    .sortedWith(compareBy<ItemColeccion> { it.prioridad.peso }.thenBy { it.precio ?: Double.MAX_VALUE })
                    .firstOrNull(),
                vistaGrid = values[8] as Boolean,
                mostrarDialogoItem = values[9] as Boolean,
                itemEditando = values[10] as ItemColeccion?
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleColeccionState())

    fun cargar(id: Long) { coleccionId.value = id }
    fun setFiltro(e: EstadoItem?) { filtroEstado.value = e }
    fun toggleVista() { vistaGrid.value = !vistaGrid.value }
    fun abrirNuevoItem() { itemEditando.value = null; dialogoItem.value = true }
    fun abrirEditarItem(i: ItemColeccion) { itemEditando.value = i; dialogoItem.value = true }
    fun cerrarDialogoItem() { dialogoItem.value = false; itemEditando.value = null }

    suspend fun buscarCartasMtgPorSet(setCode: String): List<ScryfallSetCard> =
        scryfallRepo.buscarCartasPorSet(setCode)

    suspend fun buscarSetsMtg(): List<ScryfallSet> =
        scryfallRepo.buscarSets()

    suspend fun buscarFunkos(query: String, token: String): List<FunkoProduct> =
        funkoRepo.buscar(token, query)

    suspend fun agregarCartaMtg(carta: ScryfallSetCard): Boolean {
        val cId = coleccionId.value
        if (cId == 0L) return false
        val existentes = repo.observarItems(cId).first()
        val yaExiste = existentes.any {
            it.setColeccion.equals(carta.setCode, ignoreCase = true) &&
                it.rareza?.contains("#${carta.collectorNumber}") == true
        }
        if (yaExiste) return false
        repo.guardarItem(carta.toItemColeccion(cId))
        return true
    }

    suspend fun agregarCartasMtg(cartas: List<ScryfallSetCard>): Int {
        var agregadas = 0
        cartas.forEach { if (agregarCartaMtg(it)) agregadas++ }
        return agregadas
    }

    suspend fun agregarFunko(funko: FunkoProduct): Boolean {
        val cId = coleccionId.value
        if (cId == 0L) return false
        val existentes = repo.observarItems(cId).first()
        val yaExiste = existentes.any {
            (!it.codigoBarras.isNullOrBlank() && it.codigoBarras == funko.productId) ||
                (it.nombre.equals(funko.nombre, ignoreCase = true) && it.setColeccion == funko.license)
        }
        if (yaExiste) return false
        repo.guardarItem(funko.toItemColeccion(cId))
        return true
    }

    fun guardarItem(
        nombre: String, estado: EstadoItem, precio: Double?, precioPagado: Double?,
        cantidad: Int, condicion: CondicionItem, notas: String?, urlReferencia: String?,
        rareza: String?, rutaImagen: String?, prioridad: PrioridadWishlist,
        autor: String?, plataforma: String?, setColeccion: String?, idioma: String?,
        codigoBarras: String?, fechaAdquisicion: LocalDate?
    ) {
        val cId = coleccionId.value
        if (cId == 0L) return
        viewModelScope.launch {
            val edit = itemEditando.value
            val item = edit?.copy(
                nombre = nombre, estado = estado, precio = precio, precioPagado = precioPagado,
                cantidad = cantidad, condicion = condicion, notas = notas,
                urlReferencia = urlReferencia, rareza = rareza, rutaImagen = rutaImagen,
                prioridad = prioridad, autor = autor, plataforma = plataforma,
                setColeccion = setColeccion, idioma = idioma, codigoBarras = codigoBarras,
                fechaAdquisicion = if (estado == EstadoItem.TENGO) {
                    fechaAdquisicion ?: edit.fechaAdquisicion ?: LocalDate.now()
                } else {
                    edit.fechaAdquisicion
                }
            ) ?: ItemColeccion(
                coleccionId = cId, nombre = nombre, estado = estado, precio = precio,
                precioPagado = precioPagado, cantidad = cantidad, condicion = condicion,
                notas = notas, urlReferencia = urlReferencia, rareza = rareza,
                rutaImagen = rutaImagen, prioridad = prioridad, autor = autor,
                plataforma = plataforma, setColeccion = setColeccion, idioma = idioma,
                codigoBarras = codigoBarras,
                fechaAdquisicion = if (estado == EstadoItem.TENGO) fechaAdquisicion ?: LocalDate.now() else null
            )
            repo.guardarItem(item)
            if (estado == EstadoItem.TENGO && edit?.estado != EstadoItem.TENGO && precioPagado != null && precioPagado > 0.0) {
                gastosRepo.guardar(
                    Gasto(
                        concepto = "Colección: $nombre",
                        importe = precioPagado * cantidad,
                        fecha = item.fechaAdquisicion ?: LocalDate.now(),
                        categoria = CategoriaGasto.COLECCIONES,
                        metodoPago = MetodoPago.TARJETA,
                        nota = "Compra añadida desde colección"
                    )
                )
            }
            cerrarDialogoItem()
        }
    }

    fun eliminarItem(i: ItemColeccion) { viewModelScope.launch { repo.eliminarItem(i) } }
    fun restaurarItem(i: ItemColeccion) { viewModelScope.launch { repo.guardarItem(i) } }

    fun cambiarEstadoRapido(i: ItemColeccion, nuevoEstado: EstadoItem) {
        viewModelScope.launch {
            val fecha = if (nuevoEstado == EstadoItem.TENGO && i.fechaAdquisicion == null)
                LocalDate.now() else i.fechaAdquisicion
            repo.guardarItem(
                i.copy(
                    estado = nuevoEstado,
                    fechaAdquisicion = fecha
                )
            )
            if (nuevoEstado == EstadoItem.TENGO && i.estado != EstadoItem.TENGO && i.precioPagado != null && i.precioPagado > 0.0) {
                gastosRepo.guardar(
                    Gasto(
                        concepto = "Colección: ${i.nombre}",
                        importe = i.precioPagado * i.cantidad,
                        fecha = fecha ?: LocalDate.now(),
                        categoria = CategoriaGasto.COLECCIONES,
                        metodoPago = MetodoPago.TARJETA,
                        nota = "Compra añadida desde colección"
                    )
                )
            }
        }
    }

    fun toggleFavorito(i: ItemColeccion) {
        viewModelScope.launch { repo.guardarItem(i.copy(esFavorito = !i.esFavorito)) }
    }
}

private fun ScryfallSetCard.toItemColeccion(coleccionId: Long): ItemColeccion =
    ItemColeccion(
        coleccionId = coleccionId,
        nombre = nombre,
        estado = EstadoItem.QUIERO,
        precio = precio,
        cantidad = 1,
        condicion = CondicionItem.NUEVO,
        rutaImagen = imagen,
        urlReferencia = url,
        rareza = listOfNotNull(rareza, collectorNumber.takeIf { it.isNotBlank() }?.let { "#$it" })
            .joinToString(" • ")
            .ifBlank { null },
        prioridad = PrioridadWishlist.MEDIA,
        setColeccion = setCode.ifBlank { setName },
        idioma = idioma
    )

private fun FunkoProduct.toItemColeccion(coleccionId: Long): ItemColeccion =
    ItemColeccion(
        coleccionId = coleccionId,
        nombre = nombre,
        estado = EstadoItem.QUIERO,
        precio = precioEstimado,
        cantidad = 1,
        condicion = CondicionItem.NUEVO,
        rutaImagen = image,
        urlReferencia = url,
        rareza = listOfNotNull(
            boxNumber?.let { "#$it" },
            category,
            availability,
            "Chase".takeIf { chanceOfChase }
        ).joinToString(" • ").ifBlank { null },
        prioridad = PrioridadWishlist.MEDIA,
        setColeccion = license,
        codigoBarras = productId,
        notas = description?.take(450)
    )

data class DetalleItemState(val item: ItemColeccion? = null)

@HiltViewModel
class DetalleItemViewModel @Inject constructor(
    private val repo: ColeccionRepository
) : ViewModel() {
    private val itemId = MutableStateFlow(0L)
    val estado: StateFlow<DetalleItemState> = itemId.flatMapLatest {
        if (it == 0L) flowOf(DetalleItemState()) else repo.observarItem(it).map { i -> DetalleItemState(i) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleItemState())

    fun cargar(id: Long) { itemId.value = id }
}
