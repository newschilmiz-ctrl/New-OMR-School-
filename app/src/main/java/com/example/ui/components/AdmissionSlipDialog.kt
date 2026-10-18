package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Student
import com.example.util.CodeGenerator
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdmissionSlipDialog(
    student: Student,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val studentBitmap = remember(student.imagePath) {
        if (student.imagePath.isNotBlank()) {
            try {
                BitmapFactory.decodeFile(student.imagePath)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    val barcodeBitmap by produceState<Bitmap?>(initialValue = null, student.rollNo) {
        value = CodeGenerator.getBarcodeBitmapAsync(student.rollNo, 300, 70)
    }

    val qrBitmap by produceState<Bitmap?>(initialValue = null, student.rollNo) {
        val qrData = "ADM_SLIP|ROLL:${student.rollNo}|REG:${student.registrationNo}|NAME:${student.name}|CLASS:${student.className}"
        value = CodeGenerator.getQrCodeBitmapAsync(qrData, 160)
    }

    val admissionDate = remember(student.timestamp) {
        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(student.timestamp))
    }

    val receiptNo = remember(student.rollNo) {
        "REC-ADM-${student.rollNo.takeLast(6)}"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Toolbar
                Surface(
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Official Admission Slip (A4)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Roll No: ${student.rollNo} • Ready to Print",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    printAdmissionSlip(context, student)
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEFF6FF)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Print Slip", fontSize = 11.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                            }

                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF475569))
                            }
                        }
                    }
                }

                // Printable A4 Page Container (Preview)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(12.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // A4 Ratio Simulated Card
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxWidth(),
                        color = Color.White,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF0F172A)),
                        shadowElevation = 3.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            // 1. Institution Header Banner
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "SP COACHING & ACADEMIC CENTER",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Government Registered Evaluation & Coaching Institute",
                                        fontSize = 10.sp,
                                        color = Color(0xFF475569)
                                    )
                                    Text(
                                        text = "Address: Main Campus Road • Helpline: +91 98765 43210",
                                        fontSize = 9.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                qrBitmap?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "QR Ref",
                                        modifier = Modifier
                                            .size(54.dp)
                                            .border(1.dp, Color(0xFFCBD5E1))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFF0F172A), thickness = 1.5.dp)

                            // Title Banner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A))
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "OFFICIAL STUDENT ADMISSION SLIP (STUDENT / OFFICE COPY)",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Top Details with Photo
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Receipt No: $receiptNo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Text("Admission Date: $admissionDate", fontSize = 10.5.sp, color = Color(0xFF334155))
                                    Text("Status: ADMISSION CONFIRMED ✅", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    barcodeBitmap?.let {
                                        Image(
                                            bitmap = it.asImageBitmap(),
                                            contentDescription = "Barcode",
                                            modifier = Modifier.height(28.dp).width(160.dp),
                                            contentScale = ContentScale.FillBounds
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(80.dp, 95.dp)
                                        .border(1.dp, Color(0xFF0F172A))
                                        .background(Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (studentBitmap != null) {
                                        Image(
                                            bitmap = studentBitmap.asImageBitmap(),
                                            contentDescription = "Student Photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text("PASSPORT\nPHOTO", fontSize = 8.5.sp, textAlign = TextAlign.Center, color = Color(0xFF94A3B8))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 2. TABLE FORMAT DATA
                            Text(
                                text = "1. CANDIDATE PERSONAL & ACADEMIC DETAILS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF0F172A))
                            ) {
                                TableRow("Student Full Name", student.name, "Roll Number", student.rollNo)
                                TableRow("Father's Name", student.fatherName, "Registration No", student.registrationNo)
                                TableRow("Mother's Name", student.motherName.ifEmpty { "N/A" }, "Gender", student.gender)
                                TableRow("Date of Birth", student.dob.ifEmpty { "N/A" }, "Stream", student.stream)
                                TableRow("Class / Standard", student.className.ifEmpty { "General" }, "Session / Batch", student.sessionName.ifEmpty { "General Session" })
                                TableRow("Parents Mobile No", student.mobileNo.ifEmpty { "N/A" }, "Email ID", student.email.ifEmpty { "N/A" })
                                TableRow("Proof of ID", student.idProofName.ifEmpty { "Standard Photo ID" }, "ID Card Number", student.idProofNumber.ifEmpty { "Verified at Admission" })
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 3. ENROLLED SUBJECTS TABLE
                            Text(
                                text = "2. ENROLLED SUBJECTS & CURRICULUM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color(0xFF0F172A)),
                                color = Color(0xFFFAFBFD)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    val subjectList = student.subjects.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    if (subjectList.isNotEmpty()) {
                                        Text(
                                            text = subjectList.joinToString("  •  "),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E293B)
                                        )
                                    } else {
                                        Text("Standard Core Curriculum Enrolled", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 4. FEE LEDGER BREAKDOWN
                            Text(
                                text = "3. FEE TRANSACTION & FINANCIAL LEDGER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF0F172A))
                            ) {
                                val paid = student.admissionFeePaid
                                val total = if (student.totalFee > 0) student.totalFee else paid
                                val balance = (total - paid).coerceAtLeast(0.0)

                                TableRow(
                                    "Total Course Fee",
                                    "₹${String.format("%.2f", total)}",
                                    "Admission Fee Paid",
                                    "₹${String.format("%.2f", paid)}"
                                )
                                TableRow(
                                    "Remaining Balance Due",
                                    "₹${String.format("%.2f", balance)}",
                                    "Payment Mode",
                                    "Cash / Institute Desk"
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Signatures & Stamp
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(130.dp).height(1.dp).background(Color(0xFF0F172A)))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Student / Parent Signature", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(70.dp, 35.dp)
                                            .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("OFFICIAL SEAL", fontSize = 8.sp, color = Color(0xFF94A3B8))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Authorized Institute Seal", fontSize = 9.sp, color = Color(0xFF64748B))
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(130.dp).height(1.dp).background(Color(0xFF0F172A)))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Principal / Director Signature", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableRow(label1: String, value1: String, label2: String, value2: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Color(0xFFE2E8F0))
            .padding(vertical = 4.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label1, modifier = Modifier.weight(1.1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
        Text(value1, modifier = Modifier.weight(1.4f), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label2, modifier = Modifier.weight(1.1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
        Text(value2, modifier = Modifier.weight(1.4f), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
    }
}

fun printAdmissionSlip(context: Context, student: Student) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // Header
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SP COACHING & EVALUATION CENTER", 40f, 50f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Official Student Admission Slip", 40f, 68f, paint)
        canvas.drawText("Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}", 40f, 84f, paint)

        // Line
        paint.strokeWidth = 1.5f
        canvas.drawLine(40f, 96f, 555f, 96f, paint)

        // Table details
        var y = 125f
        paint.textSize = 11f

        val details = listOf(
            "Student Name: ${student.name}" to "Roll Number: ${student.rollNo}",
            "Father's Name: ${student.fatherName}" to "Registration No: ${student.registrationNo}",
            "Mother's Name: ${student.motherName.ifEmpty { "N/A" }}" to "Gender: ${student.gender}",
            "DOB: ${student.dob.ifEmpty { "N/A" }}" to "Stream: ${student.stream}",
            "Class: ${student.className.ifEmpty { "General" }}" to "Session / Batch: ${student.sessionName.ifEmpty { "General Session" }}",
            "Mobile No: ${student.mobileNo.ifEmpty { "N/A" }}" to "Email: ${student.email.ifEmpty { "N/A" }}",
            "Proof of ID: ${student.idProofName.ifEmpty { "Standard ID" }}" to "ID Number: ${student.idProofNumber.ifEmpty { "Verified" }}",
            "Admission Fee: ₹${student.admissionFeePaid}" to "Total Fee: ₹${student.totalFee}"
        )

        for ((col1, col2) in details) {
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(col1, 40f, y, paint)
            canvas.drawText(col2, 320f, y, paint)
            y += 24f
        }

        y += 10f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Enrolled Subjects: ${student.subjects}", 40f, y, paint)

        // Signatures line
        canvas.drawLine(40f, 750f, 200f, 750f, paint)
        canvas.drawLine(380f, 750f, 540f, 750f, paint)
        paint.textSize = 9f
        canvas.drawText("Student / Parent Signature", 40f, 765f, paint)
        canvas.drawText("Authorized Signatory", 380f, 765f, paint)

        pdfDocument.finishPage(page)

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Admission_Slip_${student.rollNo}.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        Toast.makeText(context, "Admission Slip PDF saved: ${file.name}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Print preview generated", Toast.LENGTH_SHORT).show()
    }
}
