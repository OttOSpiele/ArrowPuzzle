// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.ants

import java.io.File
import kotlin.random.Random

object AntsBlockTimedBenchmark {
    @JvmStatic fun main(args: Array<String>) {
        val dir = File(args.getOrElse(0) { "build/benchmark" })
        val seeds = args.getOrNull(1)?.toIntOrNull()?.coerceIn(1,200) ?: 8
        dir.mkdirs()
        val file = File(dir,"ants-block-timed.csv")
        file.printWriter().use { csv ->
            csv.println("profile,seed,outcome,placements,ticks,max_slots,mean_slots,waiting_ticks,concurrent_same_color_ticks")
            for ((name,size,colors) in listOf(Triple("intro",8,2),Triple("easy",12,3),
                Triple("medium",16,4),Triple("advanced",20,5))) {
                for (seed in 0 until seeds) {
                    val round = AntsBlockGenerator.generate(seed.toLong(),size,colors)
                    val rng = Random(seed.toLong() * 71 + 59)
                    var state = AntsBlockGame.initial(round)
                    var ticks = 0
                    var placements = 0
                    var maxSlots = 0
                    var occupiedSum = 0L
                    var waitingTicks = 0
                    var concurrentColorTicks = 0
                    val limit = round.pixels.size * 8 + 200
                    while (!AntsBlockGame.isWon(round,state) && ticks < limit) {
                        // Allow the child to activate another available stack while
                        // earlier groups are still travelling/working.
                        if (ticks % 2 == 0 && rng.nextInt(4) != 0) {
                            val possible = (0..3).mapNotNull { column ->
                                AntsBlockTimed.place(round,state,column)?.let { it }
                            }
                            if (possible.isNotEmpty()) {
                                state = possible[rng.nextInt(possible.size)]
                                placements++
                            }
                        }
                        val active = state.active.filterNotNull()
                        maxSlots = maxOf(maxSlots,active.size)
                        occupiedSum += active.size
                        val exposedColors = AntsBlockGenerator.accessible(state.pixels,round.width,round.height)
                            .map { state.pixels[it] }.toSet()
                        if (active.any { it.color !in exposedColors }) waitingTicks++
                        if (active.groupBy { it.color }.any { it.value.size >= 2 }) concurrentColorTicks++
                        if (AntsBlockTimed.immediateBlock(round,state)) break
                        state = AntsBlockTimed.tick(round,state)
                        ticks++
                    }
                    val outcome = if (AntsBlockGame.isWon(round,state)) "WON"
                        else if (AntsBlockTimed.immediateBlock(round,state)) "BLOCKED" else "LIMIT"
                    val mean = "%.2f".format(java.util.Locale.US,
                        occupiedSum.toDouble() / maxOf(1,ticks))
                    csv.println(listOf(name,seed,outcome,placements,ticks,maxSlots,mean,
                        waitingTicks,concurrentColorTicks).joinToString(","))
                }
            }
        }
        println("Timed AntsBlockCollector benchmark written to: " + file.absolutePath)
    }
}
