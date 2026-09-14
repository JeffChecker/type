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
            MaterialTheme {
                AiSettingsScreen()
            }
        }
    }

    @Composable
    private fun AiSettingsScreen() {
        val prefs = remember { getSharedPreferences(AiAssistant.PREFS_NAME, Context.MODE_PRIVATE) }
        var apiKey by remember { mutableStateOf(prefs.getString(AiAssistant.KEY_API_KEY, "").orEmpty()) }
        var model by remember { mutableStateOf(prefs.getString(AiAssistant.KEY_MODEL, AiAssistant.DEFAULT_MODEL).orEmpty()) }
        var autoCorrection by remember { mutableStateOf(prefs.getBoolean(AiAssistant.KEY_AUTO_CORRECTION, true)) }
        var status by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()

        fun save() {
            prefs.edit()
                .putString(AiAssistant.KEY_API_KEY, apiKey.trim())
                .putString(AiAssistant.KEY_MODEL, model.trim().ifBlank { AiAssistant.DEFAULT_MODEL })
                .putBoolean(AiAssistant.KEY_AUTO_CORRECTION, autoCorrection)
                .apply()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("KI Schreibassistent", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Die KI korrigiert Rechtschreibung, Grammatik und Zeichensetzung nach einer kurzen Schreibpause. " +
                    "Größere Textblöcke aus Spracheingabe werden ebenfalls erkannt und korrigiert."
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.fillMaxWidth(0.80f)) {
                    Text("Automatische KI Korrektur", style = MaterialTheme.typography.titleMedium)
                    Text("Passwortfelder, E-Mail/URL-Felder und Inkognito werden nicht an die KI gesendet.")
                }
                Switch(
                    checked = autoCorrection,
                    onCheckedChange = {
                        autoCorrection = it
                        save()
                    },
                )
            }

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Groq API Schlüssel") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )

            Button(
                onClick = {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://console.groq.com/keys")))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Kostenlosen API Schlüssel erstellen")
            }

            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("KI Modell") },
                supportingText = { Text("Standard: ${AiAssistant.DEFAULT_MODEL}") },
                singleLine = true,
            )

            Button(
                onClick = {
                    save()
                    status = "Einstellungen gespeichert"
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Speichern")
            }

            Button(
                onClick = {
                    save()
                    status = "Verbindung wird geprüft …"
                    scope.launch {
                        status = try {
                            val result = AiAssistant.testConnection(apiKey.trim(), model.trim().ifBlank { AiAssistant.DEFAULT_MODEL })
                            "Verbindung erfolgreich. Test: $result"
                        } catch (e: Throwable) {
                            e.message ?: "Verbindung fehlgeschlagen"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("KI Verbindung testen")
            }

            if (status.isNotBlank()) {
                Text(status, style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(Modifier.height(8.dp))
            Text("Stil Funktionen", style = MaterialTheme.typography.titleLarge)
            Text(
                "In der Smartbar stehen Korrigieren, Freundlich, Professionell, Locker, Humorvoll, Ironisch, Kurz, Einfach und Direkt zur Verfügung. " +
                    "Wenn Text markiert ist, wird nur die Markierung bearbeitet. Ohne Markierung wird der aktuelle Satz verwendet."
            )

            Text("Datenschutz", style = MaterialTheme.typography.titleLarge)
            Text(
                "Auf diesem Gerät ist die Gemini Nano Korrektur von Android derzeit nicht unterstützt. Deshalb wird für die KI Funktion der aktuelle Satz oder der markierte Text über HTTPS an Groq gesendet. " +
                    "Die automatische Funktion ist in sensiblen Eingabefeldern und im Inkognito Modus gesperrt."
            )
        }
    }
}
