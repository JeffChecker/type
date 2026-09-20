#!/usr/bin/env python3
from pathlib import Path
import shutil

ROOT = Path("florisboard")
APP = ROOT / "app"
SRC = APP / "src/main"
CTRL = Path("controller/florisboard-ai")


def patch(path, old, new):
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"marker missing: {path}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


ai_dir = SRC / "kotlin/dev/patrickgold/florisboard/ime/ai"
ai_dir.mkdir(parents=True, exist_ok=True)
shutil.copyfile(CTRL / "LocalTranslator.kt", ai_dir / "LocalTranslator.kt")
shutil.copyfile(CTRL / "TranslationBackend.kt", ai_dir / "TranslationBackend.kt")
shutil.copyfile(CTRL / "TranslationQuickSetup.kt", ai_dir / "TranslationQuickSetup.kt")
shutil.copyfile(CTRL / "translation_strings.xml", SRC / "res/values/translation_strings.xml")

patch(
    APP / "build.gradle.kts",
    "    implementation(libs.kotlinx.serialization.json)\n",
    "    implementation(libs.kotlinx.serialization.json)\n"
    "    implementation(\"com.google.mlkit:language-id:17.0.6\")\n"
    "    implementation(\"com.google.mlkit:translate:17.0.3\")\n",
)

# -320 is Prompt+, therefore translation gets its own unique code -329.
patch(
    SRC / "kotlin/dev/patrickgold/florisboard/ime/text/key/KeyCode.kt",
    "    const val AI_SETTINGS =                 -319\n",
    "    const val AI_SETTINGS =                 -319\n"
    "    const val AI_TRANSLATE =                -329\n",
)

path = SRC / "kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TextKeyData.kt"
patch(path, "                AI_SETTINGS,\n", "                AI_SETTINGS,\n                AI_TRANSLATE,\n")
patch(
    path,
    "        val AI_SETTINGS = TextKeyData(KeyType.FUNCTION, KeyCode.AI_SETTINGS, \"ai_settings\")\n",
    "        val AI_SETTINGS = TextKeyData(KeyType.FUNCTION, KeyCode.AI_SETTINGS, \"ai_settings\")\n"
    "        val AI_TRANSLATE = TextKeyData(KeyType.FUNCTION, KeyCode.AI_TRANSLATE, \"ai_translate\")\n",
)

path = SRC / "kotlin/dev/patrickgold/florisboard/ime/smartbar/quickaction/QuickAction.kt"
patch(
    path,
    "            KeyCode.AI_SETTINGS -> R.string.quick_action__ai_settings\n",
    "            KeyCode.AI_SETTINGS -> R.string.quick_action__ai_settings\n"
    "            KeyCode.AI_TRANSLATE -> R.string.quick_action__translate\n",
)
patch(
    path,
    "            KeyCode.AI_SETTINGS -> R.string.quick_action__ai_settings__tooltip\n",
    "            KeyCode.AI_SETTINGS -> R.string.quick_action__ai_settings__tooltip\n"
    "            KeyCode.AI_TRANSLATE -> R.string.quick_action__translate__tooltip\n",
)

patch(
    SRC / "kotlin/dev/patrickgold/florisboard/ime/keyboard/ComputingEvaluator.kt",
    "            KeyCode.AI_DIRECT -> \"Direkt\"\n",
    "            KeyCode.AI_DIRECT -> \"Direkt\"\n"
    "            KeyCode.AI_TRANSLATE -> \"Übers.\"\n",
)

patch(
    SRC / "kotlin/dev/patrickgold/florisboard/ime/smartbar/quickaction/QuickActionArrangement.kt",
    "                QuickAction.InsertKey(TextKeyData.AI_SETTINGS),\n",
    "                QuickAction.InsertKey(TextKeyData.AI_SETTINGS),\n"
    "                QuickAction.InsertKey(TextKeyData.AI_TRANSLATE),\n",
)

path = SRC / "kotlin/dev/patrickgold/florisboard/ime/keyboard/KeyboardManager.kt"
patch(
    path,
    "import dev.patrickgold.florisboard.ime.ai.AiStyle\n",
    "import dev.patrickgold.florisboard.ime.ai.AiStyle\n"
    "import dev.patrickgold.florisboard.ime.ai.LocalTranslator\n",
)
patch(
    path,
    "    private val aiAssistant = AiAssistant(context)\n",
    "    private val aiAssistant = AiAssistant(context)\n"
    "    private val localTranslator = LocalTranslator(context)\n",
)
patch(
    path,
    "            KeyCode.AI_SETTINGS -> aiAssistant.openSettings()\n",
    "            KeyCode.AI_SETTINGS -> aiAssistant.openSettings()\n"
    "            KeyCode.AI_TRANSLATE -> localTranslator.translateTo(\n"
    "                targetLocale = subtypeManager.activeSubtype.primaryLocale,\n"
    "                sensitiveField = activeState.keyVariation != KeyVariation.NORMAL,\n"
    "                incognito = activeState.isIncognitoMode,\n"
    "                rawEditor = editorInstance.activeInfo.isRawInputEditor,\n"
    "            )\n",
)

print("Hybrid Google Cloud + ML Kit translation integrated with dynamic language list")
