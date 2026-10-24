// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.ants

/** Immutable logical state. No frame time, effects or animation state participates in the rules. */
data class AntGameState(
    val pixels: List<Int>,
    val queueOffsets: List<Int> = listOf(0, 0, 0, 0),
    val active: List<AntStack?> = List(5) { null }
) {
    init {
        require(queueOffsets.size == 4 && queueOffsets.all { it >= 0 })
        require(active.size == 5 && active.all { it == null || it.capacity > 0 })
    }
}

enum class AntOutcome { PLAYING, WON, BLOCKED, UNKNOWN }

/** UNKNOWN means a bounded search exhausted its budget: it must NEVER cause a restart. */
data class AntAnalysis(val outcome: AntOutcome, val visitedStates: Int)

object AntsBlockGame {
    fun initial(round: AntRound): AntGameState = AntGameState(round.pixels)

    /**
     * Remove accessible cells until no active stack can proceed.
     * Fixed slot order makes resolution reproducible regardless of animation frame rate.
     * A stack remains in its slot until its full capacity has been consumed.
     */
    fun settle(round: AntRound, state: AntGameState): AntGameState {
        require(state.pixels.size == round.pixels.size)
        val pixels = state.pixels.toMutableList()
        val active = state.active.toMutableList()
        while (true) {
            val exposed = AntsBlockGenerator.accessible(pixels, round.width, round.height)
            val targetSlot = active.indices.firstOrNull { slot ->
                val color = active[slot]?.color
                color != null && exposed.any { pixels[it] == color }
            } ?: break
            val stack = active[targetSlot]!!
            val targetPixel = exposed.first { pixels[it] == stack.color }
            pixels[targetPixel] = -1
            active[targetSlot] = if (stack.capacity == 1) null else stack.copy(capacity = stack.capacity - 1)
        }
        return state.copy(pixels = pixels, active = active)
    }

    /** Only the front item of one of the four refill columns can be chosen. */
    fun place(round: AntRound, state: AntGameState, column: Int): AntGameState? {
        if (column !in 0..3) return null
        val stable = settle(round, state)
        val slot = stable.active.indexOfFirst { it == null }
        if (slot < 0) return null
        val offset = stable.queueOffsets[column]
        val stack = round.queues[column].getOrNull(offset) ?: return null
        val active = stable.active.toMutableList()
        val offsets = stable.queueOffsets.toMutableList()
        active[slot] = stack
        offsets[column]++
        return settle(round, stable.copy(active = active, queueOffsets = offsets))
    }

    fun isWon(round: AntRound, state: AntGameState): Boolean =
        state.pixels.all { it < 0 } && state.active.all { it == null } &&
            round.queues.indices.all { state.queueOffsets[it] == round.queues[it].size }

    /**
     * Exact reachability over player selections from a stable state.
     * The budget only limits time/memory; exhaustion is UNKNOWN rather than falsely BLOCKED.
     */
    fun analyze(round: AntRound, current: AntGameState, maxStates: Int = 50_000): AntAnalysis {
        require(maxStates >= 1)
        val start = settle(round, current)
        val seen = HashSet<AntGameState>()
        val pending = ArrayDeque<AntGameState>()
        pending.add(start)
        while (pending.isNotEmpty()) {
            if (seen.size >= maxStates) return AntAnalysis(AntOutcome.UNKNOWN, seen.size)
            val state = pending.removeLast()
            if (!seen.add(state)) continue
            if (isWon(round, state)) return AntAnalysis(AntOutcome.WON, seen.size)
            for (column in 0..3) {
                val next = place(round, state, column) ?: continue
                if (next !in seen) pending.add(next)
            }
        }
        return AntAnalysis(AntOutcome.BLOCKED, seen.size)
    }

    /** No immediately legal player input and no work pending implies a definite block. */
    fun isImmediateBlock(round: AntRound, state: AntGameState): Boolean {
        val stable = settle(round, state)
        return !isWon(round, stable) && (0..3).none { place(round, stable, it) != null }
    }
}
