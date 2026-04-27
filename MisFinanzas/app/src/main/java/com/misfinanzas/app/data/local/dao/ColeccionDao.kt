package com.misfinanzas.app.data.local.dao

import androidx.room.*
import com.misfinanzas.app.data.local.entities.ColeccionEntity
import com.misfinanzas.app.data.local.entities.ItemColeccionEntity
import com.misfinanzas.app.domain.model.EstadoItem
import com.misfinanzas.app.domain.model.TipoColeccion
import kotlinx.coroutines.flow.Flow

@Dao
interface ColeccionDao {

    // -------- Colecciones --------
    @Query("SELECT * FROM colecciones ORDER BY fechaCreacion DESC")
    fun observarTodas(): Flow<List<ColeccionEntity>>

    @Query("SELECT * FROM colecciones WHERE id = :id")
    fun observarUna(id: Long): Flow<ColeccionEntity?>

    @Query("SELECT * FROM colecciones WHERE tipo = :tipo")
    fun observarPorTipo(tipo: TipoColeccion): Flow<List<ColeccionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertarColeccion(c: ColeccionEntity): Long
    @Update suspend fun actualizarColeccion(c: ColeccionEntity)
    @Delete suspend fun eliminarColeccion(c: ColeccionEntity)

    // -------- Items --------
    @Query("SELECT * FROM items_coleccion WHERE coleccionId = :coleccionId ORDER BY esFavorito DESC, nombre ASC")
    fun observarItems(coleccionId: Long): Flow<List<ItemColeccionEntity>>

    @Query("SELECT * FROM items_coleccion WHERE coleccionId = :coleccionId AND estado = :estado ORDER BY nombre ASC")
    fun observarItemsPorEstado(coleccionId: Long, estado: EstadoItem): Flow<List<ItemColeccionEntity>>

    @Query("SELECT * FROM items_coleccion WHERE estado = :estado ORDER BY id DESC")
    fun observarTodosPorEstado(estado: EstadoItem): Flow<List<ItemColeccionEntity>>

    @Query("SELECT * FROM items_coleccion ORDER BY id ASC")
    fun observarTodosItems(): Flow<List<ItemColeccionEntity>>

    @Query("SELECT * FROM items_coleccion WHERE id = :id")
    fun observarItem(id: Long): Flow<ItemColeccionEntity?>

    @Query("""
        SELECT IFNULL(SUM(precioPagado * cantidad),0) FROM items_coleccion
        WHERE coleccionId = :coleccionId AND estado = 'TENGO' AND precioPagado IS NOT NULL
    """)
    fun observarValorInvertido(coleccionId: Long): Flow<Double>

    @Query("""
        SELECT IFNULL(SUM(precio * cantidad),0) FROM items_coleccion
        WHERE coleccionId = :coleccionId AND estado = 'TENGO' AND precio IS NOT NULL
    """)
    fun observarValorEstimado(coleccionId: Long): Flow<Double>

    @Query("""
        SELECT IFNULL(SUM(precio * cantidad),0) FROM items_coleccion
        WHERE coleccionId = :coleccionId AND estado = 'QUIERO' AND precio IS NOT NULL
    """)
    fun observarCosteWishlist(coleccionId: Long): Flow<Double>

    @Query("SELECT COUNT(*) FROM items_coleccion WHERE coleccionId = :coleccionId AND estado = :estado")
    fun observarCuenta(coleccionId: Long, estado: EstadoItem): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertarItem(i: ItemColeccionEntity): Long
    @Update suspend fun actualizarItem(i: ItemColeccionEntity)
    @Delete suspend fun eliminarItem(i: ItemColeccionEntity)
}
