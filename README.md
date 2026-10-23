# OttOSpiele – Arrow Puzzle

**Status:** Android-Entwicklungsprototyp · **Zweig:** `develop` · **Stand der Dokumentation:** 15.10.2026

Ein offlinefähiges, vollständig werbefreies Pfeil-Denkspiel für Kinder. Das Spiel und seine Anforderungen werden vom Projektverantwortlichen vorgegeben; die bisherige Quellcodeentwicklung und diese Dokumentation erfolgten unter Verwendung von OpenAI ChatGPT (GPT-6). Ein erfolgreiches CI-Build ist keine Bestätigung der vollständigen funktionalen, rechtlichen oder kindgerechten Eignung.

## Lizenz, Rechteinhaber und Projektidentität

**Rechteinhaber: Benjamin Lilienthal.** Der Quellcode der neu veröffentlichten Projektversionen steht unter der **Mozilla Public License 2.0 (MPL-2.0)**, siehe [LICENSE](LICENSE). Die MPL erlaubt auch kommerzielle Weiterverwendung unter ihren Lizenzbedingungen; Änderungen an MPL-lizenzierten Quelldateien unterliegen bei entsprechender Weitergabe weiterhin der MPL.

**OttOSpiele-Kennzeichen und die eigene kreative Spielidentität sind nicht pauschal für fremde Projekte freigegeben.** [TRADEMARKS.md](TRADEMARKS.md) regelt die Verwendung von Namen/Logos; [ASSETS_LICENSE.md](ASSETS_LICENSE.md) beschreibt die gesonderte Zuordnung ausdrücklich gekennzeichneter eigener Grafiken, Figuren und anderer kreativer Assets. Der offene Programmcode soll auch mit neutralen Ersatzassets verwendbar bleiben. Eine Veröffentlichung der APK gewährt keine generelle Markenlizenz.

Der vorherige Entwicklungsstand wurde öffentlich unter GPLv2 veröffentlicht. **Frühere GPLv2-Lizenzrechte bestehen fort**; der Wechsel wirkt nicht rückwirkend. Siehe [Lizenzhistorie](docs/LICENSING_HISTORY.md) und [NOTICE.md](NOTICE.md).

Die Entwicklung erfolgte nach Angaben des Projektverantwortlichen bisher auf Grundlage der eigenen Anforderungen mit wesentlicher Unterstützung von OpenAI ChatGPT. Es wurde kein fremder Quellcode wissentlich übernommen. Das ist keine Garantie der Rechtefreiheit, der Urheberrechtsschutzfähigkeit jedes KI-Ergebnisses oder der Rechteklärung für zukünftige Abhängigkeiten und Assets. Details: [KI-Provenienz](docs/AI_PROVENANCE.md).

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
| Pfeilgeometrie | Gerade, geknickte und in späteren Stufen experimentelle längere Wickelformen; geometrische Selbstüberschneidung wird geprüft | Gezielte Spiralen, verlässliche Formvielfalt und Gerätevalidierung |
| Kollisionslogik | Schlangen-Ausfahrpfad an der Spitze und vorberechnete ID-Abhängigkeitsmatrix | Erweiterte geometrische Fälle |
| Generator/Solver | Rückwärts-konstruktiv lösbare Level; vollständiger logischer DAG-Planer und begrenztes geometrisches DFS-Backtracking als Versuch für Stufen 6–16 mit konstruktivem Fallback | Verlässliche Einbettung jedes Plans für alle Stufen, Mehrfachkanten, Spiralen, Dichte und Schwierigkeitseichung |
| Analyse | Schlüsselpfeil-, Richtungswechsel-, Verzweigungs- und Kettenmetriken beeinflussen Kandidatenauswahl | Verlässliche Musterquoten und Qualitätstests über viele Seeds |
| Spielregeln | Herzen 3/5, Tipps 0/1/3/5, Stufenprogression, Neustart/Stufe zurück, parallele Züge | Unbegrenzte Herzen/Tipps, Sonderfalltests, Speicherung |
| Speicherung | Versionierter privater Snapshot mit Fortschritt, Optionen, aktivem Rätsel, Leben, Tipps und Fehlerhistorie | Gerätetests und künftige Speichermigration |
| Animationen/Monster | Zellweise Schlangenanimation, verstärkte Punktspur, eigene Erfolgs-/Niederlagenbildschirme | Flüssige Interpolation, Monsterfiguren, echte Aufstiegsanimation und Sounds |
| Elternbereich/Donationware | Einfache Einstellungen vorhanden | Schutz, Spendenoption, rechtliche Prüfung |

**Hinweis:** Die frühere prototypische Pattern-Engine darf nicht mit der aktuell veröffentlichten Android-Codebasis gleichgesetzt werden. Die explizite Komposition der Muster ist noch ein offener Entwicklungsschritt. Die App ist nicht releasefähig.

## Schrittweise dichtere Level und längere Pfeile (15.10.2026)

Die frühen Stufen behalten die bisherigen Rastergrößen und Pfeilzahlen. Ab Stufe 25 können längere gerade Pfeile entstehen; ab Stufe 60 enthält der Katalog experimentelle längere Wickel- und Schlangenformen. Die Pfeilzahl bleibt aus Stabilitätsgründen zunächst auf 40 begrenzt; eine weitere Erhöhung erfordert zunächst eine belastbare dichtebewusste Backtracking-Platzierung. Die Wahl der Geometrie bewertet nun für höhere Stufen stärker die **wirklich belegten Rasterzellen**. Verknüpfungen über Ausfahrstrahlen bleiben ein wichtiges Bewertungskriterium. Seed, Seitenverhältnis, Kollisionsprüfung und Solver werden weiterhin verwendet.

Die Funktion `BoardProfile.targetOccupancy(stage)` dokumentiert eine langsam steigende **angestrebte**, nicht erzwungene Flächenbelegung von später bis zu 100 %; `actualOccupancy(level)` misst die tatsächlich belegten Zellen. Das Maximum wird ausdrücklich **nicht garantiert**: bei eng gepackten Rätseln können zu wenige konfliktfreie Positionen bleiben, und zu hohe Dichte kann Lesbarkeit oder Lösbarkeit beeinträchtigen. Ziel ist eine langfristig kindgerechte Progression für Sechsjährige, nicht eine frühe Maximierung des Schwierigkeitsgrads.

**Noch offen:** automatische Erfüllung der Dichteziele, vollständig geplante Kombinationen mehrerer Abhängigkeitsfamilien, erzwungene Spiralen, Vermeidung isolierter Restpfeile, Mindestquoten für Ketten/Verzweigungen sowie belastbare Generierungszeiten auf älteren Tablets. Die Erhöhung der Pfeilzahl ist noch keine Garantie für hohe Zellenbelegung.

## Abhängigkeitsplaner und geometrisches Backtracking (15.10.2026)

Der neue `DependencyPlanner` plant zuerst einen vollständigen, seedabhängigen DAG: Für jeden nach dem ersten Pfeil einzusetzenden Pfeil wird eine logische Blockierbeziehung zu einem bereits geplanten Pfeil definiert. Die Planung ist unabhängig von Rasterkoordinaten und Seitenverhältnis. `GeometricBacktracker` sucht anschließend deterministisch nach kollisionsfreien orthogonalen Geometrien, prüft die vorgesehenen Kanten auf dem echten Ausfahrstrahl und nimmt erfolglose Platzierungen durch rekursives Backtracking zurück. Die umgekehrte Aufbaufolge sichert während der Suche die Lösbarkeit; am Ende validiert zusätzlich `Rules.solve` das Ergebnis.

**Experimentelle Integration:** Aktuell wird dieses Verfahren auf Stufen 6–16 mit begrenztem Suchbudget versucht. Wenn keine geometrische Einbettung gelingt, greift weiterhin der bisherige konstruktive Generator. Andere Stufen verwenden noch ausschließlich den bisherigen Generator. Die logische Planung ist vollständig, **eine vollständige erfolgreiche geometrische Umsetzung jedes Plans ist derzeit nicht garantiert**. Bereits vorhandene Blueprint-Motive werden als Anker übernommen. Das Seed-Verhalten bleibt reproduzierbar; vorhandene Seed-Ausgaben können sich dadurch ändern.

**Offen:** Geometrische Einbettung ohne Rückfall für alle Stufen und Seitenverhältnisse, mehrere Abhängigkeitskanten pro Pfeil, tiefere kombinierte Muster, gezielte Spiralkonstruktion statt lediglich komplexerer Mehrfachknicke, höhere Belegungsdichte mit quantitativen Mindestquoten und belastbare Performancegrenzen. Auch eine systematische Schwierigkeitskalibrierung steht aus.

### Stufe direkt auswählen (Debug)

Im Hamburgermenü gibt es auf Debug-Builds den Eintrag **„Stufe anpassen“**. Damit lässt sich für Tests unmittelbar eine Stufe von 1 bis 9999 auswählen. Das Level wird im Hintergrund neu generiert, der Fortschritt der ausgewählten Stufe wird auf null gesetzt und das neue Rätsel als Spielstand gespeichert. Während der Generierung ist die Bestätigung gesperrt; Fehler erscheinen im Dialog.

Sichtbarkeit über `app/src/main/assets/game-defaults.properties`: `showStageAdjustment=true` (standardmäßig aktiviert). Mit `false` ist der Menüeintrag auch im Debug-Build ausgeblendet. In nicht debuggbaren Release-Builds ist der Eintrag unabhängig von diesem Wert nicht sichtbar. Änderungen an der Parameterdatei erfordern einen neuen APK-Build.

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

## Historische Kollisionstests (überholter Ansatz)

Frühe Tests gingen fälschlich von einer starren Pfeilverschiebung aus. **Verbindlich ist seit der Nutzerkorrektur ausschließlich das Schlangenmodell:** Der Körper folgt seiner eigenen Linie um Ecken; nur vor der Pfeilspitze werden neue Spielfeldzellen betreten. Ältere Einträge unten sind Entwicklungshistorie und keine aktuelle Spezifikation.

## Generatorqualität und Level-Vorbereitung (18.10.2026)

Neue Metrik `LevelQualityAnalyzer` ermittelt aus der tatsächlichen Geometrie Rasterbelegung, anfänglich freie Pfeile, vernetzte Pfeile, Kettentiefe, Schlüsselpfeile und die größte vollständig eingeschlossene freie Fläche. Die heuristische Qualitätsbewertung ersetzt nicht `Rules.solve`.

`PreparedLevels` speichert bis zu zwei vollständig vom Solver geprüfte Kandidaten für die nächste Stufe lokal in Android SharedPreferences, getrennt vom aktuellen Spielstand und nach Stufe und Seitenverhältnis getrennt. Die App versucht während der laufenden Stufe, für jeden Slot zwei deterministische Kandidaten zu berechnen und den höher bewerteten zu speichern. Ein bereits gespeichertes Level wird nach einem Neustart wieder geladen. Schlägt die Vorberechnung fehl, bleibt die normale direkte Generierung als Fallback verfügbar.

Ein **4-dp-Fortschrittsbalken** erscheint unten im Spielbereich, oberhalb der Systemnavigation. Die weiße Füllung zeigt ausschließlich den Anteil erfolgreich validierter und gespeicherter Slots an (0/2, 1/2, 2/2), **keine erfundene prozentuale Restlaufzeit** des Backtrackings. Die Vorbereitung arbeitet über Coroutines außerhalb des UI-Threads. Der Balken bleibt außerhalb des interaktiven Spielfeldes.

**Noch offen:** Echter regionaler Constraint-/Exact-Cover-Flächenplaner, gezielte Reparatur von Innenlücken, verbindliche Qualitätsgrenzen, umfangreicher Benchmark mit Laufzeitgrenzen und anschließend Minispiel-Übergänge. Der vorliegende Stand ist ein inkrementeller erster Baustein, nicht die abgeschlossene Neugestaltung des Levelgenerators. Optionales lokales ML bleibt zurückgestellt.

## Regionales Backtracking und Fallback-Kennzeichnung (18.10.2026)

`RegionalPacker` ergänzt den bisherigen Generator experimentell ab Stufe 35: Nach vollständiger und lösbarer Rückwärtskonstruktion werden die zuletzt eingebauten vier Pfeile entfernt und in einem begrenzten geometrischen DFS neu eingepasst. Kandidaten müssen innerhalb des Rasters liegen, kollisionsfrei sein und ihren Ausfahrstrahl gegenüber allen bereits platzierten Pfeilen freihalten. Ein neues Ergebnis wird nur akzeptiert, wenn es weiterhin lösbar ist und der gemessene Qualitätswert steigt. Das Suchbudget beträgt in der integrierten Erprobung 48 besuchte Knoten. Der bisherige Generator bleibt verfügbar.

Dies ist eine **regionale Constraint-Reparatur**, aber noch **kein vollständiger Exact-Cover-Algorithmus** für sämtliche freien Rasterzellen. Große innere Freiflächen lassen sich damit möglicherweise verbessern, aber noch nicht zuverlässig vollständig füllen. Auch eine garantierte höhere Belegung gibt es noch nicht.

**Unauffällige Testmarkierung:** Ist die erweiterte Konstruktion gescheitert und musste der ältere Konstruktionspfad verwendet werden (`Level.fallbackGeneration=true`), zeichnet die App die Rasterpunkte in dezentem Sandton statt Blaugrau. Es gibt bewusst weder ein Label noch zusätzliche Bedienelemente. Das Merkmal überlebt die Portraitrotation, das lokale Speichern und die Vorausberechnung. Historisch gespeicherte Level ohne Kennzeichen werden standardmäßig als normal angezeigt. Die Anzeige dient ausschließlich der visuellen Entwicklungskontrolle und ist kein allgemeines Gütesiegel; ein Level ohne Sandton muss nicht durch regionale Reparatur verbessert worden sein.

## Automatischer Generator-Benchmark und Level-PDF (18.10.2026)

Jeder erfolgreiche Android-CI-Build führt anschließend `gradle :game-core:benchmarkLevels` aus. Standardmäßig entstehen **112 Referenzlevel** (16 Stufen × 7 deterministische Seeds), mit wechselndem Seitenverhältnis. Der Gradle-Parameter `-PbenchmarkSeeds=100` erlaubt einen ausführlichen Lauf mit 100 Seeds **pro** Referenzstufe; dies kann entsprechend deutlich länger dauern.

Im GitHub-Actions-Artefakt `arrow-generator-benchmark` werden abgelegt:

- `levels.csv`: sämtliche Versuche inklusive fehlgeschlagener Generierungen, Laufzeiten, Abhängigkeits- und Dichtekennzahlen;
- `boards.tsv`: tatsächlich erzeugte Pfeil-Geometrien als maschinenlesbarer Export;
- `generated-levels.pdf`: visueller Katalog, zwei Raster pro A4-Querformatseite, mit Original-Pfeilpfaden, Stufe, Seed, Belegung, freien Pfeilen, Kettentiefe und sandfarbener Fallback-Markierung;
- `summary.txt`: Anzahl erfolgreicher und fehlgeschlagener Generierungen.

Die PDF wird **aus den exportierten Levelkoordinaten** gezeichnet und nicht durch erneute (potenziell andere) Generierung. Die Benchmarkphase ist von der APK-Veröffentlichung entkoppelt: Fehler werden im Benchmark ausgewiesen, verhindern aber die Debug-APK nicht; die Reports werden auch bei Fehlern hochgeladen. Die Level-PDF dient der Entwicklerkontrolle, nicht als Spielanleitung.

## Eigenständiger Exact-Cover-Generator (18.10.2026)

Der neue `game-core/.../ExactCoverGenerator.kt` ist unabhängig vom bisherigen greedy Reverse-Builder. Für kleine Raster (bis 12×12) enumeriert er geradlinige und L-förmige Pfeilkandidaten. Ein DFS wählt jeweils die freie Zelle mit den wenigsten kompatiblen Kandidaten (MRV), belegt sämtliche Zellen eines Kandidaten, entfernt dadurch implizit überlappende Kandidaten und setzt bei Sackgassen zurück. Erst ein **vollständiger** Zellcover wird durch `Rules.solve` auf tatsächliche Pfeil-Lösbarkeit geprüft. Die Suche ist durch ein explizites Knotenbudget begrenzt. Die Tests prüfen vollständige Belegung, Lösbarkeit und Seed-Reproduzierbarkeit auf kleinen Referenzrastern.

**Architekturgrenze:** Dies ist ein unabhängiger, kleiner Constraint-Solver als tragfähiger Grundbaustein, noch keine produktive Lösung für Stufen 35–100. Größere Bretter benötigen Constraint-Propagation, freie Regionszerlegung, Verknüpfungsplanung und Qualitätsziele. Der Legacy-Generator bleibt unverändert als bisheriger App-Pfad und Fallback. Keine weiteren Greedy-Gewichte als Entwicklungsstrategie.

## Exact-Cover-Skalierung (18.10.2026)

Der unabhängige `ExactCoverGenerator` akzeptiert jetzt Testbretter bis zu 20×20 Zellen. Neben der MRV-Zellwahl prüft eine partielle Graph-Constraint-Propagation nach jedem hinzugefügten Pfeil, ob die bereits belegten Pfeile einen irreversiblen Freigabezyklus bilden. Ein solcher Suchzweig wird sofort abgebrochen. Jede vollständige Lösung bleibt weiterhin durch `Rules.solve` validierungspflichtig; das Knotenbudget schützt vor unbegrenzter Suche. Regressionstests üben zusätzlich 8×8 und 10×8 mit begrenztem Budget aus.

**Unverändert offen:** Die Testfelder dürfen bei erschöpftem Budget `Result.level=null` liefern. Produktionsgröße, garantierte Erfolgsquoten, Flächenregionen und tiefere Constraint-Propagation stehen weiterhin aus. Der Legacy-Pfad der App wurde nicht umgeschaltet.

## Exact-Cover-Benchmark in der CI (18.10.2026)

Zusätzlich zum bisherigen Legacy-Levelkatalog wird der unabhängige Exact-Cover-Generator bei jedem erfolgreichen Build in einem **separaten, zeitlich begrenzten** Gradle-Lauf untersucht (`:game-core:benchmarkExactCover`). Getestet werden die Raster **8×8, 12×12, 16×16 und 20×20**, standardmäßig mit zwei festen Seeds pro Raster und höchstens 600 DFS-Knoten je Versuch. Mehr Stichproben lassen sich mit `-PexactCoverSeeds=N` und `-PexactCoverNodes=N` anfordern.

Die Resultate landen gemeinsam mit der bisherigen PDF im CI-Artefakt `arrow-generator-benchmark`: `exact-cover.csv` enthält Erfolg, Solver-Suchknoten, Zeit, Knotenbudget, eventuelle Fehlermeldung und bei Erfolg die Qualität des tatsächlich vollständig belegten und lösbaren Rasters. `exact-cover-summary.txt` fasst die Erfolgsquoten nach Rastergröße zusammen. Ein ausgeschöpftes Suchbudget ist ein dokumentiertes Messergebnis, kein APK-Buildfehler. Auch für diese Phase gibt es eine siebenminütige Laufzeitbegrenzung; bei einem Timeout können die Benchmarkdateien fehlen.

**Noch keine Produktivschaltung:** Der unabhängige Exact-Cover-Generator bleibt experimentell. Erst Messdaten zeigen, welche Größen zuverlässig und schnell erreicht werden und welche weitere Constraint-Propagation bzw. Regionszerlegung nötig ist.

## Exact Cover: Pfeilanzahl, Windungen und visueller Benchmark (18.10.2026)

Der unabhängige `ExactCoverGenerator` unterstützt nun die optionale Vorgabe `targetArrows` für eine **exakte** Pfeilanzahl. Eine vollständige Rasterbelegung wird nur akzeptiert, wenn die Zahl der Pfeile genau dem Ziel entspricht und der Solver eine gültige Reihenfolge liefert. Teilbelegungen oder Ergebnisse mit abweichender Pfeilanzahl gelten als erfolglose Suche (Budgetlimit kann weiterhin erreicht werden).

Der geometrische Kandidatenraum umfasst nun neben Geraden und L-Formen auch ausgewählte viersegmentige Windungen mit mehreren Richtungswechseln; die Kandidaten werden pro Startzelle/Richtung einmalig erzeugt. Das allein garantiert weder komplizierte Formen im Endergebnis noch bestimmte Schwierigkeitswerte: Die geometrische Suche wählt weiterhin die erste zulässige Lösung.

Der CI-Benchmark vergleicht zusätzlich bei 8×8 und 12×12 Versuche mit festgelegter Pfeilanzahl (jeweils etwa ein Pfeil pro vier Zellen) gegen den freien Modus. `exact-cover.csv` enthält nun `requested_arrows`; fehlgeschlagene Constraints zählen als echte Messwerte und nicht als APK-Fehler. Aus dem Geometrieexport `exact-cover-boards.tsv` erzeugt die CI einen eigenen **`exact-cover-levels.pdf`**, getrennt von der Legacy-PDF. Alle Ausgaben liegen im bisherigen GitHub-Actions-Artefakt `arrow-generator-benchmark`.

Weiter offen bleiben Regionszerlegung, skalierbare Constraint-Propagation, zielgerichtete geometrische Komplexität und ein verlässlicher hoher Erfolgsanteil bei vorgegebener Pfeilanzahl. Der App-Generator bleibt unverändert.

## Exact-Cover: geometrische Mindestanforderungen (18.10.2026)

Neu: `ExactCoverGenerator.generate(..., targetArrows, minWindingArrows)` erlaubt neben der exakt gewünschten Pfeilanzahl eine **harte Mindestzahl mehrfach geknickter Pfeile**. Ein fertiges Raster wird nur akzeptiert, wenn sämtliche Constraints erfüllt sind; andernfalls meldet der Solver einen ergebnislosen/budgeterschöpften Suchlauf, ohne auf den alten Generator auszuweichen. Die Suche priorisiert benötigte Windungen bei der Kandidatenauswahl und verwirft Ziel-Pfeilzahlen, die mit den verbleibenden Slots unmöglich werden.

Die bisherige feste Annahme von maximal 16 belegten Zellen je Pfeil wurde durch die maximale tatsächliche Kandidatenbelegung ersetzt. Im CI-Benchmark verlangen 8×8- und 12×12-Testfälle mit Ziel-Pfeilanzahl zusätzlich zwei Windungen. `exact-cover.csv` zeigt neu `requested_windings` und `actual_windings`, um Erfolgsquote und Geometrie ausdrücklich zu messen. Die Testfälle für die neue Constraint-Kombination sind ergänzt.

Weiter ausstehend: Regionszerlegung, Komponenten-Propagation, bessere Erfolgsquoten großer Zielfelder und produktive App-Integration. Unbegrenzte Suchgarantien bestehen nicht.

## Exact Cover: Regionen und Schwierigkeits-Constraints (18.10.2026)

Der unabhängige `ExactCoverGenerator` prüft während des Backtrackings die vierfach zusammenhängenden freien Regionen. Eine isolierte Einzelzelle ist nicht belegbar; bei vorgegebener Pfeilanzahl werden außerdem pro Region notwendige Unter- und Obergrenzen der noch benötigten Pfeile aufsummiert. Diese Kriterien sind **notwendige Bedingungen**, kein vollständiger Beweis dafür, dass eine Region später belegbar ist.

Neu sind die optionalen **harten Ergebnis-Constraints** `maxInitiallyFree` und `minChainLength`. Erst ein vollständig belegtes, lösbares Rätsel, dessen Qualitätsmetriken die Vorgaben erfüllen, wird akzeptiert. Die Suche darf weiterhin mit `level=null` enden. Die Benchmark-Zielkonfiguration verlangt bei jeder Rastergröße exakt `size²/4` Pfeile, mindestens zwei Windungen, maximal die Hälfte dieser Pfeile anfänglich frei und eine Kettentiefe von mindestens `size/2`. Dazu wird ein freier Vergleichsmodus ohne diese Constraints ausgeführt.

Die CI prüft jetzt standardmäßig zehn feste Seeds pro Größe statt zwei. Bei vier Rastergrößen und zwei Modi ergeben sich 80 Vergleichsversuche, jeweils mit 600 Knoten Suchbudget sowie bestehendem Gesamttimeout der Benchmark-Phase. Größere Stichproben sind **keine Erfolgsgarantie**; der Vergleich der Modi muss anhand von Erfolg, Suchknoten und Laufzeiten erfolgen. Der Legacy-Generator und der App-Pfad bleiben unverändert.

## Verbindlicher Exact-Cover-Entwicklungsplan (Oktober 2026)

Die Reihenfolge bleibt unabhängig von einzelnen Benchmark-Ergebnissen:

1. **Solver-Optimierung:** (A) Kandidaten nach verbleibender mittlerer Pfeillänge priorisieren, (B) freie Regionen anhand tatsächlich möglicher Pfeilkandidaten begrenzen, (C) wiederholte Zwangsplatzierungen/Constraint-Propagation vor regulärem Backtracking.
2. **Benchmark:** CSV-Spalten korrigieren, kontrollierten Vergleich gegen Build #221 durchführen, jeweils mehrere Seeds und zwei Suchbudgets; Fehlschläge nach Ursache differenzieren.
3. **Schwierigkeitsprofile:** altersgerechte Stufenprofile nach anfangs freien Pfeilen und Abhängigkeitstiefe.
4. **App-Integration:** erst nach objektiver Validierung, mit vorhandener Vorberechnung und Legacy-Fallback.

**Stand Phase 1:** A und B sind im Commit 109ca24 implementiert. C ist noch offen: MRV erkennt bereits Ein-Kandidaten-Zellen, aber eine eigenständige iterative Zwangsplatzierungsphase mit gemeinsamem Rollback ist noch nicht umgesetzt. In den nächsten Arbeiten nur Phase 1 abschließen, bevor Phase 2 beginnt. Die Geometrie und der Legacy-Generator bleiben unverändert.

## Exact Cover – Phase 1C abgeschlossen (Oktober 2026)

Der zellorientierte Exact-Cover-Solver führt nun **iterative Zwangsplatzierungen** aus: Wenn für eine noch freie Zelle nur ein zulässiger Kandidat verbleibt, wird dieser gesetzt und die Suche erneut nach erzwungenen Kandidaten durchsucht. Entsteht ein Widerspruch (kein Kandidat, Region nicht belegbar, Abhängigkeitszyklus), werden **sämtliche Zwangsplatzierungen des aktuellen Suchaufrufs** gemeinsam zurückgenommen. Der normale Entscheidungszweig besitzt seinen eigenen Rollback; die Rekursion kann dadurch keine belegten Zellen oder Pfeile in benachbarte Suchzweige verschleppen. Die bestehende Pfeillängenpriorisierung, Regionsprüfung, Zielpfeilzahl und Schwierigkeit bleiben erhalten; die maximale Knotenzahl bleibt begrenzt.

Phase 1A–1C ist damit im Code implementiert. **CI-Validierung und quantitativer Vergleich stehen noch aus.** Verbindliche Reihenfolge unverändert: danach Phase 2 (CSV/Benchmark gegen #221), Phase 3 (Schwierigkeitsprofile), Phase 4 (App-Integration). Keine Änderungen am Legacy-Generator.

## Dokumentationsstand / Änderungsverlauf

- **18.10.2026 – Eigenständiger Exact-Cover-Prototyp:** Neuer zellbasierter MRV-/DFS-Solver mit vollständiger Rasterbelegung und abschließender Lösbarkeitsprüfung für kleine Raster. Produktionsintegration erst nach Benchmark und regionaler Skalierung.


- **18.10.2026 – Generator-Verbesserung nach Benchmark #188:** Vor dem Abbruch einer Pfeilplatzierung prüft der Reverse-Builder jetzt auch solche geometrisch gültigen Formen und Positionen, die der schnelle Standardheuristikpfad aus Performancegründen ausschließt. Die regionale Kandidatensuche zieht zentrale Innenlücken gezielt in ihren Suchpool ein und mischt diese mit global gestreuten Kandidaten. Eine Regression für Stufe 44, Seed 15855, Seitenverhältnis 1,65 adressiert den im Benchmark entdeckten Fehler. Dies ist eine inkrementelle Robustheits-/Innenflächenverbesserung; der vollständige regionale Exact-Cover-Solver und eine garantierte Dichteprogression sind weiterhin offen.


- **18.10.2026 – CI-Levelkatalog:** Referenzlevel-Benchmark mit CSV, vollständigem Geometrieexport, Zusammenfassung und visuellem Levelkatalog als PDF in GitHub-Actions-Artefakten.


- **18.10.2026 – Regionale Constraint-Reparatur:** Begrenztes geometrisches Backtracking zur Neuordnung von vier zuletzt konstruierten Pfeilen, ausschließlich bei besserem Qualitätswert und gültigem Solver. Fallback-Level sind anhand dezent sandfarbener Rasterpunkte erkennbar; Merkmal ist in Level-Spielstand und Cache gespeichert. Ein vollständiger Exact-Cover-Generator steht weiterhin aus.


- **18.10.2026 – Qualitätsmetriken und Vorausberechnung:** Levelqualität wird anhand tatsächlicher Raster- und Graphparameter analysiert. Die App berechnet und speichert bis zu zwei validierte Folgelevel lokal und visualisiert gespeicherte Slots durch einen schmalen weißen Fortschrittsbalken am unteren Rand. Constraint-Flächenzerlegung, Minispiele und ML sind weiterhin offene Schritte.


- **15.10.2026 – Innenlücken-Füllung, erste Optimierung:** Ab Stufe 35 werden bei der Pfeilplatzierung bislang ungenutzte innere Rasterzellen, zentrumsnahe Abdeckung und Nachbarschaft zu bestehenden Pfeilen zusätzlich bewertet. Die Bewertung bleibt weich; Ausfahrwegfreiheit und Solver müssen weiterhin erfüllt sein. Ziel ist weniger ausschließlich randorientierte Pfeilbelegung. Die Konstruktion ist weiterhin greedy mit sicherem Fallback; systematisches Füllen aller freien Innenregionen und garantierte Zielbelegungsquoten stehen noch aus.


- **15.10.2026 – Qualitätsoptimierung nach Gerätetests:** Die Screenshots der Stufen 35/45/60 zeigen trotz erster Gruppenplanung weiterhin viele sofort freie Randpfeile und große freie Rasterbereiche. Die geometrische Kandidatenbewertung berücksichtigt ab Stufe 35 nun zusätzlich, wie viele **bislang freie** Pfeile durch einen neu eingefügten Pfeil tatsächlich blockiert werden. Redundante Graphkanten zu bereits blockierten Pfeilen werden dadurch relativ abgewertet; eine geringe Richtungsbalancierung vermindert wiederholte Randpfeil-Ansammlungen. Tests prüfen die wirkliche Anfangsfreiheit anhand der Kollisionsgeometrie über Seeds und Seitenverhältnisse. Qualitätsquoten und vollständige räumliche Verschachtelung sind weiterhin offen.


- **15.10.2026 – Geplante vernetzte Startgruppen:** Nach erfolgreichem Build #153 erzeugt der Generator ab Stufe 35 versuchsweise eine seedabhängige, logisch zusammenhängende Gruppe aus fünf Pfeilen über den DependencyPlanner und einen geometrischen DFS mit maximal 160 Knoten. Die Gruppe wird vor der restlichen Belegung platziert und ihre geplanten Abhängigkeiten werden am fertigen Level überprüft. Falls die Suche oder die anschließende vollständige Konstruktion scheitert, wird der bewährte Konstruktor ohne vorgeplante Gruppe verwendet. Die tatsächliche Realisierungsquote und die Zahl anfänglich freier Pfeile sind noch nicht statistisch abgesichert; insbesondere sind verschachtelte Spiralformen und hohe Dichte keine garantierten Eigenschaften.


- **15.10.2026 – Nutzerfeedback Stufe 45/60:** Screenshotanalyse ergab wiederkehrende Dreier-Restgruppe. Ursache war ein identischer Blueprint-Anker aus drei früh eingebauten und deshalb zuletzt entfernbaren Pfeilen. Ab Stufe 35 wird dieses feste Motiv nicht mehr vorgegeben; die Pfeile entstehen aus der seedabhängigen Rückwärtskonstruktion. Eine höhere Bewertung realisierter Blockierbeziehungen soll unabhängig freie Pfeile reduzieren. Die tatsächliche Anfangsfreiheit und Restgruppenqualität müssen weiterhin quantitativ evaluiert werden. Regressionstest zur Abwesenheit des festen Ankers ergänzt. Keine Garantie gegen einzelne freie Pfeile oder dreigliedrige Restgruppen.


- **15.10.2026 – Smartphone-Hochformat:** Die bisher einzeilige, feste Werkzeugleiste drückte den App-Titel auf wenige Pixel Breite und beeinträchtigte die Darstellung des Spielfelds. Unter 650 dp nutzt die App nun zwei Zeilen: Logo/Titel und darunter drei gleichmäßig breite Aktionsbuttons. Tablet-/Breitbildlayout bleibt einzeilig. Android-CI und Geräteprüfung ausstehend.


- **15.10.2026 – Korrektur Build #146:** Sechssegmentige Korridorformen führten zu Platzierungssackgassen in hohen Stufen. Konstruktion versucht jetzt deterministisch zunächst den erweiterten Formenkatalog und wiederholt bei Scheitern den vollständigen Aufbau mit dem bisherigen Katalog. Die neuen Muster sind damit weiterhin experimentell und nicht für jedes Level garantiert.


- **15.10.2026 – Verschachtelte Korridorformen:** Referenzbilder komplexer Pfeilrätsel als geometrische Anregung ausgewertet. Ab Stufe 35 ergänzen offene sechssegmentige Wickelformen den Katalog, um lange Korridore, U-Kehren und ineinanderliegende Verläufe zu ermöglichen. Reverse-Konstruktion, Selbstüberschneidungsprüfung und Solver bleiben verbindlich. Seed-/Seitenverhältnistests ergänzt; tatsächliche Häufigkeit und 100%-Dichte sind noch nicht garantiert.


- **15.10.2026 – Generator-Ausbau (inkrementell):** Geometrisches DFS priorisiert jetzt Kandidaten mit vielen Verknüpfungen zu vorhandenen Ausfahrstrahlen und größerer tatsächlicher Zellbelegung. Bei erfolgloser Einbettung des logischen Plans erfolgt für Stufen 6–16 ein zweiter deterministisch variierter Geometrieversuch desselben Graphen, bevor der bewährte konstruktive Fallback greift. Regressionstest für deterministische und geometrisch korrekte Graphrealisierung ergänzt. Noch keine garantierte Planrealisierung, Spiral- oder Dichtequote.


- **15.10.2026 – Lizenzumstellung:** Neue Quellcodeversionen unter MPL-2.0; Benjamin Lilienthal als benannter Rechteinhaber, separate Kennzeichen- und Asset-Richtlinien, unveränderte historische GPLv2-Nutzungsrechte, dokumentierte KI-Provenienz und gesonderte Fremdrechteprüfung.


- **15.10.2026 – Debug-Stufenauswahl:** Hamburgermenü „Stufe anpassen“ ausschließlich bei debuggbarer App und aktiviertem Parameter `showStageAdjustment`; Zahleneingabe, neue Generierung im Hintergrund und Zurücksetzen des Stufenfortschritts für gezielte Generator-Tests.


- **15.10.2026 – CI-Regressionskorrektur:** Build #119 scheiterte in zwei Generator-Tests durch geometrische Platzierungssackgassen. Die zuvor verfrühte Erhöhung der Pfeilzahl wurde zurückgenommen (weiterhin maximal 40), besonders komplexe Wickelformen beginnen erst ab Stufe 60; die Dichtegewichtung wurde reduziert. Zielwerte bis 100 % bleiben ausdrücklich Planungsgrößen, keine realisierte Füllgarantie.


- **15.10.2026 – Weiterentwicklung Dichte und Formen:** Größerer Längenkatalog, experimentelle Wickelformen ab Stufe 60, weiterhin auf 40 begrenzte Pfeilzahl und vorsichtige Flächenbelegungspräferenz und instrumentierte Ist-/Zielbelegung. 100 % ist bisher lediglich ein langfristiger Zielwert, keine zugesicherte Generatoreigenschaft; Performance- und Gerätetests bleiben erforderlich.


- **15.10.2026 – TODO 1/2, erste Integration:** Seedabhängiger vollständiger logischer DAG-Planer, geometrisches Backtracking mit Rücknahme von Platzierungen, experimentelle Anbindung für Stufen 6–16 und Regressionstests für Seeds, Stufen und Seitenverhältnisse. Bei fehlgeschlagener Einbettung verbleibt bewusst der bewährte konstruktive Fallback; vollständige geometrische Graphumsetzung, garantierte Spiralen und hohe Vernetzungsquote sind noch offen.


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

- **14.10.2026 – Farbcode für Kinder vor dem Lesealter:** Die drei ständig sichtbaren Hauptaktionen tragen jetzt eigenständige Vollfarben und große Symbole: **Blau (↻) = Neustart**, **Grün (💡) = Tipp**, **Lila (↓) = Stufe zurück / leichter**. Lila ersetzt die zunächst erwogene orangefarbene Variante nach Nutzerwunsch. Das permanente Punkteraster wird etwas größer und kontrastreicher, bleibt aber optisch hinter den Pfeilen. Texte ergänzen die Symbole für lesende Kinder und Begleitpersonen.

## Entwicklungsabgleich mit der Planung – 14.10.2026

### Umgesetzt (Prototyp)
- Werbefreier/offlinefähiger Android-Prototyp mit Kotlin-Spielkern, Jetpack Compose, automatischen Tests, GitHub Actions und Debug-APK über GitHub Releases.
- Orthogonale Pfeile, gerade oder mit einem Knick im Generator; Schlangenbewegung, vorberechnete Blockierer-ID-Matrix, lösbare Seed-Rätsel, Tipp und begrenzte Fehlerabzüge bei unveränderter Blockierermenge.
- Stufenaufstieg (1–8: je 1 Rätsel, 9–16: je 2, später aufsteigend bis max. 10), „Neustart“ desselben Levels und „Stufe zurück“ um eins mit neuem Level.
- Paralleles Antippen als Standard, vier Ausfahrgeschwindigkeiten (58/32/20/11 ms/Zelle), drei farbcodierte Tasten und immer sichtbares Punkteraster.
- Eigenständige Erfolgs- und Niederlagenansichten mit sichtbarer Ladephase, vorbereitetem Folgerätsel, bewusstem Weiter und Wiederholungsoption.
- Erste musterbasierte **Auswahl** generierter Kandidaten: Schlüsselpfeile, abwechselnde Ketten und Verzweigungen je nach Stufe – **nicht garantiert**.

### Teilweise umgesetzt
- Animationspfade folgen der Schlangengeometrie, laufen aber zellenweise statt flüssig interpoliert. Die Leuchtspur betont jetzt sechs aufeinanderfolgende Positionen einschließlich freigewordener Schwanzzellen; Wirkung auf Geräten erneut prüfen.
- Komplexe Mehrfachknicke sind durch die Datenstruktur möglich, aber der Generator baut überwiegend gerade Pfeile und einen Knick. Geometrische Verschachtelungen und Spiralen noch nicht systematisch realisiert.
- Leben und Hinweise konfigurierbar, aber die vorgesehenen **Unendlich**-Optionen fehlen; der Schutz des Einstellungsbereichs für Eltern fehlt.
- Kindgerechtere UI und Ergebnisse vorhanden, jedoch noch keine eigentlichen Monstercharaktere, separate Aufstiegsinszenierung oder Sounds.

### Noch offen – empfohlene Reihenfolge
1. **Generator robust machen:** Key-Arrows, Wechselketten und Verzweigungen gezielt konstruieren, strukturell prüfen, Mustervarianten kombinieren, Schwierigkeit über viele Seeds und Altersgruppen kalibrieren.
2. **Spielstand dauerhaft speichern:** Stufe, Fortschritt, ursprüngliches Level/Seed, verbliebene Pfeile, Fehlerhistorie, Leben, Tipps und Einstellungen; automatisches Resume nach Prozessende.
3. **Bewegung und Bedienbarkeit verfeinern:** Kontinuierliche Interpolation, bessere Rückfahreffekte, visuelle Qualität, reale Tablet-/Bildschirmgrößen- und Accessibility-Tests.
4. **Kinder-UX vervollständigen:** Tutorial, sympathische Monster, echte Aufstiegsfeier, optional deaktivierbarer Ton, Zoom/geeignete Darstellung dichter Level.
5. **Regel-/Elternoptionen vervollständigen:** Unendlich Leben und Hinweise, geschützte Einstellungen/Elternbereich und geeignete Hilfetexte.
6. **Veröffentlichungsreife herstellen:** Viele Seed-/Leistungs-/Gerätetests, Datenschutz-/Offline-/Kinderschutzprüfung, Release-Signing und Play-Store-Vorbereitung. Spendenoption nur nach gesonderter Prüfung.

Die Planung beschreibt technische Arbeit, keine formale Releasefreigabe.

- **14.10.2026 – Statusabgleich und kräftigere Punktpulse:** Leuchtspur über bis zu sechs Schritte mit deutlich größerem Halo und dunklem Kern, auch auf gerade verlassenen Körperzellen. README-Statusmatrix und offene Entwicklungsschritte aktualisiert; veraltete Kollisionsdarstellung als historisch markiert.

- **14.10.2026 – Lokale Spielstandspeicherung für Kindertest:** Der Stand der aktuellen Stufe und ihrer erfolgreich abgeschlossenen Rätsel sowie alle vorhandenen Einstellungen (Leben, Hinweise, Geschwindigkeit, parallele Pfeile) bleiben nach App-Neustart erhalten. Ebenfalls gesichert: ursprüngliche Levelgeometrie/Seed, verbleibende Pfeile, verbleibende Herzen/Tipps, bereits gezählte Blockierungsfehler und Ergebnisbildschirm. Die Speicherung erfolgt privat auf dem Android-Gerät über versionierte SharedPreferences und wird beim Start geprüft. Ein laufender grafischer Animationsschritt wird nicht wiederaufgenommen; die logisch entfernten Pfeile bleiben entfernt. Datenmigration und instrumentierte Gerätetests sind noch offen.

- **14.10.2026 – Persistenz-Fix nach Gerätetest:** Speicherformat um `activeMaxLives` und `activeMaxHints` ergänzt, da die neuen Einstellungen erst beim nächsten Rätsel gelten und bisher fälschlich zur Rekonstruktion der laufenden Leben-/Tippzähler verwendet wurden. Bei defektem Rätsel-Snapshot versucht die App nun, mindestens Stufe, Stufenfortschritt und Einstellungen zu retten und ein neues Rätsel auf derselben Stufe zu erzeugen; Ladeprobleme werden in Logcat protokolliert. Die älteren Speicherstände ohne aktive Maximalwerte bleiben lesbar, sofern die Einstellungen zum laufenden Rätsel passen. **Punktanimation zurückgenommen:** vier statt sechs Leuchtpositionen, kleinere und transparentere Halos; permanentes Punkteraster unverändert.

- **14.10.2026 – Verdichten und später erneut wachsen:** Raster und Pfeilanzahl bleiben in Stufen 1–5 wie zuvor. Stufen 6–15 behalten ein 25×25-Raster und erhöhen vor allem die Zahl der Pfeile. Ab Stufe 16 wächst das Raster alle zehn Stufen moderat um zwei Punkte je Achse (27×27 ab Stufe 16, 29×29 ab 26 usw., gedeckelt bei 35×35). Die Zahl der Pfeile steigt weiter, bis höchstens 52. Zentrale, zusammenhängendere Anordnungen werden bevorzugt, geometrische Lösbarkeit bleibt Pflicht. Die Werte sind heuristische Ausgangspunkte und müssen anhand von Gerätetests und Schwierigkeit kalibriert werden. Tests für die Wachstumsstufen und größere Speicherstände angepasst; garantierte Mustervorlagen weiterhin offen.

- **14.10.2026 – Kindertest: Schutzabstand:** Der Fortschrittsblock mit Stufe, Fortschritt, Herzen und Tipps wurde zwischen Spielfeld und die drei farbigen Aktionsbuttons verschoben. Als nicht anklickbarer Zwischenbereich schützt er vor versehentlichen Aktionen beim Antippen von Pfeilen am unteren Spielfeldrand. Die Bedienfunktionen und Farben bleiben unverändert.

- **14.10.2026 – Graph-first-Mustergenerierung (erste konstruktive Ausbaustufe):** Ab Stufe 6 wird zunächst ein kleines zentrales Motiv mit fest vorgegebenen Blockierer-Kanten geometrisch konstruiert. Stufen 6–8: ein Schlüsselpfeil blockiert mindestens zwei weitere Pfeile; Stufen 9–16: eine tatsächliche Rechts-/Abwärts-/Links-Abhängigkeitskette mit zwei aufeinanderfolgenden Kanten; ab 17: ein Schlüsselpfeil erschließt zwei unterschiedliche Freigaberichtungen (Verzweigung). Die komplette Geometrie wird durch Blockierermatrix und Solver überprüft, anschließend werden weitere Pfeile nach der bestehenden rückwärts lösbaren Konstruktion ergänzt. Stufen 1–5 bleiben absichtlich ohne diese vorgegebenen Motive. Neue Tests prüfen die realisierten Graphkanten über mehrere Stufen und Seeds. **Grenze:** Diese erste Version arbeitet mit festen, zentrierten Grundmotiven; variable Positionen/Rotationen, komplexe zusammengeführte Zweige und kombinierte Muster folgen später. Bei sehr hohen Pfeilzahlen bleiben Laufzeit und Abwechslungsgrad zu testen.

- **14.10.2026 – GitHub Actions #49:** Kotlin-Tests und Android-APK waren erfolgreich; die Veröffentlichung scheiterte ausschließlich an einem gleichzeitigen Push auf das bewegliche Release-Tag `develop-preview`. Die Workflow-Konfiguration serialisiert jetzt Läufe pro Branch mittels `concurrency` und bricht veraltete Läufe ab, um konkurrierende Release-Updates zu vermeiden. Die Funktionsprüfung des neuen Workflows erfolgt durch GitHub Actions.

- **14.10.2026 – Varianten der Graph-first-Motive:** Die garantierten Schlüsselpfeil-, Wechselketten- und Verzweigungsmotive werden ab Stufe 6 deterministisch aus dem Level-Seed gedreht (0/90/180/270 Grad), gespiegelt und um wenige Rasterpunkte verschoben. Alle Körperpunkte, Knicke und Pfeilrichtungen werden gemeinsam transformiert, sodass die vorgegebenen Blockierer-Kanten erhalten bleiben. Der Solver prüft weiter die Lösbarkeit. Zusätzliche Regressionstests kontrollieren Variantenvielfalt, wiederholbare Seeds und die tatsächlichen geometrischen Abhängigkeiten. **Weitere kombinierte und längere Muster stehen weiterhin aus.**

- **14.10.2026 – Intelligente Touch-Fangbereiche (Kindertest):** Die unsichtbaren Antippbereiche wurden vergrößert. Für aktuell frei ausfahrbare Pfeile gilt ein Radius von 1,75 Rasterzellen, für momentan blockierte Pfeile 1,25 Rasterzellen (vorher für alle ca. 1,1). Bei Überschneidungen erhalten freie Pfeile den Vorrang; innerhalb der jeweiligen Gruppe gewinnt der geometrisch nächste Pfeil. Blockierte Pfeile bleiben ausdrücklich antippbar, damit Fehler-/Herzen-Regeln weiterhin funktionieren. Die sichtbare Grafik bleibt unverändert. Regressionstests decken Überschneidungen, gezielte Antipps und Berührungen außerhalb des Rasters ab. Die Empfindlichkeit ist auf verschiedenen Tablet-Größen praktisch zu prüfen.

- **14.10.2026 – Bessere Flächennutzung nach Tablet-Test (Stufe 14):** Der Generator verteilt Pfeile ab Stufe 6 gezielter über die horizontalen und vertikalen Drittel des bestehenden Rasters, statt nahezu alle Pfeile in einer kleinen oberen Gruppe zu konzentrieren. Weniger belegte Zonen erhalten eine höhere Platzierungspriorität, weit isolierte oder randnah verstreute Pfeile werden zugleich abgewertet. Ab Stufe 6 dürfen zudem etwas längere Pfeile entstehen, ohne zusätzliche Knicktypen vorauszusetzen. Rastergröße und Stufen 1–5 bleiben unverändert; die konstruierten Abhängigkeitsmuster und Solver-Prüfung bleiben erhalten. Neue Tests prüfen die Nutzung beider Spielfeldhälften und die Lösbarkeit über mehrere Seeds. Ob die Verteilung optisch überzeugt, ist auf dem Tablet weiter zu prüfen.

- **14.10.2026 – Generator-Buildfehler #61 behoben (zu verifizieren):** Bei dicht belegten höheren Stufen konnte die Platzierung in eine geometrische Sackgasse geraten und `IllegalStateException` auslösen. Der Generator wiederholt in diesem Fall die vollständige deterministisch geseedete Kandidatenerzeugung mit einem anderen Zufallsstrom, statt die gesamte Levelgenerierung sofort abzubrechen. Zusätzlich stehen mehr Platzierungsversuche zur Verfügung. Musterkanten und Solver-Prüfung bleiben verpflichtend; der CI-Retest steht aus.

- **14.10.2026 – Kleine Raster und Displayformat:** Das Spielfeld bleibt bis einschließlich Stufe 24 bei höchstens 18 Punkten entlang der langen Achse (Stufe 1: 17), danach steigt die Größe schrittweise. Die kurze Achse wird beim App-Start anhand des Seitenverhältnisses reduziert; die Spielregeln, Austrittswege, Kollisionsmatrix, Generierung und Darstellung berücksichtigen Breite und Höhe getrennt. Portrait-Layouts drehen das gesamte Rätsel samt Pfeilrichtungen. Gespeicherte Rechteckfelder enthalten beide Dimensionen im neuen Speicherformat v2. **Frühere Spielstände werden auf ausdrücklichen Nutzerwunsch nicht migriert** und werden ab dieser Version nicht wiederhergestellt. Die Wirkung auf Tablet und die Generationszuverlässigkeit bei schmalen Feldern benötigen Gerätetest und CI.

- **14.10.2026 – Komplexere Pfeilformen:** Bei unveränderter Raster- und Pfeilzahlentwicklung werden ab Stufe 12 zweifach geknickte Zickzack-Pfeile ergänzt (ab Stufe 18 häufiger). Ab Stufe 24 kommen gelegentlich spiralähnliche Pfeile mit drei Knicken hinzu. Die geordneten Körperzellen bewegen sich entlang der eigenen Pfeilform, die Spitze verlässt das Spielfeld in Richtung des letzten Segments. Selbstüberschneidungen, Spielfeldgrenzen, andere Pfeile und Solver-Prüfung bleiben Ausschlusskriterien. Ergänzende Tests prüfen die Bewegung solcher Formen auch auf rechteckigem Raster. Bei zu engen Rastern sind komplexe Formen seltener; die Häufigkeiten sind auf dem Tablet zu kalibrieren.

- **14.10.2026 – Tabletfeedback zu Spiralpfeilen und Raster:** Bei komplexen Pfeilen darf die Ausfahrtrichtung den eigenen Körper nicht durchkreuzen. Der Generator verwirft solche geometrisch unglaubwürdigen Spiral-Kandidaten nun mit einer gesonderten Prüfung; ein Regressionstest deckt das ab. Ab **Stufe 22 bis 24** steigt die lange Rasterachse von 18 auf **19 Punkte**. Stufe 25 bleibt bei 20 Punkten, die übrige Größenprogression unverändert. Verifikation des konkreten Spiralverhaltens auf dem Tablet weiterhin erforderlich.

- **14.10.2026 – Generator stärker auf konstruierte, dichte Rätsel ausgerichtet:** Nach dem festen Abhängigkeits-Blueprint werden komplexe Pfeile gezielt in der frühen Füllphase (IDs 3–6) gesucht, statt sich auf zufällige Spät-Treffer zu verlassen. Zulässige Spiralformen (ab Stufe 24) und Zickzackpfeile (ab 12) bekommen eine höhere Priorität, ebenso Pfeile mit tatsächlichen Blockierer-Abhängigkeiten. Für Stufen 18–24 steigt das Ziel auf bis zu 20 Pfeile, ab 25 wächst es monoton weiter bis 40; die Rastergrößen bleiben unverändert. Mehr Platzierungsversuche sollen trotz dieser Vorgaben Lösbarkeit und Dichte ermöglichen. **Einschränkung:** Die komplexen Formen und die gesamte Belegungsdichte sind weiterhin nicht für jeden Seed garantiert; die tatsächliche Qualitätsverbesserung muss der CI-Lauf und der Tablet-Test bestätigen. Als weiterer Architekturpunkt bleibt eine vollständig konstruktive Abhängigkeitsplanung mit Platzierungs-Backtracking statt lokaler Kandidatenauswahl.

- **14.10.2026 – Build #84, Generator-Packungsstrategie:** Der Test `constructedGraphPatternsAreRealDependencies` scheiterte nach der Verdichtung, weil bei einzelnen späteren Stufen kein vollständiger Kandidat mehr geometrisch platziert werden konnte. Der Generator reserviert komplexe Pfeile für die erste Erzeugungsphase und verwendet in der zweiten Hälfte zunehmend kürzere, am Ende überwiegend gerade Füllpfeile. Die Zielanzahl, Solver- und Graphmusterbedingungen bleiben unverändert; mehr deterministische Suchläufe unterstützen schwierige Seeds. CI-Retest steht aus.

- **15.10.2026 – Kindersichere Kopfzeile:** Die drei Aktionsbuttons wandern vom unteren Spielfeldrand in die Kopfzeile neben den Spielnamen: von links nach rechts Neustart (blau), Stufe zurück (lila), Einstellungen (Zahnrad), Tipp (grün, rechts außen). Die Fortschrittskarte bleibt unter dem Spielfeld; am unteren Rand befinden sich keine destruktiven Aktionen mehr. Ein Antippen von „Stufe zurück“ öffnet jetzt einen kindgerecht formulierten Bestätigungsdialog mit großer Symbolik und Erklärung des Fortschrittsverlusts. Links bestätigt „Stufe zurück“ (lila); rechts bricht „Fortsetzen“ (blau) sicher ab. Rückstufung verändert den Spielstand erst nach expliziter Bestätigung. Auf kleinen Displays muss die Kopfzeile noch praktisch überprüft werden.

- **15.10.2026 – Hamburger-Menü links vom Spielnamen:** Der Einstellungsbutton wurde aus der Aktionsleiste entfernt und als Hamburger-Menü links vor „OttOSpiele“ umgesetzt. Das Dropdown enthält zunächst „Einstellungen“ mit dem bisherigen Dialog und „Über das Spiel“ mit kurzen Informationen. Die Aktionsbuttons rechts bleiben Neustart, Stufe zurück und Tipp; der Bestätigungsdialog für Rückstufungen bleibt erhalten. Weitere Menüeinträge können später ergänzt werden.

- **15.10.2026 – Ausfahrweg-Vorschau per Gedrückthalten:** Ein Pfeil lässt sich jetzt durch Auflegen und Halten des Fingers vorab ansehen: Eine gestrichelte, halbtransparente Linie zeigt seinen geraden Ausfahrweg von der Spitze bis zum Spielfeldrand, im Hintergrund unter allen Pfeilen. Wird der Finger aus dem Fangbereich des ausgewählten Pfeils weggezogen, verschwindet die Linie und die Geste wird endgültig abgebrochen (ohne Pfeilauslösung oder Herzenverlust). Erst das Loslassen über demselben Pfeil löst die bisherige Antipp-Aktion aus. Auch blockierte Pfeile zeigen den gesamten theoretischen Ausfahrweg zur Orientierung. Die Funktion benötigt eine manuelle Prüfung auf dem Tablet, insbesondere bei schnellen Antipps und parallelen Animationen.

- **15.10.2026 – Zurückhaltende Ausfahrweg-Vorschau:** Die Linie erscheint erst nach 1.000 ms ununterbrochenem Halten des ausgewählten Pfeils, statt sofort beim Auflegen. Sie ist nun durchgezogen, wesentlich dünner und transparenter (24 % Alpha statt 60 %). Loslassen oder Wegziehen unterbricht auch die wartende Vorschau, ohne die bekannte Antipp- bzw. Abbruchfunktion zu verändern. Tablet- und CI-Prüfung ausstehend.

- **15.10.2026 – Konfigurierbare Ausfahrweg-Vorschau:** Die Haltezeit beträgt nun standardmäßig **400 ms**. Sie wird aus `app/src/main/assets/game-defaults.properties` unter dem Parameter `arrowPreviewHoldMillis=400` geladen und kann durch Anpassung dieser Datei ohne Änderung des Kotlin-Quellcodes variiert werden (anschließend APK neu bauen/installieren). Zulässiger Bereich: 100–5000 ms, ungültige/fehlende Werte fallen auf 400 ms zurück. Das bisherige Verhalten beim Loslassen und Wegziehen bleibt unverändert.

- **15.10.2026 – Einstellungs-Defaults zentral konfigurierbar:** `app/src/main/assets/game-defaults.properties` enthält nun neben `arrowPreviewHoldMillis=400` auch `defaultLives=5`, `defaultHints=3`, `defaultConcurrentArrows=true` und `defaultAnimationSpeed=1` (0 Ruhig, 1 Normal, 2 Flott, 3 Turbo). `GameDefaults.kt` liest und validiert diese Werte; neue Spiele verwenden sie als Anfangseinstellung. Bereits gespeicherte Nutzereinstellungen haben Vorrang, damit Änderungen an der APK keine Kinderspielstände zurücksetzen. Auch Wiederherstellung beschädigter Spielstände berücksichtigt die konfigurierten Fallbacks. Ändern der mitgelieferten Datei erfordert einen erneuten APK-Build, keine Kotlin-Codeänderung.

- **15.10.2026 – Ausfahrweg besser sichtbar:** Die dezente, durchgezogene Vorschau bleibt bei 24 % Deckkraft und 400 ms Verzögerung. Die Strichstärke entspricht nun ungefähr dem Durchmesser der Rasterpunkte (0,23 Rasterzellen, mindestens 3,3 px) statt zuvor 0,055 Rasterzellen.

- **15.10.2026 – Regelbasierter Levelbau (erste konstruktive Implementierung):** `Generator` arbeitet ohne wiederholte zufällige Gesamtlevel-Versuche: Ein deterministisch nach Seed variierter Katalog gerader, geknickter, Zickzack- und offener Wickelformen wird auf dem Raster platziert. Der Aufbau erfolgt in *umgekehrter Entfernungsreihenfolge*, sodass jeder neu hinzukommende Pfeil seinen eigenen Ausfahrweg in Anwesenheit aller zuvor eingebauten Pfeile frei hat und erst dann bestehende Pfeile blockiert; daraus entsteht ein gerichteter azyklischer Abhängigkeitsgraph. Bewertet werden geometrische Komplexität, Blockierbeziehungen und Flächenverteilung. Vorhandene Muster-Blueprints, Rastergröße und Pfeilzahl bleiben erhalten. Der vollständige Solver und neue deterministische Regressionstests validieren das Ergebnis. Die nächste Ausbaustufe sind vorgeplante mehrere verschachtelte Abhängigkeitsmotive und begrenztes Backtracking bei Platzierungsengpässen; die aktuelle Platzierung wählt innerhalb jedes Konstruktionsschritts deterministisch den besten Kandidaten. CI- und Tablet-Verifikation ausstehend.
