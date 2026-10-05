package dev.patrickgold.florisboard.ime.ai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

object ScreenTranslationConsent {
    private const val KEY = "screen_translation_consent"

    fun allowed(context: Context): Boolean =
        context.getSharedPreferences(AiAssistant.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY, false)

    fun setAllowed(context: Context, value: Boolean) {
        context.getSharedPreferences(AiAssistant.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY, value).apply()
    }
}

class ScreenTranslationDisclosureActivity : Activity() {
    companion object {
        fun open(context: Context) {
            context.startActivity(
                Intent(context, ScreenTranslationDisclosureActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Automatische Gesprächsübersetzung"

        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(24))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }

        container.addView(TextView(this).apply {
            text = "Gesprächssprache automatisch erkennen"
            textSize = 22f
            setPadding(0, 0, 0, dp(18))
        })

        container.addView(TextView(this).apply {
            text = "Wenn du „KI Übersetzen“ drückst, kann KI Tastatur den aktuell sichtbaren, für Android zugänglichen Text der geöffneten App verwenden, um die Sprache des Gesprächs zu erkennen. Dein Text und der sichtbare Gesprächskontext werden dafür an deinen ausgewählten KI Anbieter gesendet. Dein Text wird anschließend in die erkannte Gesprächssprache übersetzt.\n\nDas geschieht nur nach deinem Tippen auf „KI Übersetzen“. Es wird kein Bildschirmverlauf für diese Funktion gespeichert. Du kannst diese Zustimmung jederzeit durch Zurücksetzen der App Daten widerrufen."
            textSize = 16f
            setPadding(0, 0, 0, dp(24))
        })

        container.addView(Button(this).apply {
            text = "Automatische Übersetzung erlauben"
            setOnClickListener {
                ScreenTranslationConsent.setAllowed(this@ScreenTranslationDisclosureActivity, true)
                finish()
            }
        })

        container.addView(Button(this).apply {
            text = "Nicht erlauben"
            setOnClickListener {
                ScreenTranslationConsent.setAllowed(this@ScreenTranslationDisclosureActivity, false)
                finish()
            }
        })

        setContentView(container)
    }
}
