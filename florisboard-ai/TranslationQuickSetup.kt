package dev.patrickgold.florisboard.ime.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun TranslationQuickSetupPanel(compact: Boolean = true) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(AiAssistant.PREFS_NAME, Context.MODE_PRIVATE) }
    var apiKey by remember { mutableStateOf(prefs.getString(TranslationBackend.KEY_GOOGLE_TRANSLATE_API_KEY, "").orEmpty()) }
    var modeId by remember { mutableStateOf(prefs.getString(TranslationBackend.KEY_TRANSLATION_MODE, TranslationMode.AUTO.id) ?: TranslationMode.AUTO.id) }
    var target by remember { mutableStateOf(prefs.getString(TranslationBackend.KEY_TRANSLATION_TARGET, TranslationBackend.TARGET_ACTIVE_KEYBOARD) ?: TranslationBackend.TARGET_ACTIVE_KEYBOARD) }
    var languages by remember { mutableStateOf<List<TranslationLanguage>>(emptyList()) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val mode = TranslationMode.fromId(modeId)

    fun save(): Boolean = prefs.edit()
        .putString(TranslationBackend.KEY_GOOGLE_TRANSLATE_API_KEY, apiKey.trim())
        .putString(TranslationBackend.KEY_TRANSLATION_MODE, modeId)
        .putString(TranslationBackend.KEY_TRANSLATION_TARGET, target.ifBlank { TranslationBackend.TARGET_ACTIVE_KEYBOARD })
        .commit()

    fun chooseMode(newMode: TranslationMode) {
        modeId = newMode.id
        status = if (save()) newMode.displayName + " ist für Übersetzungen aktiv" else "Übersetzungseinstellung konnte nicht gespeichert werden"
    }

    fun targetLabel(): String {
        if (target == TranslationBackend.TARGET_ACTIVE_KEYBOARD) return "Aktive Tastatursprache"
        val found = languages.firstOrNull { it.code == target }
        return if (found != null) found.name + " (" + found.code + ")" else target
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Übersetzung", style = MaterialTheme.typography.titleLarge)
        Text("Automatisch nutzt Google Cloud Translation für bessere Qualität und eine größere Sprachauswahl, wenn ein eigener API Schlüssel eingerichtet ist. Sonst wird lokal mit Google ML Kit übersetzt.")

        Text("Übersetzungsmodus", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { chooseMode(TranslationMode.AUTO) }) {
                Text(if (mode == TranslationMode.AUTO) "Automatisch ✓" else "Automatisch")
            }
            Button(onClick = { chooseMode(TranslationMode.CLOUD) }) {
                Text(if (mode == TranslationMode.CLOUD) "Cloud ✓" else "Cloud")
            }
            Button(onClick = { chooseMode(TranslationMode.LOCAL) }) {
                Text(if (mode == TranslationMode.LOCAL) "Offline ✓" else "Offline")
            }
        }

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Google Cloud Translation API Schlüssel") },
            supportingText = { Text("Wird nur auf dem Gerät gespeichert. Cloud Übersetzungen können Google Cloud Kosten verursachen.") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = {
                status = if (save()) "Übersetzungseinstellungen gespeichert" else "Speichern fehlgeschlagen"
            }) { Text("Speichern") }
            Button(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TranslationBackend.apiConsoleUrl())))
            }) { Text("Google API öffnen") }
        }

        Button(
            onClick = {
                if (!save()) {
                    status = "Speichern fehlgeschlagen"
                    return@Button
                }
                if (apiKey.isBlank()) {
                    status = "Bitte zuerst einen Google Cloud Translation API Schlüssel eintragen"
                    return@Button
                }
                status = "Google Translate Sprachen werden geladen …"
                scope.launch {
                    status = try {
                        languages = TranslationBackend.listCloudLanguages(apiKey.trim(), "de")
                        languages.size.toString() + " unterstützte Sprachen geladen"
                    } catch (e: Throwable) {
                        languages = emptyList()
                        e.message ?: "Sprachliste konnte nicht geladen werden"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Unterstützte Sprachen von Google laden") }

        Text("Zielsprache", style = MaterialTheme.typography.titleMedium)
        Box {
            Button(
                onClick = { languageMenuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(targetLabel() + " ▾") }
            DropdownMenu(
                expanded = languageMenuExpanded,
                onDismissRequest = { languageMenuExpanded = false },
                modifier = Modifier.heightIn(max = 420.dp),
            ) {
                DropdownMenuItem(
                    text = { Text("Aktive Tastatursprache") },
                    onClick = {
                        target = TranslationBackend.TARGET_ACTIVE_KEYBOARD
                        languageMenuExpanded = false
                        status = if (save()) "Zielsprache folgt der aktiven Tastatur" else "Speichern fehlgeschlagen"
                    },
                )
                languages.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language.name + " (" + language.code + ")") },
                        onClick = {
                            target = language.code
                            languageMenuExpanded = false
                            status = if (save()) "Zielsprache: " + language.name else "Speichern fehlgeschlagen"
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            value = if (target == TranslationBackend.TARGET_ACTIVE_KEYBOARD) "" else target,
            onValueChange = { target = it.ifBlank { TranslationBackend.TARGET_ACTIVE_KEYBOARD } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Oder Sprachcode direkt eingeben") },
            supportingText = { Text("Leer = aktive Tastatursprache, z. B. de, en, uk, ar, zh-CN") },
            singleLine = true,
        )

        Text("Cloud Übersetzungen werden an Google Translate gesendet. Im Inkognito Modus wird automatisch nur die lokale Offline Übersetzung verwendet.")
        Text("Übersetzungen werden durch Google Translate bzw. Google ML Kit bereitgestellt.", style = MaterialTheme.typography.bodySmall)

        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodyLarge)
    }
}
