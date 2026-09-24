package com.example.ui.designer

import java.util.UUID

enum class ElementType(val displayName: String, val iconName: String) {
    HEADER_TITLE("Header Title", "Title"),
    SUBTITLE("Subtitle / Date", "Subtitles"),
    CUSTOM_TEXT("Custom Text", "TextFields"),
    QUESTION_BLOCK("Question Bubbles", "CheckCircle"),
    ROLL_NO_GRID("Roll No Matrix", "GridOn"),
    STUDENT_INFO_BOX("Student Details Box", "Badge"),
    INSTRUCTIONS_BOX("Instructions Box", "Info"),
    SIGNATURE_BOX("Signatures Box", "Draw"),
    BARCODE_QR("QR / Barcode", "QrCode"),
    DIVIDER_LINE("Divider Line", "HorizontalRule"),
    REGISTRATION_MARKERS("Corner Markers", "CropFree")
}

enum class TextAlignment {
    LEFT, CENTER, RIGHT
}

data class OmrElement(
    val id: String = UUID.randomUUID().toString(),
    val type: ElementType,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    // Text customization
    val text: String = "",
    val fontSize: Float = 18f,
    val isBold: Boolean = true,
    val alignment: TextAlignment = TextAlignment.CENTER,
    val textColorHex: Long = 0xFF000000,
    val hasBorder: Boolean = false,
    val borderWidth: Float = 1.5f,
    val backgroundColorHex: Long? = null,
    // Question Bubbles customization
    val numQuestions: Int = 30,
    val startQuestionNum: Int = 1,
    val numOptions: Int = 4, // 3 to 5
    val numColumns: Int = 2, // 1 to 4
    val bubbleRadius: Float = 11f,
    val bubbleColorHex: Long = 0xFF000000,
    val optionsFormat: String = "ABCD",
    // Roll number grid customization
    val digitsCount: Int = 6,
    val gridTitle: String = "ROLL NUMBER",
    // Barcode / QR customization
    val barcodeFormat: String = "QR",
    val barcodeContent: String = "OMR-EXAM-2026",
    // Signature block customization
    val studentSignLabel: String = "Candidate's Signature",
    val invigilatorSignLabel: String = "Invigilator's Signature",
    // Divider line customization
    val lineThickness: Float = 2f,
    val isDashed: Boolean = false,
    // Locking & layering
    val isLocked: Boolean = false,
    val zIndex: Int = 0
)

object OmrTemplatePresets {

    fun createStandard50(): List<OmrElement> {
        return listOf(
            // 4 Corner Registration Markers for Scanner alignment
            OmrElement(
                type = ElementType.REGISTRATION_MARKERS,
                x = 0f,
                y = 0f,
                width = 1000f,
                height = 1414f,
                isLocked = true,
                zIndex = 0
            ),
            // Header Title
            OmrElement(
                type = ElementType.HEADER_TITLE,
                x = 100f,
                y = 50f,
                width = 800f,
                height = 50f,
                text = "CENTRAL BOARD OF EXAMINATION",
                fontSize = 28f,
                isBold = true,
                alignment = TextAlignment.CENTER,
                textColorHex = 0xFF0F172A
            ),
            // Subtitle
            OmrElement(
                type = ElementType.SUBTITLE,
                x = 100f,
                y = 105f,
                width = 800f,
                height = 35f,
                text = "ANNUAL EXAMINATION 2026 • OMR ANSWER SHEET",
                fontSize = 16f,
                isBold = true,
                alignment = TextAlignment.CENTER,
                textColorHex = 0xFF334155
            ),
            // Horizontal separator
            OmrElement(
                type = ElementType.DIVIDER_LINE,
                x = 80f,
                y = 148f,
                width = 840f,
                height = 2f,
                lineThickness = 2f,
                textColorHex = 0xFF0F172A
            ),
            // Student Info Box (Left side)
            OmrElement(
                type = ElementType.STUDENT_INFO_BOX,
                x = 80f,
                y = 165f,
                width = 540f,
                height = 180f,
                text = "Student Info",
                fontSize = 14f,
                hasBorder = true
            ),
            // Roll No Bubble Grid (Right side)
            OmrElement(
                type = ElementType.ROLL_NO_GRID,
                x = 640f,
                y = 165f,
                width = 280f,
                height = 340f,
                digitsCount = 6,
                gridTitle = "ROLL NUMBER",
                bubbleRadius = 9f
            ),
            // Instructions Box
            OmrElement(
                type = ElementType.INSTRUCTIONS_BOX,
                x = 80f,
                y = 360f,
                width = 540f,
                height = 145f,
                text = "INSTRUCTIONS:\n• Darken only ONE bubble per question with Black/Blue ballpoint pen.\n• Do not make stray marks or fold the sheet.\n• Correct: (●) | Incorrect: (✕) (✓) (◐)",
                fontSize = 12f,
                hasBorder = true
            ),
            // Main Question Bubbles Block (50 questions, 2 columns of 25)
            OmrElement(
                type = ElementType.QUESTION_BLOCK,
                x = 80f,
                y = 525f,
                width = 840f,
                height = 700f,
                numQuestions = 50,
                startQuestionNum = 1,
                numOptions = 4,
                numColumns = 2,
                bubbleRadius = 11f,
                optionsFormat = "ABCD",
                hasBorder = true
            ),
            // Signatures at bottom
            OmrElement(
                type = ElementType.SIGNATURE_BOX,
                x = 80f,
                y = 1250f,
                width = 840f,
                height = 90f,
                studentSignLabel = "Candidate's Signature",
                invigilatorSignLabel = "Invigilator's Signature",
                hasBorder = true
            )
        )
    }

    fun createNeet100(): List<OmrElement> {
        return listOf(
            OmrElement(
                type = ElementType.REGISTRATION_MARKERS,
                x = 0f,
                y = 0f,
                width = 1000f,
                height = 1414f,
                isLocked = true
            ),
            OmrElement(
                type = ElementType.HEADER_TITLE,
                x = 100f,
                y = 45f,
                width = 800f,
                height = 45f,
                text = "ANNUAL EXAMINATION ASSESSMENT",
                fontSize = 26f,
                isBold = true,
                alignment = TextAlignment.CENTER
            ),
            OmrElement(
                type = ElementType.SUBTITLE,
                x = 100f,
                y = 95f,
                width = 800f,
                height = 30f,
                text = "100 QUESTIONS • 4 OPTIONS (A B C D)",
                fontSize = 15f,
                isBold = true,
                alignment = TextAlignment.CENTER
            ),
            OmrElement(
                type = ElementType.DIVIDER_LINE,
                x = 80f,
                y = 135f,
                width = 840f,
                height = 2f
            ),
            OmrElement(
                type = ElementType.STUDENT_INFO_BOX,
                x = 80f,
                y = 145f,
                width = 500f,
                height = 170f,
                hasBorder = true
            ),
            OmrElement(
                type = ElementType.ROLL_NO_GRID,
                x = 600f,
                y = 145f,
                width = 320f,
                height = 330f,
                digitsCount = 7,
                gridTitle = "ROLL NUMBER",
                bubbleRadius = 9f
            ),
            OmrElement(
                type = ElementType.BARCODE_QR,
                x = 80f,
                y = 330f,
                width = 240f,
                height = 145f,
                barcodeFormat = "QR",
                barcodeContent = "EXAM-100-STANDARD"
            ),
            OmrElement(
                type = ElementType.INSTRUCTIONS_BOX,
                x = 335f,
                y = 330f,
                width = 245f,
                height = 145f,
                text = "• Darken bubble completely.\n• Rough work strictly on last page.\n• Multiple marks = 0 marks.",
                fontSize = 11f,
                hasBorder = true
            ),
            // 100 Questions in 4 columns of 25
            OmrElement(
                type = ElementType.QUESTION_BLOCK,
                x = 80f,
                y = 495f,
                width = 840f,
                height = 740f,
                numQuestions = 100,
                startQuestionNum = 1,
                numOptions = 4,
                numColumns = 4,
                bubbleRadius = 9f,
                optionsFormat = "ABCD",
                hasBorder = true
            ),
            OmrElement(
                type = ElementType.SIGNATURE_BOX,
                x = 80f,
                y = 1255f,
                width = 840f,
                height = 85f,
                hasBorder = true
            )
        )
    }

    fun createQuick20(): List<OmrElement> {
        return listOf(
            OmrElement(
                type = ElementType.REGISTRATION_MARKERS,
                x = 0f,
                y = 0f,
                width = 1000f,
                height = 1414f,
                isLocked = true
            ),
            OmrElement(
                type = ElementType.HEADER_TITLE,
                x = 100f,
                y = 60f,
                width = 800f,
                height = 55f,
                text = "CLASS UNIT TEST / WEEKLY QUIZ",
                fontSize = 30f,
                isBold = true,
                alignment = TextAlignment.CENTER
            ),
            OmrElement(
                type = ElementType.SUBTITLE,
                x = 100f,
                y = 125f,
                width = 800f,
                height = 35f,
                text = "Subject: Science / Maths • Max Marks: 20",
                fontSize = 18f,
                alignment = TextAlignment.CENTER
            ),
            OmrElement(
                type = ElementType.DIVIDER_LINE,
                x = 80f,
                y = 175f,
                width = 840f,
                height = 2f
            ),
            OmrElement(
                type = ElementType.STUDENT_INFO_BOX,
                x = 100f,
                y = 200f,
                width = 800f,
                height = 190f,
                hasBorder = true
            ),
            OmrElement(
                type = ElementType.QUESTION_BLOCK,
                x = 100f,
                y = 420f,
                width = 800f,
                height = 720f,
                numQuestions = 20,
                startQuestionNum = 1,
                numOptions = 4,
                numColumns = 1,
                bubbleRadius = 14f,
                optionsFormat = "ABCD",
                hasBorder = true
            ),
            OmrElement(
                type = ElementType.SIGNATURE_BOX,
                x = 100f,
                y = 1180f,
                width = 800f,
                height = 100f,
                hasBorder = true
            )
        )
    }
}
