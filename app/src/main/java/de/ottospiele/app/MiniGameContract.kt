// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.app

/**
 * A mini-game is an independent sequence of complete rounds.
 *
 * The host starts level preparation independently, and reports readiness
 * without interrupting a round. A running mini-game may start another round
 * while preparation is unfinished. On readiness, it finishes the current
 * round before yielding to Arrow Puzzle.
 */
internal interface MiniGameModule {
 val id: String
 fun begin()
 fun requestStopAfterCurrentRound()
 fun isCurrentRoundComplete(): Boolean
}

/** The host remains the owner of the prepared Arrow Puzzle level. */
internal enum class MiniGameTransition {
 PLAY_NEXT_ROUND,
 FINISH_CURRENT_ROUND,
 RETURN_TO_ARROW_PUZZLE
}

internal fun miniGameTransition(
 preparationReady: Boolean,
 roundComplete: Boolean
): MiniGameTransition = when {
 preparationReady && roundComplete -> MiniGameTransition.RETURN_TO_ARROW_PUZZLE
 preparationReady -> MiniGameTransition.FINISH_CURRENT_ROUND
 else -> MiniGameTransition.PLAY_NEXT_ROUND
}
