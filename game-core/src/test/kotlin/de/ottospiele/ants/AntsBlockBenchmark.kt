// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.ants

import java.io.File

/** Run via :game-core:benchmarkAnts. CSVs are artifacts, not gameplay acceptance gates. */
object AntsBlockBenchmark {
    @JvmStatic fun main(args: Array<String>) {
        val output = File(args.getOrElse(0) { "build/benchmark" })
        val samples = args.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 1000) ?: 30
        output.mkdirs()
        val detail = File(output, "ants-block-balance.csv")
        val summary = File(output, "ants-block-summary.csv")
        val profiles = listOf(Triple("intro", 8, 2), Triple("easy", 12, 3),
            Triple("medium", 16, 4), Triple("advanced", 20, 5))
        detail.printWriter().use { writer ->
            writer.println("profile,seed,round_seed,board,colors,stacks,moves,outcome,first_safe,first_options,first_unknown,max_slots")
            for ((name, size, colors) in profiles) for (seed in 0 until samples) {
                val board = AntsBlockGenerator.generate(seed.toLong(), size, colors)
                val opening = AntsBlockBalance.choices(board, AntsBlockGame.initial(board), 2000)
                val sim = AntsBlockBalance.simulate(board, seed.toLong() * 73 + 11, 400, 500)
                writer.println(listOf(name,seed,seed,size,colors,board.queues.sumOf { it.size },
                    sim.moves,sim.outcome,opening.provenSafe,opening.available,
                    opening.unknown,sim.maxOccupiedSlots).joinToString(","))
            }
        }
        // Useful grouped aggregates without requiring a Python or spreadsheet dependency.
        val lines = detail.readLines().drop(1).map { it.split(",") }
        summary.printWriter().use { writer ->
            writer.println("profile,samples,wins,blocked,unknown,average_moves,average_first_options,average_first_proven_safe,average_max_slots")
            for ((name, _, _) in profiles) {
                val rows = lines.filter { it[0] == name }
                fun avg(i: Int) = "%.2f".format(java.util.Locale.US, rows.map { it[i].toDouble() }.average())
                writer.println(listOf(name,rows.size,rows.count { it[7] == "WON" },
                    rows.count { it[7] == "BLOCKED" },rows.count { it[7] == "UNKNOWN" },
                    avg(6),avg(9),avg(8),avg(11)).joinToString(","))
            }
        }
        println(summary.readText())
    }
}
