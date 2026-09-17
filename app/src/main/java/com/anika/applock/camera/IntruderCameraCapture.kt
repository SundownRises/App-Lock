package com.anika.applock.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant

/**
 * Silent front-camera capture for intruder selfies.
 *
 * CRITICAL: This is best-effort. Camera failures must never prevent legitimate unlock.
 * If camera is unavailable, permission revoked, or hardware busy, capture fails silently.
 */
class IntruderCameraCapture(private val context: Context) {

    companion object {
        private const val TAG = "IntruderCamera"
    }

    /**
     * Asynchronously captures a photo from the front-facing camera.
     * Runs on IO dispatcher, no blocking of UI.
     *
     * @param targetApp The package name of the app that was being accessed
     */
    fun captureAsync(targetApp: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                val cameraProvider = cameraProviderFuture.get()

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                try {
                    // Bind to front camera
                    val camera = cameraProvider.bindToLifecycle(
                        ProcessLifecycleOwner.get(),
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        imageCapture
                    )

                    // Prepare output file
                    val outputFile = File(
                        context.getExternalFilesDir(null),
                        "intruder_${System.currentTimeMillis()}.jpg"
                    )

                    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

                    // Take picture
                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                Log.d(TAG, "Intruder photo saved: ${outputFile.absolutePath}")
                                // TODO: Record in IntruderLogRepository
                                // For now, just log success
                                cameraProvider.unbindAll()
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.w(TAG, "Failed to capture intruder photo", exception)
                                cameraProvider.unbindAll()
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Camera binding failed", e)
                    cameraProvider.unbindAll()
                }
            } catch (e: Exception) {
                // Camera provider unavailable, permission denied, etc.
                Log.w(TAG, "Camera capture failed", e)
            }
        }
    }
}
