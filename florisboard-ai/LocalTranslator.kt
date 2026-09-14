package dev.patrickgold.florisboard.ime.ai

import android.content.Context
import android.widget.Toast
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.editor.EditorContent
import dev.patrickgold.florisboard.lib.FlorisLocale

class LocalTranslator(private val context: Context) {
    private val appContext = context.applicationContext
    private val editorInstance by context.editorInstance()

    fun translateTo(targetLocale: FlorisLocale, sensitiveField: Boolean, rawEditor: Boolean) {
        if (sensitiveField || rawEditor) {
            toast("Übersetzung ist in diesem Eingabefeld deaktiviert")
            return
        }
        val target = selectedOrCurrentSentenceTarget(editorInstance.activeContent)
        if (target == null || target.text.isBlank()) {
            toast("Kein Text zum Übersetzen gefunden")
            return
        }
        if (target.text.length > 2_000) {
            toast("Bitte einen kürzeren Text oder Absatz markieren")
            return
        }

        val targetLanguage = TranslateLanguage.fromLanguageTag(targetLocale.languageTag())
        if (targetLanguage == null) {
            toast("Diese Zielsprache wird für die Offline Übersetzung nicht unterstützt")
            return
        }

        toast("Sprache wird erkannt …")
        val identifier = LanguageIdentification.getClient()
        identifier.identifyLanguage(target.text)
            .addOnSuccessListener { sourceTag ->
                identifier.close()
                if (sourceTag == "und") {
                    toast("Ausgangssprache konnte nicht erkannt werden")
                    return@addOnSuccessListener
                }
                val sourceLanguage = TranslateLanguage.fromLanguageTag(sourceTag)
                if (sourceLanguage == null) {
                    toast("Ausgangssprache wird nicht unterstützt")
                    return@addOnSuccessListener
                }
                if (sourceLanguage == targetLanguage) {
                    toast("Text ist bereits in ${targetLocale.displayName()}")
                    return@addOnSuccessListener
                }

                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLanguage)
                    .setTargetLanguage(targetLanguage)
                    .build()
                val translator = Translation.getClient(options)
                val conditions = DownloadConditions.Builder().build()
                toast("Übersetzungsmodell wird vorbereitet …")
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        translator.translate(target.text)
                            .addOnSuccessListener { translated ->
                                applyIfStillCurrent(target, translated)
                                translator.close()
                            }
                            .addOnFailureListener {
                                translator.close()
                                toast("Übersetzung fehlgeschlagen")
                            }
                    }
                    .addOnFailureListener {
                        translator.close()
                        toast("Sprachmodell konnte nicht geladen werden")
                    }
            }
            .addOnFailureListener {
                identifier.close()
                toast("Sprache konnte nicht erkannt werden")
            }
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

    private fun applyIfStillCurrent(target: Target, replacement: String) {
        val current = editorInstance.activeContent
        if (current.offset < 0) return
        val localStart = target.start - current.offset
        val localEnd = target.end - current.offset
        if (localStart < 0 || localEnd > current.text.length || localStart >= localEnd) return
        if (current.text.substring(localStart, localEnd) != target.text) {
            toast("Text wurde inzwischen geändert")
            return
        }
        if (!editorInstance.setSelection(target.start, target.end)) return
        editorInstance.commitText(replacement)
        toast("Übersetzt")
    }

    private fun toast(message: String) {
        Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
    }
}
