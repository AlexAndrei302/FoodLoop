package com.example.foodloop.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
import java.util.UUID

class ScanActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var photoPreview: ImageView
    private lateinit var scanResult: TextView
    private lateinit var scanTitle: TextView

    private lateinit var captureButton: Button
    private lateinit var confirmButton: Button
    private lateinit var retakeButton: Button

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var currentPhoto: File? = null

    private val textRecognitionHelper by lazy {
        TextRecognitionHelper()
    }

    private var isProcessing = false
    private var recognizerUsed = false

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
        photoPreview = findViewById(R.id.photo_preview)
        scanResult = findViewById(R.id.txt_scan_result)
        scanTitle = findViewById(R.id.txt_scan_title)

        captureButton = findViewById(R.id.btn_capture)
        confirmButton = findViewById(R.id.btn_confirm_photo)
        retakeButton = findViewById(R.id.btn_retake_photo)

        captureButton.isEnabled = false

        captureButton.setOnClickListener {
            takePhoto()
        }

        confirmButton.setOnClickListener {
            confirmPhoto()
        }

        retakeButton.setOnClickListener {
            retakePhoto()
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

        captureButton.isEnabled = false

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            if (isFinishing || isDestroyed) {
                return@addListener
            }

            try {

                val provider = cameraProviderFuture.get()

                cameraProvider = provider

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(
                            previewView.surfaceProvider
                        )
                    }

                val capture = ImageCapture.Builder()
                    .build()

                provider.unbindAll()

                provider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture
                )

                imageCapture = capture
                captureButton.isEnabled = true

            } catch (exception: Exception) {

                scanResult.text =
                    "Kamerafehler: ${exception.message}"
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {

        val capture = imageCapture ?: return

        captureButton.isEnabled = false

        val photoFile = File(
            cacheDir,
            "food_${UUID.randomUUID()}.jpg"
        )

        currentPhoto = photoFile

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

                    if (isFinishing || isDestroyed) {
                        photoFile.delete()
                        return
                    }

                    showPhotoPreview(photoFile)
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {

                    photoFile.delete()
                    currentPhoto = null

                    scanResult.text =
                        "Fehler: ${exception.message}"

                    captureButton.isEnabled = true
                }
            }
        )
    }

    private fun showPhotoPreview(photoFile: File) {

        cameraProvider?.unbindAll()
        imageCapture = null

        previewView.visibility = View.GONE

        photoPreview.setImageURI(
            Uri.fromFile(photoFile)
        )

        photoPreview.visibility = View.VISIBLE

        captureButton.visibility = View.GONE
        confirmButton.visibility = View.VISIBLE
        retakeButton.visibility = View.VISIBLE

        confirmButton.isEnabled = true
        retakeButton.isEnabled = true

        scanTitle.text = "Foto überprüfen"

        scanResult.text =
            "Ist das Foto in Ordnung?"
    }

    private fun retakePhoto() {

        if (isProcessing) {
            return
        }

        photoPreview.setImageDrawable(null)

        currentPhoto?.delete()
        currentPhoto = null

        photoPreview.visibility = View.GONE
        previewView.visibility = View.VISIBLE

        captureButton.visibility = View.VISIBLE
        confirmButton.visibility = View.GONE
        retakeButton.visibility = View.GONE

        scanTitle.text = "Produkt scannen"

        scanResult.text =
            "Richte die Kamera auf das Produkt."

        startCamera()
    }

    private fun confirmPhoto() {

        val photo = currentPhoto ?: return

        if (isProcessing) {
            return
        }

        if (!photo.exists()) {

            scanResult.text = "Foto nicht gefunden"

            return
        }

        isProcessing = true

        confirmButton.isEnabled = false
        retakeButton.isEnabled = false

        scanTitle.text = "Foto wird analysiert"

        scanResult.text =
            "Text und Ablaufdatum werden erkannt..."

        recognizerUsed = true

        textRecognitionHelper.recognizeText(

            context = this,

            photoPath = photo.absolutePath,

            onSuccess = { recognizedText ->

                if (!isFinishing && !isDestroyed) {

                    isProcessing = false

                    val expirationDate =
                        ExpirationDateParser
                            .findExpirationDate(recognizedText)

                    showRecognitionResult(
                        recognizedText,
                        expirationDate
                    )
                }
            },

            onFailure = { exception ->

                if (!isFinishing && !isDestroyed) {

                    isProcessing = false

                    scanResult.text =
                        "Texterkennung fehlgeschlagen"

                    showRecognitionResult(
                        "",
                        null
                    )
                }
            }
        )
    }

    private fun showRecognitionResult(
        recognizedText: String,
        expirationDate: String?
    ) {

        scanTitle.text = "Scan-Ergebnis"

        confirmButton.isEnabled = true
        retakeButton.isEnabled = true

        scanResult.text = if (expirationDate != null) {

            "Mögliches Ablaufdatum: $expirationDate"

        } else {

            "Kein eindeutiges Ablaufdatum erkannt"
        }

        val message = if (expirationDate != null) {

            "Mögliches Ablaufdatum:\n" +
                    "$expirationDate\n\n" +
                    "Erkannter Text:\n" +
                    recognizedText.take(2000)

        } else {

            "Es wurde kein eindeutiges Ablaufdatum erkannt.\n" +
                    "Du kannst das Datum manuell eingeben.\n\n" +
                    "Erkannter Text:\n" +
                    recognizedText.take(2000)
        }

        AlertDialog.Builder(this)

            .setTitle("Texterkennung")

            .setMessage(message)

            .setPositiveButton("Weiter") { _, _ ->

                returnPhotoResult(
                    recognizedText,
                    expirationDate
                )
            }

            .setNegativeButton("Neues Foto") { _, _ ->

                retakePhoto()
            }

            .setNeutralButton("Ohne Datum") { _, _ ->

                returnPhotoResult(
                    recognizedText,
                    null
                )
            }

            .show()
    }

    private fun returnPhotoResult(
        recognizedText: String,
        expirationDate: String?
    ) {

        val photo = currentPhoto ?: return

        if (!photo.exists()) {
            return
        }

        val resultIntent = Intent()

        resultIntent.putExtra(
            "photoPath",
            photo.absolutePath
        )

        resultIntent.putExtra(
            "recognizedText",
            recognizedText.take(10000)
        )

        resultIntent.putExtra(
            "expirationDate",
            expirationDate
        )

        currentPhoto = null

        setResult(
            RESULT_OK,
            resultIntent
        )

        finish()
    }

    override fun onDestroy() {

        if (recognizerUsed) {
            textRecognitionHelper.close()
        }

        currentPhoto?.delete()

        super.onDestroy()
    }
}
