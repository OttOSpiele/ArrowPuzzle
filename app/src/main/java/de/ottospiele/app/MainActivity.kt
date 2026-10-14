package de.ottospiele.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.DrawScope

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
 var animId by remember{mutableStateOf<Int?>(null)}
 var animStep by remember{mutableIntStateOf(0)}
 var animPlan by remember{mutableStateOf<MovePlan?>(null)}
 var animFailed by remember{mutableStateOf(false)}
 var pulsePoint by remember{mutableStateOf<Pt?>(null)}
 var traveledCells by remember{mutableStateOf(emptySet<Pt>())}
 val occupiedEver=remember(game.level){game.level.arrows.flatMap{it.cells()}.toSet()}
 LaunchedEffect(animPlan){
  val plan=animPlan?:return@LaunchedEffect
  val total=plan.allowedSteps
  for(step in 1..total){delay(58);animStep=step;pulsePoint=plan.travel.getOrNull(step-1)}
  if(animFailed){
   delay(130)
   for(step in total-1 downTo 0){delay(30);animStep=step;pulsePoint=plan.travel.getOrNull(step-1)}
   if(game.lost)dialog="Alle Herzen verbraucht"
  }else{
   delay(90)
   traveledCells=traveledCells+plan.travel.filter{it.x in 0 until game.level.size && it.y in 0 until game.level.size}
   game.tap(animId!!)
   revision++
   if(game.won)dialog="Gewonnen!"
  }
  animId=null;animPlan=null;animStep=0;pulsePoint=null
 }

 fun replace(stage:Int){game=fresh(stage,lives,hints);highlighted=null;invalid=null;animId=null;animPlan=null;traveledCells=emptySet();pulsePoint=null;revision++}
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
  Board(game.level,game.remaining.map{it.id}.toSet(),highlighted,invalid,animId,animPlan,animStep,animFailed,occupiedEver,traveledCells,pulsePoint,Modifier.weight(1f).fillMaxWidth()){
   if(dialog.isEmpty() && animPlan==null){
    val arrow=game.remaining.firstOrNull{a->a.id==it}?:return@Board
    val plan=Rules.plan(arrow,game.level,game.remaining.map{a->a.id}.toSet())
    animId=it;animPlan=plan;animStep=0;animFailed=!plan.canExit
    invalid=null;highlighted=null;pulsePoint=null
    if(!plan.canExit){game.tap(it);revision++}
   }
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
   Button(onClick={highlighted=game.hint();invalid=null;revision++},enabled=game.hints!=0 && animPlan==null,
    modifier=Modifier.weight(1f).height(52.dp),shape=RoundedCornerShape(18.dp),
    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF7264D6))){
    Text("💡  Tipp",fontWeight=FontWeight.SemiBold)
   }
   FilledTonalButton(onClick={game.retry();revision++;highlighted=null;invalid=null},enabled=animPlan==null,
    modifier=Modifier.weight(1f).height(52.dp),shape=RoundedCornerShape(18.dp),
    colors=ButtonDefaults.filledTonalButtonColors(containerColor=Color.White,contentColor=Color(0xFF565E91))){
    Text("↻  Neustart",fontWeight=FontWeight.SemiBold)
   }
  }
  TextButton(onClick={dialog="Anderes Rätsel?"},enabled=animPlan==null,modifier=Modifier.fillMaxWidth()){
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
   Text("Einstellungen gelten für neue Rätsel.")
  }},
  confirmButton={TextButton(onClick={settings=false}){Text("OK")}}
 )
}
private fun fresh(stage:Int,lives:Int,hints:Int):Game{
 repeat(15){try{return Game(Generator().generate(stage,Random.nextLong()),lives,hints)}catch(_:Exception){}}
 error("Cannot generate a valid level")
}
@Composable private fun Board(level:Level,remaining:Set<Int>,hinted:Int?,invalid:Int?,animId:Int?,plan:MovePlan?,animStep:Int,failed:Boolean,ever:Set<Pt>,traveled:Set<Pt>,pulse:Pt?,modifier:Modifier,onTap:(Int)->Unit){
 val arrows=level.arrows.filter{it.id in remaining}
 Canvas(modifier.background(Brush.verticalGradient(listOf(Color.White,Color(0xFFF9FBFF))),RoundedCornerShape(26.dp))
  .pointerInput(level,remaining,animId){
   detectTapGestures{position->
    if(animId!=null)return@detectTapGestures
    val cell=minOf(size.width.toFloat(),size.height.toFloat())/level.size
    val ox=(size.width-cell*level.size)/2f
    val oy=(size.height-cell*level.size)/2f
    val x=(position.x-ox)/cell-.5f
    val y=(position.y-oy)/cell-.5f
    val selected=arrows.minByOrNull{a->
     a.cells().minOf{p->(p.x-x)*(p.x-x)+(p.y-y)*(p.y-y)}
    }
    if(selected!=null){
     val distance=selected.cells().minOf{p->(p.x-x)*(p.x-x)+(p.y-y)*(p.y-y)}
     if(distance<1.2f)onTap(selected.id)
    }
   }
  }.padding(8.dp)){
  val cell=minOf(size.width,size.height)/level.size
  val ox=(size.width-cell*level.size)/2f;val oy=(size.height-cell*level.size)/2f
  val currentTrail=if(plan!=null)plan.travel.take(animStep.coerceAtMost(plan.travel.size)).filter{it.x in 0 until level.size && it.y in 0 until level.size}.toSet() else emptySet()
  (ever+traveled+currentTrail).forEach{p->
   val point=Offset(ox+(p.x+.5f)*cell,oy+(p.y+.5f)*cell)
   if(p==pulse){
    drawCircle(Color(0xFF9189EF).copy(alpha=.18f),radius=(cell*.42f).coerceAtLeast(4f),center=point)
    drawCircle(Color(0xFF9189EF).copy(alpha=.35f),radius=(cell*.25f).coerceAtLeast(2.5f),center=point)
   }
   drawCircle(if(p==pulse)Color(0xFF6F63DE) else if(p in currentTrail)Color(0xFFB1B7F2) else Color(0xFFD9E0F2),
    radius=(cell*(if(p==pulse).14f else .08f)).coerceAtLeast(1.3f),center=point)
  }
  arrows.forEach{a->
   val points=if(a.id==animId&&plan!=null)plan.frame(animStep) else a.body()
   if(points.isEmpty())return@forEach
   val vertices=points.map{Offset(ox+(it.x+.5f)*cell,oy+(it.y+.5f)*cell)}
   val tip=vertices.last()
   val color=if(a.id==animId)if(failed)Color(0xFFE85161)else Color(0xFF7468EB) else when(a.id){hinted->Color(0xFF21A679);invalid->Color(0xFFEF7658);else->Color(0xFF28385D)}
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
