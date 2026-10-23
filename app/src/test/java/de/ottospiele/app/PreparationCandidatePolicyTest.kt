// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.app

import org.junit.Assert.assertEquals
import org.junit.Test

class PreparationCandidatePolicyTest {
 @Test fun preserveEarlyStageSelection() {
  assertEquals(8,preparationCandidateCount(29,8))
  assertEquals(6,preparationCandidateCount(30,8))
  assertEquals(4,preparationCandidateCount(45,8))
  assertEquals(2,preparationCandidateCount(60,8))
  assertEquals(2,preparationCandidateCount(71,8))
  assertEquals(2,preparationCandidateCount(80,8))
 }
 @Test fun neverExceedConfiguredMaximum() {
  for(stage in listOf(1,29,30,44,45,59,60,71,80))
   assertEquals(1,preparationCandidateCount(stage,1))
 }
}
