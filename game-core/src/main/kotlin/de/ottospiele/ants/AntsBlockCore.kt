// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.ants

import kotlin.random.Random

/** Logical colors are palette indices; -1 represents an already removed pixel. */
data class AntStack(val color: Int, val capacity: Int)
data class AntRound(
    val width: Int,
    val height: Int,
    val pixels: List<Int>,
    val queues: List<List<AntStack>>,
    /** A constructive certificate: next queue column to select, one stack at a time. */
    val solutionColumns: List<Int>
) {
    init {
        require(width > 0 && height > 0 && pixels.size == width * height)
        require(queues.size == 4)
        require(queues.flatten().all { it.capacity > 0 && it.color >= 0 })
    }
}

/** Pure Kotlin foundation. No Android, rendering, timing or animation dependencies. */
object AntsBlockGenerator {
    fun generate(seed: Long, size: Int = 16, colors: Int = 5, maxCapacity: Int = 20): AntRound {
        require(size in 8..40 && size % 4 == 0)
        require(colors in 2..9 && maxCapacity > 0)
        val random = Random(seed)
        val regions = IntArray(16) { random.nextInt(colors) }
        // Puzzle-like pieces: a small centered tongue crosses each region boundary.
        val cell = size / 4
        val pixels = MutableList(size * size) { index ->
            val x = index % size
            val y = index / size
            var gx = x / cell
            var gy = y / cell
            val localX = x % cell
            val localY = y % cell
            if (localX == 0 && gx > 0 && localY in (cell / 2 - 1)..(cell / 2)) {
                if ((gx + gy) % 2 == 0) gx--
            }
            if (localY == 0 && gy > 0 && localX in (cell / 2 - 1)..(cell / 2)) {
                if ((gx + gy) % 2 == 1) gy--
            }
            regions[gy * 4 + gx]
        }
        val remaining = pixels.toMutableList()
        val planned = mutableListOf<AntStack>()
        while (remaining.any { it >= 0 }) {
            val exposed = accessible(remaining, size, size)
            val possible = exposed.map { remaining[it] }.distinct()
            check(possible.isNotEmpty())
            val color = if (random.nextInt(4) == 0) possible[random.nextInt(possible.size)]
                else possible.maxWith(compareBy<Int> { c ->
                    exposed.count { remaining[it] == c }
                }.thenBy { it })
            val low = maxOf(1, maxCapacity / 5)
            val target = low + random.nextInt(maxCapacity - low + 1)
            val count = minOf(target, exposed.count { remaining[it] == color })
            repeat(count) {
                val index = accessible(remaining, size, size).firstOrNull { remaining[it] == color }
                    ?: error("Constructive removal became impossible")
                remaining[index] = -1
            }
            planned.add(AntStack(color, count))
        }

        // Combine nearby matching colors into one longer-lived stack. A combined
        // stack can pause behind another color and resume when new cells open.
        // Keep a maximum of four intervening selections so the five slots can
        // accommodate the constructive order; verify using the runtime engine.
        val baseline = planned.toList()
        val merged = planned.toMutableList()
        for (i in merged.indices.reversed()) {
            if (i >= merged.size) continue
            val later = merged[i]
            val earlier = (maxOf(0, i - 4) until i).lastOrNull { j ->
                merged[j].color == later.color &&
                    merged[j].capacity + later.capacity <= maxCapacity
            }
            if (earlier != null && random.nextBoolean()) {
                merged[earlier] = merged[earlier].copy(
                    capacity = merged[earlier].capacity + later.capacity)
                merged.removeAt(i)
            }
        }
        fun makeRound(stacks: List<AntStack>): AntRound {
            val queues = List(4) { mutableListOf<AntStack>() }
            val certificate = mutableListOf<Int>()
            val lastUsed = IntArray(4) { -1 }
            for ((index, stack) in stacks.withIndex()) {
                val column = if (size <= 12) index % 4 else {
                    val candidates = (0..3).filter { lastUsed[it] < index - 1 }
                    val preferred = (index + random.nextInt(4)) % 4
                    if (preferred in candidates) preferred else
                        candidates.minByOrNull { queues[it].size } ?: preferred
                }
                queues[column].add(stack)
                lastUsed[column] = index
                certificate.add(column)
            }
            return AntRound(size, size, pixels, queues.map { it.toList() }, certificate)
        }
        val candidate = makeRound(merged)
        if (AntsBlockSolver.verifyCertificate(candidate)) return candidate
        return makeRound(baseline).also { check(AntsBlockSolver.verifyCertificate(it)) }

    }

    /**
     * Pixels touching the connected empty exterior through a four-neighbour edge.
     * Interior cavities do not count as externally accessible.
     */
    fun accessible(board: List<Int>, width: Int, height: Int): List<Int> {
        val visited = BooleanArray(board.size)
        val frontier = ArrayDeque<Int>()
        val exposed = linkedSetOf<Int>()
        fun around(i: Int): List<Int> {
            val x = i % width
            val y = i / width
            return buildList {
                if (x > 0) add(i - 1)
                if (x + 1 < width) add(i + 1)
                if (y > 0) add(i - width)
                if (y + 1 < height) add(i + width)
            }
        }
        for (i in board.indices) {
            val x = i % width
            val y = i / width
            if (x != 0 && y != 0 && x != width - 1 && y != height - 1) continue
            if (board[i] >= 0) exposed.add(i)
            else if (!visited[i]) { visited[i] = true; frontier.add(i) }
        }
        while (frontier.isNotEmpty()) {
            val i = frontier.removeFirst()
            for (j in around(i)) {
                if (board[j] >= 0) exposed.add(j)
                else if (!visited[j]) { visited[j] = true; frontier.add(j) }
            }
        }
        return exposed.toList()
    }
}

object AntsBlockSolver {
    /** Verify exact per-color totals and execute the prescribed FIFO selections
     * with the same settling rules used during gameplay. Partly used stacks may wait.
     */
    fun verifyCertificate(round: AntRound): Boolean {
        val counts = round.pixels.groupingBy { it }.eachCount()
        if (round.queues.flatten().groupBy { it.color }
                .mapValues { (_, stacks) -> stacks.sumOf { it.capacity } } != counts) return false
        var state = AntsBlockGame.initial(round)
        for (column in round.solutionColumns) {
            state = AntsBlockGame.place(round, state, column) ?: return false
        }
        return AntsBlockGame.isWon(round, state)
    }
}
