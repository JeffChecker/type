package dev.patrickgold.florisboard.ime.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class ScreenContextDisclosureActivity : ComponentActivity() {
    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, ScreenContextDisclosureActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Disclosure() } }
    }

    @Composable
    private fun Disclosure() {
        Column(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Bildschirmkontext für Antwortvorschläge", style = MaterialTheme.typography.headlineSmall)
            Text("Wenn du später auf „Antwort“ tippst, darf KI Tastatur einmalig den aktuell sichtbaren und für Android zugänglichen Text der geöffneten App lesen. Dadurch kann die KI den Gesprächsverlauf verstehen und eine passendere Antwort formulieren.")
            Text("Die Funktion liest nicht dauerhaft mit. Sie wird nur durch deinen Tastendruck ausgelöst. Passwortfelder werden ausgelassen. Inkognito und sensible Eingabefelder bleiben für Cloud KI gesperrt.")
            Text("Der gelesene sichtbare Text wird für den Antwortvorschlag an den von dir gewählten KI Anbieter gesendet. Nutzt du OpenAI, gilt der in der Tastatur ausgewählte ChatGPT Login oder API Zugang.")
            Text("Android verlangt dafür die Bedienungshilfe Berechtigung. Du kannst sie jederzeit in den Android Einstellungen wieder ausschalten.")

            Button(
                onClick = {
                    ScreenContextConsent.setConsent(this@ScreenContextDisclosureActivity, true)
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Zustimmen und Bedienungshilfe öffnen") }

            Button(
                onClick = {
                    ScreenContextConsent.setConsent(this@ScreenContextDisclosureActivity, false)
                    finish()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Nicht aktivieren") }
        }
    }
}
