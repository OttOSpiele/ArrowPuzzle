// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.ants

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AntsBlockQueuePlannerTest {
    @Test fun parkedInteriorStackUnblocksWhenOuterRingIsCleared() {
        val pixels = listOf(0,0,0,0,1,0,0,0,0)
        val round = AntRound(3,3,pixels,listOf(
            listOf(AntStack(1,1)),
            listOf(AntStack(0,8)),
            emptyList(), emptyList()), listOf(1,0))
        var state = AntsBlockGame.initial(round)
        state = requireNotNull(AntsBlockGame.place(round,state,0))
        assertEquals(1,state.active.count { it != null })
        assertEquals(9,state.pixels.count { it >= 0 })
        state = requireNotNull(AntsBlockGame.place(round,state,1))
        assertTrue(AntsBlockGame.isWon(round,state))
    }

    @Test fun regeneratedQueuesKeepExecutableWitness() {
        for (size in listOf(8,12,16,20,24)) {
            for (seed in 0L until 32L) {
                val round = AntsBlockGenerator.generate(seed,size,if(size <= 12) 3 else 5)
                var state = AntsBlockGame.initial(round)
                for (column in round.solutionColumns) {
                    state = requireNotNull(AntsBlockGame.place(round,state,column))
                }
                assertTrue(AntsBlockGame.isWon(round,state),"size=$size seed=$seed")
            }
        }
    }
}
