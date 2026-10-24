// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.ants

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AntsBlockVariableStacksTest {
    @Test fun identicalColorStacksMayWorkTogether() {
        // Red covers both outer ends while blue protects the central red cell.
        val round = AntRound(3,3,
            listOf(1,1,1, 0,0,0, 1,1,1),
            listOf(listOf(AntStack(1,3)),listOf(AntStack(1,3)),
                listOf(AntStack(0,3)),emptyList()),listOf(0,1,2))
        var state = AntsBlockGame.initial(round)
        state = requireNotNull(AntsBlockGame.place(round,state,0))
        state = requireNotNull(AntsBlockGame.place(round,state,1))
        assertTrue(AntsBlockGame.isWon(round,
            requireNotNull(AntsBlockGame.place(round,state,2))))
    }

    @Test fun aStackCanPauseAndResumeAfterAnotherColorClears() {
        val round = AntRound(3,3,
            listOf(0,0,0, 0,1,0, 0,0,0),
            listOf(listOf(AntStack(1,1)),listOf(AntStack(0,8)),
                emptyList(),emptyList()),listOf(0,1))
        var state = requireNotNull(AntsBlockGame.place(round, AntsBlockGame.initial(round),0))
        assertEquals(1,state.active.count { it != null })
        state = requireNotNull(AntsBlockGame.place(round,state,1))
        assertTrue(AntsBlockGame.isWon(round,state))
        assertTrue(AntsBlockSolver.verifyCertificate(round))
    }

    @Test fun generatedStacksAreWithinCapacityAndMatchEveryColor() {
        var varied = false
        for (size in listOf(8,12,16,20)) for (seed in 0L..25L) {
            val round = AntsBlockGenerator.generate(seed,size,if(size <= 12) 3 else 5)
            val stacks = round.queues.flatten()
            assertTrue(stacks.all { it.capacity in 1..20 })
            assertEquals(round.width * round.height,stacks.sumOf { it.capacity })
            assertTrue(AntsBlockSolver.verifyCertificate(round))
            if (stacks.map { it.capacity }.distinct().size > 2) varied = true
        }
        assertTrue(varied)
    }
}
