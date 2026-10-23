# Arrow Puzzle – manueller Android-Gerätetestkatalog

**Stand:** 23.10.2026 · **Zweig:** `develop` · **Status:** Testfälle definiert, nicht durchgeführt.

## Testprotokoll

Pro Gerät und Testlauf erfassen:
- Gerät / Modell, Android-Version, Displayauflösung, Orientierung, APK-Build und Commit-SHA
- Frische Installation oder Update über vorhandene Installation
- Testdatum, Tester, Ergebnis (PASS/FAIL/BLOCKED), erwartetes / beobachtetes Verhalten
- Bei Fehlern: Stufe, Seed soweit verfügbar, Screenshot/Video, Logcat-Auszug, Reproduktionsschritte und Wartezeit (Stoppuhr)

Keine gespeicherten Spielstände vor Update-Tests löschen. `Stufe anpassen` ist ausschließlich in der Debug-APK verfügbar.

## Aktuelles Generierungsmodell und Testabdeckung

- Stufen **1–8**: feste, solvergeprüfte Tutorial-Level; **kein Legacy-Laufzeitgenerator**.
- Ab Stufe **9**: stufenabhängiges Exact Cover. Die bisherige Generatorwechselgrenze 16/17 entfällt.
- `legacyEnabled=true` (aktueller Migrationsdefault): Legacy ist ausschließlich als konfigurierbarer Fallback vorhanden. `legacyEnabled=false`: Legacy ist auch als Fallback deaktiviert. Beide Konfigurationen müssen separat getestet werden.
- Statistik und PDF für Stufe 9–80 entstehen im separaten CI-Job `exact-cover-stage-series` ohne Legacy. Die historischen Legacy-/A/B-Benchmarks bleiben manuell ausführbar und gehören nicht zur Standard-CI.
- Testansprüche an den Schwierigkeitsverlauf sind **Prüfziele**, noch keine bestätigten Produkteigenschaften.

## P0 – verpflichtende Funktions- und Stabilitätstests

| ID | Vorgehen | Erwartung |
| --- | --- | --- |
| P0-01 | Debug-APK installieren, starten, erstes Rätsel spielen | Spielfeld und Bedienung korrekt; kein Absturz |
| P0-02 | Stufe 8 über Debug-Auswahl starten, lösen | Nach **einem** Rätsel Stufe 9 |
| P0-03 | Stufe 9 starten, ein Rätsel lösen, dann zweites | Anzeige 1/2, erst nach zweitem Rätsel Stufe 10 |
| P0-04 | Stufe 16 starten und zwei Rätsel lösen | 16/17-Übergang ohne Generatorwechsel; ab Stufe 9 Exact Cover bevorzugt, Legacy nur falls konfiguriert |
| P0-05 | Stufe 17 beginnen, Fortschritt 0/3 bis 3/3 durchspielen | Erst nach drittem Rätsel Stufe 18 |
| P0-06 | Stufen 29/30, 42/43, 54/55, 67/68 und 79/80 ansteuern | Erforderliche Rätselanzahl: 3/4, 4/5, 5/6, 6/7 und 7/8 |
| P0-07 | Auf Stufe 80 mehrere Rätsel lösen | Acht Rätsel bis zur nächsten Stufe; Raster maximal 20×20 bei Exact Cover |
| P0-08 | Nach der letzten gültigen Pfeilbewegung sofort Neustart, Stufe zurück, Tipp versuchen | Alle drei Buttons deaktiviert; nur regulärer Erfolgsübergang |
| P0-09 | Stufe zurück bestätigen, sofort schnell mehrfach auf bisherige Buttonposition tippen | Dialogaktionen verschwinden, Spinner erscheint, genau **eine** Stufe zurück |
| P0-10 | Während der Rückstufen-Generierung mehrfach Zurück/außerhalb des Dialogs tippen | Keine weiteren Rückstufungen, kein Abbruch mit inkonsistentem Spielstand |
| P0-11 | Nach Rückstufung Fortschritt prüfen | Fortschritt 0/n; alle n Rätsel bis zum erneuten Aufstieg |
| P0-12 | Niederlage absichtlich auslösen und „Noch einmal“ wählen | **Identisches** Rätsel, alle Pfeile zurück, volle Leben und Tipps |
| P0-13 | Nach Niederlage „Neues Rätsel“ wählen | Selbe Stufe, neuer Seed/Rätsel, Stufenfortschritt 0/n |
| P0-14 | App während eines ungelösten Rätsels in den Hintergrund schicken und zurückkehren | Pfeile, Fehlversuche, Leben, Tipps und Stufe bleiben erhalten |
| P0-15 | App vollständig schließen, erneut starten | Fortschritt und aktives Rätsel wiederhergestellt |
| P0-16 | Eine frühere APK mit existierendem Spielstand durch neue APK ersetzen | Kein unbeabsichtigter Datenverlust; Migration dokumentieren |
| P0-17 | Während Vorberechnung spielen und Fortschrittsstreifen beobachten | Spielfeld bleibt bedienbar; Balken wächst bei abgeschlossenen Levels |
| P0-18 | Stufe 9, 17 und hohe Stufen ohne vorbereitetes Level starten | Kein UI-Freeze; Ladezeiten erfassen, ggf. Fallback überprüfen |
| P0-19 | Portrait und Landscape, schmaler Bildschirm und Tablet | Keine abgeschnittenen Buttons, Dialoge oder unbedienbaren Pfeile |
| P0-20 | Wiederholt gleiche blockierte Pfeile ohne Blockeränderung tippen | Nur erster identischer Fehlversuch kostet ein Leben |
| P0-21 | Tipps 0/1/3/5 und Leben 3/5/10 einstellen und Neustart prüfen | Einstellungen gespeichert, Tipp zeigt aktuell freien Pfeil |
| P0-22 | Längere Pfadvorschau durch Gedrückthalten testen | Erscheint nach konfigurierter Verzögerung; Pfad geometrisch korrekt |

## P1 – Generator- und Spielerlebnis

| ID | Vorgehen | Erwartung |
| --- | --- | --- |
| P1-01 | Mindestens 20 Rätsel aus Stufen 9–24 spielen; Generationstyp/Seed erfassen | Exact-Cover-/Fallback-Quote messbar, keine unlösbaren Rätsel |
| P1-02 | Generierungszeit bei 9, 16, 17, 24, 35, 60 und 80 für je fünf Seeds messen | Verteilung und Ausreißer dokumentiert, keine blockierte UI |
| P1-03 | Tutorial-, Exact-Cover- und bei aktivierter Legacy-Option Fallback-Darstellung vergleichen | Fallback zuverlässig identifizierbar |
| P1-04 | Stufen 8/9, 16/17 und 23/24 direkt vergleichen | Keine unplausiblen Sprünge in Lesbarkeit/Schwierigkeit |
| P1-05 | Dichte und gewundene Pfeile auf kleinem Smartphone untersuchen | Pfeile, Spitze und Touchziel bleiben unterscheidbar |
| P1-06 | Mindestens zehn aufeinanderfolgende Rätsel auf unterschiedlichen Stufen bewerten | Abwechslungsreiche Geometrien/Abhängigkeiten |
| P1-07 | Dichte 20×20 auf kleinem und großem Display | Kein unverhältnismäßiger Platzmangel oder Dauerzoom |
| P1-08 | Mehrfaches Drehen, Sperren/Entsperren, Appwechsel während Spiel und Generierung | Kein Crash, keine widersprüchliche Anzeige, kein Spielstandverlust |
| P1-09 | Fehlerhafte oder veraltete Vorberechnungsdaten simulieren (separater Testaufbau) | Nur validierte Levels geladen; sichere Regeneration |
| P1-10 | Exact-Cover-Suchbudget gezielt ausschöpfen (Diagnosebuild/Testhook) bei legacyEnabled=true/false | Mit true: validierter Legacy-Fallback; mit false: kein Legacy und kein unlösbares Rätsel; fehlenden Level sicher behandeln |

## Abschlussbedingungen

Für eine positive Freigabe müssen sämtliche P0-Tests auf mindestens einem kleinen Smartphone und einem Tablet bestanden sein. P0-14 bis P0-16 zusätzlich mit Updatepfad prüfen. Für P1 Generierungsdauer, Fallback-Rate und Spielerfeedback dokumentieren, keine pauschalen Qualitätsversprechen aus einzelnen Seeds ableiten.

**Offen:** Die P1-10-Fallback-Erzwingung benötigt einen geeigneten Diagnosetesthook; ohne diesen kann nur ein natürlich aufgetretener Fallback geprüft werden.

## Zusatztests – zeitbudgetierte Level-Vorbereitung

| ID | Vorgehen | Erwartung |
| --- | --- | --- |
| PREP-01 | Stufe 1, 9, 17, 30, 68 und 80 einstellen; Vorratsgröße nach abgeschlossener Vorbereitung kontrollieren | Jeweils benötigte Rätsel + 2 für aktuelle Stufe (3, 4, 5, 6, 9, 10), zusätzlich zwei für nächste Stufe |
| PREP-02 | Mehrere Rätsel innerhalb einer Stufe abschließen, Cache nach jedem Wechsel prüfen | Verwendete Seeds nicht erneut angeboten; Vorrat wird nachgefüllt |
| PREP-03 | Ab Stufe 9 Diagnose exportieren | Erfolgreiche, ergebnislose und im Spiel nicht verwendete Generierungsversuche enthalten |
| PREP-04 | Viele fehlgeschlagene Exact-Cover-Suchen auslösen | Nur bei legacyEnabled=true werden nach dem Budget gültige Legacy-Fallbacks vorbereitet; bei false ausschließlich Exact Cover |
| PREP-05 | Budget testweise auf 0 setzen und Debug-APK neu bauen | Bei legacyEnabled=true werden validierte Legacy-Level verwendet; bei false darf kein Legacy erzeugt werden |
| PREP-06 | Ein sehr kleines Zeitbudget (z. B. 100 ms) einstellen | Kein dauerhaftes UI-Blockieren; einzelne Suche kann ihr Knotenlimit noch abschließen |
| PREP-07 | Während laufender Vorbereitung das Rätsel spielen, App wechseln oder Stufe ändern | Keine doppelt konsumierten Seeds, kein Crash und korrekt fortgesetzter Stufenfortschritt |
| PREP-08 | CSV-Export auf Stufe 9+ analysieren | Zahl und Dauer aller Kandidaten sichtbar; Rejected-Versuche nicht als App-Fehler gezählt |

## Ergänzende Migrationstests – Tutorial und Exact Cover

| ID | Vorgehen | Erwartung |
| --- | --- | --- |
| MIG-01 | Alle Stufen 1–8 nacheinander prüfen | Acht deterministische, lösbare, fest definierte Tutorial-Level; keine Laufzeit-Legacy-Generierung |
| MIG-02 | Stufen 8, 9, 10, 16 und 17 vergleichen | Exact Cover beginnt ab 9; keine künstliche Generatorgrenze 16/17 |
| MIG-03 | `legacyEnabled=false` setzen, Debug-APK neu bauen; Stufen 9–24 testen | Keine Legacy-Levels in der Diagnose; jeder präsentierte Level ist lösbar |
| MIG-04 | Bei deaktiviertem Legacy einen fehlgeschlagenen Exact-Cover-Versuch provozieren | Kein unlösbarer oder leerer Spielzustand; fehlende Level müssen sicher behandelt werden; tatsächliches Verhalten dokumentieren |
| MIG-05 | `legacyEnabled=true` wiederherstellen; Suchbudget erschöpfen | Legacy-Fallback bleibt funktional und als solcher erkennbar |
| MIG-06 | Update mit altem Vorberechnungs-Cache installieren | Versionierter Alt-Cache wird verworfen, aktueller Spielstand nicht unbeabsichtigt gelöscht |
| MIG-07 | CI-Artefakt `exact-cover-stages-9-80` prüfen | Statistik je Stufe 9–80 sowie PDF für Referenzstufen mit bis zu acht solvergeprüften Beispielen; Fehlversuche bleiben sichtbar |
