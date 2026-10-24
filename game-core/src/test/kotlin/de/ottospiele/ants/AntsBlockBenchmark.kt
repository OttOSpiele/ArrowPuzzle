// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.ants

import java.io.File

/** Run via :game-core:benchmarkAnts. CSVs are artifacts, not gameplay acceptance gates. */
object AntsBlockBenchmark {
    /** Orthogonally connected monochrome areas; no diagonal merging. */
    internal fun connectedRegions(round: AntRound): List<Int> {
        val visited = BooleanArray(round.pixels.size)
        val regions = mutableListOf<Int>()
        for (start in round.pixels.indices) {
            if (visited[start]) continue
            val color = round.pixels[start]
            var count = 0
            val queue = ArrayDeque<Int>()
            queue.add(start)
            visited[start] = true
            while (queue.isNotEmpty()) {
                val i = queue.removeFirst()
                count++
                val x = i % round.width
                val y = i / round.width
                val neighbors = buildList {
                    if (x > 0) add(i - 1)
                    if (x + 1 < round.width) add(i + 1)
                    if (y > 0) add(i - round.width)
                    if (y + 1 < round.height) add(i + round.width)
                }
                for (n in neighbors) if (!visited[n] && round.pixels[n] == color) {
                    visited[n] = true
                    queue.add(n)
                }
            }
            regions.add(count)
        }
        return regions
    }

    @JvmStatic fun main(args: Array<String>) {
        val output = File(args.getOrElse(0) { "build/benchmark" })
        val samples = args.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 1000) ?: 30
        output.mkdirs()
        val detail = File(output, "ants-block-balance.csv")
        val summary = File(output, "ants-block-summary.csv")
        val profiles = listOf(Triple("intro", 8, 2), Triple("easy", 12, 3),
            Triple("medium", 16, 4), Triple("advanced", 20, 5))
        detail.printWriter().use { writer ->
            writer.println("profile,seed,round_seed,board,colors,stacks,moves,outcome,first_safe,first_options,first_unknown,max_slots,sampled_points,sampled_safe,sampled_blocked,sampled_unknown,connected_regions,largest_region")
            for ((name, size, colors) in profiles) for (seed in 0 until samples) {
                val board = AntsBlockGenerator.generate(seed.toLong(), size, colors)
                val opening = AntsBlockBalance.choices(board, AntsBlockGame.initial(board), 2000)
                val sim = AntsBlockBalance.simulate(board, seed.toLong() * 73 + 11, 400, 500)
                val components = connectedRegions(board)
                writer.println(listOf(name,seed,seed,size,colors,board.queues.sumOf { it.size },
                    sim.moves,sim.outcome,opening.provenSafe,opening.available,
                    opening.unknown,sim.maxOccupiedSlots,sim.sampledDecisionPoints,
                    sim.sampledSafeChoices,sim.sampledBlockedChoices,sim.sampledUnknownChoices,
                    components.size,components.maxOrNull() ?: 0).joinToString(","))
            }
        }
        // Useful grouped aggregates without requiring a Python or spreadsheet dependency.
        val lines = detail.readLines().drop(1).map { it.split(",") }
        summary.printWriter().use { writer ->
            writer.println("profile,samples,wins,blocked,unknown,average_moves,average_first_options,average_first_proven_safe,average_max_slots,average_sampled_safe,average_sampled_blocked,average_sampled_unknown,average_regions,average_largest_region")
            for ((name, _, _) in profiles) {
                val rows = lines.filter { it[0] == name }
                fun avg(i: Int) = "%.2f".format(java.util.Locale.US, rows.map { it[i].toDouble() }.average())
                writer.println(listOf(name,rows.size,rows.count { it[7] == "WON" },
                    rows.count { it[7] == "BLOCKED" },rows.count { it[7] == "UNKNOWN" },
                    avg(6),avg(9),avg(8),avg(11),avg(13),avg(14),avg(15),avg(16),avg(17)).joinToString(","))
            }
        }
        println(summary.readText())
    }
}
