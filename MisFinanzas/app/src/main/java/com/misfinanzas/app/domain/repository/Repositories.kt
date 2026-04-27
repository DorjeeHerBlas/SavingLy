package com.misfinanzas.app.domain.repository

import com.misfinanzas.app.data.local.dao.TotalCategoria
import com.misfinanzas.app.data.local.dao.TotalMetodoPago
import com.misfinanzas.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface GastoRepository {
    fun observarTodos(): Flow<List<Gasto>>
    fun observarRango(inicio: LocalDate, fin: LocalDate): Flow<List<Gasto>>
    fun observarPorCategoria(c: CategoriaGasto): Flow<List<Gasto>>
    fun observarTotalRango(inicio: LocalDate, fin: LocalDate): Flow<Double>
    fun observarTotalCategoriaRango(inicio: LocalDate, fin: LocalDate, categoria: CategoriaGasto): Flow<Double>
    fun observarTotalesPorCategoria(inicio: LocalDate, fin: LocalDate): Flow<List<TotalCategoria>>
    fun observarTotalesPorMetodo(inicio: LocalDate, fin: LocalDate): Flow<List<TotalMetodoPago>>
    suspend fun guardar(g: Gasto): Long
    suspend fun eliminar(g: Gasto)
}

interface IngresoRepository {
    fun observarTodos(): Flow<List<Ingreso>>
    fun observarRango(inicio: LocalDate, fin: LocalDate): Flow<List<Ingreso>>
    fun observarTotalRango(inicio: LocalDate, fin: LocalDate): Flow<Double>
    suspend fun guardar(i: Ingreso): Long
    suspend fun eliminar(i: Ingreso)
}

interface AhorroRepository {
    fun observarMetas(): Flow<List<MetaAhorro>>
    fun observarMeta(id: Long): Flow<MetaAhorro?>
    fun observarAportaciones(metaId: Long): Flow<List<Aportacion>>
    fun observarTodasAportaciones(): Flow<List<Aportacion>>
    suspend fun guardarMeta(m: MetaAhorro): Long
    suspend fun eliminarMeta(m: MetaAhorro)
    suspend fun añadirAportacion(a: Aportacion): Long
    suspend fun eliminarAportacion(a: Aportacion)
}

interface ColeccionRepository {
    fun observarColecciones(): Flow<List<Coleccion>>
    fun observarColeccion(id: Long): Flow<Coleccion?>
    fun observarItems(coleccionId: Long): Flow<List<ItemColeccion>>
    fun observarTodosLosItems(): Flow<List<ItemColeccion>>
    fun observarItemsPorEstado(coleccionId: Long, estado: EstadoItem): Flow<List<ItemColeccion>>
    fun observarTodosLosItemsPorEstado(estado: EstadoItem): Flow<List<ItemColeccion>>
    fun observarItem(id: Long): Flow<ItemColeccion?>
    fun observarValorInvertido(coleccionId: Long): Flow<Double>
    fun observarValorEstimado(coleccionId: Long): Flow<Double>
    fun observarCosteWishlist(coleccionId: Long): Flow<Double>
    fun observarCuenta(coleccionId: Long, estado: EstadoItem): Flow<Int>
    suspend fun guardarColeccion(c: Coleccion): Long
    suspend fun eliminarColeccion(c: Coleccion)
    suspend fun guardarItem(i: ItemColeccion): Long
    suspend fun eliminarItem(i: ItemColeccion)
}

interface PresupuestoRepository {
    fun observarMes(mes: YearMonth): Flow<List<PresupuestoMensual>>
    suspend fun guardar(p: PresupuestoMensual): Long
    suspend fun eliminar(p: PresupuestoMensual)
}

interface GastoRecurrenteRepository {
    fun observarTodos(): Flow<List<GastoRecurrente>>
    suspend fun guardar(g: GastoRecurrente): Long
    suspend fun eliminar(g: GastoRecurrente)
    suspend fun aplicarPendientes(mes: YearMonth): Int
}

interface RecordatorioRepository {
    fun observarTodos(): Flow<List<Recordatorio>>
    suspend fun obtenerActivos(): List<Recordatorio>
    suspend fun guardar(r: Recordatorio): Long
    suspend fun eliminar(r: Recordatorio)
}

interface ListaCompraRepository {
    fun observarTodos(): Flow<List<ItemCompra>>
    suspend fun guardar(i: ItemCompra): Long
    suspend fun eliminar(i: ItemCompra)
    suspend fun eliminarComprados()
    suspend fun marcarComprado(id: Long, comprado: Boolean)
}
