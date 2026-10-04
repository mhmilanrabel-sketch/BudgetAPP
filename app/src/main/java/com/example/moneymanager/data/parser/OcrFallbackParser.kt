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

    /**
     * Performs on-device ML Kit OCR on a scanned Salary Slip bitmap.
     * 100% offline, zero network requests.
     */
    suspend fun recognizeAndParseBitmap(
        bitmap: Bitmap,
        targetMonthKey: String? = null
    ): ParsedSalarySlip {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)

        val ocrText = suspendCancellableCoroutine<String> { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }

        if (ocrText.isBlank()) {
            throw IllegalArgumentException("OCR completed but could not detect readable text on image.")
        }

        return paySlipParser.parseText(ocrText, targetMonthKey)
    }
}
