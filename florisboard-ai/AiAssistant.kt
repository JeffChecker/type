package dev.patrickgold.florisboard.ime.ai

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.Toast
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.editor.EditorContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** KI Schreibassistent für FlorisBoard. */
class AiAssistant(private val context: Context) {
    companion object {
        const val PREFS_NAME = "floris_ai"
        const val KEY_API_KEY = AiBackend.KEY_GROQ_API_KEY
        const val KEY_MODEL = AiBackend.LEGACY_KEY_MODEL
        const val DEFAULT_MODEL = AiBackend.AUTO_MODEL

        suspend fun testConnection(provider: AiProvider, apiKey: String, model: String): String =
            AiBackend.testConnection(provider, apiKey, model)
    }

    private val appContext = context.applicationContext
    private val editorInstance by context.editorInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var suppressUntil: Long = 0L

    fun openSettings() {
        val intent = Intent(appContext, AiSettingsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(intent)
    }

    /**
     * Bewusst leer: Es gibt keine zeitgesteuerte Autokorrektur mehr.
     * Text wird nur nach Betätigung einer Smartbar-Aktion verändert.
     */
    fun onContentChanged(
        content: EditorContent,
        sensitiveField: Boolean,
        incognito: Boolean,
        rawEditor: Boolean,
    ) {
        // Kein Timer und keine automatische Änderung während des Schreibens.
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

        val statusText = when (style) {
            AiStyle.CORRECT -> "KI prüft Sinn, Sprache und Zeichensetzung …"
            AiStyle.PROMPT -> "KI verbessert den Prompt …"
            else -> "KI setzt den gewählten Schreibstil um …"
        }
        toast(statusText)

        scope.launch {
            try {
                val resultRaw = AiBackend.request(appContext, style, target.text, voiceLike = false)
                if (resultRaw.isBlank()) return@launch

                val result = if (style == AiStyle.CORRECT) {
                    preserveUserEnding(target.text, resultRaw)
                } else {
                    resultRaw
                }

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

    /**
     * Bewahrt die vom Nutzer gesetzte Satzendabsicht.
     * !, ?, ?!, !!, … usw. bleiben exakt erhalten. Hat der Nutzer noch kein
     * Satzzeichen gesetzt, fügt die KI am Ende keines ungefragt hinzu.
     */
    private fun preserveUserEnding(original: String, corrected: String): String {
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
    CORRECT(" Erfasse zuerst den beabsichtigten Sinn des gesamten Textes. Korrigiere Rechtschreibung, Grammatik, Groß- und Kleinschreibung, Zeichensetzung und offensichtliche Diktat- oder Worterkennungsfehler anhand des vollständigen Kontexts. Formuliere unklare Stellen nur dann verständlicher, wenn die beabsichtigte Aussage eindeutig erkennbar ist. Verändere keine Fakten, Namen, Zahlen, Anrede oder Tonlage. Erfinde keine Informationen. Bewahre ein vorhandenes abschließendes !, ?, ?!, !! oder … exakt und füge am Ende kein Satzzeichen hinzu, wenn der Nutzer noch keines gesetzt hat."),
    FRIENDLY(" Erfasse den vollständigen Inhalt und formuliere ihn deutlich freundlicher, natürlicher, respektvoll und nahbar. Alle wichtigen Aussagen und Fakten müssen erhalten bleiben."),
    PROFESSIONAL(" Erfasse den vollständigen Inhalt und formuliere ihn klar, professionell, sachlich und gut strukturiert. Korrigiere dabei unklare Formulierungen, ohne Fakten oder Absichten zu verändern."),
    CASUAL(" Erfasse den vollständigen Inhalt und formuliere ihn deutlich lockerer, natürlicher und alltagstauglich. Die Kernaussage muss vollständig erhalten bleiben."),
    HUMOROUS(" Erfasse den vollständigen Inhalt und formuliere ihn erkennbar humorvoll, pointiert und sympathisch. Der Witz darf deutlicher sein, aber Fakten und Kernaussage dürfen nicht erfunden oder verfälscht werden."),
    IRONIC(" Erfasse den vollständigen Inhalt und formuliere ihn klar erkennbar sarkastisch und ironisch, pointiert und trocken, aber nicht beleidigend. Die eigentliche Aussage und alle Fakten müssen erhalten bleiben."),
    FLIRTY(" Erfasse den vollständigen Inhalt und formuliere ihn charmant, spielerisch und eindeutig flirtend. Die Aussage soll selbstbewusst und sympathisch wirken, ohne Druck, Manipulation oder explizite sexuelle Beschreibungen."),
    SUGGESTIVE(" Erfasse den vollständigen Inhalt und formuliere ihn für erwachsene, einvernehmliche Kommunikation verführerisch, zweideutig und mit klar erkennbarem sexuellem Interesse, aber nicht grafisch oder pornografisch. Kein Druck, keine Drohung, keine Manipulation und keine Annahme von Zustimmung."),
    ELEGANT(" Erfasse den vollständigen Inhalt und formuliere ihn stilvoll, elegant, sprachlich hochwertig und natürlich. Nicht gestelzt und keine Fakten verändern."),
    BUSINESS(" Erfasse den vollständigen Inhalt und formuliere ihn geschäftlich, verbindlich, klar und professionell. Wichtige Termine, Zahlen, Namen, Forderungen und Handlungsaufträge müssen vollständig erhalten bleiben."),
    PERSONAL(" Erfasse den vollständigen Inhalt und formuliere ihn persönlich, warm und authentisch, als käme er direkt vom Absender. Keine erfundenen persönlichen Details hinzufügen."),
    DU(" Behalte den vollständigen Inhalt bei und formuliere konsequent in direkter Du-Anrede. Passe Pronomen, Anrede und Satzbau natürlich an, ohne Fakten zu verändern."),
    SIE(" Behalte den vollständigen Inhalt bei und formuliere konsequent in höflicher Sie-Anrede. Passe Pronomen, Anrede und Satzbau natürlich an, ohne Fakten zu verändern."),
    SHORT(" Erfasse zuerst die Kernaussage und kürze den Text deutlich. Alle wichtigen Informationen, Namen, Zahlen und Handlungsaufforderungen müssen erhalten bleiben."),
    SIMPLE(" Erfasse den vollständigen Inhalt und formuliere ihn in sehr einfacher, leicht verständlicher Sprache mit kurzen, klaren Sätzen. Keine wichtige Information weglassen."),
    DIRECT(" Erfasse die Kernaussage und formuliere sie deutlich direkter, klarer und ohne unnötige Füllwörter. Fakten und Absicht vollständig erhalten und weiterhin angemessen höflich bleiben."),
    PROMPT(" Verwandle den Rohtext in einen klaren, wirksamen Prompt für eine KI. Erfasse zuerst das eigentliche Ziel des Nutzers. Strukturiere den Prompt sinnvoll mit Aufgabe, relevantem Kontext, gewünschtem Ergebnis, wichtigen Vorgaben und gewünschtem Stil oder Ausgabeformat, soweit diese Informationen im Ausgangstext vorhanden oder eindeutig ableitbar sind. Erfinde keine Fakten, Namen, Daten, Anforderungen oder Einschränkungen. Wenn Informationen fehlen, formuliere den Prompt so, dass die KI vernünftig damit umgehen kann, ohne Dinge zu erfinden. Gib ausschließlich den verbesserten Prompt aus."),
}

class AiException(message: String) : Exception(message)
