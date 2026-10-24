// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.ants

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AntsBlockTimedTest {
    @Test fun placementsDoNotInstantlyConsumeBlocks() {
        val round = AntRound(2,2,listOf(0,0,1,1),
            listOf(listOf(AntStack(0,2)),listOf(AntStack(1,2)),emptyList(),emptyList()),
            listOf(0,1))
        val first = requireNotNull(AntsBlockTimed.place(round,AntsBlockGame.initial(round),0))
        val both = requireNotNull(AntsBlockTimed.place(round,first,1))
        assertEquals(4,both.pixels.count { it >= 0 })
        assertEquals(2,both.active.count { it != null })
        val tick = AntsBlockTimed.tick(round,both)
        assertEquals(2,tick.pixels.count { it >= 0 })
        assertEquals(2,tick.active.count { it != null })
        assertTrue(AntsBlockGame.isWon(round,AntsBlockTimed.tick(round,tick)))
    }

    @Test fun interiorStackWaitsWithoutLosingCapacity() {
        val round = AntRound(3,3,listOf(0,0,0,0,1,0,0,0,0),
            listOf(listOf(AntStack(1,1)),listOf(AntStack(0,8)),emptyList(),emptyList()),
            listOf(1,0))
        var state = requireNotNull(AntsBlockTimed.place(round,AntsBlockGame.initial(round),0))
        state = AntsBlockTimed.tick(round,state)
        assertEquals(1,state.active.filterNotNull().single().capacity)
        state = requireNotNull(AntsBlockTimed.place(round,state,1))
        repeat(9) { state = AntsBlockTimed.tick(round,state) }
        assertTrue(AntsBlockGame.isWon(round,state))
    }

    @Test fun fiveSlotsAreStrictlyEnforced() {
        val round = AntRound(1,1,listOf(0),
            listOf(List(6){ AntStack(1,1) },emptyList(),emptyList(),emptyList()),emptyList())
        var state = AntsBlockGame.initial(round)
        repeat(5) { state = requireNotNull(AntsBlockTimed.place(round,state,0)) }
        assertNull(AntsBlockTimed.place(round,state,0))
        assertEquals(5,state.active.count { it != null })
        assertTrue(AntsBlockTimed.immediateBlock(round,state))
    }
}
