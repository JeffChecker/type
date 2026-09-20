# KI Tastatur Beta v3

**Die Android Tastatur mit KI direkt beim Schreiben.**

KI Tastatur bringt Korrektur, Stilwechsel, Prompt Optimierung und Übersetzung dorthin, wo sie gebraucht werden: direkt in die Tastatur.

Kein Kopieren in andere Apps. Kein automatisches Eingreifen während du noch schreibst. Du entscheidest selbst, wann die KI helfen soll.

## Was KI Tastatur besonders macht

### Texte verstehen statt nur Rechtschreibung prüfen

Mit **KI korrigieren** wird nicht nur auf Rechtschreibung und Grammatik geschaut. Beta v3 verwendet eine robuste Fallback Logik: markierter Text, aktueller Absatz, aktueller Satz oder ein Textblock vor dem Cursor. Dadurch funktioniert die Korrektur zuverlässiger in unterschiedlichen Apps und Eingabefeldern.

Die Korrektur startet ausschließlich auf Knopfdruck. Eigene Satzzeichen wie `!`, `?`, `?!`, `!!` oder `…` sollen erhalten bleiben.

### Ein Text, viele Stile

Mit **Stil** kannst du denselben Inhalt passend zur Situation umformulieren:

**Freundlich · Professionell · Geschäftlich · Stilvoll · Locker · Humorvoll · Sarkastisch · Flirtend · Verführerisch · Direkt · Kurz · Einfache Sprache · Du Form · Sie Form**

Die Texte sollen natürlich und menschlich wirken. Typische KI Floskeln, künstlich glatte Formulierungen und unnötige Gedankenstriche werden möglichst vermieden.

### Prompt+ für bessere KI Anfragen

Du hast eine grobe Idee, aber noch keinen guten Prompt? **Prompt+** macht daraus eine klar strukturierte Anfrage mit Ziel, Kontext und wichtigen Vorgaben, ohne neue Fakten zu erfinden.

### Übersetzen direkt auf dem Gerät

Beta v3 führt eine **Hybrid Übersetzung** ein. Mit einem eigenen Google Cloud Translation API Schlüssel wird die Cloud Translation API für höhere Qualität und eine deutlich größere Sprachauswahl verwendet. Die Ausgangssprache wird automatisch erkannt. Die Zielsprache kann der aktiven Tastatursprache folgen oder aus der aktuellen Google Sprachliste gewählt werden.

Ohne Cloud Schlüssel, bei Cloud Fehlern im Automatikmodus oder im Inkognito Modus wird lokal über **Google ML Kit** übersetzt.

### Du entscheidest, welche KI du nutzt

Unterstützt werden aktuell:

- **OpenAI**
- **Google Gemini**
- **Anthropic Claude**
- **Groq**

Die Modellkonfiguration ist jetzt direkt auf der Hauptseite sichtbar. Anbieter, API Schlüssel, Modellwahl, Modellliste und Verbindungstest sind ohne verstecktes Untermenü erreichbar. Die verfügbaren Modelle werden direkt über die jeweilige API geladen.

## Für Tippen und Spracheingabe

KI Tastatur ist nicht nur für klassische Texteingabe gedacht. Auch diktierte Texte lassen sich anschließend als Ganzes korrigieren und verständlicher formulieren.

## Datenschutz

Passwortfelder, sensible Eingabefelder und Inkognito Felder werden nicht an eine Cloud KI gesendet.

Cloud Funktionen werden nur ausgelöst, wenn du selbst auf Korrigieren, Stil oder Prompt+ tippst. Übersetzungen laufen lokal auf dem Gerät.

## Installation

1. **KI-Tastatur-Beta-v3.apk** herunterladen.
2. APK installieren.
3. KI Tastatur in den Android Einstellungen aktivieren.
4. Als Eingabemethode auswählen.
5. In den KI Einstellungen einen Anbieter wählen.
6. Eigenen API Schlüssel eintragen.
7. Modelle laden und eines auswählen oder `auto` verwenden.
8. Verbindung testen.

## Beta v3

Diese Beta konzentriert sich auf deutlich bessere Übersetzungen, mehr Sprachen, eine dynamisch von Google geladene Sprachliste und einen Offline Fallback. Weitere Testversionen folgen fortlaufend als **Beta v4, Beta v5** usw.

## Open Source Grundlage

KI Tastatur basiert auf **FlorisBoard** und wird unter Beachtung der **Apache License 2.0** weiterentwickelt.

Originalprojekt: https://github.com/florisboard/florisboard
