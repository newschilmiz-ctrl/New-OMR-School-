package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.BitmapFactory
import com.example.data.Student
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

object OmrGenerator {

    const val SHEET_WIDTH = 1000
    const val SHEET_HEIGHT = 1414
    
    private fun createBarcodeBitmap(contents: String, format: BarcodeFormat, width: Int, height: Int): Bitmap? {
        if (contents.isEmpty()) return null
        return try {
            val writer = MultiFormatWriter()
            val matrix = writer.encode(contents, format, width, height)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun drawOmrToCanvas(context: android.content.Context, canvas: Canvas, numQuestions: Int, numOptions: Int, student: Student? = null, title: String = "बिहार विद्यालय परीक्षा , समिति", logoPath: String = "", logoOpacity: Float = 0.2f, logoSize: Float = 100f, logoPosition: String = "Left", templateType: String = "Standard") {
        canvas.drawColor(Color.WHITE)
        
        // Draw Vertical Text on the left
        canvas.save()
        canvas.translate(95f, SHEET_HEIGHT - 100f)
        canvas.rotate(-90f)
        val verticalTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("EXAMINATION OMR SHEET MADE BY SUMIT KUMAR", 0f, 0f, verticalTextPaint)
        canvas.restore()

        // Draw watermark / logo if present
        if (logoPath.isNotEmpty()) {
            var logoBmp: Bitmap? = null
            if (logoPath.startsWith("content://")) {
                try {
                    val uri = android.net.Uri.parse(logoPath)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    logoBmp = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (logoPath.startsWith("http")) {
                try {
                    val url = java.net.URL(logoPath)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    logoBmp = BitmapFactory.decodeStream(connection.inputStream)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                logoBmp = BitmapFactory.decodeFile(logoPath)
            }
            if (logoBmp != null) {
                val alphaInt = (logoOpacity * 255).toInt().coerceIn(0, 255)
                val alphaPaint = Paint().apply { alpha = alphaInt }
                
                // Calculate position for top header
                val maxLogoSize = logoSize
                val scale = Math.min(maxLogoSize / logoBmp.width, maxLogoSize / logoBmp.height)
                val w = logoBmp.width * scale
                val h = logoBmp.height * scale
                
                val topMargin = 20f
                val marginX = 80f
                
                val left = when (logoPosition) {
                    "Left" -> marginX
                    "Right" -> SHEET_WIDTH - marginX - w
                    "Center" -> (SHEET_WIDTH - w) / 2f
                    else -> marginX
                }
                
                val top = topMargin
                
                canvas.drawBitmap(logoBmp, null, RectF(left, top, left + w, top + h), alphaPaint)
            }
        }

        val blackFill = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        val blackStroke = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        
        val thinStroke = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        
        val sectionTitlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        val smallTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        
        val lightGrayFill = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#E8E8E8")
            style = Paint.Style.FILL
        }
        
        val headerGrayFill = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#C0C0C0")
            style = Paint.Style.FILL
        }
        
        val headerTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        
        val leftAlignedSmallText = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        val bubbleTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        // Draw 4 Corner Registration Squares for Scanner Calibration
        val cornerSize = 40f
        val edgeMarginX = 30f
        val edgeMarginY = 30f
        
        // Top Left
        canvas.drawRect(edgeMarginX, edgeMarginY, edgeMarginX + cornerSize, edgeMarginY + cornerSize, blackFill)
        // Top Right
        canvas.drawRect(SHEET_WIDTH - edgeMarginX - cornerSize, edgeMarginY, SHEET_WIDTH - edgeMarginX, edgeMarginY + cornerSize, blackFill)
        // Bottom Left
        canvas.drawRect(edgeMarginX, SHEET_HEIGHT - edgeMarginY - cornerSize, edgeMarginX + cornerSize, SHEET_HEIGHT - edgeMarginY, blackFill)
        // Bottom Right
        canvas.drawRect(SHEET_WIDTH - edgeMarginX - cornerSize, SHEET_HEIGHT - edgeMarginY - cornerSize, SHEET_WIDTH - edgeMarginX, SHEET_HEIGHT - edgeMarginY, blackFill)

        // Draw timing marks along left and right edges
        val timingMarkW = 25f
        val timingMarkH = 8f
        val leftX = edgeMarginX + (cornerSize - timingMarkW) / 2f
        val rightX = SHEET_WIDTH - edgeMarginX - cornerSize + (cornerSize - timingMarkW) / 2f
        
        val startY = edgeMarginY + cornerSize + 30f
        val endY = SHEET_HEIGHT - edgeMarginY - cornerSize - 30f
        
        val numTimingMarks = 48
        val timingStepY = (endY - startY) / (numTimingMarks - 1)
        
        for (i in 0 until numTimingMarks) {
            val cy = startY + i * timingStepY
            canvas.drawRect(leftX, cy - timingMarkH / 2f, leftX + timingMarkW, cy + timingMarkH / 2f, blackFill)
            canvas.drawRect(rightX, cy - timingMarkH / 2f, rightX + timingMarkW, cy + timingMarkH / 2f, blackFill)
        }

        // --- TOP SECTION ---
        canvas.drawText(title, SHEET_WIDTH / 2f, 60f, titlePaint)

        val startX = 110f
        val col2X = 400f
        val col3X = 750f
        
        // OMR Serial - Removed as per user request
        
        
        if (templateType == "Standard") {
            // Fields (Left Column)
                    val textYStart = 220f
                    val lineSpacing = 40f
                    
                    canvas.drawText("1. परीक्षार्थी का नाम:- ${student?.name ?: "<<NAME>>"}", startX, textYStart, leftAlignedSmallText)
                    canvas.drawText("2. रोल नं:- ${student?.rollNo ?: "<<Roll>>"}", startX, textYStart + lineSpacing, leftAlignedSmallText)
                    canvas.drawText("3. विषय कोड:- 320", startX, textYStart + 2 * lineSpacing, leftAlignedSmallText)
                    canvas.drawText("4. विषय:- ${student?.subjects ?: "HISTORY"}", startX, textYStart + 3 * lineSpacing, leftAlignedSmallText)
            
                    // Barcode placeholder (Middle Column)
                    val barcodeY = 110f
                    val barcodeW = 200f
                    val barcodeH = 60f
                    val rollNoText = student?.rollNo ?: "8596312"
                    val barcodeBmp = createBarcodeBitmap(rollNoText, BarcodeFormat.PDF_417, barcodeW.toInt(), barcodeH.toInt())
                    // Draw scan-type corner borders around the 2D barcode area
                    val scanPadding = 5f
                    val bX = col2X - scanPadding
                    val bY = barcodeY - scanPadding
                    val bW = barcodeW + 2 * scanPadding
                    val bH = barcodeH + 2 * scanPadding
                    val cLen = 15f
                    
                    // top left
                    canvas.drawLine(bX, bY, bX + cLen, bY, thinStroke)
                    canvas.drawLine(bX, bY, bX, bY + cLen, thinStroke)
                    // top right
                    canvas.drawLine(bX + bW - cLen, bY, bX + bW, bY, thinStroke)
                    canvas.drawLine(bX + bW, bY, bX + bW, bY + cLen, thinStroke)
                    // bottom left
                    canvas.drawLine(bX, bY + bH, bX + cLen, bY + bH, thinStroke)
                    canvas.drawLine(bX, bY + bH - cLen, bX, bY + bH, thinStroke)
                    // bottom right
                    canvas.drawLine(bX + bW - cLen, bY + bH, bX + bW, bY + bH, thinStroke)
                    canvas.drawLine(bX + bW, bY + bH - cLen, bX + bW, bY + bH, thinStroke)
                    
                    if (barcodeBmp != null) {
                        canvas.drawBitmap(barcodeBmp, col2X, barcodeY, null)
                    } else {
                        // top left
                        canvas.drawLine(col2X, barcodeY, col2X + 15f, barcodeY, thinStroke) 
                        canvas.drawLine(col2X, barcodeY, col2X, barcodeY + 15f, thinStroke) 
                        // top right
                        canvas.drawLine(col2X + barcodeW - 15f, barcodeY, col2X + barcodeW, barcodeY, thinStroke) 
                        canvas.drawLine(col2X + barcodeW, barcodeY, col2X + barcodeW, barcodeY + 15f, thinStroke) 
                        // bottom left
                        canvas.drawLine(col2X, barcodeY + barcodeH, col2X + 15f, barcodeY + barcodeH, thinStroke) 
                        canvas.drawLine(col2X, barcodeY + barcodeH - 15f, col2X, barcodeY + barcodeH, thinStroke) 
                        // bottom right
                        canvas.drawLine(col2X + barcodeW - 15f, barcodeY + barcodeH, col2X + barcodeW, barcodeY + barcodeH, thinStroke) 
                        canvas.drawLine(col2X + barcodeW, barcodeY + barcodeH - 15f, col2X + barcodeW, barcodeY + barcodeH, thinStroke) 
                        
                        canvas.drawText("<<barcode", col2X + barcodeW / 2f, barcodeY + 28f, smallTextPaint)
                        canvas.drawText(">>", col2X + barcodeW / 2f, barcodeY + 52f, smallTextPaint)
                    }
            
                    // Fields (Middle Column)
                    canvas.drawText("5. पंजीयन सं:- ${student?.registrationNo ?: "<<Reg>>"}", col2X, textYStart, leftAlignedSmallText)
                    canvas.drawText("6. पाली:- FIRST", col2X, textYStart + lineSpacing, leftAlignedSmallText)
                    val todayStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                    canvas.drawText("7. परीक्षा की तिथि:- $todayStr", col2X, textYStart + 2 * lineSpacing, leftAlignedSmallText)
            
                    // Photo placeholder (Right Column)
                    val photoX = col3X
                    val photoY = 110f
                    val photoW = 110f
                    val photoH = 140f
                    val photoRect = RectF(photoX, photoY, photoX + photoW, photoY + photoH)
                    canvas.drawRect(photoRect, thinStroke)
                    if (student?.imagePath != null && student.imagePath.isNotEmpty()) {
                        var bmp: Bitmap? = null
                        if (student.imagePath.startsWith("http")) {
                            try {
                                val url = java.net.URL(student.imagePath)
                                val connection = url.openConnection() as java.net.HttpURLConnection
                                connection.doInput = true
                                connection.connect()
                                val input = connection.inputStream
                                bmp = BitmapFactory.decodeStream(input)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            bmp = BitmapFactory.decodeFile(student.imagePath)
                        }
                        if (bmp != null) {
                            canvas.drawBitmap(bmp, null, photoRect, null)
                        } else {
                            canvas.drawText("<<photo>>", photoRect.centerX(), photoRect.centerY(), smallTextPaint)
                        }
                    } else {
                        canvas.drawText("<<photo>>", photoRect.centerX(), photoRect.centerY(), smallTextPaint)
                    }
            
                    val vBarW = 50f
                    val vBarH = 140f
                    
                    val sideBarcodeW = 120
                    val sideBarcodeH = 30
                    val sideBarcodeBmp = createBarcodeBitmap(rollNoText, BarcodeFormat.CODE_128, sideBarcodeW, sideBarcodeH)
            
                    // Vertical Barcodes Left of Photo
                    val leftVBarcodeX = photoX - 70f
                    // top left
                    canvas.drawLine(leftVBarcodeX, photoY, leftVBarcodeX + 15f, photoY, thinStroke) 
                    canvas.drawLine(leftVBarcodeX, photoY, leftVBarcodeX, photoY + 15f, thinStroke) 
                    // top right
                    canvas.drawLine(leftVBarcodeX + vBarW - 15f, photoY, leftVBarcodeX + vBarW, photoY, thinStroke) 
                    canvas.drawLine(leftVBarcodeX + vBarW, photoY, leftVBarcodeX + vBarW, photoY + 15f, thinStroke) 
                    // bottom left
                    canvas.drawLine(leftVBarcodeX, photoY + vBarH, leftVBarcodeX + 15f, photoY + vBarH, thinStroke) 
                    canvas.drawLine(leftVBarcodeX, photoY + vBarH - 15f, leftVBarcodeX, photoY + vBarH, thinStroke) 
                    // bottom right
                    canvas.drawLine(leftVBarcodeX + vBarW - 15f, photoY + vBarH, leftVBarcodeX + vBarW, photoY + vBarH, thinStroke) 
                    canvas.drawLine(leftVBarcodeX + vBarW, photoY + vBarH - 15f, leftVBarcodeX + vBarW, photoY + vBarH, thinStroke) 
                    
                    if (sideBarcodeBmp != null) {
                        canvas.save()
                        canvas.translate(leftVBarcodeX + 10f, photoY + (vBarH / 2f) + (sideBarcodeW / 2f))
                        canvas.rotate(-90f)
                        canvas.drawBitmap(sideBarcodeBmp, 0f, 0f, null)
                        canvas.restore()
                    } else {
                        canvas.save()
                        canvas.translate(leftVBarcodeX + 15f, photoY + vBarH / 2f)
                        canvas.rotate(90f)
                        canvas.drawText("O V V", 0f, 0f, smallTextPaint)
                        canvas.restore()
                
                        canvas.save()
                        canvas.translate(leftVBarcodeX + 35f, photoY + vBarH / 2f)
                        canvas.rotate(90f)
                        canvas.drawText("^ < barcode", 0f, 0f, smallTextPaint)
                        canvas.restore()
                    }
            
                    // Vertical Barcodes Right of Photo
                    val rightVBarcodeX = photoX + photoW + 20f
                    // top left
                    canvas.drawLine(rightVBarcodeX, photoY, rightVBarcodeX + 15f, photoY, thinStroke) 
                    canvas.drawLine(rightVBarcodeX, photoY, rightVBarcodeX, photoY + 15f, thinStroke) 
                    // top right
                    canvas.drawLine(rightVBarcodeX + vBarW - 15f, photoY, rightVBarcodeX + vBarW, photoY, thinStroke) 
                    canvas.drawLine(rightVBarcodeX + vBarW, photoY, rightVBarcodeX + vBarW, photoY + 15f, thinStroke) 
                    // bottom left
                    canvas.drawLine(rightVBarcodeX, photoY + vBarH, rightVBarcodeX + 15f, photoY + vBarH, thinStroke) 
                    canvas.drawLine(rightVBarcodeX, photoY + vBarH - 15f, rightVBarcodeX, photoY + vBarH, thinStroke) 
                    // bottom right
                    canvas.drawLine(rightVBarcodeX + vBarW - 15f, photoY + vBarH, rightVBarcodeX + vBarW, photoY + vBarH, thinStroke) 
                    canvas.drawLine(rightVBarcodeX + vBarW, photoY + vBarH - 15f, rightVBarcodeX + vBarW, photoY + vBarH, thinStroke) 
                    
                    if (sideBarcodeBmp != null) {
                        canvas.save()
                        canvas.translate(rightVBarcodeX + 10f, photoY + (vBarH / 2f) + (sideBarcodeW / 2f))
                        canvas.rotate(-90f)
                        canvas.drawBitmap(sideBarcodeBmp, 0f, 0f, null)
                        canvas.restore()
                    } else {
                        canvas.save()
                        canvas.translate(rightVBarcodeX + 15f, photoY + vBarH / 2f)
                        canvas.rotate(90f)
                        canvas.drawText("O V V", 0f, 0f, smallTextPaint)
                        canvas.restore()
                
                        canvas.save()
                        canvas.translate(rightVBarcodeX + 35f, photoY + vBarH / 2f)
                        canvas.rotate(90f)
                        canvas.drawText("^ < barcode", 0f, 0f, smallTextPaint)
                        canvas.restore()
                    }
            
                    // QR Box
                    val qrBoxY = photoY + photoH + 20f
                    val qrBoxX = photoX + photoW / 2f
                    val qrSize = 90f
                    val qrBmp = createBarcodeBitmap(rollNoText, BarcodeFormat.QR_CODE, qrSize.toInt(), qrSize.toInt())
                    
                    if (qrBmp != null) {
                        canvas.drawBitmap(qrBmp, qrBoxX - qrSize / 2f, qrBoxY, null)
                    } else {
                        canvas.drawText("<<", qrBoxX, qrBoxY, smallTextPaint)
                        canvas.drawRect(qrBoxX - 25f, qrBoxY + 10f, qrBoxX + 25f, qrBoxY + 60f, thinStroke)
                        canvas.drawText("Q", qrBoxX, qrBoxY + 30f, smallTextPaint)
                        canvas.drawText("R", qrBoxX, qrBoxY + 50f, smallTextPaint)
                        canvas.drawText(">>", qrBoxX, qrBoxY + 75f, smallTextPaint)
                    }
            
                    
        } else {
            // Draw Roll No Bubbles (7 columns, 10 rows 0-9)
            val rStartX = 150f
            val rStartY = 120f
            val rSpacingX = 42f
            val rSpacingY = 28f
            val rBubbleRadius = 11f
            
            canvas.drawText("ROLL NUMBER", rStartX + 3 * rSpacingX, rStartY - 25f, titlePaint.apply { textSize = 20f; textAlign = Paint.Align.CENTER })
            
            for (col in 0 until 7) {
                // Draw Box for digit
                val cx = rStartX + col * rSpacingX
                canvas.drawRect(cx - 18f, rStartY - 18f, cx + 18f, rStartY + 2f, thinStroke)
                
                for (row in 0..9) {
                    val cy = rStartY + 28f + row * rSpacingY
                    canvas.drawCircle(cx, cy, rBubbleRadius, thinStroke)
                    canvas.drawText(row.toString(), cx, cy + 4f, smallTextPaint)
                }
            }
            
            // Signature box on the right
            val sigX = 650f
            val sigY = 220f
            val sigW = 250f
            val sigH = 80f
            canvas.drawRect(sigX, sigY, sigX + sigW, sigY + sigH, thinStroke)
            canvas.drawText("परीक्षार्थी का पूरा हस्ताक्षर", sigX + sigW / 2f, sigY + sigH + 25f, titlePaint.apply { textSize = 16f; textAlign = Paint.Align.CENTER })
            canvas.drawText("(Full Signature of Candidate)", sigX + sigW / 2f, sigY + sigH + 45f, smallTextPaint.apply { textAlign = Paint.Align.CENTER })
        }
    // Set Code Instructions
        val instrPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val textY = 410f + 40f
        val textX = (1000 - 80f - 150f - 20f) // marksBoxLeft - 20f
        canvas.drawText("निर्देश: परीक्षार्थी अपना प्रश्न पत्र सेट कोड नीचे (क्र. 10)", textX, textY, instrPaint)
        canvas.drawText("में अवश्य भरें, अन्यथा परिणाम अमान्य हो सकता है।", textX, textY + 30f, instrPaint)
        
        // --- MARKS BOX ---
        val marksBoxTop = 410f
        val marksBoxBottom = 495f
        val marksBoxRight = SHEET_WIDTH - 80f
        val marksBoxLeft = marksBoxRight - 150f
        
        canvas.drawRect(marksBoxLeft, marksBoxTop, marksBoxRight, marksBoxBottom, blackStroke)
        
        val marksTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        
        canvas.drawText("MARKS", marksBoxLeft + 75f, marksBoxTop + 25f, marksTextPaint)
        canvas.drawLine(marksBoxLeft + 15f, marksBoxTop + 55f, marksBoxRight - 15f, marksBoxTop + 55f, blackStroke)
        canvas.drawText("50", marksBoxLeft + 75f, marksBoxTop + 80f, marksTextPaint)

        // --- BOTTOM SECTION ---
        val boxTop = 510f
        val boxBottom = 1360f
        val boxLeft = 110f
        val boxRight = SHEET_WIDTH - 80f
        
        // Outer box
        canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, blackStroke)
        
        // Split line between left and right column
        val splitX = boxLeft + 160f
        canvas.drawLine(splitX, boxTop, splitX, boxBottom, blackStroke)
        
        // Top black header bar
        canvas.drawRect(boxLeft, boxTop, boxRight, boxTop + 40f, blackFill)
        
        // Left Column Title
        canvas.drawText("10. प्रश्न पत्र सेट कोड", boxLeft + 80f, boxTop + 25f, sectionTitlePaint)
        // Right Column Title
        canvas.drawText("वस्तुनिष्ठ प्रश्नों के उत्तर (For answering objective questions)", splitX + (boxRight - splitX) / 2f, boxTop + 25f, sectionTitlePaint)

        // --- Left Column: Set Codes ---
        val setSets = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
        val setBubbleRadius = 13f
        val setGridStartX = boxLeft + 105f
        val setGridStartY = boxTop + 80f
        val setRowHeight = 60f
        
        for (i in setSets.indices) {
            val cy = setGridStartY + i * setRowHeight
            canvas.drawText("SET-${setSets[i]}", boxLeft + 25f, cy + 6f, leftAlignedSmallText)
            canvas.drawCircle(setGridStartX, cy, setBubbleRadius, thinStroke)
            canvas.drawText(setSets[i], setGridStartX, cy + 5f, bubbleTextPaint)
        }

        // --- Right Column: Objective Questions Answers ---
        val numCols = 5
        val numQPerCol = 20
        val rightBoxWidth = boxRight - splitX
        val colWidth = rightBoxWidth / numCols
        val bubbleRadius = 10f
        val optSpacing = 21f
        val qRowHeight = 39f
        val optLetters = listOf("A", "B", "C", "D")
        
        var currentQ = 1
        
        for (col in 0 until numCols) {
            val startX = splitX + col * colWidth
            val endX = startX + colWidth
            val qNumX = startX + 18f
            val bubblesStartX = startX + 46f
            
            // Draw column separator if not first column
            if (col > 0) {
                canvas.drawLine(startX, boxTop + 40f, startX, boxBottom, blackStroke)
            }
            
            // Draw Column Header
            val headerTop = boxTop + 40f
            val headerBottom = headerTop + 30f
            canvas.drawRect(startX, headerTop, endX, headerBottom, headerGrayFill)
            canvas.drawText("No.", qNumX, headerTop + 20f, headerTextPaint)
            for (opt in 0..3) {
                canvas.drawText(optLetters[opt], bubblesStartX + opt * optSpacing, headerTop + 20f, headerTextPaint)
            }
            
            for (row in 0 until numQPerCol) {
                val rowTop = headerBottom + row * qRowHeight
                val rowBottom = rowTop + qRowHeight
                val cy = rowTop + qRowHeight / 2f
                
                val qString = currentQ.toString()
                
                // Draw Q Num
                canvas.drawText(qString, qNumX, cy + 6f, smallTextPaint)
                
                // Draw Options
                for (opt in 0..3) {
                    val cx = bubblesStartX + opt * optSpacing
                    canvas.drawCircle(cx, cy, bubbleRadius, thinStroke)
                    canvas.drawText(optLetters[opt], cx, cy + 4f, bubbleTextPaint)
                }
                currentQ++
            }
        }

    }

    fun generateOmrBitmap(context: android.content.Context, numQuestions: Int, numOptions: Int, student: Student? = null, title: String = "बिहार विद्यालय परीक्षा , समिति", logoPath: String = "", logoOpacity: Float = 0.2f, logoSize: Float = 100f, logoPosition: String = "Left", templateType: String = "Standard"): Bitmap {
        val bitmap = Bitmap.createBitmap(SHEET_WIDTH, SHEET_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawOmrToCanvas(context, canvas, numQuestions, numOptions, student, title, logoPath, logoOpacity, logoSize, logoPosition, templateType)
        return bitmap
    }
}

