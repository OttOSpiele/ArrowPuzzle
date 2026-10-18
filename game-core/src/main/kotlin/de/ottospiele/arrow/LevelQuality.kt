// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.arrow

/** Deterministic, solver-independent measurements of a generated board. */
data class LevelQuality(
 val occupancy:Double,
 val initiallyFree:Int,
 val connectedArrows:Int,
 val longestChain:Int,
 val largestInteriorVoid:Int,
 val keyArrows:Int
) {
 /** Heuristic only. Never replaces a full solvability check. */
 fun score(stage:Int):Double {
  val densityGoal=BoardProfile.targetOccupancy(stage).toDouble()
  val targetFree=if(stage<9) .45 else if(stage<35) .30 else .15
  val freeRatio=initiallyFree.toDouble()/(connectedArrows+initiallyFree).coerceAtLeast(1)
  return 80*occupancy-30*kotlin.math.abs(occupancy-densityGoal)+
   12*longestChain.coerceAtMost(10)+10*keyArrows-
   28*kotlin.math.abs(freeRatio-targetFree)-largestInteriorVoid*.4
 }
}

object LevelQualityAnalyzer {
 fun analyze(level:Level):LevelQuality {
  val deps=level.dependencies.requiredRemovals
  val related=deps.keys.filter { id ->
   deps[id].orEmpty().isNotEmpty() || deps.values.any { id in it }
  }.size
  val metrics=PatternAnalysis.analyze(level)
  val occupied=level.arrows.flatMapTo(mutableSetOf()){it.cells()}
  val visited=mutableSetOf<Pt>()
  var largest=0
  // Find orthogonally connected empty regions not touching the boundary.
  for(y in 0 until level.height)for(x in 0 until level.width) {
   val p=Pt(x,y)
   if(p in occupied || !visited.add(p))continue
   val queue=ArrayDeque<Pt>()
   queue.add(p)
   var count=0
   var boundary=false
   while(queue.isNotEmpty()) {
    val current=queue.removeFirst()
    count++
    if(current.x==0 || current.y==0 ||
       current.x==level.width-1 || current.y==level.height-1)boundary=true
    for(d in Dir.entries) {
     val next=Pt(current.x+d.x,current.y+d.y)
     if(next.x in 0 until level.width && next.y in 0 until level.height &&
        next !in occupied && visited.add(next))queue.addLast(next)
    }
   }
   if(!boundary)largest=maxOf(largest,count)
  }
  return LevelQuality(BoardProfile.actualOccupancy(level).toDouble(),
   metrics.initiallyFree,related,metrics.longestChain,largest,metrics.keyArrows)
 }
}
