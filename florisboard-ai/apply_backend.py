#!/usr/bin/env python3
from pathlib import Path
import shutil

ROOT = Path("florisboard")
SRC = ROOT / "app/src/main"
CTRL = Path("controller/florisboard-ai")
ai_dir = SRC / "kotlin/dev/patrickgold/florisboard/ime/ai"
ai_dir.mkdir(parents=True, exist_ok=True)
shutil.copyfile(CTRL / "AiBackend.kt", ai_dir / "AiBackend.kt")
print("Dynamic OpenAI, Gemini, Claude and Groq backend copied")
