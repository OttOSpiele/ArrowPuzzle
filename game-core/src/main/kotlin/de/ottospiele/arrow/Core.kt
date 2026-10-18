// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.arrow
import kotlin.random.Random
data class Pt(val x:Int,val y:Int)
enum class Dir(val x:Int,val y:Int){UP(0,-1),RIGHT(1,0),DOWN(0,1),LEFT(-1,0)}
data class Arrow(val id:Int,val x:Int,val y:Int,val length:Int,val dir:Dir,val bends:List<Pt> = emptyList()){
 fun vertices():List<Pt> = if(bends.isEmpty()) listOf(Pt(x,y),Pt(x+(length-1)*dir.x,y+(length-1)*dir.y)) else listOf(Pt(x,y))+bends
 /** Ordered points from tail to tip, retained for snake-style movement. */
 fun body():List<Pt> = buildList {
  for((index,segment) in vertices().zipWithNext().withIndex()){
   val (a,b)=segment
   val dx=(b.x-a.x).compareTo(0);val dy=(b.y-a.y).compareTo(0)
   val n=kotlin.math.abs(b.x-a.x)+kotlin.math.abs(b.y-a.y)
   for(step in (if(index==0)0 else 1)..n)add(Pt(a.x+step*dx,a.y+step*dy))
  }
 }
 fun cells():Set<Pt> = buildSet {
  for((a,b) in vertices().zipWithNext()){
   require((a.x==b.x) != (a.y==b.y)) { "Only nonzero orthogonal segments allowed" }
   val dx=(b.x-a.x).compareTo(0);val dy=(b.y-a.y).compareTo(0)
   var p=a
   while(true){add(p);if(p==b)break;p=Pt(p.x+dx,p.y+dy)}
  }
 }
}
data class Level(val seed:Long,val stage:Int,val size:Int,val arrows:List<Arrow>,val height:Int=size){
 val width:Int get()=size
 val dependencies:LevelDependencies by lazy { Rules.dependencies(this) }
}
/** Cached full-level occupancy and geometric prerequisite IDs. */
data class LevelDependencies(val requiredRemovals:Map<Int,Set<Int>>){
 fun remainingBlockers(id:Int,remainingIds:Set<Int>)=requiredRemovals[id].orEmpty().intersect(remainingIds)
}
data class MovePlan(val body:List<Pt>,val travel:List<Pt>,val allowedSteps:Int,val blockedBy:Int?){
 val canExit get()=blockedBy==null
 fun frame(steps:Int):List<Pt>{
  val whole=body+travel
  val count=steps.coerceIn(0,travel.size)
  return whole.drop(count).take(body.size)
 }
}
object Rules{
 fun plan(a:Arrow,level:Level,remainingIds:Set<Int>):MovePlan{
  val body=a.body()
  val head=body.last()
  val ray=mutableListOf<Pt>()
  var p=Pt(head.x+a.dir.x,head.y+a.dir.y)
  // Continue until the LAST body point has exited (off-board positions are valid).
  repeat(maxOf(level.width,level.height)+body.size+1){
   ray.add(p)
   p=Pt(p.x+a.dir.x,p.y+a.dir.y)
  }
  val ownership=level.arrows.filter{it.id in remainingIds && it.id!=a.id}
   .flatMap{other->other.cells().map{it to other.id}}.toMap()
  val collision=ray.indexOfFirst{ownership.containsKey(it)}
  val exitSteps=when(a.dir){
   Dir.UP->head.y+body.size
   Dir.DOWN->level.height-head.y+body.size-1
   Dir.LEFT->head.x+body.size
   Dir.RIGHT->level.width-head.x+body.size-1
  }
  val steps=exitSteps.coerceAtMost(ray.size)
  val blocker=if(collision in 0 until steps)ownership[ray[collision]] else null
  return MovePlan(body,ray.take(steps),if(blocker==null)steps else collision,blocker)
 }

 /**
  * A snake leaves head first. Its body follows its OWN polyline, corner by
  * corner; the only NEW cells entered are those in the straight exit ray
  * extending the final head segment.
  */
 fun exitPath(a:Arrow,size:Int,height:Int=size):List<Pt>{
  val head=a.vertices().last()
  val result=mutableListOf<Pt>()
  var p=Pt(head.x+a.dir.x,head.y+a.dir.y)
  while(p.x in 0 until size && p.y in 0 until height){
   result.add(p);p=Pt(p.x+a.dir.x,p.y+a.dir.y)
  }
  return result
 }
 /** Reject arrows whose outward tip ray crosses their own still-visible body.
  * Otherwise spiral-like shapes can appear to pass through themselves. */
 fun selfClear(a:Arrow,width:Int,height:Int):Boolean{
  val body=a.cells()
  return exitPath(a,width,height).none{it in body}
 }
 fun dependencies(level:Level):LevelDependencies{
  val occupancy=mutableMapOf<Pt,Int>()
  for(a in level.arrows)for(p in a.cells()){
   require(p.x in 0 until level.size && p.y in 0 until level.height){ "Arrow outside board" }
   require(occupancy.put(p,a.id)==null){ "Overlapping arrows" }
  }
  return LevelDependencies(level.arrows.associate{a->
   a.id to exitPath(a,level.width,level.height).mapNotNull{occupancy[it]}.filter{it!=a.id}.toSet()
  })
 }
 fun blockers(a:Arrow,remaining:List<Arrow>,size:Int,height:Int=size):Set<Int>{
  val occupied=remaining.filter{it.id!=a.id}.associate{it.id to it.cells()}
  val ray=exitPath(a,size,height).toSet()
  return occupied.filterValues{cells->cells.any{it in ray}}.keys
 }
 fun free(a:Arrow,remaining:List<Arrow>,size:Int,height:Int=size)=blockers(a,remaining,size,height).isEmpty()
 fun solve(level:Level):List<Int>?{
  val used=mutableSetOf<Pt>()
  for(a in level.arrows)for(p in a.cells()){
   if(p.x !in 0 until level.size||p.y !in 0 until level.height||!used.add(p))return null
  }
  val rest=level.arrows.toMutableList();val result=mutableListOf<Int>()
  while(rest.isNotEmpty()){
   val ids=rest.map{it.id}.toSet()
   val next=rest.firstOrNull{level.dependencies.remainingBlockers(it.id,ids).isEmpty()}?:return null
   result.add(next.id);rest.remove(next)
  }
  return result
 }
}
/** Hit testing in grid cells: favor removable arrows within overlapping touch regions. */
object ArrowTouch {
 fun select(level:Level,remainingIds:Set<Int>,x:Float,y:Float):Int?{
  val arrows=level.arrows.filter{it.id in remainingIds}
  val candidates=arrows.map{arrow->
   val distance=arrow.cells().minOf{p->
    val dx=p.x-x;val dy=p.y-y;dx*dx+dy*dy
   }
   val free=level.dependencies.remainingBlockers(arrow.id,remainingIds).isEmpty()
   Triple(arrow.id,distance,free)
  }
  // Previously, every arrow shared a roughly 1.1-cell radius.
  // Offer generous, overlapping targets to removable arrows first.
  return candidates.filter{it.third && it.second<=1.75f*1.75f}
   .minByOrNull{it.second}?.first
   ?:candidates.filter{!it.third && it.second<=1.25f*1.25f}
    .minByOrNull{it.second}?.first
 }
}
/** Analyzes the actual collision-based dependency graph, rather than guessing from appearance. */
data class PatternMetrics(
 val initiallyFree:Int,
 val longestChain:Int,
 val keyArrows:Int,
 val alternateDirectionEdges:Int,
 val branchArrows:Int
)
object PatternAnalysis {
 fun analyze(level:Level):PatternMetrics {
  val edges=level.arrows.associate{blocked->
   blocked.id to Rules.blockers(blocked,level.arrows,level.width,level.height)
  }
  val free=edges.values.count{it.isEmpty()}
  val byId=level.arrows.associateBy{it.id}
  val outgoing=mutableMapOf<Int,MutableSet<Int>>()
  for((blocked,blockers) in edges)for(id in blockers)outgoing.getOrPut(id){mutableSetOf()}.add(blocked)
  val memo=mutableMapOf<Int,Int>()
  val visiting=mutableSetOf<Int>()
  fun depth(id:Int):Int {
   memo[id]?.let{return it}
   if(!visiting.add(id))return 0
   val result=1+(outgoing[id]?.maxOfOrNull{depth(it)}?:0)
   visiting.remove(id);memo[id]=result
   return result
  }
  val alternating=outgoing.entries.sumOf{(from,targets)->
   targets.count{to->byId[from]?.dir!=byId[to]?.dir}
  }
  return PatternMetrics(free,level.arrows.maxOfOrNull{depth(it.id)}?:0,
   outgoing.count{it.value.size>=2},alternating,
   outgoing.count{it.value.size>=2&&it.value.mapNotNull{n->byId[n]?.dir}.distinct().size>=2})
 }
}
enum class PatternFamily { INTRO, KEY_ARROW, ALTERNATING_CHAIN, BRANCH }

/** A stage-specific preference, not a claim that a seed always yields a perfect pattern. */
object PatternGoals {
 fun preferred(stage:Int):PatternFamily=when {
  stage<=3->PatternFamily.INTRO
  stage<=8->PatternFamily.KEY_ARROW
  stage<=16->PatternFamily.ALTERNATING_CHAIN
  else->PatternFamily.BRANCH
 }
 fun reached(family:PatternFamily,m:PatternMetrics)=when(family){
  PatternFamily.INTRO->true
  PatternFamily.KEY_ARROW->m.keyArrows>=1
  PatternFamily.ALTERNATING_CHAIN->m.longestChain>=3 && m.alternateDirectionEdges>=2
  PatternFamily.BRANCH->m.branchArrows>=1
 }
 fun score(family:PatternFamily,m:PatternMetrics):Int=when(family){
  PatternFamily.INTRO->m.longestChain.coerceAtMost(2)*2-m.keyArrows*4-m.branchArrows*4
  PatternFamily.KEY_ARROW->m.keyArrows*20+m.longestChain*2-m.branchArrows*3
  PatternFamily.ALTERNATING_CHAIN->m.alternateDirectionEdges*12+m.longestChain*8+m.keyArrows*2
  PatternFamily.BRANCH->m.branchArrows*35+m.keyArrows*6+m.longestChain*4
 }
}
object BoardProfile {
 fun size(stage:Int):Int=when {
  stage<=1->17
  stage<=21->18
  stage<=24->19
  else->(18+(stage-25)/10*2+2).coerceAtMost(34)
 }
 fun height(stage:Int,aspect:Float):Int=
  (size(stage)/aspect.coerceIn(1f,1.65f)).toInt().coerceIn(11,size(stage))
 fun count(stage:Int):Int=when {
  stage<=5->stage.coerceAtLeast(1)+3
  stage<=24->(8+(stage-5)).coerceAtMost(20)
  stage<=24->(8+(stage-5)).coerceAtMost(20)
  else->(20+(stage-24)).coerceAtMost(40)
 }
 /** Long-term aspiration, not a hard promise: higher occupancy must never
  * displace solvability, readable paths, or predictable generation time. */
 fun targetOccupancy(stage:Int):Float=when {
  stage<=8->0.08f
  stage<=24->0.08f+(stage-8)*0.005f
  stage<=80->0.16f+(stage-24)*0.006f
  else->(0.496f+(stage-80)*0.0028f).coerceAtMost(1f)
 }
 fun actualOccupancy(level:Level):Float=
  level.arrows.sumOf { it.cells().size }.toFloat()/(level.width*level.height)
}

/** The arrows are listed in reverse removal order: targets first, blockers last. */
data class PatternBlueprint(val arrows:List<Arrow>,val requiredEdges:Set<Pair<Int,Int>>){
 fun realized(level:Level):Boolean=requiredEdges.all{(blocker,blocked)->
  blocker in level.dependencies.requiredRemovals[blocked].orEmpty()
 }
}
object PatternConstruction {
 fun blueprint(stage:Int,size:Int,seed:Long=0L):PatternBlueprint?{
  // High stages no longer reserve the same three final arrows on every board.
  // Their dependencies arise from the complete reverse construction instead.
  if(stage<=5 || stage>=35)return null
  val m=size/2
  val a=Arrow(0,m-5,m-1,2,Dir.RIGHT)
  val b=if(stage>=17)Arrow(1,m+5,m+1,2,Dir.LEFT)
   else Arrow(1,m-5,m+1,2,Dir.RIGHT)
  val key=Arrow(2,m,m-1,3,Dir.DOWN)
  val canonical=if(stage in 6..8 || stage>=17){
   PatternBlueprint(listOf(a,b,key),setOf(2 to 0,2 to 1))
  }else{
   val first=Arrow(0,m-5,m,2,Dir.RIGHT)
   val middle=Arrow(1,m-2,m-2,3,Dir.DOWN)
   val last=Arrow(2,m,m+2,3,Dir.LEFT)
   PatternBlueprint(listOf(first,middle,last),setOf(1 to 0,2 to 1))
  }
  // Transform the entire motif, including directions and bends, to preserve
  // all geometric collision relations and the declared prerequisite edges.
  val random=Random(seed xor (stage.toLong()*1000003L))
  val turns=random.nextInt(4)
  val mirror=random.nextBoolean()
  val shiftX=random.nextInt(-2,3)
  val shiftY=random.nextInt(-2,3)
  fun transform(p:Pt):Pt {
   var x=p.x-m
   var y=p.y-m
   if(mirror)x=-x
   repeat(turns){val tmp=x;x=-y;y=tmp}
   return Pt(x+m+shiftX,y+m+shiftY)
  }
  fun direction(d:Dir):Dir {
   val zero=transform(Pt(m,m))
   val other=transform(Pt(m+d.x,m+d.y))
   return Dir.entries.first{it.x==other.x-zero.x && it.y==other.y-zero.y}
  }
  val arrows=canonical.arrows.map{arrow->
   val base=transform(Pt(arrow.x,arrow.y))
   Arrow(arrow.id,base.x,base.y,arrow.length,direction(arrow.dir),
    arrow.bends.map(::transform))
  }
  val candidate=Level(seed,stage,size,arrows)
  val valid=try { Rules.solve(candidate)!=null && canonical.requiredEdges.all{(blocker,blocked)-> blocker in candidate.dependencies.requiredRemovals[blocked].orEmpty() } }catch(_:Exception){false}
  return PatternBlueprint(if(valid)arrows else canonical.arrows,canonical.requiredEdges)
 }
}

/**
 * Constructive level builder. Arrows are inserted in reverse removal order:
 * each new arrow MUST have a free exit through the arrows already constructed.
 * Existing arrows may become blocked by the new arrow, creating a DAG by design.
 * A deterministic, seeded enumeration selects fitting geometric candidates;
 * no random full-level retry loop is necessary.
 */
class Generator {
 fun generate(stage:Int,seed:Long,aspect:Float=1f):Level {
  val portrait=aspect<1f
  val normalized=if(portrait)1f/aspect.coerceAtLeast(.2f) else aspect
  val width=BoardProfile.size(stage)
  val height=BoardProfile.height(stage,normalized)
  val count=BoardProfile.count(stage)
  val blue=PatternConstruction.blueprint(stage,minOf(width,height),seed)
  val anchors=blue?.let { blueprint ->
   val off=Pt((width-minOf(width,height))/2,(height-minOf(width,height))/2)
   blueprint.arrows.map { a -> Arrow(a.id,a.x+off.x,a.y+off.y,
    a.length,a.dir,a.bends.map { Pt(it.x+off.x,it.y+off.y) }) }
  }.orEmpty()
  // Attempt a complete graph embedding first. The legacy reverse builder is
  // retained as a safe fallback while the family catalogue is being expanded.
  val planned=if(stage in 6..16) {
   val graph=DependencyPlanner.plan(stage,seed,count,anchors.size)
   // One logical plan, two reproducible geometric search orders. A failed
   // embedding must never force a different level seed or block gameplay.
   GeometricBacktracker.place(graph,stage,seed,width,height,anchors,maxNodes=1200)
    ?:GeometricBacktracker.place(graph,stage,seed xor 0x5eedL,width,height,anchors,maxNodes=1200)
  } else null
  val raw=if(planned!=null) Level(seed,stage,width,planned.arrows,height)
   else construct(stage,seed,normalized)
  return if(portrait)rotatePortrait(raw) else raw
 }
 private fun rotatePortrait(level:Level):Level {
  fun turn(p:Pt)=Pt(level.height-1-p.y,p.x)
  val arrows=level.arrows.map{a->
   val p=turn(Pt(a.x,a.y))
   val dir=Dir.entries.first{it.x==-a.dir.y && it.y==a.dir.x}
   Arrow(a.id,p.x,p.y,a.length,dir,a.bends.map(::turn))
  }
  return Level(level.seed,level.stage,level.height,arrows,level.width)
 }
 private data class Shape(val offsets:List<Pt>,val direction:Dir){
  fun place(id:Int,x:Int,y:Int):Arrow{
   val v=offsets.map{Pt(x+it.x,y+it.y)}
   val last=v.last()
   val before=v[v.lastIndex-1]
   val length=kotlin.math.abs(last.x-before.x)+kotlin.math.abs(last.y-before.y)+1
   return Arrow(id,x,y,length,direction,if(v.size==2)emptyList() else v.drop(1))
  }
 }
 private fun shapes(stage:Int,enableNested:Boolean=true):List<Shape> {
  val result=mutableListOf<Shape>()
  for(dir in Dir.entries){
   val side=if(dir.x!=0)listOf(Dir.UP,Dir.DOWN) else listOf(Dir.LEFT,Dir.RIGHT)
   fun add(vararg legs:Pair<Dir,Int>){
    val vertices=mutableListOf(Pt(0,0))
    for((d,n) in legs){
     val p=vertices.last()
     vertices.add(Pt(p.x+d.x*n,p.y+d.y*n))
    }
    result.add(Shape(vertices,dir))
   }
   for(n in 1..(if(stage>=45)8 else if(stage>=25)5 else 3))add(dir to n)
   if(stage>=2)for(s in side)for(n in 2..3)for(k in 2..3)
    add(s to n,dir to k)
   if(stage>=12)for(s in side)for(n in 2..3)for(k in 2..3)
    add(dir to n,s to k,dir to 2)
   if(stage>=24)for(s in side)for(w in 3..5)for(h in 3..4){
    val inner=w-2
    // A U-shaped winding with a genuine inward return and a free exit ray.
    add(s to h,Dir.entries.first{it.x==-dir.x&&it.y==-dir.y} to w,
     Dir.entries.first{it.x==-s.x&&it.y==-s.y} to (h+2),dir to inner)
   }
   // Interleaved corridors inspired by dense maze-like puzzles. Each path
   // remains a single ordered snake; the terminal ray stays outside its
   // own body. Geometry is still independently checked at placement.
   if(enableNested && stage>=35)for(s in side){
    val opposite=Dir.entries.first{it.x==-dir.x && it.y==-dir.y}
    val reverse=Dir.entries.first{it.x==-s.x && it.y==-s.y}
    for(span in 3..4) {
     // Wide open spiral / nested U, with an outward-facing final segment.
     add(s to span,opposite to (span+2),reverse to (span+2),
      dir to (span+4),s to 1,dir to 2)
    }
   }
   if(stage>=60)for(s in side) {
    val opposite=Dir.entries.first{it.x==-dir.x && it.y==-dir.y}
    val reverse=Dir.entries.first{it.x==-s.x && it.y==-s.y}
    // A readable, self-avoiding five-leg winding; longer paths consume more
    // cells without introducing abrupt difficulty for early players.
    for(span in 3..4) add(s to span,opposite to 4,
     reverse to (span+2),dir to 6,s to 2,dir to 2)
   }
  }
  return result
 }
 private fun construct(stage:Int,seed:Long,aspect:Float):Level =
  if(stage>=35) runCatching { constructOnce(stage,seed,aspect,true) }
   .getOrElse { constructOnce(stage,seed,aspect,false) }
  else constructOnce(stage,seed,aspect,false)

 private fun constructOnce(stage:Int,seed:Long,aspect:Float,enableNested:Boolean):Level {
  val width=BoardProfile.size(stage)
  val height=BoardProfile.height(stage,aspect)
  val count=BoardProfile.count(stage)
  val blueprint=PatternConstruction.blueprint(stage,minOf(width,height),seed)
  val arrows=mutableListOf<Arrow>()
  var realizedGroup:DependencyPlan?=null
  // In later stages, build a connected, seed-dependent dependency group
  // BEFORE greedy filling. The group is a DAG, not a hardcoded visual motif.
  // Its geometry is backed by bounded DFS and individually verified.
  if(enableNested && stage>=35) {
   val groupSize=minOf(5,count)
   val groupPlan=DependencyPlanner.plan(stage,seed,groupSize)
   val group=GeometricBacktracker.place(
    groupPlan,stage,seed,width,height,maxNodes=160
   )
   if(group!=null) {
    arrows.addAll(group.arrows)
    realizedGroup=groupPlan
   }
  }
  if(blueprint!=null){
   val off=Pt((width-minOf(width,height))/2,(height-minOf(width,height))/2)
   arrows.addAll(blueprint.arrows.map{a->Arrow(a.id,a.x+off.x,a.y+off.y,
    a.length,a.dir,a.bends.map{Pt(it.x+off.x,it.y+off.y)})})
  }
  val templates=shapes(stage,enableNested)
  val occupied=arrows.flatMapTo(mutableSetOf()){it.cells()}
  val random=Random(seed xor (stage.toLong()*127773L))
  // A fixed permutation makes the construction different for every seed,
  // without relying on a series of random failed complete boards.
  val order=templates.indices.shuffled(random)
  val cellsPerArrow=templates.map{shape->
   val points=shape.offsets
   points.zipWithNext().sumOf{(a,b)->kotlin.math.abs(a.x-b.x)+kotlin.math.abs(a.y-b.y)}+1
  }
  fun choose(id:Int):Arrow?{
   val targetComplexity=when{
    enableNested && stage>=35 && id in 3..5->6
    stage>=24 && id in 3..5->4
    stage>=12 && id in 3..6->3
    stage>=2 && id<count/2->2
    else->1
   }
   // Reuse geometry of all previously built arrows for this placement step.
   val priorRays=arrows.map{Rules.exitPath(it,width,height).toSet()}
   // Reward blocking arrows that are STILL free. Blocking an already blocked
   // arrow adds an edge but does not reduce the number of easy opening moves.
   val freeRays=arrows.filter { a ->
    Rules.exitPath(a,width,height).none { it in occupied && it !in a.cells() }
   }.map { Rules.exitPath(it,width,height).toSet() }
   val existingDirections=arrows.groupingBy { it.dir }.eachCount()
   val distances=Array(height){IntArray(width){Int.MAX_VALUE/2}}
   if(occupied.isNotEmpty()){
    val queue=ArrayDeque<Pt>()
    occupied.forEach{p->distances[p.y][p.x]=0;queue.addLast(p)}
    while(queue.isNotEmpty()){
     val p=queue.removeFirst()
     val next=distances[p.y][p.x]+1
     for(d in Dir.entries){
      val nx=p.x+d.x;val ny=p.y+d.y
      if(nx in 0 until width && ny in 0 until height && next<distances[ny][nx]){
       distances[ny][nx]=next
       queue.addLast(Pt(nx,ny))
      }
     }
    }
   }
   val usedBands=arrows.flatMap{it.cells()}.groupingBy{
    (it.x*3/width) to (it.y*3/height)
   }.eachCount()
   var winner:Arrow?=null
   var winningScore=Int.MIN_VALUE
   for(index in order){
    val shape=templates[index]
    val segments=shape.offsets.size-1
    if(stage<=5 && segments>2)continue
    // Reserve the expansive shapes for early motifs; late filling must be cheap.
    if(id>=count*3/4 && (segments!=1 || cellsPerArrow[index]>4))continue
    if(id>=count/2 && segments>2)continue
    if(id<count/2 && targetComplexity>=3 && segments==2)continue
    val minX=shape.offsets.minOf{it.x}
    val maxX=shape.offsets.maxOf{it.x}
    val minY=shape.offsets.minOf{it.y}
    val maxY=shape.offsets.maxOf{it.y}
    for(y in -minY until height-maxY)for(x in -minX until width-maxX){
     // Deterministic sub-lattice sampling reduces candidate churn without
     // introducing random failed whole-board generation passes.
     if(id<count/2 && (x+y+index+id)%2!=0)continue
     val a=shape.place(id,x,y)
     val body=a.cells()
     if(body.size!=cellsPerArrow[index] || body.any{it in occupied})continue
     if(!Rules.selfClear(a,width,height))continue
     // Reverse construction invariant: this arrow exits with all older arrows present.
     if(Rules.exitPath(a,width,height).any{it in occupied})continue
     val unlocked=priorRays.count{ray->body.any{it in ray}}
     val newlyBlocked=freeRays.count{ray->body.any{it in ray}}
     val centerX=body.sumOf{it.x}/body.size
     val centerY=body.sumOf{it.y}/body.size
     val band=(centerX*3/width) to (centerY*3/height)
     val coverage=-(usedBands[band]?:0)*2
     // Prefer unused interior cells and paths touching existing arrow bodies.
     // The condition remains soft; the exit-ray invariant is mandatory.
     val interiorCells=body.count { p ->
      p.x>=2 && p.y>=2 && p.x<width-2 && p.y<height-2
     }
     val contact=body.sumOf { p ->
      Dir.entries.count { d -> Pt(p.x+d.x,p.y+d.y) in occupied }
     }
     val centerVacancy=if(stage>=35) {
      val middle=Pt(width/2,height/2)
      body.count { p ->
       kotlin.math.abs(p.x-middle.x)<=width/4 &&
       kotlin.math.abs(p.y-middle.y)<=height/4
      }
     } else 0
     val preference=if(segments==targetComplexity)95 else if(segments>1 && id<count/2)22 else 0
     // Later stages favor shared unlocking relations over isolated arrows.
     // Do not make this a hard constraint: the reverse builder must always
     // retain room for an exit-ready arrow.
     val links=when {
      stage>=35 && id>=count/3 -> unlocked*65
      id<=3 -> unlocked*12
      else -> unlocked*35
     }
     val isolation=if(occupied.isEmpty())0 else body.minOf{p->distances[p.y][p.x]}
     val distancePenalty=(isolation-4).coerceAtLeast(0)*6
     val densityWeight=when {
      stage>=80->3
      stage>=45->2
      stage>=24->1
      else->0
     }
     // Reward genuinely occupied cells, not merely long bounding boxes.
     // Prefer connected dependencies even in densely packed stages.
     // Key arrows that close several currently open exit paths make a real
     // puzzle instead of merely accumulating redundant blocker edges.
     val freeReduction=if(stage>=35) newlyBlocked*100 else 0
     val directionBalance=if(stage>=35) -(existingDirections[a.dir]?:0)*4 else 0
     val fillInterior=if(stage>=35 && id<count*3/4)
      interiorCells*9+centerVacancy*12+contact*7 else 0
     val score=preference+links+freeReduction+directionBalance+fillInterior+coverage-distancePenalty+
      (if(segments>=3)15 else 0)+cellsPerArrow[index]*densityWeight+random.nextInt(12)
     if(score>winningScore){winningScore=score;winner=a}
    }
   }
   return winner
  }
  for(id in arrows.size until count){
   val a=choose(id)?:error("Cannot construct arrow $id at stage $stage")
   arrows.add(a)
   occupied.addAll(a.cells())
  }
  val level=Level(seed,stage,width,arrows,height)
  check(Rules.solve(level)!=null)
  check(realizedGroup==null || realizedGroup!!.edges.all { (from,to)->
   from in level.dependencies.requiredRemovals[to].orEmpty()
  })
  check(blueprint==null || blueprint.requiredEdges.all{(blocker,blocked)->
   blocker in level.dependencies.requiredRemovals[blocked].orEmpty()
  })
  // Regional constraint repair is deliberately bounded. It only replaces
  // late blockers and never accepts an unverified or lower-quality layout.
  return if(stage>=35 && enableNested)
   RegionalPacker.improve(level,seed,maxNodes=48).level else level
 }
}
class Game(val level:Level,val maxLives:Int=5,val maxHints:Int=3){
 val remaining=level.arrows.toMutableList()
 private val dependencyMatrix=level.dependencies
 val mistakes=mutableMapOf<Int,Set<Int>>()
 var lives=maxLives;private set
 var hints=maxHints;private set
 val won get()=remaining.isEmpty()
 val lost get()=lives==0
 fun tap(id:Int):Boolean{
  if(lost)return false
  val a=remaining.firstOrNull{it.id==id}?:return false
  val blocked=dependencyMatrix.remainingBlockers(id,remaining.map{it.id}.toSet())
  if(blocked.isEmpty()){remaining.remove(a);mistakes.remove(id);return true}
  if(mistakes[id]!=blocked){mistakes[id]=blocked;lives=(lives-1).coerceAtLeast(0)}
  return false
 }
 fun hint():Int?{
  if(hints==0)return null
  val ids=remaining.map{it.id}.toSet()
  val id=remaining.firstOrNull{dependencyMatrix.remainingBlockers(it.id,ids).isEmpty()}?.id?:return null
  hints--;return id
 }
 fun restore(ids:Set<Int>,savedLives:Int,savedHints:Int,savedMistakes:Map<Int,Set<Int>>){
  require(savedLives in 0..maxLives && savedHints in 0..maxHints)
  require(ids.all{id->level.arrows.any{it.id==id}})
  require(savedMistakes.keys.all{it in ids})
  remaining.clear();remaining.addAll(level.arrows.filter{it.id in ids})
  mistakes.clear();mistakes.putAll(savedMistakes)
  lives=savedLives;hints=savedHints
 }
 fun retry(){remaining.clear();remaining.addAll(level.arrows);mistakes.clear();lives=maxLives;hints=maxHints}
}
data class Progress(val stage:Int=1,val done:Int=0){
 fun required():Int=when{
  stage<=8->1
  stage<=16->2
  else->((stage-17)/8+3).coerceAtMost(10)
 }
 fun win():Progress=if(done+1>=required())Progress(stage+1,0)else copy(done=done+1)
 fun easier()=Progress((stage-1).coerceAtLeast(1),0)
}
