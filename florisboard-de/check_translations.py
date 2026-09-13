#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path("florisboard/app/src/main/res")
en_file = root / "values" / "strings.xml"
de_file = root / "values-de" / "strings.xml"

def key(el):
    return (el.tag, el.attrib.get("name", ""))

def text_value(el):
    return "".join(el.itertext()).strip()

en_root = ET.parse(en_file).getroot()
de_root = ET.parse(de_file).getroot()

en = {key(e): e for e in en_root if e.attrib.get("name")}
de = {key(e): e for e in de_root if e.attrib.get("name")}

missing = []
same = []
for k, e in en.items():
    d = de.get(k)
    if d is None:
        missing.append((k, text_value(e)))
    elif text_value(e) == text_value(d) and any(ch.isalpha() for ch in text_value(e)):
        same.append((k, text_value(e)))

print(f"DEFAULT_KEYS={len(en)}")
print(f"GERMAN_KEYS={len(de)}")
print(f"MISSING_KEYS={len(missing)}")
print("--- MISSING ---")
for (tag, name), value in missing:
    print(f"{tag}\t{name}\t{value}")
print(f"IDENTICAL_KEYS={len(same)}")
print("--- IDENTICAL EN/DE ---")
for (tag, name), value in same:
    print(f"{tag}\t{name}\t{value}")
