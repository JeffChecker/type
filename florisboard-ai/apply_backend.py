#!/usr/bin/env python3
from pathlib import Path
import shutil

ROOT = Path("florisboard")
SRC = ROOT / "app/src/main"
CTRL = Path("controller/florisboard-ai")
ai_dir = SRC / "kotlin/dev/patrickgold/florisboard/ime/ai"
ai_dir.mkdir(parents=True, exist_ok=True)
shutil.copyfile(CTRL / "AiBackend.kt", ai_dir / "AiBackend.kt")

# Apply shared writing rules to every provider and every style. The goal is
# natural human sounding text instead of recognisable generic AI prose.
backend = ai_dir / "AiBackend.kt"
s = backend.read_text(encoding="utf-8")
old = '''        append("Behalte die Sprache des Eingabetextes bei und schreibe natürlich. ")
        append(style.instruction)
'''
new = '''        append("Behalte die Sprache des Eingabetextes bei und schreibe natürlich. ")
        append("Der fertige Text soll wie von einer echten Person geschrieben wirken und nicht nach KI klingen. ")
        append("Vermeide typische KI Floskeln, künstliche Einleitungen, übertriebene Höflichkeit, unnötige Wiederholungen und schematische Zusammenfassungen. ")
        append("Vermeide Gedankenstriche und Bindestrich Konstruktionen, sofern sie sprachlich nicht zwingend erforderlich sind. Nutze lieber normale, natürlich fließende Sätze. ")
        append("Erhalte den persönlichen Ton, die Wortwahl und die Absicht des Ausgangstextes, soweit der gewählte Stil nichts anderes verlangt. ")
        append("Schreibe abwechslungsreich und idiomatisch, nicht steril, werblich oder überperfekt. ")
        append("Gib ausschließlich den fertigen bearbeiteten Text aus. Keine Vorbemerkung, keine Erklärung, keine Anführungszeichen und keine Analyse. ")
        append("Nutze keine künstlichen Überschriften oder Listen, außer der Ausgangstext oder die gewählte Aufgabe verlangt sie ausdrücklich. ")
        append(style.instruction)
'''
if old not in s:
    raise SystemExit("AiBackend prompt marker not found")
s = s.replace(old, new, 1)
backend.write_text(s, encoding="utf-8")

print("Dynamic OpenAI, Gemini, Claude and Groq backend copied")
print("Shared human-writing rules applied to every AI style")
