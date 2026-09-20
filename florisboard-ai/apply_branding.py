#!/usr/bin/env python3
from pathlib import Path

ROOT = Path("florisboard")
SRC = ROOT / "app/src/main"
RES = SRC / "res"

# Rename the visible app brand in the German/default UI resources.
for rel in ["values/strings.xml", "values-de/strings.xml"]:
    path = RES / rel
    text = path.read_text(encoding="utf-8")
    text = text.replace("FlorisBoard", "KI Tastatur")
    attribution = '    <string name="ki_tastatur__based_on_florisboard">Basierend auf FlorisBoard • Apache 2.0</string>\n'
    if "ki_tastatur__based_on_florisboard" not in text:
        text = text.replace("</resources>", attribution + "</resources>")
    path.write_text(text, encoding="utf-8")

# Keep the original repository/license URLs untouched, but show a clear attribution in About.
about = SRC / "kotlin/dev/patrickgold/florisboard/app/settings/about/AboutScreen.kt"
text = about.read_text(encoding="utf-8")
text = text.replace('contentDescription = "FlorisBoard app icon",', 'contentDescription = "KI Tastatur App Symbol",')
needle = '''            Text(\n                text = stringRes(R.string.floris_app_name),\n                fontSize = 24.sp,\n                fontWeight = FontWeight.SemiBold,\n                modifier = Modifier.padding(top = 16.dp),\n            )\n'''
replacement = needle + '''            Text(\n                text = "Beta v3",\n                fontSize = 14.sp,\n                fontWeight = FontWeight.Medium,\n                modifier = Modifier.padding(top = 4.dp),\n            )\n            Text(\n                text = stringRes(R.string.ki_tastatur__based_on_florisboard),\n                fontSize = 13.sp,\n                modifier = Modifier.padding(top = 2.dp),\n            )\n'''
if needle not in text:
    raise SystemExit("AboutScreen branding marker not found")
text = text.replace(needle, replacement, 1)
about.write_text(text, encoding="utf-8")

# New visual identity: keyboard + AI sparkle. Keep original package/resource aliases,
# but replace the release adaptive icon with our own artwork.
(RES / "values" / "ki_tastatur_icon.xml").write_text('''<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ki_tastatur_icon_background">#171B3A</color>\n</resources>\n''', encoding="utf-8")

foreground = '''<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="108dp"\n    android:height="108dp"\n    android:viewportWidth="108"\n    android:viewportHeight="108">\n    <path android:fillColor="#FFFFFFFF" android:pathData="M28,50 L80,50 L80,76 L28,76 Z"/>\n    <path android:fillColor="#FF171B3A" android:pathData="M33,55 L40,55 L40,61 L33,61 Z M43,55 L50,55 L50,61 L43,61 Z M53,55 L60,55 L60,61 L53,61 Z M63,55 L70,55 L70,61 L63,61 Z M73,55 L76,55 L76,61 L73,61 Z M33,64 L40,64 L40,70 L33,70 Z M43,64 L50,64 L50,70 L43,70 Z M53,64 L60,64 L60,70 L53,70 Z M63,64 L76,64 L76,70 L63,70 Z"/>\n    <path android:fillColor="#FF7C5CFF" android:pathData="M54,27 L58,35 L66,39 L58,43 L54,51 L50,43 L42,39 L50,35 Z"/>\n    <path android:fillColor="#FF51D7FF" android:pathData="M72,31 L74,35 L78,37 L74,39 L72,43 L70,39 L66,37 L70,35 Z"/>\n    <path android:fillColor="#FF51D7FF" android:pathData="M36,34 L38,38 L42,40 L38,42 L36,46 L34,42 L30,40 L34,38 Z"/>\n</vector>\n'''
(RES / "drawable" / "ki_tastatur_icon_foreground.xml").write_text(foreground, encoding="utf-8")

monochrome = '''<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="108dp"\n    android:height="108dp"\n    android:viewportWidth="108"\n    android:viewportHeight="108">\n    <path android:fillColor="#FFFFFFFF" android:pathData="M28,50 L80,50 L80,76 L28,76 Z M54,27 L58,35 L66,39 L58,43 L54,51 L50,43 L42,39 L50,35 Z"/>\n</vector>\n'''
(RES / "drawable" / "ki_tastatur_icon_monochrome.xml").write_text(monochrome, encoding="utf-8")

adaptive = '''<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@color/ki_tastatur_icon_background"/>\n    <foreground android:drawable="@drawable/ki_tastatur_icon_foreground"/>\n    <monochrome android:drawable="@drawable/ki_tastatur_icon_monochrome"/>\n</adaptive-icon>\n'''
for name in ["ic_app_icon_stable.xml", "ic_app_icon_stable_round.xml"]:
    (RES / "mipmap-anydpi-v26" / name).write_text(adaptive, encoding="utf-8")

print("Visible branding changed to KI Tastatur Beta v3 with new AI keyboard icon; FlorisBoard kept only as attribution/source reference")
