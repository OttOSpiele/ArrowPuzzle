// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.arrow

import kotlin.math.abs
import kotlin.random.Random

/**
 * A logical construction order. An edge (blocker -> blocked) means that the
 * newly inserted blocker must occupy the exit ray of the earlier arrow.
 * Because every edge points to an earlier insertion, the graph is a DAG.
 */
data class DependencyPlan(
 val order: List<Int>,
 val targets: Map<Int, Int>,
 val family: PatternFamily
) {
 init {
  require(order.distinct().size == order.size)
  require(targets.keys.all { it in order })
  require(targets.all { (from, to) -> order.indexOf(to) < order.indexOf(from) })
 }
 val edges: Set<Pair<Int, Int>> get() = targets.map { it.key to it.value }.toSet()
}

/** Plans the complete graph independently of geometry or the board aspect ratio. */
object DependencyPlanner {
 fun plan(stage: Int, seed: Long, count: Int, anchors: Int = 0): DependencyPlan {
  require(count > 0 && anchors in 0..count)
  val random = Random(seed xor (stage.toLong() * 0x2c1b3c6dL))
  val targets = mutableMapOf<Int, Int>()
  val family = PatternGoals.preferred(stage)
  for (id in maxOf(1, anchors) until count) {
   // Alternating chains favour the immediate predecessor; key and branch
   // families favour reusing an earlier hub as the shared blocked arrow.
   val target = when (family) {
    PatternFamily.INTRO -> if (id % 3 == 0) random.nextInt(id) else id - 1
    PatternFamily.KEY_ARROW -> if (id % 4 != 0) 0 else random.nextInt(id)
    PatternFamily.ALTERNATING_CHAIN -> id - 1
    PatternFamily.BRANCH -> if (id % 3 != 0) (id / 3).coerceAtMost(id - 1) else random.nextInt(id)
   }
   targets[id] = target
  }
  return DependencyPlan((0 until count).toList(), targets, family)
 }
}

/**
 * Geometry realization is a bounded, deterministic depth-first search.
 * Each candidate is checked against all occupied cells, its own tip ray,
 * and the reverse-construction solvability invariant. Failed placements
 * are undone before trying another candidate.
 *
 * A null result means the plan cannot be embedded within the search budget;
 * callers may fall back to the established constructive algorithm.
 */
object GeometricBacktracker {
 data class Result(val arrows: List<Arrow>, val nodesVisited: Int)
 private data class Candidate(val arrow: Arrow, val cells: Set<Pt>, val ray: Set<Pt>)
 fun place(
  plan: DependencyPlan, stage: Int, seed: Long, width: Int, height: Int,
  anchors: List<Arrow> = emptyList(), maxNodes: Int = 24000
 ): Result? {
  require(plan.order == (0 until plan.order.size).toList())
  require(anchors.map { it.id } == (0 until anchors.size).toList())
  val occupied = mutableSetOf<Pt>()
  for (a in anchors) {
   if (!Rules.selfClear(a, width, height)) return null
   for (p in a.cells()) if (p.x !in 0 until width || p.y !in 0 until height || !occupied.add(p)) return null
  }
  val placed = anchors.toMutableList()
  val random = Random(seed xor (stage.toLong() * 0x9e3779bL))
  val catalogue = mutableListOf<Candidate>()
  val legLengths = if (stage >= 12) 2..3 else 2..2
  for (dir in Dir.entries) {
   val sides = Dir.entries.filter { it.x * dir.x + it.y * dir.y == 0 }
   for (y in 0 until height) for (x in 0 until width) {
    val paths = mutableListOf<List<Pt>>()
    for (n in 1..3) paths.add(listOf(Pt(x,y), Pt(x+n*dir.x,y+n*dir.y)))
    if(stage >= 2) for(side in sides) for(n in legLengths) for(k in 2..3)
     paths.add(listOf(Pt(x,y),Pt(x+n*side.x,y+n*side.y),
      Pt(x+n*side.x+k*dir.x,y+n*side.y+k*dir.y)))
    if(stage >= 12) for(side in sides)
     paths.add(listOf(Pt(x,y),Pt(x+2*dir.x,y+2*dir.y),
      Pt(x+2*dir.x+2*side.x,y+2*dir.y+2*side.y),
      Pt(x+4*dir.x+2*side.x,y+4*dir.y+2*side.y)))
    for(path in paths) {
     if(path.any { it.x !in 0 until width || it.y !in 0 until height }) continue
     val last = path.last()
     val before = path[path.lastIndex-1]
     val len = abs(last.x-before.x)+abs(last.y-before.y)+1
     val a = Arrow(-1,x,y,len,dir,if(path.size==2) emptyList() else path.drop(1))
     val cells = a.cells()
     if(cells.size != path.zipWithNext().sumOf { (p,q)->abs(p.x-q.x)+abs(p.y-q.y) }+1) continue
     if(!Rules.selfClear(a,width,height)) continue
     catalogue.add(Candidate(a,cells,Rules.exitPath(a,width,height).toSet()))
    }
   }
  }
  catalogue.shuffle(random)
  // Independent of candidate enumeration: deterministic tie-breaking.
  var nodes = 0
  fun dfs(id: Int): Boolean {
   if (id == plan.order.size) return true
   if (nodes >= maxNodes) return false
   val targetId = plan.targets[id]
   val targetRay = targetId?.let { target ->
    placed.firstOrNull { it.id == target }?.let { Rules.exitPath(it,width,height).toSet() }
   }
   if (targetId != null && targetRay == null) return false
   val priorRays = placed.associate { it.id to Rules.exitPath(it,width,height).toSet() }
   // Prefer candidates satisfying the planned edge and unlocking other
   // arrows too; cap branching while still allowing backtracking.
   val options = catalogue.asSequence().filter { c ->
    c.cells.none { it in occupied } &&
     c.ray.none { it in occupied } &&
     (targetRay == null || c.cells.any { it in targetRay })
   }.map { c ->
    c to priorRays.values.count { ray -> c.cells.any { it in ray } }
   }.sortedByDescending { it.second }.take(20).toList()
   for ((candidate, _) in options) {
    if (++nodes > maxNodes) return false
    val a = candidate.arrow.copy(id=id)
    placed.add(a)
    occupied.addAll(candidate.cells)
    if(dfs(id+1)) return true
    occupied.removeAll(candidate.cells)
    placed.removeAt(placed.lastIndex)
   }
   return false
  }
  if(!dfs(anchors.size)) return null
  val level=Level(seed,stage,width,placed.toList(),height)
  if(Rules.solve(level)==null || !plan.edges.all { (from,to)->
    from in level.dependencies.requiredRemovals[to].orEmpty()
   }) return null
  return Result(level.arrows,nodes)
 }
}
