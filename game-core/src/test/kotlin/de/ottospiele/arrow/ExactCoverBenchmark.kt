// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.arrow

import java.io.File
import java.util.Locale

/**
 * Dedicated bounded CI experiment. Records unsuccessful searches as results,
 * never silently substitutes the legacy generator.
 */
object ExactCoverBenchmark {
 @JvmStatic fun main(args:Array<String>) {
  val directory=File(args.getOrNull(0)?:"build/benchmark").apply{mkdirs()}
  val seeds=(args.getOrNull(1)?.toIntOrNull()?:2).coerceIn(1,20)
  val budget=(args.getOrNull(2)?.toIntOrNull()?:600).coerceIn(1,100000)
  val boards=listOf(8,12,16,20)
  val lines=mutableListOf("size,seed,node_budget,visited_nodes,elapsed_ms,success,exhausted,arrows,occupancy_pct,initially_free,longest_chain,quality,error")
  var success=0
  for(size in boards)for(index in 0 until seeds) {
   val seed=17L+index*7919L
   val started=System.nanoTime()
   try {
    val result=ExactCoverGenerator.generate(size,size,seed,stage=35,maxNodes=budget)
    val elapsed=(System.nanoTime()-started)/1_000_000
    val level=result.level
    if(level!=null) {
     require(Rules.solve(level)!=null)
     val q=LevelQualityAnalyzer.analyze(level)
     fun fmt(value:Double)=String.format(Locale.ROOT,"%.3f",value)
     lines.add(listOf(size,seed,budget,result.nodes,elapsed,true,false,level.arrows.size,
      fmt(q.occupancy*100),q.initiallyFree,q.longestChain,fmt(q.score(35)),"").joinToString(","))
     success++
    } else {
     lines.add("$size,$seed,$budget,${result.nodes},$elapsed,false,${result.exhausted},,,,,,")
    }
   } catch(error:Exception) {
    val elapsed=(System.nanoTime()-started)/1_000_000
    val message=(error::class.simpleName+":"+error.message).replace(',',';').replace('\n',' ')
    lines.add("$size,$seed,$budget,,$elapsed,false,false,,,,,,$message")
   }
  }
  File(directory,"exact-cover.csv").writeText(lines.joinToString("\n")+"\n")
  val summary=buildString {
   appendLine("Independent Exact-Cover Generator")
   appendLine("sizes=${boards.joinToString(",")} seeds_per_size=$seeds node_budget=$budget")
   appendLine("completed=$success attempted=${boards.size*seeds}")
   for(size in boards) {
    val subset=lines.drop(1).filter{it.substringBefore(',')==size.toString()}
    val passed=subset.count{it.split(',').getOrNull(5)=="true"}
    appendLine("${size}x$size: $passed/${subset.size} succeeded")
   }
   appendLine("See exact-cover.csv for elapsed time, explored nodes and failures.")
  }
  File(directory,"exact-cover-summary.txt").writeText(summary)
  println(summary)
 }
}
