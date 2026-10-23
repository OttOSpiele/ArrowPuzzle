// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.app

import org.junit.Assert.*
import org.junit.Test

class PuzzleItModelTest {
 @Test fun deterministicGenerator(){
  for(seed in 0L..200L){
   val a=PuzzleItGenerator.generate(seed)
   val b=PuzzleItGenerator.generate(seed)
   assertEquals(a,b)
   assertEquals(9,a.pieces.size)
   assertEquals(12,a.edges.size)
   assertTrue(a.edges.all{it==-1||it==1})
   assertTrue(a.number in 1..10)
   assertEquals(4,a.answers.size)
   assertEquals(4,a.answers.toSet().size)
   assertEquals(1,a.answers.count{it==a.number})
   assertTrue(a.pieces.none{it.fixed})
   assertTrue(a.pieces.indices.any{a.pieces[it].left != (it%3).toFloat() || a.pieces[it].top != (it/3).toFloat()})
  }
 }
 @Test fun snapIsRestrictedToItsOwnPosition(){
  val p=PuzzleItPiece(4,0f,0f)
  assertFalse(PuzzleItGenerator.attemptSnap(p,1.16f,1f,1f,1f,.15f).fixed)
  assertTrue(PuzzleItGenerator.attemptSnap(p,1.1f,1.1f,1f,1f,.15f).fixed)
 }
 @Test fun wrongAnswersAreDisabledAndIgnored(){
  val c=PuzzleItController(PuzzleItConfig(),12)
  val all=c.round.pieces.map{it.copy(fixed=true)}
  c.replace(c.round.copy(pieces=all,phase=PuzzleItPhase.CHOICE_ACTIVE),false,0)
  val wrong=c.round.answers.first{it!=c.round.number}
  assertFalse(c.choose(wrong))
  val after=c.round
  assertTrue(wrong in after.disabled)
  assertFalse(c.choose(wrong))
  assertEquals(after,c.round)
  assertTrue(c.choose(c.round.number))
  assertFalse(c.choose(c.round.number))
  assertEquals(PuzzleItPhase.ROUND_SUCCESS,c.round.phase)
 }
 @Test fun deferredExitFinishesRoundOnlyOnce(){
  val c=PuzzleItController(PuzzleItConfig(),18)
  c.requestStopAfterCurrentRound()
  assertFalse(c.finishRound(true,1))
  c.replace(c.round.copy(phase=PuzzleItPhase.CHOICE_ACTIVE),true,0)
  assertTrue(c.choose(c.round.number))
  assertTrue(c.finishRound(false,4))
  assertEquals(PuzzleItPhase.EXITING,c.round.phase)
  assertEquals(1,c.completedRounds)
  assertFalse(c.finishRound(false,4))
  assertEquals(1,c.completedRounds)
 }
 @Test fun continuesWithoutStopAndAvoidsPreviousNumber(){
  val c=PuzzleItController(PuzzleItConfig(),84)
  val old=c.round.number
  c.replace(c.round.copy(phase=PuzzleItPhase.CHOICE_ACTIVE),false,0)
  assertTrue(c.choose(old))
  assertFalse(c.finishRound(false,5))
  assertEquals(1,c.completedRounds)
  assertEquals(PuzzleItPhase.PUZZLE_ACTIVE,c.round.phase)
  assertNotEquals(old,c.round.number)
 }
}
