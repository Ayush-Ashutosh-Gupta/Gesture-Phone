package com.example.audio

import android.content.Context
import android.media.AudioManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class AudioVolumeManager(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val maxVolume = try {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
    } catch (e: Throwable) {
        15
    }

    private val _currentVolumePercent = MutableStateFlow(getActualVolumePercent())
    val currentVolumePercent: StateFlow<Float> = _currentVolumePercent.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    fun getActualVolumePercent(): Float {
        return try {
            val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVolume / 2)
            (current.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f)
        } catch (e: Throwable) {
            0.5f
        }
    }

    fun setVolumePercent(percent: Float) {
        val clamped = percent.coerceIn(0f, 1f)
        val targetIndex = (clamped * maxVolume).roundToInt()
        try {
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetIndex, 0)
            _currentVolumePercent.value = clamped
            _isMuted.value = targetIndex == 0
        } catch (e: Throwable) {
            _currentVolumePercent.value = clamped
        }
    }

    fun stepVolume(deltaPercent: Float) {
        val newPercent = (_currentVolumePercent.value + deltaPercent).coerceIn(0f, 1f)
        setVolumePercent(newPercent)
    }

    fun toggleMute(): Boolean {
        val willMute = !_isMuted.value
        try {
            if (willMute) {
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                _isMuted.value = true
            } else {
                val restoreVolume = (0.5f * maxVolume).roundToInt().coerceAtLeast(1)
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, restoreVolume, 0)
                _isMuted.value = false
                _currentVolumePercent.value = 0.5f
            }
        } catch (e: Throwable) {
            _isMuted.value = willMute
        }
        return willMute
    }
}
