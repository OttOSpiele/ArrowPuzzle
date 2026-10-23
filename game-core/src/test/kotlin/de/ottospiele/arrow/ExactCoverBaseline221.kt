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
object ExactCoverBaseline221 {
 data class Result(val level:Level?,val nodes:Int,val exhausted:Boolean)
 private data class Placement(val arrow:Arrow,val cells:Set<Pt>)

 fun generate(width:Int,height:Int,seed:Long,stage:Int=35,maxNodes:Int=50000,targetArrows:Int?=null,minWindingArrows:Int=0,maxInitiallyFree:Int?=null,minChainLength:Int=0):Result {
  require(width in 2..20 && height in 2..20)
  require(maxNodes>0)
  require(targetArrows==null || targetArrows in 1..width*height)
  require(minWindingArrows in 0..(targetArrows?:width*height))
  require(maxInitiallyFree==null || maxInitiallyFree>=0)
  require(minChainLength>=0)
  val random=Random(seed)
  val placements=mutableListOf<Placement>()
  var number=0
  for(y in 0 until height)for(x in 0 until width)for(dir in Dir.entries) {
   val limit=when(dir){Dir.LEFT->x+1;Dir.RIGHT->width-x;Dir.UP->y+1;Dir.DOWN->height-y}
   for(length in 2..minOf(limit,if(targetArrows!=null)10 else 6)) {
    val tail=Pt(x,y)

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
   }
   // Multi-bend meanders: orthogonal first, then a reverse or forward
   // return, finishing in the designated tip direction.
   // Generate a bounded family to avoid quadratic candidate explosion.
   if(width>=6 && height>=6 && stage>=24) {
    val sides=Dir.entries.filter { it.x*dir.x+it.y*dir.y==0 }
    for(side in sides)for(lateral in 2..3)for(run in 2..4) {
     val p1=Pt(x+side.x*lateral,y+side.y*lateral)
     val p2=Pt(p1.x+dir.x*run,p1.y+dir.y*run)
     val p3=Pt(p2.x-side.x,p2.y-side.y)
     val p4=Pt(p3.x+dir.x*2,p3.y+dir.y*2)
     val vertices=listOf(p1,p2,p3,p4)
     if(vertices.all { it.x in 0 until width && it.y in 0 until height }) {
      val winding=Arrow(number++,x,y,3,dir,vertices)
      if(winding.cells().size==winding.body().size &&
         Rules.selfClear(winding,width,height))
       placements.add(Placement(winding,winding.cells()))
     }
    }
   }
  }
  val order=placements.indices.shuffled(random)
  val candidates=order.map{placements[it]}
  val maxCells=candidates.maxOf { it.cells.size }
  val winding=BooleanArray(candidates.size) { candidates[it].arrow.vertices().size>=5 }
  val byCell=Array(width*height){mutableListOf<Int>()}
  candidates.forEachIndexed { index,placement ->
   for(p in placement.cells)byCell[p.y*width+p.x].add(index)
  }
  val used=BooleanArray(width*height)
  val chosen=mutableListOf<Int>()
  // A cycle between already selected arrows cannot be undone by adding
  // more arrows. Prune such branches before expensive completion search.
  fun partialAcyclic():Boolean {
   val arrows=chosen.mapIndexed { id,index -> candidates[index].arrow.copy(id=id) }
   val owners=mutableMapOf<Pt,Int>()
   for(a in arrows)for(p in a.cells())owners[p]=a.id
   val edges=arrows.associate { a ->
    a.id to Rules.exitPath(a,width,height).mapNotNull { owners[it] }
     .filter { it!=a.id }.toSet()
   }
   val states=IntArray(arrows.size)
   fun visit(id:Int):Boolean {
    if(states[id]==1)return false
    if(states[id]==2)return true
    states[id]=1
    for(next in edges[id].orEmpty())if(!visit(next))return false
    states[id]=2
    return true
   }
   return arrows.indices.all { visit(it) }
  }
  fun regionsFeasible():Boolean {
   val seen=BooleanArray(width*height)
   var minimum=0
   var maximum=0
   for(root in used.indices) {
    if(used[root] || seen[root])continue
    val queue=ArrayDeque<Int>()
    queue.add(root)
    seen[root]=true
    var count=0
    while(queue.isNotEmpty()) {
     val cell=queue.removeFirst()
     count++
     val x=cell%width
     val y=cell/width
     val adjacent=intArrayOf(if(x>0)cell-1 else -1,if(x<width-1)cell+1 else -1,if(y>0)cell-width else -1,if(y<height-1)cell+width else -1)
     for(next in adjacent)if(next>=0 && !used[next] && !seen[next]) {
      seen[next]=true
      queue.add(next)
     }
    }
    if(count<2)return false
    minimum+=(count+maxCells-1)/maxCells
    maximum+=count/2
   }
   if(targetArrows!=null) {
    val slots=targetArrows-chosen.size
    if(slots<minimum || slots>maximum)return false
   }
   return true
  }
  var nodes=0
  var best:Level?=null
  fun search(remaining:Int) {
   if(best!=null || nodes>=maxNodes)return
   if(remaining==0) {
    if(targetArrows!=null && chosen.size!=targetArrows)return
    if(chosen.count { winding[it] }<minWindingArrows)return
    val arrows=chosen.mapIndexed{ id,index ->
     candidates[index].arrow.copy(id=id)
    }
    val trial=Level(seed,stage,width,arrows,height)
    if(Rules.solve(trial)!=null) {
     val q=LevelQualityAnalyzer.analyze(trial)
     if((maxInitiallyFree==null || q.initiallyFree<=maxInitiallyFree) && q.longestChain>=minChainLength)best=trial
    }
    return
   }
   if(targetArrows!=null) {
    val missing=targetArrows-chosen.size
    if(missing<=0 || remaining<missing*2 || remaining>missing*maxCells)return
   }
   val windingCount=chosen.count { winding[it] }
   if(targetArrows!=null && windingCount+(targetArrows-chosen.size)<minWindingArrows)return
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
   val ordered=if(minWindingArrows>0 && windingCount<minWindingArrows)viable!!.sortedByDescending { winding[it] } else viable!!.toList()
   for(index in ordered) {
    if(++nodes>maxNodes)return
    val cells=candidates[index].cells.map{it.y*width+it.x}
    if(targetArrows!=null && windingCount+(if(winding[index])1 else 0)+targetArrows-chosen.size-1<minWindingArrows)continue
    if(targetArrows!=null) {
     val left=remaining-cells.size
     val slots=targetArrows-chosen.size-1
     if(left<slots*2 || left>slots*maxCells)continue
    }
    cells.forEach{used[it]=true}
    chosen.add(index)
    if(partialAcyclic() && regionsFeasible())search(remaining-cells.size)
    chosen.removeAt(chosen.lastIndex)
    cells.forEach{used[it]=false}
    if(best!=null)return
   }
  }
  search(width*height)
  return Result(best,nodes,best==null && nodes>=maxNodes)
 }
}
