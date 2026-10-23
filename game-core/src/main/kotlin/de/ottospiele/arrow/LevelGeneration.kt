// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.arrow

/** Configurable, gradually scaled level generation. */
data class GenerationPolicy(val legacyEnabled:Boolean=true)

data class StageGeometry(val width:Int,val height:Int,val targetArrows:Int,
 val windingArrows:Int,val maxInitiallyFree:Int,val minChainLength:Int)

object StageGenerationProfile {
 fun forStage(stage:Int,aspect:Float=1f):StageGeometry {
  require(stage>=1)
  val width=when {
   stage<=8->4+((stage-1)/2)
   stage<=16->7+((stage-9)/3)
   stage<=24->10+((stage-17)/3)
   stage<=40->13+((stage-25)/4)
   else->(17+(stage-41)/12).coerceAtMost(20)
  }.coerceAtMost(20)
  val height=(width/aspect.coerceIn(1f,1.65f)).toInt().coerceIn(4,width)
  val cells=width*height
  val target=(cells/4).coerceAtLeast(2)
  return StageGeometry(width,height,target,if(stage>=24)2 else 0,
   (target*(if(stage<17)0.70 else 0.5)).toInt().coerceAtLeast(1),
   if(stage<=8)1 else (width/3).coerceAtLeast(2))
 }
}

/** Validated tutorial puzzles avoid a runtime dependency on either generator. */
object TutorialLevels {
 fun level(stage:Int,seed:Long=stage.toLong()):Level {
  require(stage in 1..8)
  val width=7
  val height=10
  val count=stage+1
  val arrows=(0 until count).map { i->
   // Parallel, disjoint rows: the puzzle never requires a guessed blocker.
   Arrow(i,1,i,2,Dir.RIGHT)
  }
  val level=Level(seed,stage,width,arrows,height)
  require(Rules.solve(level)!=null)
  return level
 }
}

/** Legacy is optional for every stage; the tutorial is a built-in solvable pool. */
object LevelGeneration {
 internal fun requiredWindingArrows(stage:Int):Int=if(stage>=24)2 else 0
 fun generateExactOrNull(stage:Int,seed:Long,aspect:Float=1f):Level? {
  require(stage>=1)
  if(stage<=8)return TutorialLevels.level(stage,seed)
  val profile=StageGenerationProfile.forStage(stage,aspect)
  val level=runCatching {
   ExactCoverGenerator.generate(profile.width,profile.height,seed,stage,
    maxNodes=3000,targetArrows=profile.targetArrows,
    minWindingArrows=profile.windingArrows,
    maxInitiallyFree=profile.maxInitiallyFree,
    minChainLength=profile.minChainLength).level
  }.getOrNull()
  return level?.takeIf { Rules.solve(it)!=null }
 }
 fun generateFallback(stage:Int,seed:Long,aspect:Float=1f):Level {
  val level=Generator().generate(stage,seed,aspect)
  require(Rules.solve(level)!=null)
  return level.copy(fallbackGeneration=true)
 }
 fun generate(stage:Int,seed:Long,aspect:Float=1f,policy:GenerationPolicy=GenerationPolicy()):Level {
  require(stage>=1)
  if(stage<=8)return TutorialLevels.level(stage,seed)
  return generateExactOrNull(stage,seed,aspect)
   ?: if(policy.legacyEnabled)generateFallback(stage,seed,aspect)
   else error("No validated Exact Cover level for stage $stage")
 }
}
