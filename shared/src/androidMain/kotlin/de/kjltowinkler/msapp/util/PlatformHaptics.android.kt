package de.kjltowinkler.msapp.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

actual object PlatformHaptics {

    actual fun performTaskCompletedHaptic() {
        try {
            val context = AppContextProvider.context
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                // Moderne Android-Geräte (z. B. Pixel 10 Pro mit Android 11+ und Haptik-Primitives):
                // Zweistufige fröhliche Komposition: initialer Klick (0.75f) gefolgt von einem hellen,
                // belohnenden Tick (1.0f) nach 65ms – erzeugt ein extrem befriedigendes "Pop-Pop / Pling!"-Gefühl
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    vibrator.areAllPrimitivesSupported(
                        VibrationEffect.Composition.PRIMITIVE_CLICK,
                        VibrationEffect.Composition.PRIMITIVE_TICK
                    )
                ) {
                    val composition = VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.75f)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f, 65)
                    vibrator.vibrate(composition.compose())
                    return
                }

                // Fallback für Android 8.0+ mit Amplitudensteuerung
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 18, 55, 30)
                    val amplitudes = intArrayOf(0, 160, 0, 255)
                    if (vibrator.hasAmplitudeControl()) {
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                        return
                    } else {
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
                        return
                    }
                }

                // Klassischer Fallback für ältere Android-Versionen
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 18, 55, 30), -1)
            }
        } catch (e: Throwable) {
            println("PlatformHaptics.android: Fehler bei Haptik: ${e.message}")
        }
    }
}
