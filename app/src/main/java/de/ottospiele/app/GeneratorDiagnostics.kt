// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.app

import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

/** Bounded, on-device generator diagnostics for debug builds only. */
internal object GeneratorDiagnostics {
 private const val PREF = "arrow_generator_diagnostics"
 private const val KEY = "events_v1"
 private const val LIMIT = 300
 @Volatile private var appContext:Context?=null
 fun initialize(context:Context){appContext=context.applicationContext}
 fun record(event:Event){appContext?.let { record(it,event) }}

 data class Event(val timeMs:Long,val origin:String,val stage:Int,val seed:Long,
  val width:Int,val height:Int,val elapsedMs:Long,val generator:String,val outcome:String)

 @Synchronized fun record(context:Context,event:Event) {
  if(context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE == 0) return
  val prefs=context.applicationContext.getSharedPreferences(PREF,Context.MODE_PRIVATE)
  val existing=runCatching { JSONArray(prefs.getString(KEY,"[]")) }.getOrElse { JSONArray() }
  val updated=JSONArray()
  val start=(existing.length()-(LIMIT-1)).coerceAtLeast(0)
  for(index in start until existing.length())updated.put(existing.optJSONObject(index))
  updated.put(JSONObject().put("timeMs",event.timeMs).put("origin",event.origin)
   .put("stage",event.stage).put("seed",event.seed).put("width",event.width)
   .put("height",event.height).put("elapsedMs",event.elapsedMs)
   .put("generator",event.generator).put("outcome",event.outcome))
  prefs.edit().putString(KEY,updated.toString()).apply()
 }

 @Synchronized fun entries(context:Context):List<Event> {
  val prefs=context.applicationContext.getSharedPreferences(PREF,Context.MODE_PRIVATE)
  return runCatching {
   val data=JSONArray(prefs.getString(KEY,"[]"))
   (0 until data.length()).mapNotNull { i ->
    val obj=data.optJSONObject(i)?:return@mapNotNull null
    Event(obj.getLong("timeMs"),obj.getString("origin"),obj.getInt("stage"),
     obj.getLong("seed"),obj.getInt("width"),obj.getInt("height"),
     obj.getLong("elapsedMs"),obj.getString("generator"),obj.getString("outcome"))
   }
  }.getOrDefault(emptyList())
 }

 @Synchronized fun clear(context:Context) {
  context.applicationContext.getSharedPreferences(PREF,Context.MODE_PRIVATE)
   .edit().remove(KEY).apply()
 }

 fun csv(events:List<Event>):String = buildString {
  appendLine("timestamp_ms,origin,stage,seed,width,height,elapsed_ms,generator,outcome")
  for(e in events)appendLine(listOf(e.timeMs,e.origin,e.stage,e.seed,e.width,
   e.height,e.elapsedMs,e.generator,e.outcome).joinToString(","))
 }

 /** Android shares CSV text without external storage permission or a content provider. */
 fun share(context:Context,events:List<Event>) {
  val intent=Intent(Intent.ACTION_SEND).apply {
   type="text/csv"
   putExtra(Intent.EXTRA_SUBJECT,"Arrow Puzzle – Generator-Diagnose.csv")
   putExtra(Intent.EXTRA_TEXT,csv(events))
  }
  context.startActivity(Intent.createChooser(intent,"Generator-Diagnose teilen"))
 }
}
