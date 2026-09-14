package dev.patrickgold.florisboard.ime.ai

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.Toast
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.editor.EditorContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** KI Schreibassistent für FlorisBoard. */
class AiAssistant(private val context: Context) {
    companion object {
        const val PREFS_NAME = "floris_ai"
        const val KEY_AUTO_CORRECTION = "auto_correction"
        const val KEY_API_KEY = AiBackend.KEY_GROQ_API_KEY
        const val KEY_MODEL = AiBackend.LEGACY_KEY_MODEL
        const val DEFAULT_MODEL = AiBackend.AUTO_MODEL

        suspend fun testConnection(provider: AiProvider, apiKey: String, model: String): String =
            AiBackend.testConnection(provider, apiKey, model)
    }

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val editorInstance by context.editorInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var autoJob: Job? = null
    private var previousText: String = ""
    private var previousOffset: Int = -1
    private var suppressUntil: Long = 0L

    fun openSettings() {
        val intent = Intent(appContext, AiSettingsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(intent)
    }

    fun toggleAutoCorrection() {
        if (!AiBackend.hasApiKey(appContext)) {
            toast("Für die KI Autokorrektur zuerst einen ${AiBackend.providerDisplayName(appContext)} API Schlüssel eintragen")
            openSettings()
            return
        }
        val enabled = !prefs.getBoolean(KEY_AUTO_CORRECTION, true)
        prefs.edit().putBoolean(KEY_AUTO_CORRECTION, enabled).apply()
        toast(if (enabled) "KI Autokorrektur aktiviert" else "KI Autokorrektur deaktiviert")
    }

    fun onContentChanged(
        content: EditorContent,
        sensitiveField: Boolean,
        incognito: Boolean,
        rawEditor: Boolean,
    ) {
        val now = SystemClock.elapsedRealtime()
        val voiceLike = detectVoiceLike(content)
        previousText = content.text
        previousOffset = content.offset

        if (now < suppressUntil) return
        if (sensitiveField || incognito || rawEditor) return
        if (!prefs.getBoolean(KEY_AUTO_CORRECTION, true)) return
        if (!AiBackend.hasApiKey(appContext)) return
        if (!content.localSelection.isValid || content.selectedText.isNotEmpty()) return

        val target = currentSentenceTarget(content) ?: return
        val trimmed = target.text.trim()
        if (trimmed.length < 5 || trimmed.length > 650) return
        if (trimmed.count { it.isLetter() } < 3) return

        autoJob?.cancel()
        val endsSentence = trimmed.last() in charArrayOf('.', '!', '?', '…')
        val waitMs = when {
            voiceLike -> 450L
            endsSentence -> 550L
            else -> 1_250L
        }
        autoJob = scope.launch {
            delay(waitMs)
            val corrected = try {
                AiBackend.request(appContext, AiStyle.CORRECT, target.text, voiceLike)
            } catch (_: Throwable) {
                return@launch
            }
            if (corrected == target.text || corrected.isBlank()) return@launch
            if (corrected.length > target.text.length * 2 + 80) return@launch
            applyIfStillCurrent(target, corrected)
        }
    }

    fun runStyle(style: AiStyle, sensitiveField: Boolean, incognito: Boolean, rawEditor: Boolean) {
        if (sensitiveField || rawEditor) {
            toast("KI ist in diesem Eingabefeld deaktiviert")
            return
        }
        if (incognito) {
            toast("KI ist im Inkognito Modus deaktiviert")
            return
        }
        if (!AiBackend.hasApiKey(appContext)) {
            toast("Bitte zuerst einen ${AiBackend.providerDisplayName(appContext)} API Schlüssel eintragen")
            openSettings()
            return
        }
        val target = selectedOrCurrentSentenceTarget(editorInstance.activeContent)
        if (target == null || target.text.isBlank()) {
            toast("Kein Text zum Bearbeiten gefunden")
            return
        }
        if (target.text.length > 1_500) {
            toast("Bitte einen kürzeren Text oder Absatz markieren")
            return
        }
        autoJob?.cancel()
        toast("KI bearbeitet den Text …")
        scope.launch {
            try {
                val result = AiBackend.request(appContext, style, target.text, voiceLike = false)
                if (result.isBlank()) return@launch
                if (style == AiStyle.CORRECT && result.length > target.text.length * 2 + 80) {
                    throw AiException("Die KI Antwort war unplausibel lang.")
                }
                applyIfStillCurrent(target, result)
            } catch (e: AiException) {
                toast(e.message ?: "KI Fehler")
            } catch (_: Throwable) {
                toast("KI Verbindung fehlgeschlagen")
            }
        }
    }

    private fun detectVoiceLike(content: EditorContent): Boolean {
        if (previousOffset < 0 || previousOffset != content.offset) return false
        if (content.text.length <= previousText.length) return false
        var common = 0
        val max = minOf(previousText.length, content.text.length)
        while (common < max && previousText[common] == content.text[common]) common++
        val inserted = content.text.substring(common)
        return inserted.length >= 8 && inserted.any { it.isWhitespace() }
    }

    private data class Target(val start: Int, val end: Int, val text: String)

    private fun selectedOrCurrentSentenceTarget(content: EditorContent): Target? {
        if (!content.localSelection.isValid || content.offset < 0) return null
        if (content.selectedText.isNotEmpty()) {
            return Target(
                start = content.offset + content.localSelection.start,
                end = content.offset + content.localSelection.end,
                text = content.selectedText,
            )
        }
        return currentSentenceTarget(content)
    }

    private fun currentSentenceTarget(content: EditorContent): Target? {
        if (!content.localSelection.isValid || content.offset < 0) return null
        val cursor = content.localSelection.end.coerceIn(0, content.text.length)
        if (cursor <= 0) return null
        var end = cursor
        while (end > 0 && content.text[end - 1].isWhitespace()) end--
        if (end <= 0) return null
        var searchEnd = end
        if (content.text[searchEnd - 1] in charArrayOf('.', '!', '?', '…')) searchEnd--
        val prefix = content.text.substring(0, searchEnd.coerceAtLeast(0))
        val boundary = prefix.indexOfLast { it == '.' || it == '!' || it == '?' || it == '…' || it == '\n' }
        var start = boundary + 1
        while (start < end && content.text[start].isWhitespace()) start++
        if (start >= end) return null
        return Target(
            start = content.offset + start,
            end = content.offset + end,
            text = content.text.substring(start, end),
        )
    }

    private suspend fun applyIfStillCurrent(target: Target, replacement: String) {
        withContext(Dispatchers.Main) {
            val current = editorInstance.activeContent
            if (current.offset < 0) return@withContext
            val localStart = target.start - current.offset
            val localEnd = target.end - current.offset
            if (localStart < 0 || localEnd > current.text.length || localStart >= localEnd) return@withContext
            if (current.text.substring(localStart, localEnd) != target.text) return@withContext
            if (!editorInstance.setSelection(target.start, target.end)) return@withContext
            suppressUntil = SystemClock.elapsedRealtime() + 1_500L
            editorInstance.commitText(replacement)
        }
    }

    private fun toast(message: String) {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}

enum class AiStyle(val instruction: String) {
    CORRECT(" Korrigiere ausschließlich Rechtschreibung, Grammatik, Groß- und Kleinschreibung sowie Zeichensetzung. Bewahre Bedeutung, Namen, Zahlen und Wortwahl soweit möglich unverändert."),
    FRIENDLY(" Formuliere denselben Inhalt freundlich, natürlich, respektvoll und nahbar."),
    PROFESSIONAL(" Formuliere denselben Inhalt professionell, klar, sachlich und geschäftlich, ohne unnötig komplizierte Sprache."),
    CASUAL(" Formuliere denselben Inhalt locker, natürlich und alltagstauglich."),
    HUMOROUS(" Formuliere denselben Inhalt humorvoll und sympathisch. Die Aussage muss erhalten bleiben und darf nicht beleidigend werden."),
    IRONIC(" Formuliere denselben Inhalt mit klar erkennbarer leichter Ironie, ohne die Aussage zu verfälschen oder Personen herabzusetzen."),
    SHORT(" Kürze den Text deutlich, ohne wichtige Informationen zu verlieren."),
    SIMPLE(" Formuliere den Text in einfacher, leicht verständlicher Sprache mit kurzen Sätzen."),
    DIRECT(" Formuliere den Text direkt und klar, ohne unnötige Füllwörter, aber weiterhin höflich."),
}

class AiException(message: String) : Exception(message)
