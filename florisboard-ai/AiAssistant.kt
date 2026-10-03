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
        val allEndingPunctuation = charArrayOf('.', '!', '?', '…')
        val expressivePunctuation = charArrayOf('!', '?', '…')
        val originalTrimmed = original.trimEnd()
        val correctedTrimmed = corrected.trimEnd()
        if (correctedTrimmed.isBlank()) return corrected

        // Bewusst gesetzte emotionale Satzenden bleiben exakt erhalten.
        // Ein normaler Punkt darf ergänzt werden, wenn eine schnelle Eingabe
        // oder ein Diktat ohne Satzzeichen endet.
        val expressiveEnding = when {
            originalTrimmed.endsWith("...") -> "..."
            else -> originalTrimmed.takeLastWhile { it in expressivePunctuation }
        }
        if (expressiveEnding.isEmpty()) return correctedTrimmed

        val correctedBase = correctedTrimmed.dropLastWhile { it in allEndingPunctuation }.trimEnd()
        return correctedBase + expressiveEnding
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
    CORRECT(
        "Prüfe den gesamten Text als zusammenhängende Aussage und nicht Wort für Wort. " +
            "Ermittle intern zuerst, was die Person tatsächlich sagen will, an wen sich der Text richtet und welche Informationen zusammengehören. " +
            "Prüfe danach jeden vollständigen Satz im Zusammenhang mit den Sätzen davor und danach auf Sinn, Logik, Grammatik, Wortwahl, Satzbau, Bezüge, Zeitform und Zeichensetzung. " +
            "Wenn ein Satz zwar einzelne richtige Wörter enthält, aber unnatürlich, missverständlich oder durch Diktat verdreht ist, formuliere den ganzen Satz neu. " +
            "Korrigiere offensichtliche Spracherkennungsfehler anhand des Zusammenhangs. " +
            "Erhalte alle sicher erkennbaren Fakten, Namen, Zahlen, Termine, Forderungen, Fragen, Anreden und die beabsichtigte Wirkung. " +
            "Erfinde keine Informationen und ändere keine Aussage nur, um den Text schöner wirken zu lassen. " +
            "Das Ergebnis soll sich lesen, als hätte ein aufmerksamer Mensch den Text selbst sauber formuliert. Gib ausschließlich den fertigen Text aus."
    ),
    FRIENDLY(
        "Überarbeite zuerst Sinn, Grammatik und Satzbau vollständig. Formuliere danach denselben Inhalt freundlich, warm und respektvoll. " +
            "Freundlichkeit soll durch natürliche Wortwahl und einen angenehmen Ton entstehen, nicht durch übertriebene Höflichkeitsfloskeln. " +
            "Lass Bitten und Aussagen klar. Bewahre Fakten, Grenzen und gewünschte Handlungen. Gib ausschließlich den fertigen Text aus."
    ),
    PROFESSIONAL(
        "Überarbeite zuerst Sinn, Grammatik und Satzbau vollständig. Formuliere danach professionell, klar, souverän und präzise. " +
            "Ordne Gedanken in einer nachvollziehbaren Reihenfolge und formuliere auch ganze Sätze neu, wenn sie holprig oder unklar sind. " +
            "Nutze natürliche berufliche Sprache statt Amtsdeutsch, Werbesprache oder typischer KI Formulierungen. " +
            "Fakten, Namen, Zahlen, Fristen, Zuständigkeiten und Absichten bleiben unverändert. Gib ausschließlich den fertigen Text aus."
    ),
    CASUAL(
        "Überarbeite zuerst Sinn, Grammatik und Satzbau vollständig. Formuliere danach locker, spontan und natürlich wie in einer echten Alltagsnachricht. " +
            "Der Text darf unkompliziert klingen, soll aber verständlich bleiben. Keine künstliche Jugendsprache, keine aufgesetzte Coolness und keine generischen KI Floskeln. " +
            "Inhalt, Persönlichkeit und Absicht bleiben erhalten. Gib ausschließlich den fertigen Text aus."
    ),
    HUMOROUS(
        "Überarbeite zuerst Sinn, Grammatik und Satzbau vollständig. Formuliere dann deutlich humorvoller, ohne den eigentlichen Inhalt zu verlieren. " +
            "Nutze den vorhandenen Kontext für Wortwitz, überraschende Formulierungen, trockene Pointen oder leichte Übertreibung. " +
            "Der Humor soll zur Situation passen und wie spontan von einem Menschen wirken. Erfinde keine Tatsachen und erkläre keinen Witz. " +
            "Wichtige Informationen und das Anliegen müssen weiterhin eindeutig verständlich sein. Gib ausschließlich den fertigen Text aus."
    ),
    IRONIC(
        "Überarbeite zuerst Sinn, Grammatik und Satzbau vollständig. Formuliere danach trocken, deutlich sarkastisch und erkennbar ironisch. " +
            "Nutze den tatsächlichen Kontext für die Spitze. Der Text darf bissig sein, aber nicht beleidigend, entwürdigend oder bedrohend. " +
            "Keine erfundenen Behauptungen und keine Erklärung der Ironie. Die eigentliche Aussage und alle Fakten bleiben erhalten. Gib ausschließlich den fertigen Text aus."
    ),
    FLIRTY(
        "Überarbeite zuerst Sinn, Grammatik und Satzbau vollständig. Formuliere dann charmant, spielerisch, selbstbewusst und eindeutig flirtend. " +
            "Baue natürliche Leichtigkeit, Interesse und Spannung auf, ohne kitschig, plump, bedürftig, drängend oder manipulativ zu wirken. " +
            "Der Text soll wie eine echte persönliche Nachricht klingen. Erfinde keine gemeinsamen Erlebnisse, Gefühle oder Zusagen. Gib ausschließlich den fertigen Text aus."
    ),
    SUGGESTIVE(
        "Überarbeite zuerst Sinn, Grammatik und Satzbau vollständig. Formuliere den Text für erwachsene einvernehmliche Kommunikation verführerisch, selbstbewusst und deutlich zweideutig. " +
            "Spannung und sexuelles Interesse dürfen klar erkennbar sein, aber ohne grafische sexuelle Beschreibungen, Druck, Drohung, Manipulation oder unterstellte Zustimmung. " +
            "Nutze Andeutungen und natürliche Sprache statt plumper Formulierungen. Gib ausschließlich den fertigen Text aus."
    ),
    ELEGANT(
        "Überarbeite zuerst Sinn, Grammatik und Satzbau vollständig. Formuliere danach stilvoll, souverän und sprachlich hochwertig, aber weiterhin glaubwürdig und menschlich. " +
            "Verbessere Rhythmus und Wortwahl, ohne den Text mit Fremdwörtern, Pathos oder unnötig komplizierten Sätzen aufzublähen. " +
            "Aussage, Fakten und Persönlichkeit bleiben erhalten. Gib ausschließlich den fertigen Text aus."
    ),
    BUSINESS(
        "Überarbeite zuerst Sinn, Grammatik und Satzbau vollständig. Formuliere den Inhalt danach als klare geschäftliche Nachricht. " +
            "Das eigentliche Anliegen, die gewünschte Handlung, Zuständigkeiten, Termine, Zahlen und offene Punkte sollen schnell erfassbar sein. " +
            "Ordne den Text sinnvoll und formuliere verbindlich, professionell und menschlich, nicht bürokratisch oder künstlich. " +
            "Keine Fakten ergänzen oder weglassen. Gib ausschließlich den fertigen Text aus."
    ),
    PERSONAL(
        "Überarbeite zuerst Sinn, Grammatik und Satzbau vollständig. Formuliere danach persönlich, authentisch und nahbar. " +
            "Erhalte erkennbare Gefühle, individuelle Wortwahl und die Beziehung zum Empfänger. Glätte den Text nicht so stark, dass er austauschbar klingt. " +
            "Entferne generische Wohlfühlfloskeln und formuliere lieber konkrete, natürliche Sätze. Erfinde keine persönlichen Details oder Gefühle. Gib ausschließlich den fertigen Text aus."
    ),
    DU(
        "Prüfe den vollständigen Text auf Sinn und sprachliche Fehler. Ändere die Ansprache anschließend konsequent in eine natürliche Du Form. " +
            "Passe Pronomen, Anrede, Verbformen und notwendigen Satzbau an. Inhalt, Fakten, Ton und Aussage dürfen sich ansonsten nicht verändern. Gib ausschließlich den fertigen Text aus."
    ),
    SIE(
        "Prüfe den vollständigen Text auf Sinn und sprachliche Fehler. Ändere die Ansprache anschließend konsequent in eine natürliche höfliche Sie Form. " +
            "Passe Pronomen, Anrede, Verbformen und notwendigen Satzbau an. Inhalt, Fakten, Ton und Aussage dürfen sich ansonsten nicht verändern. Gib ausschließlich den fertigen Text aus."
    ),
    SHORT(
        "Prüfe zuerst Sinn, Grammatik und Satzbau. Kürze den Text danach deutlich, ohne die Aussage zu beschädigen. " +
            "Streiche Wiederholungen, Füllwörter und Nebensächlichkeiten und fasse zusammengehörige Aussagen natürlich zusammen. " +
            "Namen, Zahlen, Termine, Bedingungen, Entscheidungen, Fragen und Handlungsaufforderungen müssen vollständig erhalten bleiben. Gib ausschließlich den fertigen Text aus."
    ),
    SIMPLE(
        "Prüfe zuerst den vollständigen Sinn des Textes. Formuliere danach in sehr klarer, natürlicher Alltagssprache. " +
            "Nutze kurze vollständige Sätze, bekannte Wörter und eine eindeutige Reihenfolge. Löse komplizierte Satzkonstruktionen auf und erkläre schwierige Formulierungen einfacher. " +
            "Lass keine wichtige Information weg und erfinde nichts. Der Text soll leicht verständlich sein, aber nicht kindlich wirken. Gib ausschließlich den fertigen Text aus."
    ),
    DIRECT(
        "Prüfe zuerst Sinn, Grammatik und Satzbau. Formuliere danach wesentlich direkter und klarer. " +
            "Beginne mit dem eigentlichen Anliegen, entferne Umwege, Ausreden, Wiederholungen und unnötige Einleitungen. " +
            "Bleibe respektvoll und eindeutig. Fakten, Bedingungen, Grenzen und gewünschte Handlungen müssen vollständig erhalten bleiben. Gib ausschließlich den fertigen Text aus."
    ),
    PROMPT(
        "Der Eingabetext ist ein Rohentwurf für einen KI Prompt. Lies ihn vollständig und ermittle intern Ziel, Kontext, gewünschtes Ergebnis, wichtige Einschränkungen und gewünschtes Ausgabeformat. " +
            "Formuliere daraus einen direkt nutzbaren, klar gegliederten Arbeitsauftrag. Löse Widersprüche auf, wenn die beabsichtigte Richtung eindeutig ist, und entferne unnötige Wiederholungen. " +
            "Erfinde niemals Fakten, Namen, Daten oder Anforderungen. Wenn eine entscheidende Information fehlt und nicht sicher ableitbar ist, formuliere eine klare Platzhalterstelle oder eine Anweisung an die ausführende KI, diese Information zu klären. " +
            "Gib ausschließlich den verbesserten Prompt aus."
    ),
}

class AiException(message: String) : Exception(message)
