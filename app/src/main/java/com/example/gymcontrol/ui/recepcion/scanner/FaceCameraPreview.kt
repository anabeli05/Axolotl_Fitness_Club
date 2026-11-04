package com.example.gymcontrol.ui.recepcion.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors

/**
 * Cámara que detecta rostros automáticamente (ML Kit).
 * Por cada cuadro llama [onFrame] con la imagen ya derecha y los rostros encontrados.
 * [onFrame] se ejecuta en el hilo de la cámara, no en el principal.
 * Usa la cámara frontal (si no hay, la trasera).
 */
@Composable
fun FaceCameraPreview(
    modifier: Modifier = Modifier,
    onFrame: (Bitmap, List<Face>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val frameCallback by rememberUpdatedState(onFrame)

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    DisposableEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        onDispose { }
    }

    if (!hasPermission) return

    val executor = remember { Executors.newSingleThreadExecutor() }
    val detector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.10f)
                .enableTracking()
                .build()
        )
    }
    val usosDeCamara = remember { mutableListOf<androidx.camera.core.UseCase>() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                val provider = ProcessCameraProvider.getInstance(context).get()
                provider.unbind(*usosDeCamara.toTypedArray())
            } catch (_: Exception) { }
            executor.shutdown()
            detector.close()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                // TextureView: funciona también dentro de diálogos
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy: ImageProxy ->
                    try {
                        val frame = derecho(imageProxy)
                        val rostros = Tasks.await(detector.process(InputImage.fromBitmap(frame, 0)))
                        frameCallback(frame, rostros)
                    } catch (e: Exception) {
                        // cuadro descartado
                    } finally {
                        imageProxy.close()
                    }
                }

                try {
                    val cameraSelector =
                        if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA))
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        else CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    usosDeCamara.clear()
                    usosDeCamara += listOf(preview, imageAnalysis)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

/** Cuadro de la cámara girado a la orientación real. */
private fun derecho(imageProxy: ImageProxy): Bitmap {
    val original = imageProxy.toBitmap()
    val grados = imageProxy.imageInfo.rotationDegrees
    if (grados == 0) return original
    val m = Matrix().apply { postRotate(grados.toFloat()) }
    return Bitmap.createBitmap(original, 0, 0, original.width, original.height, m, true)
}
