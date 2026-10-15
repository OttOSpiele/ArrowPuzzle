// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.arrow
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GameTest {
 @Test fun dependencyPlannerProducesCompleteAcyclicGraph() {
  for(stage in listOf(1,6,9,17,25))for(seed in listOf(0L,1L,42L,2026L)){
   val count=BoardProfile.count(stage)
   val plan=DependencyPlanner.plan(stage,seed,count)
   assertEquals((0 until count).toList(),plan.order)
   assertEquals(count-1,plan.edges.size)
   assertTrue(plan.edges.all { (from,to)->to<from })
   assertEquals(plan,DependencyPlanner.plan(stage,seed,count))
  }
 }
 @Test fun geometricBacktrackingEmbedsSmallPlannedGraph() {
  for(seed in 0L..3L) {
   val plan=DependencyPlanner.plan(9,seed,2)
   val placed=GeometricBacktracker.place(plan,9,seed,12,12,maxNodes=1000)
   assertNotNull(placed,"No small graph embedding for seed $seed")
   val level=Level(seed,9,12,placed!!.arrows)
   assertNotNull(Rules.solve(level))
   assertTrue(plan.edges.all { (from,to)->
    from in level.dependencies.requiredRemovals[to].orEmpty()
   })
   assertEquals(placed.arrows,GeometricBacktracker.place(plan,9,seed,12,12,maxNodes=1000)?.arrows)
  }
 }
 @Test fun plannedGeneratorPreservesPortraitAndLandscapeProgression() {
  for(stage in listOf(6,9,12,16,17,25))for(aspect in listOf(0.7f,1f,1.5f)){
   for(seed in listOf(0L,17L)){
    val level=Generator().generate(stage,seed,aspect)
    assertEquals(BoardProfile.count(stage),level.arrows.size)
    assertNotNull(Rules.solve(level))
    assertEquals(level.arrows,Generator().generate(stage,seed,aspect).arrows)
    assertTrue(level.arrows.all { a->a.cells().all { p->
     p.x in 0 until level.width && p.y in 0 until level.height
    } })
   }
  }
 }
 @Test fun highStageOccupancyTargetsAreGradualAndBounded() {
  assertTrue(BoardProfile.targetOccupancy(1)<BoardProfile.targetOccupancy(25))
  assertTrue(BoardProfile.targetOccupancy(25)<BoardProfile.targetOccupancy(80))
  assertEquals(1f,BoardProfile.targetOccupancy(300))
  assertEquals(40,BoardProfile.count(44))
  assertEquals(40,BoardProfile.count(100))
  assertEquals(40,BoardProfile.count(300))
 }
 @Test fun denseStagesStayReproducibleSolvableAndNonOverlapping(){
  for(stage in listOf(24,35,45)) for(seed in listOf(0L,7L)) {
   val level=Generator().generate(stage,seed,1.35f)
   assertEquals(BoardProfile.count(stage),level.arrows.size)
   assertNotNull(Rules.solve(level))
   assertEquals(level.arrows,Generator().generate(stage,seed,1.35f).arrows)
   assertTrue(BoardProfile.actualOccupancy(level) in 0f..1f)
   assertEquals(level.arrows.sumOf{it.cells().size},
    level.arrows.flatMap{it.cells()}.distinct().size)
  }
 }
 @Test fun touchPrefersRemovableArrowsInOverlappingRegions(){
  val blocked=Arrow(0,2,4,2,Dir.RIGHT)
  val free=Arrow(1,5,4,2,Dir.UP)
  val level=Level(7,6,12,listOf(blocked,free))
  assertEquals(setOf(1),level.dependencies.requiredRemovals[0])
  val both=setOf(0,1)
  // The blocked arrow is physically closer, but the free arrow wins.
  assertEquals(1,ArrowTouch.select(level,both,3.75f,4f))
  // A deliberate press right on the blocked arrow still works.
  assertEquals(0,ArrowTouch.select(level,both,2f,4f))
  // The free arrow benefits from an enlarged invisible region.
  assertEquals(1,ArrowTouch.select(level,both,6.5f,4f))
  assertNull(ArrowTouch.select(level,both,10f,10f))
  assertEquals(0,ArrowTouch.select(level,setOf(0),3f,4f))
 }
 @Test fun validGeneratedLevels() {
  for(stage in 1..6) for(seed in 0L..11L){
   val level=Generator().generate(stage,seed)
   assertNotNull(Rules.solve(level))
  }
 }
 @Test fun laterBoardsUseBothVerticalHalves(){
  for(stage in listOf(9,14,17))for(seed in 0L..3L){
   val level=Generator().generate(stage,seed)
   val points=level.arrows.flatMap{it.cells()}
   assertTrue(points.any{it.y<level.size/2},"no upper-half occupancy")
   assertTrue(points.any{it.y>=level.size/2},"no lower-half occupancy")
   assertNotNull(Rules.solve(level))
  }
 }
 @Test fun compactLaterStagesGetMoreArrowsWithoutGrowingGrid(){
  assertEquals(18,BoardProfile.size(21))
  assertEquals(19,BoardProfile.size(24))
  assertEquals(20,BoardProfile.count(18))
  assertEquals(20,BoardProfile.count(24))
 }
 @Test fun compactBoardsAfterStageFive(){
  assertEquals(17,BoardProfile.size(1))
  for(stage in 2..21)assertEquals(18,BoardProfile.size(stage))
  assertEquals(18,BoardProfile.size(21))
  assertEquals(19,BoardProfile.size(22))
  assertEquals(19,BoardProfile.size(24))
  assertEquals(20,BoardProfile.size(25))
  assertEquals(22,BoardProfile.size(35))
  assertEquals(34,BoardProfile.size(100))
  assertEquals(11,BoardProfile.height(14,1.8f))
  assertTrue(BoardProfile.count(12)>BoardProfile.count(6))
  assertEquals(19,BoardProfile.count(16))
  assertEquals(20,BoardProfile.count(18))
  assertTrue(BoardProfile.count(25)>BoardProfile.count(18))
 }
 @Test fun rectangularBoardPreservesSolutionAndBounds(){
  for(stage in listOf(6,9,14,17))for(seed in 0L..2L){
   val level=Generator().generate(stage,seed,1.5f)
   assertEquals(BoardProfile.height(stage,1.5f),level.height)
   assertTrue(level.height<level.width)
   assertTrue(level.arrows.all{a->a.cells().all{p->p.x in 0 until level.width && p.y in 0 until level.height}})
   assertNotNull(Rules.solve(level))
  }
 }
 @Test fun constructedBoardsRemainSolvableAndSeeded(){
  for(stage in listOf(12,18,24,25))for(seed in 0L..2L){
   val level=Generator().generate(stage,seed,1.4f)
   assertEquals(BoardProfile.count(stage),level.arrows.size)
   assertEquals(level.arrows,Generator().generate(stage,seed,1.4f).arrows)
   assertNotNull(Rules.solve(level))
   assertTrue(level.arrows.all{Rules.selfClear(it,level.width,level.height)})
   if(stage>=12)assertTrue(level.arrows.any{it.vertices().size>=4},
    "No multi-turn motif at stage $stage")
  }
 }
 @Test fun compactBoardGenerationStaysSolvable(){
  for(stage in listOf(6,9,17,25))for(seed in 0L..2L){
   val level=Generator().generate(stage,seed)
   assertEquals(BoardProfile.size(stage),level.size)
   assertEquals(BoardProfile.count(stage),level.arrows.size)
   assertNotNull(Rules.solve(level))
  }
 }
 @Test fun constructedGraphPatternsAreRealDependencies(){
  for(stage in listOf(6,8,9,12,16,17,25,46)){
   val blue=PatternConstruction.blueprint(stage,BoardProfile.size(stage))
   assertNotNull(blue)
   for(seed in 0L..3L){
    val level=Generator().generate(stage,seed)
    assertTrue(PatternConstruction.blueprint(stage,level.size,seed)!!.realized(level),"required edges absent at stage $stage, seed $seed")
    assertTrue(PatternGoals.reached(PatternGoals.preferred(stage),PatternAnalysis.analyze(level)))
    assertNotNull(Rules.solve(level))
   }
  }
 }
 @Test fun patternTransformationsVaryBySeedButKeepEdges(){
  for(stage in listOf(6,9,17)){
   val variants=(0L..24L).map{seed->
    val blueprint=PatternConstruction.blueprint(stage,BoardProfile.size(stage),seed)!!
    val level=Level(seed,stage,BoardProfile.size(stage),blueprint.arrows)
    assertTrue(blueprint.realized(level))
    assertNotNull(Rules.solve(level))
    blueprint.arrows
   }
   assertTrue(variants.distinct().size>=5,"motifs do not vary enough at stage $stage")
   assertEquals(PatternConstruction.blueprint(stage,BoardProfile.size(stage),42L),
    PatternConstruction.blueprint(stage,BoardProfile.size(stage),42L))
  }
 }
 @Test fun firstFiveStagesHaveNoForcedPatternBlueprint(){
  for(stage in 1..5)assertNull(PatternConstruction.blueprint(stage,BoardProfile.size(stage)))
 }
 @Test fun patternGoalsChangeWithStage(){
  assertEquals(PatternFamily.INTRO,PatternGoals.preferred(3))
  assertEquals(PatternFamily.KEY_ARROW,PatternGoals.preferred(4))
  assertEquals(PatternFamily.ALTERNATING_CHAIN,PatternGoals.preferred(9))
  assertEquals(PatternFamily.BRANCH,PatternGoals.preferred(17))
 }
 @Test fun seededGenerationRemainsDeterministicAndSolvable(){
  for(stage in listOf(2,5,12,19))for(seed in 0L..3L){
   val first=Generator().generate(stage,seed)
   val second=Generator().generate(stage,seed)
   assertEquals(first.arrows,second.arrows)
   assertNotNull(Rules.solve(first))
   val metrics=PatternAnalysis.analyze(first)
   assertTrue(metrics.initiallyFree>=1)
  }
 }
 @Test fun patternGoalEvaluatorUsesObservedDependencies(){
  val key=PatternMetrics(1,2,1,0,0)
  assertTrue(PatternGoals.reached(PatternFamily.KEY_ARROW,key))
  assertFalse(PatternGoals.reached(PatternFamily.BRANCH,key))
  assertTrue(PatternGoals.reached(PatternFamily.ALTERNATING_CHAIN,
   PatternMetrics(1,3,0,2,0)))
 }
 @Test fun savedGameRestoresState(){
  val l=Generator().generate(1,42)
  val original=Game(l,5,3)
  assertTrue(original.tap(Rules.solve(l)!!.first()))
  val restored=Game(l,5,3)
  restored.restore(original.remaining.map{it.id}.toSet(),original.lives,original.hints,original.mistakes)
  assertEquals(original.remaining,restored.remaining)
  assertEquals(original.lives,restored.lives)
  assertEquals(original.hints,restored.hints)
 }
 @Test fun ageAppropriateStageProgression(){
  for(stage in 1..8)assertEquals(1,Progress(stage).required())
  for(stage in 9..16)assertEquals(2,Progress(stage).required())
  assertEquals(3,Progress(17).required())
  assertEquals(3,Progress(24).required())
  assertEquals(4,Progress(25).required())
  assertEquals(10,Progress(200).required())
  assertEquals(Progress(2,0),Progress(1).win())
  assertEquals(Progress(9,1),Progress(9).win())
  assertEquals(Progress(10,0),Progress(9,1).win())
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
 @Test fun spiralExitCannotCrossItsOwnBody(){
  val crossing=Arrow(7,2,2,3,Dir.RIGHT,
   listOf(Pt(2,7),Pt(8,7),Pt(8,4),Pt(10,4)))
  // Tip ray here is clear; a reversed spiral may have a ray through its body.
  assertTrue(Rules.selfClear(crossing,18,12))
  val selfCrossing=Arrow(8,2,4,3,Dir.LEFT,
   listOf(Pt(2,8),Pt(9,8),Pt(9,4),Pt(4,4)))
  assertFalse(Rules.selfClear(selfCrossing,18,12))
 }
 @Test fun multiTurnArrowsFollowTheirOwnBodies(){
  val zigzag=Arrow(0,2,2,3,Dir.RIGHT,listOf(Pt(5,2),Pt(5,5),Pt(7,5)))
  val spiral=Arrow(1,10,2,3,Dir.RIGHT,
   listOf(Pt(10,7),Pt(6,7),Pt(6,4),Pt(8,4)))
  for(arrow in listOf(zigzag,spiral)){
   val level=Level(1,24,18,listOf(arrow),12)
   assertEquals(arrow.body().size,arrow.cells().size)
   assertNotNull(Rules.solve(level))
   val plan=Rules.plan(arrow,level,setOf(arrow.id))
   assertTrue(plan.canExit)
   assertEquals(arrow.body().drop(1)+plan.travel.take(1),plan.frame(1))
  }
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
 @Test fun movingBodyRoundsCornerWithoutRigidSweep(){
  val a=Arrow(1,2,5,3,Dir.UP,listOf(Pt(5,5),Pt(5,3)))
  val level=Level(1,2,10,listOf(a))
  val plan=Rules.plan(a,level,setOf(1))
  assertTrue(plan.canExit)
  assertEquals(a.body().drop(1)+listOf(Pt(5,2)),plan.frame(1))
  assertEquals(a.body().size,plan.frame(1).size)
 }
 @Test fun redAttemptStopsBeforeFirstBlocker(){
  val a=Arrow(1,2,5,3,Dir.UP,listOf(Pt(5,5),Pt(5,3)))
  val b=Arrow(2,4,1,3,Dir.RIGHT)
  val level=Level(1,2,10,listOf(a,b))
  val plan=Rules.plan(a,level,setOf(1,2))
  assertFalse(plan.canExit)
  assertEquals(1,plan.allowedSteps)
  assertEquals(2,plan.blockedBy)
 }
 @Test fun spiralStylePathOnlyNeedsHeadExitRay(){
  val spiral=Arrow(1,1,1,3,Dir.RIGHT,listOf(Pt(1,6),Pt(7,6),Pt(7,2),Pt(4,2),Pt(4,4),Pt(6,4)))
  val obstacle=Arrow(2,2,4,2,Dir.DOWN)
  assertTrue(Rules.exitPath(spiral,12).all{it.y==4 && it.x>6})
  assertTrue(Rules.blockers(spiral,listOf(spiral,obstacle),12).isEmpty())
 }
}