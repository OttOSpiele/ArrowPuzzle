package de.ottospiele.arrow
import kotlin.random.Random
data class Pt(val x:Int,val y:Int)
enum class Dir(val x:Int,val y:Int){UP(0,-1),RIGHT(1,0),DOWN(0,1),LEFT(-1,0)}
data class Arrow(val id:Int,val x:Int,val y:Int,val length:Int,val dir:Dir,val bends:List<Pt> = emptyList()){
 fun vertices():List<Pt> = if(bends.isEmpty()) listOf(Pt(x,y),Pt(x+(length-1)*dir.x,y+(length-1)*dir.y)) else listOf(Pt(x,y))+bends
 fun cells():Set<Pt> = buildSet {
  for((a,b) in vertices().zipWithNext()){
   require((a.x==b.x) != (a.y==b.y)) { "Only nonzero orthogonal segments allowed" }
   val dx=(b.x-a.x).compareTo(0);val dy=(b.y-a.y).compareTo(0)
   var p=a
   while(true){add(p);if(p==b)break;p=Pt(p.x+dx,p.y+dy)}
  }
 }
}
data class Level(val seed:Long,val stage:Int,val size:Int,val arrows:List<Arrow>)
object Rules{
 /** Every board position occupied while the entire rigid polyline exits in its tip direction. */
 fun exitFrames(a:Arrow,size:Int):List<Set<Pt>> {
  val initial=a.cells()
  val frames=mutableListOf<Set<Pt>>()
  // The farthest trailing cell determines when the full arrow has left the board.
  val steps=when(a.dir){
   Dir.UP->initial.maxOf{it.y}+1
   Dir.DOWN->size-initial.minOf{it.y}
   Dir.LEFT->initial.maxOf{it.x}+1
   Dir.RIGHT->size-initial.minOf{it.x}
  }
  for(step in 1..steps)frames.add(initial.map{Pt(it.x+step*a.dir.x,it.y+step*a.dir.y)}.toSet())
  return frames
 }
 fun blockers(a:Arrow,remaining:List<Arrow>,size:Int):Set<Int>{
  val other=remaining.filter{it.id!=a.id}.associate{it.id to it.cells()}
  val hit=mutableSetOf<Int>()
  for(frame in exitFrames(a,size)){
   other.forEach{(id,occupied)->if(frame.any{it in occupied})hit.add(id)}
  }
  return hit
 }
 fun free(a:Arrow,remaining:List<Arrow>,size:Int)=blockers(a,remaining,size).isEmpty()
 fun solve(level:Level):List<Int>?{
  val used=mutableSetOf<Pt>()
  for(a in level.arrows)for(p in a.cells()){
   if(p.x !in 0 until level.size||p.y !in 0 until level.size||!used.add(p))return null
  }
  val rest=level.arrows.toMutableList();val result=mutableListOf<Int>()
  while(rest.isNotEmpty()){
   val next=rest.firstOrNull{free(it,rest,level.size)}?:return null
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
class Generator{
 fun generate(stage:Int,seed:Long):Level{
  val random=Random(seed);val size=15+stage.coerceAtMost(8)*2
  val arrows=mutableListOf<Arrow>();val count=(stage+3).coerceAtMost(15)
  repeat(count){id->
   var best:Arrow?=null;var score=-1
   repeat(600){
    val dir=Dir.entries.random(random);val length=random.nextInt(2,5)
    val sx=random.nextInt(size);val sy=random.nextInt(size)
    // The final segment always points in the declared exit direction.
    val a=if(stage>=2 && random.nextInt(100)<65){
     val middleDir=if(dir.x!=0) if(random.nextBoolean())Dir.UP else Dir.DOWN else if(random.nextBoolean())Dir.LEFT else Dir.RIGHT
     val firstLen=random.nextInt(2,5)
     val turn=Pt(sx+firstLen*middleDir.x,sy+firstLen*middleDir.y)
     val tip=Pt(turn.x+(length-1)*dir.x,turn.y+(length-1)*dir.y)
     Arrow(id,sx,sy,length,dir,listOf(turn,tip))
    } else Arrow(id,sx,sy,length,dir)
    if(a.cells().size!=a.vertices().zipWithNext().sumOf{(p,q)->kotlin.math.abs(p.x-q.x)+kotlin.math.abs(p.y-q.y)}+1)return@repeat
    if(a.cells().any{p->p.x !in 0 until size||p.y !in 0 until size||arrows.any{p in it.cells()}})return@repeat
    if(!Rules.free(a,arrows+a,size))return@repeat
    val blocked=arrows.count{Rules.blockers(it,listOf(it,a),size).isNotEmpty()}
    val s=blocked*10+random.nextInt(5) + if(a.bends.isNotEmpty()) 2 else 0
    if(s>score){best=a;score=s}
   }
   arrows.add(best?:error("No placement for arrow "+id))
  }
  return Level(seed,stage,size,arrows).also{check(Rules.solve(it)!=null)}
 }
}
class Game(val level:Level,val maxLives:Int=5,val maxHints:Int=3){
 val remaining=level.arrows.toMutableList()
 val mistakes=mutableMapOf<Int,Set<Int>>()
 var lives=maxLives;private set
 var hints=maxHints;private set
 val won get()=remaining.isEmpty()
 val lost get()=lives==0
 fun tap(id:Int):Boolean{
  if(lost)return false
  val a=remaining.firstOrNull{it.id==id}?:return false
  val blocked=Rules.blockers(a,remaining,level.size)
  if(blocked.isEmpty()){remaining.remove(a);mistakes.remove(id);return true}
  if(mistakes[id]!=blocked){mistakes[id]=blocked;lives=(lives-1).coerceAtLeast(0)}
  return false
 }
 fun hint():Int?{
  if(hints==0)return null
  val id=remaining.firstOrNull{Rules.free(it,remaining,level.size)}?.id?:return null
  hints--;return id
 }
 fun retry(){remaining.clear();remaining.addAll(level.arrows);mistakes.clear();lives=maxLives;hints=maxHints}
}
data class Progress(val stage:Int=1,val done:Int=0){
 fun required()=when(stage){1,2->3;3->4;4->5;5->6;6->7;7->8;8->9;else->10}
 fun win():Progress=if(done+1>=required())Progress(stage+1,0)else copy(done=done+1)
 fun easier()=Progress((stage-1).coerceAtLeast(1),0)
}
