package dev.patrickgold.florisboard.ime.ai

import android.content.Context
import android.text.Html
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

enum class TranslationMode(val id: String, val displayName: String) {
    AUTO("auto", "Automatisch"),
    CLOUD("cloud", "Google Cloud"),
    LOCAL("local", "Offline");

    companion object {
        fun fromId(id: String?): TranslationMode = entries.firstOrNull { it.id == id } ?: AUTO
    }
}

data class TranslationLanguage(
    val code: String,
    val name: String,
)

data class CloudTranslationResult(
    val text: String,
    val detectedSourceLanguage: String?,
)

object TranslationBackend {
    const val KEY_GOOGLE_TRANSLATE_API_KEY = "google_translate_api_key"
    const val KEY_TRANSLATION_MODE = "translation_mode"
    const val KEY_TRANSLATION_TARGET = "translation_target"
    const val TARGET_ACTIVE_KEYBOARD = "keyboard"

    private const val TRANSLATE_ENDPOINT = "https://translation.googleapis.com/language/translate/v2"
    private const val LANGUAGES_ENDPOINT = "https://translation.googleapis.com/language/translate/v2/languages"

    fun mode(context: Context): TranslationMode {
        val prefs = context.getSharedPreferences(AiAssistant.PREFS_NAME, Context.MODE_PRIVATE)
        return TranslationMode.fromId(prefs.getString(KEY_TRANSLATION_MODE, TranslationMode.AUTO.id))
    }

    fun apiKey(context: Context): String {
        val prefs = context.getSharedPreferences(AiAssistant.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_GOOGLE_TRANSLATE_API_KEY, "").orEmpty().trim()
    }

    fun targetSetting(context: Context): String {
        val prefs = context.getSharedPreferences(AiAssistant.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TRANSLATION_TARGET, TARGET_ACTIVE_KEYBOARD)
            .orEmpty().ifBlank { TARGET_ACTIVE_KEYBOARD }
    }

    fun apiConsoleUrl(): String = "https://console.cloud.google.com/apis/library/translate.googleapis.com"

    fun normalizeKeyboardLanguageTag(tag: String): String {
        val cleaned = tag.replace('_', '-')
        val lower = cleaned.lowercase()
        return when {
            lower == "zh-tw" || lower == "zh-hant" || lower.startsWith("zh-hant-") -> "zh-TW"
            lower == "zh-cn" || lower == "zh-hans" || lower.startsWith("zh-hans-") -> "zh-CN"
            else -> cleaned.substringBefore('-').lowercase()
        }
    }

    suspend fun translateCloud(
        apiKey: String,
        text: String,
        targetLanguage: String,
    ): CloudTranslationResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw AiException("Kein Google Cloud Translation API Schlüssel eingerichtet")
        if (targetLanguage.isBlank()) throw AiException("Keine Zielsprache ausgewählt")

        val key = URLEncoder.encode(apiKey.trim(), "UTF-8")
        val endpoint = "$TRANSLATE_ENDPOINT?key=$key"
        val body = buildJsonObject {
            put("q", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive(text)) })
            put("target", targetLanguage)
            put("format", "text")
        }

        val (code, response) = post(endpoint, body.toString())
        checkError(code, response)
        val root = Json.parseToJsonElement(response).jsonObject
        val item = root["data"]?.jsonObject?.get("translations")?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw AiException("Google Translate hat keine Übersetzung zurückgegeben")
        val translatedRaw = item["translatedText"]?.jsonPrimitive?.content.orEmpty()
        val detected = item["detectedSourceLanguage"]?.jsonPrimitive?.content
        val translated = decodeHtmlEntities(translatedRaw).trim()
        if (translated.isBlank()) throw AiException("Google Translate hat keinen Text zurückgegeben")
        CloudTranslationResult(translated, detected)
    }

    suspend fun listCloudLanguages(
        apiKey: String,
        displayLanguage: String = "de",
    ): List<TranslationLanguage> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw AiException("Bitte zuerst einen Google Cloud Translation API Schlüssel eintragen")
        val key = URLEncoder.encode(apiKey.trim(), "UTF-8")
        val target = URLEncoder.encode(displayLanguage, "UTF-8")
        val endpoint = "$LANGUAGES_ENDPOINT?target=$target&key=$key"
        val (code, response) = get(endpoint)
        checkError(code, response)
        val root = Json.parseToJsonElement(response).jsonObject
        val languages = root["data"]?.jsonObject?.get("languages")?.jsonArray.orEmpty()
            .mapNotNull { item ->
                val obj = item.jsonObject
                val codeValue = obj["language"]?.jsonPrimitive?.content.orEmpty()
                val nameValue = obj["name"]?.jsonPrimitive?.content.orEmpty().ifBlank { codeValue }
                codeValue.takeIf { it.isNotBlank() }?.let { TranslationLanguage(it, nameValue) }
            }
            .distinctBy { it.code }
            .sortedBy { it.name.lowercase() }
        if (languages.isEmpty()) throw AiException("Google Translate hat keine Sprachliste geliefert")
        languages
    }

    private fun get(endpoint: String): Pair<Int, String> {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
        }
        return readResponse(connection)
    }

    private fun post(endpoint: String, body: String): Pair<Int, String> {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            readResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    private fun readResponse(connection: HttpURLConnection): Pair<Int, String> = try {
        val code = connection.responseCode
        val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        code to text
    } finally {
        connection.disconnect()
    }

    private fun checkError(code: Int, response: String) {
        if (code in 200..299) return
        val detail = runCatching {
            Json.parseToJsonElement(response).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
        }.getOrNull()
        when (code) {
            400 -> throw AiException(detail ?: "Google Cloud Translation Anfrage ist ungültig")
            401, 403 -> throw AiException("Google Cloud Translation API Schlüssel ist ungültig, eingeschränkt oder die API ist nicht aktiviert")
            429 -> throw AiException("Google Cloud Translation Limit oder Kontingent erreicht")
            else -> throw AiException(detail?.takeIf { it.isNotBlank() } ?: "Google Cloud Translation fehlgeschlagen (HTTP $code)")
        }
    }

    @Suppress("DEPRECATION")
    private fun decodeHtmlEntities(value: String): String =
        Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString()
}
