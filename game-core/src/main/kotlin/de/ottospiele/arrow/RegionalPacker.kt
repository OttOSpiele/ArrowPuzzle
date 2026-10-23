// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.arrow

import kotlin.math.abs
import kotlin.random.Random

/**
 * Bounded regional repair of a completed, solvable reverse-constructed board.
 * A suffix of blockers is removed, then placed afresh with DFS. Earlier arrows
 * remain fixed. Every new arrow must exit through the fixed+already placed
 * arrows; therefore every successful result retains an acyclic dependency DAG.
 */
object RegionalPacker {
 data class Result(val level:Level,val visited:Int,val improved:Boolean)
 fun improve(level:Level,seed:Long,maxNodes:Int=160):Result {
  if(level.stage<35 || level.arrows.size<9)return Result(level,0,false)
  val width=level.width;val height=level.height
  val suffix=4
  val prefix=level.arrows.dropLast(suffix)
  val taken=prefix.flatMapTo(mutableSetOf()){it.cells()}
  val random=Random(seed xor 0x1A2B3C4DL)
  val candidates=mutableListOf<Arrow>()
  // Enumerate local replacements and corridor-following L shapes.
  for(dir in Dir.entries)for(y in 0 until height)for(x in 0 until width){
   for(length in 2..5){
    candidates.add(Arrow(-1,x,y,length,dir))
    val sides=Dir.entries.filter{it.x*dir.x+it.y*dir.y==0}
    for(side in sides) {
     val bend=Pt(x+side.x*2,y+side.y*2)
     candidates.add(Arrow(-1,x,y,length,dir,listOf(bend,
      Pt(bend.x+dir.x*(length-1),bend.y+dir.y*(length-1)))))
    }
   }
  }
  val feasible=candidates.filter { a ->
   val cells=a.cells()
   cells.size==a.body().size && cells.all{it.x in 0 until width && it.y in 0 until height} &&
    Rules.selfClear(a,width,height)
  }
  // Sampling only randomly heavily underrepresents the central gaps.
  // Preserve a mixed pool: half center-covering shapes, half global shapes.
  val center=Pt(width/2,height/2)
  val ranked=feasible.sortedByDescending { a ->
   a.cells().count { p -> abs(p.x-center.x)<=width/4 &&
    abs(p.y-center.y)<=height/4 }
  }
  val pool=(ranked.take(450)+feasible.shuffled(random).take(450)).distinct()
  val paths=feasible.map{it to Rules.exitPath(it,width,height).toSet()}
  val added=mutableListOf<Arrow>()
  var visited=0
  var best:Level?=null
  var bestScore=LevelQualityAnalyzer.analyze(level).score(level.stage)
  fun search(depth:Int){
   if(visited>=maxNodes)return
   if(depth==suffix) {
    val candidate=Level(level.seed,level.stage,width,prefix+added,height)
    if(Rules.solve(candidate)!=null){
     val score=LevelQualityAnalyzer.analyze(candidate).score(level.stage)
     if(score>bestScore+0.01){bestScore=score;best=candidate}
    }
    return
   }
   val id=prefix.size+depth
   val occupied=taken+added.flatMap{it.cells()}
   val oldRays=(prefix+added).map{Rules.exitPath(it,width,height).toSet()}
   // A regional exact-placement search: choose alternatives occupying interior
   // empty cells with useful dependencies before low-value boundary fillers.
   val options=paths.asSequence().filter{(a,ray)->
    a.cells().none{it in occupied} && ray.none{it in occupied}
   }.map{(a,_)->
    val cells=a.cells()
    val contacts=oldRays.count{ray->cells.any{it in ray}}
    val interior=cells.count{it.x in 2 until width-2 && it.y in 2 until height-2}
    val center=cells.count{abs(it.x-width/2)<=width/4 && abs(it.y-height/2)<=height/4}
    a to (contacts*35+interior*8+center*12)
   }.sortedByDescending{it.second}.take(7).toList()
   for((shape,_) in options){
    if(++visited>maxNodes)return
    added.add(shape.copy(id=id))
    search(depth+1)
    added.removeAt(added.lastIndex)
   }
  }
  search(0)
  return Result(best?:level,visited,best!=null)
 }
}
