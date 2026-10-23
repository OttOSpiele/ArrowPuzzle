# Puzzle It – Android mini-game (development)

> AI-assisted implementation on `develop`; human device validation and publication review are required.

## Integration

`MainActivity.Puzzle` owns ArrowPuzzle progress and asynchronous next-level preparation. On a qualifying stage transition (at least four puzzles completed since the previous mini-game), the stage-success screen offers Play. The host replaces the former placeholder with `PuzzleItScreen`, passing readiness/error states and a completion callback. Puzzle It never generates or mutates an ArrowPuzzle level.

`PuzzleItController` is a deterministic round state machine. The caller supplies `requiredRounds`, preparation readiness and a completion callback. The current ArrowPuzzle integration sets a practically unbounded round count and stops when the next ArrowPuzzle puzzle is ready. This avoids a competing level-generation loop. An already started Puzzle It round **always** finishes the jigsaw, quantity selection and success animation before returning. `requestStopAfterCurrentRound` is deferred until `finishRound`.

## Round state

`GENERATING → SCATTERING → PUZZLE_ACTIVE → CHOICE_ACTIVE → ROUND_SUCCESS → ROUND_FINISHING → { PUZZLE_ACTIVE | EXITING }`.

`PUZZLE_COMPLETE` is the conceptual transition after the ninth snap; the state machine advances directly to `CHOICE_ACTIVE` because no intermediate animation is necessary. A wrong quantity selection stays in `CHOICE_ACTIVE`, with that specific button disabled. `ROUND_SUCCESS` locks further inputs; a nonblocking coroutine delays by the configured success animation time, then calls `finishRound` exactly once.

The game controller owns the current round, the deferred stop flag and completed-round count. The host owns the stop decision, generator readiness and transition to ArrowPuzzle. There are no lives or time limits inside Puzzle It.

## Motifs and geometry

A seed determines the integer (1–10), palette color, twelve internal-edge polarities, answer options and a non-solved scatter order. The Android canvas draws the motif anew under each translated puzzle-piece clip, in shared 3×3 motif coordinates. Every piece uses the same global color/text/dot origin, including the interlocking tabs. The outlines are built from shared complementary edge polarities and Bézier curves; the full puzzle's outer boundaries remain straight. Touch picking uses the same Android `Path` rasterized to a `Region`, so transparent notches do not receive input.

The large text uses `Paint.getTextBounds` and `measureText` to fit a bounded area, leaving space for one or two compact rows of smaller dots below. Each answer button displays only dots representing the corresponding count.

Positions and snap radius use logical tile widths, with a 0.15-tile Euclidean tolerance. Already snapped pieces are immutable and drawn behind free pieces. Free pieces are painted above the target frame, and the dragged piece is raised above them.

The starting distribution uses a deterministic shuffled 3×3 staggered arrangement with bounded overlap; no image preview or internal grid is drawn. The finite arrangement serves as the safe fallback, independent of repeated random placement attempts.

## Lifecycle and configuration

`rememberSaveable` serializes the current logical round, positions, fixed-piece flags, answers, disabled options, completion count and stop intent as a small JSON string. This supports normal Android state restoration, including typical short interruptions and configuration recreation. Active touch gestures are not serialized. A process restart without restored Compose state may begin a fresh round.

Settings reside in `app/src/main/assets/game-defaults.properties` and are loaded by `GameDefaults`:
- `puzzle.snapToleranceRatio=0.15` (validated range 0.05–0.40)
- `puzzle.successAnimationMs=1000` (800–1200 ms)
- `puzzle.minVisibleAreaRatio=0.40` (0.25–0.90; reserved for richer future scatter validation)
- `puzzle.maxPlacementAttempts=100` (1–1000; reserved, since the bounded arrangement is used)
- `puzzle.preventImmediateNumberRepeat=true`
- `puzzle.bottomSafetySpacingDp=56` (24–100 dp; extra child-safe gap below buttons, additional to system navigation padding)

The first version deliberately fixes the 3×3 size, 1–10 number range, four answers and nonrotating pieces. None of these rules is user-adjustable.

## Verification

`app/src/test/java/de/ottospiele/app/PuzzleItModelTest.kt` tests deterministic seeds across 201 rounds, number and answer invariants, snap tolerance, disabled wrong answers, once-only completion and deferred stop. `.github/workflows/android.yml` now invokes `:app:testDebugUnitTest` before APK assembly.

**Open before release:** Android Compose/instrumented tests for actual `Path` complementarity, hit regions, drags and display sizes; manual phone/tablet layout review; timing and lifecycle tests under activity recreation. CI compilation is not by itself evidence of this behavior.
