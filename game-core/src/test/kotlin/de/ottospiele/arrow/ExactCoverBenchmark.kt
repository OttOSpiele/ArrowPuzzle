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
  val lines=mutableListOf("size,seed,node_budget,visited_nodes,elapsed_ms,success,exhausted,arrows,occupancy_pct,initially_free,longest_chain,quality,error,requested_arrows,requested_windings,actual_windings,max_initially_free,min_chain_length")
  val drawings=mutableListOf<String>()
  val profileRows=mutableListOf("board,seed,mode,profile_stage,free,chain,windings,free_fit,chain_fit,winding_fit,all_fit,fit_score")
  var success=0
  for(size in boards)for(index in 0 until seeds)for(target in listOf<Int?>(null,size*size/4).distinct()) {
   val windingGoal=if(target!=null)2 else 0
   val freeGoal=if(target!=null)target/2 else null
   val chainGoal=if(target!=null) (size/2) else 0
   val seed=17L+index*7919L
   val started=System.nanoTime()
   try {
    val result=ExactCoverGenerator.generate(size,size,seed,stage=35,maxNodes=budget,targetArrows=target,minWindingArrows=windingGoal,maxInitiallyFree=freeGoal,minChainLength=chainGoal)
    val elapsed=(System.nanoTime()-started)/1_000_000
    val level=result.level
    if(level!=null) {
     require(Rules.solve(level)!=null)
     val q=LevelQualityAnalyzer.analyze(level)
     fun fmt(value:Double)=String.format(Locale.ROOT,"%.3f",value)
     lines.add(listOf(size,seed,budget,result.nodes,elapsed,true,false,level.arrows.size,
      fmt(q.occupancy*100),q.initiallyFree,q.longestChain,fmt(q.score(35)),"",target?:"",windingGoal,level.arrows.count { it.vertices().size>=5 },freeGoal?:"",chainGoal).joinToString(","))
     val paths=level.arrows.joinToString("|") { a ->
      a.vertices().joinToString(";") { p -> "${p.x}:${p.y}" }
     }
     drawings.add(listOf(35,seed,1f,size,size,false,
      fmt(q.occupancy*100),q.initiallyFree,q.longestChain,fmt(q.score(35)),paths)
      .joinToString("\t"))
     for(profileStage in listOf(1,8,16,35,60,80)) {
      val assessment=DifficultyProfiles.forStage(profileStage).assess(level)
      profileRows.add(listOf(size,seed,if(target==null)"free" else "target",
       profileStage,assessment.free,assessment.chain,assessment.windings,
       assessment.freeFit,assessment.chainFit,assessment.windingFit,
       assessment.withinAllRanges,fmt(assessment.score)).joinToString(","))
     }
     success++
    } else {
     lines.add(listOf(size,seed,budget,result.nodes,elapsed,false,result.exhausted,"","","","","","",target?:"",windingGoal,"",freeGoal?:"",chainGoal).joinToString(","))
    }
   } catch(error:Exception) {
    val elapsed=(System.nanoTime()-started)/1_000_000
    val message=(error::class.simpleName+":"+error.message).replace(',',';').replace('\n',' ')
    lines.add(listOf(size,seed,budget,"",elapsed,false,false,"","","","","",message,target?:"",windingGoal,"",freeGoal?:"",chainGoal).joinToString(","))
   }
  }
  File(directory,"exact-cover-difficulty.csv").writeText(profileRows.joinToString("\n")+"\n")
  val profileData=profileRows.drop(1).map { it.split(",") }
  val report=buildString {
   appendLine("Difficulty profile evaluation (same generated boards scored at reference stages)")
   appendLine("These are profile-fit diagnostics, not actual consecutive game-stage levels.")
   for(stage in listOf(1,8,16,35,60,80)) {
    val selected=profileData.filter { it[3]==stage.toString() }
    val fitted=selected.count { it[10]=="true" }
    val mean=selected.map { it[11].toDouble() }.average()
    appendLine("stage=$stage total=${selected.size} all_fit=$fitted mean_score=${String.format(Locale.ROOT,"%.2f",mean)}")
   }
   // Difference in fit of each fixed board between adjacent reference stages.
   val jumps=profileData.groupBy { it[0]+"|"+it[1]+"|"+it[2] }.values
    .flatMap { group ->
     group.sortedBy { it[3].toInt() }.zipWithNext().map { (before,after) ->
      kotlin.math.abs(after[11].toDouble()-before[11].toDouble())
     }
    }
   appendLine("max_adjacent_reference_fit_delta=${String.format(Locale.ROOT,"%.2f",jumps.maxOrNull()?:0.0)}")
  }
  File(directory,"exact-cover-difficulty-summary.txt").writeText(report)
  println(report)
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
   appendLine("Constrained runs request exactly area/4 arrows, >=2 windings, initially free <= target/2, and chain >= size/2.")
   appendLine("See exact-cover.csv for elapsed time, explored nodes and failures.")
  }
  File(directory,"exact-cover-summary.txt").writeText(summary)
  println(summary)
 }
}
