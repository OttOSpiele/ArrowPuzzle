// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.app

import de.ottospiele.arrow.Progress
import org.junit.Assert.assertEquals
import org.junit.Test

class MiniGamePreparationTest {
 @Test fun earlyStagesSpanMultipleStagesUntilMini() {
  assertEquals(listOf(11 to 2,12 to 2),goalsUntilNextMini(Progress(11,0),0,false))
 }
 @Test fun playingPuzzleIsNotReservedTwice() {
  assertEquals(listOf(11 to 1,12 to 2),goalsUntilNextMini(Progress(11,0),0,true))
 }
 @Test fun stageBoundaryAfterFourthPuzzleNeedsNoActiveReserve() {
  assertEquals(emptyList<Pair<Int,Int>>(),goalsUntilNextMini(Progress(30,3),3,true))
 }
 @Test fun longStageReservesRemainingOnly() {
  assertEquals(listOf(45 to 4),goalsUntilNextMini(Progress(45,0),0,true))
 }
 @Test fun progressWithinStageIsRespected() {
  assertEquals(listOf(11 to 1),goalsUntilNextMini(Progress(11,1),3,false))
 }
}
