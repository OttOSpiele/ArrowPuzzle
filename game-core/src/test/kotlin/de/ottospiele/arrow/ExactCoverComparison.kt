// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.arrow

import java.io.File
import java.util.Locale

/** Same runner, inputs, and JVM for historical build #221 and current solver. */
object ExactCoverComparison {
 @JvmStatic fun main(args:Array<String>) {
  val directory=File(args.getOrNull(0)?:"build/benchmark").apply{mkdirs()}
  val seedCount=(args.getOrNull(1)?.toIntOrNull()?:10).coerceIn(1,25)
  val budgets=listOf(600,3000)
  val rows=mutableListOf("solver,board,seed,mode,target_arrows,winding_min,free_max,chain_min,budget,success,exhausted,nodes,elapsed_ms,arrows,initially_free,longest_chain,windings,failure_reason")
  for(size in listOf(8,12,16,20))for(i in 0 until seedCount)for(constrained in listOf(false,true))for(budget in budgets) {
   val seed=17L+i*7919L
   val target=if(constrained)size*size/4 else null
   val winding=if(constrained)2 else 0
   val free=if(constrained)target!!/2 else null
   val chain=if(constrained)size/2 else 0
   for(solver in listOf("baseline221","current")) {
    val began=System.nanoTime()
    try {
     val result=if(solver=="baseline221") {
      val r=ExactCoverBaseline221.generate(size,size,seed,35,budget,target,winding,free,chain)
      Triple(r.level,r.nodes,r.exhausted)
     } else {
      val r=ExactCoverGenerator.generate(size,size,seed,35,budget,target,winding,free,chain)
      Triple(r.level,r.nodes,r.exhausted)
     }
     val ms=(System.nanoTime()-began)/1_000_000
     val level=result.first
     val metrics=level?.let { LevelQualityAnalyzer.analyze(it) }
     if(level!=null)check(Rules.solve(level)!=null)
     val columns=listOf(solver,size,seed,if(constrained)"target" else "free",target?:"",winding,
      free?:"",chain,budget,level!=null,result.third,result.second,ms,
      level?.arrows?.size?:"",metrics?.initiallyFree?:"",metrics?.longestChain?:"",
      level?.arrows?.count { it.vertices().size>=5 }?:"",
      if(level==null)if(result.third)"node_budget" else "no_solution" else "")
     rows.add(columns.joinToString(","))
    } catch(error:Exception) {
     val ms=(System.nanoTime()-began)/1_000_000
     val reason=(error::class.simpleName?:"error").replace(',',';')
     rows.add(listOf(solver,size,seed,if(constrained)"target" else "free",target?:"",winding,
      free?:"",chain,budget,false,false,"",ms,"","","","",reason).joinToString(","))
    }
   }
  }
  File(directory,"exact-cover-ab.csv").writeText(rows.joinToString("\n")+"\n")
  val summary=buildString {
   appendLine("Exact Cover Phase 2 A/B | frozen #221 vs current")
   appendLine("Seed count=$seedCount | budgets=600,3000 | identical order and runner")
   for(size in listOf(8,12,16,20))for(budget in budgets)for(mode in listOf("free","target")) {
    val matches=rows.drop(1).map{it.split(',')}.filter{it[1]==size.toString() && it[3]==mode && it[8]==budget.toString()}
    val old=matches.filter{it[0]=="baseline221"}
    val now=matches.filter{it[0]=="current"}
    appendLine("${size}x$size $mode $budget: baseline=${old.count{it[9]=="true"}}/${old.size}, current=${now.count{it[9]=="true"}}/${now.size}")
   }
  }
  File(directory,"exact-cover-ab-summary.txt").writeText(summary)
  println(summary)
 }
}
