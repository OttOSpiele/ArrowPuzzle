// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.arrow

/** Preferred difficulty bands are soft quality targets, not solvability constraints. */
data class DifficultyProfile(
 val stage:Int,
 val freeRange:ClosedFloatingPointRange<Double>,
 val chainRange:ClosedFloatingPointRange<Double>,
 val windingRange:ClosedFloatingPointRange<Double>
) {
 fun assess(level:Level):DifficultyAssessment {
  val q=LevelQualityAnalyzer.analyze(level)
  val count=level.arrows.size.coerceAtLeast(1)
  val free=q.initiallyFree.toDouble()/count
  val chain=q.longestChain.toDouble()/count
  val bends=level.arrows.count { it.vertices().size>=5 }
  val winding=bends.toDouble()/count
  fun delta(value:Double,range:ClosedFloatingPointRange<Double>):Double =
   if(value<range.start)range.start-value else if(value>range.endInclusive)value-range.endInclusive else 0.0
  val cost=4*delta(free,freeRange)+2*delta(chain,chainRange)+delta(winding,windingRange)
  return DifficultyAssessment(q.initiallyFree,q.longestChain,bends,
   free in freeRange,chain in chainRange,winding in windingRange,
   (100.0-100.0*cost).coerceIn(0.0,100.0))
 }
}
data class DifficultyAssessment(
 val free:Int,val chain:Int,val windings:Int,
 val freeFit:Boolean,val chainFit:Boolean,val windingFit:Boolean,val score:Double
) {
 val withinAllRanges get()=freeFit && chainFit && windingFit
}
object DifficultyProfiles {
 fun forStage(stage:Int):DifficultyProfile {
  require(stage>=1)
  val t=((stage-1).toDouble()/79).coerceIn(0.0,1.0)
  fun range(mid:Double,margin:Double):ClosedFloatingPointRange<Double> =
   (mid-margin).coerceIn(0.0,1.0)..(mid+margin).coerceIn(0.0,1.0)
  return DifficultyProfile(stage,range(.43-.27*t,.12),
   range(.20+.35*t,.18),range(.02+.13*t,.12))
 }
 fun choose(stage:Int,levels:List<Level>):Level? {
  val profile=forStage(stage)
  return levels.filter { Rules.solve(it)!=null }
   .maxWithOrNull(compareBy<Level> { profile.assess(it).score }.thenByDescending { it.seed })
 }
}
