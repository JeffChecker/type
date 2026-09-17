#!/usr/bin/env python3
from pathlib import Path
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


# Copy AI implementation, settings, strings and Smartbar dropdown implementation.
ai_dir = SRC / "kotlin/dev/patrickgold/florisboard/ime/ai"
ai_dir.mkdir(parents=True, exist_ok=True)
shutil.copyfile(CTRL / "AiAssistant.kt", ai_dir / "AiAssistant.kt")
shutil.copyfile(CTRL / "AiSettingsActivity.kt", ai_dir / "AiSettingsActivity.kt")
shutil.copyfile(CTRL / "ai_strings.xml", SRC / "res/values/ai_strings.xml")
shutil.copyfile(
    CTRL / "QuickActionButton.kt",
    SRC / "kotlin/dev/patrickgold/florisboard/ime/smartbar/quickaction/QuickActionButton.kt",
)

# Add dedicated key codes for manual AI actions and dropdown styles.
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
    "    const val AI_SETTINGS =                 -319\n"
    "    const val AI_PROMPT =                   -320\n"
    "    const val AI_STYLE_MENU =               -321\n"
    "    const val AI_FLIRTY =                   -322\n"
    "    const val AI_ELEGANT =                  -323\n"
    "    const val AI_BUSINESS =                 -324\n"
    "    const val AI_DU =                       -325\n"
    "    const val AI_SIE =                      -326\n"
    "    const val AI_PERSONAL =                 -327\n"
    "    const val AI_SUGGESTIVE =               -328\n",
    "KeyCode AI constants",
)
write(path, s)

# Make key data instances available to Smartbar and the dropdown menu.
path = SRC / "kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TextKeyData.kt"
s = read(path)
s = replace_once(
    s,
    "                TOGGLE_AUTOCORRECT,\n",
    "                TOGGLE_AUTOCORRECT,\n"
    "                AI_CORRECT,\n"
    "                AI_STYLE_MENU,\n"
    "                AI_FRIENDLY,\n"
    "                AI_PROFESSIONAL,\n"
    "                AI_CASUAL,\n"
    "                AI_HUMOROUS,\n"
    "                AI_IRONIC,\n"
    "                AI_FLIRTY,\n"
    "                AI_SUGGESTIVE,\n"
    "                AI_ELEGANT,\n"
    "                AI_BUSINESS,\n"
    "                AI_PERSONAL,\n"
    "                AI_DU,\n"
    "                AI_SIE,\n"
    "                AI_SHORT,\n"
    "                AI_SIMPLE,\n"
    "                AI_DIRECT,\n"
    "                AI_PROMPT,\n"
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
        val AI_STYLE_MENU = TextKeyData(KeyType.FUNCTION, KeyCode.AI_STYLE_MENU, "ai_style_menu")
        val AI_FRIENDLY = TextKeyData(KeyType.FUNCTION, KeyCode.AI_FRIENDLY, "ai_friendly")
        val AI_PROFESSIONAL = TextKeyData(KeyType.FUNCTION, KeyCode.AI_PROFESSIONAL, "ai_professional")
        val AI_CASUAL = TextKeyData(KeyType.FUNCTION, KeyCode.AI_CASUAL, "ai_casual")
        val AI_HUMOROUS = TextKeyData(KeyType.FUNCTION, KeyCode.AI_HUMOROUS, "ai_humorous")
        val AI_IRONIC = TextKeyData(KeyType.FUNCTION, KeyCode.AI_IRONIC, "ai_ironic")
        val AI_FLIRTY = TextKeyData(KeyType.FUNCTION, KeyCode.AI_FLIRTY, "ai_flirty")
        val AI_SUGGESTIVE = TextKeyData(KeyType.FUNCTION, KeyCode.AI_SUGGESTIVE, "ai_suggestive")
        val AI_ELEGANT = TextKeyData(KeyType.FUNCTION, KeyCode.AI_ELEGANT, "ai_elegant")
        val AI_BUSINESS = TextKeyData(KeyType.FUNCTION, KeyCode.AI_BUSINESS, "ai_business")
        val AI_PERSONAL = TextKeyData(KeyType.FUNCTION, KeyCode.AI_PERSONAL, "ai_personal")
        val AI_DU = TextKeyData(KeyType.FUNCTION, KeyCode.AI_DU, "ai_du")
        val AI_SIE = TextKeyData(KeyType.FUNCTION, KeyCode.AI_SIE, "ai_sie")
        val AI_SHORT = TextKeyData(KeyType.FUNCTION, KeyCode.AI_SHORT, "ai_short")
        val AI_SIMPLE = TextKeyData(KeyType.FUNCTION, KeyCode.AI_SIMPLE, "ai_simple")
        val AI_DIRECT = TextKeyData(KeyType.FUNCTION, KeyCode.AI_DIRECT, "ai_direct")
        val AI_PROMPT = TextKeyData(KeyType.FUNCTION, KeyCode.AI_PROMPT, "ai_prompt")
        val AI_SETTINGS = TextKeyData(KeyType.FUNCTION, KeyCode.AI_SETTINGS, "ai_settings")
'''
s = replace_once(s, marker, addition, "TextKeyData AI definitions")
write(path, s)

# Give AI actions readable names/tooltips in the action editor.
path = SRC / "kotlin/dev/patrickgold/florisboard/ime/smartbar/quickaction/QuickAction.kt"
s = read(path)
s = replace_once(
    s,
    "            KeyCode.TOGGLE_AUTOCORRECT -> R.string.quick_action__toggle_autocorrect\n",
    "            KeyCode.TOGGLE_AUTOCORRECT -> R.string.quick_action__ai_correct\n"
    "            KeyCode.AI_CORRECT -> R.string.quick_action__ai_correct\n"
    "            KeyCode.AI_STYLE_MENU -> R.string.quick_action__ai_style_menu\n"
    "            KeyCode.AI_FRIENDLY -> R.string.quick_action__ai_friendly\n"
    "            KeyCode.AI_PROFESSIONAL -> R.string.quick_action__ai_professional\n"
    "            KeyCode.AI_CASUAL -> R.string.quick_action__ai_casual\n"
    "            KeyCode.AI_HUMOROUS -> R.string.quick_action__ai_humorous\n"
    "            KeyCode.AI_IRONIC -> R.string.quick_action__ai_ironic\n"
    "            KeyCode.AI_SHORT -> R.string.quick_action__ai_short\n"
    "            KeyCode.AI_SIMPLE -> R.string.quick_action__ai_simple\n"
    "            KeyCode.AI_DIRECT -> R.string.quick_action__ai_direct\n"
    "            KeyCode.AI_PROMPT -> R.string.quick_action__ai_prompt\n"
    "            KeyCode.AI_SETTINGS -> R.string.quick_action__ai_settings\n",
    "QuickAction display names",
)
s = replace_once(
    s,
    "            KeyCode.TOGGLE_AUTOCORRECT -> R.string.quick_action__toggle_autocorrect__tooltip\n",
    "            KeyCode.TOGGLE_AUTOCORRECT -> R.string.quick_action__ai_correct__tooltip\n"
    "            KeyCode.AI_CORRECT -> R.string.quick_action__ai_correct__tooltip\n"
    "            KeyCode.AI_STYLE_MENU -> R.string.quick_action__ai_style_menu__tooltip\n"
    "            KeyCode.AI_FRIENDLY -> R.string.quick_action__ai_friendly__tooltip\n"
    "            KeyCode.AI_PROFESSIONAL -> R.string.quick_action__ai_professional__tooltip\n"
    "            KeyCode.AI_CASUAL -> R.string.quick_action__ai_casual__tooltip\n"
    "            KeyCode.AI_HUMOROUS -> R.string.quick_action__ai_humorous__tooltip\n"
    "            KeyCode.AI_IRONIC -> R.string.quick_action__ai_ironic__tooltip\n"
    "            KeyCode.AI_SHORT -> R.string.quick_action__ai_short__tooltip\n"
    "            KeyCode.AI_SIMPLE -> R.string.quick_action__ai_simple__tooltip\n"
    "            KeyCode.AI_DIRECT -> R.string.quick_action__ai_direct__tooltip\n"
    "            KeyCode.AI_PROMPT -> R.string.quick_action__ai_prompt__tooltip\n"
    "            KeyCode.AI_SETTINGS -> R.string.quick_action__ai_settings__tooltip\n",
    "QuickAction tooltips",
)
write(path, s)

# Short Smartbar labels.
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
            KeyCode.AI_STYLE_MENU -> "Stil"
            KeyCode.AI_FRIENDLY -> "☺"
            KeyCode.AI_PROFESSIONAL -> "Pro"
            KeyCode.AI_CASUAL -> "Locker"
            KeyCode.AI_HUMOROUS -> "Humor"
            KeyCode.AI_IRONIC -> "Sark."
            KeyCode.AI_SHORT -> "Kurz"
            KeyCode.AI_SIMPLE -> "Einfach"
            KeyCode.AI_DIRECT -> "Direkt"
            KeyCode.AI_PROMPT -> "Prompt+"
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

# Keep the Smartbar compact: one correction button, one style dropdown, Prompt+ and settings.
path = SRC / "kotlin/dev/patrickgold/florisboard/ime/smartbar/quickaction/QuickActionArrangement.kt"
s = read(path)
s = replace_once(
    s,
    "                QuickAction.InsertKey(TextKeyData.TOGGLE_AUTOCORRECT),\n",
    "                QuickAction.InsertKey(TextKeyData.TOGGLE_AUTOCORRECT),\n"
    "                QuickAction.InsertKey(TextKeyData.AI_STYLE_MENU),\n"
    "                QuickAction.InsertKey(TextKeyData.AI_PROMPT),\n"
    "                QuickAction.InsertKey(TextKeyData.AI_SETTINGS),\n",
    "QuickActionArrangement defaults",
)
write(path, s)

# Wire manual correction and styles into KeyboardManager.
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
        runAiStyle(AiStyle.CORRECT)
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
    "KeyboardManager manual correction implementation",
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
    "            KeyCode.AI_FLIRTY -> runAiStyle(AiStyle.FLIRTY)\n"
    "            KeyCode.AI_SUGGESTIVE -> runAiStyle(AiStyle.SUGGESTIVE)\n"
    "            KeyCode.AI_ELEGANT -> runAiStyle(AiStyle.ELEGANT)\n"
    "            KeyCode.AI_BUSINESS -> runAiStyle(AiStyle.BUSINESS)\n"
    "            KeyCode.AI_PERSONAL -> runAiStyle(AiStyle.PERSONAL)\n"
    "            KeyCode.AI_DU -> runAiStyle(AiStyle.DU)\n"
    "            KeyCode.AI_SIE -> runAiStyle(AiStyle.SIE)\n"
    "            KeyCode.AI_SHORT -> runAiStyle(AiStyle.SHORT)\n"
    "            KeyCode.AI_SIMPLE -> runAiStyle(AiStyle.SIMPLE)\n"
    "            KeyCode.AI_DIRECT -> runAiStyle(AiStyle.DIRECT)\n"
    "            KeyCode.AI_PROMPT -> runAiStyle(AiStyle.PROMPT)\n"
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
print("Features: manual contextual correction, style dropdown, Prompt+, dynamic providers")
