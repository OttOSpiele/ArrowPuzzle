package de.ottospiele.arrow
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GameTest {
 @Test fun validGeneratedLevels() {
  for(stage in 1..5) for(seed in 0L..7L){
   assertNotNull(Rules.solve(Generator().generate(stage,seed)))
  }
 }
 @Test fun retryRestoresLivesAndBoard() {
  val level=Generator().generate(1,42)
  val game=Game(level,3,2)
  val next=Rules.solve(level)!!.first()
  assertTrue(game.tap(next))
  game.retry()
  assertEquals(level.arrows.size,game.remaining.size)
  assertEquals(3,game.lives)
  assertEquals(2,game.hints)
 }
 @Test fun simplerLevelResetsProgress() {
  assertEquals(Progress(2,0),Progress(3,2).easier())
 }
 @Test fun bentArrowCollisionUsesWholeMovingShape() {
  val moving=Arrow(1,1,1,3,Dir.RIGHT,listOf(Pt(1,3),Pt(3,3)))
  val blocker=Arrow(2,4,1,3,Dir.DOWN)
  val level=Level(1,2,9,listOf(moving,blocker))
  assertTrue(2 in Rules.blockers(moving,level.arrows,level.size))
 }
 @Test fun curvedArrowTipIsFreeButTrailingSegmentIsBlocked() {
  // Up-pointing tip at x=6 is clear. Horizontal tail at y=5 sweeps over
  // the end of the left-pointing arrow at x=3 on its first upward move.
  val bent=Arrow(1,3,5,4,Dir.UP,listOf(Pt(6,5),Pt(6,2)))
  val crossing=Arrow(2,3,4,3,Dir.LEFT)
  val level=Level(15,3,10,listOf(bent,crossing))
  assertEquals(setOf(2),Rules.blockers(bent,level.arrows,level.size))
  assertFalse(Rules.free(bent,level.arrows,level.size))
  val game=Game(level,5,3)
  assertEquals(null,game.hint()?.takeIf{it==1})
  assertFalse(game.tap(1))
  assertEquals(4,game.lives)
  assertTrue(game.tap(2))
  assertTrue(game.tap(1))
 }
 @Test fun exitFramesIncludeLastTrailingCell() {
  val arrow=Arrow(1,3,6,3,Dir.UP,listOf(Pt(5,6),Pt(5,4)))
  val frames=Rules.exitFrames(arrow,8)
  assertTrue(frames.last().none{it.y in 0 until 8})
  assertEquals(7,frames.size)
 }
 @Test fun bendGeometryRemainsSolvable() {
  for(stage in 2..5)for(seed in 10L..20L){
   val level=Generator().generate(stage,seed)
   assertNotNull(Rules.solve(level))
   assertTrue(level.arrows.any{it.bends.isNotEmpty()})
   assertTrue(PatternAnalysis.analyze(level).longestChain>=1)
  }
 }
}
