// SPDX-License-Identifier: MPL-2.0
package de.ottospiele.app

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

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
 fun buttonTick()=pulse(14,90)
 fun arrowSelected()=pulse(12,75)
 fun arrowRemoved()=pulse(18,105)
 fun blockedArrow()=pattern(longArrayOf(0,55,22,105),intArrayOf(0,230,0,255))
 fun snapZoneEntered()=pulse(55,200)
 // Child-friendly success: "bumm ... bumm-bumm ... sparkle".
 // Short accents with a soft, fading shimmer; no sustained maximum-power finale.
 fun stageSuccess()=pattern(
  longArrayOf(0,42,65,55,45,35,65,22,28,18,30,12),
  intArrayOf(0,155,0,205,0,165,0,105,0,70,0,40)
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

/** Adds a brief tick before executing any enabled Compose button action. */
@Composable internal fun ticked(action:()->Unit):()->Unit {
 val context=LocalContext.current
 val haptics=remember(context){HapticEffects(context)}
 return {haptics.buttonTick();action()}
}
