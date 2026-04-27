package com.misfinanzas.app.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.misfinanzas.app.data.local.converters.Converters
import com.misfinanzas.app.data.local.dao.*
import com.misfinanzas.app.data.local.entities.*

@Database(
    entities = [
        GastoEntity::class,
        IngresoEntity::class,
        MetaAhorroEntity::class,
        AportacionEntity::class,
        ColeccionEntity::class,
        ItemColeccionEntity::class,
        PresupuestoMensualEntity::class,
        GastoRecurrenteEntity::class,
        RecordatorioEntity::class,
        ItemCompraEntity::class,
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MisFinanzasDatabase : RoomDatabase() {
    abstract fun gastoDao(): GastoDao
    abstract fun ingresoDao(): IngresoDao
    abstract fun metaAhorroDao(): MetaAhorroDao
    abstract fun coleccionDao(): ColeccionDao
    abstract fun presupuestoDao(): PresupuestoDao
    abstract fun gastoRecurrenteDao(): GastoRecurrenteDao
    abstract fun recordatorioDao(): RecordatorioDao
    abstract fun itemCompraDao(): ItemCompraDao

    companion object {
        const val NOMBRE = "misfinanzas.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ingresos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `concepto` TEXT NOT NULL,
                        `importe` REAL NOT NULL,
                        `fecha` TEXT NOT NULL,
                        `fuente` TEXT NOT NULL,
                        `nota` TEXT
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `presupuestos_mensuales` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `mes` TEXT NOT NULL,
                        `categoria` TEXT NOT NULL,
                        `limite` REAL NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_presupuestos_mensuales_mes_categoria`
                    ON `presupuestos_mensuales` (`mes`, `categoria`)
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `gastos_recurrentes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `concepto` TEXT NOT NULL,
                        `importe` REAL NOT NULL,
                        `categoria` TEXT NOT NULL,
                        `metodoPago` TEXT NOT NULL,
                        `diaMes` INTEGER NOT NULL,
                        `activo` INTEGER NOT NULL,
                        `ultimoGenerado` TEXT,
                        `nota` TEXT
                    )
                """.trimIndent())
                db.execSQL("ALTER TABLE `items_coleccion` ADD COLUMN `prioridad` TEXT NOT NULL DEFAULT 'MEDIA'")
                db.execSQL("ALTER TABLE `items_coleccion` ADD COLUMN `autor` TEXT")
                db.execSQL("ALTER TABLE `items_coleccion` ADD COLUMN `plataforma` TEXT")
                db.execSQL("ALTER TABLE `items_coleccion` ADD COLUMN `setColeccion` TEXT")
                db.execSQL("ALTER TABLE `items_coleccion` ADD COLUMN `idioma` TEXT")
                db.execSQL("ALTER TABLE `items_coleccion` ADD COLUMN `codigoBarras` TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `items_compra` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nombre` TEXT NOT NULL,
                        `cantidad` REAL NOT NULL,
                        `unidad` TEXT,
                        `categoria` TEXT NOT NULL,
                        `comprado` INTEGER NOT NULL,
                        `precioEstimado` REAL,
                        `urgente` INTEGER NOT NULL,
                        `notas` TEXT,
                        `fechaAdicion` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
