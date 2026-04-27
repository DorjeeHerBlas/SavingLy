package com.misfinanzas.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

data class FunkoProduct(
    val nombre: String,
    val productId: String?,
    val boxNumber: String?,
    val license: String?,
    val category: String?,
    val availability: String?,
    val description: String?,
    val originalPrice: String?,
    val salePrice: String?,
    val image: String?,
    val url: String?,
    val chanceOfChase: Boolean,
    val precioEstimado: Double?
)

@Singleton
class ApifyFunkoRepository @Inject constructor() {

    suspend fun buscar(token: String, query: String, store: String = "ES"): List<FunkoProduct> =
        withContext(Dispatchers.IO) {
            val cleanToken = token.trim()
            val cleanQuery = query.trim()
            if (cleanToken.isBlank()) error("Falta el token de Apify")
            if (cleanQuery.isBlank()) return@withContext emptyList()

            val input = JSONObject()
                .put("search", cleanQuery)
                .put("productFilter", "pop")
                .put("store", store)
                .put("maxPages", 2)

            val endpoint = "https://api.apify.com/v2/acts/true.false.maybe~funko-pop-product-scraper/run-sync-get-dataset-items" +
                "?token=${cleanToken.url()}&format=json&clean=true&maxItems=60"
            val results = postJsonArray(endpoint, input)

            buildList {
                for (i in 0 until results.length()) {
                    results.optJSONObject(i)?.toFunkoProduct()?.let(::add)
                }
            }
        }

    private fun postJsonArray(url: String, input: JSONObject): JSONArray {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 300_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        connection.outputStream.use { it.write(input.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) error("Apify respondió $code")
        return JSONArray(body)
    }
}

private fun JSONObject.toFunkoProduct(): FunkoProduct? {
    val name = optNullableString("name") ?: return null
    val sale = optNullableString("salePrice")
    val original = optNullableString("originalPrice")
    return FunkoProduct(
        nombre = name,
        productId = optNullableString("productId"),
        boxNumber = optNullableString("boxNumber"),
        license = optNullableString("license"),
        category = optNullableString("category"),
        availability = optNullableString("availability"),
        description = optNullableString("description"),
        originalPrice = original,
        salePrice = sale,
        image = optNullableString("mainImage"),
        url = optNullableString("url"),
        chanceOfChase = optBoolean("chanceOfChase", false),
        precioEstimado = (sale ?: original)?.toPriceDouble()
    )
}

private fun JSONObject.optNullableString(name: String): String? =
    if (has(name) && !isNull(name)) optString(name).takeIf { it.isNotBlank() } else null

private fun String.toPriceDouble(): Double? {
    val normalized = filter { it.isDigit() || it == '.' || it == ',' }
        .replace(",", ".")
    return normalized.toDoubleOrNull()
}

private fun String.url(): String = URLEncoder.encode(this, "UTF-8")
