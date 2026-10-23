// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.app

import android.content.Context
import java.util.Properties

/** Initial gameplay values. Saved player preferences always take precedence. */
internal data class GameDefaults(
 val previewHoldMillis:Long=400L,
 val lives:Int=5,
 val hints:Int=3,
 val concurrentArrows:Boolean=true,
 val animationSpeed:Int=1,
 val preparationExactAttemptsPerSlot:Int=8,
 val legacyEnabled:Boolean=true,
 val showStageAdjustment:Boolean=true,
 val puzzleIt:PuzzleItConfig=PuzzleItConfig()
) {
 companion object {
  fun load(context:Context):GameDefaults {
   val props=Properties()
   val loaded=runCatching {
    context.assets.open("game-defaults.properties").use { props.load(it) }
   }.isSuccess
   if(!loaded)return GameDefaults()
   fun int(key:String,valid:Set<Int>,fallback:Int):Int =
    props.getProperty(key)?.trim()?.toIntOrNull()?.takeIf { it in valid } ?: fallback
   val preview=props.getProperty("arrowPreviewHoldMillis")?.trim()?.toLongOrNull()
    ?.takeIf { it in 100L..5000L } ?: 400L
   val concurrent=when(props.getProperty("defaultConcurrentArrows")?.trim()?.lowercase()){
    "true" -> true
    "false" -> false
    else -> true
   }
   fun floatRange(key:String,min:Float,max:Float,def:Float)=
    props.getProperty(key)?.trim()?.toFloatOrNull()?.takeIf{it in min..max}?:def
   fun longRange(key:String,min:Long,max:Long,def:Long)=
    props.getProperty(key)?.trim()?.toLongOrNull()?.takeIf{it in min..max}?:def
   fun intRange(key:String,min:Int,max:Int,def:Int)=
    props.getProperty(key)?.trim()?.toIntOrNull()?.takeIf{it in min..max}?:def
   fun boolean(key:String,def:Boolean)=when(props.getProperty(key)?.trim()?.lowercase()){
    "true"->true
    "false"->false
    else->def
   }
   return GameDefaults(
    previewHoldMillis=preview,
    lives=int("defaultLives",setOf(3,5,10),5),
    hints=int("defaultHints",setOf(0,1,3,5),3),
    concurrentArrows=concurrent,
    animationSpeed=int("defaultAnimationSpeed",setOf(0,1,2,3),1),
    preparationExactAttemptsPerSlot=props.getProperty("preparationExactAttemptsPerSlot")?.trim()?.toIntOrNull()
     ?.takeIf { it in 1..12 } ?: 8,
    legacyEnabled=when(props.getProperty("legacyEnabled")?.trim()?.lowercase()){
     "true"->true
     "false"->false
     else->true
    },
    showStageAdjustment=boolean("showStageAdjustment",true),
    puzzleIt=PuzzleItConfig(
     snapRatio=floatRange("puzzle.snapToleranceRatio",.05f,.4f,.15f),
     successMs=longRange("puzzle.successAnimationMs",800L,1200L,1000L),
     minVisibleRatio=floatRange("puzzle.minVisibleAreaRatio",.25f,.9f,.40f),
     maxAttempts=intRange("puzzle.maxPlacementAttempts",1,1000,100),
     preventRepeat=boolean("puzzle.preventImmediateNumberRepeat",true)
    )
   )
  }
 }
}
