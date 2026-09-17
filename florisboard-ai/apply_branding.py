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
replacement = needle + '''            Text(\n                text = "Beta v1",\n                fontSize = 14.sp,\n                fontWeight = FontWeight.Medium,\n                modifier = Modifier.padding(top = 4.dp),\n            )\n            Text(\n                text = stringRes(R.string.ki_tastatur__based_on_florisboard),\n                fontSize = 13.sp,\n                modifier = Modifier.padding(top = 2.dp),\n            )\n'''
if needle not in text:
    raise SystemExit("AboutScreen branding marker not found")
text = text.replace(needle, replacement, 1)
about.write_text(text, encoding="utf-8")

print("Visible branding changed to KI Tastatur Beta v1; FlorisBoard kept only as attribution/source reference")
