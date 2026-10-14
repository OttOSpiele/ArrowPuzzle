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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.ottospiele.arrow.*
import kotlin.math.abs
import kotlin.random.Random

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
 var dialog by remember{mutableStateOf("")}
 var settings by remember{mutableStateOf(false)}
 fun replace(stage:Int){game=fresh(stage,lives,hints);highlighted=null;revision++}
 Column(Modifier.fillMaxSize().background(Color(0xFFFFF8EB)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
   Text("OttOSpiele",fontSize=25.sp)
   TextButton(onClick={settings=true}){Text("⚙")}
  }
  Text("Stufe ${progress.stage} · ★ ${progress.done}/${progress.required()}")
  LinearProgressIndicator(progress={progress.done.toFloat()/progress.required()},modifier=Modifier.fillMaxWidth())
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
   Text("♥ ${game.lives}",fontSize=24.sp)
   Text("💡 ${game.hints}",fontSize=24.sp)
  }
  val redraw=revision
  Board(game.level,game.remaining.map{it.id}.toSet(),highlighted,Modifier.weight(1f).fillMaxWidth()){
   if(dialog.isEmpty()){
    val removed=game.tap(it);revision++;highlighted=if(removed)null else it
    if(game.won)dialog="Gewonnen!" else if(game.lost)dialog="Alle Herzen verbraucht"
   }
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){
   Button(onClick={highlighted=game.hint();revision++},enabled=game.hints!=0){Text("💡 Tipp")}
   OutlinedButton(onClick={game.retry();revision++;highlighted=null}){Text("Neustart")}
  }
  OutlinedButton(onClick={dialog="Anderes Rätsel?"},modifier=Modifier.fillMaxWidth()){Text("Anderes Rätsel (leichter)")}
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
    "Alle Herzen verbraucht"->{game.retry();revision++;highlighted=null}
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
@Composable private fun Board(level:Level,remaining:Set<Int>,hinted:Int?,modifier:Modifier,onTap:(Int)->Unit){
 val arrows=level.arrows.filter{it.id in remaining}
 Canvas(modifier.background(Color.White,RoundedCornerShape(18.dp))
  .pointerInput(level,remaining){
   detectTapGestures{position->
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
  arrows.forEach{a->
   val vertices=a.vertices().map{Offset(ox+(it.x+.5f)*cell,oy+(it.y+.5f)*cell)}
   val tip=vertices.last()
   val color=if(a.id==hinted)Color(0xFFEF7658)else Color(0xFF24677D)
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
