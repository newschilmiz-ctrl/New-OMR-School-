import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    content = f.read()

# Change ScanResult
content = content.replace("data class ScanResult(val studentId: String, val paperSet: String, val answers: List<Int>, val annotatedBitmap: Bitmap)",
                          "data class ScanResult(val studentId: String, val paperSet: String, val answers: List<Int>, val annotatedBitmap: Bitmap, val optionCoords: List<List<Pair<Float, Float>>> = emptyList())")

# Revert the scan function signature
content = content.replace("fun scan(bitmap: Bitmap, numQuestions: Int, numOptions: Int, answerKey: List<Int>? = null): ScanResult {",
                          "fun scan(bitmap: Bitmap, numQuestions: Int, numOptions: Int): ScanResult {")

# Remove the inline evaluation logic and collect optionCoords instead
target_loop = """            val optionCoords = mutableListOf<Pair<Float, Float>>()
            for (opt in 0 until numOptions) {
                val vx = qStartX + opt * ansSpacingX
                val vy = qStartY
                val actual = mapVirtualToActual(vx, vy)
                optionCoords.add(actual)
                val darkness = sampleDarkness(bitmap, matrix, vx, vy, actual.first, actual.second, ansBubbleRadius)
                
                if (darkness > maxDarkness) {
                    secondMaxDarkness = maxDarkness
                    maxDarkness = darkness
                    bestOpt = opt
                } else if (darkness > secondMaxDarkness) {
                    secondMaxDarkness = darkness
                }
            }

            // A valid mark should be significantly darker than the empty bubbles and mostly filled
            val studentAns = if (maxDarkness > 0.3f && (maxDarkness - secondMaxDarkness) > 0.12f && bestOpt != -1) bestOpt else -1
            answers.add(studentAns)
            
            if (answerKey != null && q < answerKey.size) {
                val correctAns = answerKey[q]
                if (studentAns == correctAns) {
                    // Draw Green Circle on studentAns
                    if (studentAns != -1 && studentAns < optionCoords.size) {
                        canvas.drawCircle(optionCoords[studentAns].first, optionCoords[studentAns].second, 12f, paintGreen)
                    }
                } else {
                    // Draw Red Crossed Circle on studentAns
                    if (studentAns != -1 && studentAns < optionCoords.size) {
                        val cx = optionCoords[studentAns].first
                        val cy = optionCoords[studentAns].second
                        canvas.drawCircle(cx, cy, 12f, paintRed)
                        // Draw Cross inside the red circle
                        canvas.drawLine(cx - 8f, cy - 8f, cx + 8f, cy + 8f, paintRed)
                        canvas.drawLine(cx + 8f, cy - 8f, cx - 8f, cy + 8f, paintRed)
                    }
                    // Draw Green Circle on correctAns
                    if (correctAns != -1 && correctAns < optionCoords.size) {
                        canvas.drawCircle(optionCoords[correctAns].first, optionCoords[correctAns].second, 12f, paintGreen)
                    }
                }
            } else {
                // If no answer key, just draw what was detected
                if (studentAns != -1 && studentAns < optionCoords.size) {
                    canvas.drawCircle(optionCoords[studentAns].first, optionCoords[studentAns].second, 12f, paintGreen)
                }
            }"""

replacement_loop = """            val currentOptionCoords = mutableListOf<Pair<Float, Float>>()
            for (opt in 0 until numOptions) {
                val vx = qStartX + opt * ansSpacingX
                val vy = qStartY
                val actual = mapVirtualToActual(vx, vy)
                currentOptionCoords.add(actual)
                val darkness = sampleDarkness(bitmap, matrix, vx, vy, actual.first, actual.second, ansBubbleRadius)
                
                if (darkness > maxDarkness) {
                    secondMaxDarkness = maxDarkness
                    maxDarkness = darkness
                    bestOpt = opt
                } else if (darkness > secondMaxDarkness) {
                    secondMaxDarkness = darkness
                }
            }
            allOptionCoords.add(currentOptionCoords)

            // A valid mark should be significantly darker than the empty bubbles and mostly filled
            val studentAns = if (maxDarkness > 0.3f && (maxDarkness - secondMaxDarkness) > 0.12f && bestOpt != -1) bestOpt else -1
            answers.add(studentAns)"""

content = content.replace(target_loop, replacement_loop)

# Add `val allOptionCoords = mutableListOf<List<Pair<Float, Float>>>()` before the `for (q in 0 until numQuestions)`
content = content.replace("val answers = mutableListOf<Int>()\n        for (q in 0 until numQuestions) {",
                          "val answers = mutableListOf<Int>()\n        val allOptionCoords = mutableListOf<List<Pair<Float, Float>>>()\n        for (q in 0 until numQuestions) {")

content = content.replace("return ScanResult(studentId, paperSet, answers, annotatedBitmap)",
                          "return ScanResult(studentId, paperSet, answers, annotatedBitmap, allOptionCoords)")

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(content)
print("Updated OmrScanner.kt")
