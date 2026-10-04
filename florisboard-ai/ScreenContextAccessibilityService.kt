package dev.patrickgold.florisboard.ime.ai

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.lang.ref.WeakReference

data class ScreenContextSnapshot(
    val text: String,
    val packageName: String?,
)

class ScreenContextAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile private var instanceRef: WeakReference<ScreenContextAccessibilityService>? = null

        fun isConnected(): Boolean = instanceRef?.get() != null

        fun readVisibleText(): ScreenContextSnapshot? {
            val service = instanceRef?.get() ?: return null
            val root = service.windows
                .asSequence()
                .filter { it.isActive || it.isFocused }
                .mapNotNull { it.root }
                .firstOrNull { it.packageName?.toString() != service.packageName }
                ?: service.rootInActiveWindow
                ?: return null
            return try {
                val lines = LinkedHashSet<String>()
                collectVisibleText(root, lines)
                val text = lines
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
                    .takeLast(8_000)
                if (text.isBlank()) null else ScreenContextSnapshot(text, root.packageName?.toString())
            } finally {
                root.recycle()
            }
        }

        private fun collectVisibleText(node: AccessibilityNodeInfo, out: LinkedHashSet<String>) {
            if (!node.isVisibleToUser || node.isPassword) return
            node.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let(out::add)
            node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let(out::add)
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try { collectVisibleText(child, out) } finally { child.recycle() }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        instanceRef = null
        super.onDestroy()
    }
}

object ScreenContextConsent {
    private const val PREF = "screen_context_consent"
    private const val KEY = "approved"

    fun hasConsent(context: android.content.Context): Boolean =
        context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun setConsent(context: android.content.Context, approved: Boolean) {
        context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
            .edit().putBoolean(KEY, approved).apply()
    }
}
