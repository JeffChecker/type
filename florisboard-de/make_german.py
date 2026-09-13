#!/usr/bin/env python3
from pathlib import Path
from xml.sax.saxutils import escape
import shutil
import xml.etree.ElementTree as ET

RES = Path("florisboard/app/src/main/res")
EN = RES / "values" / "strings.xml"
DE = RES / "values-de" / "strings.xml"
GRADLE = Path("florisboard/app/build.gradle.kts")

T = {
    "quick_action__ime_hide_ui": "Tastatur ausblenden",
    "quick_action__ime_hide_ui__tooltip": "Tastatur ausblenden",
    "quick_action__forward_delete": "Vorwärts löschen",
    "quick_action__language_switch": "Sprache wechseln",
    "settings__localization__display_keyboard_labels_in_subtype_language": "Tastaturbeschriftungen in der Sprache des Untertyps anzeigen",
    "settings__theme_editor__rule_modes": "Zielmodi (Ebenen)",
    "settings__theme_editor__rule_shift_states": "Zielzustände der Umschalttaste",
    "settings__theme_editor__component_meta_material_you__title": "Material You",
    "settings__theme_editor__stylesheet_error_description": "{app_name} kann versuchen, das Stylesheet tolerant zu laden und fehlende Schemata oder Regeln hinzuzufügen sowie ungültige Regeln, Eigenschaften oder Werte zu entfernen. Soll {app_name} diese Änderungen anwenden?",
    "settings__theme_editor__component_meta_material_you__palette_style": "Palettenstil",
    "settings__theme_editor__component_meta_material_you__color_contrast": "Farbkontrast",
    "settings__theme_editor__component_meta_material_you__spec_version": "Spezifikationsversion",
    "snygg__rule_annotation__defines_description": "Definiere innerhalb dieser Regel Variablen, um häufig verwendete Farben oder Größen im Stylesheet wiederzuverwenden.",
    "snygg__rule_element__key_popup_box": "Tasten Popup Feld",
    "snygg__rule_element__key_popup_element": "Tasten Popup Element",
    "snygg__rule_element__key_popup_extended_indicator": "Erweiterungsanzeige des Tasten Popups",
    "snygg__rule_element__clipboard_filter_row": "Filterzeile der Zwischenablage",
    "snygg__rule_element__clipboard_filter_chip": "Filterauswahl der Zwischenablage",
    "snygg__rule_element__clipboard_filter_chip_icon": "Symbol der Zwischenablage Filterauswahl",
    "snygg__rule_element__clipboard_filter_chip_text": "Text der Zwischenablage Filterauswahl",
    "snygg__rule_element__clipboard_grid": "Raster der Zwischenablage",
    "snygg__rule_element__clipboard_item_description": "Beschreibung des Zwischenablageeintrags",
    "snygg__rule_element__clipboard_item_timestamp": "Zeitstempel des Zwischenablageeintrags",
    "snygg__rule_element__clipboard_item_actions": "Aktionen des Zwischenablageeintrags",
    "snygg__rule_element__clipboard_item_action": "Aktion des Zwischenablageeintrags",
    "snygg__rule_element__clipboard_item_action_icon": "Symbol der Zwischenablageaktion",
    "snygg__rule_element__clipboard_item_action_text": "Text der Zwischenablageaktion",
    "snygg__rule_element__clipboard_clear_all_dialog": "Dialog zum vollständigen Löschen der Zwischenablage",
    "snygg__rule_element__clipboard_clear_all_dialog_message": "Meldung im Dialog zum vollständigen Löschen der Zwischenablage",
    "snygg__rule_element__clipboard_clear_all_dialog_buttons": "Schaltflächen im Dialog zum vollständigen Löschen der Zwischenablage",
    "snygg__rule_element__clipboard_clear_all_dialog_button": "Schaltfläche im Dialog zum vollständigen Löschen der Zwischenablage",
    "snygg__rule_element__clipboard_history_disabled_title": "Titel bei deaktiviertem Zwischenablageverlauf",
    "snygg__rule_element__clipboard_history_locked_title": "Titel bei gesperrtem Zwischenablageverlauf",
    "snygg__rule_element__clipboard_history_locked_message": "Meldung bei gesperrtem Zwischenablageverlauf",
    "snygg__rule_element__inline_autofill_chip": "Inline Schaltfläche zum automatischen Ausfüllen",
    "snygg__rule_element__media_emoji_subheader": "Emoji Unterüberschrift im Medienbereich",
    "snygg__rule_element__media_emoji_key": "Emoji Taste im Medienbereich",
    "snygg__rule_element__media_emoji_key_popup_box": "Popup Feld der Emoji Taste im Medienbereich",
    "snygg__rule_element__media_emoji_key_popup_element": "Popup Element der Emoji Taste im Medienbereich",
    "snygg__rule_element__media_emoji_key_popup_extended_indicator": "Erweiterungsanzeige des Emoji Tasten Popups im Medienbereich",
    "snygg__rule_element__media_emoji_tab": "Emoji Registerkarte im Medienbereich",
    "snygg__rule_element__media_bottom_row": "Untere Zeile im Medienbereich",
    "snygg__rule_element__media_bottom_row_button": "Schaltfläche der unteren Zeile im Medienbereich",
    "snygg__rule_element__one_handed_panel_button": "Schaltfläche des Einhandmodus",
    "snygg__rule_element__smartbar_action_tile_icon": "Symbol der Smartbar Aktionskachel",
    "snygg__rule_element__smartbar_action_tile_text": "Text der Smartbar Aktionskachel",
    "snygg__rule_element__smartbar_actions_editor_header_button": "Kopfschaltfläche des Smartbar Aktionseditors",
    "snygg__rule_element__smartbar_actions_editor_tile_grid": "Kachelraster des Smartbar Aktionseditors",
    "snygg__rule_element__smartbar_actions_editor_tile": "Kachel des Smartbar Aktionseditors",
    "snygg__rule_element__smartbar_candidate_word_text": "Text des Smartbar Wortvorschlags",
    "snygg__rule_element__smartbar_candidate_word_secondary_text": "Zusatztext des Smartbar Wortvorschlags",
    "snygg__rule_element__smartbar_candidate_clip_icon": "Symbol des Smartbar Zwischenablagevorschlags",
    "snygg__rule_element__smartbar_candidate_clip_text": "Text des Smartbar Zwischenablagevorschlags",
    "snygg__rule_element__subtype_panel": "Untertyp Auswahl",
    "snygg__rule_element__subtype_panel_header": "Kopfbereich der Untertyp Auswahl",
    "snygg__rule_element__subtype_panel_list": "Liste der Untertyp Auswahl",
    "snygg__rule_element__subtype_panel_list_item": "Listeneintrag der Untertyp Auswahl",
    "snygg__rule_element__subtype_panel_list_item_icon_leading": "Vorangestelltes Symbol des Untertyp Listeneintrags",
    "snygg__rule_element__subtype_panel_list_item_text": "Text des Untertyp Listeneintrags",
    "snygg__property_name__content_scale": "Inhaltsskalierung",
    "snygg__property_name__margin": "Außenabstand",
    "snygg__property_name__padding": "Innenabstand",
    "snygg__property_name__shadow_color": "Schattenfarbe",
    "snygg__property_name__clip": "Beschneiden",
    "snygg__property_name__src": "Quelle",
    "snygg__property_name__text_align": "Textausrichtung",
    "snygg__property_name__text_decoration_line": "Textdekoration",
    "snygg__property_name__text_max_lines": "Maximale Textzeilen",
    "snygg__property_name__text_overflow": "Textüberlauf",
    "snygg__property_value__font_family_generic": "Schriftfamilie (generisch)",
    "snygg__property_value__font_family_custom": "Schriftfamilie (benutzerdefiniert)",
    "snygg__property_value__font_weight": "Schriftstärke",
    "snygg__property_value__padding": "Innen oder Außenabstand",
    "snygg__property_value__content_scale": "Inhaltsskalierung",
    "snygg__property_value__text_align": "Textausrichtung",
    "snygg__property_value__text_decoration_line": "Textdekoration",
    "snygg__property_value__text_max_lines": "Maximale Textzeilen",
    "snygg__property_value__text_overflow": "Textüberlauf",
    "snygg__property_value__uri": "URI Verweis",
    "pref__suggestion__block_possibly_offensive__summary": "Verhindert, dass möglicherweise anstößige Wörter während der Eingabe vorgeschlagen werden",
    "pref__suggestion__api30_inline_suggestions_enabled__label": "Vorschläge zum automatischen Ausfüllen des Systems",
    "physical_keyboard__title": "Physische Tastatur",
    "physical_keyboard__system_settings__title": "Systemeinstellungen für die physische Tastatur",
    "physical_keyboard__system_settings__summary": "Layouts, Tastenkombinationen und Sondertasten",
    "physical_keyboard__system_settings__summary_not_attached": "Nur verfügbar, wenn eine physische Tastatur angeschlossen ist",
    "physical_keyboard__show_on_screen_keyboard__title": "Bildschirmtastatur anzeigen",
    "physical_keyboard__show_on_screen_keyboard__summary": "Bildschirmtastatur bei Verwendung einer physischen Tastatur anzeigen",
    "crash_dialog__bug_report_template": "Absturzbericht",
    "clipboard__confirm_clear_unfiltered_history__message": "Möchtest du wirklich den gesamten Zwischenablageverlauf löschen? Dies betrifft alle Einträge außer angehefteten.",
    "clipboard__confirm_clear_filtered_history__message": "Möchtest du wirklich den Zwischenablageverlauf löschen? Dies betrifft alle aktuell gefilterten Einträge außer angehefteten.",
    "pref__clipboard__num_history_grid_columns__label": "Anzahl der Rasterspalten",
    "pref__clipboard__history_hide_on_paste__label": "Verlauf nach dem Einfügen ausblenden",
    "pref__clipboard__history_hide_on_next_text_field__label": "Verlauf beim nächsten Textfeld ausblenden",
    "pref__clipboard__clear_primary_clip_affects_history_if_unpinned__label": "Löschen des primären Zwischenablageinhalts wirkt sich auf den Verlauf aus",
    "pref__clipboard__clear_primary_clip_affects_history_if_unpinned__summary": "Beim Löschen des primären Zwischenablageinhalts wird er auch aus dem Verlauf entfernt, sofern er nicht angeheftet ist",
    "general__auto": "Automatisch",
    "enum__clipboard_sync_behavior__no_events": "Keine Ereignisse",
    "enum__clipboard_sync_behavior__no_events__description": "In dieser Richtung werden keine Ereignisse synchronisiert. In diesem Modus wird Textinhalt von der Synchronisierung ausgeschlossen.",
    "enum__clipboard_sync_behavior__only_clear_events": "Nur Löschereignisse",
    "enum__clipboard_sync_behavior__only_clear_events__description": "In dieser Richtung werden nur Ereignisse zum Löschen von Zwischenablagedaten synchronisiert. In diesem Modus wird Textinhalt von der Synchronisierung ausgeschlossen.",
    "enum__clipboard_sync_behavior__only_set_events": "Nur Setzereignisse",
    "enum__clipboard_sync_behavior__only_set_events__description": "In dieser Richtung werden nur Ereignisse zum Setzen von Zwischenablagedaten synchronisiert. In diesem Modus wird Textinhalt in die Synchronisierung einbezogen.",
    "enum__clipboard_sync_behavior__all_events": "Alle Ereignisse",
    "enum__clipboard_sync_behavior__all_events__description": "In dieser Richtung werden sowohl Ereignisse zum Setzen als auch zum Löschen von Zwischenablagedaten synchronisiert. In diesem Modus wird Textinhalt in die Synchronisierung einbezogen.",
    "enum__keyboard_mode__characters": "Buchstaben",
    "enum__keyboard_mode__symbols": "Symbole",
    "enum__keyboard_mode__symbols2": "Symbole 2",
    "enum__keyboard_mode__numeric": "Zahlen",
    "enum__keyboard_mode__numeric_advanced": "Erweiterte Zahlen",
    "enum__keyboard_mode__phone": "Telefon",
    "enum__keyboard_mode__phone2": "Telefon 2",
    "enum__swipe_action__switch_to_media_context": "Emoji Bereich öffnen",
    "unit__hours__symbol": "{v} h",
    "unit__minutes__symbol": "{v} m",
    "unit__seconds__symbol": "{v} s",
    "unit__milliseconds__symbol": "{v} ms",
    "unit__percent__symbol": "{v}%",
    "unit__display_pixel__symbol": "{v} dp",
    "unit__display_pixel_per_seconds__symbol": "{v} dp/s",
}

# Determine exactly which resources are missing in the existing German file.
en_root = ET.parse(EN).getroot()
de_root = ET.parse(DE).getroot()
en_names = {e.attrib.get("name") for e in en_root if e.tag == "string" and e.attrib.get("name")}
de_names = {e.attrib.get("name") for e in de_root if e.tag == "string" and e.attrib.get("name")}
missing = en_names - de_names

if missing != set(T):
    print("Translation map mismatch")
    print("Missing but not translated:", sorted(missing - set(T)))
    print("Translated but not missing:", sorted(set(T) - missing))
    raise SystemExit(1)

# Add all 119 missing translations to the upstream German resource file.
de_text = DE.read_text(encoding="utf-8")
rows = ["", "    <!-- Complete German additions for v0.5.2 -->"]
for name in sorted(T):
    rows.append(f'    <string name="{name}">{escape(T[name])}</string>')
insert = "\n".join(rows) + "\n"
de_text = de_text.replace("</resources>", insert + "</resources>")
DE.write_text(de_text, encoding="utf-8")

# The default fallback becomes the complete German resource file.
# This guarantees that missing locale matches never fall back to English.
shutil.copyfile(DE, EN)

# Remove other UI language string files so Android cannot override our German
# default when the device itself is set to another language. Keyboard layouts,
# dictionaries and language packs are not touched.
for p in RES.glob("values-*/strings.xml"):
    if p.parent.name != "values-de":
        p.unlink()

# Make the installable custom build visually match the stable app, but use its
# own package suffix so it can coexist with an official FlorisBoard install.
gradle = GRADLE.read_text(encoding="utf-8")
gradle = gradle.replace('applicationIdSuffix = ".debug"', 'applicationIdSuffix = ".de"')
gradle = gradle.replace('versionNameSuffix = "-debug+${getGitCommitHash(short = true).get()}"', 'versionNameSuffix = "-de"')
gradle = gradle.replace('@mipmap/ic_app_icon_debug"', '@mipmap/ic_app_icon_stable"')
gradle = gradle.replace('@mipmap/ic_app_icon_debug_round"', '@mipmap/ic_app_icon_stable_round"')
gradle = gradle.replace('@drawable/ic_app_icon_debug_foreground"', '@drawable/ic_app_icon_stable_foreground"')
gradle = gradle.replace('resValue("string", "floris_app_name", "FlorisBoard Debug")', 'resValue("string", "floris_app_name", "@string/app_name")')
gradle = gradle.replace('isDebuggable = true', 'isDebuggable = false', 1)
GRADLE.write_text(gradle, encoding="utf-8")

# Final structural checks.
final_root = ET.parse(EN).getroot()
final_names = {e.attrib.get("name") for e in final_root if e.tag == "string" and e.attrib.get("name")}
if final_names != en_names:
    raise SystemExit(f"German fallback is incomplete: {len(final_names)} / {len(en_names)}")

print(f"German UI complete: {len(final_names)} string resources")
print("Other UI locale string files removed; keyboard language assets preserved.")
print("Build package: dev.patrickgold.florisboard.de")
