package com.misfinanzas.app.ui.screens.compra

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misfinanzas.app.domain.model.CategoriaProducto
import com.misfinanzas.app.domain.model.ItemCompra
import com.misfinanzas.app.domain.repository.ListaCompraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListaCompraState(
    val items: List<ItemCompra> = emptyList(),
    val mostrarDialogo: Boolean = false,
    val itemEditando: ItemCompra? = null,
) {
    val pendientes: List<ItemCompra> get() = items.filterNot { it.comprado }
    val comprados: List<ItemCompra> get() = items.filter { it.comprado }
    val totalEstimado: Double
        get() = pendientes.sumOf { (it.precioEstimado ?: 0.0) * it.cantidad }
    val porCategoria: Map<CategoriaProducto, List<ItemCompra>>
        get() = pendientes.groupBy { it.categoria }
}

@HiltViewModel
class ListaCompraViewModel @Inject constructor(
    private val repo: ListaCompraRepository,
) : ViewModel() {

    private val dialogo = MutableStateFlow(false)
    private val editando = MutableStateFlow<ItemCompra?>(null)

    val estado: StateFlow<ListaCompraState> = combine(
        repo.observarTodos(),
        dialogo,
        editando,
    ) { items, d, e ->
        ListaCompraState(items = items, mostrarDialogo = d, itemEditando = e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListaCompraState())

    fun abrirNuevo() { editando.value = null; dialogo.value = true }
    fun abrirEditar(i: ItemCompra) { editando.value = i; dialogo.value = true }
    fun cerrarDialogo() { dialogo.value = false; editando.value = null }

    fun guardar(
        nombre: String,
        cantidad: Double,
        unidad: String?,
        categoria: CategoriaProducto,
        precioEstimado: Double?,
        urgente: Boolean,
        notas: String?,
    ) {
        viewModelScope.launch {
            val edit = editando.value
            val item = edit?.copy(
                nombre = nombre,
                cantidad = cantidad,
                unidad = unidad,
                categoria = categoria,
                precioEstimado = precioEstimado,
                urgente = urgente,
                notas = notas,
            ) ?: ItemCompra(
                nombre = nombre,
                cantidad = cantidad,
                unidad = unidad,
                categoria = categoria,
                precioEstimado = precioEstimado,
                urgente = urgente,
                notas = notas,
            )
            repo.guardar(item)
            cerrarDialogo()
        }
    }

    fun toggleComprado(i: ItemCompra) {
        viewModelScope.launch { repo.marcarComprado(i.id, !i.comprado) }
    }

    fun eliminar(i: ItemCompra) {
        viewModelScope.launch { repo.eliminar(i) }
    }

    fun limpiarComprados() {
        viewModelScope.launch { repo.eliminarComprados() }
    }
}
