package com.misfinanzas.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.misfinanzas.app.data.local.entities.GastoRecurrenteEntity
import com.misfinanzas.app.data.local.entities.PresupuestoMensualEntity
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

@Dao
interface PresupuestoDao {
    @Query("SELECT * FROM presupuestos_mensuales WHERE mes = :mes ORDER BY categoria ASC")
    fun observarMes(mes: YearMonth): Flow<List<PresupuestoMensualEntity>>

    @Query("SELECT * FROM presupuestos_mensuales ORDER BY mes DESC, categoria ASC")
    fun observarTodos(): Flow<List<PresupuestoMensualEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertar(p: PresupuestoMensualEntity): Long
    @Delete suspend fun eliminar(p: PresupuestoMensualEntity)
}

@Dao
interface GastoRecurrenteDao {
    @Query("SELECT * FROM gastos_recurrentes ORDER BY activo DESC, diaMes ASC, concepto ASC")
    fun observarTodos(): Flow<List<GastoRecurrenteEntity>>

    @Query("SELECT * FROM gastos_recurrentes WHERE activo = 1 ORDER BY diaMes ASC")
    suspend fun obtenerActivos(): List<GastoRecurrenteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertar(g: GastoRecurrenteEntity): Long
    @Update suspend fun actualizar(g: GastoRecurrenteEntity)
    @Delete suspend fun eliminar(g: GastoRecurrenteEntity)
}
