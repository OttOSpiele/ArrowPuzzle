// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.app

import android.os.Bundle
import android.util.Log
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.drawscope.DrawScope

private data class Flight(val id:Int,val plan:MovePlan,val failed:Boolean,val step:Int=0)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState);setContent{MaterialTheme{Puzzle()}}
 }
}
@Composable fun Puzzle(){
 val context=LocalContext.current
 val screenAspect=remember{
  val metrics=context.resources.displayMetrics
  metrics.widthPixels.toFloat()/metrics.heightPixels.coerceAtLeast(1)
 }
 val defaults=remember(context){GameDefaults.load(context)}
 val previewHoldMillis=defaults.previewHoldMillis
 val canAdjustStage=defaults.showStageAdjustment &&
  (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
 val saved=remember{LocalSave.read(context)}
 var progress by remember{mutableStateOf(saved?.progress?:Progress())}
 var lives by remember{mutableIntStateOf(saved?.lifeSetting?:defaults.lives)}
 var hints by remember{mutableIntStateOf(saved?.hintSetting?:defaults.hints)}
 var game by remember{mutableStateOf(saved?.game?:fresh(1,defaults.lives,defaults.hints,screenAspect))}
 var revision by remember{mutableIntStateOf(0)}
 var highlighted by remember{mutableStateOf<Int?>(null)}
 var invalid by remember{mutableStateOf<Int?>(null)}
 var dialog by remember{mutableStateOf("")}
 var winScreen by remember{mutableStateOf(saved?.win?:false)}
 var defeatScreen by remember{mutableStateOf(saved?.defeat?:false)}
 var defeatMode by remember{mutableStateOf("choice")}
 var defeatPrepared by remember{mutableStateOf<Game?>(null)}
 var defeatAttempt by remember{mutableIntStateOf(0)}
 var nextGame by remember{mutableStateOf<Game?>(null)}
 var nextProgress by remember{mutableStateOf<Progress?>(null)}
 var prepareError by remember{mutableStateOf(false)}
 var preparationAttempt by remember{mutableIntStateOf(0)}
 var prepDone by remember{mutableIntStateOf(0)}
 var prepTotal by remember{mutableIntStateOf(2)}
 var prepStage by remember{mutableIntStateOf(0)}
 var settings by remember{mutableStateOf(false)}
 var menuOpen by remember{mutableStateOf(false)}
 var aboutOpen by remember{mutableStateOf(false)}
 var stageInput by remember{mutableStateOf("")}
 var stageLoading by remember{mutableStateOf(false)}
  var backLoading by remember{mutableStateOf(false)}
 var stageError by remember{mutableStateOf(false)}
 var concurrent by remember{mutableStateOf(saved?.concurrent?:defaults.concurrentArrows)}
 var speed by remember{mutableIntStateOf(saved?.speed?:defaults.animationSpeed)}
 val flights=remember{mutableStateListOf<Flight>()}
 val scope=rememberCoroutineScope()
 val jobs=remember{mutableMapOf<Int,Job>()}
 var traveledCells by remember{mutableStateOf(emptySet<Pt>())}
 val occupiedEver=remember(game.level){game.level.arrows.flatMap{it.cells()}.toSet()}
 // 0 = already much faster than the old 58 ms per cell.
 val stepDelay=when(speed){0->58L;1->32L;2->20L;else->11L}
 LaunchedEffect(progress,lives,hints,concurrent,speed,game,revision,winScreen,defeatScreen){
  LocalSave.write(context,progress,lives,hints,concurrent,speed,game,winScreen,defeatScreen)
 }
 fun stopFlights(){
  jobs.values.forEach{it.cancel()}
  jobs.clear();flights.clear()
 }
 fun replaceWith(ready:Game){stopFlights();game=ready;highlighted=null;invalid=null;traveledCells=emptySet();revision++}
 // Cooperative precomputation. Only saved and solver-validated levels count
 // toward the progress bar; search backtracking itself has unknown duration.
 LaunchedEffect(progress.stage,progress.done,screenAspect) {
  val target=progress.win().stage
  prepStage=target
  prepTotal=2
  val existing=withContext(Dispatchers.IO) {
   PreparedLevels.read(context,target,screenAspect)
  }.filter { it.seed!=game.level.seed }.take(2).toMutableList()
  prepDone=existing.size
  for(index in existing.size until prepTotal) {
   val candidate=withContext(Dispatchers.Default) {
    // Prefer the best valid level of two candidates, within a fixed
    // per-slot effort budget, not an unbounded quality search.
    val candidates=(0 until 2).mapNotNull { attempt ->
     runCatching {
      val seed=target.toLong()*1000003L + progress.done*104729L + index*733L + attempt*41L
      generateMeasured(target,seed,screenAspect,"preparation")
     }.getOrNull()
    }
    candidates.filter { Rules.solve(it)!=null }
     .let { DifficultyProfiles.choose(target,it) }
   } ?: break
   existing.add(candidate)
   withContext(Dispatchers.IO) {
    PreparedLevels.write(context,target,screenAspect,existing)
   }
   prepDone=existing.size
  }
 }
 LaunchedEffect(winScreen,preparationAttempt){
  if(!winScreen)return@LaunchedEffect
  nextGame=null
  nextProgress=null
  prepareError=false
  // Give the celebration a perceptible opening, even on fast hardware.
  delay(450)
  try {
   val target=progress.win()
   val ready=withContext(Dispatchers.Default){
    val prepared=PreparedLevels.take(context,target.stage,screenAspect,game.level.seed)
    if(prepared!=null)Game(prepared,lives,hints)
    else fresh(target.stage,lives,hints,screenAspect)
   }
   nextProgress=target
   nextGame=ready
  }catch(_:Exception){prepareError=true}
 }

 LaunchedEffect(defeatScreen,defeatMode,defeatAttempt){
  if(!defeatScreen || defeatMode!="loading")return@LaunchedEffect
  defeatPrepared=null
  delay(350)
  try{
   val ready=withContext(Dispatchers.Default){fresh(progress.stage,lives,hints,screenAspect)}
   defeatPrepared=ready
   defeatMode="ready"
  }catch(_:Exception){defeatMode="error"}
 }

 if(winScreen){
  WinScreen(
   stage=progress.stage,
   nextStage=nextProgress?.stage,
   ready=nextGame!=null,
   failed=prepareError,
   onRetry={preparationAttempt++},
   onContinue={
    val next=nextGame
    val nextP=nextProgress
    if(next!=null && nextP!=null){
     stopFlights()
     progress=nextP
     game=next
     highlighted=null;invalid=null;traveledCells=emptySet();revision++
     winScreen=false
     nextGame=null;nextProgress=null
    }
   }
  )
 }else if(defeatScreen){
  DefeatScreen(
   stage=progress.stage,
   mode=defeatMode,
   onAgain={
    stopFlights()
    game.retry()
    traveledCells=emptySet();highlighted=null;invalid=null;revision++
    defeatScreen=false;defeatMode="choice";defeatPrepared=null
   },
   onNew={
    defeatPrepared=null
    defeatMode="loading"
   },
   onRetryGeneration={
    defeatAttempt++
    defeatMode="loading"
   },
   onContinue={
    val ready=defeatPrepared
    if(ready!=null){
     stopFlights()
     progress=progress.copy(done=0)
     game=ready
     traveledCells=emptySet();highlighted=null;invalid=null;revision++
     defeatScreen=false;defeatMode="choice";defeatPrepared=null
    }
   }
  )
 }else Column(
  Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF4F2FD),Color(0xFFEDF4FF),Color(0xFFFFFBF5))))
   .safeDrawingPadding().padding(horizontal=20.dp,vertical=14.dp),
  verticalArrangement=Arrangement.spacedBy(14.dp)
 ){
  BoxWithConstraints(Modifier.fillMaxWidth()) {
   if(maxWidth<650.dp) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
      horizontalArrangement=Arrangement.spacedBy(10.dp)) {
   Box {
    FilledTonalIconButton(
     onClick={menuOpen=true},
     modifier=Modifier.size(56.dp),
     colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=Color.White)){
     Text("☰",fontSize=27.sp,color=Color(0xFF5662A7))
    }
    DropdownMenu(expanded=menuOpen,onDismissRequest={menuOpen=false}){
     DropdownMenuItem(text={Text("⚙  Einstellungen")},onClick={menuOpen=false;settings=true})
     if(canAdjustStage)DropdownMenuItem(text={Text("↕  Stufe anpassen")},onClick={
      menuOpen=false;stageInput=progress.stage.toString();stageError=false;dialog="stufeAnpassen"
     })
     DropdownMenuItem(text={Text("ⓘ  Über das Spiel")},onClick={menuOpen=false;aboutOpen=true})
    }
   }
   Column(Modifier.weight(1f)) {
    Text("OttOSpiele",fontSize=25.sp,fontWeight=FontWeight.Bold,color=Color(0xFF24294B),maxLines=1)
    Text("ARROW PUZZLE",fontSize=11.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFF7E84A6),letterSpacing=2.sp,maxLines=1)
   }
     }
     Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
      horizontalArrangement=Arrangement.spacedBy(6.dp)) {
   Button(
    enabled=!game.won && !backLoading,
     onClick={stopFlights();game.retry();traveledCells=emptySet();revision++;highlighted=null;invalid=null},
    modifier=Modifier.weight(1f).height(58.dp),shape=RoundedCornerShape(17.dp),
    contentPadding=PaddingValues(horizontal=4.dp),
    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2469D8),contentColor=Color.White)){
    Column(horizontalAlignment=Alignment.CenterHorizontally){
     Text("↻",fontSize=22.sp,fontWeight=FontWeight.Bold)
     Text("Neustart",fontSize=11.sp,fontWeight=FontWeight.Bold)
    }
   }
   Button(
    enabled=!game.won && !backLoading,
     onClick={dialog="stufeZurueck"},
    modifier=Modifier.weight(1f).height(58.dp),shape=RoundedCornerShape(17.dp),
    contentPadding=PaddingValues(horizontal=4.dp),
    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF7955CE),contentColor=Color.White)){
    Column(horizontalAlignment=Alignment.CenterHorizontally){
     Text("↓",fontSize=22.sp,fontWeight=FontWeight.Bold)
     Text("Stufe zurück",fontSize=11.sp,fontWeight=FontWeight.Bold,maxLines=1)
    }
   }
   Button(
    onClick={highlighted=game.hint();invalid=null;revision++},
    enabled=game.hints!=0 && !game.lost && !game.won && !backLoading,
    modifier=Modifier.weight(1f).height(58.dp),shape=RoundedCornerShape(17.dp),
    contentPadding=PaddingValues(horizontal=4.dp),
    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF158B65),contentColor=Color.White)){
    Column(horizontalAlignment=Alignment.CenterHorizontally){
     Text("💡",fontSize=21.sp)
     Text("Tipp",fontSize=11.sp,fontWeight=FontWeight.Bold)
    }
   }
     }
    }
   } else {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
     horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   Box {
    FilledTonalIconButton(
     onClick={menuOpen=true},
     modifier=Modifier.size(56.dp),
     colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=Color.White)){
     Text("☰",fontSize=27.sp,color=Color(0xFF5662A7))
    }
    DropdownMenu(expanded=menuOpen,onDismissRequest={menuOpen=false}){
     DropdownMenuItem(text={Text("⚙  Einstellungen")},onClick={menuOpen=false;settings=true})
     if(canAdjustStage)DropdownMenuItem(text={Text("↕  Stufe anpassen")},onClick={
      menuOpen=false;stageInput=progress.stage.toString();stageError=false;dialog="stufeAnpassen"
     })
     DropdownMenuItem(text={Text("ⓘ  Über das Spiel")},onClick={menuOpen=false;aboutOpen=true})
    }
   }
   Column(Modifier.weight(1f)) {
    Text("OttOSpiele",fontSize=25.sp,fontWeight=FontWeight.Bold,color=Color(0xFF24294B),maxLines=1)
    Text("ARROW PUZZLE",fontSize=11.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFF7E84A6),letterSpacing=2.sp,maxLines=1)
   }
   Button(
    enabled=!game.won && !backLoading,
     onClick={stopFlights();game.retry();traveledCells=emptySet();revision++;highlighted=null;invalid=null},
    modifier=Modifier.width(122.dp).height(58.dp),shape=RoundedCornerShape(17.dp),
    contentPadding=PaddingValues(horizontal=4.dp),
    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2469D8),contentColor=Color.White)){
    Column(horizontalAlignment=Alignment.CenterHorizontally){
     Text("↻",fontSize=22.sp,fontWeight=FontWeight.Bold)
     Text("Neustart",fontSize=11.sp,fontWeight=FontWeight.Bold)
    }
   }
   Button(
    enabled=!game.won && !backLoading,
     onClick={dialog="stufeZurueck"},
    modifier=Modifier.width(136.dp).height(58.dp),shape=RoundedCornerShape(17.dp),
    contentPadding=PaddingValues(horizontal=4.dp),
    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF7955CE),contentColor=Color.White)){
    Column(horizontalAlignment=Alignment.CenterHorizontally){
     Text("↓",fontSize=22.sp,fontWeight=FontWeight.Bold)
     Text("Stufe zurück",fontSize=11.sp,fontWeight=FontWeight.Bold,maxLines=1)
    }
   }
   Button(
    onClick={highlighted=game.hint();invalid=null;revision++},
    enabled=game.hints!=0 && !game.lost && !game.won && !backLoading,
    modifier=Modifier.width(122.dp).height(58.dp),shape=RoundedCornerShape(17.dp),
    contentPadding=PaddingValues(horizontal=4.dp),
    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF158B65),contentColor=Color.White)){
    Column(horizontalAlignment=Alignment.CenterHorizontally){
     Text("💡",fontSize=21.sp)
     Text("Tipp",fontSize=11.sp,fontWeight=FontWeight.Bold)
    }
   }
    }
   }
  }

  Board(game.level,game.remaining.map{it.id}.toSet(),highlighted,invalid,
   flights.toList(),occupiedEver,traveledCells,Modifier.weight(1f).fillMaxWidth(),previewHoldMillis){
   if(dialog.isNotEmpty() || game.won || game.lost || (!concurrent && flights.isNotEmpty()) || flights.any{f->f.id==it})return@Board
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
     if(game.won)winScreen=true
     else if(game.lost){defeatMode="choice";defeatScreen=true}
    }
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
  // 4 dp preparation strip, placed above Android system navigation and
  // outside the game canvas so it never intercepts board touches.
  Box(Modifier.fillMaxWidth().height(4.dp)
   .background(Color(0xFFD6D9E9),RoundedCornerShape(2.dp))) {
   Box(Modifier.fillMaxWidth(
    if(prepStage==progress.win().stage) prepDone.toFloat()/prepTotal.coerceAtLeast(1)
    else 0f
   ).fillMaxHeight().background(Color.White,RoundedCornerShape(2.dp)))
  }

 }
 if(canAdjustStage && dialog=="stufeAnpassen")AlertDialog(
  onDismissRequest={if(!stageLoading)dialog=""},
  title={Text("Stufe anpassen (Test)")},
  text={
   Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
    Text("Wähle eine Stufe. Das aktuelle Rätsel und der Stufenfortschritt werden ersetzt.")
    OutlinedTextField(
     value=stageInput,
     onValueChange={value->if(!stageLoading){stageInput=value.filter{it.isDigit()}.take(4);stageError=false}},
     label={Text("Stufe (1–9999)")},
     singleLine=true,
     enabled=!stageLoading,
     keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),
     isError=stageInput.toIntOrNull()?.let { it in 1..9999 } != true
    )
    if(stageError)Text("Rätsel konnte nicht erstellt werden. Bitte erneut versuchen.",
     color=MaterialTheme.colorScheme.error)
    if(stageLoading)CircularProgressIndicator()
   }
  },
  confirmButton={
   Button(
    enabled=!stageLoading && stageInput.toIntOrNull()?.let { it in 1..9999 } == true,
    onClick={
     val selected=stageInput.toIntOrNull()?:return@Button
     stageLoading=true;stageError=false
     scope.launch {
      val ready=runCatching {withContext(Dispatchers.Default){
       fresh(selected,lives,hints,screenAspect)
      }}.getOrNull()
      stageLoading=false
      if(ready==null){stageError=true}else{
       stopFlights()
       progress=Progress(selected,0)
       game=ready
       highlighted=null;invalid=null;traveledCells=emptySet();revision++
       nextGame=null;nextProgress=null;defeatPrepared=null
       winScreen=false;defeatScreen=false;dialog=""
      }
     }
    }
   ){Text("Stufe starten")}
  },
  dismissButton={TextButton(enabled=!stageLoading,onClick={dialog=""}){Text("Abbrechen")}}
 )
 if(dialog=="stufeZurueckFehler")AlertDialog(onDismissRequest={dialog=""},confirmButton={TextButton(onClick={dialog=""}){Text("OK")}},title={Text("Rätsel konnte nicht erstellt werden")},text={Text("Bitte erneut versuchen.")})
 if(dialog=="stufeZurueck")AlertDialog(
  onDismissRequest={if(!backLoading)dialog=""},
  title={Text("Wirklich eine Stufe zurück?",fontWeight=FontWeight.Bold)},
  text={
   Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(14.dp)){
    Text("⭐  ↓  ⭐",fontSize=48.sp,color=Color(0xFF7955CE))
    Text("Du spielst gerade Stufe ${progress.stage}.",fontSize=18.sp)
    Text("Wenn du zurückgehst, startest du Stufe ${(progress.stage-1).coerceAtLeast(1)} mit einem neuen Rätsel. Dein Fortschritt in dieser Stufe geht verloren.",fontSize=16.sp)
    Text("Möchtest du lieber weiterspielen?",fontSize=16.sp,fontWeight=FontWeight.SemiBold)
   }
  },
  confirmButton={
   if(backLoading) CircularProgressIndicator(color=Color(0xFF7955CE))
   else Button(onClick={dialog=""},colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2469D8))){
    Text("Fortsetzen",fontWeight=FontWeight.Bold)
   }
  },
  dismissButton={
   if(!backLoading) Button(onClick={
    if(!backLoading){
     backLoading=true
     val target=progress.easier()
     scope.launch {
      val ready=runCatching { withContext(Dispatchers.Default) { fresh(target.stage,lives,hints,screenAspect) } }.getOrNull()
      if(ready!=null){ progress=target;replaceWith(ready);dialog="" }
      else dialog="stufeZurueckFehler"
      backLoading=false
     }
    }
   },colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF7955CE))){
    Text("Stufe zurück",fontWeight=FontWeight.Bold)
   }
  }
 )
 if(aboutOpen)AlertDialog(
  onDismissRequest={aboutOpen=false},
  title={Text("Über Arrow Puzzle")},
  text={Text("OttOSpiele – Arrow Puzzle\n\nEin Rätselspiel mit Pfeilen für Kinder. Finde die freien Pfeile und lasse sie aus dem Spielfeld fahren.\n\nOffline spielbar, ohne Werbung.")},
  confirmButton={TextButton(onClick={aboutOpen=false}){Text("Schließen")}}
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
@Composable
private fun DefeatScreen(
 stage:Int,
 mode:String,
 onAgain:()->Unit,
 onNew:()->Unit,
 onRetryGeneration:()->Unit,
 onContinue:()->Unit
){
 Column(
  Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFEFF2),Color(0xFFF6F3FF),Color(0xFFEDF6FF))))
   .safeDrawingPadding().padding(28.dp),
  horizontalAlignment=Alignment.CenterHorizontally,
  verticalArrangement=Arrangement.Center
 ){
  Text("♡  ✨  ♡",fontSize=47.sp,color=Color(0xFFE587A3))
  Spacer(Modifier.height(20.dp))
  Text("Fast geschafft!",fontSize=34.sp,fontWeight=FontWeight.ExtraBold,color=Color(0xFF34345D))
  Spacer(Modifier.height(12.dp))
  Text("Jedes Rätsel ist eine neue Chance.",fontSize=18.sp,color=Color(0xFF706D98))
  Spacer(Modifier.height(10.dp))
  Text("Du bleibst auf Stufe $stage.",color=Color(0xFF767DA2))
  Spacer(Modifier.height(44.dp))
  when(mode){
   "choice"->{
    Button(onClick=onAgain,modifier=Modifier.fillMaxWidth().height(60.dp),
     shape=RoundedCornerShape(20.dp),
     colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2469D8))){
     Text("↻  Nochmal versuchen",fontSize=19.sp,fontWeight=FontWeight.Bold)
    }
    Spacer(Modifier.height(14.dp))
    FilledTonalButton(onClick=onNew,modifier=Modifier.fillMaxWidth().height(60.dp),
     shape=RoundedCornerShape(20.dp)){
     Text("Neues Rätsel",fontSize=19.sp,fontWeight=FontWeight.SemiBold)
    }
    Spacer(Modifier.height(15.dp))
    Text("Neues Rätsel setzt deinen Stufenfortschritt auf 0.",
     fontSize=13.sp,color=Color(0xFF7C80A1))
   }
   "loading"->{
    CircularProgressIndicator(color=Color(0xFF2469D8))
    Spacer(Modifier.height(18.dp))
    Text("Ein neues Rätsel wird vorbereitet …",color=Color(0xFF666D96))
   }
   "ready"->{
    Text("Dein neues Rätsel ist bereit!",fontWeight=FontWeight.SemiBold,color=Color(0xFF306E9B))
    Spacer(Modifier.height(16.dp))
    Button(onClick=onContinue,modifier=Modifier.fillMaxWidth().height(60.dp),
     shape=RoundedCornerShape(20.dp),
     colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2469D8))){
     Text("Weiter  →",fontSize=20.sp,fontWeight=FontWeight.Bold)
    }
   }
   else->{
    Text("Das Rätsel konnte nicht vorbereitet werden.",color=Color(0xFFB14A64))
    Spacer(Modifier.height(15.dp))
    Button(onClick=onRetryGeneration){Text("Erneut versuchen")}
    TextButton(onClick=onAgain){Text("Dasselbe Rätsel wiederholen")}
   }
  }
 }
}

@Composable
private fun WinScreen(
 stage:Int,
 nextStage:Int?,
 ready:Boolean,
 failed:Boolean,
 onRetry:()->Unit,
 onContinue:()->Unit
){
 Column(
  Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFECEBFF),Color(0xFFF6FAFF),Color(0xFFFFF9EC))))
   .padding(28.dp),
  horizontalAlignment=Alignment.CenterHorizontally,
  verticalArrangement=Arrangement.Center
 ){
  Text("✦  ✨  ✦",fontSize=48.sp,color=Color(0xFF7770D8))
  Spacer(Modifier.height(22.dp))
  Text("Großartig!",fontSize=36.sp,fontWeight=FontWeight.ExtraBold,color=Color(0xFF2D3568))
  Spacer(Modifier.height(10.dp))
  Text("Rätsel geschafft!",fontSize=21.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFF615F9D))
  Spacer(Modifier.height(12.dp))
  Text("Stufe $stage gemeistert",color=Color(0xFF787FA4))
  Spacer(Modifier.height(48.dp))
  if(ready){
   Text(if(nextStage!=null && nextStage>stage)"Neue Stufe freigeschaltet!" else "Dein nächstes Rätsel ist bereit!",
    color=Color(0xFF3E699C),fontWeight=FontWeight.SemiBold)
   Spacer(Modifier.height(16.dp))
   Button(onClick=onContinue,
    modifier=Modifier.fillMaxWidth().height(60.dp),
    shape=RoundedCornerShape(20.dp),
    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2469D8))){
    Text("Weiter  →",fontSize=20.sp,fontWeight=FontWeight.Bold,color=Color.White)
   }
  }else if(failed){
   Text("Das neue Rätsel konnte nicht vorbereitet werden.",color=Color(0xFFB14A64))
   Spacer(Modifier.height(12.dp))
   OutlinedButton(onClick=onRetry){Text("Erneut versuchen")}
  }else{
   CircularProgressIndicator(color=Color(0xFF7468DD))
   Spacer(Modifier.height(16.dp))
   Text("Dein nächstes Rätsel entsteht …",color=Color(0xFF676D9A))
  }
 }
}

/** Local diagnostic events; no network access or player identifiers. */
private fun generateMeasured(stage:Int,seed:Long,aspect:Float,origin:String):Level {
 val start=System.nanoTime()
 return try {
  val level=LevelGeneration.generate(stage,seed,aspect)
  val elapsed=(System.nanoTime()-start)/1_000_000
  Log.i("ArrowPuzzleGeneration",
   "origin=$origin stage=$stage seed=$seed grid=${level.width}x${level.height} elapsedMs=$elapsed generator=${if(stage<=16) "legacy" else if(level.fallbackGeneration) "legacy-fallback" else "exact-cover"}")
  level
 } catch(error:Exception) {
  Log.w("ArrowPuzzleGeneration","origin=$origin stage=$stage seed=$seed elapsedMs=${(System.nanoTime()-start)/1_000_000} result=error",error)
  throw error
 }
}
private fun fresh(stage:Int,lives:Int,hints:Int,aspect:Float=1f):Game{
 repeat(15){try{return Game(generateMeasured(stage,Random.nextLong(),aspect,"fresh"),lives,hints)}catch(_:Exception){}}
 error("Cannot generate a valid level")
}
@Composable private fun Board(level:Level,remaining:Set<Int>,hinted:Int?,invalid:Int?,flights:List<Flight>,ever:Set<Pt>,traveled:Set<Pt>,modifier:Modifier,previewHoldMillis:Long,onTap:(Int)->Unit){
 val arrows=level.arrows.filter{it.id in remaining}
 var previewId by remember(level){mutableStateOf<Int?>(null)}
 Canvas(modifier.clip(RoundedCornerShape(26.dp)).background(Brush.verticalGradient(listOf(Color.White,Color(0xFFF9FBFF))))
  .pointerInput(level,remaining,flights.map{it.id},previewHoldMillis){
   coroutineScope {
    awaitEachGesture {
    val down=awaitFirstDown(requireUnconsumed=false)
    val sx=size.width.toFloat()/level.width
    val sy=size.height.toFloat()/level.height
    val selected=ArrowTouch.select(level,remaining,down.position.x/sx-.5f,down.position.y/sy-.5f)
    var active=selected!=null
    val previewJob=if(selected!=null)launch {
     delay(previewHoldMillis)
     previewId=selected
    }else null
    var released=false
    do {
     val event=awaitPointerEvent()
     val pointer=event.changes.firstOrNull{it.id==down.id}
     if(pointer==null || !pointer.pressed){
      released=pointer!=null && !pointer.pressed
      break
     }
     if(active && selected!=null){
      val arrow=level.arrows.first{it.id==selected}
      val px=pointer.position.x/sx-.5f
      val py=pointer.position.y/sy-.5f
      val near=arrow.cells().any{p->
       val dx=p.x-px;val dy=p.y-py
       dx*dx+dy*dy<=1.35f*1.35f
      }
      if(!near){active=false;previewJob?.cancel();previewId=null}
     }
    }while(true)
    previewJob?.cancel()
    previewId=null
    if(active && released && selected!=null)onTap(selected)
    }
   }
  }){
  val sx=size.width/level.width;val sy=size.height/level.height
  val cell=minOf(sx,sy)
  val ox=0f;val oy=0f
  val currentTrail=flights.flatMap{f->f.plan.travel.take(f.step)}.filter{it.x in 0 until level.size && it.y in 0 until level.height}.toSet()
  // Visible afterglow over several steps, also including just-vacated tail cells.
  val glow=mutableMapOf<Pt,Float>()
  flights.forEach{f->
   for(age in 0..3){
    val step=f.step-age
    if(step<=0)continue
    val strength=1f-age/4f
    listOfNotNull(f.plan.body.getOrNull(step-1),f.plan.travel.getOrNull(step-1))
     .filter{it.x in 0 until level.size && it.y in 0 until level.size}
     .forEach{p->glow[p]=maxOf(glow[p]?:0f,strength)}
   }
  }
  for(y in 0 until level.height)for(x in 0 until level.width){
   drawCircle(if(level.fallbackGeneration)Color(0xFFE4C58D) else Color(0xFFCED7ED),
    radius=(cell*.115f).coerceAtLeast(1.65f),
    center=Offset(ox+(x+.5f)*sx,oy+(y+.5f)*sy))
  }
  (ever+traveled+currentTrail+glow.keys).forEach{p->
   val point=Offset(ox+(p.x+.5f)*sx,oy+(p.y+.5f)*sy)
   val power=glow[p]?:0f
   if(power>0f){
    drawCircle(Color(0xFF6956EF).copy(alpha=.15f*power),
     radius=(cell*.58f).coerceAtLeast(5f),center=point)
    drawCircle(Color(0xFF8B7DFB).copy(alpha=.30f*power),
     radius=(cell*.31f).coerceAtLeast(3f),center=point)
   }
   drawCircle(if(power>0f)Color(0xFF655AD2) else if(p in currentTrail)Color(0xFFA4A8EE) else Color(0xFFBFCBE7),
    radius=(cell*(if(power>0f).17f else .125f)).coerceAtLeast(1.65f),center=point)
  }
  // Draw behind every arrow: full projected exit ray, even through blockers.
  previewId?.let{id->
   arrows.firstOrNull{it.id==id}?.let{arrow->
    val head=arrow.body().last()
    val exit=Rules.exitPath(arrow,level.width,level.height)
    val last=exit.lastOrNull()
    if(last!=null){
     val origin=Offset((head.x+.5f)*sx,(head.y+.5f)*sy)
     val finish=Offset((last.x+.5f)*sx,(last.y+.5f)*sy)
     drawLine(Color(0xFF7366D9).copy(alpha=.24f),origin,finish,
      strokeWidth=(cell*.23f).coerceAtLeast(3.3f))
    }
   }
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
