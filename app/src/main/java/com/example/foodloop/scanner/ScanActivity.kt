
package com.example.foodloop.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.foodloop.R
import java.io.File

class ScanActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var scanResult: TextView

    private var imageCapture: ImageCapture? = null

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Kamerazugriff verweigert",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_scan)

        previewView = findViewById(R.id.camera_preview)
        scanResult = findViewById(R.id.txt_scan_result)

        findViewById<Button>(R.id.btn_capture)
            .setOnClickListener {
                takePhoto()
            }

        findViewById<Button>(R.id.btn_cancel_scan)
            .setOnClickListener {
                finish()
            }

        checkCameraPermission()
    }

    private fun checkCameraPermission() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    private fun startCamera() {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            try {

                val cameraProvider =
                    cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(
                            previewView.surfaceProvider
                        )
                    }

                imageCapture = ImageCapture.Builder()
                    .build()

                val cameraSelector =
                    CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )

            } catch (e: Exception) {

                scanResult.text =
                    "Kamerafehler: ${e.message}"
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {

        val capture = imageCapture

        if (capture == null) {
            scanResult.text = "Kamera noch nicht bereit"
            return
        }

        val photoFile = File(
            cacheDir,
            "food_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions =
            ImageCapture.OutputFileOptions
                .Builder(photoFile)
                .build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    output: ImageCapture.OutputFileResults
                ) {

                    scanResult.text =
                        "Foto erfolgreich aufgenommen"

                    Toast.makeText(
                        this@ScanActivity,
                        "Foto gespeichert",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {

                    scanResult.text =
                        "Fehler: ${exception.message}"
                }
            }
        )
    }
}
