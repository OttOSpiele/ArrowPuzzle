// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.app

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Strong, brief game feedback. Respects devices without a vibrator and system
 * vibration restrictions; never blocks UI rendering.
 */
internal class HapticEffects(context:Context) {
 private val vibrator:Vibrator?=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S) {
  (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
 } else {
  @Suppress("DEPRECATION")
  (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
 }
 fun arrowSelected()=pulse(12,75)
 fun arrowRemoved()=pulse(18,105)
 fun blockedArrow()=pattern(longArrayOf(0,55,22,105),intArrayOf(0,230,0,255))
 fun snapZoneEntered()=pulse(55,200)
 // Escalating firework: launch ticks followed by three distinct bursts.
 fun stageSuccess()=pattern(
  longArrayOf(0,35,85,45,80,100,45,85,95,130,55,185),
  intArrayOf(0,95,0,120,0,235,0,190,0,255,0,255)
 )
 fun miniGameEntered()=pulse(90,230)
 fun arrowPuzzleReturned()=pattern(longArrayOf(0,70,55,120),intArrayOf(0,230,0,255))
 private fun pulse(milliseconds:Long,amplitude:Int) {
  val v=vibrator?.takeIf{it.hasVibrator()}?:return
  if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
   v.vibrate(VibrationEffect.createOneShot(milliseconds,amplitude))
  else {
   @Suppress("DEPRECATION")
   v.vibrate(milliseconds)
  }
 }
 private fun pattern(timing:LongArray,amplitudes:IntArray) {
  val v=vibrator?.takeIf{it.hasVibrator()}?:return
  if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
   v.vibrate(VibrationEffect.createWaveform(timing,amplitudes,-1))
  else {
   @Suppress("DEPRECATION")
   v.vibrate(timing,-1)
  }
 }
}
