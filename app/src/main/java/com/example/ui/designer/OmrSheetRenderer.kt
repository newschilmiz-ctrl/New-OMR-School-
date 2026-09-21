package com.example.ui.designer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.io.File
import java.io.FileOutputStream

object OmrSheetRenderer {

    const val CANVAS_WIDTH = 1000
    const val CANVAS_HEIGHT = 1414

    /**
     * Renders the elements list onto a high-res 1000x1414 Android Bitmap
     */
    fun renderToBitmap(elements: List<OmrElement>): Bitmap {
        val bitmap = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val sortedElements = elements.sortedBy { it.zIndex }
        for (element in sortedElements) {
            drawElement(canvas, element)
        }

        return bitmap
    }

    private fun drawElement(canvas: Canvas, el: OmrElement) {
        when (el.type) {
            ElementType.REGISTRATION_MARKERS -> drawRegistrationMarkers(canvas, el)
            ElementType.HEADER_TITLE,
            ElementType.SUBTITLE,
            ElementType.CUSTOM_TEXT -> drawTextElement(canvas, el)
            ElementType.QUESTION_BLOCK -> drawQuestionBlock(canvas, el)
            ElementType.ROLL_NO_GRID -> drawRollNoGrid(canvas, el)
            ElementType.STUDENT_INFO_BOX -> drawStudentInfoBox(canvas, el)
            ElementType.INSTRUCTIONS_BOX -> drawInstructionsBox(canvas, el)
            ElementType.SIGNATURE_BOX -> drawSignatureBox(canvas, el)
            ElementType.BARCODE_QR -> drawBarcodeQr(canvas, el)
            ElementType.DIVIDER_LINE -> drawDividerLine(canvas, el)
        }
    }

    private fun drawRegistrationMarkers(canvas: Canvas, el: OmrElement) {
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val markerSize = 40f
        val margin = 20f

        // Top-Left
        canvas.drawRect(margin, margin, margin + markerSize, margin + markerSize, paint)
        // Top-Right
        canvas.drawRect(CANVAS_WIDTH - margin - markerSize, margin, CANVAS_WIDTH - margin, margin + markerSize, paint)
        // Bottom-Left
        canvas.drawRect(margin, CANVAS_HEIGHT - margin - markerSize, margin + markerSize, CANVAS_HEIGHT - margin, paint)
        // Bottom-Right
        canvas.drawRect(CANVAS_WIDTH - margin - markerSize, CANVAS_HEIGHT - margin - markerSize, CANVAS_WIDTH - margin, CANVAS_HEIGHT - margin, paint)
    }

    private fun drawTextElement(canvas: Canvas, el: OmrElement) {
        if (el.backgroundColorHex != null) {
            val bgPaint = Paint().apply {
                color = el.backgroundColorHex.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawRect(el.x, el.y, el.x + el.width, el.y + el.height, bgPaint)
        }

        if (el.hasBorder) {
            val borderPaint = Paint().apply {
                color = el.textColorHex.toInt()
                style = Paint.Style.STROKE
                strokeWidth = el.borderWidth
                isAntiAlias = true
            }
            canvas.drawRect(el.x, el.y, el.x + el.width, el.y + el.height, borderPaint)
        }

        val textPaint = Paint().apply {
            color = el.textColorHex.toInt()
            textSize = el.fontSize
            typeface = if (el.isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            isAntiAlias = true
            textAlign = when (el.alignment) {
                TextAlignment.LEFT -> Paint.Align.LEFT
                TextAlignment.CENTER -> Paint.Align.CENTER
                TextAlignment.RIGHT -> Paint.Align.RIGHT
            }
        }

        val drawX = when (el.alignment) {
            TextAlignment.LEFT -> el.x + 8f
            TextAlignment.CENTER -> el.x + el.width / 2f
            TextAlignment.RIGHT -> el.x + el.width - 8f
        }

        // Split multi-line text if any
        val lines = el.text.split("\n")
        val fontMetrics = textPaint.fontMetrics
        val lineHeight = fontMetrics.descent - fontMetrics.ascent + 4f
        val totalTextH = lines.size * lineHeight
        val startY = el.y + (el.height - totalTextH) / 2f - fontMetrics.ascent

        for (i in lines.indices) {
            canvas.drawText(lines[i], drawX, startY + i * lineHeight, textPaint)
        }
    }

    private fun drawDividerLine(canvas: Canvas, el: OmrElement) {
        val paint = Paint().apply {
            color = el.textColorHex.toInt()
            strokeWidth = el.lineThickness
            style = Paint.Style.STROKE
            isAntiAlias = true
            if (el.isDashed) {
                pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
            }
        }
        val midY = el.y + el.height / 2f
        canvas.drawLine(el.x, midY, el.x + el.width, midY, paint)
    }

    private fun drawQuestionBlock(canvas: Canvas, el: OmrElement) {
        // Optional boundary box
        if (el.hasBorder) {
            val borderPaint = Paint().apply {
                color = Color.DKGRAY
                style = Paint.Style.STROKE
                strokeWidth = el.borderWidth
                isAntiAlias = true
            }
            canvas.drawRect(el.x, el.y, el.x + el.width, el.y + el.height, borderPaint)
        }

        val totalQ = el.numQuestions
        val cols = el.numColumns.coerceIn(1, 4)
        val qPerCol = Math.ceil(totalQ.toDouble() / cols).toInt()
        val colWidth = el.width / cols

        val qNumPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val bubbleOutlinePaint = Paint().apply {
            color = el.bubbleColorHex.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
            isAntiAlias = true
        }

        val optionTextPaint = Paint().apply {
            color = el.bubbleColorHex.toInt()
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val optLabels = if (el.optionsFormat == "1234") listOf("1", "2", "3", "4", "5") else listOf("A", "B", "C", "D", "E")
        val numOpts = el.numOptions.coerceIn(3, 5)

        val verticalPadding = 20f
        val usableHeight = el.height - verticalPadding * 2
        val rowHeight = (usableHeight / qPerCol).coerceAtLeast(18f)
        val bubbleRadius = el.bubbleRadius.coerceIn(7f, 15f)
        val bubbleSpacing = (bubbleRadius * 2.5f).coerceIn(20f, 36f)

        for (qIdx in 0 until totalQ) {
            val col = qIdx / qPerCol
            val row = qIdx % qPerCol
            val qNum = el.startQuestionNum + qIdx

            val colStartX = el.x + col * colWidth
            val rowCenterY = el.y + verticalPadding + row * rowHeight + rowHeight / 2f

            // Draw Question Number
            val qNumX = colStartX + 36f
            canvas.drawText("$qNum.", qNumX, rowCenterY + 4f, qNumPaint)

            // Draw Option Bubbles
            val bubblesStartX = qNumX + 18f
            for (opt in 0 until numOpts) {
                val cx = bubblesStartX + opt * bubbleSpacing + bubbleRadius
                val cy = rowCenterY
                canvas.drawCircle(cx, cy, bubbleRadius, bubbleOutlinePaint)

                // Draw letter inside bubble
                val label = optLabels.getOrElse(opt) { ('A' + opt).toString() }
                canvas.drawText(label, cx, cy + 3.5f, optionTextPaint)
            }

            // Divider line between columns if multiple
            if (col < cols - 1 && row == 0) {
                val dividerX = colStartX + colWidth
                val divPaint = Paint().apply {
                    color = Color.LTGRAY
                    strokeWidth = 1f
                }
                canvas.drawLine(dividerX, el.y + 10f, dividerX, el.y + el.height - 10f, divPaint)
            }
        }
    }

    private fun drawRollNoGrid(canvas: Canvas, el: OmrElement) {
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        canvas.drawRect(el.x, el.y, el.x + el.width, el.y + el.height, borderPaint)

        // Header Title Bar
        val headerPaint = Paint().apply {
            color = 0xFFF1F5F9.toInt()
            style = Paint.Style.FILL
        }
        val headerH = 26f
        canvas.drawRect(el.x, el.y, el.x + el.width, el.y + headerH, headerPaint)
        canvas.drawLine(el.x, el.y + headerH, el.x + el.width, el.y + headerH, borderPaint)

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(el.gridTitle, el.x + el.width / 2f, el.y + 18f, titlePaint)

        val digits = el.digitsCount.coerceIn(4, 10)
        val colW = el.width / digits
        val boxH = 28f
        val boxStartY = el.y + headerH

        // Top empty write-in boxes
        val boxPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        for (i in 0 until digits) {
            val bx = el.x + i * colW
            canvas.drawRect(bx, boxStartY, bx + colW, boxStartY + boxH, boxPaint)
        }

        // Bubbles for 0-9
        val bubbleStartY = boxStartY + boxH + 8f
        val bubbleAreaH = el.height - (boxStartY + boxH + 8f - el.y)
        val rowH = bubbleAreaH / 10f
        val radius = el.bubbleRadius.coerceIn(6f, 11f)

        val bubblePaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            isAntiAlias = true
        }

        val digitTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        for (d in 0..9) {
            val cy = bubbleStartY + d * rowH + rowH / 2f
            for (col in 0 until digits) {
                val cx = el.x + col * colW + colW / 2f
                canvas.drawCircle(cx, cy, radius, bubblePaint)
                canvas.drawText(d.toString(), cx, cy + 3.2f, digitTextPaint)
            }
        }
    }

    private fun drawStudentInfoBox(canvas: Canvas, el: OmrElement) {
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        canvas.drawRect(el.x, el.y, el.x + el.width, el.y + el.height, borderPaint)

        val headerH = 24f
        val headerBg = Paint().apply {
            color = 0xFFF8FAFC.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(el.x, el.y, el.x + el.width, el.y + headerH, headerBg)
        canvas.drawLine(el.x, el.y + headerH, el.x + el.width, el.y + headerH, borderPaint)

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }
        canvas.drawText("CANDIDATE DETAILS (IN CAPITAL LETTERS)", el.x + 12f, el.y + 16f, titlePaint)

        val fields = listOf("CANDIDATE NAME:", "FATHER'S NAME:", "ROLL NO / ENROLLMENT:", "SUBJECT / CODE:", "EXAM DATE:")
        val usableH = el.height - headerH
        val rowH = usableH / fields.size

        val fieldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        for (i in fields.indices) {
            val y = el.y + headerH + i * rowH
            canvas.drawText(fields[i], el.x + 12f, y + rowH * 0.65f, fieldPaint)
            if (i < fields.size - 1) {
                canvas.drawLine(el.x, y + rowH, el.x + el.width, y + rowH, linePaint)
            }
        }
    }

    private fun drawInstructionsBox(canvas: Canvas, el: OmrElement) {
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        canvas.drawRect(el.x, el.y, el.x + el.width, el.y + el.height, borderPaint)

        val headerH = 22f
        val headerBg = Paint().apply {
            color = 0xFFFEF2F2.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(el.x, el.y, el.x + el.width, el.y + headerH, headerBg)
        canvas.drawLine(el.x, el.y + headerH, el.x + el.width, el.y + headerH, borderPaint)

        val headerTitle = Paint().apply {
            color = 0xFF991B1B.toInt()
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("IMPORTANT INSTRUCTIONS FOR CANDIDATES", el.x + 10f, el.y + 15f, headerTitle)

        val lines = el.text.ifEmpty {
            "• Use Black or Blue ball-point pen only.\n• Darken the bubble completely and evenly.\n• Multiple answers or cutting will be marked incorrect."
        }.split("\n")

        val textPaint = Paint().apply {
            color = 0xFF1E293B.toInt()
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        var startY = el.y + headerH + 18f
        for (line in lines) {
            canvas.drawText(line, el.x + 10f, startY, textPaint)
            startY += 18f
        }
    }

    private fun drawSignatureBox(canvas: Canvas, el: OmrElement) {
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        canvas.drawRect(el.x, el.y, el.x + el.width, el.y + el.height, borderPaint)

        val midX = el.x + el.width / 2f
        canvas.drawLine(midX, el.y, midX, el.y + el.height, borderPaint)

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Left box (Student Sign)
        canvas.drawText(el.studentSignLabel, el.x + (midX - el.x) / 2f, el.y + el.height - 12f, textPaint)
        // Right box (Invigilator Sign)
        canvas.drawText(el.invigilatorSignLabel, midX + (el.x + el.width - midX) / 2f, el.y + el.height - 12f, textPaint)
    }

    private fun drawBarcodeQr(canvas: Canvas, el: OmrElement) {
        val format = if (el.barcodeFormat == "BARCODE") BarcodeFormat.CODE_128 else BarcodeFormat.QR_CODE
        val w = el.width.toInt().coerceAtLeast(30)
        val h = el.height.toInt().coerceAtLeast(30)

        try {
            val writer = MultiFormatWriter()
            val matrix = writer.encode(el.barcodeContent.ifEmpty { "OMR-EXAM" }, format, w, h)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            for (x in 0 until w) {
                for (y in 0 until h) {
                    bmp.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            canvas.drawBitmap(bmp, el.x, el.y, null)
        } catch (e: Exception) {
            // Draw placeholder box
            val paint = Paint().apply {
                color = Color.LTGRAY
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRect(el.x, el.y, el.x + el.width, el.y + el.height, paint)
        }
    }

    /**
     * Generates a standard A4 PDF Document from the elements
     */
    fun generatePdf(context: Context, elements: List<OmrElement>, fileName: String = "custom_omr_sheet.pdf"): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points
        val page = pdfDocument.startPage(pageInfo)

        val highResBitmap = renderToBitmap(elements)
        val scaledBitmap = Bitmap.createScaledBitmap(highResBitmap, 595, 842, true)
        page.canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
        pdfDocument.finishPage(page)

        val cacheDir = context.cacheDir
        val file = File(cacheDir, fileName)
        val fos = FileOutputStream(file)
        pdfDocument.writeTo(fos)
        fos.close()
        pdfDocument.close()

        return file
    }

    /**
     * Triggers the Android system Print Dialog
     */
    fun printOmrSheet(context: Context, elements: List<OmrElement>, jobName: String = "OMR Sheet") {
        try {
            val pdfFile = generatePdf(context, elements, "print_omr.pdf")
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val printAdapter = object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: android.os.CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: android.os.Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = android.print.PrintDocumentInfo.Builder("OMR_Sheet.pdf")
                            .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(1)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out android.print.PageRange>?,
                        destination: android.os.ParcelFileDescriptor?,
                        cancellationSignal: android.os.CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        try {
                            val inputStream = pdfFile.inputStream()
                            val outputStream = FileOutputStream(destination?.fileDescriptor)
                            inputStream.copyTo(outputStream)
                            inputStream.close()
                            outputStream.close()
                            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message)
                        }
                    }
                }
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            } else {
                Toast.makeText(context, "Printing service unavailable", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error printing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares the generated OMR Sheet PDF via Intent
     */
    fun sharePdf(context: Context, elements: List<OmrElement>) {
        try {
            val file = generatePdf(context, elements, "OMR_Sheet_${System.currentTimeMillis()}.pdf")
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share OMR Sheet"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
