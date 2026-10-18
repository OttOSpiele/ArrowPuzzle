// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.arrow

import kotlin.random.Random

/**
 * Independent exact-cover proof of concept. Unlike the legacy greedy builder,
 * a free cell is the search variable: choose one arrow covering it, remove all
 * overlapping candidates, and backtrack. At a complete cover, the actual
 * collision dependency graph must admit a removal order.
 *
 * Intentionally limited to small boards. Scaling requires regional
 * decomposition and propagation before enabling this in production.
 */
object ExactCoverGenerator {
 data class Result(val level:Level?,val nodes:Int,val exhausted:Boolean)
 private data class Placement(val arrow:Arrow,val cells:Set<Pt>)

 fun generate(width:Int,height:Int,seed:Long,stage:Int=35,maxNodes:Int=50000):Result {
  require(width in 2..12 && height in 2..12)
  require(maxNodes>0)
  val random=Random(seed)
  val placements=mutableListOf<Placement>()
  var number=0
  for(y in 0 until height)for(x in 0 until width)for(dir in Dir.entries) {
   val limit=when(dir){Dir.LEFT->x+1;Dir.RIGHT->width-x;Dir.UP->y+1;Dir.DOWN->height-y}
   for(length in 2..minOf(limit,6)) {
    val tail=Pt(x,y)
    val tip=Pt(x+dir.x*(length-1),y+dir.y*(length-1))
    val arrow=Arrow(number++,tail.x,tail.y,length,dir)
    placements.add(Placement(arrow,arrow.cells()))
    // Orthogonal L paths are independent candidate geometries, rather than
    // shapes inserted afterwards to repair greedy packing.
    if(length>=2)for(side in Dir.entries.filter{it.x*dir.x+it.y*dir.y==0}) {
     val elbow=Pt(x+side.x*2,y+side.y*2)
     val last=Pt(elbow.x+dir.x*(length-1),elbow.y+dir.y*(length-1))
     if(listOf(elbow,last).all{it.x in 0 until width && it.y in 0 until height}) {
      val bent=Arrow(number++,x,y,length,dir,listOf(elbow,last))
      if(Rules.selfClear(bent,width,height))
       placements.add(Placement(bent,bent.cells()))
     }
    }
  }}
  val order=placements.indices.shuffled(random)
  val candidates=order.map{placements[it]}
  val byCell=Array(width*height){mutableListOf<Int>()}
  candidates.forEachIndexed { index,placement ->
   for(p in placement.cells)byCell[p.y*width+p.x].add(index)
  }
  val used=BooleanArray(width*height)
  val chosen=mutableListOf<Int>()
  var nodes=0
  var best:Level?=null
  fun search(remaining:Int) {
   if(best!=null || nodes>=maxNodes)return
   if(remaining==0) {
    val arrows=chosen.mapIndexed{ id,index ->
     candidates[index].arrow.copy(id=id)
    }
    val trial=Level(seed,stage,width,arrows,height)
    if(Rules.solve(trial)!=null)best=trial
    return
   }
   var selected=-1
   var viable:IntArray?=null
   // Minimum-remaining-values constraint: select the hardest free cell.
   for(cell in used.indices)if(!used[cell]) {
    val compatible=byCell[cell].filter{candidate ->
     candidates[candidate].cells.none{p->used[p.y*width+p.x]}
    }.toIntArray()
    if(compatible.isEmpty())return
    if(viable==null || compatible.size<viable!!.size) {
     selected=cell;viable=compatible
     if(compatible.size==1)break
    }
   }
   if(selected<0)return
   for(index in viable!!) {
    if(++nodes>maxNodes)return
    val cells=candidates[index].cells.map{it.y*width+it.x}
    cells.forEach{used[it]=true}
    chosen.add(index)
    search(remaining-cells.size)
    chosen.removeAt(chosen.lastIndex)
    cells.forEach{used[it]=false}
    if(best!=null)return
   }
  }
  search(width*height)
  return Result(best,nodes,best==null && nodes>=maxNodes)
 }
}
