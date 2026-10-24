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

    @Test fun insufficientSearchBudgetNeverCountsAsCertainBlockage() {
        val round = AntsBlockGenerator.generate(17L, size = 8, colors = 3)
        val result = AntsBlockBalance.choices(round, AntsBlockGame.initial(round), 1)
        assertEquals(result.available, result.provenSafe + result.provenBlocked + result.unknown)
        assertEquals(0, result.provenBlocked)
        assertTrue(result.unknown > 0)
    }
}
