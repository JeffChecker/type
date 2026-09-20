package dev.patrickgold.florisboard.ime.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class AiSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AiSettingsScreen() } }
    }

    @Composable
    private fun AiSettingsScreen() {
        val prefs = remember { getSharedPreferences(AiAssistant.PREFS_NAME, Context.MODE_PRIVATE) }
        var providerId by remember { mutableStateOf(prefs.getString(AiBackend.KEY_PROVIDER, AiBackend.DEFAULT_PROVIDER.id) ?: AiBackend.DEFAULT_PROVIDER.id) }
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
        var savedProviderId by remember { mutableStateOf(providerId) }
        var savedModelId by remember {
            mutableStateOf(
                when (AiProvider.fromId(providerId)) {
                    AiProvider.OPENAI -> openAiModel
                    AiProvider.GEMINI -> geminiModel
                    AiProvider.CLAUDE -> claudeModel
                    AiProvider.GROQ -> groqModel
                }
            )
        }
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
            val v = value.ifBlank { AiBackend.AUTO_MODEL }
            when (provider) {
                AiProvider.OPENAI -> openAiModel = v
                AiProvider.GEMINI -> geminiModel = v
                AiProvider.CLAUDE -> claudeModel = v
                AiProvider.GROQ -> groqModel = v
            }
        }

        fun save(): Boolean {
            val ok = prefs.edit()
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
            if (ok) {
                savedProviderId = providerId
                savedModelId = currentModel().trim().ifBlank { AiBackend.AUTO_MODEL }
            }
            return ok
        }

        fun chooseProvider(newProvider: AiProvider) {
            providerId = newProvider.id
            models = emptyList()
            val ok = prefs.edit().putString(AiBackend.KEY_PROVIDER, newProvider.id).commit()
            if (ok) {
                savedProviderId = newProvider.id
                savedModelId = when (newProvider) {
                    AiProvider.OPENAI -> openAiModel
                    AiProvider.GEMINI -> geminiModel
                    AiProvider.CLAUDE -> claudeModel
                    AiProvider.GROQ -> groqModel
                }
                status = "${newProvider.displayName} ist jetzt aktiv"
            } else {
                status = "Anbieter konnte nicht gespeichert werden"
            }
        }

        val currentSelectionIsSaved = savedProviderId == providerId && savedModelId == currentModel()

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("KI Schreibassistent", style = MaterialTheme.typography.headlineSmall)
            Text("Die KI verändert deinen Text nur noch, wenn du selbst eine Taste in der Smartbar drückst. Es gibt keine zeitgesteuerte Autokorrektur mehr.")

            Text("Bedienung", style = MaterialTheme.typography.titleMedium)
            Text("KI korrigieren: prüft den aktuellen Absatz oder markierten Text auf Sinn, Sprache, Diktatfehler und Zeichensetzung. Deine eigenen Satzendzeichen wie !, ?, ?! oder !! bleiben erhalten.")
            Text("Stil: öffnet ein Auswahlmenü mit Sarkastisch, Flirtend, Verführerisch, Stilvoll, Geschäftlich, Professionell, Freundlich, Persönlich, Du-Form, Sie-Form, Locker, Humorvoll, Direkt, Kurz und Einfach.")
            Text("Prompt+: verbessert deinen Rohtext zu einem klaren KI Prompt, ohne neue Fakten zu erfinden.")

            Text("KI Anbieter", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { chooseProvider(AiProvider.OPENAI) }) { Text(if (provider == AiProvider.OPENAI) "OpenAI ✓" else "OpenAI") }
                Button(onClick = { chooseProvider(AiProvider.GEMINI) }) { Text(if (provider == AiProvider.GEMINI) "Gemini ✓" else "Gemini") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { chooseProvider(AiProvider.CLAUDE) }) { Text(if (provider == AiProvider.CLAUDE) "Claude ✓" else "Claude") }
                Button(onClick = { chooseProvider(AiProvider.GROQ) }) { Text(if (provider == AiProvider.GROQ) "Groq ✓" else "Groq") }
            }

            OutlinedTextField(
                value = currentKey(),
                onValueChange = { setCurrentKey(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("${provider.displayName} API Schlüssel") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )

            Button(
                onClick = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AiBackend.apiKeyPage(provider)))) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("API Schlüssel Seite öffnen") }

            Text("Modell", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = currentModel(),
                onValueChange = { setCurrentModel(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Modell-ID") },
                supportingText = { Text("'auto' = Modell wird aus der aktuellen API Modellliste automatisch gewählt") },
                singleLine = true,
            )

            Text(
                if (currentSelectionIsSaved) {
                    "✓ Aktiv gespeichert: ${provider.displayName} • ${currentModel()}"
                } else {
                    "Noch nicht gespeichert: ${provider.displayName} • ${currentModel()}"
                },
                style = MaterialTheme.typography.bodyLarge,
            )

            Button(onClick = {
                status = if (save()) {
                    "Gespeichert. ${provider.displayName} mit ${currentModel()} wird verwendet."
                } else {
                    "Speichern fehlgeschlagen"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Speichern und verwenden") }

            if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodyLarge)

            Button(onClick = {
                setCurrentModel(AiBackend.AUTO_MODEL)
                status = if (save()) {
                    "Automatische Modellwahl ist gespeichert und aktiv"
                } else {
                    "Speichern fehlgeschlagen"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Automatisch wählen") }

            Button(onClick = {
                if (!save()) {
                    status = "Speichern fehlgeschlagen"
                    return@Button
                }
                status = "Modelle werden über ${provider.displayName} geladen …"
                scope.launch {
                    status = try {
                        models = AiBackend.listModels(provider, currentKey().trim())
                        val recommended = AiBackend.chooseAutomaticModel(provider, models)
                        "${models.size} Textmodelle gefunden. Automatisch würde ${recommended.displayName} gewählt."
                    } catch (e: Throwable) {
                        models = emptyList()
                        e.message ?: "Modellliste konnte nicht geladen werden"
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Verfügbare Modelle laden") }

            if (models.isNotEmpty()) {
                Text("Modelle von ${provider.displayName}", style = MaterialTheme.typography.titleMedium)
                models.take(20).forEach { model ->
                    Button(onClick = {
                        setCurrentModel(model.id)
                        status = if (save()) {
                            "Gespeichert und aktiv: ${provider.displayName} • ${model.displayName}"
                        } else {
                            "Modell konnte nicht gespeichert werden"
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (currentModel() == model.id) "${model.displayName} ✓" else model.displayName)
                    }
                    if (currentModel() == model.id && savedProviderId == providerId && savedModelId == model.id) {
                        Text("✓ Dieses Modell ist gespeichert und wird verwendet.")
                    }
                }
                if (models.size > 20) Text("Weitere Modelle können über ihre Modell-ID eingetragen werden.")
            }

            Button(onClick = {
                if (!save()) {
                    status = "Speichern fehlgeschlagen"
                    return@Button
                }
                status = "Verbindung wird geprüft …"
                scope.launch {
                    status = try {
                        val result = AiAssistant.testConnection(provider, currentKey().trim(), currentModel().trim())
                        "✓ Verbindung erfolgreich. ${provider.displayName} mit ${currentModel()} funktioniert. Test: $result"
                    } catch (e: Throwable) {
                        e.message ?: "Verbindung fehlgeschlagen"
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("KI Verbindung testen") }

            Button(onClick = {
                if (save()) {
                    finish()
                } else {
                    status = "Speichern fehlgeschlagen"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Speichern und schließen") }

            Spacer(Modifier.height(8.dp))
            Text("Übersetzen", style = MaterialTheme.typography.titleLarge)
            Text("Die Übersetzung läuft lokal mit ML Kit. Die Ausgangssprache wird automatisch erkannt. Übersetzt wird in die aktuell aktive Tastatursprache. Sprachmodelle werden bei Bedarf einmalig geladen.")

            Text("Datenschutz", style = MaterialTheme.typography.titleLarge)
            Text("Cloud-KI: Nur der aktuelle Absatz oder markierte Text wird per HTTPS an den ausgewählten Anbieter gesendet, und nur nachdem du eine KI Taste drückst. Übersetzen läuft nach dem Modelldownload lokal auf dem Gerät.")
        }
    }
}
