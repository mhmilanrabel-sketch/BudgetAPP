package com.example.moneymanager.data.receipt

import android.graphics.Bitmap
import android.net.Uri
import android.content.Context
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ParsedReceipt(
    val merchant: String?,
    val amount: Double?,
    val rawText: String
)

object ReceiptScanner {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun scan(context: Context, uri: Uri): ParsedReceipt {
        val image = InputImage.fromFilePath(context, uri)
        return scanImage(image)
    }

    suspend fun scan(bitmap: Bitmap): ParsedReceipt {
        val image = InputImage.fromBitmap(bitmap, 0)
        return scanImage(image)
    }

    private suspend fun scanImage(image: InputImage): ParsedReceipt =
        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val fullText = visionText.text
                    cont.resume(parse(fullText))
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }

    // ─────────────────────────────────────────────────────────────
    // Regex parser for Sri Lankan receipts
    // ─────────────────────────────────────────────────────────────
    private val AMOUNT_REGEXES = listOf(
        // "Rs. 1,234.56" / "Rs 1234.56" / "LKR 1,234"
        Regex("""(?i)(?:rs\.?|lkr)\s*([\d,]+(?:\.\d{1,2})?)"""),
        // "Total 1,234.56" / "Amount 1234"
        Regex("""(?i)(?:total|amount|net|balance)[^\d]{0,10}([\d,]+(?:\.\d{1,2})?)"""),
        // bare decimal number at end of line, e.g. "1,234.56"
        Regex("""(?:^|\s)([\d,]{2,}\.\d{2})\s*$""", RegexOption.MULTILINE)
    )

    private val MERCHANT_HINTS = listOf(
        "keells", "cargills", "arpico", "laugfs", "glomark", "spar",
        "food city", "super", "pharmacy", "hotel", "restaurant",
        "cafe", "kfc", "pizza", "burger", "uber", "pickme"
    )

    private fun parse(text: String): ParsedReceipt {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // ── Merchant: prefer a known brand name; fall back to the first line
        val merchant = lines.firstOrNull { line ->
            MERCHANT_HINTS.any { line.lowercase().contains(it) }
        } ?: lines.firstOrNull { line ->
            line.length in 3..40 &&
            !line.any { it.isDigit() } &&
            !line.startsWith("Rs", ignoreCase = true)
        }

        // ── Amount: try each regex, take the largest match
        val candidates = mutableListOf<Double>()
        for (re in AMOUNT_REGEXES) {
            re.findAll(text).forEach { m ->
                val value = m.groupValues[1].replace(",", "").toDoubleOrNull()
                if (value != null && value > 0) candidates += value
            }
        }
        val amount = candidates.maxOrNull()

        return ParsedReceipt(
            merchant = merchant?.takeIf { it.isNotBlank() },
            amount = amount,
            rawText = text
        )
    }

    fun close() {
        recognizer.close()
    }
}