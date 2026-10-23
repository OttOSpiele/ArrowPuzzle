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

 @Test fun requiredPlusTwoPreparationCapacityAcrossBoundaries() {
  val expected=mapOf(1 to 3,8 to 3,9 to 4,16 to 4,
   17 to 5,29 to 5,30 to 6,42 to 6,43 to 7,
   54 to 7,55 to 8,67 to 8,68 to 9,79 to 9,80 to 10)
  expected.forEach { (stage,capacity)->
   assertEquals(capacity,Progress(stage).required()+2,"prepared current-stage reserve at $stage")
  }
 }

 @Test fun windingConstraintStartsWhenMultiBendCandidatesExist() {
  for(stage in 17..27)assertEquals(0,LevelGeneration.requiredWindingArrows(stage),"stage $stage")
  for(stage in 28..39)assertEquals(1,LevelGeneration.requiredWindingArrows(stage),"stage $stage")
  for(stage in listOf(40,60,80))assertEquals(2,LevelGeneration.requiredWindingArrows(stage),"stage $stage")
 }

 @Test fun tutorialStagesAreDeterministicAndSolvable() {
  for(stage in 1..8) {
   val first=LevelGeneration.generate(stage,42L,1f,GenerationPolicy(legacyEnabled=false))
   val again=LevelGeneration.generate(stage,42L,1f,GenerationPolicy(legacyEnabled=false))
   assertEquals(first,again)
   assertNotNull(Rules.solve(first),"tutorial stage $stage")
   assertFalse(first.fallbackGeneration)
   assertEquals(stage+1,first.arrows.size)
  }
 }

 @Test fun exactCoverDisplayFootprintIsSparseAndCentered() {
  for(stage in listOf(9,16,17,24,25,40,80)) {
   val profile=StageGenerationProfile.forStage(stage,1f)
   assertTrue(profile.coveredWidth<profile.width)
   assertTrue(profile.coveredHeight<profile.height)
   assertEquals(profile.coveredWidth*profile.coveredHeight/4,profile.targetArrows)
  }
 }
 @Test fun stageGeometryGrowsGradually() {
  val sizes=(9..80).map{StageGenerationProfile.forStage(it).width}
  assertTrue(sizes.zipWithNext().all{(a,b)->b>=a && b-a<=1})
  assertTrue(sizes.first()<sizes.last())
 }
}
