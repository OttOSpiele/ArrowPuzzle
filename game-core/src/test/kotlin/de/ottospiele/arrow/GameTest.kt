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
 @Test fun bendGeometryRemainsSolvable() {
  for(stage in 2..5)for(seed in 10L..20L){
   val level=Generator().generate(stage,seed)
   assertNotNull(Rules.solve(level))
   assertTrue(level.arrows.any{it.bends.isNotEmpty()})
   assertTrue(PatternAnalysis.analyze(level).longestChain>=1)
  }
 }
}
