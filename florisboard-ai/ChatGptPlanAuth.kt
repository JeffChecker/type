package dev.patrickgold.florisboard.ime.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URL
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.RSAPublicKeySpec
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Offizieller "Sign in with ChatGPT" OAuth Ablauf für Open Source Apps.
 *
 * Die App verwendet ausschließlich die dokumentierten OpenAI Endpunkte:
 * https://auth.openai.com/api/accounts/authorize
 * https://auth.openai.com/api/accounts/oauth/token
 * https://api.openai.com/v1
 *
 * Es wird weder chatgpt.com automatisiert noch ein Nutzer API Schlüssel ausgelesen.
 */
object ChatGptPlanAuth {
    private const val AUTHORIZE_ENDPOINT = "https://auth.openai.com/api/accounts/authorize"
    private const val TOKEN_ENDPOINT = "https://auth.openai.com/api/accounts/oauth/token"
    private const val REVOCATION_ENDPOINT = "https://auth.openai.com/api/accounts/oauth/revoke"
    private const val JWKS_ENDPOINT = "https://auth.openai.com/.well-known/jwks.json"
    private const val ISSUER = "https://auth.openai.com"
    private const val RESOURCE = "https://api.openai.com/v1"
    private const val DYNAMIC_CLIENT_ID = "dynamic_agent_client"
    private const val AGENT_NAME = "KI Tastatur"
    private const val REQUIRED_SCOPE = "chatgpt.tokens.use.direct"
    private const val REQUESTED_SCOPES =
        "openid profile email offline_access resource.invoke chatgpt.tokens.use.direct"

    private const val KEY_HOST_ID = "chatgpt_host_id"
    private const val KEY_CLIENT_ID = "chatgpt_client_id"
    private const val KEY_SUBJECT = "chatgpt_subject"
    private const val KEY_EMAIL = "chatgpt_email"
    private const val KEY_ID_TOKEN = "chatgpt_id_token"
    private const val KEY_ACCESS_TOKEN = "chatgpt_access_token"
    private const val KEY_REFRESH_TOKEN = "chatgpt_refresh_token"
    private const val KEY_ACCESS_EXPIRES_AT = "chatgpt_access_expires_at"
    private const val KEY_SCOPES = "chatgpt_scopes"
    const val KEY_USE_CHATGPT_PLAN = "openai_use_chatgpt_plan"

    private val json = Json { ignoreUnknownKeys = true }
    private val refreshMutex = Mutex()

    private fun prefs(context: Context) =
        context.getSharedPreferences(AiAssistant.PREFS_NAME, Context.MODE_PRIVATE)

    fun isConnected(context: Context): Boolean {
        val p = prefs(context)
        return p.getString(KEY_CLIENT_ID, "").orEmpty().startsWith("oaiapp_") &&
            (
                p.getString(KEY_ACCESS_TOKEN, "").orEmpty().isNotBlank() ||
                    p.getString(KEY_REFRESH_TOKEN, "").orEmpty().isNotBlank()
                )
    }

    fun hasPlanPermission(context: Context): Boolean =
        prefs(context).getString(KEY_SCOPES, "").orEmpty()
            .split(' ')
            .any { it == REQUIRED_SCOPE }

    fun shouldUsePlan(context: Context): Boolean =
        prefs(context).getBoolean(KEY_USE_CHATGPT_PLAN, true) &&
            isConnected(context) &&
            hasPlanPermission(context)

    fun accountLabel(context: Context): String =
        prefs(context).getString(KEY_EMAIL, "").orEmpty().ifBlank { "ChatGPT Konto" }

    fun setUsePlan(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_USE_CHATGPT_PLAN, enabled).apply()
    }

    private fun stableHostId(context: Context): String {
        val p = prefs(context)
        val saved = p.getString(KEY_HOST_ID, "").orEmpty()
        if (saved.isNotBlank()) return saved
        val created = "urn:uuid:" + UUID.randomUUID().toString()
        p.edit().putString(KEY_HOST_ID, created).commit()
        return created
    }

    suspend fun signIn(context: Context): String = withContext(Dispatchers.IO) {
        val p = prefs(context)
        val existingClientId = p.getString(KEY_CLIENT_ID, "").orEmpty()
            .takeIf { it.startsWith("oaiapp_") }
        val requestedClientId = existingClientId ?: DYNAMIC_CLIENT_ID
        val hostId = stableHostId(context)

        val state = randomToken(32)
        val nonce = randomToken(32)
        val verifier = randomToken(64)
        val challenge = base64Url(
            MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        )

        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            server.soTimeout = 180_000
            val redirectUri = "http://127.0.0.1:${server.localPort}/auth/callback"

            val authUri = Uri.parse(AUTHORIZE_ENDPOINT).buildUpon()
                .appendQueryParameter("client_id", requestedClientId)
                .appendQueryParameter("ext_agent_host_id", hostId)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("redirect_uri", redirectUri)
                .appendQueryParameter("scope", REQUESTED_SCOPES)
                .appendQueryParameter("resource", RESOURCE)
                .appendQueryParameter("state", state)
                .appendQueryParameter("nonce", nonce)
                .appendQueryParameter("code_challenge_method", "S256")
                .appendQueryParameter("code_challenge", challenge)
                .apply {
                    if (existingClientId == null) {
                        appendQueryParameter("agent_name_hint", AGENT_NAME)
                    } else {
                        p.getString(KEY_ID_TOKEN, "").orEmpty().takeIf { it.isNotBlank() }?.let {
                            appendQueryParameter("id_token_hint", it)
                        }
                        p.getString(KEY_EMAIL, "").orEmpty().takeIf { it.isNotBlank() }?.let {
                            appendQueryParameter("login_hint", it)
                        }
                    }
                }
                .build()

            withContext(Dispatchers.Main) {
                val intent = Intent(Intent.ACTION_VIEW, authUri).apply {
                    if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }

            val callback = server.accept().use { socket ->
                socket.soTimeout = 15_000
                val reader = socket.getInputStream().bufferedReader(Charsets.UTF_8)
                val requestLine = reader.readLine().orEmpty()
                val target = requestLine.split(' ').getOrNull(1).orEmpty()
                val callbackUri = Uri.parse("http://127.0.0.1$target")

                val html = """
                    <!doctype html>
                    <html lang="de">
                    <head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                    <body style="font-family:sans-serif;padding:32px">
                    <h2>KI Tastatur</h2>
                    <p>Die ChatGPT Anmeldung wurde an die App zurückgegeben. Du kannst dieses Fenster schließen und zur KI Tastatur zurückkehren.</p>
                    </body></html>
                """.trimIndent()
                val bytes = html.toByteArray(Charsets.UTF_8)
                socket.getOutputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write("HTTP/1.1 200 OK\r\n")
                    writer.write("Content-Type: text/html; charset=utf-8\r\n")
                    writer.write("Content-Length: ${bytes.size}\r\n")
                    writer.write("Connection: close\r\n\r\n")
                    writer.write(html)
                    writer.flush()
                }
                callbackUri
            }

            val returnedState = callback.getQueryParameter("state").orEmpty()
            if (returnedState != state) {
                throw AiException("ChatGPT Anmeldung konnte nicht sicher bestätigt werden. Bitte erneut versuchen.")
            }

            callback.getQueryParameter("error")?.let {
                throw AiException(
                    if (it == "access_denied") {
                        "Die ChatGPT Anmeldung oder die Nutzung des ChatGPT Plans wurde nicht freigegeben."
                    } else {
                        "ChatGPT Anmeldung fehlgeschlagen: $it"
                    }
                )
            }

            val code = callback.getQueryParameter("code").orEmpty()
            if (code.isBlank()) throw AiException("ChatGPT hat keinen Anmeldecode zurückgegeben.")

            val callbackClientId = callback.getQueryParameter("client_id").orEmpty()
            val issuedClientId = if (existingClientId == null) {
                callbackClientId.takeIf { it.startsWith("oaiapp_") }
                    ?: throw AiException("ChatGPT hat keine gültige App Registrierung zurückgegeben.")
            } else {
                if (callbackClientId.isNotBlank() && callbackClientId != existingClientId) {
                    throw AiException("Die ChatGPT Anmeldung gehört nicht zur gespeicherten Registrierung.")
                }
                existingClientId
            }

            val tokenResponse = postForm(
                TOKEN_ENDPOINT,
                mapOf(
                    "grant_type" to "authorization_code",
                    "client_id" to issuedClientId,
                    "code" to code,
                    "code_verifier" to verifier,
                    "redirect_uri" to redirectUri,
                    "resource" to RESOURCE,
                )
            )
            if (tokenResponse.first !in 200..299) {
                throw oauthError("ChatGPT Token Austausch fehlgeschlagen", tokenResponse.second)
            }

            val tokenJson = json.parseToJsonElement(tokenResponse.second).jsonObject
            val accessToken = tokenJson.string("access_token")
            val refreshToken = tokenJson.string("refresh_token")
            val idToken = tokenJson.string("id_token")
            val scopes = tokenJson.string("scope")
            val expiresIn = tokenJson.long("expires_in") ?: 3600L

            if (accessToken.isBlank() || refreshToken.isBlank() || idToken.isBlank()) {
                throw AiException("ChatGPT hat unvollständige Anmeldedaten zurückgegeben.")
            }
            if (scopes.split(' ').none { it == REQUIRED_SCOPE }) {
                throw AiException(
                    "Das ChatGPT Konto ist angemeldet, aber die Nutzung des ChatGPT Plans für KI Anfragen wurde nicht freigegeben."
                )
            }

            val identity = verifyIdToken(idToken, issuedClientId, nonce)
            val previousSubject = p.getString(KEY_SUBJECT, "").orEmpty()
            if (existingClientId != null && previousSubject.isNotBlank() && previousSubject != identity.subject) {
                throw AiException("Das angemeldete ChatGPT Konto stimmt nicht mit der gespeicherten Registrierung überein.")
            }

            val now = System.currentTimeMillis()
            p.edit()
                .putString(KEY_CLIENT_ID, issuedClientId)
                .putString(KEY_SUBJECT, identity.subject)
                .putString(KEY_EMAIL, identity.email)
                .putString(KEY_ID_TOKEN, idToken)
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putLong(KEY_ACCESS_EXPIRES_AT, now + expiresIn * 1000L)
                .putString(KEY_SCOPES, scopes)
                .putBoolean(KEY_USE_CHATGPT_PLAN, true)
                .commit()

            if (identity.email.isNotBlank()) {
                "✓ Mit ChatGPT verbunden: ${identity.email}"
            } else {
                "✓ Mit ChatGPT verbunden. Der ChatGPT Plan wird für OpenAI Anfragen verwendet."
            }
        }
    }

    suspend fun accessToken(context: Context): String = refreshMutex.withLock {
        val p = prefs(context)
        if (!hasPlanPermission(context)) {
            throw AiException("Die ChatGPT Plan Nutzung ist für dieses Konto nicht freigegeben.")
        }

        val current = p.getString(KEY_ACCESS_TOKEN, "").orEmpty()
        val expiresAt = p.getLong(KEY_ACCESS_EXPIRES_AT, 0L)
        if (current.isNotBlank() && System.currentTimeMillis() + 120_000L < expiresAt) {
            return@withLock current
        }

        val clientId = p.getString(KEY_CLIENT_ID, "").orEmpty()
        val refreshToken = p.getString(KEY_REFRESH_TOKEN, "").orEmpty()
        if (!clientId.startsWith("oaiapp_") || refreshToken.isBlank()) {
            throw AiException("ChatGPT Anmeldung ist abgelaufen. Bitte erneut mit ChatGPT anmelden.")
        }

        val response = withContext(Dispatchers.IO) {
            postForm(
                TOKEN_ENDPOINT,
                mapOf(
                    "grant_type" to "refresh_token",
                    "client_id" to clientId,
                    "refresh_token" to refreshToken,
                    "resource" to RESOURCE,
                )
            )
        }

        if (response.first !in 200..299) {
            val lower = response.second.lowercase()
            if (
                "invalid_grant" in lower ||
                "invalid_refresh_token" in lower ||
                "refresh_token_expired" in lower ||
                "refresh_token_invalidated" in lower ||
                "refresh_token_reused" in lower
            ) {
                clearTokens(context)
                throw AiException("ChatGPT Anmeldung ist abgelaufen. Bitte erneut mit ChatGPT anmelden.")
            }
            throw oauthError("ChatGPT Anmeldung konnte nicht erneuert werden", response.second)
        }

        val root = json.parseToJsonElement(response.second).jsonObject
        val newAccess = root.string("access_token")
        val newRefresh = root.string("refresh_token")
        val newIdToken = root.string("id_token")
        val newScopes = root.string("scope").ifBlank { p.getString(KEY_SCOPES, "").orEmpty() }
        val expiresIn = root.long("expires_in") ?: 3600L
        if (newAccess.isBlank() || newRefresh.isBlank()) {
            throw AiException("ChatGPT hat bei der Erneuerung unvollständige Anmeldedaten zurückgegeben.")
        }

        p.edit()
            .putString(KEY_ACCESS_TOKEN, newAccess)
            .putString(KEY_REFRESH_TOKEN, newRefresh)
            .putString(KEY_SCOPES, newScopes)
            .putLong(KEY_ACCESS_EXPIRES_AT, System.currentTimeMillis() + expiresIn * 1000L)
            .apply {
                if (newIdToken.isNotBlank()) putString(KEY_ID_TOKEN, newIdToken)
            }
            .commit()

        newAccess
    }

    suspend fun signOut(context: Context): String {
        val p = prefs(context)
        val clientId = p.getString(KEY_CLIENT_ID, "").orEmpty()
        val refreshToken = p.getString(KEY_REFRESH_TOKEN, "").orEmpty()
        var remotelyRevoked = true

        if (clientId.startsWith("oaiapp_") && refreshToken.isNotBlank()) {
            val response = withContext(Dispatchers.IO) {
                runCatching {
                    postForm(
                        REVOCATION_ENDPOINT,
                        mapOf(
                            "token" to refreshToken,
                            "token_type_hint" to "refresh_token",
                            "client_id" to clientId,
                        )
                    )
                }.getOrNull()
            }
            remotelyRevoked = response?.first?.let { it in 200..299 } == true
        }

        clearTokens(context)
        return if (remotelyRevoked) {
            "ChatGPT Konto wurde abgemeldet."
        } else {
            "Lokal abgemeldet. Die Verbindung konnte bei OpenAI gerade nicht bestätigt werden."
        }
    }

    private fun clearTokens(context: Context) {
        prefs(context).edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_ID_TOKEN)
            .remove(KEY_ACCESS_EXPIRES_AT)
            .remove(KEY_SCOPES)
            .putBoolean(KEY_USE_CHATGPT_PLAN, false)
            .apply()
    }

    private data class Identity(val subject: String, val email: String)

    private fun verifyIdToken(idToken: String, expectedClientId: String, expectedNonce: String): Identity {
        val parts = idToken.split('.')
        if (parts.size != 3) throw AiException("Ungültiges ChatGPT Identitätstoken.")

        val header = json.parseToJsonElement(
            String(base64UrlDecode(parts[0]), Charsets.UTF_8)
        ).jsonObject
        val payload = json.parseToJsonElement(
            String(base64UrlDecode(parts[1]), Charsets.UTF_8)
        ).jsonObject

        val alg = header.string("alg")
        val kid = header.string("kid")
        if (alg != "RS256" || kid.isBlank()) throw AiException("ChatGPT Identitätstoken verwendet eine unerwartete Signatur.")

        val jwksResponse = get(JWKS_ENDPOINT)
        if (jwksResponse.first !in 200..299) throw AiException("OpenAI Signaturschlüssel konnten nicht geladen werden.")
        val keys = json.parseToJsonElement(jwksResponse.second).jsonObject["keys"]?.jsonArray
            ?: throw AiException("OpenAI Signaturschlüssel sind unvollständig.")
        val jwk = keys.mapNotNull { runCatching { it.jsonObject }.getOrNull() }
            .firstOrNull { it.string("kid") == kid }
            ?: throw AiException("Passender OpenAI Signaturschlüssel wurde nicht gefunden.")

        val n = BigInteger(1, base64UrlDecode(jwk.string("n")))
        val e = BigInteger(1, base64UrlDecode(jwk.string("e")))
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(n, e))
        val verified = Signature.getInstance("SHA256withRSA").run {
            initVerify(publicKey)
            update((parts[0] + "." + parts[1]).toByteArray(Charsets.US_ASCII))
            verify(base64UrlDecode(parts[2]))
        }
        if (!verified) throw AiException("Die ChatGPT Identität konnte nicht verifiziert werden.")

        val issuer = payload.string("iss")
        val subject = payload.string("sub")
        val nonce = payload.string("nonce")
        val exp = payload.long("exp") ?: 0L
        val now = System.currentTimeMillis() / 1000L
        val audienceOk = when (val aud = payload["aud"]) {
            is JsonArray -> aud.any { runCatching { it.jsonPrimitive.content }.getOrNull() == expectedClientId }
            else -> runCatching { aud?.jsonPrimitive?.content }.getOrNull() == expectedClientId
        }

        if (issuer != ISSUER) throw AiException("Unerwarteter Aussteller des ChatGPT Identitätstokens.")
        if (!audienceOk) throw AiException("ChatGPT Identitätstoken gehört nicht zu dieser App Registrierung.")
        if (exp <= now - 5L) throw AiException("ChatGPT Identitätstoken ist abgelaufen.")
        if (nonce != expectedNonce) throw AiException("ChatGPT Anmeldung konnte nicht eindeutig dieser Anfrage zugeordnet werden.")
        if (subject.isBlank()) throw AiException("ChatGPT Identitätstoken enthält keine Konto Identität.")

        return Identity(subject, payload.string("email"))
    }

    private fun randomToken(size: Int): String {
        val bytes = ByteArray(size)
        SecureRandom().nextBytes(bytes)
        return base64Url(bytes)
    }

    private fun base64Url(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private fun base64UrlDecode(value: String): ByteArray =
        Base64.decode(value, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private fun postForm(endpoint: String, values: Map<String, String>): Pair<Int, String> {
        val body = values.entries.joinToString("&") { (key, value) ->
            URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
        }
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
        }
        return try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            read(connection)
        } finally {
            connection.disconnect()
        }
    }

    private fun get(endpoint: String): Pair<Int, String> {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            read(connection)
        } finally {
            connection.disconnect()
        }
    }

    private fun read(connection: HttpURLConnection): Pair<Int, String> {
        val code = connection.responseCode
        val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()
        return code to text
    }

    private fun oauthError(prefix: String, body: String): AiException {
        val detail = runCatching {
            val root = json.parseToJsonElement(body).jsonObject
            root.string("error_description").ifBlank { root.string("error") }
        }.getOrNull().orEmpty()
        return AiException(if (detail.isBlank()) prefix else "$prefix: $detail")
    }

    private fun JsonObject.string(key: String): String =
        runCatching { this[key]?.jsonPrimitive?.content }.getOrNull().orEmpty()

    private fun JsonObject.long(key: String): Long? =
        runCatching { this[key]?.jsonPrimitive?.content?.toLongOrNull() }.getOrNull()
}
