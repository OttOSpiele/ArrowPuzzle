// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.ants

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AntsBlockCoreTest {
    @Test fun deterministicGenerationAndConstructiveSolutions() {
        for (seed in 0L until 100L) {
            val first = AntsBlockGenerator.generate(seed)
            val second = AntsBlockGenerator.generate(seed)
            assertEquals(first, second, "seed=$seed")
            assertTrue(AntsBlockSolver.verifyCertificate(first), "seed=$seed")
            assertEquals(4, first.queues.size)
            assertEquals(first.width * first.height, first.queues.flatten().sumOf { it.capacity })
        }
    }

    @Test fun supportedConfigurationsRemainPlayable() {
        for (size in listOf(8, 12, 16, 24, 32)) {
            for (colors in listOf(2, 3, 5, 7)) {
                for (seed in 0L until 12L) {
                    val round = AntsBlockGenerator.generate(seed, size, colors)
                    assertTrue(AntsBlockSolver.verifyCertificate(round), "size=$size colors=$colors seed=$seed")
                }
            }
        }
    }

    @Test fun inaccessibleEnclosedPixelsStayProtected() {
        val pixels = listOf(
            1, 1, 1,
            1, 2, 1,
            1, 1, 1
        )
        val available = AntsBlockGenerator.accessible(pixels, 3, 3)
        assertFalse(4 in available)
        assertEquals(8, available.size)
    }

    @Test fun tamperedCapacityIsRejected() {
        val original = AntsBlockGenerator.generate(42L)
        val tampered = original.queues.map { it.toMutableList() }
        val first = tampered.first { it.isNotEmpty() }
        first[0] = first[0].copy(capacity = first[0].capacity + 1)
        assertFalse(AntsBlockSolver.verifyCertificate(original.copy(queues = tampered)))
    }
}
