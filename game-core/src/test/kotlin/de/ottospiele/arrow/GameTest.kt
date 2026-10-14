package de.ottospiele.arrow
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GameTest {
 @Test fun validGeneratedLevels() {
  for(stage in 1..6) for(seed in 0L..11L){
   val level=Generator().generate(stage,seed)
   assertNotNull(Rules.solve(level))
  }
 }
 @Test fun retryRestoresLivesAndBoard() {
  val level=Generator().generate(1,42)
  val game=Game(level,3,2)
  val next=Rules.solve(level)!!.first()
  assertTrue(game.tap(next));game.retry()
  assertEquals(level.arrows.size,game.remaining.size)
  assertEquals(3,game.lives);assertEquals(2,game.hints)
 }
 @Test fun simplerLevelResetsProgress(){
  assertEquals(Progress(2,0),Progress(3,2).easier())
 }
 @Test fun bendFollowsOwnShapeNotRigidTranslation(){
  val snake=Arrow(1,2,7,4,Dir.UP,listOf(Pt(6,7),Pt(6,4)))
  val underSide=Arrow(2,3,5,3,Dir.RIGHT)
  // A rigid upward translation collides. The snake follows its own
  // horizontal turn, and its head exits at x=6 without collision.
  assertEquals(emptySet<Int>(),Rules.blockers(snake,listOf(snake,underSide),12))
  assertTrue(Rules.free(snake,listOf(snake,underSide),12))
  assertEquals((3 downTo 0).map{Pt(6,it)},Rules.exitPath(snake,12))
 }
 @Test fun tipRayHasRealBlockerAndCacheReactsToRemoval(){
  val snake=Arrow(1,2,7,4,Dir.UP,listOf(Pt(6,7),Pt(6,4)))
  val blocking=Arrow(2,5,2,3,Dir.RIGHT)
  val level=Level(1,2,12,listOf(snake,blocking))
  assertEquals(setOf(2),level.dependencies.requiredRemovals[1])
  val game=Game(level,3,2)
  assertFalse(game.tap(1));assertEquals(2,game.lives)
  assertFalse(game.tap(1));assertEquals(2,game.lives)
  assertTrue(game.tap(2))
  assertTrue(game.tap(1))
 }
 @Test fun levelDependenciesAreImmutableDuringPlay(){
  val snake=Arrow(1,2,7,4,Dir.UP,listOf(Pt(6,7),Pt(6,4)))
  val blocking=Arrow(2,5,2,3,Dir.RIGHT)
  val level=Level(1,2,12,listOf(snake,blocking))
  assertEquals(setOf(2),level.dependencies.remainingBlockers(1,setOf(1,2)))
  assertEquals(emptySet<Int>(),level.dependencies.remainingBlockers(1,setOf(1)))
 }
 @Test fun spiralStylePathOnlyNeedsHeadExitRay(){
  val spiral=Arrow(1,1,1,3,Dir.RIGHT,listOf(Pt(1,6),Pt(7,6),Pt(7,2),Pt(4,2),Pt(4,4),Pt(6,4)))
  val obstacle=Arrow(2,2,4,2,Dir.DOWN)
  assertTrue(Rules.exitPath(spiral,12).all{it.y==4 && it.x>6})
  assertTrue(Rules.blockers(spiral,listOf(spiral,obstacle),12).isEmpty())
 }
}