#!/usr/bin/env python3
from pathlib import Path
import re
import shutil

ROOT = Path("florisboard")
APP = ROOT / "app"
SRC = APP / "src/main"
CTRL = Path("controller/florisboard-ai")


def read(path):
    return path.read_text(encoding="utf-8")


def write(path, text):
    path.write_text(text, encoding="utf-8")


def replace_once(text, old, new, label):
    if old not in text:
        raise SystemExit(f"Patch marker not found: {label}")
    if text.count(old) != 1:
        raise SystemExit(f"Patch marker not unique ({text.count(old)}): {label}")
    return text.replace(old, new, 1)


# Copy the AI implementation and settings activity into the exact v0.5.2 source tree.
ai_dir = SRC / "kotlin/dev/patrickgold/florisboard/ime/ai"
ai_dir.mkdir(parents=True, exist_ok=True)
shutil.copyfile(CTRL / "AiAssistant.kt", ai_dir / "AiAssistant.kt")
shutil.copyfile(CTRL / "AiSettingsActivity.kt", ai_dir / "AiSettingsActivity.kt")
shutil.copyfile(CTRL / "ai_strings.xml", SRC / "res/values/ai_strings.xml")

# Add dedicated key codes for manual AI actions.
path = SRC / "kotlin/dev/patrickgold/florisboard/ime/text/key/KeyCode.kt"
s = read(path)
s = replace_once(
    s,
    "    const val SETTINGS =                    -301\n",
    "    const val SETTINGS =                    -301\n"
    "\n"
    "    const val AI_CORRECT =                  -310\n"
    "    const val AI_FRIENDLY =                 -311\n"
    "    const val AI_PROFESSIONAL =             -312\n"
    "    const val AI_CASUAL =                   -313\n"
    "    const val AI_HUMOROUS =                 -314\n"
    "    const val AI_IRONIC =                   -315\n"
    "    const val AI_SHORT =                    -316\n"
    "    const val AI_SIMPLE =                   -317\n"
    "    const val AI_DIRECT =                   -318\n"
    "    const val AI_SETTINGS =                 -319\n",
    "KeyCode AI constants",
)
write(path, s)

# Make key data instances available to the Smartbar quick action editor.
path = SRC / "kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TextKeyData.kt"
s = read(path)
s = replace_once(
    s,
    "                TOGGLE_AUTOCORRECT,\n",
    "                TOGGLE_AUTOCORRECT,\n"
    "                AI_CORRECT,\n"
    "                AI_FRIENDLY,\n"
    "                AI_PROFESSIONAL,\n"
    "                AI_CASUAL,\n"
    "                AI_HUMOROUS,\n"
    "                AI_IRONIC,\n"
    "                AI_SHORT,\n"
    "                AI_SIMPLE,\n"
    "                AI_DIRECT,\n"
    "                AI_SETTINGS,\n",
    "TextKeyData InternalKeys",
)
marker = '''        /** Predefined key data for [KeyCode.TOGGLE_AUTOCORRECT] */
        val TOGGLE_AUTOCORRECT = TextKeyData(
            type = KeyType.FUNCTION,
            code = KeyCode.TOGGLE_AUTOCORRECT,
            label = "toggle_autocorrect",
        )
'''
addition = marker + '''
        val AI_CORRECT = TextKeyData(KeyType.FUNCTION, KeyCode.AI_CORRECT, "ai_correct")
        val AI_FRIENDLY = TextKeyData(KeyType.FUNCTION, KeyCode.AI_FRIENDLY, "ai_friendly")
        val AI_PROFESSIONAL = TextKeyData(KeyType.FUNCTION, KeyCode.AI_PROFESSIONAL, "ai_professional")
        val AI_CASUAL = TextKeyData(KeyType.FUNCTION, KeyCode.AI_CASUAL, "ai_casual")
        val AI_HUMOROUS = TextKeyData(KeyType.FUNCTION, KeyCode.AI_HUMOROUS, "ai_humorous")
        val AI_IRONIC = TextKeyData(KeyType.FUNCTION, KeyCode.AI_IRONIC, "ai_ironic")
        val AI_SHORT = TextKeyData(KeyType.FUNCTION, KeyCode.AI_SHORT, "ai_short")
        val AI_SIMPLE = TextKeyData(KeyType.FUNCTION, KeyCode.AI_SIMPLE, "ai_simple")
        val AI_DIRECT = TextKeyData(KeyType.FUNCTION, KeyCode.AI_DIRECT, "ai_direct")
        val AI_SETTINGS = TextKeyData(KeyType.FUNCTION, KeyCode.AI_SETTINGS, "ai_settings")
'''
s = replace_once(s, marker, addition, "TextKeyData AI definitions")
write(path, s)

# Give AI actions readable names/tooltips in the existing action editor.
path = SRC / "kotlin/dev/patrickgold/florisboard/ime/smartbar/quickaction/QuickAction.kt"
s = read(path)
s = replace_once(
    s,
    "            KeyCode.TOGGLE_AUTOCORRECT -> R.string.quick_action__toggle_autocorrect\n",
    "            KeyCode.TOGGLE_AUTOCORRECT -> R.string.quick_action__toggle_autocorrect\n"
    "            KeyCode.AI_CORRECT -> R.string.quick_action__ai_correct\n"
    "            KeyCode.AI_FRIENDLY -> R.string.quick_action__ai_friendly\n"
    "            KeyCode.AI_PROFESSIONAL -> R.string.quick_action__ai_professional\n"
    "            KeyCode.AI_CASUAL -> R.string.quick_action__ai_casual\n"
    "            KeyCode.AI_HUMOROUS -> R.string.quick_action__ai_humorous\n"
    "            KeyCode.AI_IRONIC -> R.string.quick_action__ai_ironic\n"
    "            KeyCode.AI_SHORT -> R.string.quick_action__ai_short\n"
    "            KeyCode.AI_SIMPLE -> R.string.quick_action__ai_simple\n"
    "            KeyCode.AI_DIRECT -> R.string.quick_action__ai_direct\n"
    "            KeyCode.AI_SETTINGS -> R.string.quick_action__ai_settings\n",
    "QuickAction display names",
)
s = replace_once(
    s,
    "            KeyCode.TOGGLE_AUTOCORRECT -> R.string.quick_action__toggle_autocorrect__tooltip\n",
    "            KeyCode.TOGGLE_AUTOCORRECT -> R.string.quick_action__toggle_autocorrect__tooltip\n"
    "            KeyCode.AI_CORRECT -> R.string.quick_action__ai_correct__tooltip\n"
    "            KeyCode.AI_FRIENDLY -> R.string.quick_action__ai_friendly__tooltip\n"
    "            KeyCode.AI_PROFESSIONAL -> R.string.quick_action__ai_professional__tooltip\n"
    "            KeyCode.AI_CASUAL -> R.string.quick_action__ai_casual__tooltip\n"
    "            KeyCode.AI_HUMOROUS -> R.string.quick_action__ai_humorous__tooltip\n"
    "            KeyCode.AI_IRONIC -> R.string.quick_action__ai_ironic__tooltip\n"
    "            KeyCode.AI_SHORT -> R.string.quick_action__ai_short__tooltip\n"
    "            KeyCode.AI_SIMPLE -> R.string.quick_action__ai_simple__tooltip\n"
    "            KeyCode.AI_DIRECT -> R.string.quick_action__ai_direct__tooltip\n"
    "            KeyCode.AI_SETTINGS -> R.string.quick_action__ai_settings__tooltip\n",
    "QuickAction tooltips",
)
write(path, s)

# Show short labels on the Smartbar. AI settings keeps the normal Settings icon.
path = SRC / "kotlin/dev/patrickgold/florisboard/ime/keyboard/ComputingEvaluator.kt"
s = read(path)
s = replace_once(
    s,
    '''            KeyCode.KESHIDA -> {
                evaluator.context()?.getString(R.string.key__view_keshida)
            }
            else -> null
''',
    '''            KeyCode.KESHIDA -> {
                evaluator.context()?.getString(R.string.key__view_keshida)
            }
            KeyCode.AI_CORRECT -> "AI✓"
            KeyCode.AI_FRIENDLY -> "☺"
            KeyCode.AI_PROFESSIONAL -> "Pro"
            KeyCode.AI_CASUAL -> "Locker"
            KeyCode.AI_HUMOROUS -> "Humor"
            KeyCode.AI_IRONIC -> "Ironie"
            KeyCode.AI_SHORT -> "Kurz"
            KeyCode.AI_SIMPLE -> "Einfach"
            KeyCode.AI_DIRECT -> "Direkt"
            else -> null
''',
    "ComputingEvaluator AI labels",
)
s = replace_once(
    s,
    '''        KeyCode.SETTINGS -> {
            Icons.Default.Settings
        }
''',
    '''        KeyCode.SETTINGS, KeyCode.AI_SETTINGS -> {
            Icons.Default.Settings
        }
''',
    "ComputingEvaluator AI settings icon",
)
write(path, s)

# Add AI actions to the default Smartbar action catalogue.
path = SRC / "kotlin/dev/patrickgold/florisboard/ime/smartbar/quickaction/QuickActionArrangement.kt"
s = read(path)
s = replace_once(
    s,
    "                QuickAction.InsertKey(TextKeyData.TOGGLE_AUTOCORRECT),\n",
    "                QuickAction.InsertKey(TextKeyData.TOGGLE_AUTOCORRECT),\n"
    "                QuickAction.InsertKey(TextKeyData.AI_CORRECT),\n"
    "                QuickAction.InsertKey(TextKeyData.AI_FRIENDLY),\n"
    "                QuickAction.InsertKey(TextKeyData.AI_PROFESSIONAL),\n"
    "                QuickAction.InsertKey(TextKeyData.AI_CASUAL),\n"
    "                QuickAction.InsertKey(TextKeyData.AI_HUMOROUS),\n"
    "                QuickAction.InsertKey(TextKeyData.AI_IRONIC),\n"
    "                QuickAction.InsertKey(TextKeyData.AI_SHORT),\n"
    "                QuickAction.InsertKey(TextKeyData.AI_SIMPLE),\n"
    "                QuickAction.InsertKey(TextKeyData.AI_DIRECT),\n"
    "                QuickAction.InsertKey(TextKeyData.AI_SETTINGS),\n",
    "QuickActionArrangement defaults",
)
write(path, s)

# Wire automatic correction and manual style actions into KeyboardManager.
path = SRC / "kotlin/dev/patrickgold/florisboard/ime/keyboard/KeyboardManager.kt"
s = read(path)
s = replace_once(
    s,
    "import dev.patrickgold.florisboard.ime.ImeUiMode\n",
    "import dev.patrickgold.florisboard.ime.ImeUiMode\n"
    "import dev.patrickgold.florisboard.ime.ai.AiAssistant\n"
    "import dev.patrickgold.florisboard.ime.ai.AiStyle\n",
    "KeyboardManager AI imports",
)
s = replace_once(
    s,
    "import dev.patrickgold.florisboard.ime.text.key.KeyType\n",
    "import dev.patrickgold.florisboard.ime.text.key.KeyType\n"
    "import dev.patrickgold.florisboard.ime.text.key.KeyVariation\n",
    "KeyboardManager KeyVariation import",
)
s = replace_once(
    s,
    "    private val nlpManager by context.nlpManager()\n",
    "    private val nlpManager by context.nlpManager()\n"
    "    private val aiAssistant = AiAssistant(context)\n",
    "KeyboardManager AiAssistant field",
)
s = replace_once(
    s,
    '''            editorInstance.activeContentFlow.collectIn(scope) { content ->
                resetSuggestions(content)
            }
''',
    '''            editorInstance.activeContentFlow.collectIn(scope) { content ->
                resetSuggestions(content)
                aiAssistant.onContentChanged(
                    content = content,
                    sensitiveField = activeState.keyVariation != KeyVariation.NORMAL,
                    incognito = activeState.isIncognitoMode,
                    rawEditor = editorInstance.activeInfo.isRawInputEditor,
                )
            }
''',
    "KeyboardManager content observer",
)
s = replace_once(
    s,
    '''    private fun handleToggleAutocorrect() {
        lastToastReference.get()?.cancel()
        lastToastReference = WeakReference(
            appContext.showLongToastSync("Autocorrect toggle is a placeholder and not yet implemented")
        )
    }
''',
    '''    private fun handleToggleAutocorrect() {
        aiAssistant.toggleAutoCorrection()
    }

    private fun runAiStyle(style: AiStyle) {
        aiAssistant.runStyle(
            style = style,
            sensitiveField = activeState.keyVariation != KeyVariation.NORMAL,
            incognito = activeState.isIncognitoMode,
            rawEditor = editorInstance.activeInfo.isRawInputEditor,
        )
    }
''',
    "KeyboardManager autocorrect implementation",
)
s = replace_once(
    s,
    "            KeyCode.TOGGLE_AUTOCORRECT -> handleToggleAutocorrect()\n",
    "            KeyCode.TOGGLE_AUTOCORRECT -> handleToggleAutocorrect()\n"
    "            KeyCode.AI_CORRECT -> runAiStyle(AiStyle.CORRECT)\n"
    "            KeyCode.AI_FRIENDLY -> runAiStyle(AiStyle.FRIENDLY)\n"
    "            KeyCode.AI_PROFESSIONAL -> runAiStyle(AiStyle.PROFESSIONAL)\n"
    "            KeyCode.AI_CASUAL -> runAiStyle(AiStyle.CASUAL)\n"
    "            KeyCode.AI_HUMOROUS -> runAiStyle(AiStyle.HUMOROUS)\n"
    "            KeyCode.AI_IRONIC -> runAiStyle(AiStyle.IRONIC)\n"
    "            KeyCode.AI_SHORT -> runAiStyle(AiStyle.SHORT)\n"
    "            KeyCode.AI_SIMPLE -> runAiStyle(AiStyle.SIMPLE)\n"
    "            KeyCode.AI_DIRECT -> runAiStyle(AiStyle.DIRECT)\n"
    "            KeyCode.AI_SETTINGS -> aiAssistant.openSettings()\n",
    "KeyboardManager AI key handling",
)
write(path, s)

# Network permission + private settings activity.
path = SRC / "AndroidManifest.xml"
s = read(path)
s = replace_once(
    s,
    "    <uses-permission android:name=\"android.permission.VIBRATE\"/>\n",
    "    <uses-permission android:name=\"android.permission.VIBRATE\"/>\n"
    "    <uses-permission android:name=\"android.permission.INTERNET\"/>\n",
    "Manifest internet permission",
)
s = replace_once(
    s,
    "        <!-- Main App Activity -->\n",
    "        <activity\n"
    "            android:name=\"dev.patrickgold.florisboard.ime.ai.AiSettingsActivity\"\n"
    "            android:label=\"KI Schreibassistent\"\n"
    "            android:exported=\"false\"\n"
    "            android:theme=\"@style/FlorisAppTheme\"/>\n\n"
    "        <!-- Main App Activity -->\n",
    "Manifest AI settings activity",
)
write(path, s)

print("AI patch applied to FlorisBoard v0.5.2")
print("Features: debounced automatic proofreading, voice-like correction, 9 rewrite styles, Groq settings/test screen")
