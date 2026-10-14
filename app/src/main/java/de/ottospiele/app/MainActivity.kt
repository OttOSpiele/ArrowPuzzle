package de.ottospiele.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.ottospiele.arrow.*
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.compose.ui.graphics.drawscope.DrawScope

private data class Flight(val id:Int,val plan:MovePlan,val failed:Boolean,val step:Int=0)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState);setContent{MaterialTheme{Puzzle()}}
 }
}
@Composable fun Puzzle(){
 var progress by remember{mutableStateOf(Progress())}
 var lives by remember{mutableIntStateOf(5)}
 var hints by remember{mutableIntStateOf(3)}
 var game by remember{mutableStateOf(fresh(1,5,3))}
 var revision by remember{mutableIntStateOf(0)}
 var highlighted by remember{mutableStateOf<Int?>(null)}
 var invalid by remember{mutableStateOf<Int?>(null)}
 var dialog by remember{mutableStateOf("")}
 var settings by remember{mutableStateOf(false)}
 var concurrent by remember{mutableStateOf(true)}
 var speed by remember{mutableIntStateOf(1)}
 val flights=remember{mutableStateListOf<Flight>()}
 val scope=rememberCoroutineScope()
 val jobs=remember{mutableMapOf<Int,Job>()}
 var traveledCells by remember{mutableStateOf(emptySet<Pt>())}
 val occupiedEver=remember(game.level){game.level.arrows.flatMap{it.cells()}.toSet()}
 // 0 = already much faster than the old 58 ms per cell.
 val stepDelay=when(speed){0->58L;1->32L;2->20L;else->11L}
 fun stopFlights(){
  jobs.values.forEach{it.cancel()}
  jobs.clear();flights.clear()
 }
 fun replace(stage:Int){stopFlights();game=fresh(stage,lives,hints);highlighted=null;invalid=null;traveledCells=emptySet();revision++}
 Column(
  Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF4F2FD),Color(0xFFEDF4FF),Color(0xFFFFFBF5))))
   .padding(horizontal=20.dp,vertical=14.dp),
  verticalArrangement=Arrangement.spacedBy(14.dp)
 ){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
   Column {
    Text("OttOSpiele",fontSize=27.sp,fontWeight=FontWeight.Bold,color=Color(0xFF24294B))
    Text("ARROW PUZZLE",fontSize=11.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFF7E84A6),letterSpacing=2.sp)
   }
   FilledTonalIconButton(onClick={settings=true},colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=Color.White)){
    Text("⚙",fontSize=22.sp,color=Color(0xFF5662A7))
   }
  }
  Surface(shape=RoundedCornerShape(22.dp),color=Color.White.copy(alpha=.90f),shadowElevation=2.dp){
   Column(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
     Text("Stufe ${progress.stage}",fontWeight=FontWeight.Bold,fontSize=20.sp,color=Color(0xFF292D55))
     Text("★  ${progress.done} / ${progress.required()}",fontWeight=FontWeight.SemiBold,color=Color(0xFF7364C7))
    }
    LinearProgressIndicator(progress={progress.done.toFloat()/progress.required()},modifier=Modifier.fillMaxWidth().height(7.dp),
     color=Color(0xFF7464D7),trackColor=Color(0xFFE7E6F8))
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
     Text("❤️  ${game.lives}  Herzen",fontWeight=FontWeight.Medium,color=Color(0xFFCE5575))
     Text("💡  ${game.hints}  Tipps",fontWeight=FontWeight.Medium,color=Color(0xFF5F66A9))
    }
   }
  }
  Board(game.level,game.remaining.map{it.id}.toSet(),highlighted,invalid,
   flights.toList(),occupiedEver,traveledCells,Modifier.weight(1f).fillMaxWidth()){
   if(dialog.isNotEmpty() || game.lost || (!concurrent && flights.isNotEmpty()) || flights.any{f->f.id==it})return@Board
   val arrow=game.remaining.firstOrNull{a->a.id==it}?:return@Board
   val plan=Rules.plan(arrow,game.level,game.remaining.map{a->a.id}.toSet())
   val failed=!plan.canExit
   highlighted=null;invalid=null
   // Apply success to game immediately: subsequently tapped arrows see updated blockers.
   if(!failed){game.tap(it);revision++}
   else{game.tap(it);revision++}
   flights.add(Flight(it,plan,failed))
   val levelAtStart=game.level
   jobs[it]=scope.launch {
    val duration=stepDelay
    for(step in 1..plan.allowedSteps){
     delay(duration)
     val index=flights.indexOfFirst{f->f.id==it}
     if(index<0)return@launch
     flights[index]=flights[index].copy(step=step)
    }
    if(failed){
     delay(75)
     for(step in plan.allowedSteps-1 downTo 0){
      delay((duration/2).coerceAtLeast(5L))
      val index=flights.indexOfFirst{f->f.id==it}
      if(index<0)return@launch
      flights[index]=flights[index].copy(step=step)
     }
    }else{
     traveledCells=traveledCells+plan.travel.filter{c->c.x in 0 until levelAtStart.size && c.y in 0 until levelAtStart.size}
    }
    flights.removeAll{f->f.id==it}
    jobs.remove(it)
    if(flights.isEmpty()){
     if(game.won)dialog="Gewonnen!"
     else if(game.lost)dialog="Alle Herzen verbraucht"
    }
   }
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
   Button(onClick={highlighted=game.hint();invalid=null;revision++},enabled=game.hints!=0 && !game.lost && !game.won,
    modifier=Modifier.weight(1f).height(52.dp),shape=RoundedCornerShape(18.dp),
    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF7264D6))){
    Text("💡  Tipp",fontWeight=FontWeight.SemiBold)
   }
   FilledTonalButton(onClick={stopFlights();game.retry();traveledCells=emptySet();revision++;highlighted=null;invalid=null},
    modifier=Modifier.weight(1f).height(52.dp),shape=RoundedCornerShape(18.dp),
    colors=ButtonDefaults.filledTonalButtonColors(containerColor=Color.White,contentColor=Color(0xFF565E91))){
    Text("↻  Neustart",fontWeight=FontWeight.SemiBold)
   }
  }
  TextButton(onClick={stopFlights();dialog="Anderes Rätsel?"},modifier=Modifier.fillMaxWidth()){
   Text("Anderes Rätsel  ·  eine Stufe leichter",color=Color(0xFF646998))
  }
 }
 if(dialog.isNotEmpty())AlertDialog(
  onDismissRequest={},
  title={Text(dialog)},
  text={Text(when(dialog){
   "Gewonnen!"->"Toll gemacht! Noch ein Rätsel?"
   "Alle Herzen verbraucht"->"Nicht schlimm. Probiere dasselbe Rätsel erneut!"
   else->"Ein neues, etwas leichteres Rätsel starten?"
  })},
  confirmButton={TextButton(onClick={
   when(dialog){
    "Gewonnen!"->{progress=progress.win();replace(progress.stage)}
    "Alle Herzen verbraucht"->{game.retry();revision++;highlighted=null;invalid=null}
    else->{progress=progress.easier();replace(progress.stage)}
   }
   dialog=""
  }){Text("Weiter")}},
  dismissButton={if(dialog=="Anderes Rätsel?")TextButton(onClick={dialog=""}){Text("Abbrechen")}}
 )
 if(settings)AlertDialog(
  onDismissRequest={settings=false},
  title={Text("Spieleinstellungen")},
  text={Column{
   Text("Herzen pro Level")
   Row{listOf(3,5,10).forEach{n->TextButton(onClick={lives=n}){Text(if(lives==n)"[$n]"else"$n")}}}
   Text("Tipps pro Level")
   Row{listOf(0,1,3,5).forEach{n->TextButton(onClick={hints=n}){Text(if(hints==n)"[$n]"else"$n")}}}
   Text("Parallele Pfeile")
   Row(verticalAlignment=Alignment.CenterVertically){
    Text("Nächsten Pfeil sofort antippen",modifier=Modifier.weight(1f))
    Switch(checked=concurrent,onCheckedChange={concurrent=it})
   }
   Text("Ausfahrgeschwindigkeit")
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
    listOf("Ruhig","Normal","Flott","Turbo").forEachIndexed{index,label->
     FilterChip(selected=speed==index,onClick={speed=index},label={Text(label)})
    }
   }
   Text("Herzen und Tipps gelten ab dem nächsten Rätsel. Animationseinstellungen sofort.")
  }},
  confirmButton={TextButton(onClick={settings=false}){Text("OK")}}
 )
}
private fun fresh(stage:Int,lives:Int,hints:Int):Game{
 repeat(15){try{return Game(Generator().generate(stage,Random.nextLong()),lives,hints)}catch(_:Exception){}}
 error("Cannot generate a valid level")
}
@Composable private fun Board(level:Level,remaining:Set<Int>,hinted:Int?,invalid:Int?,flights:List<Flight>,ever:Set<Pt>,traveled:Set<Pt>,modifier:Modifier,onTap:(Int)->Unit){
 val arrows=level.arrows.filter{it.id in remaining}
 Canvas(modifier.clip(RoundedCornerShape(26.dp)).background(Brush.verticalGradient(listOf(Color.White,Color(0xFFF9FBFF))))
  .pointerInput(level,remaining,flights.map{it.id}){
   detectTapGestures{position->
    val sx=size.width.toFloat()/level.size
    val sy=size.height.toFloat()/level.size
    val x=position.x/sx-.5f
    val y=position.y/sy-.5f
    val selected=arrows.minByOrNull{a->
     a.cells().minOf{p->(p.x-x)*(p.x-x)+(p.y-y)*(p.y-y)}
    }
    if(selected!=null){
     val distance=selected.cells().minOf{p->(p.x-x)*(p.x-x)+(p.y-y)*(p.y-y)}
     if(distance<1.2f)onTap(selected.id)
    }
   }
  }){
  val sx=size.width/level.size;val sy=size.height/level.size
  val cell=minOf(sx,sy)
  val ox=0f;val oy=0f
  val currentTrail=flights.flatMap{f->f.plan.travel.take(f.step)}.filter{it.x in 0 until level.size && it.y in 0 until level.size}.toSet()
  val pulse=flights.mapNotNull{f->f.plan.travel.getOrNull(f.step-1)}.toSet()
  (ever+traveled+currentTrail).forEach{p->
   val point=Offset(ox+(p.x+.5f)*sx,oy+(p.y+.5f)*sy)
   if(p in pulse){
    drawCircle(Color(0xFF9189EF).copy(alpha=.18f),radius=(cell*.42f).coerceAtLeast(4f),center=point)
    drawCircle(Color(0xFF9189EF).copy(alpha=.35f),radius=(cell*.25f).coerceAtLeast(2.5f),center=point)
   }
   drawCircle(if(p in pulse)Color(0xFF6F63DE) else if(p in currentTrail)Color(0xFFB1B7F2) else Color(0xFFD9E0F2),
    radius=(cell*(if(p in pulse).14f else .08f)).coerceAtLeast(1.3f),center=point)
  }
  val allVisible=arrows.filter{a->flights.none{f->f.id==a.id}}.map{it to (it.body() to false)}+flights.mapNotNull{f->level.arrows.firstOrNull{it.id==f.id}?.let{a->a to (f.plan.frame(f.step) to f.failed)}}
  allVisible.forEach{(a,shape)->
   val points=shape.first
   if(points.isEmpty())return@forEach
   val vertices=points.map{Offset(ox+(it.x+.5f)*sx,oy+(it.y+.5f)*sy)}
   val tip=vertices.last()
   val color=if(flights.any{it.id==a.id})if(shape.second)Color(0xFFE85161)else Color(0xFF7468EB) else when(a.id){hinted->Color(0xFF21A679);invalid->Color(0xFFEF7658);else->Color(0xFF28385D)}
   val stroke=(cell*.20f).coerceAtLeast(2.5f);val wing=cell*.42f
   vertices.zipWithNext().forEach{(from,to)->drawLine(color,from,to,stroke,cap=StrokeCap.Round)}
   if(a.dir.x!=0){
    drawLine(color,tip,Offset(tip.x-a.dir.x*wing,tip.y-wing*.7f),stroke,cap=StrokeCap.Round)
    drawLine(color,tip,Offset(tip.x-a.dir.x*wing,tip.y+wing*.7f),stroke,cap=StrokeCap.Round)
   }else{
    drawLine(color,tip,Offset(tip.x-wing*.7f,tip.y-a.dir.y*wing),stroke,cap=StrokeCap.Round)
    drawLine(color,tip,Offset(tip.x+wing*.7f,tip.y-a.dir.y*wing),stroke,cap=StrokeCap.Round)
   }
  }
 }
}
