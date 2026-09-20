package dev.patrickgold.florisboard.ime.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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

/**
 * Kompakte KI Modellkonfiguration für die Hauptseite und den Einrichtungsassistenten.
 */
@Composable
fun AiQuickSetupPanel(
    modifier: Modifier = Modifier,
    compact: Boolean = true,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(AiAssistant.PREFS_NAME, Context.MODE_PRIVATE) }

    var providerId by remember {
        mutableStateOf(prefs.getString(AiBackend.KEY_PROVIDER, AiBackend.DEFAULT_PROVIDER.id) ?: AiBackend.DEFAULT_PROVIDER.id)
    }
    var openAiKey by remember { mutableStateOf(prefs.getString(AiBackend.KEY_OPENAI_API_KEY, "").orEmpty()) }
    var geminiKey by remember { mutableStateOf(prefs.getString(AiBackend.KEY_GEMINI_API_KEY, "").orEmpty()) }
    var claudeKey by remember { mutableStateOf(prefs.getString(AiBackend.KEY_CLAUDE_API_KEY, "").orEmpty()) }
    var groqKey by remember { mutableStateOf(prefs.getString(AiBackend.KEY_GROQ_API_KEY, "").orEmpty()) }

    var openAiModel by remember { mutableStateOf(prefs.getString(AiBackend.KEY_OPENAI_MODEL, AiBackend.AUTO_MODEL).orEmpty().ifBlank { AiBackend.AUTO_MODEL }) }
    var geminiModel by remember { mutableStateOf(prefs.getString(AiBackend.KEY_GEMINI_MODEL, AiBackend.AUTO_MODEL).orEmpty().ifBlank { AiBackend.AUTO_MODEL }) }
    var claudeModel by remember { mutableStateOf(prefs.getString(AiBackend.KEY_CLAUDE_MODEL, AiBackend.AUTO_MODEL).orEmpty().ifBlank { AiBackend.AUTO_MODEL }) }
    var groqModel by remember {
        mutableStateOf(
            prefs.getString(AiBackend.KEY_GROQ_MODEL, null)
                ?: prefs.getString(AiBackend.LEGACY_KEY_MODEL, AiBackend.AUTO_MODEL)
                ?: AiBackend.AUTO_MODEL
        )
    }

    var status by remember { mutableStateOf("") }
    var models by remember { mutableStateOf<List<AiModel>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val provider = AiProvider.fromId(providerId)

    fun currentKey(): String = when (provider) {
        AiProvider.OPENAI -> openAiKey
        AiProvider.GEMINI -> geminiKey
        AiProvider.CLAUDE -> claudeKey
        AiProvider.GROQ -> groqKey
    }

    fun setCurrentKey(value: String) {
        when (provider) {
            AiProvider.OPENAI -> openAiKey = value
            AiProvider.GEMINI -> geminiKey = value
            AiProvider.CLAUDE -> claudeKey = value
            AiProvider.GROQ -> groqKey = value
        }
    }

    fun currentModel(): String = when (provider) {
        AiProvider.OPENAI -> openAiModel
        AiProvider.GEMINI -> geminiModel
        AiProvider.CLAUDE -> claudeModel
        AiProvider.GROQ -> groqModel
    }

    fun setCurrentModel(value: String) {
        val normalized = value.trim().ifBlank { AiBackend.AUTO_MODEL }
        when (provider) {
            AiProvider.OPENAI -> openAiModel = normalized
            AiProvider.GEMINI -> geminiModel = normalized
            AiProvider.CLAUDE -> claudeModel = normalized
            AiProvider.GROQ -> groqModel = normalized
        }
    }

    fun save(): Boolean = prefs.edit()
        .putString(AiBackend.KEY_PROVIDER, providerId)
        .putString(AiBackend.KEY_OPENAI_API_KEY, openAiKey.trim())
        .putString(AiBackend.KEY_GEMINI_API_KEY, geminiKey.trim())
        .putString(AiBackend.KEY_CLAUDE_API_KEY, claudeKey.trim())
        .putString(AiBackend.KEY_GROQ_API_KEY, groqKey.trim())
        .putString(AiBackend.KEY_OPENAI_MODEL, openAiModel.trim().ifBlank { AiBackend.AUTO_MODEL })
        .putString(AiBackend.KEY_GEMINI_MODEL, geminiModel.trim().ifBlank { AiBackend.AUTO_MODEL })
        .putString(AiBackend.KEY_CLAUDE_MODEL, claudeModel.trim().ifBlank { AiBackend.AUTO_MODEL })
        .putString(AiBackend.KEY_GROQ_MODEL, groqModel.trim().ifBlank { AiBackend.AUTO_MODEL })
        .commit()

    fun chooseProvider(newProvider: AiProvider) {
        providerId = newProvider.id
        models = emptyList()
        val ok = prefs.edit().putString(AiBackend.KEY_PROVIDER, newProvider.id).commit()
        status = if (ok) newProvider.displayName + " ist aktiv" else "Anbieter konnte nicht gespeichert werden"
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("KI Modellkonfiguration", style = MaterialTheme.typography.titleLarge)
        Text("Anbieter, API Schlüssel und Modell sind hier direkt erreichbar. Mit „auto“ wird ein passendes Textmodell aus der aktuellen API Modellliste gewählt.")

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { chooseProvider(AiProvider.OPENAI) }) { Text(if (provider == AiProvider.OPENAI) "OpenAI ✓" else "OpenAI") }
            Button(onClick = { chooseProvider(AiProvider.GEMINI) }) { Text(if (provider == AiProvider.GEMINI) "Gemini ✓" else "Gemini") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { chooseProvider(AiProvider.CLAUDE) }) { Text(if (provider == AiProvider.CLAUDE) "Claude ✓" else "Claude") }
            Button(onClick = { chooseProvider(AiProvider.GROQ) }) { Text(if (provider == AiProvider.GROQ) "Groq ✓" else "Groq") }
        }

        OutlinedTextField(
            value = currentKey(),
            onValueChange = { setCurrentKey(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(provider.displayName + " API Schlüssel") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
        )

        OutlinedTextField(
            value = currentModel(),
            onValueChange = { setCurrentModel(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Modell") },
            supportingText = { Text("auto = automatisch aus der aktuellen Modellliste wählen") },
            singleLine = true,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = {
                setCurrentModel(AiBackend.AUTO_MODEL)
                status = if (save()) "Automatische Modellwahl gespeichert" else "Speichern fehlgeschlagen"
            }) { Text("Auto") }
            Button(onClick = {
                status = if (save()) "Gespeichert: " + provider.displayName + " • " + currentModel() else "Speichern fehlgeschlagen"
            }) { Text("Speichern") }
        }

        Button(
            onClick = {
                if (!save()) {
                    status = "Speichern fehlgeschlagen"
                    return@Button
                }
                status = "Modelle werden geladen …"
                scope.launch {
                    status = try {
                        models = AiBackend.listModels(provider, currentKey().trim())
                        val automatic = AiBackend.chooseAutomaticModel(provider, models)
                        models.size.toString() + " Modelle gefunden. Auto würde " + automatic.displayName + " verwenden."
                    } catch (e: Throwable) {
                        models = emptyList()
                        e.message ?: "Modellliste konnte nicht geladen werden"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Verfügbare Modelle laden") }

        if (models.isNotEmpty()) {
            Text("Modelle von " + provider.displayName, style = MaterialTheme.typography.titleMedium)
            val limit = if (compact) 8 else 20
            models.take(limit).forEach { model ->
                Button(
                    onClick = {
                        setCurrentModel(model.id)
                        status = if (save()) "Aktiv: " + provider.displayName + " • " + model.displayName else "Modell konnte nicht gespeichert werden"
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (currentModel() == model.id) model.displayName + " ✓" else model.displayName)
                }
            }
            if (models.size > limit) Text("Weitere Modelle können über die Modell ID eingetragen werden.")
        }

        Button(
            onClick = {
                if (!save()) {
                    status = "Speichern fehlgeschlagen"
                    return@Button
                }
                status = "Verbindung wird geprüft …"
                scope.launch {
                    status = try {
                        AiAssistant.testConnection(provider, currentKey().trim(), currentModel().trim())
                        "✓ Verbindung erfolgreich"
                    } catch (e: Throwable) {
                        e.message ?: "Verbindung fehlgeschlagen"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("KI Verbindung testen") }

        Button(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AiBackend.apiKeyPage(provider))))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("API Schlüssel beim Anbieter öffnen") }

        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodyLarge)

        TranslationQuickSetupPanel(compact = compact)
    }
}
