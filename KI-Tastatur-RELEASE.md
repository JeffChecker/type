# KI Tastatur Beta v1

**Schreiben, korrigieren, umformulieren und übersetzen direkt über die Tastatur.**

KI Tastatur bringt einen KI Schreibassistenten direkt in die Android Tastatur. Statt Texte erst in eine andere App zu kopieren, kannst du Korrekturen, Stiländerungen und Prompt Verbesserungen direkt dort ausführen, wo du gerade schreibst.

## Die wichtigsten Funktionen

### KI Korrektur auf Knopfdruck

Du bestimmst selbst, wann korrigiert wird. Es gibt keine automatische Korrektur nach einem Zeitintervall mehr.

Die KI berücksichtigt:

- Rechtschreibung
- Grammatik
- Zeichensetzung
- typische Fehler aus der Spracheingabe
- Sinn und Zusammenhang des gesamten Absatzes

Eigene Satzzeichen wie `!`, `?`, `?!`, `!!` und `…` sollen erhalten bleiben.

### Stil mit einem Fingertipp ändern

Über **Stil** kannst du deinen Text passend zur Situation umschreiben lassen. Enthalten sind unter anderem freundlich, professionell, geschäftlich, locker, stilvoll, humorvoll, sarkastisch, flirtend, verführerisch, direkt, kurz, einfache Sprache, Du Form und Sie Form.

Die Texte sollen natürlich klingen. Typische KI Floskeln, unnötige Gedankenstriche und künstlich wirkende Formulierungen werden möglichst vermieden.

### Prompt+

Aus einer groben Idee wird ein klarer Prompt. Prompt+ strukturiert Ziel, Kontext, gewünschtes Ergebnis und wichtige Vorgaben, ohne zusätzliche Fakten zu erfinden.

### Übersetzung direkt auf dem Gerät

Übersetzen läuft lokal über Google ML Kit. Die Ausgangssprache wird automatisch erkannt. Die aktive Tastatursprache bestimmt das Übersetzungsziel.

Dafür ist kein Cloud API Schlüssel erforderlich.

### Freie Wahl des KI Anbieters

Unterstützt werden:

- OpenAI
- Google Gemini
- Anthropic Claude
- Groq

Jeder Anbieter bekommt seinen eigenen API Schlüssel. Verfügbare Modelle werden direkt über die jeweilige API geladen. Du kannst ein Modell selbst auswählen oder `auto` verwenden.

## Datenschutz

Passwortfelder, sensible Eingabefelder und Inkognito werden nicht an die Cloud KI gesendet. Übersetzungen laufen lokal auf dem Gerät.

Bei Korrektur, Stiländerung oder Prompt+ wird nur der Text verarbeitet, für den du die Funktion bewusst auslöst. Der Text wird an den von dir ausgewählten KI Anbieter gesendet.

## Installation

1. `KI-Tastatur-Beta-v1.apk` herunterladen.
2. APK installieren.
3. KI Tastatur in Android aktivieren.
4. Als Tastatur auswählen.
5. In den KI Einstellungen einen Anbieter und API Schlüssel eintragen.
6. Modelle laden und `auto` oder ein gewünschtes Modell auswählen.
7. Verbindung testen.

## Beta Hinweis

Dies ist **Beta v1**. Die App wird anhand der praktischen Nutzung weiter verbessert. Künftige Testversionen heißen Beta v2, Beta v3 usw.

## Open Source Grundlage

KI Tastatur basiert auf dem Open Source Projekt **FlorisBoard** und verwendet dessen technische Grundlage unter der **Apache License 2.0**.

Originalprojekt: https://github.com/florisboard/florisboard
