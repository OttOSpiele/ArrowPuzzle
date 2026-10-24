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
        val queues = List(4) { mutableListOf<AntStack>() }
        val certificate = mutableListOf<Int>()
        var nextColumn = 0
        while (remaining.any { it >= 0 }) {
            val exposed = accessible(remaining, size, size)
            val possible = exposed.map { remaining[it] }.distinct()
            check(possible.isNotEmpty()) { "No accessible pixels on a nonempty board" }
            // Prefer colors with large reachable surfaces and reduce fragmentation.
            val color = possible.maxWith(compareBy<Int> { c ->
                exposed.count { remaining[it] == c }
            }.thenBy { it })
            val count = minOf(maxCapacity, exposed.count { remaining[it] == color })
            // Use the exact same per-pixel selection as the runtime engine and
            // certificate verifier. A fixed initial frontier can diverge when
            // removing one pixel exposes new matching cells.
            repeat(count) {
                val currentFrontier = accessible(remaining, size, size)
                val index = currentFrontier.firstOrNull { remaining[it] == color }
                    ?: error("Constructive removal became impossible")
                remaining[index] = -1
            }
            queues[nextColumn].add(AntStack(color, count))
            certificate.add(nextColumn)
            nextColumn = (nextColumn + 1) % 4
        }
        return AntRound(size, size, pixels, queues.map { it.toList() }, certificate)
            .also { check(AntsBlockSolver.verifyCertificate(it)) }
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
    /** Verifies stack sums, four FIFO queues and a real executable removal sequence. */
    fun verifyCertificate(round: AntRound): Boolean {
        val remaining = round.pixels.toMutableList()
        val positions = IntArray(4)
        for (column in round.solutionColumns) {
            if (column !in 0..3) return false
            val stack = round.queues[column].getOrNull(positions[column]++) ?: return false
            repeat(stack.capacity) {
                val accessible = AntsBlockGenerator.accessible(remaining, round.width, round.height)
                val index = accessible.firstOrNull { remaining[it] == stack.color } ?: return false
                remaining[index] = -1
            }
        }
        return remaining.all { it == -1 } &&
            round.queues.indices.all { positions[it] == round.queues[it].size } &&
            round.pixels.groupingBy { it }.eachCount().all { (color, count) ->
                round.queues.flatten().filter { it.color == color }.sumOf { it.capacity } == count
            }
    }
}
