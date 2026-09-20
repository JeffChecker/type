# KI Tastatur Beta v4

**Die Android Tastatur mit KI direkt beim Schreiben.**

KI Tastatur bringt Korrektur, Stilwechsel, Prompt Optimierung und Übersetzung dorthin, wo sie gebraucht werden: direkt in die Tastatur.

Kein Kopieren in andere Apps. Kein automatisches Eingreifen während du noch schreibst. Du entscheidest selbst, wann die KI helfen soll.

## Was KI Tastatur besonders macht

### Texte verstehen statt nur Rechtschreibung prüfen

Mit **KI korrigieren** wird nicht nur auf Rechtschreibung und Grammatik geschaut. Beta v4 verwendet eine robuste Fallback Logik: markierter Text, aktueller Absatz, aktueller Satz oder ein Textblock vor dem Cursor. Dadurch funktioniert die Korrektur zuverlässiger in unterschiedlichen Apps und Eingabefeldern.

Die Korrektur startet ausschließlich auf Knopfdruck. Eigene Satzzeichen wie `!`, `?`, `?!`, `!!` oder `…` sollen erhalten bleiben.

### Ein Text, viele Stile

Mit **Stil** kannst du denselben Inhalt passend zur Situation umformulieren:

**Freundlich · Professionell · Geschäftlich · Stilvoll · Locker · Humorvoll · Sarkastisch · Flirtend · Verführerisch · Direkt · Kurz · Einfache Sprache · Du Form · Sie Form**

Die Texte sollen natürlich und menschlich wirken. Typische KI Floskeln, künstlich glatte Formulierungen und unnötige Gedankenstriche werden möglichst vermieden.

### Prompt+ für bessere KI Anfragen

Du hast eine grobe Idee, aber noch keinen guten Prompt? **Prompt+** macht daraus eine klar strukturierte Anfrage mit Ziel, Kontext und wichtigen Vorgaben, ohne neue Fakten zu erfinden.

### Übersetzen direkt auf dem Gerät

Beta v4 übersetzt standardmäßig mit dem **bereits eingerichteten KI Anbieter**. OpenAI, Gemini, Claude oder Groq verwenden denselben API Schlüssel und dasselbe ausgewählte Modell wie die übrigen KI Funktionen. Ein zusätzlicher Übersetzungs Schlüssel ist nicht erforderlich. Die Ausgangssprache wird automatisch erkannt und der Text wird mit einem strengen Übersetzungs Prompt sinngenau, vollständig und ohne zusätzliche Inhalte übertragen.

Die Zielsprache kann der aktiven Tastatursprache folgen oder aus einer großen Sprachliste gewählt werden. Optional kann Google Cloud Translation als Fallback hinterlegt werden. Im Inkognito Modus wird ausschließlich lokal über **Google ML Kit** übersetzt.

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

Cloud Funktionen werden nur ausgelöst, wenn du selbst auf Korrigieren, Stil, Prompt+ oder eine aktivierte Cloud Übersetzung tippst. Google Cloud Übersetzungen werden an Google Translate gesendet. Im Inkognito Modus verwendet die App ausschließlich die lokale ML Kit Übersetzung.

## Installation

1. **KI-Tastatur-Beta-v4.apk** herunterladen.
2. APK installieren.
3. KI Tastatur in den Android Einstellungen aktivieren.
4. Als Eingabemethode auswählen.
5. In den KI Einstellungen einen Anbieter wählen.
6. Eigenen API Schlüssel eintragen.
7. Modelle laden und eines auswählen oder `auto` verwenden.
8. Verbindung testen.

## Beta v4

Diese Beta konzentriert sich auf kontextbezogene KI Übersetzungen mit dem bereits vorhandenen API Schlüssel, eine große Sprachauswahl, optionalen Google Fallback und lokale Offline Übersetzung. Weitere Testversionen folgen fortlaufend als **Beta v5, Beta v6** usw.

## Open Source Grundlage

KI Tastatur basiert auf **FlorisBoard** und wird unter Beachtung der **Apache License 2.0** weiterentwickelt.

Originalprojekt: https://github.com/florisboard/florisboard
