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
}
