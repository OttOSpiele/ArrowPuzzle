// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.arrow

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull

class MovingArrowTouchTest {
 @Test fun flyingArrowDoesNotCaptureInput() {
  val a=Arrow(1,1,1,2,Dir.RIGHT)
  val b=Arrow(2,7,7,2,Dir.UP)
  val level=Level(42L,12,10,listOf(a,b))
  val remaining=setOf(1,2)
  assertEquals(1,ArrowTouch.select(level,remaining,1f,1f))
  assertNull(ArrowTouch.select(level,remaining,1f,1f,setOf(1)))
  assertEquals(2,ArrowTouch.select(level,remaining,7f,7f,setOf(1)))
 }
}
