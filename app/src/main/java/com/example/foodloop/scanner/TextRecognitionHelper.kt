package com.example.foodloop.scanner

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File

class TextRecognitionHelper {

    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    fun recognizeText(
        context: Context,
        photoPath: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {

        try {

            val image = InputImage.fromFilePath(
                context,
                Uri.fromFile(File(photoPath))
            )

            recognizer.process(image)
                .addOnSuccessListener { result ->

                    onSuccess(result.text)

                }
                .addOnFailureListener { exception ->

                    onFailure(exception)

                }

        } catch (exception: Exception) {

            onFailure(exception)

        }
    }

    fun close() {
        recognizer.close()
    }
}
