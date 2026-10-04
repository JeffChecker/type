package dev.patrickgold.florisboard.ime.ai

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

data class ScreenContextSnapshot(
    val packageName: String,
    val text: String,
)

object ScreenContextConsent {
    private const val KEY_SCREEN_CONTEXT_CONSENT = "screen_context_consent"

    fun hasConsent(context: Context): Boolean =
        context.getSharedPreferences(AiAssistant.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SCREEN_CONTEXT_CONSENT, false)

    fun setConsent(context: Context, allowed: Boolean) {
        context.getSharedPreferences(AiAssistant.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SCREEN_CONTEXT_CONSENT, allowed)
            .apply()
    }
}

class ScreenContextAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile
        private var activeService: ScreenContextAccessibilityService? = null

        fun isConnected(): Boolean = activeService != null

        fun readVisibleText(): ScreenContextSnapshot? {
            val service = activeService ?: return null
            if (!ScreenContextConsent.hasConsent(service)) return null
            return service.captureVisibleText()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeService = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Absichtlich leer. Es wird kein Verlauf mitgeschnitten oder gespeichert.
        // Der sichtbare Text wird ausschließlich synchron nach Tippen auf "Antwort" gelesen.
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (activeService === this) activeService = null
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (activeService === this) activeService = null
        return super.onUnbind(intent)
    }

    private fun captureVisibleText(): ScreenContextSnapshot? {
        val candidates = windows
            .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            .sortedWith(
                compareByDescending<AccessibilityWindowInfo> { it.isActive }
                    .thenByDescending { it.isFocused }
            )

        for (window in candidates) {
            val root = window.root ?: continue
            val snapshot = snapshotFromRoot(root)
            if (snapshot != null) return snapshot
        }

        return rootInActiveWindow?.let(::snapshotFromRoot)
    }

    private fun snapshotFromRoot(root: AccessibilityNodeInfo): ScreenContextSnapshot? {
        val sourcePackage = root.packageName?.toString().orEmpty()
        if (sourcePackage.isBlank() || sourcePackage == packageName) return null

        val lines = LinkedHashSet<String>()
        collectText(root, lines, Counter(), depth = 0)

        val joined = lines.joinToString("\n")
            .trim()
            .take(6_000)

        if (joined.isBlank()) return null
        return ScreenContextSnapshot(sourcePackage, joined)
    }

    private data class Counter(var nodes: Int = 0, var chars: Int = 0)

    private fun collectText(
        node: AccessibilityNodeInfo,
        lines: LinkedHashSet<String>,
        counter: Counter,
        depth: Int,
    ) {
        if (counter.nodes >= 1_200 || counter.chars >= 7_000 || depth > 40) return
        counter.nodes += 1

        if (!node.isVisibleToUser || node.isPassword) return

        // Editierbare Felder werden ausgelassen. Den eigenen Entwurf holt die
        // Tastatur direkt aus dem Editor, damit keine fremden Formulareingaben
        // unnötig in den Bildschirmkontext geraten.
        if (!node.isEditable) {
            addCandidate(node.text?.toString(), lines, counter)
            if (node.text.isNullOrBlank()) {
                addCandidate(node.contentDescription?.toString(), lines, counter)
            }
        }

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            collectText(child, lines, counter, depth + 1)
            if (counter.nodes >= 1_200 || counter.chars >= 7_000) break
        }
    }

    private fun addCandidate(
        value: String?,
        lines: LinkedHashSet<String>,
        counter: Counter,
    ) {
        val normalized = value
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()

        if (normalized.length < 2) return
        if (normalized.all { it == '•' || it == '*' || it == '·' }) return

        if (lines.add(normalized)) {
            counter.chars += normalized.length + 1
        }
    }
}

class ScreenContextDisclosureActivity : Activity() {
    companion object {
        fun open(context: Context) {
            context.startActivity(
                Intent(context, ScreenContextDisclosureActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Bildschirmkontext für Antwortvorschläge"

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
            text = "Bildschirmtext lesen für passende Antworten"
            textSize = 22f
            setPadding(0, 0, 0, dp(18))
        })

        container.addView(TextView(this).apply {
            text = "Wenn du in der Tastatur auf „Antwort“ tippst, darf KI Tastatur den aktuell sichtbaren, für Android zugänglichen Text der geöffneten App lesen. Der Text wird zusammen mit deinem vorhandenen Entwurf an den von dir gewählten KI Anbieter gesendet, damit eine passende Antwort formuliert werden kann.\n\nDie Funktion liest nicht dauerhaft mit. Es wird kein Bildschirmverlauf gespeichert. Passwortfelder und editierbare Fremdfelder werden ausgelassen. Ohne dein Tippen auf „Antwort“ wird kein Bildschirmtext für diese Funktion an die KI gesendet.\n\nAndroid verlangt dafür die Aktivierung eines Bedienungshilfe Dienstes. Du kannst ihn jederzeit in den Android Einstellungen wieder ausschalten."
            textSize = 16f
            setPadding(0, 0, 0, dp(24))
        })

        container.addView(Button(this).apply {
            text = "Zustimmen und Bedienungshilfe öffnen"
            setOnClickListener {
                ScreenContextConsent.setConsent(this@ScreenContextDisclosureActivity, true)
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                finish()
            }
        })

        container.addView(Button(this).apply {
            text = "Nicht aktivieren"
            setOnClickListener {
                ScreenContextConsent.setConsent(this@ScreenContextDisclosureActivity, false)
                finish()
            }
        })

        setContentView(container)
    }
}
