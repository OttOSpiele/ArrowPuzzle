// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.ants

import kotlin.random.Random

/** Measurable decisions: safe means a proven successful continuation, not just any legal move. */
data class AntChoiceMetrics(
    val available: Int,
    val provenSafe: Int,
    val provenBlocked: Int,
    val unknown: Int
)
data class AntSimulation(
    val seed: Long,
    val moves: Int,
    val outcome: AntOutcome,
    val earlySafeChoices: Int,
    val earlyChoices: Int,
    val maxOccupiedSlots: Int
)

/** A reproducible, bounded diagnostic, NOT a certification of age suitability. */
object AntsBlockBalance {
    fun choices(round: AntRound, state: AntGameState, searchBudget: Int = 3000): AntChoiceMetrics {
        var available = 0
        var safe = 0
        var blocked = 0
        var unknown = 0
        for (column in 0..3) {
            val next = AntsBlockGame.place(round, state, column) ?: continue
            available++
            when (AntsBlockGame.analyze(round, next, searchBudget).outcome) {
                AntOutcome.WON -> safe++
                AntOutcome.BLOCKED -> blocked++
                AntOutcome.UNKNOWN -> unknown++
                AntOutcome.PLAYING -> error("Unexpected unresolved solver outcome")
            }
        }
        return AntChoiceMetrics(available, safe, blocked, unknown)
    }

    /**
     * Every game uses a deterministic yet different selection policy.
     * Only the first choice is exhaustively analyzed, keeping CI runtime bounded.
     * UNKNOWN is never treated as losing.
     */
    fun simulate(round: AntRound, seed: Long, maxMoves: Int = 500,
                 searchBudget: Int = 1000): AntSimulation {
        val rng = Random(seed xor 0x4D414E44414C41L)
        var state = AntsBlockGame.initial(round)
        val initial = choices(round, state, searchBudget)
        var maxSlots = 0
        for (move in 0 until maxMoves) {
            if (AntsBlockGame.isWon(round, state)) {
                return AntSimulation(seed, move, AntOutcome.WON,
                    initial.provenSafe, initial.available, maxSlots)
            }
            val moves = (0..3).mapNotNull { col ->
                AntsBlockGame.place(round, state, col)?.let { col to it }
            }
            if (moves.isEmpty()) return AntSimulation(seed, move, AntOutcome.BLOCKED,
                initial.provenSafe, initial.available, maxSlots)
            val chosen = moves[rng.nextInt(moves.size)]
            state = chosen.second
            maxSlots = maxOf(maxSlots, state.active.count { it != null })
        }
        return AntSimulation(seed, maxMoves, AntOutcome.UNKNOWN,
            initial.provenSafe, initial.available, maxSlots)
    }
}
