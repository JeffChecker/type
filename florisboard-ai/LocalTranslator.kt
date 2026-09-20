package dev.patrickgold.florisboard.ime.ai

import android.content.Context
import android.widget.Toast
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.editor.EditorContent
import dev.patrickgold.florisboard.lib.FlorisLocale
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Hybride Übersetzung:
 * - Google Cloud Translation für höhere Qualität und größere Sprachauswahl.
 * - ML Kit lokal als Offline- und Datenschutz-Fallback.
 */
class LocalTranslator(private val context: Context) {
    private val appContext = context.applicationContext
    private val editorInstance by context.editorInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun translateTo(
        targetLocale: FlorisLocale,
        sensitiveField: Boolean,
        incognito: Boolean,
        rawEditor: Boolean,
    ) {
        if (sensitiveField || rawEditor) {
            toast("Übersetzung ist in diesem Eingabefeld deaktiviert")
            return
        }

        val target = selectedOrBestTarget(editorInstance.activeContent)
        if (target == null || target.text.isBlank()) {
            toast("Kein Text zum Übersetzen gefunden")
            return
        }
        if (target.text.length > 5_000) {
            toast("Bitte einen kürzeren Text oder Absatz markieren")
            return
        }

        val setting = TranslationBackend.targetSetting(appContext)
        val targetCode = if (setting == TranslationBackend.TARGET_ACTIVE_KEYBOARD) {
            TranslationBackend.normalizeKeyboardLanguageTag(targetLocale.languageTag())
        } else {
            setting
        }
        val targetName = if (setting == TranslationBackend.TARGET_ACTIVE_KEYBOARD) {
            targetLocale.displayName()
        } else {
            Locale.forLanguageTag(targetCode).getDisplayLanguage(Locale.GERMAN)
                .ifBlank { targetCode }
        }

        val mode = TranslationBackend.mode(appContext)
        val cloudKey = TranslationBackend.apiKey(appContext)

        when {
            incognito -> {
                toast("Inkognito: Übersetzung bleibt auf dem Gerät")
                translateLocal(target, targetCode, targetName)
            }
            mode == TranslationMode.LOCAL -> translateLocal(target, targetCode, targetName)
            mode == TranslationMode.CLOUD -> {
                if (cloudKey.isBlank()) {
                    toast("Bitte zuerst einen Google Cloud Translation API Schlüssel eintragen")
                } else {
                    translateCloud(target, targetCode, targetName, cloudKey, allowLocalFallback = false)
                }
            }
            cloudKey.isNotBlank() -> {
                translateCloud(target, targetCode, targetName, cloudKey, allowLocalFallback = true)
            }
            else -> translateLocal(target, targetCode, targetName)
        }
    }

    private fun translateCloud(
        target: Target,
        targetCode: String,
        targetName: String,
        apiKey: String,
        allowLocalFallback: Boolean,
    ) {
        toast("Übersetze mit Google Translate nach $targetName …")
        scope.launch {
            try {
                val result = TranslationBackend.translateCloud(
                    apiKey = apiKey,
                    text = target.text,
                    targetLanguage = targetCode,
                )
                withContext(Dispatchers.Main) {
                    applyIfStillCurrent(target, result.text, "Mit Google Translate übersetzt")
                }
            } catch (e: Throwable) {
                if (allowLocalFallback) {
                    withContext(Dispatchers.Main) {
                        toast("Cloud Übersetzung nicht verfügbar. Offline Versuch startet.")
                        translateLocal(target, targetCode, targetName)
                    }
                } else {
                    toast(e.message ?: "Google Translate Übersetzung fehlgeschlagen")
                }
            }
        }
    }

    private fun translateLocal(target: Target, targetCode: String, targetName: String) {
        if (target.text.length > 2_500) {
            toast("Für Offline Übersetzung bitte höchstens 2500 Zeichen markieren")
            return
        }

        val targetLanguage = TranslateLanguage.fromLanguageTag(targetCode)
        if (targetLanguage == null) {
            toast("Diese Sprache ist offline nicht verfügbar. Für mehr Sprachen Google Cloud Translation einrichten.")
            return
        }

        toast("Offline Übersetzung nach $targetName wird vorbereitet …")
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
                    toast("Ausgangssprache wird offline nicht unterstützt")
                    return@addOnSuccessListener
                }
                if (sourceLanguage == targetLanguage) {
                    toast("Text ist bereits in $targetName")
                    return@addOnSuccessListener
                }

                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLanguage)
                    .setTargetLanguage(targetLanguage)
                    .build()
                val translator = Translation.getClient(options)
                val conditions = DownloadConditions.Builder().build()
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        translator.translate(target.text)
                            .addOnSuccessListener { translated ->
                                translator.close()
                                applyIfStillCurrent(target, translated, "Offline mit Google ML Kit übersetzt")
                            }
                            .addOnFailureListener {
                                translator.close()
                                toast("Offline Übersetzung fehlgeschlagen")
                            }
                    }
                    .addOnFailureListener {
                        translator.close()
                        toast("Offline Sprachmodell konnte nicht geladen werden")
                    }
            }
            .addOnFailureListener {
                identifier.close()
                toast("Ausgangssprache konnte nicht erkannt werden")
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
                return Target(content.offset + localStart, content.offset + localEnd, selected)
            }
        }

        currentParagraphTarget(content)?.let {
            if (it.text.length <= 5_000) return it
        }
        return currentSentenceTarget(content)
    }

    private fun currentParagraphTarget(content: EditorContent): Target? {
        val cursor = content.localSelection.end.coerceIn(0, content.text.length)
        var start = if (cursor > 0) content.text.lastIndexOf('\n', cursor - 1) + 1 else 0
        var end = content.text.indexOf('\n', cursor)
        if (end < 0) end = content.text.length
        while (start < end && content.text[start].isWhitespace()) start++
        while (end > start && content.text[end - 1].isWhitespace()) end--
        if (start >= end) return null
        return Target(content.offset + start, content.offset + end, content.text.substring(start, end))
    }

    private fun currentSentenceTarget(content: EditorContent): Target? {
        val text = content.text
        if (text.isEmpty()) return null
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
        return Target(content.offset + start, content.offset + end, text.substring(start, end))
    }

    private fun applyIfStillCurrent(target: Target, replacement: String, successMessage: String) {
        val current = editorInstance.activeContent
        if (current.offset < 0) {
            toast("Textfeld ist nicht mehr verfügbar")
            return
        }

        val expectedStart = target.start - current.offset
        val expectedEnd = target.end - current.offset
        var localStart = expectedStart
        var localEnd = expectedEnd
        val exactMatch =
            localStart >= 0 && localEnd <= current.text.length && localStart < localEnd &&
                current.text.substring(localStart, localEnd) == target.text

        if (!exactMatch) {
            val hit = current.text.indexOf(target.text)
            if (hit < 0) {
                toast("Text wurde inzwischen verändert. Bitte erneut übersetzen.")
                return
            }
            localStart = hit
            localEnd = hit + target.text.length
        }

        val absoluteStart = current.offset + localStart
        val absoluteEnd = current.offset + localEnd
        if (!editorInstance.setSelection(absoluteStart, absoluteEnd)) {
            toast("Text konnte in dieser App nicht ausgewählt werden")
            return
        }
        if (!editorInstance.commitText(replacement)) {
            toast("Übersetzung konnte nicht eingesetzt werden")
            return
        }
        toast(successMessage)
    }

    private fun toast(message: String) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
