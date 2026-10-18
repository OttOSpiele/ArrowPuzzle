// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.app

import android.content.Context
import android.util.Log
import de.ottospiele.arrow.*
import org.json.JSONArray
import org.json.JSONObject

internal data class SavedSession(
 val progress:Progress, val lifeSetting:Int, val hintSetting:Int,
 val concurrent:Boolean, val speed:Int, val game:Game,
 val win:Boolean, val defeat:Boolean
)

internal object LocalSave {
 private const val NAME="arrow_puzzle_local"
 private const val KEY="session_v2"
 private const val TAG="ArrowPuzzleSave"
 fun read(context:Context):SavedSession? {
  val json=context.getSharedPreferences(NAME,Context.MODE_PRIVATE).getString(KEY,null)?:return null
  return try {
   val root=JSONObject(json)
   require(root.getInt("version")==2)
   val stage=root.getInt("stage")
   val progress=Progress(stage,root.getInt("done"))
   require(stage in 1..100000 && progress.done in 0 until progress.required())
   val lifeSetting=root.getInt("lifeSetting")
   val hintSetting=root.getInt("hintSetting")
   require(lifeSetting in listOf(3,5,10) && hintSetting in listOf(0,1,3,5))
   val activeLives=root.optInt("activeMaxLives",lifeSetting)
   val activeHints=root.optInt("activeMaxHints",hintSetting)
   require(activeLives in listOf(3,5,10) && activeHints in listOf(0,1,3,5))
   val size=root.getInt("size")
   require(size in 10..80)
   val savedArrows=root.getJSONArray("arrows")
   require(savedArrows.length() in 1..60)
   val arrows=(0 until savedArrows.length()).map{index->
    val obj=savedArrows.getJSONObject(index)
    val bendArray=obj.getJSONArray("bends")
    val bends=(0 until bendArray.length()).map{
     val pt=bendArray.getJSONArray(it)
     Pt(pt.getInt(0),pt.getInt(1))
    }
    Arrow(obj.getInt("id"),obj.getInt("x"),obj.getInt("y"),
     obj.getInt("length"),Dir.valueOf(obj.getString("direction")),bends)
   }
   require(arrows.map{it.id}.toSet().size==arrows.size)
   val level=Level(root.getLong("seed"),stage,size,arrows,root.getInt("height"),root.optBoolean("fallbackGeneration",false))
   require(Rules.solve(level)!=null)
   val remainingArray=root.getJSONArray("remaining")
   val remaining=(0 until remainingArray.length()).map{remainingArray.getInt(it)}.toSet()
   require(remaining.size==remainingArray.length())
   val mistakesObject=root.getJSONObject("mistakes")
   val mistakes=mutableMapOf<Int,Set<Int>>()
   for(key in mistakesObject.keys()){
    val values=mistakesObject.getJSONArray(key)
    mistakes[key.toInt()]=(0 until values.length()).map{values.getInt(it)}.toSet()
   }
   val game=Game(level,activeLives,activeHints)
   game.restore(remaining,root.getInt("remainingLives"),root.getInt("remainingHints"),mistakes)
   SavedSession(progress,lifeSetting,hintSetting,
    root.optBoolean("concurrent",true),root.optInt("speed",1).coerceIn(0,3),
    game,root.optBoolean("win",false)||game.won,
    root.optBoolean("defeat",false)||game.lost)
  }catch(error:Exception){
   Log.w(TAG,"Puzzle snapshot could not be restored",error)
   // Preserve the child's progress and settings even when board data is damaged.
   try{
    val root=JSONObject(json)
    val stage=root.optInt("stage",1).coerceIn(1,100000)
    val progress=Progress(stage,root.optInt("done",0).coerceIn(0,Progress(stage).required()-1))
    val defaults=GameDefaults.load(context)
    val life=root.optInt("lifeSetting",defaults.lives).takeIf{it in listOf(3,5,10)}?:defaults.lives
    val hint=root.optInt("hintSetting",defaults.hints).takeIf{it in listOf(0,1,3,5)}?:defaults.hints
    val newGame=Game(Generator().generate(stage,java.util.Random().nextLong()),life,hint)
    SavedSession(progress,life,hint,root.optBoolean("concurrent",defaults.concurrentArrows),
     root.optInt("speed",defaults.animationSpeed).coerceIn(0,3),newGame,false,false)
   }catch(secondary:Exception){
    Log.w(TAG,"Cannot recover stage/settings either",secondary)
    null
   }
  }
 }
 fun write(context:Context,progress:Progress,lifeSetting:Int,hintSetting:Int,
  concurrent:Boolean,speed:Int,game:Game,win:Boolean,defeat:Boolean) {
  val root=JSONObject()
  root.put("version",2).put("stage",progress.stage).put("done",progress.done)
   .put("lifeSetting",lifeSetting).put("hintSetting",hintSetting)
   .put("concurrent",concurrent).put("speed",speed)
   .put("seed",game.level.seed).put("size",game.level.size).put("height",game.level.height)
   .put("fallbackGeneration",game.level.fallbackGeneration)
   .put("activeMaxLives",game.maxLives).put("activeMaxHints",game.maxHints)
   .put("remainingLives",game.lives).put("remainingHints",game.hints)
   .put("win",win).put("defeat",defeat)
  val arrows=JSONArray()
  game.level.arrows.forEach{a->
   val bends=JSONArray()
   a.bends.forEach{p->bends.put(JSONArray().put(p.x).put(p.y))}
   arrows.put(JSONObject().put("id",a.id).put("x",a.x).put("y",a.y)
    .put("length",a.length).put("direction",a.dir.name).put("bends",bends))
  }
  root.put("arrows",arrows)
  val remaining=JSONArray()
  game.remaining.forEach{remaining.put(it.id)}
  root.put("remaining",remaining)
  val mistakes=JSONObject()
  game.mistakes.forEach{(id,blockers)->
   val values=JSONArray()
   blockers.forEach{values.put(it)}
   mistakes.put(id.toString(),values)
  }
  root.put("mistakes",mistakes)
  val success=context.getSharedPreferences(NAME,Context.MODE_PRIVATE).edit()
   .putString(KEY,root.toString()).commit()
  if(!success)Log.e(TAG,"Saving puzzle state failed")
 }
}
