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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Action Bar
                Surface(
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Student Admission Slip", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                Text("Official Registration & Fee Receipt • A4 Format", fontSize = 10.5.sp, color = Color(0xFF64748B))
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    generateAdmissionSlipPdf(context, student)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download / Print PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                            }
                        }
                    }
                }

                // Scrollable A4 Document View
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // A4 Document Simulation (Proportions ~ 1 : 1.414)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        shadowElevation = 3.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            // Institute Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "SP ACADEMIC & COACHING INSTITUTE",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Affiliated & Advanced Learning Center • Estd. 2018",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "Campus Road, Knowledge Hub • Phone: +91 98765 43210 • Email: info@spinstitute.edu",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                // Student Passport Photo
                                Box(
                                    modifier = Modifier
                                        .size(75.dp, 90.dp)
                                        .border(1.dp, Color(0xFF0F172A))
                                        .background(Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val photoBmp = remember(student.imagePath) {
                                        if (student.imagePath.isNotBlank()) {
                                            try { BitmapFactory.decodeFile(student.imagePath) } catch (e: Exception) { null }
                                        } else null
                                    }
                                    if (photoBmp != null) {
                                        Image(
                                            bitmap = photoBmp.asImageBitmap(),
                                            contentDescription = "Student Photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text("PASSPORT\nPHOTO", fontSize = 8.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFF0F172A), thickness = 2.dp)
                            Spacer(modifier = Modifier.height(6.dp))

                            // Slip Title & Meta Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF0F172A)) {
                                    Text(
                                        text = "OFFICIAL ADMISSION SLIP (ACADEMIC YEAR)",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                val admissionDateStr = remember(student.timestamp) {
                                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(student.timestamp))
                                }
                                Text("Date: $admissionDateStr", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // ==================== TABLE 1: STUDENT PROFILE ====================
                            Text("1. CANDIDATE PARTICULARS", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.height(4.dp))

                            TableGrid {
                                TableRowItem("Candidate Full Name", student.name, "Registration No.", student.registrationNo)
                                TableRowItem("Father's Name", student.fatherName, "Assigned Roll No.", student.rollNo, isHighlight = true)
                                TableRowItem("Mother's Name", student.motherName.ifBlank { "N/A" }, "Date of Birth (DOB)", student.dob.ifBlank { "N/A" })
                                TableRowItem("Gender", student.gender, "Stream / Department", student.stream)
                                TableRowItem("Class / Course", student.className.ifBlank { "Academic Batch" }, "Session / Batch", student.sessionName.ifBlank { "Regular Session" })
                                TableRowItem("Mobile Contact", student.mobileNo.ifBlank { "N/A" }, "Email Address", student.email.ifBlank { "N/A" })
                                TableRowItem("ID Proof Type", student.idProofName.ifBlank { "Official ID" }, "ID Number", student.idProofNumber.ifBlank { "Verified" })
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // ==================== TABLE 2: ENROLLED SUBJECTS ====================
                            Text("2. SUBJECTS ENROLLED FOR ADMISSION", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.height(4.dp))

                            val subjectList = remember(student.subjects) {
                                student.subjects.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFCBD5E1))
                            ) {
                                // Table Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF1F5F9))
                                        .padding(vertical = 5.dp, horizontal = 8.dp)
                                ) {
                                    Text("S.No", modifier = Modifier.width(40.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF334155))
                                    Text("Subject Title", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF334155))
                                    Text("Category / Stream", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF334155))
                                    Text("Status", modifier = Modifier.width(70.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF334155), textAlign = TextAlign.End)
                                }
                                HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 1.dp)

                                subjectList.forEachIndexed { index, subj ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (index % 2 == 0) Color.White else Color(0xFFF8FAFC))
                                            .padding(vertical = 5.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${index + 1}", modifier = Modifier.width(40.dp), fontSize = 10.sp, color = Color(0xFF475569))
                                        Text(subj, modifier = Modifier.weight(1.5f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                        Text(student.stream, modifier = Modifier.weight(1f), fontSize = 9.5.sp, color = Color(0xFF64748B))
                                        Text("ENROLLED", modifier = Modifier.width(70.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), textAlign = TextAlign.End)
                                    }
                                    if (index < subjectList.size - 1) {
                                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // ==================== TABLE 3: ADMISSION FEE LEDGER ====================
                            Text("3. ADMISSION FEE & PAYMENT LEDGER", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.height(4.dp))

                            val admissionPaid = student.admissionFeePaid
                            val totalCourseFee = if (student.totalFee > 0) student.totalFee else admissionPaid
                            val dueAmount = (totalCourseFee - admissionPaid).coerceAtLeast(0.0)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF1F5F9))
                                        .padding(vertical = 6.dp, horizontal = 8.dp)
                                ) {
                                    Text("Particulars", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF334155))
                                    Text("Total Course Fee", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF334155), textAlign = TextAlign.Center)
                                    Text("Admission Fee Paid", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF15803D), textAlign = TextAlign.Center)
                                    Text("Balance Due", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFFB91C1C), textAlign = TextAlign.End)
                                }
                                HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 1.dp)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 7.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Academic Year Tuition & Admission", modifier = Modifier.weight(1.5f), fontSize = 10.sp, color = Color(0xFF0F172A))
                                    Text("₹ ${String.format("%.2f", totalCourseFee)}", modifier = Modifier.weight(1f), fontSize = 10.sp, textAlign = TextAlign.Center, color = Color(0xFF475569))
                                    Text("₹ ${String.format("%.2f", admissionPaid)}", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFF15803D))
                                    Text("₹ ${String.format("%.2f", dueAmount)}", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = if (dueAmount > 0) Color(0xFFB91C1C) else Color(0xFF15803D))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Barcode & QR Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val barcodeBmp: Bitmap? = remember(student.rollNo) {
                                    CodeGenerator.generateBarcodeBitmap(student.rollNo, 260, 50)
                                }
                                if (barcodeBmp != null) {
                                    Image(
                                        bitmap = barcodeBmp.asImageBitmap(),
                                        contentDescription = "Barcode",
                                        modifier = Modifier.height(28.dp).width(160.dp),
                                        contentScale = ContentScale.FillBounds
                                    )
                                } else {
                                    Text("Roll: ${student.rollNo}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    text = "System Generated • SP SIS-ERP v2.4",
                                    fontSize = 8.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Signatures Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(90.dp).height(1.dp).background(Color(0xFF0F172A)))
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text("Candidate Signature", fontSize = 8.sp, color = Color(0xFF475569))
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(90.dp).height(1.dp).background(Color(0xFF0F172A)))
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text("Parent / Guardian", fontSize = 8.sp, color = Color(0xFF475569))
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(110.dp).height(1.dp).background(Color(0xFF0F172A)))
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text("Authorized Officer & Seal", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
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
fun TableGrid(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFCBD5E1))
    ) {
        content()
    }
}

@Composable
fun TableRowItem(label1: String, value1: String, label2: String, value2: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isHighlight) Color(0xFFFFFBEB) else Color.White),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Col 1 (Label)
        Box(
            modifier = Modifier
                .weight(1.1f)
                .background(Color(0xFFF8FAFC))
                .border(0.5.dp, Color(0xFFE2E8F0))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(label1, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
        }
        // Col 2 (Value)
        Box(
            modifier = Modifier
                .weight(1.4f)
                .border(0.5.dp, Color(0xFFE2E8F0))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(value1, fontSize = 9.5.sp, fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal, color = if (isHighlight) Color(0xFFB45309) else Color(0xFF0F172A))
        }
        // Col 3 (Label)
        Box(
            modifier = Modifier
                .weight(1.1f)
                .background(Color(0xFFF8FAFC))
                .border(0.5.dp, Color(0xFFE2E8F0))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(label2, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
        }
        // Col 4 (Value)
        Box(
            modifier = Modifier
                .weight(1.4f)
                .border(0.5.dp, Color(0xFFE2E8F0))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(value2, fontSize = 9.5.sp, fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal, color = if (isHighlight) Color(0xFFB45309) else Color(0xFF0F172A))
        }
    }
}

/**
 * Creates and exports a pristine A4 PDF Admission Slip for the student.
 */
fun generateAdmissionSlipPdf(context: Context, student: Student) {
    try {
        val pdfDocument = PdfDocument()

        // Standard A4: 595 x 842 points
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // Border around page
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = android.graphics.Color.BLACK
        canvas.drawRect(25f, 25f, 570f, 817f, paint)

        // Inner header border
        canvas.drawLine(25f, 105f, 570f, 105f, paint)

        paint.style = Paint.Style.FILL

        // Header Title
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 15f
        canvas.drawText("SP ACADEMIC & COACHING INSTITUTE", 40f, 55f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        canvas.drawText("Affiliated Higher Secondary & Advanced Coaching Center", 40f, 70f, paint)
        canvas.drawText("Phone: +91 98765 43210 • Email: info@spinstitute.edu", 40f, 85f, paint)

        // Title Ribbon
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("OFFICIAL STUDENT ADMISSION SLIP (A4 FORMAT)", 40f, 125f, paint)

        paint.textSize = 9f
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(student.timestamp))
        canvas.drawText("Date: $dateStr", 470f, 125f, paint)

        // Table grid for student profile
        var y = 145f
        val col1X = 40f
        val col2X = 170f
        val col3X = 310f
        val col4X = 440f

        val details = listOf(
            Pair("Student Full Name:", student.name) to Pair("Registration No.:", student.registrationNo),
            Pair("Father's Name:", student.fatherName) to Pair("Roll Number:", student.rollNo),
            Pair("Mother's Name:", student.motherName.ifBlank { "N/A" }) to Pair("Date of Birth:", student.dob.ifBlank { "N/A" }),
            Pair("Gender:", student.gender) to Pair("Class / Stream:", "${student.className} (${student.stream})"),
            Pair("Mobile Number:", student.mobileNo.ifBlank { "N/A" }) to Pair("Email ID:", student.email.ifBlank { "N/A" }),
            Pair("Session / Batch:", student.sessionName.ifBlank { "Regular" }) to Pair("ID Proof:", "${student.idProofName}: ${student.idProofNumber.ifBlank { "Verified" }}")
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.5f
        paint.color = android.graphics.Color.LTGRAY

        details.forEach { (left, right) ->
            canvas.drawRect(40f, y, 555f, y + 22f, paint)

            paint.style = Paint.Style.FILL
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = 9f
            paint.color = android.graphics.Color.DKGRAY
            canvas.drawText(left.first, col1X + 5f, y + 15f, paint)

            paint.typeface = Typeface.DEFAULT
            paint.color = android.graphics.Color.BLACK
            canvas.drawText(left.second, col2X + 5f, y + 15f, paint)

            paint.typeface = Typeface.DEFAULT_BOLD
            paint.color = android.graphics.Color.DKGRAY
            canvas.drawText(right.first, col3X + 5f, y + 15f, paint)

            paint.typeface = Typeface.DEFAULT
            paint.color = android.graphics.Color.BLACK
            canvas.drawText(right.second, col4X + 5f, y + 15f, paint)

            paint.style = Paint.Style.STROKE
            paint.color = android.graphics.Color.LTGRAY
            y += 22f
        }

        y += 20f

        // Enrolled Subjects Table
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 11f
        paint.color = android.graphics.Color.BLACK
        canvas.drawText("ENROLLED SUBJECTS", 40f, y, paint)
        y += 12f

        paint.style = Paint.Style.STROKE
        paint.color = android.graphics.Color.LTGRAY
        canvas.drawRect(40f, y, 555f, y + 20f, paint)

        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 9f
        canvas.drawText("S.No", 45f, y + 14f, paint)
        canvas.drawText("Subject Title", 90f, y + 14f, paint)
        canvas.drawText("Stream", 350f, y + 14f, paint)
        canvas.drawText("Status", 480f, y + 14f, paint)
        y += 20f

        val subjectList = student.subjects.split(",").map { it.trim() }.filter { it.isNotBlank() }
        paint.typeface = Typeface.DEFAULT
        subjectList.forEachIndexed { idx, subj ->
            paint.style = Paint.Style.STROKE
            paint.color = android.graphics.Color.LTGRAY
            canvas.drawRect(40f, y, 555f, y + 18f, paint)

            paint.style = Paint.Style.FILL
            paint.color = android.graphics.Color.BLACK
            canvas.drawText("${idx + 1}", 45f, y + 13f, paint)
            canvas.drawText(subj, 90f, y + 13f, paint)
            canvas.drawText(student.stream, 350f, y + 13f, paint)
            paint.color = android.graphics.Color.parseColor("#15803D")
            canvas.drawText("ENROLLED", 480f, y + 13f, paint)
            paint.color = android.graphics.Color.BLACK
            y += 18f
        }

        y += 25f

        // Fee Summary Table
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 11f
        paint.color = android.graphics.Color.BLACK
        canvas.drawText("ADMISSION FEE & FINANCIAL BREAKDOWN", 40f, y, paint)
        y += 12f

        val totalCourseFee = if (student.totalFee > 0) student.totalFee else student.admissionFeePaid
        val paid = student.admissionFeePaid
        val due = (totalCourseFee - paid).coerceAtLeast(0.0)

        paint.style = Paint.Style.STROKE
        paint.color = android.graphics.Color.LTGRAY
        canvas.drawRect(40f, y, 555f, y + 20f, paint)

        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 9f
        canvas.drawText("Total Course Fee", 60f, y + 14f, paint)
        canvas.drawText("Admission Fee Paid", 240f, y + 14f, paint)
        canvas.drawText("Balance Due", 440f, y + 14f, paint)
        y += 20f

        paint.style = Paint.Style.STROKE
        canvas.drawRect(40f, y, 555f, y + 22f, paint)

        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 10f
        canvas.drawText("₹ ${String.format("%.2f", totalCourseFee)}", 60f, y + 15f, paint)
        paint.color = android.graphics.Color.parseColor("#15803D")
        canvas.drawText("₹ ${String.format("%.2f", paid)}", 240f, y + 15f, paint)
        paint.color = if (due > 0) android.graphics.Color.parseColor("#B91C1C") else android.graphics.Color.parseColor("#15803D")
        canvas.drawText("₹ ${String.format("%.2f", due)}", 440f, y + 15f, paint)

        // Signatures at bottom
        val sigY = 770f
        paint.color = android.graphics.Color.BLACK
        canvas.drawLine(50f, sigY, 150f, sigY, paint)
        canvas.drawLine(240f, sigY, 340f, sigY, paint)
        canvas.drawLine(440f, sigY, 540f, sigY, paint)

        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Candidate Sign", 65f, sigY + 12f, paint)
        canvas.drawText("Parent Sign", 265f, sigY + 12f, paint)
        canvas.drawText("Registrar & Seal", 455f, sigY + 12f, paint)

        pdfDocument.finishPage(page)

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Admission_Slip_${student.rollNo}.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        Toast.makeText(context, "Admission Slip PDF Saved: ${file.name}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "PDF export error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
