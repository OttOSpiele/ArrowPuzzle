# AntsBlockCollector – Phase 3 balancing diagnostics

AI-assisted experimental code; not yet integrated into Android. See draft PR #2.

## Run

```bash
./gradlew :game-core:test
./gradlew :game-core:benchmarkAnts -PantsSeeds=8
```

Generated in `game-core/build/benchmark/`:
- `ants-block-balance.csv`: one deterministic policy run per generated round.
- `ants-block-summary.csv`: aggregate wins, blocks, unknowns, opening choice safety and occupied slots.

Profiles use 8×8/2 colors, 12×12/3, 16×16/4 and 20×20/5. These are **diagnostic profiles, not approved difficulty settings**.

## Definitions

- **Available**: immediately selectable front stack among four FIFO queues with a free slot.
- **Proven safe**: the bounded exhaustive continuation search found a win after this choice.
- **Proven blocked**: all subsequent combinations were exhausted without a win.
- **Unknown**: search budget exhausted; never interpreted as blocked.
- **Simulation win/block**: outcome of a deterministic randomized policy, not the full search outcome.
- **Moves**: refill selections, not removed pixels or elapsed seconds.

The generator supplies a constructed solution witness. It does **not** yet optimize safe-choice breadth, difficulty or repeated dead-end rates. Random policies also cannot measure how a 5–6-year-old actually plays. An initial success-rate target must not be promoted to a quality gate until reliable numbers exist.

## Known limitations / next steps

1. Solver complexity grows rapidly with queue combinations; all search budgets are explicitly capped.
2. Most generated rounds derive queues from a simple deterministic round-robin constructive order; this may make early profiles too easy and does not yet exploit all five slots.
3. Quantify safe-choice ratios along complete playthroughs, not only at the first choice.
4. Add motif metrics (connected component sizes, isolated color islands and exposed-color count) before declaring imagery child-friendly.
5. Investigate representative seed failures and build a parameterized difficulty model only after analyzing CSV evidence.
6. Add a dedicated CI artifact step if benchmarking is stable; do not merge into ordinary ArrowPuzzle generation metrics.

The runtime may restart with a fresh seed only on a **proven** blocked state. UNKNOWN must preserve the ongoing round.
