// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.arrow

/**
 * Product routing for stage generation. Tutorial stages preserve the
 * established legacy generator; later stages prefer bounded Exact Cover.
 * Never expose an incomplete or unsolvable board.
 */
object LevelGeneration {
 internal fun requiredWindingArrows(stage:Int):Int=if(stage>=24)2 else 0
 fun generateExactOrNull(stage:Int,seed:Long,aspect:Float=1f):Level? {
  require(stage>=17)
  val width=BoardProfile.size(stage).coerceAtMost(20)
  val height=BoardProfile.height(stage,aspect).coerceAtMost(20)
  val target=(width*height/4).coerceAtLeast(2)
  val level=runCatching {
   ExactCoverGenerator.generate(width,height,seed,stage,
    maxNodes=3000,targetArrows=target,minWindingArrows=requiredWindingArrows(stage),
    maxInitiallyFree=target/2,minChainLength=width/2).level
  }.getOrNull()
  return level?.takeIf { Rules.solve(it)!=null }
 }
 fun generateFallback(stage:Int,seed:Long,aspect:Float=1f):Level {
  val level=Generator().generate(stage,seed,aspect)
  require(Rules.solve(level)!=null)
  return level.copy(fallbackGeneration=stage>=17)
 }
 fun generate(stage:Int,seed:Long,aspect:Float=1f):Level {
  require(stage>=1)
  if(stage<=16)return Generator().generate(stage,seed,aspect)
  return generateExactOrNull(stage,seed,aspect) ?: generateFallback(stage,seed,aspect)
 }
}
