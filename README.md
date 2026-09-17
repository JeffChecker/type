# KI Tastatur

**Eine Android Tastatur mit integrierter KI für Korrektur, Stil, Prompts und Übersetzung.**

KI Tastatur verbindet eine vollwertige Android Tastatur mit einem direkten Schreibassistenten. Texte werden nicht automatisch im Hintergrund verändert. Du entscheidest selbst, wann die KI eingreift.

Aktueller Stand: **Beta v1**

[Beta v1 herunterladen](https://github.com/JeffChecker/type/releases/tag/ki-tastatur-beta-v1)

## Was KI Tastatur kann

### KI Korrektur auf Knopfdruck

Mit **KI korrigieren** wird der markierte Text oder der aktuelle Absatz geprüft. Die KI berücksichtigt nicht nur Rechtschreibung und Grammatik, sondern auch den Sinn des Textes, typische Diktierfehler und den Zusammenhang.

Eigene Satzzeichen wie `!`, `?`, `?!`, `!!` oder `…` sollen erhalten bleiben. Es gibt keine zeitgesteuerte automatische Korrektur mehr.

### Schreibstil ändern

Über die Taste **Stil** kann ein vorhandener Text gezielt umgeschrieben werden. Zur Auswahl gehören unter anderem:

- Freundlich
- Professionell
- Geschäftlich
- Locker
- Stilvoll
- Humorvoll
- Sarkastisch
- Flirtend
- Verführerisch oder zweideutig
- Direkt
- Kurz
- Einfache Sprache
- Du Form
- Sie Form

Die Stilprompts sind darauf ausgelegt, natürlich zu klingen. Typische KI Floskeln, unnötige Gedankenstriche und künstlich überstrukturierte Formulierungen werden vermieden.

### Prompt verbessern

Mit **Prompt+** wird aus einem groben Text ein klarer, verständlicher Prompt. Ziel, Kontext, gewünschtes Ergebnis und wichtige Vorgaben werden strukturiert, ohne zusätzliche Fakten zu erfinden.

### Übersetzen

Die Übersetzungsfunktion läuft lokal über **Google ML Kit**. Die Ausgangssprache wird automatisch erkannt. Als Zielsprache wird die aktuell aktive Tastatursprache verwendet.

Für die lokale Übersetzung ist kein Cloud API Schlüssel erforderlich.

### Mehrere KI Anbieter

KI Tastatur unterstützt:

- OpenAI
- Google Gemini
- Anthropic Claude
- Groq

Für jeden Anbieter wird der eigene API Schlüssel getrennt gespeichert. Die verfügbaren Modelle werden direkt beim jeweiligen Anbieter abgefragt. Du kannst ein Modell selbst auswählen oder die automatische Auswahl verwenden.

## Datenschutz

Cloud KI wird nicht für Passwortfelder, sensible Eingabefelder oder den Inkognito Modus verwendet. Übersetzungen laufen lokal auf dem Gerät.

Bei Korrektur, Stiländerung oder Prompt Verbesserung wird der dafür ausgewählte Text an den von dir eingerichteten KI Anbieter gesendet. Es werden keine API Schlüssel im Quellcode der App mitgeliefert.

## Installation

1. Lade die aktuelle APK aus den Releases herunter.
2. Installiere **KI Tastatur** auf deinem Android Gerät.
3. Aktiviere die Tastatur in den Android Einstellungen.
4. Lege KI Tastatur als Eingabemethode fest.
5. Öffne die Einstellungen der KI Tastatur.
6. Wähle einen KI Anbieter aus und trage deinen API Schlüssel ein.
7. Lade die verfügbaren Modelle und wähle ein Modell oder `auto`.
8. Teste die Verbindung.

> Hinweis: Die App verwendet derzeit weiterhin die ursprüngliche interne Paketkennung der technischen Basis. Eine anders signierte Installation mit derselben Paketkennung kann deshalb nicht direkt überschrieben werden.

## Beta Status

Beta v1 ist eine Testversion. Funktionen und Bedienung werden auf Grundlage praktischer Nutzung weiter verbessert. Die Versionsreihe wird künftig als **Beta v1, Beta v2, Beta v3** usw. geführt.

## Technische Basis und Lizenz

KI Tastatur basiert auf dem Open Source Projekt **FlorisBoard** und wird auf Grundlage der **Apache License 2.0** weiterentwickelt.

Originalprojekt: https://github.com/florisboard/florisboard

Die sichtbare Produktbezeichnung dieser Variante ist **KI Tastatur**. FlorisBoard bleibt als technische Herkunft und Lizenzquelle genannt.
