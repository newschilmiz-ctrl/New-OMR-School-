package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.FeeRecord
import com.example.data.Student
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeTrackerScreen(
    navController: NavController,
    viewModel: OmrViewModel
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsStateWithLifecycle()
    val fees by viewModel.fees.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PAID, DUE
    var showAddFeeDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf<FeeRecord?>(null) }

    // Aggregate Fee Metrics
    val totalCollected = remember(fees) { fees.sumOf { it.amountPaid } }
    val totalRecordsCount = remember(fees) { fees.size }

    // Map each student's paid vs total fee
    val feesByRoll = remember(fees) {
        fees.groupBy { it.studentRollNo.lowercase().trim() }
    }

    val studentFeeMap = remember(students, feesByRoll) {
        students.associate { st ->
            val stFees = feesByRoll[st.rollNo.lowercase().trim()] ?: emptyList()
            val paid = stFees.sumOf { it.amountPaid }
            val totalAssigned = stFees.maxOfOrNull { it.totalFee } ?: 10000.0
            val balance = (totalAssigned - paid).coerceAtLeast(0.0)
            st.rollNo to Triple(paid, totalAssigned, balance)
        }
    }

    val filteredStudents = remember(students, searchQuery, selectedFilter, studentFeeMap) {
        students.filter { st ->
            val matchesQuery = st.name.contains(searchQuery, ignoreCase = true) ||
                    st.rollNo.contains(searchQuery, ignoreCase = true) ||
                    st.mobileNo.contains(searchQuery, ignoreCase = true)
            val feeInfo = studentFeeMap[st.rollNo] ?: Triple(0.0, 10000.0, 10000.0)
            val balance = feeInfo.third

            val matchesFilter = when (selectedFilter) {
                "DUE" -> balance > 0
                "CLEARED" -> balance == 0.0 && feeInfo.first > 0
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Fee Management",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Installments, Dues & Receipts",
                            fontSize = 10.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF059669),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showAddFeeDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Collect Fee", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFAFBFD)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. STATS OVERVIEW CARDS
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Collected", fontSize = 10.5.sp, color = Color(0xFF065F46), fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "₹${String.format("%,.0f", totalCollected)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = "$totalRecordsCount payments logged",
                                fontSize = 9.sp,
                                color = Color(0xFF059669)
                            )
                        }
                    }

                    val totalDue = remember(students, studentFeeMap) {
                        students.sumOf { studentFeeMap[it.rollNo]?.third ?: 0.0 }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF1F2),
                        border = BorderStroke(1.dp, Color(0xFFFECDD3))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Outstanding", fontSize = 10.5.sp, color = Color(0xFF9F1239), fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Outlined.PendingActions, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "₹${String.format("%,.0f", totalDue)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF9F1239)
                            )
                            Text(
                                text = "Across active students",
                                fontSize = 9.sp,
                                color = Color(0xFFBE123C)
                            )
                        }
                    }
                }
            }

            // 2. SEARCH & FILTER CHIPS
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by student name, roll number...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = Color(0xFF0F172A)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ALL" to "All Students", "DUE" to "Has Due Balance", "CLEARED" to "Fully Paid").forEach { (filterKey, label) ->
                            val isSelected = selectedFilter == filterKey
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color(0xFF0F172A) else Color.White,
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedFilter = filterKey }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 3. STUDENT FEE CARDS
            if (filteredStudents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No matching student fee records found.", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                }
            } else {
                items(filteredStudents, key = { it.rollNo }) { student ->
                    val (paid, total, balance) = studentFeeMap[student.rollNo] ?: Triple(0.0, 10000.0, 10000.0)
                    val studentReceipts = feesByRoll[student.rollNo.lowercase().trim()] ?: emptyList()

                    StudentFeeCard(
                        student = student,
                        paid = paid,
                        total = total,
                        balance = balance,
                        receipts = studentReceipts,
                        onCollectMore = {
                            showAddFeeDialog = true
                        },
                        onViewReceipt = { receipt ->
                            showReceiptDialog = receipt
                        }
                    )
                }
            }
        }
    }

    // DIALOG: COLLECT FEE
    if (showAddFeeDialog) {
        AddFeeDialog(
            students = students,
            onDismiss = { showAddFeeDialog = false },
            onSave = { feeRecord ->
                viewModel.addFeeRecord(feeRecord) {
                    Toast.makeText(context, "Fee receipt ${feeRecord.receiptNo} generated!", Toast.LENGTH_SHORT).show()
                    showAddFeeDialog = false
                    showReceiptDialog = feeRecord
                }
            }
        )
    }

    // DIALOG: RECEIPT PREVIEW
    if (showReceiptDialog != null) {
        FeeReceiptDialog(
            fee = showReceiptDialog!!,
            student = students.find { it.rollNo.equals(showReceiptDialog!!.studentRollNo, ignoreCase = true) },
            onDismiss = { showReceiptDialog = null }
        )
    }
}

@Composable
fun StudentFeeCard(
    student: Student,
    paid: Double,
    total: Double,
    balance: Double,
    receipts: List<FeeRecord>,
    onCollectMore: () -> Unit,
    onViewReceipt: (FeeRecord) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = student.name.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = student.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Roll #${student.rollNo}",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                            if (student.sessionName.isNotBlank()) {
                                Text(
                                    text = "• ${student.sessionName}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                // Balance Status Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (balance <= 0) Color(0xFFDCFCE7) else Color(0xFFFFE4E6)
                ) {
                    Text(
                        text = if (balance <= 0) "CLEARED" else "DUE ₹${String.format("%.0f", balance)}",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (balance <= 0) Color(0xFF15803D) else Color(0xFFBE123C),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar (Paid vs Total)
            val progress = (paid / total.coerceAtLeast(1.0)).coerceIn(0.0, 1.0).toFloat()
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (balance <= 0) Color(0xFF10B981) else Color(0xFF3B82F6),
                trackColor = Color(0xFFF1F5F9)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Paid: ₹${String.format("%.0f", paid)} of ₹${String.format("%.0f", total)}",
                    fontSize = 10.sp,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${(progress * 100).toInt()}% Paid",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            // Expandable Receipts Section
            if (receipts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 1.dp)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${receipts.size} Receipts Logged",
                        fontSize = 10.sp,
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (expanded) {
                    Column(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        receipts.forEach { rec ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onViewReceipt(rec) },
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${rec.receiptNo} • ${rec.monthOrInstallment}",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "${rec.paymentDate} via ${rec.paymentMode}",
                                            fontSize = 8.5.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "+₹${String.format("%.0f", rec.amountPaid)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFeeDialog(
    students: List<Student>,
    onDismiss: () -> Unit,
    onSave: (FeeRecord) -> Unit
) {
    var selectedStudentRoll by remember { mutableStateOf(students.firstOrNull()?.rollNo ?: "") }
    var amountPaidStr by remember { mutableStateOf("3000") }
    var totalFeeStr by remember { mutableStateOf("12000") }
    var installmentName by remember { mutableStateOf("Monthly Installment") }
    var paymentMode by remember { mutableStateOf("Cash") }
    var remarks by remember { mutableStateOf("") }

    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val selectedStudent = students.find { it.rollNo == selectedStudentRoll }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF059669))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Collect Student Fee", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Select Student", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(students) { st ->
                        val isSelected = st.rollNo == selectedStudentRoll
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedStudentRoll = st.rollNo },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = "${st.name} (#${st.rollNo})",
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amountPaidStr,
                        onValueChange = { amountPaidStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Amount Paid (₹)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = totalFeeStr,
                        onValueChange = { totalFeeStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Total Fee (₹)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = installmentName,
                    onValueChange = { installmentName = it },
                    label = { Text("Installment / Description", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Payment Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Cash", "UPI", "Cheque", "Card").forEach { mode ->
                        val isSel = paymentMode == mode
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSel) Color(0xFF059669) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { paymentMode = mode }
                        ) {
                            Text(
                                text = mode,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks (Optional)", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paid = amountPaidStr.toDoubleOrNull() ?: 0.0
                    val total = totalFeeStr.toDoubleOrNull() ?: 10000.0
                    if (selectedStudent != null && paid > 0) {
                        val randomNum = (1000..9999).random()
                        val recNo = "REC-${selectedStudent.rollNo}-$randomNum"
                        val record = FeeRecord(
                            studentRollNo = selectedStudent.rollNo,
                            studentName = selectedStudent.name,
                            amountPaid = paid,
                            totalFee = total,
                            paymentDate = todayDate,
                            paymentMode = paymentMode,
                            receiptNo = recNo,
                            monthOrInstallment = installmentName,
                            remarks = remarks
                        )
                        onSave(record)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                Text("Generate Receipt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FeeReceiptDialog(
    fee: FeeRecord,
    student: Student?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF059669))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Payment Receipt", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFDCFCE7)
                ) {
                    Text(
                        text = "PAID",
                        color = Color(0xFF166534),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "SHREE PRABHA COACHING",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Official Fee Deposit Voucher",
                        fontSize = 9.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    ReceiptRow("Receipt No:", fee.receiptNo)
                    ReceiptRow("Date:", fee.paymentDate)
                    ReceiptRow("Student Name:", fee.studentName)
                    ReceiptRow("Roll Number:", "#${fee.studentRollNo}")
                    if (student != null) {
                        ReceiptRow("Batch / Class:", "${student.sessionName} (${student.className})")
                    }
                    ReceiptRow("Payment Mode:", fee.paymentMode)
                    ReceiptRow("Installment:", fee.monthOrInstallment)

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Amount Deposited:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text(
                            "₹${String.format("%,.2f", fee.amountPaid)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF059669)
                        )
                    }

                    if (fee.remarks.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Note: ${fee.remarks}",
                            fontSize = 9.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Done")
            }
        }
    )
}

@Composable
fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 10.sp, color = Color(0xFF64748B))
        Text(text = value, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
    }
}
