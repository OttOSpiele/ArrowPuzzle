// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.arrow

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** Regression guard for the stage progression and production generator router. */
class IntegrationRegressionTest {
 @Test fun stageBoundariesAndRequiredPuzzles() {
  val expected=mapOf(1 to 1,8 to 1,9 to 2,16 to 2,
   17 to 3,29 to 3,30 to 4,42 to 4,43 to 5,54 to 5,
   55 to 6,67 to 6,68 to 7,79 to 7,80 to 8,120 to 8)
  expected.forEach { (stage,required)->
   assertEquals(required,Progress(stage).required(),"stage $stage")
  }
 }

 @Test fun winsOnlyAdvanceAfterAllPuzzles() {
  for(stage in listOf(1,8,9,16,17,29,30,42,43,54,55,67,68,79,80)){
   val required=Progress(stage).required()
   for(done in 0 until required-1){
    assertEquals(Progress(stage,done+1),Progress(stage,done).win())
   }
   assertEquals(Progress(stage+1,0),Progress(stage,required-1).win())
  }
 }

 @Test fun rollbackResetsCurrentStageProgress() {
  assertEquals(Progress(16,0),Progress(17,2).easier())
  assertEquals(Progress(79,0),Progress(80,7).easier())
  assertEquals(Progress(1,0),Progress(1,0).easier())
  assertEquals(Progress(16,1),Progress(17,2).easier().win())
 }

 @Test fun earlyStagesUseUnmodifiedLegacyGenerator() {
  for(stage in listOf(1,8,9,16)){
   val seed=104729L+stage
   val expected=Generator().generate(stage,seed,1f)
   val routed=LevelGeneration.generate(stage,seed,1f)
   assertEquals(expected,routed,"Legacy routing stage $stage")
   assertFalse(routed.fallbackGeneration)
   assertNotNull(Rules.solve(routed))
  }
 }
}
