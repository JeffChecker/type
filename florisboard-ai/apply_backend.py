#!/usr/bin/env python3
from pathlib import Path
import shutil

ROOT = Path("florisboard")
SRC = ROOT / "app/src/main"
CTRL = Path("controller/florisboard-ai")
ai_dir = SRC / "kotlin/dev/patrickgold/florisboard/ime/ai"
ai_dir.mkdir(parents=True, exist_ok=True)

# Provider backends and official OpenAI "Sign in with ChatGPT" support are kept
# as normal source files. Prompt rules live in AiBackend.kt itself so there is
# only one source of truth for every provider.
shutil.copyfile(CTRL / "AiBackend.kt", ai_dir / "AiBackend.kt")
shutil.copyfile(CTRL / "ChatGptPlanAuth.kt", ai_dir / "ChatGptPlanAuth.kt")

print("Dynamic OpenAI, Gemini, Claude and Groq backend copied")
print("Official ChatGPT plan OAuth support copied")
print("Shared semantic writing rules loaded from AiBackend.kt")
