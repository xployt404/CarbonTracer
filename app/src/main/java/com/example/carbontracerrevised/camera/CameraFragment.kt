package com.example.carbontracerrevised.camera


import android.Manifest.permission.CAMERA
import android.Manifest.permission.INTERNET
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.carbontracerrevised.GeminiModel
import com.example.carbontracerrevised.MainActivity
import com.example.carbontracerrevised.tracer.Traceable
import com.example.carbontracerrevised.R
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {
    private lateinit var captureBtn: ImageButton
    private lateinit var flashBtn: ImageButton
    private lateinit var objectInfo: String
    private lateinit var previewView: PreviewView
    private lateinit var imageView: ImageView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraControl: CameraControl
    private lateinit var progressBar: ProgressBar
    private lateinit var model: GeminiModel
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var orientationEventListener: OrientationEventListener
    private var imageCapture: ImageCapture? = null
    var newRotation = 0f
    private var flashMode = FLASH_MODE_OFF
    private var imageAnalyzeJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val cameraView = inflater.inflate(R.layout.camera_fragment, container, false)
        previewView = cameraView.findViewById(R.id.previewView)
        imageView = cameraView.findViewById(R.id.imageView)
        progressBar = cameraView.findViewById(R.id.progressBar)
        captureBtn = cameraView.findViewById(R.id.captureButton)
        flashBtn = cameraView.findViewById(R.id.flashButton)


        // Initialize the OrientationEventListener
        orientationEventListener =
            object : OrientationEventListener(requireContext(), SensorManager.SENSOR_DELAY_NORMAL) {
                override fun onOrientationChanged(orientation: Int) {
                    // Check the orientation and determine landscape side
                    newRotation = when (orientation) {
                        in 315..360, in 0..45 -> {
                            0f
                        }

                        in 46..134 -> {
                            -90f
                        }
//                    in 135..225 -> {
//                        // Landscape Left (270 degrees)
//                        flashBtn.rotation = 0f
//                        (activity as MainActivity).handleCameraRotation(0f)
//                    }
                        in 226..314 -> {
                            90f
                        }

                        else -> {
                            flashBtn.rotation
                        }
                    }
                    val animator =
                        ObjectAnimator.ofFloat(flashBtn, "rotation", flashBtn.rotation, newRotation)
                    animator.duration = 200 // Duration in milliseconds
                    animator.interpolator = LinearInterpolator()
                    animator.repeatCount = 0
                    animator.doOnEnd {
                        flashBtn.rotation = newRotation
                    }
                    animator.start()
                }
            }

        cameraExecutor = Executors.newSingleThreadExecutor()

        model = GeminiModel()

        captureBtn.setOnClickListener {
            if (imageView.visibility == View.VISIBLE) {
                captureBtn.setImageResource(0)
                imageView.visibility = View.INVISIBLE
                imageAnalyzeJob?.cancel()
            } else {
                progressBar.visibility = View.VISIBLE
                takePhoto()
            }
        }

        flashBtn.setOnClickListener {
            when (flashMode) {
                FLASH_MODE_ON -> {
                    flashMode = FLASH_MODE_OFF
                    flashBtn.setImageResource(R.drawable.ic_lightning_crossed)
                }

                FLASH_MODE_OFF -> {
                    flashMode = FLASH_MODE_ON
                    flashBtn.setImageResource(R.drawable.ic_lightning)
                }
            }
        }



        return cameraView
    }

    private fun startCamera() {

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9) // Set your desired aspect ratio here
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // Optional: Set capture mode
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                cameraControl = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                ).cameraControl
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun stopCamera() {
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll() // Unbind all use cases
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    private fun takePhoto() {
        Log.i(TAG, "Photo was taken")
        imageCapture = imageCapture ?: return
        imageCapture!!.flashMode = flashMode
        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture!!.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    var bitmap = imageProxyToBitmap(image)
                    val imageRotation =
                        -newRotation // store so scale type and orientation are coherent
                    bitmap = rotateImage(bitmap, imageRotation)
                    if (imageRotation == 0f)
                        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    imageView.setImageBitmap(bitmap)
                    imageView.visibility = View.VISIBLE
                    captureBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    previewView.setBackgroundColor(Color.WHITE)
                    imageAnalyzeJob = lifecycleScope.launch {
                        try {
                            objectInfo =
                                model.Tracer().interpretImage(requireContext(), listOf(bitmap))

                            val chunkedResponse = createTraceableFromGeminiResponse(
                                objectInfo
                            )
                            val dialog =
                                (activity as MainActivity).showAddTraceableDialog(chunkedResponse)
                            dialog.setOnDismissListener {
                                captureBtn.performClick()
                            }

                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    requireContext(),
                                    "Unable to reach Gemini >_<",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            Log.e(
                                "Gemini ERROR",
                                "${e.message.toString()}\nCause: $e \n ${e.suppressedExceptions}"
                            )
                            e.printStackTrace()
                            model.generating = false
                        } finally {
                            image.close()
                            progressBar.visibility = View.INVISIBLE
                        }
                    }
                }
            }
        )
    }


    override fun onPause() {
        super.onPause()
        progressBar.visibility = View.GONE
        stopCamera()
        orientationEventListener.disable()
    }

    override fun onResume() {
        super.onResume()
        orientationEventListener.enable()
        if ((requireActivity() as MainActivity).checkAndRequestPermissions(REQUIRED_PERMISSIONS)) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Missing Permissions", Toast.LENGTH_SHORT).show()
        }
        if (model.generating) {
            progressBar.visibility = View.VISIBLE
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        println(image.imageInfo.rotationDegrees)
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        // Rotate the bitmap if necessary
        return rotateBitmap(
            bitmap,
            image.imageInfo.rotationDegrees.toFloat()
        ) // Adjust the angle as needed
    }


    companion object {
        private const val TAG = "CameraFragment"
        private val REQUIRED_PERMISSIONS = arrayOf(CAMERA, INTERNET)

    }

    fun createTraceableFromGeminiResponse(response: String): Traceable {
        println(response)

        val objectName = Regex("objectName: (.*)").find(response)?.groupValues?.get(1)
        val material = Regex("material: (.*)").find(response)?.groupValues?.get(1)
        val amount = Regex("amount: (.*)").find(response)?.groupValues?.get(1)
        val unit = Regex("unit: (.*)").find(response)?.groupValues?.get(1)


        return Traceable(
            0,
            objectName.toString(),
            material.toString(),
            "$amount $unit",
            "",
            0,
            "0.0"
        )

    }


}