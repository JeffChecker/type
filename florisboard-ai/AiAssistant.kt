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

        val target = selectedOrBestTarget(editorInstance.activeContent)
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

    private fun selectedOrBestTarget(content: EditorContent): Target? {
        if (!content.localSelection.isValid || content.offset < 0) return null

        val localStart = minOf(content.localSelection.start, content.localSelection.end)
            .coerceIn(0, content.text.length)
        val localEnd = maxOf(content.localSelection.start, content.localSelection.end)
            .coerceIn(0, content.text.length)

        if (localEnd > localStart) {
            val selected = content.text.substring(localStart, localEnd)
            if (selected.isNotBlank()) {
                return Target(
                    start = content.offset + localStart,
                    end = content.offset + localEnd,
                    text = selected,
                )
            }
        }

        // 1. Bevorzugt den ganzen aktuellen Absatz, damit die KI den Sinn versteht.
        val paragraph = currentParagraphTarget(content)
        if (paragraph != null && paragraph.text.length <= 2_500) return paragraph

        // 2. Fallback für sehr lange oder ungewöhnlich gelieferte Editor-Inhalte.
        currentSentenceTarget(content)?.let {
            if (it.text.isNotBlank() && it.text.length <= 2_500) return it
        }

        // 3. Letzter Fallback: sinnvoller Textblock direkt vor dem Cursor.
        return textBeforeCursorTarget(content) ?: paragraph
    }

    private fun currentParagraphTarget(content: EditorContent): Target? {
        if (!content.localSelection.isValid || content.offset < 0) return null
        val cursor = content.localSelection.end.coerceIn(0, content.text.length)
        var start = if (cursor > 0) content.text.lastIndexOf('\n', cursor - 1) + 1 else 0
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
        if (!content.localSelection.isValid || content.offset < 0 || content.text.isEmpty()) return null
        val text = content.text
        val cursor = content.localSelection.end.coerceIn(0, text.length)
        val punctuation = charArrayOf('.', '!', '?', '…')

        var scan = (cursor - 1).coerceAtLeast(0)
        while (scan >= 0 && text[scan].isWhitespace()) scan--
        while (scan >= 0 && text[scan] in punctuation) scan--
        while (scan >= 0 && text[scan] !in punctuation && text[scan] != '\n') scan--
        var start = scan + 1

        var end = cursor
        while (end < text.length && text[end] !in punctuation && text[end] != '\n') end++
        while (end < text.length && text[end] in punctuation) end++

        while (start < end && text[start].isWhitespace()) start++
        while (end > start && text[end - 1].isWhitespace()) end--
        if (start >= end) return null

        return Target(
            start = content.offset + start,
            end = content.offset + end,
            text = text.substring(start, end),
        )
    }

    private fun textBeforeCursorTarget(content: EditorContent): Target? {
        if (!content.localSelection.isValid || content.offset < 0) return null
        val cursor = content.localSelection.end.coerceIn(0, content.text.length)
        if (cursor <= 0) return null

        var start = (cursor - 1_500).coerceAtLeast(0)
        val lastBreak = content.text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
        if (lastBreak >= start) start = lastBreak + 1
        var end = cursor
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
            if (current.offset < 0) {
                toast("Textfeld ist nicht mehr verfügbar")
                return@withContext
            }

            val expectedStart = target.start - current.offset
            val expectedEnd = target.end - current.offset

            var localStart = expectedStart
            var localEnd = expectedEnd
            val exactRangeStillMatches =
                localStart >= 0 && localEnd <= current.text.length && localStart < localEnd &&
                    current.text.substring(localStart, localEnd) == target.text

            if (!exactRangeStillMatches) {
                val hits = mutableListOf<Int>()
                var from = 0
                while (from <= current.text.length - target.text.length) {
                    val hit = current.text.indexOf(target.text, from)
                    if (hit < 0) break
                    hits += hit
                    from = hit + 1
                }

                val relocated = when {
                    hits.size == 1 -> hits.first()
                    hits.isNotEmpty() -> hits.minByOrNull { kotlin.math.abs(it - expectedStart) }
                    else -> null
                }

                if (relocated == null ||
                    (hits.size > 1 && kotlin.math.abs(relocated - expectedStart) > 300)
                ) {
                    toast("Der Text wurde inzwischen verändert. Bitte KI korrigieren erneut drücken.")
                    return@withContext
                }

                localStart = relocated
                localEnd = relocated + target.text.length
            }

            val absoluteStart = current.offset + localStart
            val absoluteEnd = current.offset + localEnd
            if (!editorInstance.setSelection(absoluteStart, absoluteEnd)) {
                toast("Text konnte in dieser App nicht ausgewählt werden. Bitte Text markieren und erneut versuchen.")
                return@withContext
            }

            suppressUntil = SystemClock.elapsedRealtime() + 1_500L
            if (!editorInstance.commitText(replacement)) {
                toast("Korrektur konnte in diesem Textfeld nicht eingesetzt werden")
            }
        }
    }

    private fun toast(message: String) {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}

enum class AiStyle(val instruction: String) {
    CORRECT("Lies den gesamten Eingabetext zuerst vollständig und bestimme seine beabsichtigte Aussage. Korrigiere dann nur, was tatsächlich fehlerhaft oder durch Diktat offensichtlich falsch erkannt wurde: Rechtschreibung, Grammatik, Groß und Kleinschreibung, Wortwahl im eindeutigen Kontext und Zeichensetzung. Formuliere unklare Stellen nur dann um, wenn die beabsichtigte Aussage sicher erkennbar ist. Behalte Namen, Zahlen, Termine, Fachbegriffe, Anrede, Ton und persönliche Wortwahl bei. Erfinde nichts. Entferne keine wichtigen Informationen. Ein vorhandenes abschließendes !, ?, ?!, !! oder … muss exakt erhalten bleiben. Hat der Nutzer am Ende noch kein Satzzeichen gesetzt, füge keines hinzu. Gib ausschließlich den korrigierten Text aus."),
    FRIENDLY("Formuliere denselben Inhalt spürbar freundlicher, warm und respektvoll, aber nicht überschwänglich. Vermeide Floskeln, künstliche Herzlichkeit und übertriebene Höflichkeit. Die Nachricht soll wie von einer echten Person wirken. Alle Fakten, Wünsche und Aussagen bleiben vollständig erhalten. Gib nur den fertigen Text aus."),
    PROFESSIONAL("Formuliere den Inhalt professionell, klar, souverän und präzise. Nutze natürliche Geschäftssprache statt Amtsdeutsch oder KI Floskeln. Ordne Gedanken sinnvoll, beseitige Unklarheiten und lasse alle Fakten, Namen, Zahlen, Fristen und Absichten unverändert. Gib nur den fertigen Text aus."),
    CASUAL("Formuliere denselben Inhalt locker, direkt und natürlich, wie in einer echten Alltagsnachricht. Keine künstliche Jugendsprache, keine übertriebene Coolness und keine KI Floskeln. Inhalt und Absicht vollständig erhalten. Gib nur den fertigen Text aus."),
    HUMOROUS("Formuliere denselben Inhalt deutlich humorvoller und pointierter. Der Humor soll aus Situation und Wortwahl entstehen, nicht aus erfundenen Fakten. Keine Witze erklären. Nicht albern, verletzend oder künstlich wirken. Kernaussage und wichtige Informationen bleiben erhalten. Gib nur den fertigen Text aus."),
    IRONIC("Formuliere denselben Inhalt klar erkennbar sarkastisch und trocken ironisch. Die Spitze darf deutlich sein, soll aber nicht beleidigen oder entmenschlichen. Keine Erklärung des Sarkasmus und keine erfundenen Behauptungen. Fakten und eigentliche Aussage bleiben vollständig erhalten. Gib nur den fertigen Text aus."),
    FLIRTY("Formuliere den Inhalt charmant, spielerisch, selbstbewusst und eindeutig flirtend. Zeige echtes Interesse und leichte Spannung, ohne kitschig, plump, drängend oder manipulativ zu wirken. Keine expliziten sexuellen Beschreibungen. Vorhandene Fakten und Absichten erhalten. Gib nur den fertigen Text aus."),
    SUGGESTIVE("Formuliere den Inhalt für erwachsene einvernehmliche Kommunikation verführerisch, selbstbewusst und deutlich zweideutig. Sexuelles Interesse darf klar erkennbar sein, aber ohne grafische sexuelle Beschreibungen, Druck, Drohung, Manipulation oder unterstellte Zustimmung. Der Ton soll natürlich und respektvoll bleiben. Gib nur den fertigen Text aus."),
    ELEGANT("Formuliere denselben Inhalt stilvoll, souverän und sprachlich hochwertig, aber weiterhin natürlich. Vermeide gestelzte Fremdwörter, Pathos, Floskeln und übertriebene Eleganz. Aussage, Fakten und Persönlichkeit des Ausgangstextes bleiben erhalten. Gib nur den fertigen Text aus."),
    BUSINESS("Formuliere den Inhalt als klare geschäftliche Nachricht. Das Ziel, gewünschte Handlung, Verantwortlichkeit, Termine, Zahlen und offene Punkte müssen sofort verständlich sein. Schreibe verbindlich und professionell, aber nicht bürokratisch. Keine Fakten ergänzen oder weglassen. Gib nur den fertigen Text aus."),
    PERSONAL("Formuliere den Inhalt persönlich, authentisch und nahbar, als hätte der Absender ihn selbst bewusst geschrieben. Behalte individuelle Wortwahl und Emotionen soweit möglich. Keine generischen Wohlfühlfloskeln und keine erfundenen persönlichen Details. Gib nur den fertigen Text aus."),
    DU("Ändere ausschließlich die Ansprache konsequent in eine natürliche Du Form. Passe Pronomen, Anrede und notwendigen Satzbau an. Inhalt, Ton, Fakten und Aussage dürfen sich sonst nicht verändern. Gib nur den fertigen Text aus."),
    SIE("Ändere ausschließlich die Ansprache konsequent in eine höfliche, natürliche Sie Form. Passe Pronomen, Anrede und notwendigen Satzbau an. Inhalt, Ton, Fakten und Aussage dürfen sich sonst nicht verändern. Gib nur den fertigen Text aus."),
    SHORT("Kürze den Text deutlich und entferne Wiederholungen, Füllwörter und Nebensächlichkeiten. Ziel ist ungefähr ein Drittel weniger Text, sofern das ohne Informationsverlust möglich ist. Namen, Zahlen, Termine, Forderungen, Entscheidungen und Handlungsaufforderungen müssen erhalten bleiben. Gib nur den gekürzten Text aus."),
    SIMPLE("Formuliere den vollständigen Inhalt in sehr klarer Alltagssprache. Nutze kurze Sätze, bekannte Wörter und eine eindeutige Reihenfolge. Erkläre schwierige Formulierungen einfacher, ohne wichtige Informationen zu streichen oder neue Fakten hinzuzufügen. Gib nur den fertigen Text aus."),
    DIRECT("Formuliere die Aussage wesentlich direkter und klarer. Beginne mit dem eigentlichen Anliegen, entferne Umwege, Füllwörter und unnötige Einleitungen. Bleibe angemessen respektvoll. Fakten, Bedingungen und Absicht müssen vollständig erhalten bleiben. Gib nur den fertigen Text aus."),
    PROMPT("Der Eingabetext ist ein Rohentwurf für einen KI Prompt. Ermittle zuerst das konkrete Ziel. Formuliere daraus einen präzisen Arbeitsauftrag mit relevantem Kontext, klaren Anforderungen, Grenzen und gewünschtem Ausgabeformat, soweit diese Angaben vorhanden oder eindeutig ableitbar sind. Entferne Widersprüche und unnötige Wiederholungen. Erfinde niemals Fakten, Namen, Daten oder Anforderungen. Fehlen entscheidende Angaben, formuliere sinnvolle Platzhalter oder weise im Prompt darauf hin, was die ausführende KI selbst klären soll. Der verbesserte Prompt muss direkt verwendbar sein. Gib ausschließlich den verbesserten Prompt aus."),
}

class AiException(message: String) : Exception(message)
