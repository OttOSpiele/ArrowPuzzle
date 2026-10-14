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
data class Level(val seed:Long,val stage:Int,val size:Int,val arrows:List<Arrow>){
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
  repeat(level.size+body.size+1){
   ray.add(p)
   p=Pt(p.x+a.dir.x,p.y+a.dir.y)
  }
  val ownership=level.arrows.filter{it.id in remainingIds && it.id!=a.id}
   .flatMap{other->other.cells().map{it to other.id}}.toMap()
  val collision=ray.indexOfFirst{ownership.containsKey(it)}
  val exitSteps=when(a.dir){
   Dir.UP->head.y+body.size
   Dir.DOWN->level.size-head.y+body.size-1
   Dir.LEFT->head.x+body.size
   Dir.RIGHT->level.size-head.x+body.size-1
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
 fun exitPath(a:Arrow,size:Int):List<Pt>{
  val head=a.vertices().last()
  val result=mutableListOf<Pt>()
  var p=Pt(head.x+a.dir.x,head.y+a.dir.y)
  while(p.x in 0 until size && p.y in 0 until size){
   result.add(p);p=Pt(p.x+a.dir.x,p.y+a.dir.y)
  }
  return result
 }
 fun dependencies(level:Level):LevelDependencies{
  val occupancy=mutableMapOf<Pt,Int>()
  for(a in level.arrows)for(p in a.cells()){
   require(p.x in 0 until level.size && p.y in 0 until level.size){ "Arrow outside board" }
   require(occupancy.put(p,a.id)==null){ "Overlapping arrows" }
  }
  return LevelDependencies(level.arrows.associate{a->
   a.id to exitPath(a,level.size).mapNotNull{occupancy[it]}.filter{it!=a.id}.toSet()
  })
 }
 fun blockers(a:Arrow,remaining:List<Arrow>,size:Int):Set<Int>{
  val occupied=remaining.filter{it.id!=a.id}.associate{it.id to it.cells()}
  val ray=exitPath(a,size).toSet()
  return occupied.filterValues{cells->cells.any{it in ray}}.keys
 }
 fun free(a:Arrow,remaining:List<Arrow>,size:Int)=blockers(a,remaining,size).isEmpty()
 fun solve(level:Level):List<Int>?{
  val used=mutableSetOf<Pt>()
  for(a in level.arrows)for(p in a.cells()){
   if(p.x !in 0 until level.size||p.y !in 0 until level.size||!used.add(p))return null
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
   blocked.id to Rules.blockers(blocked,level.arrows,level.size)
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
 fun size(stage:Int)=15+stage.coerceIn(1,5)*2
 fun count(stage:Int)=if(stage<=5)stage+3 else (8+(stage-5)*2).coerceAtMost(24)
}
class Generator{
 fun generate(stage:Int,seed:Long):Level{
  val family=PatternGoals.preferred(stage)
  val attempts=if(stage<=3)1 else 8
  var best:Level?=null
  var bestScore=Int.MIN_VALUE
  for(attempt in 0 until attempts){
   // Derived independently but deterministically, without using random global state.
   val random=Random(seed+attempt*1000003L)
   val level=makeCandidate(stage,seed,random)
   val m=PatternAnalysis.analyze(level)
   val score=PatternGoals.score(family,m)
   if(score>bestScore){bestScore=score;best=level}
   if(PatternGoals.reached(family,m))return level
  }
  return best?:error("Unable to construct a puzzle")
 }
 private fun makeCandidate(stage:Int,seed:Long,random:Random):Level{
  val size=BoardProfile.size(stage)
  val arrows=mutableListOf<Arrow>()
  val count=BoardProfile.count(stage)
  val family=PatternGoals.preferred(stage)
  repeat(count){id->
   var best:Arrow?=null
   var bestScore=Int.MIN_VALUE
   repeat(if(stage<=5)260 else 420){
    val dir=Dir.entries.random(random)
    val length=random.nextInt(2,5)
    val sx=if(stage<=5)random.nextInt(size) else random.nextInt(3,size-3)
    val sy=if(stage<=5)random.nextInt(size) else random.nextInt(3,size-3)
    val a=if(stage>=2 && random.nextInt(100)<65){
     val middleDir=if(dir.x!=0)if(random.nextBoolean())Dir.UP else Dir.DOWN
      else if(random.nextBoolean())Dir.LEFT else Dir.RIGHT
     val turn=Pt(sx+random.nextInt(2,5)*middleDir.x,
      sy+random.nextInt(2,5)*middleDir.y)
     val tip=Pt(turn.x+(length-1)*dir.x,turn.y+(length-1)*dir.y)
     Arrow(id,sx,sy,length,dir,listOf(turn,tip))
    }else Arrow(id,sx,sy,length,dir)
    val cells=a.cells()
    if(cells.size!=a.vertices().zipWithNext().sumOf{(p,q)->
      kotlin.math.abs(p.x-q.x)+kotlin.math.abs(p.y-q.y)
     }+1)return@repeat
    if(cells.any{p->p.x !in 0 until size||p.y !in 0 until size||arrows.any{p in it.cells()}})return@repeat
    if(!Rules.free(a,arrows+a,size))return@repeat
    val unlocked=arrows.filter{old->Rules.blockers(old,listOf(old,a),size).isNotEmpty()}
    val different=unlocked.count{it.dir!=a.dir}
    val branches=unlocked.map{it.dir}.distinct().size
    val base=unlocked.size*12+random.nextInt(7)
    val patternBonus=when(family){
     PatternFamily.INTRO->-unlocked.size*6
     PatternFamily.KEY_ARROW->if(unlocked.size>=2)35 else 0
     PatternFamily.ALTERNATING_CHAIN->different*18
     PatternFamily.BRANCH->if(branches>=2)45 else different*4
    }
    val cluster=if(stage<=5)0 else {
     val cx=cells.sumOf{it.x}/cells.size
     val cy=cells.sumOf{it.y}/cells.size
     val centerDistance=kotlin.math.abs(cx-size/2)+kotlin.math.abs(cy-size/2)
     val neighborDistance=if(arrows.isEmpty())0 else arrows.minOf{old->
      old.cells().minOf{p->cells.minOf{q->
       kotlin.math.abs(p.x-q.x)+kotlin.math.abs(p.y-q.y)
      }}
     }
     -centerDistance*4-neighborDistance*6
    }
    val score=base+patternBonus+(if(a.bends.isNotEmpty())2 else 0)+cluster
    if(score>bestScore){best=a;bestScore=score}
   }
   arrows.add(best?:error("No placement for arrow $id"))
  }
  return Level(seed,stage,size,arrows).also{check(Rules.solve(it)!=null)}
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
