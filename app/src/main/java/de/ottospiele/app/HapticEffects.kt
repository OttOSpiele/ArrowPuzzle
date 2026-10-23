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
 fun blockedArrow()=pulse(65,220)
 fun snapZoneEntered()=pulse(55,200)
 fun stageSuccess()=pattern(longArrayOf(0,85,65,150),intArrayOf(0,235,0,255))
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
