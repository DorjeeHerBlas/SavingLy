package com.misfinanzas.app.di

import android.content.Context
import androidx.room.Room
import com.misfinanzas.app.data.local.MisFinanzasDatabase
import com.misfinanzas.app.data.local.dao.*
import com.misfinanzas.app.data.repository.*
import com.misfinanzas.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): MisFinanzasDatabase =
        Room.databaseBuilder(ctx, MisFinanzasDatabase::class.java, MisFinanzasDatabase.NOMBRE)
            .addMigrations(
                MisFinanzasDatabase.MIGRATION_1_2,
                MisFinanzasDatabase.MIGRATION_2_3,
            )
            .build()

    @Provides fun gastoDao(db: MisFinanzasDatabase): GastoDao = db.gastoDao()
    @Provides fun ingresoDao(db: MisFinanzasDatabase): IngresoDao = db.ingresoDao()
    @Provides fun metaDao(db: MisFinanzasDatabase): MetaAhorroDao = db.metaAhorroDao()
    @Provides fun colDao(db: MisFinanzasDatabase): ColeccionDao = db.coleccionDao()
    @Provides fun presupuestoDao(db: MisFinanzasDatabase): PresupuestoDao = db.presupuestoDao()
    @Provides fun gastoRecurrenteDao(db: MisFinanzasDatabase): GastoRecurrenteDao = db.gastoRecurrenteDao()
    @Provides fun recDao(db: MisFinanzasDatabase): RecordatorioDao = db.recordatorioDao()
    @Provides fun itemCompraDao(db: MisFinanzasDatabase): ItemCompraDao = db.itemCompraDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindGastoRepo(impl: GastoRepositoryImpl): GastoRepository
    @Binds abstract fun bindIngresoRepo(impl: IngresoRepositoryImpl): IngresoRepository
    @Binds abstract fun bindAhorroRepo(impl: AhorroRepositoryImpl): AhorroRepository
    @Binds abstract fun bindColRepo(impl: ColeccionRepositoryImpl): ColeccionRepository
    @Binds abstract fun bindPresupuestoRepo(impl: PresupuestoRepositoryImpl): PresupuestoRepository
    @Binds abstract fun bindGastoRecurrenteRepo(impl: GastoRecurrenteRepositoryImpl): GastoRecurrenteRepository
    @Binds abstract fun bindRecRepo(impl: RecordatorioRepositoryImpl): RecordatorioRepository
    @Binds abstract fun bindListaCompraRepo(impl: ListaCompraRepositoryImpl): ListaCompraRepository
}
