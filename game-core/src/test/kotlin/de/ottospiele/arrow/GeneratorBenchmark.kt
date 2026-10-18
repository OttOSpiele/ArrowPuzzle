// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.arrow

import java.io.File
import java.util.Locale

/**
 * Standalone CI benchmark: records EVERY attempted board, including failures.
 * Run with Gradle :game-core:benchmarkLevels; outputs build/benchmark/*.csv.
 */
object GeneratorBenchmark {
 @JvmStatic fun main(args:Array<String>) {
  val output=File(args.getOrNull(0) ?: "build/benchmark").apply{mkdirs()}
  val stages=listOf(1,2,5,8,9,16,17,24,25,34,35,44,45,60,80,100)
  val seedCount=(args.getOrNull(1)?.toIntOrNull()?:7).coerceIn(1,1000)
  val aspects=listOf(.65f,1.0f,1.65f)
  val lines=mutableListOf("stage,seed,aspect,success,elapsed_ms,width,height,arrows,occupancy_pct,free,connected,longest_chain,key_arrows,largest_interior_void,fallback,quality,error")
  val generator=Generator()
  val boards=mutableListOf<String>()
  var succeeded=0
  var failed=0
  for(stage in stages)for(i in 0 until seedCount) {
   val aspect=aspects[i%aspects.size]
   val seed=i.toLong()*7919L+17
   val start=System.nanoTime()
   try {
    val level=generator.generate(stage,seed,aspect)
    require(Rules.solve(level)!=null){"unsolvable"}
    val m=LevelQualityAnalyzer.analyze(level)
    val elapsed=(System.nanoTime()-start)/1_000_000
    fun dec(n:Double)=String.format(Locale.ROOT,"%.4f",n)
    lines.add(listOf(stage,seed,aspect,true,elapsed,level.width,level.height,
     level.arrows.size,dec(m.occupancy*100),m.initiallyFree,m.connectedArrows,
     m.longestChain,m.keyArrows,m.largestInteriorVoid,level.fallbackGeneration,
     dec(m.score(stage)),"").joinToString(","))
    // Geometry export: one board per TSV row, paths as ordered vertices.
    // Separate file ensures PDF renderer can draw exactly the generated data.
    val encoded=level.arrows.joinToString("|") { a ->
     a.vertices().joinToString(";") { p -> "${p.x}:${p.y}" }
    }
    boards.add(listOf(stage,seed,aspect,level.width,level.height,
     level.fallbackGeneration,dec(m.occupancy*100),m.initiallyFree,
     m.longestChain,dec(m.score(stage)),encoded).joinToString("\t"))
    succeeded++
   } catch(e:Exception) {
    val elapsed=(System.nanoTime()-start)/1_000_000
    val error=(e::class.simpleName+":"+e.message).replace(',',';').replace('\n',' ')
    lines.add("$stage,$seed,$aspect,false,$elapsed,,,,,,,,,,,$error")
    failed++
   }
  }
  File(output,"boards.tsv").writeText(
   "stage\tseed\taspect\twidth\theight\tfallback\toccupancy_pct\tfree\tchain\tquality\tpaths\n"+
    boards.joinToString("\n")+"\n")
  File(output,"levels.csv").writeText(lines.joinToString("\n")+"\n")
  val summary=buildString {
   appendLine("Arrow Puzzle generator benchmark")
   appendLine("stages=${stages.joinToString(",")} attempts_per_stage=$seedCount")
   appendLine("total=${succeeded+failed} passed=$succeeded failed=$failed")
   appendLine("Details: levels.csv (includes all failed attempts).")
  }
  File(output,"summary.txt").writeText(summary)
  println(summary)
  // Reports are always written; CI still surfaces generation errors.
  if(failed>0)error("$failed levels failed to generate")
 }
}
