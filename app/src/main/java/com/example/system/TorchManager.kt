package com.example.system

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TorchManager(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var torchCameraId: String? = null

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn = _isTorchOn.asStateFlow()

    init {
        try {
            val cameraIds = cameraManager?.cameraIdList ?: emptyArray()
            for (id in cameraIds) {
                val characteristics = cameraManager?.getCameraCharacteristics(id)
                val hasFlash = characteristics?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = characteristics?.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    torchCameraId = id
                    break
                }
            }
            // Fallback to any camera with flash if back facing not matched
            if (torchCameraId == null) {
                for (id in cameraIds) {
                    val hasFlash = cameraManager?.getCameraCharacteristics(id)?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    if (hasFlash) {
                        torchCameraId = id
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TorchManager", e)
        }
    }

    fun toggleTorch(): Boolean {
        return setTorch(!_isTorchOn.value)
    }

    fun setTorch(enabled: Boolean): Boolean {
        val id = torchCameraId ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager?.setTorchMode(id, enabled)
                _isTorchOn.value = enabled
                true
            } else {
                false
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "CameraAccessException setting torch mode", e)
            false
        } catch (e: Throwable) {
            Log.e(TAG, "Exception setting torch mode", e)
            false
        }
    }

    companion object {
        private const val TAG = "TorchManager"
    }
}
