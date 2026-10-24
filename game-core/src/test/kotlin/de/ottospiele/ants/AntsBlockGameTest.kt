// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.ants

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AntsBlockGameTest {
    @Test fun certifiedSequenceWinsUnderFiveSlotRules() {
        for (seed in 0L until 40L) {
            val round = AntsBlockGenerator.generate(seed, size = 12, colors = 4)
            var state = AntsBlockGame.initial(round)
            for (column in round.solutionColumns) {
                state = requireNotNull(AntsBlockGame.place(round, state, column)) { "seed=$seed" }
            }
            assertTrue(AntsBlockGame.isWon(round, state), "seed=$seed")
        }
    }

    @Test fun onlyFourQueueFrontsAndFiveActiveSlots() {
        val round = AntRound(1, 1, listOf(0),
            listOf(listOf(AntStack(1, 1), AntStack(1, 1)), listOf(AntStack(2, 1)),
                listOf(AntStack(3, 1)), listOf(AntStack(4, 1), AntStack(5, 1))),
            emptyList())
        var state = AntsBlockGame.initial(round)
        for (column in listOf(0, 1, 2, 3, 0)) {
            state = requireNotNull(AntsBlockGame.place(round, state, column))
        }
        assertEquals(5, state.active.count { it != null })
        assertNull(AntsBlockGame.place(round, state, 3))
        assertEquals(listOf(2, 1, 1, 1), state.queueOffsets)
    }

    @Test fun anImpossibleColorIsDefinitelyBlocked() {
        val round = AntRound(1, 1, listOf(0), listOf(
            listOf(AntStack(1, 1)), emptyList(), emptyList(), emptyList()), emptyList())
        val state = requireNotNull(AntsBlockGame.place(round, AntsBlockGame.initial(round), 0))
        assertTrue(AntsBlockGame.isImmediateBlock(round, state))
        assertEquals(AntOutcome.BLOCKED, AntsBlockGame.analyze(round, state).outcome)
    }

    @Test fun noFalseDeadlockWhenQueuesCanProgress() {
        val round = AntsBlockGenerator.generate(78, size = 8, colors = 3)
        assertEquals(AntOutcome.WON, AntsBlockGame.analyze(round, AntsBlockGame.initial(round)).outcome)
    }

    @Test fun lowSearchBudgetIsUnknownNotBlocked() {
        val round = AntsBlockGenerator.generate(78, size = 8, colors = 3)
        assertEquals(AntOutcome.UNKNOWN, AntsBlockGame.analyze(round, AntsBlockGame.initial(round), 1).outcome)
    }

    @Test fun blockCollectionFollowsExteriorOnly() {
        val pixels = listOf(1, 1, 1, 1, 2, 1, 1, 1, 1)
        val round = AntRound(3, 3, pixels,
            listOf(listOf(AntStack(2, 1)), emptyList(), emptyList(), emptyList()), emptyList())
        val state = requireNotNull(AntsBlockGame.place(round, AntsBlockGame.initial(round), 0))
        assertEquals(2, state.pixels[4], "enclosed inner block may not be consumed")
        assertEquals(1, state.active.filterNotNull().single().capacity)
    }
}
