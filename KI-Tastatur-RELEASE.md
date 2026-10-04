# KI Tastatur Beta v7

**Antworten mit sichtbarem Gesprächskontext.**

Beta v7 baut auf ChatGPT Login und der semantischen Textprüfung aus Beta v6 auf und ergänzt die neue Funktion **Antwort**.

## Neu: Antwort aus dem sichtbaren Kontext

Nach Tippen auf **Antwort** liest KI Tastatur den aktuell sichtbaren, für Android zugänglichen Text der geöffneten App. Daraus wird der Gesprächszusammenhang erkannt und eine passende Antwort formuliert.

Wenn bereits ein eigener Entwurf im Eingabefeld steht, wird er als gewünschte Richtung berücksichtigt und sinnvoll verbessert. Ist das Feld leer, wird eine neue Antwort eingefügt.

Bildschirmtext wird ausdrücklich nur als Kontext behandelt, nicht als Anweisung an die KI. Fremde Prompts oder Webseitenanweisungen sollen dadurch die Antwortlogik nicht übernehmen.

## Datenschutz und Einwilligung

Die Funktion liest nicht dauerhaft mit und speichert keinen Bildschirmverlauf. Der Zugriff erfolgt nur nach Tippen auf **Antwort**.

Passwortfelder und editierbare Fremdfelder werden ausgelassen. Inkognito und sensible Eingabefelder bleiben für Cloud KI gesperrt.

Vor der ersten Nutzung erscheint eine eigene Offenlegung. Erst nach ausdrücklicher Zustimmung kann der erforderliche Android Dienst aktiviert werden. Der gelesene Text wird für die Antwort an den in KI Tastatur ausgewählten KI Anbieter gesendet.

## Noch stärkere Satzprüfung

Die allgemeine Korrektur arbeitet nun in drei Schritten: Bedeutung und Absicht erfassen, jeden Satz im Zusammenhang prüfen und die fertige Fassung noch einmal als Ganzes auf Sinn und Natürlichkeit kontrollieren.

Sätze dürfen vollständig neu gebaut, geteilt, zusammengeführt oder umgestellt werden, wenn das Ergebnis dadurch menschlicher und verständlicher wird.

Die Prompts für Korrigieren, Humorvoll, Persönlich, Professionell und Geschäftlich wurden zusätzlich verschärft.

## Login

Der offizielle **Mit ChatGPT anmelden** Ablauf bleibt erhalten. OpenAI kann weiterhin über ChatGPT Login oder alternativ mit einem eigenen API Schlüssel verwendet werden.

Gemini, Claude und Groq bleiben über ihre offiziellen Entwickler Zugänge verfügbar.

## Einschränkung

Es wird zugänglicher Bildschirmtext verwendet. Text, der nur in Bildern, Videos oder speziell geschützten Oberflächen dargestellt wird, kann nicht zuverlässig gelesen werden.

## Installation

1. **KI-Tastatur-Beta-v7.apk** herunterladen.
2. APK installieren.
3. KI Tastatur in Android aktivieren.
4. Als Eingabemethode auswählen.
5. KI Anbieter einrichten oder mit ChatGPT anmelden.
6. In den KI Einstellungen **Bildschirmkontext einrichten** öffnen.
7. Der Offenlegung zustimmen und den Dienst **KI Tastatur Bildschirmkontext** in den Android Bedienungshilfen aktivieren.
8. In einem Chat auf **Antwort** tippen.

## Open Source Grundlage

KI Tastatur basiert auf **FlorisBoard** und wird unter Beachtung der **Apache License 2.0** weiterentwickelt.

Originalprojekt: https://github.com/florisboard/florisboard
