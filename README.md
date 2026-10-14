# OttOSpiele – Arrow Puzzle

**Status:** Android-Entwicklungsprototyp · **Zweig:** `develop` · **Stand der Dokumentation:** 14.10.2026

Ein offlinefähiges, vollständig werbefreies Pfeil-Denkspiel für Kinder. Das Spiel und seine Anforderungen werden vom Projektverantwortlichen vorgegeben; die bisherige Quellcodeentwicklung und diese Dokumentation erfolgten unter Verwendung von OpenAI ChatGPT (GPT-6). Ein erfolgreiches CI-Build ist keine Bestätigung der vollständigen funktionalen, rechtlichen oder kindgerechten Eignung.

## Spielidee und ursprünglicher Anforderungsauftrag (sinngemäß zusammengefasst)

Der ursprüngliche Auftrag vom 14.10.2026 beschreibt eine Android-Adaption des bekannten Pfeil-Entwirr-Spiels: Aus einem Spielfeld mit teils verwinkelten, orthogonalen Pfeilen werden diejenigen angetippt, die entlang ihrer Pfeilrichtung ohne Zusammenstoß das Spielfeld verlassen können. Das Spiel soll ohne Werbung, möglichst vollständig offline und kostenfrei als Donationware erhältlich sein. Unbegrenzt viele Level sollen automatisch generiert werden, mit behutsam zunehmender Schwierigkeit.

Ursprünglich gefordert wurden: konfigurierbare Leben für Fehlversuche, keine mehrfachen Lebensabzüge für unverändert blockierte Pfeile, einstellbare Glühbirnen-Tipps, ein jederzeit erreichbarer Knopf für ein anderes leichteres Rätsel, dabei Rückstufung um eine Schwierigkeitsstufe und Zurücksetzen des Stufenfortschritts, frühestens drei abgeschlossene Level bis zum nächsten Aufstieg, eine symbolische Aufstiegsanzeige, anfangs drei und später bis maximal zehn erforderliche Level pro Stufe sowie freudige Erfolgs- und ermutigende Niederlagenanimationen mit sympathischen Monsterfiguren.

Dies ist eine redaktionelle Zusammenfassung des Benutzerauftrags und **kein wörtlich reproduzierter Prompt**. Sensible Sitzungsdaten und interne Modell-Gedankengänge werden nicht veröffentlicht.

## Verbindliche Anforderungen und anschließende Entscheidungen

### Pfeile und Kollisionen

- Vier Bewegungsrichtungen: oben, unten, links, rechts. Die Pfeilspitze bestimmt die Austrittsrichtung.
- Verwinkelte Pfeile bestehen aus einer zusammenhängenden orthogonalen Polylinie.
- **Die gesamte bewegte Pfeilgeometrie** muss den Weg bis über den Spielfeldrand kollisionsfrei durchlaufen können, nicht nur die Spitze.
- Freie Pfeile verschwinden mit einer Bewegungsanimation; blockierte Pfeile werden sichtbar markiert und bleiben stehen.
- Erneutes Antippen eines bei gleicher Blockierermenge weiterhin blockierten Pfeils kostet nicht erneut ein Leben; bei Veränderung der Blockierer kann ein erneuter Fehler zählen. Alternative: höchstens ein Abzug pro Pfeil und Level.

### Leben, Tipps und Niederlage

- Leben: 3, 5 (Standard) oder unbegrenzt; Tipps: 0, 1, 3 (Standard), 5 oder unbegrenzt.
- Tipps zeigen einen aktuell freien Pfeil an und entfernen ihn nicht automatisch. Pro Level werden sie neu bereitgestellt; Tippgebrauch verhindert keinen Aufstieg.
- **Verbindliche Nutzerentscheidung:** Nach Verbrauch aller Leben wird **dasselbe Rätsel** von Anfang an mit frisch aufgefüllten Leben und Tipps wiederholt. Kein automatischer Abstieg und kein Stufenfortschrittsverlust.

### Level, Fortschritt und Rückstufung

- Vollständig lösbare, deterministisch über einen Seed reproduzierbare Rätsel; der Solver prüft jedes generierte Level.
- Langsame Progression: erforderliche abgeschlossene Level pro Stufe `3,3,4,5,6,7,8,9,10,10` für Stufen 1–10; später maximal zehn.
- Anzahl und optische Länge der Pfeile allein definieren die Schwierigkeit nicht: auch Blockierungsabhängigkeiten, Freigaben, Dichte, Knicke und Kettentiefe zählen.
- Ein Levelabschluss erhöht den Stufenfortschritt; bei Erreichen des Schwellenwerts steigt die Stufe.
- „Anderes Rätsel“ generiert neu, setzt den Fortschrittszähler auf null und senkt die aktuelle Schwierigkeitsstufe um eins (minimal Stufe 1). Mindestens drei erfolgreich absolvierte Level bis zum nächsten Aufstieg.
- **Verbindliche Nutzerentscheidung:** **Nur aktueller Fortschritt zählt**; eine einmal erreichte höchste Stufe wird nicht separat gespeichert oder wiederhergestellt.

### Gezielte Rätselmuster – spätere Erweiterung des Auftrags

Spielerfahrungen zeigten, dass interessante Rätsel aus Gruppen gleichgerichteter Pfeile, entscheidenden Schlüsselpfeilen und wechselnden Freigaberichtungen entstehen. Deshalb sollen folgende Musterfamilien konstruiert und geprüft werden:

1. **Schlüsselpfeil:** Ein bestimmter Pfeil gibt eine Gruppe anderer Pfeile frei, oft in gleicher Richtung.
2. **Wechselkette:** Abwechselnde Bewegungsrichtungen, beispielsweise ↓ → ↓ →.
3. **Verzweigung:** Ein Pfeil öffnet mehrere Wege, ggf. mit späterer Zusammenführung.
4. **Verschachtelung:** Geometrisch komplexe, längere Pfeile umschließen andere.
5. **Unabhängige Gruppen:** Mehrere gleichzeitig verfügbare Teilrätsel.
6. **Täuschend freie Pfeile:** Räumlich schwer erkennbare Blockierungen.

Die Mustervorlagen sollen zuerst logische Blockierungsgraphen beschreiben, anschließend geometrisch realisiert, kombiniert und durch den Solver validiert werden. Vorgesehen ist zunächst eine zuverlässige Realisierung der ersten drei Familien, bevor weitere kombiniert werden.

### Gestaltung, Bedienung und Datenschutz

- Primär kinderfreundliche Bedienung ohne Zeitdruck, ausreichend große Touchflächen, Zoom bei komplexen Leveln, barrierearme Rückmeldungen, überspringbare Animationen und ein kindgerechtes Tutorial.
- Monstercharaktere vermitteln Erfolg und ermutigen bei Misserfolg, ohne Straf- oder Leistungsdruck.
- Einstellungen und freiwillige Spenden nur in einem geschützten Elternbereich. **Keine** Spendenaufforderungen im Spiel, keine Werbe-SDKs, keine Tracking-SDKs, keine erforderlichen Benutzerkonten.
- Automatisches lokales Speichern eines begonnenen Spiels inklusive Pfeilzuständen, Fehlerhistorie, Leben, Tipps und Stufenfortschritt.
- Kein zeitlich begrenztes Belohnungssystem, keine Streaks, keine bezahlten Zusatzleben.
- Freiwillige Spenden werden **erst für eine spätere Version** geplant; konkrete Play-Store- und Kinderschutzanforderungen sind vor Veröffentlichung gesondert zu prüfen.

## Technische Realisierung und tatsächlicher Implementierungsstand

| Bereich | Aktueller Entwicklungsstand | Noch offen |
| --- | --- | --- |
| Kotlin/JVM-Spielkern | Vorhanden, CI-Tests erfolgreich | Erweiterte Validierung und Lasttests |
| Android / Jetpack Compose | Startbar, Debug-APK über GitHub Actions | Gerätetests und UI-Politur |
| Pfeilgeometrie | Gerade und einfach geknickte orthogonale Pfeile | Mehrfach geknickte komplexe Pfade |
| Kollisionslogik | Rasterbasierte Bewegung der gesamten Pfeilzellen | Erweiterte geometrische Fälle |
| Generator/Solver | Konstruktive Generierung, Solver | Gezielt realisierte und kombinierte Mustervorlagen |
| Analyse | Schlüsselpfeil-, Verzweigungs- und Kettenmetriken | Rückkopplung in die Erzeugung |
| Spielregeln | Grundlegende Herzen, Tipps, Neustart und Stufen | Alle Einstellungsvarianten, UX und Robustheit |
| Speicherung | Noch **nicht** implementiert | Vollständiges lokales Resume |
| Animationen/Monster | Noch **nicht** vollständig implementiert | Erfolg, Niederlage, Aufstieg und Sounds |
| Elternbereich/Donationware | Einfache Einstellungen vorhanden | Schutz, Spendenoption, rechtliche Prüfung |

**Hinweis:** Die frühere prototypische Pattern-Engine darf nicht mit der aktuell veröffentlichten Android-Codebasis gleichgesetzt werden. Die explizite Komposition der Muster ist noch ein offener Entwicklungsschritt. Die App ist nicht releasefähig.

## APK auf einem Android-Tablet testen

- Stetig aktualisierte Entwicklungs-Vorabversion: [GitHub Release `develop-preview`](https://github.com/OttOSpiele/ArrowPuzzle/releases/tag/develop-preview).
- Dort `OttOSpiele-ArrowPuzzle-debug.apk` herunterladen und auf dem eigenen Gerät installieren.
- Alternativ: [GitHub Actions](https://github.com/OttOSpiele/ArrowPuzzle/actions/workflows/android.yml) → letzter erfolgreicher Lauf → Artefakt `OttOSpiele-ArrowPuzzle-debug`.
- Es handelt sich um eine Debug-APK; kein Release-Signing für Google Play.

### Lokaler Build (optional)

Voraussetzungen: JDK 17, Android SDK 35, Gradle 8.11.1.

```sh
gradle :game-core:test :app:assembleDebug
```

Die Gradle-Wrapper-Binärdateien sind noch nicht im Repository vorhanden. Der CI-Workflow installiert Gradle 8.11.1 über `gradle/actions/setup-gradle`.

## Transparenz: KI-unterstützte Entwicklung

Der Quellcode, Teile der Architektur, die Tests, CI/CD-Konfigurationen und die Dokumentation dieses Projekts wurden bislang wesentlich mit **OpenAI ChatGPT, Modell GPT-6**, erstellt und bearbeitet. Die inhaltlichen Produktziele, fachlichen Korrekturen und verbindlichen Entscheidungen stammen vom menschlichen Projektverantwortlichen. Einzelne Commits sind teilweise vollständig KI-generiert und über GitHub-Werkzeuge eingebracht worden.

- **KI-Rolle:** Entwurf, Quellcodegenerierung, Refactoring, technische Analyse, Tests, Dokumentation und CI/CD-Konfiguration.
- **Menschliche Rolle:** Anforderungsdefinition, Richtungsentscheidungen, App-Erprobung und letztendliche Freigabe. Nicht jede Änderung ist bereits einzeln menschlich geprüft.
- **Prüfstatus:** GitHub Actions kann erfolgreiche Builds und automatisierte Tests nachweisen, **nicht** automatisch funktionale Vollständigkeit, sichere Kinderbedienbarkeit, urheberrechtliche Unbedenklichkeit oder gesetzliche Konformität.
- **Provenienz:** Für künftige KI-erzeugte Commits ist das Format in [`AGENTS.md`](AGENTS.md) verbindliche Projektkonvention. Ältere Commits behalten ihre unveränderte Git-Historie; ihr KI-Einsatz wird hier global dokumentiert.
- **Änderungspflege:** Bei jeder funktionalen Änderung sind diese Anforderungen, die Implementierungsstand-Tabelle und ggf. die Änderungsdokumentation im selben Commit zu aktualisieren.

### EU AI Act – Einordnung

Diese Angaben dienen freiwilliger Transparenz und guter Entwicklungsnachvollziehbarkeit; sie sind **keine Behauptung einer gesetzlich vorgeschriebenen Commit-Kennzeichnung** und keine Konformitätserklärung nach dem EU AI Act. Die Kommission nennt **Quellcode** ausdrücklich als außerhalb bestimmter Kennzeichnungspflichten für synthetische Ausgaben nach Artikel 50 liegend. Das gegenwärtige Spiel enthält selbst kein während der Nutzung interagierendes generatives KI-System. Eine erneute rechtliche Bewertung ist erforderlich, falls später KI-Funktionen, KI-generierte Inhalte oder andere einschlägige Funktionen ins Endprodukt aufgenommen werden.

Rechtliche Quellen (Stand 14.10.2026):
- [EU AI Act, Art. 50](https://ai-act-service-desk.ec.europa.eu/de/ai-act/article-50)
- [Europäische Kommission: FAQ zu Transparenzpflichten (einschl. Quellcode)](https://digital-strategy.ec.europa.eu/en/faqs/transparency-obligations-under-article-50-ai-act)
- [EU-Leitlinien zu Art. 50 (20.07.2026)](https://digital-strategy.ec.europa.eu/en/library/guidelines-transparency-obligations-providers-and-deployers-ai-systems)

## Dokumentationsstand / Änderungsverlauf

- **14.10.2026 – Initiale Anforderungen:** Werbefreies Pfeilrätsel, konfigurierbare Leben und Tipps, Levelgenerierung, Progression und Monster.
- **14.10.2026 – Fachliche Präzisierungen:** Dasselbe Rätsel nach Niederlage wiederholen; nur aktuellen Stufenfortschritt erhalten.
- **14.10.2026 – Algorithmik:** Musterfamilien, Schlüsselpfeile, Wechselketten, Verzweigungen und geometrisch überprüfte Abhängigkeiten.
- **14.10.2026 – Prototyp/Build:** Kotlin/JVM-Spielkern, Jetpack Compose, GitHub Actions, GitHub-Releases-Vorschau, geknickte Pfeile und Abhängigkeitsanalyse.
- **14.10.2026 – Transparenz und Dokumentation:** KI-Provenienz, Commit-Konvention und laufend gepflegte Anforderungshistorie eingeführt.
