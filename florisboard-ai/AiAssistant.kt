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
        const val KEY_AUTO_DELAY_MS = "auto_delay_ms"
        const val DEFAULT_AUTO_DELAY_MS = 5_000L
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
        val baseDelay = prefs.getLong(KEY_AUTO_DELAY_MS, DEFAULT_AUTO_DELAY_MS).coerceIn(2_500L, 10_000L)
        val waitMs = when {
            voiceLike -> (baseDelay + 1_000L).coerceAtMost(10_000L)
            endsSentence -> baseDelay
            else -> (baseDelay + 1_500L).coerceAtMost(10_000L)
        }
        autoJob = scope.launch {
            delay(waitMs)
            val correctedRaw = try {
                AiBackend.request(appContext, AiStyle.CORRECT, target.text, voiceLike)
            } catch (_: Throwable) {
                return@launch
            }
            val corrected = preserveAutomaticEnding(target.text, correctedRaw)
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
        val target = selectedOrCurrentParagraphTarget(editorInstance.activeContent)
        if (target == null || target.text.isBlank()) {
            toast("Kein Text zum Bearbeiten gefunden")
            return
        }
        if (target.text.length > 2_500) {
            toast("Bitte einen kürzeren Text oder Absatz markieren")
            return
        }
        autoJob?.cancel()
        toast("KI versteht und bearbeitet den Text …")
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

    private fun selectedOrCurrentParagraphTarget(content: EditorContent): Target? {
        if (!content.localSelection.isValid || content.offset < 0) return null
        if (content.selectedText.isNotEmpty()) {
            return Target(
                start = content.offset + content.localSelection.start,
                end = content.offset + content.localSelection.end,
                text = content.selectedText,
            )
        }
        return currentParagraphTarget(content)
    }

    private fun currentParagraphTarget(content: EditorContent): Target? {
        if (!content.localSelection.isValid || content.offset < 0) return null
        val cursor = content.localSelection.end.coerceIn(0, content.text.length)
        var start = content.text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)) + 1
        var end = content.text.indexOf('\n', cursor)
        if (end < 0) end = content.text.length
        while (start < end && content.text[start].isWhitespace()) start++
        while (end > start && content.text[end - 1].isWhitespace()) end--
        if (start >= end) return null
        return Target(
            start = content.offset + start,
            end = content.offset + end,
            text = content.text.substring(start, end),
        )
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

    /**
     * Automatische Korrektur darf die Absicht des Nutzers am Satzende nicht verändern.
     * Ein vorhandenes !, ?, ?!, !! usw. bleibt exakt erhalten. Hat der Nutzer noch kein
     * Satzzeichen gesetzt, fügt die KI auch keines ungefragt hinzu.
     */
    private fun preserveAutomaticEnding(original: String, corrected: String): String {
        val punctuation = charArrayOf('.', '!', '?', '…')
        val originalTrimmed = original.trimEnd()
        val correctedTrimmed = corrected.trimEnd()
        if (correctedTrimmed.isBlank()) return corrected

        val originalEnding = originalTrimmed.takeLastWhile { it in punctuation }
        val correctedBase = correctedTrimmed.dropLastWhile { it in punctuation }.trimEnd()

        return if (originalEnding.isNotEmpty()) {
            correctedBase + originalEnding
        } else {
            correctedBase
        }
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
    CORRECT(" Erfasse zuerst den beabsichtigten Sinn des Textes. Korrigiere Rechtschreibung, Grammatik, Groß- und Kleinschreibung, Zeichensetzung und offensichtliche Diktat- oder Worterkennungsfehler anhand des Satzkontexts. Verändere keine Aussage, Namen, Zahlen, Anrede oder Tonlage. Erfinde keine Informationen. Bewahre ein vorhandenes abschließendes !, ?, ?!, !! oder … exakt und füge am Ende kein Satzzeichen hinzu, wenn der Nutzer noch keines gesetzt hat."),
    FRIENDLY(" Erfasse den vollständigen Inhalt und formuliere ihn deutlich freundlicher, natürlicher, respektvoll und nahbar. Alle wichtigen Aussagen und Fakten müssen erhalten bleiben."),
    PROFESSIONAL(" Erfasse den vollständigen Inhalt und formuliere ihn klar, professionell, sachlich und gut strukturiert. Korrigiere dabei unklare Formulierungen, ohne Fakten oder Absichten zu verändern."),
    CASUAL(" Erfasse den vollständigen Inhalt und formuliere ihn deutlich lockerer, natürlicher und alltagstauglich. Die Kernaussage muss vollständig erhalten bleiben."),
    HUMOROUS(" Erfasse den vollständigen Inhalt und formuliere ihn erkennbar humorvoll, pointiert und sympathisch. Der Witz darf deutlicher sein, aber Fakten und Kernaussage dürfen nicht erfunden oder verfälscht werden."),
    IRONIC(" Erfasse den vollständigen Inhalt und formuliere ihn klar erkennbar sarkastisch und ironisch, pointiert und trocken, aber nicht beleidigend. Die eigentliche Aussage und alle Fakten müssen erhalten bleiben."),
    SHORT(" Erfasse zuerst die Kernaussage und kürze den Text deutlich. Alle wichtigen Informationen, Namen, Zahlen und Handlungsaufforderungen müssen erhalten bleiben."),
    SIMPLE(" Erfasse den vollständigen Inhalt und formuliere ihn in sehr einfacher, leicht verständlicher Sprache mit kurzen, klaren Sätzen. Keine wichtige Information weglassen."),
    DIRECT(" Erfasse die Kernaussage und formuliere sie deutlich direkter, klarer und ohne unnötige Füllwörter. Fakten und Absicht vollständig erhalten und weiterhin angemessen höflich bleiben."),
}

class AiException(message: String) : Exception(message)
