// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.ants

/**
 * Tick-driven gameplay model. A placement only occupies a free slot; no blocks
 * are removed until a tick. Several stacks, including matching colors, may work
 * concurrently. Each worker group performs at most one removal per tick.
 * Tick order is deterministic and independent of frame rate.
 */
object AntsBlockTimed {
    fun place(round: AntRound, state: AntGameState, column: Int): AntGameState? {
        if (column !in 0..3) return null
        val free = state.active.indexOfFirst { it == null }
        if (free < 0) return null
        val offset = state.queueOffsets[column]
        val stack = round.queues[column].getOrNull(offset) ?: return null
        val slots = state.active.toMutableList()
        val offsets = state.queueOffsets.toMutableList()
        slots[free] = stack
        offsets[column]++
        return state.copy(active = slots, queueOffsets = offsets)
    }

    fun tick(round: AntRound, state: AntGameState): AntGameState {
        require(state.pixels.size == round.pixels.size)
        val pixels = state.pixels.toMutableList()
        val slots = state.active.toMutableList()
        // Recompute exposure for each removal, including newly exposed cells.
        for (slot in slots.indices) {
            val stack = slots[slot] ?: continue
            val target = AntsBlockGenerator.accessible(pixels, round.width, round.height)
                .firstOrNull { pixels[it] == stack.color } ?: continue
            pixels[target] = -1
            slots[slot] = if (stack.capacity == 1) null
                else stack.copy(capacity = stack.capacity - 1)
        }
        return state.copy(pixels = pixels, active = slots)
    }

    fun canWork(round: AntRound, state: AntGameState): Boolean {
        val exposed = AntsBlockGenerator.accessible(state.pixels, round.width, round.height)
        return state.active.any { stack -> stack != null && exposed.any { state.pixels[it] == stack.color } }
    }

    /** Only definitely stranded states are reported as blocked. */
    fun immediateBlock(round: AntRound, state: AntGameState): Boolean =
        !AntsBlockGame.isWon(round, state) &&
            !canWork(round, state) &&
            (0..3).none { place(round, state, it) != null }
}
