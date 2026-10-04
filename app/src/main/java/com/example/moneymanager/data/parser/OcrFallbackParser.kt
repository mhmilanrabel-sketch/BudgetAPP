package com.example.moneymanager.data.parser

import android.content.Context
import android.graphics.Bitmap
import com.example.moneymanager.domain.model.ParsedSalarySlip
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OcrFallbackParser(
    private val context: Context,
    private val paySlipParser: PaySlipParser
) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeAndParse(bitmap: Bitmap): ParsedSalarySlip = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                try {
                    val slip = paySlipParser.parseFromText(visionText.text, isOcr = true)
                    continuation.resume(slip)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }
}
