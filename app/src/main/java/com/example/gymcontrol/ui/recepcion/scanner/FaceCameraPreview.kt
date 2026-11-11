package com.example.gymcontrol.ui.recepcion.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
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
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Toma una foto a resolución completa con la misma cámara de [FaceCameraPreview].
 * Los cuadros del análisis son de baja resolución (~640×480) y se ven borrosos como foto
 * de perfil; esta foto sale nítida.
 */
class CamaraFoto {
    @Volatile internal var imageCapture: ImageCapture? = null

    private companion object {
        // Un solo hilo para todas las fotos (no se crea uno por diálogo)
        val executor = Executors.newSingleThreadExecutor()
    }

    /** Foto ya derecha, o null si la cámara no está lista o falla. */
    suspend fun tomar(): Bitmap? {
        val ic = imageCapture ?: return null
        return suspendCancellableCoroutine { cont ->
            ic.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bmp = try { derecho(image) } catch (e: Exception) { null } finally { image.close() }
                    if (cont.isActive) cont.resume(bmp)
                }

                override fun onError(exception: ImageCaptureException) {
                    if (cont.isActive) cont.resume(null)
                }
            })
        }
    }
}

/**
 * Cámara que detecta rostros automáticamente (ML Kit).
 * Por cada cuadro llama [onFrame] con la imagen ya derecha y los rostros encontrados.
 * [onFrame] se ejecuta en el hilo de la cámara, no en el principal.
 * Usa la cámara frontal (si no hay, la trasera).
 * Con [camaraFoto] se puede además tomar una foto a máxima calidad (foto de perfil).
 */
@Composable
fun FaceCameraPreview(
    modifier: Modifier = Modifier,
    camaraFoto: CamaraFoto? = null,
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
            camaraFoto?.imageCapture = null
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
                    usosDeCamara.clear()
                    // Foto a máxima calidad (solo si se pidió); si el equipo no admite las tres
                    // funciones juntas, se sigue sin ella y la foto sale del cuadro del análisis
                    val imageCapture = camaraFoto?.let {
                        ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .build()
                    }
                    val conFoto = imageCapture != null && try {
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview, imageAnalysis, imageCapture
                        )
                        true
                    } catch (e: Exception) {
                        cameraProvider.unbindAll()
                        false
                    }
                    if (conFoto) {
                        camaraFoto?.imageCapture = imageCapture
                        usosDeCamara += listOf(preview, imageAnalysis, imageCapture!!)
                    } else {
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                        usosDeCamara += listOf(preview, imageAnalysis)
                    }
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
