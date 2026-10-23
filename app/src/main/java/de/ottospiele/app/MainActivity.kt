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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.graphics.drawscope.DrawScope

/** Cached puzzles needed until the next mini-game boundary (exclusive).
 * During active play the already displayed puzzle does not occupy a cache slot.
 * Mini-games begin at stage boundaries, after at least four solved puzzles.
 */
internal fun goalsUntilNextMini(start:Progress,solvedSinceMini:Int,skipCurrent:Boolean):List<Pair<Int,Int>> {
 var current=start
 var solved=solvedSinceMini.coerceIn(0,4)
 val targets=linkedMapOf<Int,Int>()
 repeat(48){index->
  if(!(skipCurrent && index==0))targets[current.stage]=(targets[current.stage]?:0)+1
  val next=current.win()
  solved++
  if(current.stage>=10 && next.stage>current.stage && solved>=4)
   return targets.entries.map{it.key to it.value}
  current=next
 }
 return targets.entries.map{it.key to it.value}
}

/** Preserve the full quality search in early stages; reduce costly candidate sampling later. */
internal fun preparationCandidateCount(stage:Int,maximum:Int):Int =
 when {
  stage<30 -> maximum
  stage<45 -> minOf(maximum,6)
  stage<60 -> minOf(maximum,4)
  else -> minOf(maximum,2)
 }

private val generationMutex=Mutex()

private data class Flight(val id:Int,val plan:MovePlan,val failed:Boolean,val step:Int=0,val impactOffset:Float=0f)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState);setContent{MaterialTheme{Puzzle()}}
 }
}
@Composable fun Puzzle(){
 val context=LocalContext.current
 val haptics=remember(context){HapticEffects(context)}
 GeneratorDiagnostics.initialize(context)
 val screenAspect=remember{
  val metrics=context.resources.displayMetrics
  metrics.widthPixels.toFloat()/metrics.heightPixels.coerceAtLeast(1)
 }
 val defaults=remember(context){GameDefaults.load(context)}
 val previewHoldMillis=defaults.previewHoldMillis
 val canAdjustStage=defaults.showStageAdjustment &&
  (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
 val saved=remember{LocalSave.read(context)}
 val miniPrefs=remember(context){context.getSharedPreferences("arrow_puzzle_minigames",android.content.Context.MODE_PRIVATE)}
 val pendingStage=remember{miniPrefs.getInt("preparing_target_stage",0)}
 val pendingDone=remember{miniPrefs.getInt("preparing_target_done",0)}
 val restoringPreparation=pendingStage>0
 val restoringCurrent=restoringPreparation && miniPrefs.getBoolean("preparing_resume_current",false) && saved!=null
 val restoredTarget=remember(pendingStage,pendingDone){
  if(restoringPreparation)Progress(pendingStage,pendingDone) else null
 }
 var progress by remember{mutableStateOf(saved?.progress?:Progress())}
 var lives by remember{mutableIntStateOf(saved?.lifeSetting?:defaults.lives)}
 var hints by remember{mutableIntStateOf(saved?.hintSetting?:defaults.hints)}
 var game by remember{mutableStateOf(saved?.game?:fresh(1,defaults.lives,defaults.hints,screenAspect,defaults.legacyEnabled))}
 var revision by remember{mutableIntStateOf(0)}
 var highlighted by remember{mutableStateOf<Int?>(null)}
 var invalid by remember{mutableStateOf<Int?>(null)}
 var failedArrowIds by remember(game.level.seed,game.level.stage){mutableStateOf(emptySet<Int>())}
 var dialog by remember{mutableStateOf("")}
 var winScreen by remember{mutableStateOf(restoringPreparation || saved?.win==true)}
 var defeatScreen by remember{mutableStateOf(saved?.defeat?:false)}
 var defeatMode by remember{mutableStateOf("choice")}
 var defeatPrepared by remember{mutableStateOf<Game?>(null)}
 var defeatAttempt by remember{mutableIntStateOf(0)}
 var nextGame by remember{mutableStateOf<Game?>(if(restoringCurrent)saved?.game else null)}
 var nextProgress by remember{mutableStateOf<Progress?>(if(restoringCurrent)saved?.progress else null)}
 var forcedNextProgress by remember{mutableStateOf<Progress?>(restoredTarget)}
 var resumeCurrentAfterMini by remember{mutableStateOf(restoringCurrent)}
 var prepareError by remember{mutableStateOf(false)}
 var preparationAttempt by remember{mutableIntStateOf(0)}
 // A mini-game is offered only on a stage transition after four completed puzzles.
 var solvedSinceMini by rememberSaveable { mutableIntStateOf(miniPrefs.getInt("solved",0).coerceIn(0,4)) }
 var miniPending by rememberSaveable { mutableStateOf(restoringPreparation || miniPrefs.getBoolean("pending",false)) }
 var miniOpen by rememberSaveable { mutableStateOf(restoringPreparation || miniPrefs.getBoolean("open",false)) }
 LaunchedEffect(solvedSinceMini,miniPending,miniOpen){
  miniPrefs.edit().putInt("solved",solvedSinceMini).putBoolean("pending",miniPending)
   .putBoolean("open",miniOpen).commit()
 }
 var prepComplete by remember{mutableStateOf(false)}
 var prepFailed by remember{mutableStateOf(false)}
 var prepAttempt by remember{mutableIntStateOf(0)}
 var prepDone by remember{mutableIntStateOf(0)}
 var prepTotal by remember{mutableIntStateOf(2)}
 var prepStage by remember{mutableIntStateOf(0)}
 var settings by remember{mutableStateOf(false)}
 var menuOpen by remember{mutableStateOf(false)}
 var aboutOpen by remember{mutableStateOf(false)}
 var diagnosticsOpen by remember{mutableStateOf(false)}
 var diagnosticEvents by remember{mutableStateOf(emptyList<GeneratorDiagnostics.Event>())}
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
 // Resume an unfinished reserve via Puzzle It, without replacing the active puzzle.
 // An installed app with a valid, fully prepared save starts normally.
 LaunchedEffect(progress.stage,progress.done,winScreen,defeatScreen,screenAspect,solvedSinceMini) {
  if(winScreen || defeatScreen || saved==null)return@LaunchedEffect
  val goals=if(progress.stage>=10)goalsUntilNextMini(progress,solvedSinceMini,true)
   else listOf(progress.stage to (progress.required()-progress.done-1).coerceAtLeast(0))
  val missing=withContext(Dispatchers.IO) {
   goals.any{(stage,needed)->PreparedLevels.read(context,stage,screenAspect).size<needed}
  }
  if(missing && !winScreen && !defeatScreen) {
   val persisted=miniPrefs.edit()
    .putInt("preparing_target_stage",progress.stage)
    .putInt("preparing_target_done",progress.done)
    .putBoolean("preparing_resume_current",true).commit()
   if(persisted){
    stopFlights()
    forcedNextProgress=progress
    nextGame=game;nextProgress=progress;resumeCurrentAfterMini=true
    solvedSinceMini=0;miniPending=true;miniOpen=true;winScreen=true
    haptics.miniGameEntered()
   }
  }
 }
 // Prepare only the missing puzzles until the NEXT mini-game, across stage boundaries.
 // Replenishment happens during success screens / Puzzle It; active play only fills deficits.
 // Keep this effect independent of presentation-only changes (miniOpen,
// miniPending and nextGame); otherwise a candidate batch is repeatedly cancelled.
 LaunchedEffect(progress.stage,progress.done,winScreen,solvedSinceMini,forcedNextProgress,prepAttempt,screenAspect) {
  prepComplete=false
  prepFailed=false
  val target=if(winScreen)forcedNextProgress?:progress.win() else progress
  val count=if(winScreen && (miniOpen || miniPending || forcedNextProgress!=null))0 else solvedSinceMini
  // Snapshot the reservation only at the start of this work order.
  // Do not restart the work when nextGame is assigned asynchronously.
  val reservedNext=winScreen && nextGame!=null
  val goals=if(target.stage>=10)goalsUntilNextMini(target,count,!winScreen || reservedNext)
   else if(winScreen)listOf(target.stage to (target.required()-target.done))
   else listOf(target.stage to (target.required()-target.done-1).coerceAtLeast(0))
  val currentStage=target.stage
  Log.i("ArrowPuzzlePreparation","phase=${if(winScreen)"transition" else "play"} stage=$currentStage untilMini=$goals")
  prepStage=currentStage
  prepDone=if(reservedNext)1 else 0
  prepTotal=goals.sumOf { it.second }+if(reservedNext)1 else 0
  var allPrepared=true
  val preparationStarted=android.os.SystemClock.elapsedRealtime()
  val generationOrigin=if(winScreen)"transition" else "play"
  try {
  for((target,capacity) in goals) {
   val slotStart=android.os.SystemClock.elapsedRealtime()
   var cached=withContext(Dispatchers.IO) {
    PreparedLevels.read(context,target,screenAspect)
   }
   prepDone=(if(reservedNext)1 else 0)+goals.takeWhile { it.first!=target }.sumOf { it.second }+cached.size
   while(cached.size<capacity) {
    val attemptStart=android.os.SystemClock.elapsedRealtime()
    val candidate=withContext(Dispatchers.Default) { generationMutex.withLock {
     var chosen:Level?=null
     if(target>=9) {
      val candidates=mutableListOf<Level>()
      repeat(preparationCandidateCount(target,defaults.preparationExactAttemptsPerSlot)) {
       val seed=Random.nextLong()
       if(seed!=game.level.seed && cached.none { it.seed==seed })
        generateExactMeasured(target,seed,screenAspect)?.let { candidates.add(it) }
      }
      chosen=DifficultyProfiles.choose(target,candidates)
     }
     if(chosen==null && defaults.legacyEnabled) {
      repeat(3) {
       if(chosen==null) {
        val seed=Random.nextLong()
        if(seed!=game.level.seed && cached.none { it.seed==seed })
         chosen=runCatching { generateFallbackMeasured(target,seed,screenAspect) }.getOrNull()
       }
      }
     }
     chosen ?: if(target<=8)TutorialLevels.level(target,Random.nextLong()) else null
    } }
    if(candidate==null){
     allPrepared=false
     GeneratorDiagnostics.recordPreparation("slot_failed",target,generationOrigin,
      android.os.SystemClock.elapsedRealtime()-attemptStart)
     break
    }
    withContext(Dispatchers.IO) {
     PreparedLevels.append(context,target,screenAspect,candidate,capacity)
    }
    cached=withContext(Dispatchers.IO){PreparedLevels.read(context,target,screenAspect)}
    GeneratorDiagnostics.recordPreparation("slot_completed",target,generationOrigin,
     android.os.SystemClock.elapsedRealtime()-attemptStart)
    prepDone=goals.takeWhile { it.first!=target }.sumOf { it.second }+cached.size
   }
   if(cached.size<capacity)allPrepared=false
   else if(cached.size>=capacity && capacity>0)Log.i("ArrowPuzzlePreparation",
    "Stage $target cache ready: ${cached.size}/$capacity; elapsed=${android.os.SystemClock.elapsedRealtime()-slotStart}ms")
  }
  prepComplete=allPrepared
  prepFailed=!allPrepared
  GeneratorDiagnostics.recordPreparation(if(allPrepared)"preparation_complete" else "preparation_incomplete",
   currentStage,generationOrigin,android.os.SystemClock.elapsedRealtime()-preparationStarted)
  }catch(cancel:CancellationException){
   GeneratorDiagnostics.recordPreparation("preparation_cancelled",currentStage,generationOrigin,
    android.os.SystemClock.elapsedRealtime()-preparationStarted)
   throw cancel
  }
 }
 LaunchedEffect(winScreen,preparationAttempt,forcedNextProgress,resumeCurrentAfterMini){
  if(!winScreen || resumeCurrentAfterMini)return@LaunchedEffect
  nextGame=null
  nextProgress=null
  prepareError=false
  // Give the celebration a perceptible opening, even on fast hardware.
  delay(450)
  try {
   val target=forcedNextProgress?:progress.win()
   // Cache extraction is synchronized by PreparedLevels itself. Do not wait
   // for an unrelated generation holding generationMutex if a level is ready.
   val prepared=withContext(Dispatchers.IO){
    PreparedLevels.take(context,target.stage,screenAspect,game.level.seed)
   }
   if(prepared!=null){
    Log.i("ArrowPuzzlePreparation","Cache hit stage=${target.stage} seed=${prepared.seed}")
    GeneratorDiagnostics.recordPreparation("cache_hit",target.stage,"transition",0L)
   }
   val ready=if(prepared!=null)Game(prepared,lives,hints)
    else withContext(Dispatchers.Default){generationMutex.withLock{
     // Background preparation may have finished while this coroutine waited.
     val late=PreparedLevels.take(context,target.stage,screenAspect,game.level.seed)
     if(late!=null){
      GeneratorDiagnostics.recordPreparation("cache_hit_late",target.stage,"transition",0L)
      Log.i("ArrowPuzzlePreparation","Late cache hit stage=${target.stage} seed=${late.seed}")
      Game(late,lives,hints)
     }else{
      GeneratorDiagnostics.recordPreparation("cache_miss",target.stage,"transition",0L)
      Log.w("ArrowPuzzlePreparation","Cache miss stage=${target.stage}; generating during win/mini-game")
      fresh(target.stage,lives,hints,screenAspect,defaults.legacyEnabled)
     }
    }}
   nextProgress=target
   nextGame=ready
  }catch(cancel:CancellationException){throw cancel}
   catch(_:Exception){prepareError=true}
 }

 LaunchedEffect(defeatScreen,defeatMode,defeatAttempt){
  if(!defeatScreen || defeatMode!="loading")return@LaunchedEffect
  defeatPrepared=null
  delay(350)
  try{
   val ready=withContext(Dispatchers.Default){generationMutex.withLock{fresh(progress.stage,lives,hints,screenAspect,defaults.legacyEnabled)}}
   defeatPrepared=ready
   defeatMode="ready"
  }catch(_:Exception){defeatMode="error"}
 }

 if(winScreen && miniOpen){
  PuzzleItScreen(
   config=defaults.puzzleIt,
   preparationReady=nextGame!=null && nextProgress!=null && prepComplete,
   preparationProgress=if(prepTotal<=0)1f else (prepDone.toFloat()/prepTotal).coerceIn(0f,1f),
   preparationFailed=prepareError || prepFailed,
   requiredRounds=Int.MAX_VALUE,
   onRetryPreparation={preparationAttempt++;prepAttempt++},
   onCompleted={
    val next=nextGame;val nextP=nextProgress
    if(miniOpen && next!=null && nextP!=null){
     haptics.arrowPuzzleReturned()
     stopFlights();progress=nextP;game=next;highlighted=null;invalid=null
     miniPrefs.edit().remove("preparing_target_stage").remove("preparing_target_done")
      .remove("preparing_resume_current").commit()
     resumeCurrentAfterMini=false
     traveledCells=emptySet();revision++;winScreen=false;miniOpen=false;miniPending=false;forcedNextProgress=null
     nextGame=null;nextProgress=null
    }
   }
  )
 }else if(winScreen){
  WinScreen(
   stage=progress.stage,
   nextStage=nextProgress?.stage,
   ready=nextGame!=null,
   failed=prepareError,
   miniAvailable=miniPending && progress.stage>=10,
   onPlay={
    if(!miniOpen){
     haptics.miniGameEntered()
     miniOpen=true;solvedSinceMini=0
    }
   },
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
     miniPending=false;miniOpen=false
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
    failedArrowIds=emptySet()
    traveledCells=emptySet();highlighted=null;invalid=null;revision++
    defeatScreen=false;defeatMode="choice";defeatPrepared=null
   },
   onNew={
    val target=progress.copy(done=0)
    miniPrefs.edit().putInt("preparing_target_stage",target.stage)
     .putInt("preparing_target_done",target.done).commit()
    stopFlights();defeatScreen=false;defeatPrepared=null
    resumeCurrentAfterMini=false
    miniPrefs.edit().remove("preparing_resume_current").commit()
    forcedNextProgress=target;nextGame=null;nextProgress=null;prepareError=false
    solvedSinceMini=0;miniPending=true;miniOpen=true;winScreen=true
    haptics.miniGameEntered()
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
     onClick=ticked {menuOpen=true},
     modifier=Modifier.size(56.dp),
     colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=Color.White)){
     Text("☰",fontSize=27.sp,color=Color(0xFF5662A7))
    }
    DropdownMenu(expanded=menuOpen,onDismissRequest={menuOpen=false}){
     DropdownMenuItem(text={Text("⚙  Einstellungen")},onClick=ticked {menuOpen=false;settings=true})
     if(canAdjustStage)DropdownMenuItem(text={Text("↕  Stufe anpassen")},onClick=ticked {
      menuOpen=false;stageInput=progress.stage.toString();stageError=false;dialog="stufeAnpassen"
     })
     if(context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)DropdownMenuItem(text={Text("📊  Generator-Diagnose")},onClick=ticked {menuOpen=false;diagnosticEvents=GeneratorDiagnostics.entries(context);diagnosticsOpen=true})
     DropdownMenuItem(text={Text("ⓘ  Über das Spiel")},onClick=ticked {menuOpen=false;aboutOpen=true})
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
     onClick=ticked {stopFlights();game.retry();failedArrowIds=emptySet();traveledCells=emptySet();revision++;highlighted=null;invalid=null},
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
     onClick=ticked {dialog="stufeZurueck"},
    modifier=Modifier.weight(1f).height(58.dp),shape=RoundedCornerShape(17.dp),
    contentPadding=PaddingValues(horizontal=4.dp),
    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF7955CE),contentColor=Color.White)){
    Column(horizontalAlignment=Alignment.CenterHorizontally){
     Text("↓",fontSize=22.sp,fontWeight=FontWeight.Bold)
     Text("Stufe zurück",fontSize=11.sp,fontWeight=FontWeight.Bold,maxLines=1)
    }
   }
   Button(
    onClick=ticked {highlighted=game.hint();invalid=null;revision++},
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
     onClick=ticked {menuOpen=true},
     modifier=Modifier.size(56.dp),
     colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=Color.White)){
     Text("☰",fontSize=27.sp,color=Color(0xFF5662A7))
    }
    DropdownMenu(expanded=menuOpen,onDismissRequest={menuOpen=false}){
     DropdownMenuItem(text={Text("⚙  Einstellungen")},onClick=ticked {menuOpen=false;settings=true})
     if(canAdjustStage)DropdownMenuItem(text={Text("↕  Stufe anpassen")},onClick=ticked {
      menuOpen=false;stageInput=progress.stage.toString();stageError=false;dialog="stufeAnpassen"
     })
     if(context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)DropdownMenuItem(text={Text("📊  Generator-Diagnose")},onClick=ticked {menuOpen=false;diagnosticEvents=GeneratorDiagnostics.entries(context);diagnosticsOpen=true})
     DropdownMenuItem(text={Text("ⓘ  Über das Spiel")},onClick=ticked {menuOpen=false;aboutOpen=true})
    }
   }
   Column(Modifier.weight(1f)) {
    Text("OttOSpiele",fontSize=25.sp,fontWeight=FontWeight.Bold,color=Color(0xFF24294B),maxLines=1)
    Text("ARROW PUZZLE",fontSize=11.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFF7E84A6),letterSpacing=2.sp,maxLines=1)
   }
   Button(
    enabled=!game.won && !backLoading,
     onClick=ticked {stopFlights();game.retry();failedArrowIds=emptySet();traveledCells=emptySet();revision++;highlighted=null;invalid=null},
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
     onClick=ticked {dialog="stufeZurueck"},
    modifier=Modifier.width(136.dp).height(58.dp),shape=RoundedCornerShape(17.dp),
    contentPadding=PaddingValues(horizontal=4.dp),
    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF7955CE),contentColor=Color.White)){
    Column(horizontalAlignment=Alignment.CenterHorizontally){
     Text("↓",fontSize=22.sp,fontWeight=FontWeight.Bold)
     Text("Stufe zurück",fontSize=11.sp,fontWeight=FontWeight.Bold,maxLines=1)
    }
   }
   Button(
    onClick=ticked {highlighted=game.hint();invalid=null;revision++},
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

  Board(game.level,game.remaining.map{it.id}.toSet(),highlighted,failedArrowIds,
   flights.toList(),occupiedEver,traveledCells,Modifier.weight(1f).fillMaxWidth(),previewHoldMillis,onPreview={haptics.arrowSelected()}){
   if(dialog.isNotEmpty() || game.won || game.lost || (!concurrent && flights.isNotEmpty()) || flights.any{f->f.id==it})return@Board
   val arrow=game.remaining.firstOrNull{a->a.id==it}?:return@Board
   val plan=Rules.plan(arrow,game.level,game.remaining.map{a->a.id}.toSet())
   val failed=!plan.canExit
   if(failed){
    failedArrowIds=failedArrowIds+it
    haptics.blockedArrow()
   }
   highlighted=null;invalid=null
   // Apply success to game immediately: subsequently tapped arrows see updated blockers.
   if(!failed){
    haptics.arrowRemoved()
    game.tap(it)
    // Re-evaluate only arrows previously marked red. A successfully removed
    // blocker may have made their exit path free again.
    val remainingIds=game.remaining.map{a->a.id}.toSet()
    failedArrowIds=failedArrowIds.filterTo(mutableSetOf()){id->
     val candidate=game.remaining.firstOrNull{a->a.id==id}
     candidate!=null && !Rules.plan(candidate,game.level,remainingIds).canExit
    }
    revision++
   }else{game.tap(it);revision++}
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
     // A blocker immediately in front of the arrow produces allowedSteps=0.
     // Animate a short elastic bump so even this collision is clearly visible.
     for(offset in listOf(.17f,.37f,.22f,-.10f,0f)){
      delay(55)
      val index=flights.indexOfFirst{f->f.id==it}
      if(index<0)return@launch
      flights[index]=flights[index].copy(impactOffset=offset)
     }
     delay(55)
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
     if(game.won){
      solvedSinceMini=(solvedSinceMini+1).coerceAtMost(4)
      miniPending=progress.stage>=10 && progress.win().stage>progress.stage && solvedSinceMini>=4
      haptics.stageSuccess()
      winScreen=true
     }
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
    if(prepStage==progress.stage) prepDone.toFloat()/prepTotal.coerceAtLeast(1)
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
    onClick=ticked {
     val selected=stageInput.toIntOrNull()?:return@ticked
     val target=Progress(selected,0)
     if(!miniPrefs.edit().putInt("preparing_target_stage",target.stage)
       .putInt("preparing_target_done",target.done).commit()){
      stageError=true;return@ticked
     }
     stopFlights();dialog="";defeatScreen=false;defeatPrepared=null
     resumeCurrentAfterMini=false
     miniPrefs.edit().remove("preparing_resume_current").commit()
     nextGame=null;nextProgress=null;prepareError=false
     forcedNextProgress=target
     solvedSinceMini=0;miniPending=true;miniOpen=true;winScreen=true
     haptics.miniGameEntered()
    }
   ){Text("Stufe starten")}
  },
  dismissButton={TextButton(enabled=!stageLoading,onClick=ticked {dialog=""}){Text("Abbrechen")}}
 )
 if(dialog=="stufeZurueckFehler")AlertDialog(onDismissRequest={dialog=""},confirmButton={TextButton(onClick=ticked {dialog=""}){Text("OK")}},title={Text("Rätsel konnte nicht erstellt werden")},text={Text("Bitte erneut versuchen.")})
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
   else Button(onClick=ticked {dialog=""},colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2469D8))){
    Text("Fortsetzen",fontWeight=FontWeight.Bold)
   }
  },
  dismissButton={
   if(!backLoading) Button(onClick=ticked {
    if(!backLoading){
     // A deliberate downgrade starts mandatory Puzzle It. Generate the
     // replacement puzzle while the child is playing the mini-game.
     val target=progress.easier()
     miniPrefs.edit().putInt("preparing_target_stage",target.stage)
      .putInt("preparing_target_done",target.done).commit()
     stopFlights();dialog=""
     resumeCurrentAfterMini=false
     miniPrefs.edit().remove("preparing_resume_current").commit()
     forcedNextProgress=target;nextProgress=null;nextGame=null;prepareError=false
     solvedSinceMini=0;miniPending=true;miniOpen=true;winScreen=true
     haptics.miniGameEntered()
    }
   },colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF7955CE))){
    Text("Stufe zurück",fontWeight=FontWeight.Bold)
   }
  }
 )
 if(diagnosticsOpen) {
  val successes=diagnosticEvents.filter{it.outcome=="success"}
  val exact=successes.count{it.generator=="exact-cover"}
  val fallbacks=successes.count{it.generator=="legacy-fallback"}
  val legacy=successes.count{it.generator=="legacy"}
  val ordered=successes.map{it.elapsedMs}.sorted()
  val mean=if(ordered.isEmpty()) 0L else ordered.sum()/ordered.size
  val maximum=ordered.lastOrNull()?:0L
  AlertDialog(
   onDismissRequest={diagnosticsOpen=false},
   title={Text("Generator-Diagnose (Debug)")},
   text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
    Text("Gespeicherte Versuche: ${diagnosticEvents.size} / 300")
    Text("Exact Cover: $exact   Legacy: $legacy   Fallback: $fallbacks")
    Text("Fallback-Anteil ab Stufe 17: "+run {
     val later=successes.count{it.stage>=17}
     if(later==0)"–" else "${fallbacks*100/later} %"
    })
    Text("Generierung: Ø ${mean} ms, Maximum ${maximum} ms")
    Text("Exact-Cover-Versuche ohne Ergebnis: ${diagnosticEvents.count{it.outcome=="rejected"}}")
    Text("Generierungsfehler: ${diagnosticEvents.count{it.outcome=="error"}}")
    Text("Letzte Generierungen:")
    Column(Modifier.heightIn(max=170.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())){
     diagnosticEvents.takeLast(8).asReversed().forEach{event->
      Text("Stufe ${event.stage} · ${event.width}×${event.height} · ${event.elapsedMs} ms · ${event.generator} · ${event.outcome}",fontSize=12.sp)
     }
    }
   }},
   confirmButton={TextButton(onClick=ticked {GeneratorDiagnostics.share(context,diagnosticEvents)}){Text("CSV teilen")}},
   dismissButton={Row{
    TextButton(onClick=ticked {GeneratorDiagnostics.clear(context);diagnosticEvents=emptyList()}){Text("Löschen")}
    TextButton(onClick=ticked {diagnosticEvents=GeneratorDiagnostics.entries(context)}){Text("Aktualisieren")}
    TextButton(onClick=ticked {diagnosticsOpen=false}){Text("Schließen")}
   }}
  )
 }
 if(aboutOpen)AlertDialog(
  onDismissRequest={aboutOpen=false},
  title={Text("Über Arrow Puzzle")},
  text={Text("OttOSpiele – Arrow Puzzle\n\nEin Rätselspiel mit Pfeilen für Kinder. Finde die freien Pfeile und lasse sie aus dem Spielfeld fahren.\n\nOffline spielbar, ohne Werbung.")},
  confirmButton={TextButton(onClick=ticked {aboutOpen=false}){Text("Schließen")}}
 )
 if(settings)AlertDialog(
  onDismissRequest={settings=false},
  title={Text("Spieleinstellungen")},
  text={Column{
   Text("Herzen pro Level")
   Row{listOf(3,5,10).forEach{n->TextButton(onClick=ticked {lives=n}){Text(if(lives==n)"[$n]"else"$n")}}}
   Text("Tipps pro Level")
   Row{listOf(0,1,3,5).forEach{n->TextButton(onClick=ticked {hints=n}){Text(if(hints==n)"[$n]"else"$n")}}}
   Text("Parallele Pfeile")
   Row(verticalAlignment=Alignment.CenterVertically){
    Text("Nächsten Pfeil sofort antippen",modifier=Modifier.weight(1f))
    Switch(checked=concurrent,onCheckedChange={concurrent=it})
   }
   Text("Ausfahrgeschwindigkeit")
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
    listOf("Ruhig","Normal","Flott","Turbo").forEachIndexed{index,label->
     FilterChip(selected=speed==index,onClick=ticked {speed=index},label={Text(label)})
    }
   }
   Text("Herzen und Tipps gelten ab dem nächsten Rätsel. Animationseinstellungen sofort.")
  }},
  confirmButton={TextButton(onClick=ticked {settings=false}){Text("OK")}}
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
    Button(onClick=ticked(onAgain),modifier=Modifier.fillMaxWidth().height(60.dp),
     shape=RoundedCornerShape(20.dp),
     colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2469D8))){
     Text("↻  Nochmal versuchen",fontSize=19.sp,fontWeight=FontWeight.Bold)
    }
    Spacer(Modifier.height(14.dp))
    FilledTonalButton(onClick=ticked(onNew),modifier=Modifier.fillMaxWidth().height(60.dp),
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
    Button(onClick=ticked(onContinue),modifier=Modifier.fillMaxWidth().height(60.dp),
     shape=RoundedCornerShape(20.dp),
     colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2469D8))){
     Text("Weiter  →",fontSize=20.sp,fontWeight=FontWeight.Bold)
    }
   }
   else->{
    Text("Das Rätsel konnte nicht vorbereitet werden.",color=Color(0xFFB14A64))
    Spacer(Modifier.height(15.dp))
    Button(onClick=ticked(onRetryGeneration)){Text("Erneut versuchen")}
    TextButton(onClick=ticked(onAgain)){Text("Dasselbe Rätsel wiederholen")}
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
 miniAvailable:Boolean,
 onPlay:()->Unit,
 onRetry:()->Unit,
 onContinue:()->Unit
){
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFECEBFF),Color(0xFFF6FAFF),Color(0xFFFFF9EC))))) {
 Column(
  Modifier.fillMaxSize().padding(28.dp),
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
  if(miniAvailable){
   Text("Zeit für eine kleine Spielpause!",color=Color(0xFF3E699C),fontWeight=FontWeight.SemiBold)
   Spacer(Modifier.height(16.dp))
   Button(onClick=ticked(onPlay),modifier=Modifier.fillMaxWidth().height(60.dp),
    shape=RoundedCornerShape(20.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2469D8))){
    Text("▶  Spielen",fontSize=20.sp,fontWeight=FontWeight.Bold,color=Color.White)
   }
  }else if(ready){
   Text(if(nextStage!=null && nextStage>stage)"Neue Stufe freigeschaltet!" else "Dein nächstes Rätsel ist bereit!",
    color=Color(0xFF3E699C),fontWeight=FontWeight.SemiBold)
   Spacer(Modifier.height(16.dp))
   Button(onClick=ticked(onContinue),
    modifier=Modifier.fillMaxWidth().height(60.dp),
    shape=RoundedCornerShape(20.dp),
    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2469D8))){
    Text("Weiter  →",fontSize=20.sp,fontWeight=FontWeight.Bold,color=Color.White)
   }
  }else if(failed){
   Text("Das neue Rätsel konnte nicht vorbereitet werden.",color=Color(0xFFB14A64))
   Spacer(Modifier.height(12.dp))
   OutlinedButton(onClick=ticked(onRetry)){Text("Erneut versuchen")}
  }else{
   CircularProgressIndicator(color=Color(0xFF7468DD))
   Spacer(Modifier.height(16.dp))
   Text("Dein nächstes Rätsel entsteht …",color=Color(0xFF676D9A))
  }
 }
 if(!ready && !failed) LinearProgressIndicator(modifier=Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
  color=Color.White,trackColor=Color(0xFFD6D9E9))
 else Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp).background(Color.White))
 }
}

/** Local diagnostic events; no network access or player identifiers. */
private fun generateExactMeasured(stage:Int,seed:Long,aspect:Float):Level? {
 val start=System.nanoTime()
 val level=runCatching { LevelGeneration.generateExactOrNull(stage,seed,aspect) }.getOrNull()
 val elapsed=(System.nanoTime()-start)/1_000_000
 GeneratorDiagnostics.record(GeneratorDiagnostics.Event(System.currentTimeMillis(),"preparation",stage,seed,
  level?.width?:0,level?.height?:0,elapsed,"exact-cover",if(level==null)"rejected" else "success"))
 Log.i("ArrowPuzzleGeneration","origin=preparation stage=$stage seed=$seed elapsedMs=$elapsed generator=exact-cover outcome=${if(level==null)"rejected" else "success"}")
 return level
}
private fun generateFallbackMeasured(stage:Int,seed:Long,aspect:Float):Level {
 val start=System.nanoTime()
 val level=LevelGeneration.generateFallback(stage,seed,aspect)
 val elapsed=(System.nanoTime()-start)/1_000_000
 GeneratorDiagnostics.record(GeneratorDiagnostics.Event(System.currentTimeMillis(),"preparation",stage,seed,
  level.width,level.height,elapsed,"legacy-fallback","success"))
 Log.i("ArrowPuzzleGeneration","origin=preparation stage=$stage seed=$seed elapsedMs=$elapsed generator=${"legacy-fallback"} outcome=success")
 return level
}
private fun generateMeasured(stage:Int,seed:Long,aspect:Float,origin:String,legacyEnabled:Boolean):Level {
 val start=System.nanoTime()
 return try {
  val level=LevelGeneration.generate(stage,seed,aspect,GenerationPolicy(legacyEnabled))
  val elapsed=(System.nanoTime()-start)/1_000_000
  GeneratorDiagnostics.record(GeneratorDiagnostics.Event(System.currentTimeMillis(),origin,stage,seed,
   level.width,level.height,elapsed,
   if(stage<=8)"tutorial" else if(level.fallbackGeneration)"legacy-fallback" else "exact-cover","success"))
  Log.i("ArrowPuzzleGeneration",
   "origin=$origin stage=$stage seed=$seed grid=${level.width}x${level.height} elapsedMs=$elapsed generator=${if(stage<=8) "tutorial" else if(level.fallbackGeneration) "legacy-fallback" else "exact-cover"}")
  level
 } catch(error:Exception) {
  GeneratorDiagnostics.record(GeneratorDiagnostics.Event(System.currentTimeMillis(),origin,stage,seed,
   0,0,(System.nanoTime()-start)/1_000_000,"error","error"))
  Log.w("ArrowPuzzleGeneration","origin=$origin stage=$stage seed=$seed elapsedMs=${(System.nanoTime()-start)/1_000_000} result=error",error)
  throw error
 }
}
private fun fresh(stage:Int,lives:Int,hints:Int,aspect:Float=1f,legacyEnabled:Boolean=true):Game{
 repeat(15){try{return Game(generateMeasured(stage,Random.nextLong(),aspect,"fresh",legacyEnabled),lives,hints)}catch(_:Exception){}}
 error("Cannot generate a valid level")
}
@Composable private fun Board(level:Level,remaining:Set<Int>,hinted:Int?,failedArrowIds:Set<Int>,flights:List<Flight>,ever:Set<Pt>,traveled:Set<Pt>,modifier:Modifier,previewHoldMillis:Long,onPreview:()->Unit,onTap:(Int)->Unit){
 val arrows=level.arrows.filter{it.id in remaining}
 var previewId by remember(level){mutableStateOf<Int?>(null)}
 Canvas(modifier.clip(RoundedCornerShape(26.dp)).background(Brush.verticalGradient(listOf(Color.White,Color(0xFFF9FBFF))))
  .pointerInput(level,remaining,flights.map{it.id},previewHoldMillis){
   coroutineScope {
    awaitEachGesture {
    val down=awaitFirstDown(requireUnconsumed=false)
    val sx=size.width.toFloat()/level.width
    val sy=size.height.toFloat()/level.height
    val selected=ArrowTouch.select(level,remaining,down.position.x/sx-.5f,down.position.y/sy-.5f,flights.map{it.id}.toSet())
    var active=selected!=null
    val previewJob=if(selected!=null)launch {
     delay(previewHoldMillis)
     previewId=selected
     onPreview()
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
   drawCircle(if(level.fallbackGeneration && level.stage>16)Color(0xFFE4C58D) else Color(0xFFCED7ED),
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
  // Draw animations underneath stationary pieces; only idle arrows own hit areas.
  val allVisible=flights.mapNotNull{f->level.arrows.firstOrNull{it.id==f.id}?.let{a->a to (f.plan.frame(f.step) to f.failed)}}+arrows.filter{a->flights.none{f->f.id==a.id}}.map{it to (it.body() to false)}
  allVisible.forEach{(a,shape)->
   val points=shape.first
   if(points.isEmpty())return@forEach
   val impact=flights.firstOrNull{it.id==a.id}?.impactOffset?:0f
   val vertices=points.map{Offset(ox+(it.x+.5f+impact*a.dir.x)*sx,oy+(it.y+.5f+impact*a.dir.y)*sy)}
   val tip=vertices.last()
   val color=if(flights.any{it.id==a.id})if(shape.second)Color(0xFFE85161)else Color(0xFF7468EB) else when {a.id in failedArrowIds->Color(0xFFCB4D64);a.id==hinted->Color(0xFF21A679);else->Color(0xFF28385D)}
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
