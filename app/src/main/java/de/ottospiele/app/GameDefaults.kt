package de.ottospiele.app

import android.content.Context
import java.util.Properties

/** Initial gameplay values. Saved player preferences always take precedence. */
internal data class GameDefaults(
 val previewHoldMillis:Long=400L,
 val lives:Int=5,
 val hints:Int=3,
 val concurrentArrows:Boolean=true,
 val animationSpeed:Int=1
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
   return GameDefaults(
    previewHoldMillis=preview,
    lives=int("defaultLives",setOf(3,5,10),5),
    hints=int("defaultHints",setOf(0,1,3,5),3),
    concurrentArrows=concurrent,
    animationSpeed=int("defaultAnimationSpeed",setOf(0,1,2,3),1)
   )
  }
 }
}
