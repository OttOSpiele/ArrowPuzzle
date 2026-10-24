// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.ants

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AntsBlockBalanceTest {
    @Test fun safeOpeningAndReproducibleSimulation() {
        for (seed in 0L..5L) {
            val round = AntsBlockGenerator.generate(seed, size = 8, colors = 3)
            val state = AntsBlockGame.initial(round)
            val choice = AntsBlockBalance.choices(round, state, 5000)
            assertTrue(choice.available in 1..4)
            assertEquals(choice.available, choice.provenSafe + choice.provenBlocked + choice.unknown)
            assertTrue(choice.provenSafe >= 1, "constructive first move must be safe, seed=$seed")
            assertEquals(AntsBlockBalance.simulate(round, seed), AntsBlockBalance.simulate(round, seed))
        }
    }

    @Test fun blockedChoicesAreDistinguishedFromUnknown() {
        val round = AntRound(1, 1, listOf(0),
            listOf(listOf(AntStack(1, 1)), listOf(AntStack(0, 1)), emptyList(), emptyList()),
            listOf(1, 0))
        val choices = AntsBlockBalance.choices(round, AntsBlockGame.initial(round), 100)
        assertEquals(2, choices.available)
        // Placing a wrong stack first is not necessarily fatal while another slot remains.
        assertEquals(1, choices.provenSafe)
        assertEquals(1, choices.provenBlocked)
    }
}
