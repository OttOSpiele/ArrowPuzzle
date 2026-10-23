// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.arrow

import java.io.File
import java.util.Locale

/**
 * Stage-by-stage production-profile benchmark, without invoking Legacy.
 * Every attempt (including failures) is retained; only solver-proved boards
 * are included in the visual catalog.
 */
object ExactCoverStageBenchmark {
 @JvmStatic fun main(args:Array<String>) {
  val directory=File(args.getOrNull(0)?:"build/benchmark").apply{mkdirs()}
  val perStage=(args.getOrNull(1)?.toIntOrNull()?:16).coerceIn(8,100)
  val selectedStages=setOf(9,10,12,16,17,20,24,30,40,50,60,70,80)
  val csv=File(directory,"exact-cover-stages.csv")
  val summary=File(directory,"exact-cover-stages-summary.csv")
  val drawings=File(directory,"exact-cover-stage-catalog.tsv")
  csv.writeText("stage,attempt,seed,aspect,width,height,target_arrows,winding_min,max_free,min_chain,node_budget,visited_nodes,elapsed_ms,success,exhausted,arrows,occupancy_pct,initially_free,longest_chain,windings,profile_score,error\n")
  summary.writeText("stage,attempts,successes,failures,success_pct,mean_ms,p50_ms,p90_ms,max_ms,mean_arrows,mean_occupancy_pct,mean_initially_free,mean_chain,mean_windings,mean_profile_score,catalog_boards\n")
  drawings.writeText("stage\tseed\taspect\twidth\theight\tfallback\toccupancy_pct\tfree\tchain\tquality\tpaths\n")
  fun dec(d:Double)=String.format(Locale.ROOT,"%.3f",d)
  fun percentile(sorted:List<Long>,p:Double):Long=if(sorted.isEmpty())0 else sorted[((sorted.size-1)*p).toInt()]
  for(stage in 9..80) {
   var count=0
   val times=mutableListOf<Long>()
   val arrows=mutableListOf<Int>()
   val occupancies=mutableListOf<Double>()
   val frees=mutableListOf<Int>()
   val chains=mutableListOf<Int>()
   val windings=mutableListOf<Int>()
   val scores=mutableListOf<Double>()
   var catalogCount=0
   for(index in 0 until perStage) {
    val aspect=listOf(1f,0.75f,1.5f)[index%3]
    val shape=StageGenerationProfile.forStage(stage,aspect)
    val seed=stage.toLong()*1000003L+index*7919L+17L
    val started=System.nanoTime()
    try {
     // Exact-cover ONLY, using exactly the app's production stage profile.
     val level=LevelGeneration.generateExactOrNull(stage,seed,aspect)
     val elapsed=(System.nanoTime()-started)/1_000_000
     times.add(elapsed)
     if(level==null) {
      csv.appendText(listOf(stage,index,seed,aspect,shape.width,shape.height,
       shape.targetArrows,shape.windingArrows,shape.maxInitiallyFree,
       shape.minChainLength,3000,"",elapsed,false,"","",
       "","","","","","","no_solution_or_node_budget").joinToString(",")+"\n")
      continue
     }
     check(Rules.solve(level)!=null){"Unsolvable Exact Cover output"}
     val metrics=LevelQualityAnalyzer.analyze(level)
     val bends=level.arrows.count{it.vertices().size>=5}
     val score=DifficultyProfiles.forStage(stage).assess(level).score
     val occupancy=metrics.occupancy*100
     count++
     arrows.add(level.arrows.size);occupancies.add(occupancy)
     frees.add(metrics.initiallyFree);chains.add(metrics.longestChain)
     windings.add(bends);scores.add(score)
     csv.appendText(listOf(stage,index,seed,aspect,shape.width,shape.height,
      shape.targetArrows,shape.windingArrows,shape.maxInitiallyFree,
      shape.minChainLength,3000,"",elapsed,true,false,level.arrows.size,
      dec(occupancy),metrics.initiallyFree,metrics.longestChain,bends,dec(score),"").joinToString(",")+"\n")
     if(stage in selectedStages && catalogCount<8) {
      val paths=level.arrows.joinToString("|"){a->a.vertices().joinToString(";"){p->"${p.x}:${p.y}"}}
      drawings.appendText(listOf(stage,seed,aspect,level.width,level.height,false,
       dec(occupancy),metrics.initiallyFree,metrics.longestChain,dec(score),paths).joinToString("\t")+"\n")
      catalogCount++
     }
    } catch(e:Exception) {
     val elapsed=(System.nanoTime()-started)/1_000_000
     times.add(elapsed)
     val reason=(e::class.simpleName+":"+(e.message?:"")).replace(',',';').replace('\n',' ')
     csv.appendText(listOf(stage,index,seed,aspect,shape.width,shape.height,
      shape.targetArrows,shape.windingArrows,shape.maxInitiallyFree,
      shape.minChainLength,3000,"",elapsed,false,false,
      "","","","","","","",reason).joinToString(",")+"\n")
    }
   }
   // Continue a separate bounded catalog search to get eight visual examples.
   // Extra catalog attempts do NOT change the fixed-size statistical sample.
   if(stage in selectedStages && catalogCount<8) {
    for(extra in 0 until 64) {
     if(catalogCount>=8)break
     val aspect=listOf(1f,0.75f,1.5f)[extra%3]
     val seed=stage.toLong()*1000003L+(perStage+extra)*7919L+17L
     val level=runCatching{LevelGeneration.generateExactOrNull(stage,seed,aspect)}.getOrNull()
      ?:continue
     if(Rules.solve(level)==null)continue
     val metrics=LevelQualityAnalyzer.analyze(level)
     val score=DifficultyProfiles.forStage(stage).assess(level).score
     val paths=level.arrows.joinToString("|"){a->a.vertices().joinToString(";"){p->"${p.x}:${p.y}"}}
     drawings.appendText(listOf(stage,seed,aspect,level.width,level.height,false,
      dec(metrics.occupancy*100),metrics.initiallyFree,metrics.longestChain,
      dec(score),paths).joinToString("\t")+"\n")
     catalogCount++
    }
   }
   val sorted=times.sorted()
   fun avg(numbers:Collection<Number>):Double=if(numbers.isEmpty())0.0 else numbers.sumOf{it.toDouble()}/numbers.size
   summary.appendText(listOf(stage,perStage,count,perStage-count,dec(count*100.0/perStage),
    dec(avg(times)),percentile(sorted,.50),percentile(sorted,.90),sorted.lastOrNull()?:0,
    dec(avg(arrows)),dec(avg(occupancies)),dec(avg(frees)),dec(avg(chains)),
    dec(avg(windings)),dec(avg(scores)),catalogCount).joinToString(",")+"\n")
   println("stage=$stage exact=$count/$perStage meanMs=${dec(avg(times))} catalog=$catalogCount")
  }
  File(directory,"exact-cover-stages-summary.txt").writeText(
   "Stages 9..80; exact-only production-profile testing. Seeds per stage=$perStage; node budget=3000.\n"+
   "Failed searches stay in CSV. Catalog contains up to eight solver-valid puzzles per selected stage.\n"+
   "See exact-cover-stages-summary.csv and exact-cover-stages.csv.\n")
 }
}
