package com.misfinanzas.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.misfinanzas.app.data.local.entities.ItemCompraEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemCompraDao {

    @Query("SELECT * FROM items_compra ORDER BY comprado ASC, urgente DESC, categoria ASC, fechaAdicion DESC")
    fun observarTodos(): Flow<List<ItemCompraEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(item: ItemCompraEntity): Long

    @Delete
    suspend fun eliminar(item: ItemCompraEntity)

    @Query("DELETE FROM items_compra WHERE comprado = 1")
    suspend fun eliminarComprados()

    @Query("UPDATE items_compra SET comprado = :comprado WHERE id = :id")
    suspend fun marcarComprado(id: Long, comprado: Boolean)
}
