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
- **Schlangenbewegung statt starrer Translation:** Die Pfeilspitze verlässt das Spielfeld in Richtung ihres letzten Segments; der restliche Körper folgt der bereits gezeichneten Linie einschließlich aller Kurven. Neue Zellen werden nur auf dem geraden Austrittsstrahl vor der Spitze betreten. Andere Pfeile auf diesem Strahl sind Blockierer.
- Freie Pfeile verschwinden mit einer Bewegungsanimation; blockierte Pfeile werden sichtbar markiert und bleiben stehen.
- Erneutes Antippen eines bei gleicher Blockierermenge weiterhin blockierten Pfeils kostet nicht erneut ein Leben; bei Veränderung der Blockierer kann ein erneuter Fehler zählen. Alternative: höchstens ein Abzug pro Pfeil und Level.

### Leben, Tipps und Niederlage

- Leben: 3, 5 (Standard) oder unbegrenzt; Tipps: 0, 1, 3 (Standard), 5 oder unbegrenzt.
- Tipps zeigen einen aktuell freien Pfeil an und entfernen ihn nicht automatisch. Pro Level werden sie neu bereitgestellt; Tippgebrauch verhindert keinen Aufstieg.
- **Verbindliche Nutzerentscheidung:** Nach Verbrauch aller Leben wird **dasselbe Rätsel** von Anfang an mit frisch aufgefüllten Leben und Tipps wiederholt. Kein automatischer Abstieg und kein Stufenfortschrittsverlust.

### Level, Fortschritt und Rückstufung

- Vollständig lösbare, deterministisch über einen Seed reproduzierbare Rätsel; der Solver prüft jedes generierte Level.
- Langsame Progression: erforderliche abgeschlossene Level pro Stufe: Stufen 1–8 je 1, Stufen 9–16 je 2, anschließend jeweils in 8er-Stufenblöcken 3, 4, 5 usw. bis maximal 10. Diese neue Vorgabe ersetzt die ursprüngliche Staffelung.
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
| Kollisionslogik | Schlangen-Ausfahrpfad an der Spitze und vorberechnete ID-Abhängigkeitsmatrix | Erweiterte geometrische Fälle |
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

## Kollisionstest aus dem Tablet-Feedback (14.10.2026)

Bei einem nach oben zeigenden L-Pfeil muss auch das horizontal herausragende Schwanzsegment gegen alle verbliebenen Pfeile geprüft werden. Die Bewegung gilt als starr und kontinuierlich, nicht als Folge von Teleportationen zwischen Rasterpunkten. Der Spielkern prüft daher den **gesamten kontinuierlich überstrichenen Bereich jedes Liniensegments** bis zum vollständigen Ausfahren. Das ist eine konservative Kollisionsprüfung von Linienmittelachsen; optische Strichdicke und Pfeilflügel werden noch nicht in die Kollisionsgeometrie integriert. Der rote/koral farbige Pfeil im Screenshot kennzeichnet den vom Spiel als blockiert bewerteten Antippversuch. Ein Screenshot allein beweist daher nicht, dass ein physikalisch freier Pfeil zurückgewiesen wurde.

## Dokumentationsstand / Änderungsverlauf

- **14.10.2026 – Initiale Anforderungen:** Werbefreies Pfeilrätsel, konfigurierbare Leben und Tipps, Levelgenerierung, Progression und Monster.
- **14.10.2026 – Fachliche Präzisierungen:** Dasselbe Rätsel nach Niederlage wiederholen; nur aktuellen Stufenfortschritt erhalten.
- **14.10.2026 – Algorithmik:** Musterfamilien, Schlüsselpfeile, Wechselketten, Verzweigungen und geometrisch überprüfte Abhängigkeiten.
- **14.10.2026 – Prototyp/Build:** Kotlin/JVM-Spielkern, Jetpack Compose, GitHub Actions, GitHub-Releases-Vorschau, geknickte Pfeile und Abhängigkeitsanalyse.
- **14.10.2026 – Transparenz und Dokumentation:** KI-Provenienz, Commit-Konvention und laufend gepflegte Anforderungshistorie eingeführt.

- **14.10.2026 – Korrektur Ausfahrpfad (Gerätetest):** Für verwinkelte Pfeile wird der vollständige Ausfahrweg des gesamten starr in Pfeilrichtung verschobenen Linienverlaufs bis einschließlich des letzten austretenden Zellenpunkts überprüft. Ein freier Pfeilkopf allein genügt ausdrücklich nicht. Regressionstest für einen durch das seitliche Segment blockierten Pfeil. Visuell werden Tippempfehlungen (grün) und falsche Antippversuche (korallfarben) nun unterschieden.

- **14.10.2026 – Nachbesserung Kollisionen:** Kontinuierlicher Bewegungs-Sweep statt einzelner Raster-Frames; die seitlichen Segmente geknickter Pfeile werden über ihre gesamte Ausfahrdistanz berücksichtigt. Weitere Regressionstests für blockierte und freie L-Pfeile. Gerätetest weiter erforderlich.

- **14.10.2026 – Verbindliche Korrektur des Bewegungsmodells:** Ein Pfeil bewegt sich wie eine Schlange entlang seiner eigenen Kontur um Kurven und Spiralen. Die zuvor implementierte starre Translation bzw. Rechteck-Sweep-Prüfung war fachlich falsch und wurde entfernt. Beim Levelaufbau entsteht eine ID-basierte Matrix `requiredRemovals[arrowId] -> Set<blockingArrowId>`. Bei einem Klick wird nur diese vorberechnete Menge gegen die verbleibenden IDs geschnitten; Tipps und Solver verwenden dieselbe Datenbasis. Neue Regressionstests prüfen L- und spiralartige Formen. Die spätere Darstellung einer kontinuierlichen Schlangen-Animation bleibt offen.

- **14.10.2026 – Animation der Schlangenbewegung:** Geordneter Körperpfad je Pfeil, framebasierte Fortschreibung entlang der Kurven, blau-lila Erfolgsfahrt, rotes Vorfahren bis unmittelbar vor den ersten Blockierer und schnelle Rückbewegung. Freigewordene ursprüngliche Zellen bleiben als dezente Rasterpunkte sichtbar. Während der Animation werden Eingaben gesperrt; der erfolgreiche Pfeil wird erst nach der Ausfahrt aus dem Spielzustand entfernt. Die Bewegung ist zunächst zellenweise (noch keine subzellgenaue Interpolation); Aufleuchten der jeweils verlassenen Punkte wird später verfeinert.

- **14.10.2026 – Punktspur auf Austrittswegen:** Das Punkteraster ergänzt auch neue, zuvor freie Zellen, die der Pfeil auf dem Weg zum Spielfeldrand passiert. Die Punkte werden während der Bewegung eingeblendet und bleiben nach erfolgreichem Ausfahren für das laufende Rätsel erhalten. Bei einem blockierten Versuch ist die vorübergehende Spur nur während der Bewegung sichtbar. Punkte außerhalb des Spielfelds werden nicht gezeichnet.

- **14.10.2026 – Modernisierung der Spieloberfläche:** Heller Farbverlauf, abgesetzte Stufen-/Fortschrittskarte, gerundete Schaltflächen, ruhigere Linienfarben und ein kurz aufleuchtender Leuchthof an neu durchlaufenen Rasterpunkten. Die optische Überarbeitung verändert die Spielregeln nicht. Die Animation bleibt zunächst zellenweise; Gerätetest und ggf. Anpassungen an verschiedenen Displaygrößen stehen noch aus.

- **14.10.2026 – Schneller Spielfluss:** Erfolgreiche Pfeile werden sofort aus der logischen Belegungsmatrix entfernt, während ihre Ausfahranimation weiterläuft. Standardmäßig können mehrere Pfeile parallel angetippt und animiert werden; per Einstellungen lässt sich die sequentielle Eingabesperre wieder aktivieren. Geschwindigkeiten Ruhig (32 ms/Zelle), Flott (20 ms/Zelle, Standard) und Turbo (11 ms/Zelle) sind deutlich schneller als bisher (58 ms/Zelle). Animationen und Punktspuren unterstützen mehrere gleichzeitig laufende Pfeile. Neustart oder Levelwechsel beendet laufende Animationen. Noch keine subzellgenaue Interpolation.

- **14.10.2026 – Spielfluss und 6-Jährigen-Progression:** Geschwindigkeiten Ruhig 58, Normal 32 (Standard), Flott 20 und Turbo 11 ms/Zelle. Stufen 1–8 jeweils ein erfolgreiches Level, 9–16 jeweils zwei, anschließend alle acht Stufen um ein erforderliches Level mehr, höchstens zehn. Die Spielfläche benutzt die verfügbare Breite und Höhe ohne quadratische Zentrierung; die Animation wird auf ihre gerundeten Grenzen begrenzt. Die Rasterzellen können auf nichtquadratischen Displays rechteckig sein. Während der Animation bleiben Tipp, Neustart und Rätselauswahl sichtbar aktiv; Neustart/Rätselwechsel brechen laufende Animationen ab. Der Wunsch nach langfristig gespeicherten Einstellungen bleibt offen.

- **14.10.2026 – Erfolgsfluss:** Die Gewinn-Messagebox wird durch einen vollflächigen Erfolgsscreen ersetzt. Das nächste Rätsel wird nach einem kurzen visuellen Auftakt auf einem Hintergrund-Dispatcher generiert, inklusive des zugehörigen Stufenfortschritts. Währenddessen läuft eine sichtbare Ladeanimation. Erst wenn das neue `Game` vollständig vorbereitet ist, erscheint ein **blauer Weiter-Button**; dieser übernimmt das vorbereitete Level ohne blockierende Generierung im Button-Callback. Bei Generierungsfehler kann erneut versucht werden. Die bisherigen Regeln für Levelaufstieg bleiben unverändert.

- **14.10.2026 – Musterbasierte Levelgenerierung, erste Stufe:** Nach der rückwärts aufgebauten, lösbaren Geometrie wird der tatsächliche ID-Abhängigkeitsgraph analysiert. Der Generator erstellt ab Stufe 4 mehrere deterministische Kandidaten und bevorzugt den ersten, der sein Zielmuster nachweisbar erreicht (ansonsten den bestbewerteten Kandidaten). Stufen 1–3: leichter Einstieg; 4–8: Schlüsselpfeile, die mehrere Pfeile freigeben; 9–16: längere Ketten mit Richtungswechseln; ab 17: Verzweigungen in verschiedene Freigaberichtungen. Die Gewichte berücksichtigen dabei gezielt Anzahl freigegebener Pfeile, abweichende Richtungen und verschiedene Zweige. **Noch keine harte Garantie**, dass jedes Seed-Rätsel das gewünschte Muster erreicht; solche Fälle bleiben lösbar und werden als bestmögliche Kandidaten genutzt. Automatisierte Tests prüfen Determinismus, Lösbarkeit und Zielmusterbewertung. Eine streng konstruierende Musterplatzierung bleibt eine spätere Erweiterung.

- **14.10.2026 – Drei Hauptaktionen:** Die bisherige Neustart-Taste wurde zugunsten einer klaren Dreierleiste aufgeteilt: links **Neustart** (exakt dasselbe Rätsel erneut, volle Herzen und Tipps), mittig **Tipp**, rechts **Stufe zurück** (aktuelle Stufe um genau eins senken, mindestens Stufe 1; Fortschrittszähler auf null und neues Level). Die separate Schaltfläche „Anderes Rätsel“ entfällt. Die Funktionen bleiben auch bei laufenden Pfeilanimationen bedienbar, sofern spielerisch sinnvoll; bei Stufenwechsel werden diese Animationen abgebrochen.

- **14.10.2026 – Niederlagenbildschirm statt Popup:** Nach dem letzten verbrauchten Herz erscheint ein vollflächiger, ermutigender Bildschirm analog zum Erfolgsscreen. **Nochmal versuchen** wiederholt exakt dasselbe Rätsel mit frischen Herzen/Tipps ohne Fortschrittsverlust. **Neues Rätsel** erzeugt im Hintergrund ein anderes Rätsel bei **unveränderter Schwierigkeitsstufe** und zeigt dabei eine eindeutige Ladeanzeige; erst wenn die Vorbereitung erfolgreich abgeschlossen ist, erscheint der blaue Button **Weiter**. Erst bei diesem Wechsel wird der Zähler der bereits absolvierten Rätsel innerhalb der Stufe auf **0** gesetzt. Bei Fehlern sind ein neuer Generierungsversuch oder die Wiederholung des alten Rätsels möglich.

- **14.10.2026 – Tablet-Navigationsleisten-Fix:** Die Hauptspielfläche einschließlich ihrer drei Aktionsschaltflächen sowie Erfolgs- und Niederlagenbildschirm berücksichtigt nun Androids Safe-Drawing-Inset für Status- und Navigationsleiste. So sollen die unteren Schaltflächen nicht mehr hinter der Android-Taskleiste liegen und die Kopfzeile nicht unter Statussymbole ragen. Die Schaltflächen bleiben bei laufenden Animationen erreichbar. Gerätetest auf Tablets mit sichtbarer Taskleiste erforderlich.

- **14.10.2026 – Immer sichtbares Punkteraster:** Auf dem gesamten Spielfeld werden die Rasterpunkte unabhängig von bereits belegten, zurückgelegten oder freigewordenen Zellen dezent dauerhaft angezeigt. Vorherige Pfeilpositionen, Austrittswege und kurz aufleuchtende Punkte bleiben als hervorgehobene Ebene darüber sichtbar. Die Punktstruktur ändert die Kollisionen und Levelgenerierung nicht.
