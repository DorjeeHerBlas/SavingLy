package com.misfinanzas.app.data.local.dao

import androidx.room.*
import com.misfinanzas.app.data.local.entities.GastoEntity
import com.misfinanzas.app.data.local.entities.IngresoEntity
import com.misfinanzas.app.domain.model.CategoriaGasto
import com.misfinanzas.app.domain.model.MetodoPago
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface GastoDao {
    @Query("SELECT * FROM gastos ORDER BY fecha DESC, id DESC")
    fun observarTodos(): Flow<List<GastoEntity>>

    @Query("SELECT * FROM gastos WHERE fecha BETWEEN :inicio AND :fin ORDER BY fecha DESC")
    fun observarRango(inicio: LocalDate, fin: LocalDate): Flow<List<GastoEntity>>

    @Query("SELECT * FROM gastos WHERE categoria = :categoria ORDER BY fecha DESC")
    fun observarPorCategoria(categoria: CategoriaGasto): Flow<List<GastoEntity>>

    @Query("SELECT IFNULL(SUM(importe),0) FROM gastos WHERE fecha BETWEEN :inicio AND :fin")
    fun observarTotalRango(inicio: LocalDate, fin: LocalDate): Flow<Double>

    @Query("SELECT IFNULL(SUM(importe),0) FROM gastos WHERE fecha BETWEEN :inicio AND :fin AND categoria = :categoria")
    fun observarTotalCategoriaRango(inicio: LocalDate, fin: LocalDate, categoria: CategoriaGasto): Flow<Double>

    @Query("""
        SELECT categoria AS categoria, IFNULL(SUM(importe),0) AS total
        FROM gastos WHERE fecha BETWEEN :inicio AND :fin
        GROUP BY categoria ORDER BY total DESC
    """)
    fun observarTotalesPorCategoria(inicio: LocalDate, fin: LocalDate): Flow<List<TotalCategoria>>

    @Query("""
        SELECT metodoPago AS metodoPago, IFNULL(SUM(importe),0) AS total
        FROM gastos WHERE fecha BETWEEN :inicio AND :fin
        GROUP BY metodoPago ORDER BY total DESC
    """)
    fun observarTotalesPorMetodo(inicio: LocalDate, fin: LocalDate): Flow<List<TotalMetodoPago>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertar(g: GastoEntity): Long
    @Update suspend fun actualizar(g: GastoEntity)
    @Delete suspend fun eliminar(g: GastoEntity)
}

data class TotalCategoria(val categoria: CategoriaGasto, val total: Double)
data class TotalMetodoPago(val metodoPago: MetodoPago, val total: Double)

@Dao
interface IngresoDao {
    @Query("SELECT * FROM ingresos ORDER BY fecha DESC, id DESC")
    fun observarTodos(): Flow<List<IngresoEntity>>

    @Query("SELECT * FROM ingresos WHERE fecha BETWEEN :inicio AND :fin ORDER BY fecha DESC, id DESC")
    fun observarRango(inicio: LocalDate, fin: LocalDate): Flow<List<IngresoEntity>>

    @Query("SELECT IFNULL(SUM(importe),0) FROM ingresos WHERE fecha BETWEEN :inicio AND :fin")
    fun observarTotalRango(inicio: LocalDate, fin: LocalDate): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertar(i: IngresoEntity): Long
    @Delete suspend fun eliminar(i: IngresoEntity)
}
