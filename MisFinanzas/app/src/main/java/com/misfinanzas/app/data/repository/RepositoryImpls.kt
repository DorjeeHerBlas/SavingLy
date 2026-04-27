package com.misfinanzas.app.data.repository

import com.misfinanzas.app.data.local.dao.*
import com.misfinanzas.app.data.local.entities.*
import com.misfinanzas.app.domain.model.*
import com.misfinanzas.app.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GastoRepositoryImpl @Inject constructor(
    private val dao: GastoDao
) : GastoRepository {
    override fun observarTodos() = dao.observarTodos().map { it.map(GastoEntity::toDomain) }
    override fun observarRango(inicio: LocalDate, fin: LocalDate) =
        dao.observarRango(inicio, fin).map { it.map(GastoEntity::toDomain) }
    override fun observarPorCategoria(c: CategoriaGasto) =
        dao.observarPorCategoria(c).map { it.map(GastoEntity::toDomain) }
    override fun observarTotalRango(inicio: LocalDate, fin: LocalDate) = dao.observarTotalRango(inicio, fin)
    override fun observarTotalCategoriaRango(inicio: LocalDate, fin: LocalDate, categoria: CategoriaGasto) =
        dao.observarTotalCategoriaRango(inicio, fin, categoria)
    override fun observarTotalesPorCategoria(inicio: LocalDate, fin: LocalDate) =
        dao.observarTotalesPorCategoria(inicio, fin)
    override fun observarTotalesPorMetodo(inicio: LocalDate, fin: LocalDate) =
        dao.observarTotalesPorMetodo(inicio, fin)
    override suspend fun guardar(g: Gasto) = dao.insertar(GastoEntity.fromDomain(g))
    override suspend fun eliminar(g: Gasto) = dao.eliminar(GastoEntity.fromDomain(g))
}

@Singleton
class IngresoRepositoryImpl @Inject constructor(
    private val dao: IngresoDao
) : IngresoRepository {
    override fun observarTodos() = dao.observarTodos().map { it.map(IngresoEntity::toDomain) }
    override fun observarRango(inicio: LocalDate, fin: LocalDate) =
        dao.observarRango(inicio, fin).map { it.map(IngresoEntity::toDomain) }
    override fun observarTotalRango(inicio: LocalDate, fin: LocalDate) = dao.observarTotalRango(inicio, fin)
    override suspend fun guardar(i: Ingreso) = dao.insertar(IngresoEntity.fromDomain(i))
    override suspend fun eliminar(i: Ingreso) = dao.eliminar(IngresoEntity.fromDomain(i))
}

@Singleton
class AhorroRepositoryImpl @Inject constructor(
    private val dao: MetaAhorroDao
) : AhorroRepository {
    override fun observarMetas() = dao.observarTodas().map { it.map(MetaAhorroEntity::toDomain) }
    override fun observarMeta(id: Long): Flow<MetaAhorro?> =
        dao.observarUna(id).map { it?.toDomain() }
    override fun observarAportaciones(metaId: Long) =
        dao.observarAportaciones(metaId).map { it.map(AportacionEntity::toDomain) }
    override fun observarTodasAportaciones() =
        dao.observarTodasAportaciones().map { it.map(AportacionEntity::toDomain) }
    override suspend fun guardarMeta(m: MetaAhorro) = dao.insertarMeta(MetaAhorroEntity.fromDomain(m))
    override suspend fun eliminarMeta(m: MetaAhorro) = dao.eliminarMeta(MetaAhorroEntity.fromDomain(m))

    override suspend fun añadirAportacion(a: Aportacion): Long {
        val id = dao.insertarAportacion(AportacionEntity.fromDomain(a))
        dao.ajustarImporte(a.metaId, a.importe)
        return id
    }

    override suspend fun eliminarAportacion(a: Aportacion) {
        dao.eliminarAportacion(AportacionEntity.fromDomain(a))
        dao.ajustarImporte(a.metaId, -a.importe)
    }
}

@Singleton
class ColeccionRepositoryImpl @Inject constructor(
    private val dao: ColeccionDao
) : ColeccionRepository {
    override fun observarColecciones() = dao.observarTodas().map { it.map(ColeccionEntity::toDomain) }
    override fun observarColeccion(id: Long): Flow<Coleccion?> =
        dao.observarUna(id).map { it?.toDomain() }
    override fun observarItems(coleccionId: Long) =
        dao.observarItems(coleccionId).map { it.map(ItemColeccionEntity::toDomain) }
    override fun observarTodosLosItems() =
        dao.observarTodosItems().map { it.map(ItemColeccionEntity::toDomain) }
    override fun observarItemsPorEstado(coleccionId: Long, estado: EstadoItem) =
        dao.observarItemsPorEstado(coleccionId, estado).map { it.map(ItemColeccionEntity::toDomain) }
    override fun observarTodosLosItemsPorEstado(estado: EstadoItem) =
        dao.observarTodosPorEstado(estado).map { it.map(ItemColeccionEntity::toDomain) }
    override fun observarItem(id: Long): Flow<ItemColeccion?> = dao.observarItem(id).map { it?.toDomain() }
    override fun observarValorInvertido(coleccionId: Long) = dao.observarValorInvertido(coleccionId)
    override fun observarValorEstimado(coleccionId: Long) = dao.observarValorEstimado(coleccionId)
    override fun observarCosteWishlist(coleccionId: Long) = dao.observarCosteWishlist(coleccionId)
    override fun observarCuenta(coleccionId: Long, estado: EstadoItem) = dao.observarCuenta(coleccionId, estado)

    override suspend fun guardarColeccion(c: Coleccion) = dao.insertarColeccion(ColeccionEntity.fromDomain(c))
    override suspend fun eliminarColeccion(c: Coleccion) = dao.eliminarColeccion(ColeccionEntity.fromDomain(c))
    override suspend fun guardarItem(i: ItemColeccion) = dao.insertarItem(ItemColeccionEntity.fromDomain(i))
    override suspend fun eliminarItem(i: ItemColeccion) = dao.eliminarItem(ItemColeccionEntity.fromDomain(i))
}

@Singleton
class PresupuestoRepositoryImpl @Inject constructor(
    private val dao: PresupuestoDao
) : PresupuestoRepository {
    override fun observarMes(mes: YearMonth) =
        dao.observarMes(mes).map { it.map(PresupuestoMensualEntity::toDomain) }
    override suspend fun guardar(p: PresupuestoMensual) = dao.insertar(PresupuestoMensualEntity.fromDomain(p))
    override suspend fun eliminar(p: PresupuestoMensual) = dao.eliminar(PresupuestoMensualEntity.fromDomain(p))
}

@Singleton
class GastoRecurrenteRepositoryImpl @Inject constructor(
    private val dao: GastoRecurrenteDao,
    private val gastoDao: GastoDao
) : GastoRecurrenteRepository {
    override fun observarTodos() = dao.observarTodos().map { it.map(GastoRecurrenteEntity::toDomain) }
    override suspend fun guardar(g: GastoRecurrente) = dao.insertar(GastoRecurrenteEntity.fromDomain(g))
    override suspend fun eliminar(g: GastoRecurrente) = dao.eliminar(GastoRecurrenteEntity.fromDomain(g))

    override suspend fun aplicarPendientes(mes: YearMonth): Int {
        val hoy = LocalDate.now()
        val esMesActual = YearMonth.from(hoy) == mes
        var generados = 0
        dao.obtenerActivos().forEach { recurrente ->
            if (recurrente.ultimoGenerado == mes) return@forEach
            if (esMesActual && recurrente.diaMes > hoy.dayOfMonth) return@forEach
            val dia = recurrente.diaMes.coerceIn(1, mes.lengthOfMonth())
            gastoDao.insertar(
                GastoEntity(
                    concepto = recurrente.concepto,
                    importe = recurrente.importe,
                    fecha = mes.atDay(dia),
                    categoria = recurrente.categoria,
                    metodoPago = recurrente.metodoPago,
                    nota = recurrente.nota ?: "Gasto recurrente"
                )
            )
            dao.actualizar(recurrente.copy(ultimoGenerado = mes))
            generados++
        }
        return generados
    }
}

@Singleton
class RecordatorioRepositoryImpl @Inject constructor(
    private val dao: RecordatorioDao
) : RecordatorioRepository {
    override fun observarTodos() = dao.observarTodos().map { it.map(RecordatorioEntity::toDomain) }
    override suspend fun obtenerActivos() = dao.obtenerActivos().map(RecordatorioEntity::toDomain)
    override suspend fun guardar(r: Recordatorio) = dao.insertar(RecordatorioEntity.fromDomain(r))
    override suspend fun eliminar(r: Recordatorio) = dao.eliminar(RecordatorioEntity.fromDomain(r))
}

@Singleton
class ListaCompraRepositoryImpl @Inject constructor(
    private val dao: ItemCompraDao
) : ListaCompraRepository {
    override fun observarTodos() = dao.observarTodos().map { it.map(ItemCompraEntity::toDomain) }
    override suspend fun guardar(i: ItemCompra) = dao.insertar(ItemCompraEntity.fromDomain(i))
    override suspend fun eliminar(i: ItemCompra) = dao.eliminar(ItemCompraEntity.fromDomain(i))
    override suspend fun eliminarComprados() = dao.eliminarComprados()
    override suspend fun marcarComprado(id: Long, comprado: Boolean) = dao.marcarComprado(id, comprado)
}
