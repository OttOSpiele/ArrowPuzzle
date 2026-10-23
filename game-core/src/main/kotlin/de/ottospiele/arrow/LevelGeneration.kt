// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.arrow

/**
 * Product routing for stage generation. Tutorial stages preserve the
 * established legacy generator; later stages prefer bounded Exact Cover.
 * Never expose an incomplete or unsolvable board.
 */
object LevelGeneration {
 fun generate(stage:Int,seed:Long,aspect:Float=1f):Level {
  require(stage>=1)
  if(stage<=16)return Generator().generate(stage,seed,aspect)
  val width=BoardProfile.size(stage).coerceAtMost(20)
  val height=BoardProfile.height(stage,aspect).coerceAtMost(20)
  val target=(width*height/4).coerceAtLeast(2)
  val preferred=runCatching {
   ExactCoverGenerator.generate(width,height,seed,stage,
    maxNodes=3000,targetArrows=target,minWindingArrows=2,
    maxInitiallyFree=target/2,minChainLength=width/2).level
  }.getOrNull()
  if(preferred!=null && Rules.solve(preferred)!=null)return preferred
  // Explicitly mark the alternate generation path for the UI.
  return Generator().generate(stage,seed,aspect).copy(fallbackGeneration=true)
 }
}
