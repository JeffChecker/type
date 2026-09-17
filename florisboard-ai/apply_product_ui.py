#!/usr/bin/env python3
from pathlib import Path
import shutil

ROOT = Path("florisboard")
APP = ROOT / "app"
SRC = APP / "src/main"
RES = SRC / "res"
CTRL = Path("controller/florisboard-ai")


def replace_once(path: Path, old: str, new: str, label: str):
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Patch marker not found: {label}")
    if text.count(old) != 1:
        raise SystemExit(f"Patch marker not unique ({text.count(old)}): {label}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def replace_all(path: Path, old: str, new: str, label: str):
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count < 1:
        raise SystemExit(f"Patch marker not found: {label}")
    path.write_text(text.replace(old, new), encoding="utf-8")
    print(f"{label}: replaced {count} occurrence(s)")


# 1) Copy the dedicated AI toolbar into the actual app sources.
ai_dir = SRC / "kotlin/dev/patrickgold/florisboard/ime/ai"
ai_dir.mkdir(parents=True, exist_ok=True)
shutil.copyfile(CTRL / "AiToolbar.kt", ai_dir / "AiToolbar.kt")

# 2) Make the AI toolbar a real, fixed product UI element above the old Smartbar.
smartbar = SRC / "kotlin/dev/patrickgold/florisboard/ime/smartbar/Smartbar.kt"
replace_once(
    smartbar,
    "import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing\n",
    "import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing\n"
    "import dev.patrickgold.florisboard.ime.ai.AiToolbar\n",
    "Smartbar AiToolbar import",
)

old_fun = '''@Composable
fun Smartbar() {
    val prefs by FlorisPreferenceStore
    val smartbarEnabled by prefs.smartbar.enabled.observeAsState()
    val extendedActionsPlacement by prefs.smartbar.extendedActionsPlacement.observeAsState()

    AnimatedVisibility(
        visible = smartbarEnabled,
        enter = VerticalEnterTransition,
        exit = VerticalExitTransition,
    ) {
        when (extendedActionsPlacement) {
            ExtendedActionsPlacement.ABOVE_CANDIDATES -> {
                SnyggColumn(FlorisImeUi.Smartbar.elementName) {
                    SmartbarSecondaryRow()
                    SmartbarMainRow()
                }
            }

            ExtendedActionsPlacement.BELOW_CANDIDATES -> {
                SnyggColumn(FlorisImeUi.Smartbar.elementName) {
                    SmartbarMainRow()
                    SmartbarSecondaryRow()
                }
            }

            ExtendedActionsPlacement.OVERLAY_APP_UI -> {
                SnyggBox(FlorisImeUi.Smartbar.elementName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FlorisImeSizing.smartbarHeight),
                    allowClip = false,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(FlorisImeSizing.smartbarHeight * 2)
                            .absoluteOffset(y = -FlorisImeSizing.smartbarHeight),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        SmartbarSecondaryRow()
                    }
                    SmartbarMainRow()
                }
            }
        }
    }
}
'''

new_fun = '''@Composable
fun Smartbar() {
    val prefs by FlorisPreferenceStore
    val smartbarEnabled by prefs.smartbar.enabled.observeAsState()
    val extendedActionsPlacement by prefs.smartbar.extendedActionsPlacement.observeAsState()

    // KI Tastatur has its own product toolbar. It is intentionally independent
    // from the user's legacy FlorisBoard quick-action arrangement, so updates
    // cannot hide newly added KI functions behind old stored preferences.
    SnyggColumn(FlorisImeUi.Smartbar.elementName) {
        AiToolbar()

        AnimatedVisibility(
            visible = smartbarEnabled,
            enter = VerticalEnterTransition,
            exit = VerticalExitTransition,
        ) {
            when (extendedActionsPlacement) {
                ExtendedActionsPlacement.ABOVE_CANDIDATES -> {
                    SnyggColumn(FlorisImeUi.Smartbar.elementName) {
                        SmartbarSecondaryRow()
                        SmartbarMainRow()
                    }
                }

                ExtendedActionsPlacement.BELOW_CANDIDATES -> {
                    SnyggColumn(FlorisImeUi.Smartbar.elementName) {
                        SmartbarMainRow()
                        SmartbarSecondaryRow()
                    }
                }

                ExtendedActionsPlacement.OVERLAY_APP_UI -> {
                    SnyggBox(FlorisImeUi.Smartbar.elementName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(FlorisImeSizing.smartbarHeight),
                        allowClip = false,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(FlorisImeSizing.smartbarHeight * 2)
                                .absoluteOffset(y = -FlorisImeSizing.smartbarHeight),
                            contentAlignment = Alignment.BottomStart,
                        ) {
                            SmartbarSecondaryRow()
                        }
                        SmartbarMainRow()
                    }
                }
            }
        }
    }
}
'''
replace_once(smartbar, old_fun, new_fun, "Smartbar dedicated AI toolbar")

# 3) Provide a complete launcher icon for all Android resource paths, not only
# adaptive-icon devices. This makes the visual change visible on launchers too.
(RES / "mipmap-anydpi").mkdir(parents=True, exist_ok=True)
(RES / "mipmap-anydpi-v26").mkdir(parents=True, exist_ok=True)

legacy_icon = '''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="#FF111827" android:pathData="M0,0 H108 V108 H0 Z"/>
    <path android:fillColor="#FFFFFFFF" android:pathData="M22,48 H86 V79 H22 Z"/>
    <path android:fillColor="#FF111827" android:pathData="M28,54 H36 V61 H28 Z M39,54 H47 V61 H39 Z M50,54 H58 V61 H50 Z M61,54 H69 V61 H61 Z M72,54 H80 V61 H72 Z M28,65 H36 V72 H28 Z M39,65 H47 V72 H39 Z M50,65 H58 V72 H50 Z M61,65 H80 V72 H61 Z"/>
    <path android:fillColor="#FF7C5CFF" android:pathData="M54,20 L59,31 L70,36 L59,41 L54,52 L49,41 L38,36 L49,31 Z"/>
    <path android:fillColor="#FF51D7FF" android:pathData="M78,25 L81,31 L87,34 L81,37 L78,43 L75,37 L69,34 L75,31 Z"/>
</vector>
'''
for name in ["ki_tastatur_icon.xml", "ki_tastatur_icon_round.xml"]:
    (RES / "mipmap-anydpi" / name).write_text(legacy_icon, encoding="utf-8")

adaptive = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ki_tastatur_icon_background"/>
    <foreground android:drawable="@drawable/ki_tastatur_icon_foreground"/>
    <monochrome android:drawable="@drawable/ki_tastatur_icon_monochrome"/>
</adaptive-icon>
'''
for name in ["ki_tastatur_icon.xml", "ki_tastatur_icon_round.xml"]:
    (RES / "mipmap-anydpi-v26" / name).write_text(adaptive, encoding="utf-8")

# 4) Point all aliases that were normalized to the old stable FlorisBoard icon
# at the KI Tastatur identity. make_german.py intentionally normalizes debug
# resources to stable first, so there can be more than one matching line here.
gradle = APP / "build.gradle.kts"
replace_all(
    gradle,
    '@mipmap/ic_app_icon_stable")',
    '@mipmap/ki_tastatur_icon")',
    "launcher icon aliases",
)
replace_all(
    gradle,
    '@mipmap/ic_app_icon_stable_round")',
    '@mipmap/ki_tastatur_icon_round")',
    "round launcher icon aliases",
)
replace_all(
    gradle,
    '@drawable/ic_app_icon_stable_foreground")',
    '@drawable/ki_tastatur_icon_foreground")',
    "launcher foreground aliases",
)

print("KI Tastatur product UI integrated: fixed AI toolbar + full launcher icon branding")
