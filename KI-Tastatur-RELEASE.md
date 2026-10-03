# KI Tastatur Beta v6

**Texte werden jetzt zuerst verstanden und danach neu formuliert.**

Beta v6 überarbeitet die eigentliche Schreiblogik. Korrigieren und alle Stilarten prüfen nicht mehr nur einzelne Wörter. Der vollständige Absatz oder markierte Text wird zuerst als zusammenhängende Aussage verstanden.

## Neu: deutlich stärkere Korrektur

**KI korrigieren** prüft Sinn, Logik, Grammatik, Satzbau, Wortbezüge, Zeitform, Zeichensetzung und typische Diktatfehler im Zusammenhang.

Ein grammatisch falscher oder unnatürlicher Satz darf vollständig neu aufgebaut werden. Dabei sollen Fakten, Namen, Zahlen, Termine, Fragen, Forderungen und die erkennbare Absicht erhalten bleiben.

Bewusst gesetzte Endzeichen wie `!`, `?`, `?!` und `…` bleiben erhalten. Ein normaler fehlender Punkt darf jetzt sinnvoll ergänzt werden.

## Neu: alle Stil Prompts überarbeitet

Freundlich, Professionell, Geschäftlich, Stilvoll, Locker, Humorvoll, Sarkastisch, Flirtend, Verführerisch, Persönlich, Direkt, Kurz, Einfache Sprache, Du Form und Sie Form führen zuerst eine Sinnprüfung durch.

Danach wird der gewünschte Stil umgesetzt. Humor entsteht aus dem vorhandenen Zusammenhang. Persönliche Texte sollen individuell und nahbar wirken. Professionelle Texte bleiben klar und natürlich. Unnötige KI Floskeln und künstliche Formulierungen werden ausdrücklich vermieden.

## Neu: Mit ChatGPT anmelden

OpenAI kann in Beta v6 über den offiziellen **Sign in with ChatGPT** Ablauf verbunden werden.

Berechtigte Nutzer können nach eigener Zustimmung ihre freigegebene ChatGPT Plan Nutzung für OpenAI Anfragen der Tastatur verwenden. Dafür muss kein OpenAI API Schlüssel in der App eingetragen werden.

Die bisherige OpenAI API Schlüssel Nutzung bleibt als Alternative erhalten. In den Einstellungen kann jederzeit zwischen ChatGPT Plan und API Schlüssel gewechselt werden.

Der Login verwendet den Systembrowser, PKCE, State und Nonce Prüfung, ID Token Prüfung und rotierende OAuth Tokens. Die Inferenz läuft über die öffentliche OpenAI Responses API mit `store: false` und `stream: true`.

## Weitere Anbieter

Google Gemini, Anthropic Claude und Groq bleiben verfügbar. Sie verwenden weiterhin ihre offiziellen Entwickler Zugänge. Die Tastatur automatisiert keine Chat Webseiten und versucht nicht, normale Abonnements über undokumentierte Schnittstellen zu verwenden.

## Datenschutz

Cloud Funktionen laufen nur auf Knopfdruck. Passwortfelder, sensible Eingaben und Inkognito Felder werden nicht an Cloud KI Anbieter gesendet.

## Installation

1. **KI-Tastatur-Beta-v6.apk** herunterladen.
2. APK installieren.
3. KI Tastatur in Android aktivieren.
4. Als Eingabemethode auswählen.
5. KI Einstellungen öffnen.
6. OpenAI direkt mit ChatGPT verbinden oder einen API Anbieter konfigurieren.
7. `auto` für die Modellwahl verwenden oder ein verfügbares Modell auswählen.
8. Verbindung testen.

## Open Source Grundlage

KI Tastatur basiert auf **FlorisBoard** und wird unter Beachtung der **Apache License 2.0** weiterentwickelt.

Originalprojekt: https://github.com/florisboard/florisboard
