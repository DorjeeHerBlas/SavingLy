package com.misfinanzas.app.data.repository

import com.misfinanzas.app.data.local.dao.ColeccionDao
import com.misfinanzas.app.data.local.dao.GastoDao
import com.misfinanzas.app.data.local.dao.GastoRecurrenteDao
import com.misfinanzas.app.data.local.dao.IngresoDao
import com.misfinanzas.app.data.local.dao.MetaAhorroDao
import com.misfinanzas.app.data.local.dao.PresupuestoDao
import com.misfinanzas.app.data.local.dao.RecordatorioDao
import com.misfinanzas.app.data.local.entities.AportacionEntity
import com.misfinanzas.app.data.local.entities.ColeccionEntity
import com.misfinanzas.app.data.local.entities.GastoEntity
import com.misfinanzas.app.data.local.entities.GastoRecurrenteEntity
import com.misfinanzas.app.data.local.entities.IngresoEntity
import com.misfinanzas.app.data.local.entities.ItemColeccionEntity
import com.misfinanzas.app.data.local.entities.MetaAhorroEntity
import com.misfinanzas.app.data.local.entities.PresupuestoMensualEntity
import com.misfinanzas.app.data.local.entities.RecordatorioEntity
import com.misfinanzas.app.domain.model.CategoriaGasto
import com.misfinanzas.app.domain.model.CondicionItem
import com.misfinanzas.app.domain.model.EstadoItem
import com.misfinanzas.app.domain.model.FuenteIngreso
import com.misfinanzas.app.domain.model.MetodoPago
import com.misfinanzas.app.domain.model.PrioridadWishlist
import com.misfinanzas.app.domain.model.TipoColeccion
import com.misfinanzas.app.domain.model.TipoRecordatorio
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val gastoDao: GastoDao,
    private val ingresoDao: IngresoDao,
    private val metaDao: MetaAhorroDao,
    private val coleccionDao: ColeccionDao,
    private val presupuestoDao: PresupuestoDao,
    private val recurrenteDao: GastoRecurrenteDao,
    private val recordatorioDao: RecordatorioDao
) {
    suspend fun exportarJson(): String = JSONObject()
        .put("schemaVersion", 2)
        .put("exportedAt", java.time.Instant.now().toString())
        .put("gastos", gastoDao.observarTodos().first().mapToJson { it.toJson() })
        .put("ingresos", ingresoDao.observarTodos().first().mapToJson { it.toJson() })
        .put("metas", metaDao.observarTodas().first().mapToJson { it.toJson() })
        .put("aportaciones", metaDao.observarTodasAportaciones().first().mapToJson { it.toJson() })
        .put("colecciones", coleccionDao.observarTodas().first().mapToJson { it.toJson() })
        .put("items", coleccionDao.observarTodosItems().first().mapToJson { it.toJson() })
        .put("presupuestos", presupuestoDao.observarTodos().first().mapToJson { it.toJson() })
        .put("recurrentes", recurrenteDao.observarTodos().first().mapToJson { it.toJson() })
        .put("recordatorios", recordatorioDao.observarTodos().first().mapToJson { it.toJson() })
        .toString(2)

    suspend fun importarJson(json: String) {
        val root = JSONObject(json)
        root.optJSONArray("gastos")?.forEachObject { gastoDao.insertar(it.toGastoEntity()) }
        root.optJSONArray("ingresos")?.forEachObject { ingresoDao.insertar(it.toIngresoEntity()) }
        root.optJSONArray("metas")?.forEachObject { metaDao.insertarMeta(it.toMetaEntity()) }
        root.optJSONArray("aportaciones")?.forEachObject { metaDao.insertarAportacion(it.toAportacionEntity()) }
        root.optJSONArray("colecciones")?.forEachObject { coleccionDao.insertarColeccion(it.toColeccionEntity()) }
        root.optJSONArray("items")?.forEachObject { coleccionDao.insertarItem(it.toItemEntity()) }
        root.optJSONArray("presupuestos")?.forEachObject { presupuestoDao.insertar(it.toPresupuestoEntity()) }
        root.optJSONArray("recurrentes")?.forEachObject { recurrenteDao.insertar(it.toRecurrenteEntity()) }
        root.optJSONArray("recordatorios")?.forEachObject { recordatorioDao.insertar(it.toRecordatorioEntity()) }
    }
}

private inline fun <T> Iterable<T>.mapToJson(block: (T) -> JSONObject): JSONArray =
    JSONArray().also { arr -> forEach { arr.put(block(it)) } }

private inline fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
    for (i in 0 until length()) block(getJSONObject(i))
}

private fun JSONObject.putOpt(name: String, value: Any?): JSONObject =
    if (value == null) put(name, JSONObject.NULL) else put(name, value)

private fun JSONObject.optNullableString(name: String): String? =
    if (has(name) && !isNull(name)) getString(name) else null

private fun JSONObject.optNullableDouble(name: String): Double? =
    if (has(name) && !isNull(name)) getDouble(name) else null

private fun JSONObject.optNullableLong(name: String): Long? =
    if (has(name) && !isNull(name)) getLong(name) else null

private fun JSONObject.optNullableDate(name: String): LocalDate? =
    optNullableString(name)?.let(LocalDate::parse)

private fun JSONObject.optNullableYearMonth(name: String): YearMonth? =
    optNullableString(name)?.let(YearMonth::parse)

private fun GastoEntity.toJson() = JSONObject()
    .put("id", id)
    .put("concepto", concepto)
    .put("importe", importe)
    .put("fecha", fecha.toString())
    .put("categoria", categoria.name)
    .put("metodoPago", metodoPago.name)
    .putOpt("nota", nota)

private fun JSONObject.toGastoEntity() = GastoEntity(
    id = optLong("id", 0),
    concepto = getString("concepto"),
    importe = getDouble("importe"),
    fecha = LocalDate.parse(getString("fecha")),
    categoria = CategoriaGasto.valueOf(getString("categoria")),
    metodoPago = MetodoPago.valueOf(getString("metodoPago")),
    nota = optNullableString("nota")
)

private fun IngresoEntity.toJson() = JSONObject()
    .put("id", id)
    .put("concepto", concepto)
    .put("importe", importe)
    .put("fecha", fecha.toString())
    .put("fuente", fuente.name)
    .putOpt("nota", nota)

private fun JSONObject.toIngresoEntity() = IngresoEntity(
    id = optLong("id", 0),
    concepto = getString("concepto"),
    importe = getDouble("importe"),
    fecha = LocalDate.parse(getString("fecha")),
    fuente = FuenteIngreso.valueOf(getString("fuente")),
    nota = optNullableString("nota")
)

private fun MetaAhorroEntity.toJson() = JSONObject()
    .put("id", id)
    .put("nombre", nombre)
    .put("importeObjetivo", importeObjetivo)
    .put("importeActual", importeActual)
    .put("fechaInicio", fechaInicio.toString())
    .putOpt("fechaLimite", fechaLimite?.toString())
    .put("color", color)
    .put("icono", icono)
    .putOpt("notas", notas)

private fun JSONObject.toMetaEntity() = MetaAhorroEntity(
    id = optLong("id", 0),
    nombre = getString("nombre"),
    importeObjetivo = getDouble("importeObjetivo"),
    importeActual = getDouble("importeActual"),
    fechaInicio = LocalDate.parse(getString("fechaInicio")),
    fechaLimite = optNullableDate("fechaLimite"),
    color = getLong("color"),
    icono = getString("icono"),
    notas = optNullableString("notas")
)

private fun AportacionEntity.toJson() = JSONObject()
    .put("id", id)
    .put("metaId", metaId)
    .put("importe", importe)
    .put("fecha", fecha.toString())
    .putOpt("nota", nota)

private fun JSONObject.toAportacionEntity() = AportacionEntity(
    id = optLong("id", 0),
    metaId = getLong("metaId"),
    importe = getDouble("importe"),
    fecha = LocalDate.parse(getString("fecha")),
    nota = optNullableString("nota")
)

private fun ColeccionEntity.toJson() = JSONObject()
    .put("id", id)
    .put("nombre", nombre)
    .put("tipo", tipo.name)
    .putOpt("descripcion", descripcion)
    .put("color", color)
    .put("icono", icono)
    .put("fechaCreacion", fechaCreacion.toString())

private fun JSONObject.toColeccionEntity() = ColeccionEntity(
    id = optLong("id", 0),
    nombre = getString("nombre"),
    tipo = TipoColeccion.valueOf(getString("tipo")),
    descripcion = optNullableString("descripcion"),
    color = getLong("color"),
    icono = getString("icono"),
    fechaCreacion = LocalDate.parse(getString("fechaCreacion"))
)

private fun ItemColeccionEntity.toJson() = JSONObject()
    .put("id", id)
    .put("coleccionId", coleccionId)
    .put("nombre", nombre)
    .put("estado", estado.name)
    .putOpt("precio", precio)
    .putOpt("precioPagado", precioPagado)
    .put("cantidad", cantidad)
    .putOpt("fechaAdquisicion", fechaAdquisicion?.toString())
    .putOpt("notas", notas)
    .putOpt("rutaImagen", rutaImagen)
    .putOpt("urlReferencia", urlReferencia)
    .put("condicion", condicion.name)
    .put("esFavorito", esFavorito)
    .putOpt("rareza", rareza)
    .put("prioridad", prioridad.name)
    .putOpt("autor", autor)
    .putOpt("plataforma", plataforma)
    .putOpt("setColeccion", setColeccion)
    .putOpt("idioma", idioma)
    .putOpt("codigoBarras", codigoBarras)

private fun JSONObject.toItemEntity() = ItemColeccionEntity(
    id = optLong("id", 0),
    coleccionId = getLong("coleccionId"),
    nombre = getString("nombre"),
    estado = EstadoItem.valueOf(getString("estado")),
    precio = optNullableDouble("precio"),
    precioPagado = optNullableDouble("precioPagado"),
    cantidad = optInt("cantidad", 1),
    fechaAdquisicion = optNullableDate("fechaAdquisicion"),
    notas = optNullableString("notas"),
    rutaImagen = optNullableString("rutaImagen"),
    urlReferencia = optNullableString("urlReferencia"),
    condicion = CondicionItem.valueOf(getString("condicion")),
    esFavorito = optBoolean("esFavorito", false),
    rareza = optNullableString("rareza"),
    prioridad = PrioridadWishlist.valueOf(optString("prioridad", PrioridadWishlist.MEDIA.name)),
    autor = optNullableString("autor"),
    plataforma = optNullableString("plataforma"),
    setColeccion = optNullableString("setColeccion"),
    idioma = optNullableString("idioma"),
    codigoBarras = optNullableString("codigoBarras")
)

private fun PresupuestoMensualEntity.toJson() = JSONObject()
    .put("id", id)
    .put("mes", mes.toString())
    .put("categoria", categoria.name)
    .put("limite", limite)

private fun JSONObject.toPresupuestoEntity() = PresupuestoMensualEntity(
    id = optLong("id", 0),
    mes = YearMonth.parse(getString("mes")),
    categoria = CategoriaGasto.valueOf(getString("categoria")),
    limite = getDouble("limite")
)

private fun GastoRecurrenteEntity.toJson() = JSONObject()
    .put("id", id)
    .put("concepto", concepto)
    .put("importe", importe)
    .put("categoria", categoria.name)
    .put("metodoPago", metodoPago.name)
    .put("diaMes", diaMes)
    .put("activo", activo)
    .putOpt("ultimoGenerado", ultimoGenerado?.toString())
    .putOpt("nota", nota)

private fun JSONObject.toRecurrenteEntity() = GastoRecurrenteEntity(
    id = optLong("id", 0),
    concepto = getString("concepto"),
    importe = getDouble("importe"),
    categoria = CategoriaGasto.valueOf(getString("categoria")),
    metodoPago = MetodoPago.valueOf(getString("metodoPago")),
    diaMes = getInt("diaMes"),
    activo = optBoolean("activo", true),
    ultimoGenerado = optNullableYearMonth("ultimoGenerado"),
    nota = optNullableString("nota")
)

private fun RecordatorioEntity.toJson() = JSONObject()
    .put("id", id)
    .put("titulo", titulo)
    .put("mensaje", mensaje)
    .put("tipo", tipo.name)
    .put("hora", hora.toString())
    .put("diasSemana", diasSemana.mapToJson { JSONObject().put("dia", it) })
    .put("activo", activo)
    .putOpt("referenciaId", referenciaId)

private fun JSONObject.toRecordatorioEntity() = RecordatorioEntity(
    id = optLong("id", 0),
    titulo = getString("titulo"),
    mensaje = getString("mensaje"),
    tipo = TipoRecordatorio.valueOf(getString("tipo")),
    hora = LocalTime.parse(getString("hora")),
    diasSemana = optJSONArray("diasSemana")?.let { arr ->
        buildSet {
            for (i in 0 until arr.length()) add(arr.getJSONObject(i).getInt("dia"))
        }
    } ?: emptySet(),
    activo = optBoolean("activo", true),
    referenciaId = optNullableLong("referenciaId")
)
