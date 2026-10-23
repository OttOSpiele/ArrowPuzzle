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
  val indexedCells=Array(candidates.size) { i -> candidates[i].cells.map { it.y*width+it.x }.toIntArray() }
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
    // Candidate-aware per-region bounds, not global length guesses.
    val membership=BooleanArray(width*height)
    val frontier=ArrayDeque<Int>()
    frontier.add(root)
    membership[root]=true
    while(frontier.isNotEmpty()) {
     val cell=frontier.removeFirst()
     val x=cell%width
     val y=cell/width
     val around=intArrayOf(if(x>0)cell-1 else -1,if(x+1<width)cell+1 else -1,if(y>0)cell-width else -1,if(y+1<height)cell+width else -1)
     for(n in around)if(n>=0 && !used[n] && !membership[n]) {
      membership[n]=true
      frontier.add(n)
     }
    }
    var longest=0
    var shortest=Int.MAX_VALUE
    for(path in indexedCells)if(path.all { membership[it] }) {
     longest=maxOf(longest,path.size)
     shortest=minOf(shortest,path.size)
    }
    if(longest==0)return false
    minimum+=(count+longest-1)/longest
    maximum+=count/shortest
   }
   if(targetArrows!=null) {
    val slots=targetArrows-chosen.size
    if(slots<minimum || slots>maximum)return false
   }
   return true
  }
  var nodes=0
  var best:Level?=null

  // Backtrack every placement, including deterministic deductions made
  // below a decision. This restores both the occupancy and dependency graph.
  fun rollback(mark:Int) {
   while(chosen.size>mark) {
    val index=chosen.removeAt(chosen.lastIndex)
    indexedCells[index].forEach { used[it]=false }
   }
  }
  fun admissible(index:Int,remaining:Int):Boolean {
   val cells=indexedCells[index]
   if(cells.any { used[it] })return false
   if(targetArrows!=null) {
    val windings=chosen.count { winding[it] }
    val slots=targetArrows-chosen.size-1
    val left=remaining-cells.size
    if(left<slots*2 || left>slots*maxCells)return false
    if(windings+(if(winding[index])1 else 0)+slots<minWindingArrows)return false
   }
   return true
  }
  fun place(index:Int):Boolean {
   chosen.add(index)
   indexedCells[index].forEach { used[it]=true }
   return partialAcyclic() && regionsFeasible()
  }
  fun search(initialRemaining:Int) {
   if(best!=null || nodes>=maxNodes)return
   val mark=chosen.size
   var remaining=initialRemaining
   try {
    // Iterate to a fixed point: each forced placement may create another
    // one-candidate cell. A contradiction rejects the whole branch.
    while(remaining>0) {
     if(targetArrows!=null) {
      val slots=targetArrows-chosen.size
      if(slots<=0 || remaining<slots*2 || remaining>slots*maxCells)return
     }
     var forced=-1
     for(cell in used.indices)if(!used[cell]) {
      var only=-1
      var multiple=false
      for(index in byCell[cell]) {
       if(!admissible(index,remaining))continue
       if(only>=0) { multiple=true;break }
       only=index
      }
      if(only<0)return
      if(!multiple) { forced=only;break }
     }
     if(forced<0)break
     if(!place(forced))return
     remaining-=indexedCells[forced].size
    }
    if(remaining==0) {
     if(targetArrows!=null && chosen.size!=targetArrows)return
     if(chosen.count { winding[it] }<minWindingArrows)return
     val trial=Level(seed,stage,width,chosen.mapIndexed { id,index ->
      candidates[index].arrow.copy(id=id)
     },height)
     if(Rules.solve(trial)!=null) {
      val q=LevelQualityAnalyzer.analyze(trial)
      if((maxInitiallyFree==null || q.initiallyFree<=maxInitiallyFree) &&
         q.longestChain>=minChainLength)best=trial
     }
     return
    }
    var viable:IntArray?=null
    for(cell in used.indices)if(!used[cell]) {
     val compatible=byCell[cell].filter { admissible(it,remaining) }.toIntArray()
     if(compatible.isEmpty())return
     if(viable==null || compatible.size<viable.size)viable=compatible
    }
    val windingCount=chosen.count { winding[it] }
    val desired=if(targetArrows!=null)remaining.toDouble()/(targetArrows-chosen.size) else 0.0
    val ordered=(viable?:return).sortedWith(compareByDescending<Int> {
     if(minWindingArrows>0 && windingCount<minWindingArrows && winding[it])1 else 0
    }.thenBy { if(targetArrows!=null)kotlin.math.abs(indexedCells[it].size-desired) else 0.0 })
    for(index in ordered) {
     if(best!=null || nodes>=maxNodes)return
     nodes++
     val branchMark=chosen.size
     if(place(index))search(remaining-indexedCells[index].size)
     rollback(branchMark)
    }
   } finally {
    rollback(mark)
   }
  }
  search(width*height)
  return Result(best,nodes,best==null && nodes>=maxNodes)
 }
}
