package com.misfinanzas.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

data class ScryfallSetCard(
    val nombre: String,
    val setCode: String,
    val setName: String,
    val collectorNumber: String,
    val idioma: String?,
    val rareza: String?,
    val imagen: String?,
    val url: String?,
    val precio: Double?
)

data class ScryfallSet(
    val code: String,
    val name: String,
    val setType: String,
    val cardCount: Int,
    val releasedAt: String?,
    val iconSvgUri: String?,
    val parentSetCode: String?
)

@Singleton
class ScryfallSetRepository @Inject constructor() {

    suspend fun buscarSets(): List<ScryfallSet> = withContext(Dispatchers.IO) {
        val root = getJson("https://api.scryfall.com/sets")
        val data = root.optJSONArray("data") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until data.length()) {
                data.optJSONObject(i)?.toSet()?.let(::add)
            }
        }.sortedWith(
            compareByDescending<ScryfallSet> { it.releasedAt.orEmpty() }
                .thenBy { it.name }
        )
    }

    suspend fun buscarCartasPorSet(setCode: String): List<ScryfallSetCard> = withContext(Dispatchers.IO) {
        val code = setCode.trim().lowercase()
        if (code.isBlank()) return@withContext emptyList()

        val cards = mutableListOf<ScryfallSetCard>()
        var nextUrl: String? = "https://api.scryfall.com/cards/search?unique=prints&order=set&include_extras=true&q=${"e:$code".url()}"

        while (nextUrl != null) {
            val page = getJson(nextUrl)
            page.optJSONArray("data")?.let { data ->
                for (i in 0 until data.length()) {
                    data.optJSONObject(i)?.toSetCard()?.let(cards::add)
                }
            }
            nextUrl = page.optNullableString("next_page")
            if (nextUrl != null) delay(120)
        }

        cards.sortedWith(compareBy<ScryfallSetCard> { it.collectorNumber.toIntOrNull() ?: Int.MAX_VALUE }.thenBy { it.collectorNumber })
    }

    private fun JSONObject.toSet(): ScryfallSet? {
        val code = optNullableString("code") ?: return null
        val name = optNullableString("name") ?: return null
        return ScryfallSet(
            code = code.uppercase(),
            name = name,
            setType = optNullableString("set_type") ?: "",
            cardCount = optInt("card_count", 0),
            releasedAt = optNullableString("released_at"),
            iconSvgUri = optNullableString("icon_svg_uri"),
            parentSetCode = optNullableString("parent_set_code")?.uppercase()
        )
    }

    private fun JSONObject.toSetCard(): ScryfallSetCard? {
        val name = optNullableString("name") ?: return null
        val image = optJSONObject("image_uris")?.optNullableString("normal")
            ?: optJSONArray("card_faces")
                ?.firstObject()
                ?.optJSONObject("image_uris")
                ?.optNullableString("normal")
        val price = optJSONObject("prices")?.let {
            it.optNullableDouble("eur") ?: it.optNullableDouble("usd")
        }
        return ScryfallSetCard(
            nombre = name,
            setCode = optNullableString("set")?.uppercase() ?: "",
            setName = optNullableString("set_name") ?: "",
            collectorNumber = optNullableString("collector_number") ?: "",
            idioma = optNullableString("lang")?.uppercase(),
            rareza = optNullableString("rarity")?.replaceFirstChar(Char::uppercase),
            imagen = image,
            url = optNullableString("scryfall_uri"),
            precio = price
        )
    }

    private fun getJson(url: String): JSONObject = JSONObject(getText(url))

    private fun getText(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json;q=0.9,*/*;q=0.8")
            setRequestProperty("User-Agent", "MisFinanzas/1.0 Android MTG set search")
        }
        return connection.inputStream.bufferedReader().use { it.readText() }
    }
}

private fun String.url(): String = URLEncoder.encode(this, "UTF-8")

private fun JSONObject.optNullableString(name: String): String? =
    if (has(name) && !isNull(name)) optString(name).takeIf { it.isNotBlank() } else null

private fun JSONObject.optNullableDouble(name: String): Double? =
    optNullableString(name)?.toDoubleOrNull()

private fun JSONArray.firstObject(): JSONObject? =
    if (length() > 0) optJSONObject(0) else null
