#!/usr/bin/env python3
from pathlib import Path
import shutil

ROOT = Path("florisboard")
APP = ROOT / "app"
SRC = APP / "src/main"
CTRL = Path("controller/florisboard-ai")


def patch(path: Path, old: str, new: str, label: str):
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"marker missing: {label} in {path}")
    if text.count(old) != 1:
        raise SystemExit(f"marker not unique ({text.count(old)}): {label} in {path}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


ai_dir = SRC / "kotlin/dev/patrickgold/florisboard/ime/ai"
ai_dir.mkdir(parents=True, exist_ok=True)
shutil.copyfile(CTRL / "ScreenContextAccess.kt", ai_dir / "ScreenContextAccess.kt")
shutil.copyfile(CTRL / "ScreenTranslationDisclosure.kt", ai_dir / "ScreenTranslationDisclosure.kt")

xml_dir = SRC / "res/xml"
xml_dir.mkdir(parents=True, exist_ok=True)
(xml_dir / "ai_screen_context_accessibility.xml").write_text(
    '''<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/screen_context_service_description"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100"
    android:canRetrieveWindowContent="true"
    android:accessibilityFlags="flagRetrieveInteractiveWindows|flagReportViewIds"
    android:isAccessibilityTool="false" />
''',
    encoding="utf-8",
)

(SRC / "res/values/screen_context_strings.xml").write_text(
    '''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="screen_context_service_description">Liest nur nach Tippen auf Antwort den aktuell sichtbaren zugänglichen Text, damit KI Tastatur eine passende Antwort formulieren kann.</string>
    <string name="quick_action__ai_screen_reply">Antwort vorschlagen</string>
    <string name="quick_action__ai_screen_reply__tooltip">Sichtbaren Gesprächskontext lesen und eine passende Antwort formulieren</string>
</resources>
''',
    encoding="utf-8",
)

# New action code. Translation already uses -329.
path = SRC / "kotlin/dev/patrickgold/florisboard/ime/text/key/KeyCode.kt"
patch(
    path,
    "    const val AI_TRANSLATE =                -329\n",
    "    const val AI_TRANSLATE =                -329\n"
    "    const val AI_SCREEN_REPLY =             -330\n",
    "screen reply key code",
)

path = SRC / "kotlin/dev/patrickgold/florisboard/ime/text/keyboard/TextKeyData.kt"
patch(
    path,
    "                AI_TRANSLATE,\n",
    "                AI_TRANSLATE,\n"
    "                AI_SCREEN_REPLY,\n",
    "screen reply internal key",
)
patch(
    path,
    "        val AI_TRANSLATE = TextKeyData(KeyType.FUNCTION, KeyCode.AI_TRANSLATE, \"ai_translate\")\n",
    "        val AI_TRANSLATE = TextKeyData(KeyType.FUNCTION, KeyCode.AI_TRANSLATE, \"ai_translate\")\n"
    "        val AI_SCREEN_REPLY = TextKeyData(KeyType.FUNCTION, KeyCode.AI_SCREEN_REPLY, \"ai_screen_reply\")\n",
    "screen reply key data",
)

path = SRC / "kotlin/dev/patrickgold/florisboard/ime/smartbar/quickaction/QuickAction.kt"
patch(
    path,
    "            KeyCode.AI_TRANSLATE -> R.string.quick_action__translate\n",
    "            KeyCode.AI_TRANSLATE -> R.string.quick_action__translate\n"
    "            KeyCode.AI_SCREEN_REPLY -> R.string.quick_action__ai_screen_reply\n",
    "screen reply action name",
)
patch(
    path,
    "            KeyCode.AI_TRANSLATE -> R.string.quick_action__translate__tooltip\n",
    "            KeyCode.AI_TRANSLATE -> R.string.quick_action__translate__tooltip\n"
    "            KeyCode.AI_SCREEN_REPLY -> R.string.quick_action__ai_screen_reply__tooltip\n",
    "screen reply action tooltip",
)

path = SRC / "kotlin/dev/patrickgold/florisboard/ime/keyboard/ComputingEvaluator.kt"
patch(
    path,
    "            KeyCode.AI_TRANSLATE -> \"Übers.\"\n",
    "            KeyCode.AI_TRANSLATE -> \"Übers.\"\n"
    "            KeyCode.AI_SCREEN_REPLY -> \"Antwort\"\n",
    "screen reply compact label",
)

path = SRC / "kotlin/dev/patrickgold/florisboard/ime/smartbar/quickaction/QuickActionArrangement.kt"
patch(
    path,
    "                QuickAction.InsertKey(TextKeyData.AI_TRANSLATE),\n",
    "                QuickAction.InsertKey(TextKeyData.AI_TRANSLATE),\n"
    "                QuickAction.InsertKey(TextKeyData.AI_SCREEN_REPLY),\n",
    "screen reply default action",
)

path = SRC / "kotlin/dev/patrickgold/florisboard/ime/keyboard/KeyboardManager.kt"
patch(
    path,
    "            KeyCode.AI_TRANSLATE -> localTranslator.translateTo(\n",
    "            KeyCode.AI_SCREEN_REPLY -> aiAssistant.suggestReplyFromScreen(\n"
    "                sensitiveField = activeState.keyVariation != KeyVariation.NORMAL,\n"
    "                incognito = activeState.isIncognitoMode,\n"
    "                rawEditor = editorInstance.activeInfo.isRawInputEditor,\n"
    "            )\n"
    "            KeyCode.AI_TRANSLATE -> localTranslator.translateTo(\n",
    "screen reply keyboard handling",
)

path = SRC / "AndroidManifest.xml"
patch(
    path,
    "        <!-- Main App Activity -->\n",
    '''        <activity
            android:name="dev.patrickgold.florisboard.ime.ai.ScreenContextDisclosureActivity"
            android:label="Bildschirmkontext"
            android:exported="false"
            android:theme="@style/FlorisAppTheme"/>

        <activity
            android:name="dev.patrickgold.florisboard.ime.ai.ScreenTranslationDisclosureActivity"
            android:label="Automatische Gesprächsübersetzung"
            android:exported="false"
            android:theme="@style/FlorisAppTheme"/>

        <service
            android:name="dev.patrickgold.florisboard.ime.ai.ScreenContextAccessibilityService"
            android:label="KI Tastatur Bildschirmkontext"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService"/>
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/ai_screen_context_accessibility"/>
        </service>

        <!-- Main App Activity -->
''',
    "screen context manifest components",
)

print("Screen context reply feature integrated")
print("Behavior: explicit button only, disclosure + consent, no background history")
