// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.app

import kotlin.math.hypot
import kotlin.random.Random

/** All logical positions are expressed in tile-width units, independent of screen density. */
internal enum class PuzzleItPhase { GENERATING, SCATTERING, PUZZLE_ACTIVE, PUZZLE_COMPLETE, CHOICE_ACTIVE, ROUND_SUCCESS, ROUND_FINISHING, EXITING }
internal data class PuzzleItConfig(
 val snapRatio:Float=.15f,
 val successMs:Long=1000L,
 val minVisibleRatio:Float=.40f,
 val maxAttempts:Int=100,
 val preventRepeat:Boolean=true
)
internal data class PuzzleItPiece(
 val index:Int,
 val left:Float,
 val top:Float,
 val fixed:Boolean=false
)
internal data class PuzzleItRound(
 val seed:Long,
 val number:Int,
 val color:Int,
 /** 12 shared internal-edge orientations: 6 vertical, then 6 horizontal. */
 val edges:List<Int>,
 val answers:List<Int>,
 val pieces:List<PuzzleItPiece>,
 val disabled:Set<Int> = emptySet(),
 val phase:PuzzleItPhase = PuzzleItPhase.PUZZLE_ACTIVE
)

/** No view/device dependencies: round generation is repeatable for a given seed. */
internal object PuzzleItGenerator {
 val colors=listOf(0xFFF9D46B.toInt(),0xFFA9E3CF.toInt(),0xFFB9D7FC.toInt(),
  0xFFF5B8C8.toInt(),0xFFD9C5F6.toInt(),0xFFFFD5AE.toInt())
 fun generate(seed:Long,previous:Int?=null,config:PuzzleItConfig=PuzzleItConfig()):PuzzleItRound {
  val random=Random(seed)
  val numbers=(1..10).filter { !config.preventRepeat || it!=previous }
  val number=numbers[random.nextInt(numbers.size)]
  val wrong=(1..10).filter{it!=number}.sortedWith(compareBy<Int>{kotlin.math.abs(it-number)}.thenBy{it})
   .take(5).shuffled(random).take(3)
  val options=(wrong+number).shuffled(random)
  val edges=List(12){if(random.nextBoolean())1 else -1}
  // Non-overlapping 3x3 scatter slots: each position is reachable, even on narrow screens.
  // Shuffle a derangement; none of the pieces starts at its finished coordinate.
  val slots=(0..8).toMutableList()
  var nonSolved=false
  repeat(config.maxAttempts.coerceIn(1,1000)) {
   if(!nonSolved) {
    slots.shuffle(random)
    nonSolved=slots.indices.none{slots[it]==it}
   }
  }
  if(!nonSolved) {
   // Guaranteed finite fallback: one cyclic shift, no tile starts in its own slot.
   for(i in 0..8)slots[i]=(i+1)%9
  }
  val pieces=slots.mapIndexed { i,slot ->
   PuzzleItPiece(i,(slot%3)*1.12f,(slot/3)*1.12f)
  }
  return PuzzleItRound(seed,number,colors[random.nextInt(colors.size)],edges,options,pieces)
 }
 fun attemptSnap(piece:PuzzleItPiece,x:Float,y:Float,targetX:Float,targetY:Float,ratio:Float):PuzzleItPiece =
  if(hypot((x-targetX).toDouble(),(y-targetY).toDouble())<=ratio)
   piece.copy(left=targetX,top=targetY,fixed=true) else piece.copy(left=x,top=y)
 fun answer(round:PuzzleItRound,value:Int):PuzzleItRound {
  if(round.phase!=PuzzleItPhase.CHOICE_ACTIVE || value !in round.answers || value in round.disabled)return round
  return if(value==round.number)round.copy(phase=PuzzleItPhase.ROUND_SUCCESS)
  else round.copy(disabled=round.disabled+value)
 }
}

internal class PuzzleItController(val config:PuzzleItConfig, seed:Long) {
 var round:PuzzleItRound=PuzzleItGenerator.generate(seed)
  private set
 var stopAfterCurrentRound:Boolean=false
  private set
 var completedRounds:Int=0
  private set
 fun requestStopAfterCurrentRound(){stopAfterCurrentRound=true}
 fun replace(snapshot:PuzzleItRound,stopping:Boolean,completed:Int) {
  round=snapshot;stopAfterCurrentRound=stopping;completedRounds=completed
 }
 fun move(index:Int,x:Float,y:Float,snap:Boolean):Boolean {
  if(round.phase!=PuzzleItPhase.PUZZLE_ACTIVE)return false
  val p=round.pieces.getOrNull(index)?:return false
  if(p.fixed)return false
  val changed=if(snap)PuzzleItGenerator.attemptSnap(p,x,y,(index%3).toFloat(),(index/3).toFloat(),config.snapRatio)
   else p.copy(left=x,top=y)
  val pieces=round.pieces.toMutableList()
  pieces[index]=changed
  val complete=pieces.all{it.fixed}
  round=round.copy(pieces=pieces,phase=if(complete)PuzzleItPhase.CHOICE_ACTIVE else PuzzleItPhase.PUZZLE_ACTIVE)
  return changed.fixed
 }
 fun choose(value:Int):Boolean {
  val old=round.phase
  round=PuzzleItGenerator.answer(round,value)
  return old==PuzzleItPhase.CHOICE_ACTIVE && round.phase==PuzzleItPhase.ROUND_SUCCESS
 }
 /** Invoked once, following the nonblocking success animation. */
 fun finishRound(preparationReady:Boolean,requiredRounds:Int):Boolean {
  if(round.phase!=PuzzleItPhase.ROUND_SUCCESS)return false
  round=round.copy(phase=PuzzleItPhase.ROUND_FINISHING)
  completedRounds++
  if(preparationReady || stopAfterCurrentRound || completedRounds>=requiredRounds){
   round=round.copy(phase=PuzzleItPhase.EXITING)
   return true
  }
  round=PuzzleItGenerator.generate(round.seed+1,round.number,config)
  return false
 }
}
