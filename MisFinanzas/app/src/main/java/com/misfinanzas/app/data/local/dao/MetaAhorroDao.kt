package com.misfinanzas.app.data.local.dao

import androidx.room.*
import com.misfinanzas.app.data.local.entities.AportacionEntity
import com.misfinanzas.app.data.local.entities.MetaAhorroEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetaAhorroDao {
    @Query("SELECT * FROM metas_ahorro ORDER BY id DESC")
    fun observarTodas(): Flow<List<MetaAhorroEntity>>

    @Query("SELECT * FROM metas_ahorro WHERE id = :id")
    fun observarUna(id: Long): Flow<MetaAhorroEntity?>

    @Query("SELECT * FROM aportaciones WHERE metaId = :metaId ORDER BY fecha DESC")
    fun observarAportaciones(metaId: Long): Flow<List<AportacionEntity>>

    @Query("SELECT * FROM aportaciones ORDER BY fecha DESC")
    fun observarTodasAportaciones(): Flow<List<AportacionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertarMeta(m: MetaAhorroEntity): Long
    @Update suspend fun actualizarMeta(m: MetaAhorroEntity)
    @Delete suspend fun eliminarMeta(m: MetaAhorroEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertarAportacion(a: AportacionEntity): Long
    @Delete suspend fun eliminarAportacion(a: AportacionEntity)

    @Query("UPDATE metas_ahorro SET importeActual = importeActual + :delta WHERE id = :metaId")
    suspend fun ajustarImporte(metaId: Long, delta: Double)
}
