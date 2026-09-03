package com.attentiontracker.camera

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * CameraX [ImageAnalysis.Analyzer] that uses ML Kit Face Detection to determine
 * whether a face is actively facing the front camera (i.e. looking at the screen).
 *
 * Runs on the camera executor thread. Invoke [onResult] on that same thread;
 * callers should post to the main thread if UI updates are needed.
 *
 * Decision logic:
 *   - No face detected          → not looking
 *   - |eulerAngleY| > 35°       → face turned left/right, not looking
 *   - |eulerAngleX| > 30°       → face tilted up/down too much, not looking
 *   - Otherwise                 → looking at screen
 *
 * @param onResult called with `true` when a face is facing the screen,
 *                 `false` when no face is present or the face is turned away.
 */
class FaceAnalyzer(private val onResult: (isFacing: Boolean) -> Unit) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            // FAST mode: skips landmark/classification for minimal latency
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.15f)   // ignore tiny faces far from the camera
            .enableTracking()         // re-uses detection across frames for speed
            .build()
    )

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        detector.process(inputImage)
            .addOnSuccessListener { faces -> onResult(isFacingScreen(faces)) }
            .addOnFailureListener { onResult(false) }
            .addOnCompleteListener { imageProxy.close() }   // always release the buffer
    }

    /**
     * Returns `true` only when the primary detected face is oriented toward the camera.
     * Uses Euler angles:
     *   Y-axis (yaw)  : positive = face turned right, negative = turned left
     *   X-axis (pitch): positive = face tilted up,    negative = tilted down
     */
    private fun isFacingScreen(faces: List<Face>): Boolean {
        if (faces.isEmpty()) return false
        val face = faces[0]
        return Math.abs(face.headEulerAngleY) <= 35f &&
               Math.abs(face.headEulerAngleX) <= 30f
    }
}
