package de.ottospiele.arrow
import kotlin.random.Random
data class Pt(val x:Int,val y:Int)
enum class Dir(val x:Int,val y:Int){UP(0,-1),RIGHT(1,0),DOWN(0,1),LEFT(-1,0)}
data class Arrow(val id:Int,val x:Int,val y:Int,val length:Int,val dir:Dir){
 fun cells()=(0 until length).map{Pt(x+it*dir.x,y+it*dir.y)}.toSet()
}
data class Level(val seed:Long,val stage:Int,val size:Int,val arrows:List<Arrow>)
object Rules{
 fun blockers(a:Arrow,remaining:List<Arrow>,size:Int):Set<Int>{
  val other=remaining.filter{it.id!=a.id}.associate{it.id to it.cells()}
  val hit=mutableSetOf<Int>();var cells=a.cells()
  repeat(size+1){
   cells=cells.map{Pt(it.x+a.dir.x,it.y+a.dir.y)}.toSet()
   other.forEach{(id,occupied)->if(cells.any{it in occupied})hit.add(id)}
   if(cells.none{it.x in 0 until size&&it.y in 0 until size})return hit
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
class Generator{
 fun generate(stage:Int,seed:Long):Level{
  val random=Random(seed);val size=15+stage.coerceAtMost(8)*2
  val arrows=mutableListOf<Arrow>();val count=(stage+3).coerceAtMost(15)
  repeat(count){id->
   var best:Arrow?=null;var score=-1
   repeat(600){
    val dir=Dir.entries.random(random);val length=random.nextInt(2,5)
    val a=Arrow(id,random.nextInt(size),random.nextInt(size),length,dir)
    if(a.cells().any{p->p.x !in 0 until size||p.y !in 0 until size||arrows.any{p in it.cells()}})return@repeat
    if(!Rules.free(a,arrows+a,size))return@repeat
    val blocked=arrows.count{Rules.blockers(it,listOf(it,a),size).isNotEmpty()}
    val s=blocked*10+random.nextInt(5)
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
