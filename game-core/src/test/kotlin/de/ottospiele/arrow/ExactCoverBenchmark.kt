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
  val lines=mutableListOf("size,seed,node_budget,visited_nodes,elapsed_ms,success,exhausted,arrows,occupancy_pct,initially_free,longest_chain,quality,error,requested_arrows")
  val drawings=mutableListOf<String>()
  var success=0
  for(size in boards)for(index in 0 until seeds)for(target in listOf<Int?>(null,if(size<=12) size*size/4 else null).distinct()) {
   val seed=17L+index*7919L
   val started=System.nanoTime()
   try {
    val result=ExactCoverGenerator.generate(size,size,seed,stage=35,maxNodes=budget,targetArrows=target)
    val elapsed=(System.nanoTime()-started)/1_000_000
    val level=result.level
    if(level!=null) {
     require(Rules.solve(level)!=null)
     val q=LevelQualityAnalyzer.analyze(level)
     fun fmt(value:Double)=String.format(Locale.ROOT,"%.3f",value)
     lines.add(listOf(size,seed,budget,result.nodes,elapsed,true,false,level.arrows.size,
      fmt(q.occupancy*100),q.initiallyFree,q.longestChain,fmt(q.score(35)),"",target?:"").joinToString(","))
     val paths=level.arrows.joinToString("|") { a ->
      a.vertices().joinToString(";") { p -> "${p.x}:${p.y}" }
     }
     drawings.add(listOf(35,seed,1f,size,size,false,
      fmt(q.occupancy*100),q.initiallyFree,q.longestChain,fmt(q.score(35)),paths)
      .joinToString("\t"))
     success++
    } else {
     lines.add(listOf(size,seed,budget,result.nodes,elapsed,false,result.exhausted,"","","","","","",target?:"").joinToString(","))
    }
   } catch(error:Exception) {
    val elapsed=(System.nanoTime()-started)/1_000_000
    val message=(error::class.simpleName+":"+error.message).replace(',',';').replace('\n',' ')
    lines.add(listOf(size,seed,budget,"",elapsed,false,false,"","","","","",message,target?:"").joinToString(","))
   }
  }
  File(directory,"exact-cover-boards.tsv").writeText(
   "stage\tseed\taspect\twidth\theight\tfallback\toccupancy_pct\tfree\tchain\tquality\tpaths\n"+
   drawings.joinToString("\n")+"\n")
  File(directory,"exact-cover.csv").writeText(lines.joinToString("\n")+"\n")
  val summary=buildString {
   appendLine("Independent Exact-Cover Generator")
   appendLine("sizes=${boards.joinToString(",")} seeds_per_size=$seeds node_budget=$budget")
   appendLine("completed=$success attempted=${lines.size-1}")
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
