package com.misfinanzas.app.utils

import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formato {
    private val moneda: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
    private val fechaCorta = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "ES"))
    private val fechaLarga = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy", Locale("es", "ES"))
    private val mesAnyo = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))

    fun importe(valor: Double): String = moneda.format(valor)
    fun fecha(d: LocalDate): String = d.format(fechaCorta)
    fun fechaLarga(d: LocalDate): String = d.format(fechaLarga).replaceFirstChar { it.titlecase(Locale("es", "ES")) }
    fun mes(d: LocalDate): String = d.format(mesAnyo).replaceFirstChar { it.titlecase(Locale("es", "ES")) }
    fun mes(ym: YearMonth): String = ym.format(mesAnyo).replaceFirstChar { it.titlecase(Locale("es", "ES")) }
}
