package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Exam
import com.example.data.Student
import com.example.util.CodeGenerator
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdmitCardsTab(
    exam: Exam,
    students: List<Student>
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    // Filter students enrolled in this exam's subject
    val examStudents = remember(students, exam.subject) {
        students.filter { exam.subject.isBlank() || it.subjects.contains(exam.subject, ignoreCase = true) }
            .ifEmpty { students }
    }

    val filtered = remember(examStudents, searchQuery) {
        examStudents.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.rollNo.contains(searchQuery, ignoreCase = true)
        }
    }

    // Chunk students in pairs of 2 for A4 Landscape dual admit cards
    val pairs = remember(filtered) {
        filtered.chunked(2)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & Print Action Banner
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Exam Admit Cards (A4 Landscape)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF10B981)) {
                                Text(
                                    "2 PER A4 PAGE",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Paper-Saving Layout: Exactly 2 admit cards placed horizontally on every A4 page",
                            fontSize = 10.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Button(
                        onClick = {
                            generateDualAdmitCardsPdf(context, exam, examStudents)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search student name or roll no...", fontSize = 11.5.sp) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
        }

        // List of A4 Landscape Pages with 2 Cards Horizontally
        items(pairs) { pair ->
            A4LandscapeDualAdmitCardPage(exam = exam, student1 = pair[0], student2 = pair.getOrNull(1))
        }
    }
}

/**
 * A simulated A4 Landscape Sheet containing 2 Admit Cards side-by-side horizontally.
 */
@Composable
fun A4LandscapeDualAdmitCardPage(
    exam: Exam,
    student1: Student,
    student2: Student?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        color = Color(0xFFF8FAFC),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Label showing page division
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("📄 A4 Landscape Sheet (Page Preview)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                Text("2 Admit Cards Horizontally", fontSize = 9.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
            }

            // Two Admit Cards Horizontally (Side by Side)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SingleAdmitCardView(exam = exam, student = student1)
                }

                if (student2 != null) {
                    Box(modifier = Modifier.weight(1f)) {
                        SingleAdmitCardView(exam = exam, student = student2)
                    }
                } else {
                    // Empty half to show printable paper slot
                    Surface(
                        modifier = Modifier.weight(1f).height(240.dp),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        color = Color.White
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Empty Slot (End of List)", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SingleAdmitCardView(exam: Exam, student: Student) {
    val photoBitmap = remember(student.imagePath) {
        if (student.imagePath.isNotBlank()) {
            try { BitmapFactory.decodeFile(student.imagePath) } catch (e: Exception) { null }
        } else null
    }

    val barcodeBitmap by produceState<Bitmap?>(initialValue = null, student.rollNo) {
        value = CodeGenerator.getBarcodeBitmapAsync(student.rollNo, 220, 50)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.2.dp, Color(0xFF0F172A)),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("SP ACADEMIC EVALUATION CENTER", fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    Text("OFFICIAL EXAMINATION ADMIT CARD", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                }
                Surface(color = Color(0xFF0F172A), shape = RoundedCornerShape(2.dp)) {
                    Text(exam.subject.uppercase(), fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.8.dp, color = Color(0xFF0F172A))

            // Body: Details & Photo
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Candidate: ${student.name}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("Roll No: ${student.rollNo}", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("Reg No: ${student.registrationNo}", fontSize = 7.5.sp, color = Color(0xFF475569))
                    Text("Father: ${student.fatherName}", fontSize = 7.5.sp, color = Color(0xFF475569))
                    Text("Exam: ${exam.name}", fontSize = 7.5.sp, color = Color(0xFF475569))
                    Text("Schedule: ${if (exam.examSchedule.isNotBlank()) exam.examSchedule else exam.date}", fontSize = 7.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF059669))
                    Text("Center: Main Campus Hall", fontSize = 7.5.sp, color = Color(0xFF475569))
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .size(46.dp, 56.dp)
                        .border(0.8.dp, Color(0xFF0F172A))
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("PHOTO", fontSize = 7.sp, color = Color(0xFF94A3B8))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Barcode
            barcodeBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.height(18.dp).fillMaxWidth(),
                    contentScale = ContentScale.FillBounds
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Signatures
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.width(70.dp).height(0.8.dp).background(Color(0xFF0F172A)))
                    Text("Candidate Sign", fontSize = 6.5.sp, color = Color(0xFF475569))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.width(70.dp).height(0.8.dp).background(Color(0xFF0F172A)))
                    Text("Controller of Exam", fontSize = 6.5.sp, color = Color(0xFF475569))
                }
            }
        }
    }
}

/**
 * Generates an A4 Landscape PDF with 2 Admit Cards horizontally on each page.
 */
fun generateDualAdmitCardsPdf(context: Context, exam: Exam, students: List<Student>) {
    try {
        val pdfDocument = PdfDocument()
        val pairs = students.chunked(2)

        // A4 Landscape: 842 pt width x 595 pt height
        val pageWidth = 842
        val pageHeight = 595

        val paint = Paint().apply { isAntiAlias = true }

        pairs.forEachIndexed { pageIndex, pair ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Left Card (Student 1): x from 25 to 405 (width = 380)
            drawAdmitCardOnCanvas(canvas, exam, pair[0], 25f, 30f, 380f, 530f, paint)

            // Right Card (Student 2): x from 435 to 815 (width = 380)
            if (pair.size > 1) {
                drawAdmitCardOnCanvas(canvas, exam, pair[1], 435f, 30f, 380f, 530f, paint)
            }

            // Cut guideline in the middle
            paint.strokeWidth = 0.5f
            paint.color = android.graphics.Color.GRAY
            canvas.drawLine(421f, 15f, 421f, 580f, paint)

            pdfDocument.finishPage(page)
        }

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Admit_Cards_${exam.name.replace(" ", "_")}_A4_Landscape.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        Toast.makeText(context, "Dual Admit Cards PDF Saved: ${file.name}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to export PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun drawAdmitCardOnCanvas(
    canvas: android.graphics.Canvas,
    exam: Exam,
    student: Student,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    paint: Paint
) {
    // Outer border
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f
    paint.color = android.graphics.Color.BLACK
    canvas.drawRect(x, y, x + width, y + height, paint)

    paint.style = Paint.Style.FILL

    // Header
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 12f
    canvas.drawText("SP EVALUATION CENTER", x + 12f, y + 25f, paint)

    paint.textSize = 9f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("OFFICIAL EXAMINATION ADMIT CARD", x + 12f, y + 40f, paint)

    paint.strokeWidth = 1f
    canvas.drawLine(x + 12f, y + 48f, x + width - 12f, y + 48f, paint)

    // Details
    var currentY = y + 70f
    paint.textSize = 10f
    paint.typeface = Typeface.DEFAULT_BOLD
    canvas.drawText("Name: ${student.name}", x + 15f, currentY, paint)
    currentY += 20f

    canvas.drawText("Roll No: ${student.rollNo}", x + 15f, currentY, paint)
    currentY += 20f

    paint.typeface = Typeface.DEFAULT
    canvas.drawText("Reg No: ${student.registrationNo}", x + 15f, currentY, paint)
    currentY += 20f

    canvas.drawText("Father: ${student.fatherName}", x + 15f, currentY, paint)
    currentY += 20f

    canvas.drawText("Class: ${student.className.ifEmpty { "General" }}", x + 15f, currentY, paint)
    currentY += 20f

    canvas.drawText("Subject: ${exam.subject}", x + 15f, currentY, paint)
    currentY += 20f

    canvas.drawText("Date/Schedule: ${exam.examSchedule.ifEmpty { exam.date }}", x + 15f, currentY, paint)
    currentY += 20f

    canvas.drawText("Exam Center: Main Examination Hall", x + 15f, currentY, paint)

    // Photo Box on top right
    paint.style = Paint.Style.STROKE
    canvas.drawRect(x + width - 90f, y + 60f, x + width - 20f, y + 145f, paint)
    paint.style = Paint.Style.FILL
    paint.textSize = 8f
    canvas.drawText("PHOTO", x + width - 65f, y + 105f, paint)

    // Signatures
    val signY = y + height - 35f
    canvas.drawLine(x + 20f, signY, x + 120f, signY, paint)
    canvas.drawLine(x + width - 130f, signY, x + width - 20f, signY, paint)
    paint.textSize = 8f
    canvas.drawText("Candidate Sign", x + 25f, signY + 14f, paint)
    canvas.drawText("Controller of Exam", x + width - 125f, signY + 14f, paint)
}
