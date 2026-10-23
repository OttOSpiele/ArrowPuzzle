// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.app

import android.content.Context
import de.ottospiele.arrow.*
import org.json.JSONArray
import org.json.JSONObject

/** Validated levels are stored separately from the child's active game snapshot. */
internal object PreparedLevels {
 private const val PREF="arrow_puzzle_prepared"
 private const val VERSION=2
 private fun key(stage:Int,aspect:Float)="stage_${stage}_${(aspect*100).toInt()}"
 @Synchronized fun read(context:Context,stage:Int,aspect:Float):List<Level> {
  return runCatching {
   val json=context.getSharedPreferences(PREF,Context.MODE_PRIVATE)
    .getString(key(stage,aspect),null)?:return emptyList()
   val root=JSONObject(json)
   require(root.getInt("version")==VERSION)
   require(root.getInt("stage")==stage)
   val items=root.getJSONArray("levels")
   (0 until items.length()).map { index->
    val item=items.getJSONObject(index)
    val arrows=item.getJSONArray("arrows")
    val list=(0 until arrows.length()).map { i->
     val a=arrows.getJSONObject(i)
     val bends=a.getJSONArray("bends")
     Arrow(a.getInt("id"),a.getInt("x"),a.getInt("y"),a.getInt("length"),
      Dir.valueOf(a.getString("dir")),(0 until bends.length()).map { n->
       val point=bends.getJSONArray(n)
       Pt(point.getInt(0),point.getInt(1))
      })
    }
    Level(item.getLong("seed"),stage,item.getInt("width"),list,item.getInt("height"),item.optBoolean("fallbackGeneration",false))
     .also { require(Rules.solve(it)!=null) }
   }
  }.getOrElse { emptyList() }
 }
 /** Claim one valid prepared puzzle and remove it before the next win can reuse it. */
 @Synchronized fun take(context:Context,stage:Int,aspect:Float,excludeSeed:Long):Level? {
  val cached=read(context,stage,aspect)
  val chosen=cached.firstOrNull { it.seed!=excludeSeed } ?: return null
  write(context,stage,aspect,cached.filterNot { it.seed==chosen.seed })
  return chosen
 }
 @Synchronized fun write(context:Context,stage:Int,aspect:Float,levels:List<Level>) {
  require(levels.all { it.stage==stage && Rules.solve(it)!=null })
  val root=JSONObject().put("version",VERSION).put("stage",stage)
  val items=JSONArray()
  for(level in levels) {
   val arrows=JSONArray()
   for(a in level.arrows) {
    val bends=JSONArray()
    a.bends.forEach { p -> bends.put(JSONArray().put(p.x).put(p.y)) }
    arrows.put(JSONObject().put("id",a.id).put("x",a.x).put("y",a.y)
     .put("length",a.length).put("dir",a.dir.name).put("bends",bends))
   }
   items.put(JSONObject().put("seed",level.seed).put("width",level.width)
    .put("height",level.height).put("fallbackGeneration",level.fallbackGeneration)
    .put("arrows",arrows))
  }
  root.put("levels",items)
  context.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit()
   .putString(key(stage,aspect),root.toString()).apply()
 }
}
