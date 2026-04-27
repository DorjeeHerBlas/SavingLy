package com.misfinanzas.app.data.local.dao

import androidx.room.*
import com.misfinanzas.app.data.local.entities.RecordatorioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordatorioDao {
    @Query("SELECT * FROM recordatorios ORDER BY hora ASC")
    fun observarTodos(): Flow<List<RecordatorioEntity>>

    @Query("SELECT * FROM recordatorios WHERE activo = 1")
    suspend fun obtenerActivos(): List<RecordatorioEntity>

    @Query("SELECT * FROM recordatorios WHERE id = :id")
    suspend fun obtenerUno(id: Long): RecordatorioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertar(r: RecordatorioEntity): Long
    @Update suspend fun actualizar(r: RecordatorioEntity)
    @Delete suspend fun eliminar(r: RecordatorioEntity)
}
