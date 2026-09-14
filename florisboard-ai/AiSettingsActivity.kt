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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class AiSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { AiSettingsScreen() }
        }
    }

    @Composable
    private fun AiSettingsScreen() {
        val prefs = remember { getSharedPreferences(AiAssistant.PREFS_NAME, Context.MODE_PRIVATE) }
        var providerId by remember {
            mutableStateOf(prefs.getString(AiAssistant.KEY_PROVIDER, AiAssistant.DEFAULT_PROVIDER.id) ?: AiAssistant.DEFAULT_PROVIDER.id)
        }
        var openAiKey by remember { mutableStateOf(prefs.getString(AiAssistant.KEY_OPENAI_API_KEY, "").orEmpty()) }
        var groqKey by remember { mutableStateOf(prefs.getString(AiAssistant.KEY_GROQ_API_KEY, "").orEmpty()) }
        var openAiModel by remember { mutableStateOf(prefs.getString(AiAssistant.KEY_OPENAI_MODEL, AiAssistant.DEFAULT_OPENAI_MODEL).orEmpty()) }
        var groqModel by remember {
            mutableStateOf(
                prefs.getString(AiAssistant.KEY_GROQ_MODEL, "").orEmpty().ifBlank {
                    prefs.getString(AiAssistant.LEGACY_KEY_MODEL, AiAssistant.DEFAULT_GROQ_MODEL).orEmpty()
                }
            )
        }
        var autoCorrection by remember { mutableStateOf(prefs.getBoolean(AiAssistant.KEY_AUTO_CORRECTION, true)) }
        var status by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
        val provider = AiProvider.fromId(providerId)

        fun save() {
            prefs.edit()
                .putString(AiAssistant.KEY_PROVIDER, providerId)
                .putString(AiAssistant.KEY_OPENAI_API_KEY, openAiKey.trim())
                .putString(AiAssistant.KEY_GROQ_API_KEY, groqKey.trim())
                .putString(AiAssistant.KEY_OPENAI_MODEL, openAiModel.trim().ifBlank { AiAssistant.DEFAULT_OPENAI_MODEL })
                .putString(AiAssistant.KEY_GROQ_MODEL, groqModel.trim().ifBlank { AiAssistant.DEFAULT_GROQ_MODEL })
                .putBoolean(AiAssistant.KEY_AUTO_CORRECTION, autoCorrection)
                .apply()
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("KI Schreibassistent", style = MaterialTheme.typography.headlineSmall)
            Text("Automatische Korrektur für getippte und diktierte Texte sowie Umschreiben in verschiedenen Stilen.")

            Text("KI Anbieter", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    providerId = AiProvider.OPENAI.id
                    prefs.edit().putString(AiAssistant.KEY_PROVIDER, AiProvider.OPENAI.id).apply()
                    status = "OpenAI ausgewählt"
                }) { Text(if (provider == AiProvider.OPENAI) "OpenAI ✓" else "OpenAI") }
                Button(onClick = {
                    providerId = AiProvider.GROQ.id
                    prefs.edit().putString(AiAssistant.KEY_PROVIDER, AiProvider.GROQ.id).apply()
                    status = "Groq ausgewählt"
                }) { Text(if (provider == AiProvider.GROQ) "Groq ✓" else "Groq") }
            }

            Text(
                if (provider == AiProvider.OPENAI) {
                    "OpenAI ist als Standard vorgesehen. GPT-5.6 Luna ist günstig für häufige kurze Korrekturen. Die OpenAI API wird getrennt von ChatGPT abgerechnet."
                } else {
                    "Groq bleibt als kostenlose Alternative verfügbar."
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.fillMaxWidth(0.80f)) {
                    Text("Automatische KI Korrektur", style = MaterialTheme.typography.titleMedium)
                    Text("Passwortfelder, E-Mail/URL-Felder und Inkognito werden nicht automatisch an die KI gesendet.")
                }
                Switch(
                    checked = autoCorrection,
                    onCheckedChange = {
                        autoCorrection = it
                        save()
                    },
                )
            }

            if (provider == AiProvider.OPENAI) {
                OutlinedTextField(
                    value = openAiKey,
                    onValueChange = { openAiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("OpenAI API Schlüssel") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Button(
                    onClick = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://platform.openai.com/api-keys"))) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("OpenAI API Schlüssel öffnen") }
                OutlinedTextField(
                    value = openAiModel,
                    onValueChange = { openAiModel = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("OpenAI Modell") },
                    supportingText = { Text("Standard: ${AiAssistant.DEFAULT_OPENAI_MODEL}") },
                    singleLine = true,
                )
            } else {
                OutlinedTextField(
                    value = groqKey,
                    onValueChange = { groqKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Groq API Schlüssel") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Button(
                    onClick = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://console.groq.com/keys"))) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Kostenlosen Groq API Schlüssel erstellen") }
                OutlinedTextField(
                    value = groqModel,
                    onValueChange = { groqModel = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Groq Modell") },
                    supportingText = { Text("Standard: ${AiAssistant.DEFAULT_GROQ_MODEL}") },
                    singleLine = true,
                )
            }

            Button(onClick = {
                save()
                status = "Einstellungen gespeichert"
            }, modifier = Modifier.fillMaxWidth()) { Text("Speichern") }

            Button(onClick = {
                save()
                status = "Verbindung wird geprüft …"
                val key = if (provider == AiProvider.OPENAI) openAiKey else groqKey
                val model = if (provider == AiProvider.OPENAI) {
                    openAiModel.ifBlank { AiAssistant.DEFAULT_OPENAI_MODEL }
                } else {
                    groqModel.ifBlank { AiAssistant.DEFAULT_GROQ_MODEL }
                }
                scope.launch {
                    status = try {
                        val result = AiAssistant.testConnection(provider.id, key.trim(), model.trim())
                        "Verbindung erfolgreich. Test: $result"
                    } catch (e: Throwable) {
                        e.message ?: "Verbindung fehlgeschlagen"
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("KI Verbindung testen") }

            if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodyLarge)

            Spacer(Modifier.height(8.dp))
            Text("Stil Funktionen", style = MaterialTheme.typography.titleLarge)
            Text("Korrigieren, Freundlich, Professionell, Locker, Humorvoll, Ironisch, Kurz, Einfach und Direkt. Markierter Text wird gezielt bearbeitet; sonst der aktuelle Satz.")

            Text("Datenschutz", style = MaterialTheme.typography.titleLarge)
            Text("Für KI Funktionen wird nur der aktuelle Satz oder markierte Text per HTTPS an den ausgewählten Anbieter gesendet. Automatische Verarbeitung ist in sensiblen Feldern und im Inkognito Modus gesperrt.")
        }
    }
}
