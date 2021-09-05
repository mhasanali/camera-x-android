package com.example.custom_camera

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.custom_camera.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var binding: ActivityMainBinding
    private var isRecording: Boolean = false
    private lateinit var videoCapture: VideoCapture
    private val REQUIRED_PERMISSIONS =
        arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO)
//    private lateinit var videoCapture: VideoCapture


    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        videoCapture=VideoCapture.Builder().setVideoFrameRate(15).build()


        // hide the action bar
        supportActionBar?.hide()

        // Check camera permissions if all permission granted
        // start camera else ask for the permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // set on click listener for the button of capture photo
        // it calls a method which is implemented below
        findViewById<Button>(R.id.camera_capture_button).setOnClickListener {
            takePhoto()
        }
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    @SuppressLint("RestrictedApi")
    private fun takePhoto() {
        isRecording = !isRecording

        if(!isRecording){
            videoCapture.stopRecording()
            binding.cameraCaptureButton.text = "Start Recording"
            return
        }
        binding.cameraCaptureButton.text = "Stop Recording"
        startRecording()


    }
    @SuppressLint("RestrictedApi")
    fun startRecording(){
        val videoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".mp4")
        val outputOptions = VideoCapture.OutputFileOptions.Builder(videoFile).build()
        videoCapture.startRecording(outputOptions, ContextCompat.getMainExecutor(this), object: VideoCapture.OnVideoSavedCallback{
            override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                Toast.makeText(this@MainActivity, "Video Saved", LENGTH_SHORT).show()
            }
            override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                Toast.makeText(this@MainActivity, message, LENGTH_SHORT).show()
            }
        })
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {

            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()


            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }


            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // creates a folder inside internal storage
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    // checks the camera permission
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // If all permissions granted , then start Camera
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                // If permissions are not granted,
                // present a toast to notify the user that
                // the permissions were not granted.
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "CameraXGFG"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 20
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
