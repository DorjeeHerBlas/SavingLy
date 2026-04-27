package com.misfinanzas.app.data.local.converters

import androidx.room.TypeConverter
import com.misfinanzas.app.domain.model.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class Converters {

    // LocalDate
    @TypeConverter fun fromLocalDate(date: LocalDate?): String? = date?.toString()
    @TypeConverter fun toLocalDate(s: String?): LocalDate? = s?.let { LocalDate.parse(it) }

    // LocalTime
    @TypeConverter fun fromLocalTime(t: LocalTime?): String? = t?.toString()
    @TypeConverter fun toLocalTime(s: String?): LocalTime? = s?.let { LocalTime.parse(it) }

    // YearMonth
    @TypeConverter fun fromYearMonth(ym: YearMonth?): String? = ym?.toString()
    @TypeConverter fun toYearMonth(s: String?): YearMonth? = s?.let { YearMonth.parse(it) }

    // Set<Int>
    @TypeConverter fun fromIntSet(set: Set<Int>?): String =
        set.orEmpty().joinToString(",")
    @TypeConverter fun toIntSet(s: String?): Set<Int> =
        if (s.isNullOrBlank()) emptySet() else s.split(",").mapNotNull { it.toIntOrNull() }.toSet()

    // Enums (almacenados como name)
    @TypeConverter fun fromCategoria(c: CategoriaGasto): String = c.name
    @TypeConverter fun toCategoria(s: String): CategoriaGasto = CategoriaGasto.valueOf(s)

    @TypeConverter fun fromMetodoPago(m: MetodoPago): String = m.name
    @TypeConverter fun toMetodoPago(s: String): MetodoPago = MetodoPago.valueOf(s)

    @TypeConverter fun fromFuenteIngreso(f: FuenteIngreso): String = f.name
    @TypeConverter fun toFuenteIngreso(s: String): FuenteIngreso = FuenteIngreso.valueOf(s)

    @TypeConverter fun fromTipoColeccion(t: TipoColeccion): String = t.name
    @TypeConverter fun toTipoColeccion(s: String): TipoColeccion = TipoColeccion.valueOf(s)

    @TypeConverter fun fromEstadoItem(e: EstadoItem): String = e.name
    @TypeConverter fun toEstadoItem(s: String): EstadoItem = EstadoItem.valueOf(s)

    @TypeConverter fun fromCondicion(c: CondicionItem): String = c.name
    @TypeConverter fun toCondicion(s: String): CondicionItem = CondicionItem.valueOf(s)

    @TypeConverter fun fromPrioridadWishlist(p: PrioridadWishlist): String = p.name
    @TypeConverter fun toPrioridadWishlist(s: String): PrioridadWishlist = PrioridadWishlist.valueOf(s)

    @TypeConverter fun fromTipoRecordatorio(t: TipoRecordatorio): String = t.name
    @TypeConverter fun toTipoRecordatorio(s: String): TipoRecordatorio = TipoRecordatorio.valueOf(s)

    @TypeConverter fun fromCategoriaProducto(c: CategoriaProducto): String = c.name
    @TypeConverter fun toCategoriaProducto(s: String): CategoriaProducto = CategoriaProducto.valueOf(s)
}
