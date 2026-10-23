// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.app

import android.graphics.Paint
import android.graphics.Path
import android.graphics.Region
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A single self-contained round renderer. No preview is drawn behind the pieces.
 * Caller decides whether to continue after each *complete* round.
 */
@Composable
internal fun PuzzleItScreen(
 config:PuzzleItConfig,
 preparationReady:Boolean,
 preparationFailed:Boolean,
 requiredRounds:Int,
 onCompleted:()->Unit,
 onRetryPreparation:()->Unit
) {
 val snapshotKey=rememberSaveable { System.nanoTime() }
 var snapshot by rememberSaveable { mutableStateOf("") }
 val controller=remember(snapshotKey) {
  PuzzleItController(config,snapshotKey).also { c ->
   if(snapshot.isNotEmpty()) runCatching { decodePuzzleIt(snapshot,c) }
  }
 }
 var round by remember { mutableStateOf(controller.round) }
 var dragIndex by remember { mutableIntStateOf(-1) }
 var grip by remember { mutableStateOf(Offset.Zero) }
 // The saved snapshot stores logical tile-space anchors and the current phase.
 fun publish(){
  round=controller.round.copy()
  snapshot=encodePuzzleIt(controller)
 }
 LaunchedEffect(preparationReady) {
  if(preparationReady) controller.requestStopAfterCurrentRound()
  publish()
 }
 LaunchedEffect(round.phase,round.seed) {
  if(round.phase==PuzzleItPhase.ROUND_SUCCESS) {
   delay(config.successMs)
   val exiting=controller.finishRound(preparationReady,requiredRounds)
   publish()
   if(exiting)onCompleted()
  }else if(round.phase==PuzzleItPhase.EXITING)onCompleted()
 }
 Column(Modifier.fillMaxSize().background(Color(0xFFF0F1F8))) {
  Box(Modifier.weight(1f).fillMaxWidth().padding(4.dp)) {
   Canvas(Modifier.fillMaxSize().pointerInput(round.seed,round.phase) {
    detectDragGestures(
     onDragStart=start@{at->
      if(round.phase!=PuzzleItPhase.PUZZLE_ACTIVE)return@start
      val geometry=PuzzleItLayout(size.width.toFloat(),size.height.toFloat())
      val index=round.pieces.indices.reversed().firstOrNull { i->
       val piece=controller.round.pieces[i]
       !piece.fixed && containsPuzzlePiece(controller.round,i,at,geometry,piece)
      } ?:return@detectDragGestures
      dragIndex=index
      val p=controller.round.pieces[index]
      grip=Offset(at.x-geometry.scatterX-p.left*geometry.tile,
       at.y-geometry.scatterY-p.top*geometry.tile)
     },
     onDragCancel={dragIndex=-1},
     onDragEnd={
      val i=dragIndex
      if(i>=0) {
       val p=controller.round.pieces[i]
       val geometry=PuzzleItLayout(size.width.toFloat(),size.height.toFloat())
       val absoluteX=geometry.scatterX+p.left*geometry.tile
       val absoluteY=geometry.scatterY+p.top*geometry.tile
       val snapX=(absoluteX-geometry.boardX)/geometry.tile
       val snapY=(absoluteY-geometry.boardY)/geometry.tile
       controller.move(i,snapX,snapY,true)
       // Free positions are stored relative to the scatter origin, fixed positions
       // are stored at target board coordinates. Keep the two spaces separate.
       if(!controller.round.pieces[i].fixed) {
        val ps=controller.round.pieces.toMutableList()
        ps[i]=ps[i].copy(left=p.left,top=p.top)
        controller.replace(controller.round.copy(pieces=ps),controller.stopAfterCurrentRound,controller.completedRounds)
       }
       publish()
      }
      dragIndex=-1
     },
     onDrag=drag@{change,delta->
      val i=dragIndex
      if(i<0)return@detectDragGestures
      change.consume()
      val geometry=PuzzleItLayout(size.width.toFloat(),size.height.toFloat())
      val p=round.pieces[i]
      // Keep the full shape, including outward tabs, inside the available canvas.
      val maxX=((size.width-geometry.scatterX)/geometry.tile-1.23f).coerceAtLeast(0f)
      val maxY=((size.height-geometry.scatterY)/geometry.tile-1.23f).coerceAtLeast(0f)
      val next=p.copy(left=(p.left+delta.x/geometry.tile).coerceIn(.25f,maxX),
       top=(p.top+delta.y/geometry.tile).coerceIn(.25f,maxY))
      val ps=controller.round.pieces.toMutableList()
      ps[i]=next
      controller.replace(controller.round.copy(pieces=ps),controller.stopAfterCurrentRound,controller.completedRounds)
      publish()
     }
    )
   }) {
    val geometry=PuzzleItLayout(size.width,size.height)
    val canvas=drawContext.canvas.nativeCanvas
    val frame=Paint(Paint.ANTI_ALIAS_FLAG).apply {
     color=android.graphics.Color.rgb(152,154,179)
     style=Paint.Style.STROKE;strokeWidth=3f
    }
    canvas.drawRoundRect(geometry.boardX,geometry.boardY,
     geometry.boardX+geometry.tile*3,geometry.boardY+geometry.tile*3,
     12f,12f,frame)
    val ordering=round.pieces.indices.sortedWith(compareBy<Int>{!round.pieces[it].fixed}.thenBy{it})
     .filter{it!=dragIndex} + listOfNotNull(dragIndex.takeIf{it>=0})
    ordering.forEach { i->
     val piece=round.pieces[i]
     val x=if(piece.fixed)geometry.boardX+(i%3)*geometry.tile else geometry.scatterX+piece.left*geometry.tile
     val y=if(piece.fixed)geometry.boardY+(i/3)*geometry.tile else geometry.scatterY+piece.top*geometry.tile
     drawPuzzlePiece(canvas,round,i,x,y,geometry.tile)
    }
   }
  }
  if(round.phase==PuzzleItPhase.CHOICE_ACTIVE || round.phase==PuzzleItPhase.ROUND_SUCCESS) {
   Column(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=6.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    repeat(2){row->
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
      repeat(2){col->
       val value=round.answers[row*2+col]
       val active=round.phase==PuzzleItPhase.CHOICE_ACTIVE && value !in round.disabled
       Button(onClick={if(controller.choose(value))publish() else publish()},
        enabled=active,modifier=Modifier.weight(1f).height(62.dp),
        shape=RoundedCornerShape(17.dp),
        contentPadding=PaddingValues(3.dp),
        colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF4773BD))) {
        // A compact two-row symbol display remains legible at ten objects.
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
         Text("●".repeat(min(value,5)),fontSize=16.sp,lineHeight=18.sp)
         if(value>5)Text("●".repeat(value-5),fontSize=16.sp,lineHeight=18.sp)
        }
       }
      }
     }
    }
   }
  }
  if(round.phase==PuzzleItPhase.ROUND_SUCCESS) {
   Text("★",fontSize=32.sp,color=Color(0xFF8B63CA),
    modifier=Modifier.align(Alignment.CenterHorizontally))
  }
  if(preparationFailed) TextButton(onClick=onRetryPreparation,modifier=Modifier.align(Alignment.CenterHorizontally)){
   Text("Rätselvorbereitung wiederholen")
  }
  if(!preparationReady) LinearProgressIndicator(modifier=Modifier.fillMaxWidth().height(3.dp),
   color=Color.White,trackColor=Color(0xFFD1D2DD))
  else Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White))
 }
}

private class PuzzleItLayout(w:Float,h:Float){
 val tile=min(w/4.35f,h/4.55f).coerceAtLeast(1f)
 val boardX=(w-3*tile)/2f
 val boardY=(h-3*tile)/2f
 val scatterX=(w-3.24f*tile)/2f
 val scatterY=(h-3.24f*tile)/2f
}

private fun puzzlePath(round:PuzzleItRound,index:Int,t:Float):Path {
 val row=index/3;val col=index%3
 val top=if(row==0)0 else -round.edges[6+(row-1)*3+col]
 val right=if(col==2)0 else round.edges[row*2+col]
 val bottom=if(row==2)0 else round.edges[6+row*3+col]
 val left=if(col==0)0 else -round.edges[row*2+col-1]
 val path=Path()
 path.moveTo(0f,0f)
 fun edge(x:Float,y:Float,dx:Float,dy:Float,polarity:Int) {
  if(polarity==0){path.lineTo(x+dx,y+dy);return}
  val nx=dy*polarity*.21f;val ny=-dx*polarity*.21f
  fun pos(f:Float)=Pair(x+dx*f,y+dy*f)
  val a=pos(.33f);val b=pos(.67f)
  path.lineTo(a.first,a.second)
  path.cubicTo(x+dx*.34f+nx*.15f,y+dy*.34f+ny*.15f,
   x+dx*.30f+nx,y+dy*.30f+ny,
   x+dx*.50f+nx,y+dy*.50f+ny)
  path.cubicTo(x+dx*.70f+nx,y+dy*.70f+ny,
   x+dx*.66f+nx*.15f,y+dy*.66f+ny*.15f,
   b.first,b.second)
  path.lineTo(x+dx,y+dy)
 }
 edge(0f,0f,t,0f,top)
 edge(t,0f,0f,t,right)
 edge(t,t,-t,0f,bottom)
 edge(0f,t,0f,-t,left)
 path.close()
 return path
}

private fun containsPuzzlePiece(round:PuzzleItRound,i:Int,at:Offset,l:PuzzleItLayout,p:PuzzleItPiece):Boolean {
 val x=l.scatterX+p.left*l.tile;val y=l.scatterY+p.top*l.tile
 val path=puzzlePath(round,i,l.tile)
 val region=Region()
 val bound=Rect(-l.tile.toInt(),-l.tile.toInt(),(l.tile*2).toInt(),(l.tile*2).toInt())
 region.setPath(path,Region(bound))
 return region.contains((at.x-x).roundToInt(),(at.y-y).roundToInt())
}

private fun drawPuzzlePiece(canvas:android.graphics.Canvas,round:PuzzleItRound,i:Int,x:Float,y:Float,t:Float) {
 val checkpoint=canvas.save()
 canvas.translate(x,y)
 val path=puzzlePath(round,i,t)
 canvas.clipPath(path)
 // Repaint exactly the same global motif under every local clip.
 canvas.translate(-(i%3)*t,-(i/3)*t)
 val base=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=round.color }
 canvas.drawRect(0f,0f,3*t,3*t,base)
 val ink=Paint(Paint.ANTI_ALIAS_FLAG).apply {
  color=android.graphics.Color.rgb(45,54,95)
  textAlign=Paint.Align.CENTER
  typeface=android.graphics.Typeface.create("sans-serif-black",android.graphics.Typeface.BOLD)
 }
 val word=round.number.toString()
 ink.textSize=3.0f*t
 val glyph=Rect()
 ink.getTextBounds(word,0,word.length,glyph)
 val targetHeight=2.31f*t
 val maxWidth=2.80f*t
 val heightScale=targetHeight/glyph.height().coerceAtLeast(1)
 val widthScale=maxWidth/ink.measureText(word).coerceAtLeast(1f)
 ink.textSize*=min(heightScale,widthScale)
 ink.getTextBounds(word,0,word.length,glyph)
 val baseline=2.52f*t-glyph.bottom
 canvas.drawText(word,1.5f*t,baseline,ink)
 val dot=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.rgb(45,54,95)}
 val n=round.number
 val first=min(n,5)
 val second=n-first
 fun dots(count:Int,y:Float){
  val spacing=.17f*t
  repeat(count){j->canvas.drawCircle(1.5f*t+(j-(count-1)/2f)*spacing,y,.043f*t,dot)}
 }
 if(second==0)dots(first,2.85f*t)
 else {dots(first,2.77f*t);dots(second,2.92f*t)}
 canvas.restoreToCount(checkpoint)
 val border=Paint(Paint.ANTI_ALIAS_FLAG).apply {
  style=Paint.Style.STROKE;color=android.graphics.Color.rgb(103,111,150)
  strokeWidth=1.25f
 }
 val outline=canvas.save();canvas.translate(x,y);canvas.drawPath(path,border);canvas.restoreToCount(outline)
}

private fun encodePuzzleIt(c:PuzzleItController):String {
 val r=c.round
 return JSONObject().put("seed",r.seed).put("number",r.number).put("color",r.color)
  .put("edges",JSONArray(r.edges)).put("answers",JSONArray(r.answers))
  .put("pieces",JSONArray(r.pieces.map{JSONArray(listOf(it.left.toDouble(),it.top.toDouble(),it.fixed))}))
  .put("disabled",JSONArray(r.disabled.toList())).put("phase",r.phase.name)
  .put("stop",c.stopAfterCurrentRound).put("completed",c.completedRounds).toString()
}
private fun decodePuzzleIt(raw:String,c:PuzzleItController) {
 val o=JSONObject(raw)
 fun integers(name:String):List<Int>{val a=o.getJSONArray(name);return (0 until a.length()).map{a.getInt(it)}}
 val pieces=o.getJSONArray("pieces")
 val list=(0 until pieces.length()).map{i->
  val a=pieces.getJSONArray(i)
  PuzzleItPiece(i,a.getDouble(0).toFloat(),a.getDouble(1).toFloat(),a.getBoolean(2))
 }
 val r=PuzzleItRound(o.getLong("seed"),o.getInt("number"),o.getInt("color"),
  integers("edges"),integers("answers"),list,integers("disabled").toSet(),
  PuzzleItPhase.valueOf(o.getString("phase")))
 c.replace(r,o.getBoolean("stop"),o.getInt("completed"))
}
