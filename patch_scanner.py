import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    content = f.read()

target1 = """    fun scan(bitmap: Bitmap, numQuestions: Int, numOptions: Int): ScanResult {"""
replacement1 = """    fun scan(bitmap: Bitmap, numQuestions: Int, numOptions: Int, answerKey: List<Int>? = null): ScanResult {"""
content = content.replace(target1, replacement1)

target2 = """            for (opt in 0 until numOptions) {
                val vx = qStartX + opt * ansSpacingX
                val vy = qStartY
                val actual = mapVirtualToActual(vx, vy)
                val darkness = sampleDarkness(bitmap, matrix, vx, vy, actual.first, actual.second, ansBubbleRadius)
                
                canvas.drawCircle(actual.first, actual.second, 12f, paintRed)
                
                if (darkness > maxDarkness) {
                    secondMaxDarkness = maxDarkness
                    maxDarkness = darkness
                    bestOpt = opt
                } else if (darkness > secondMaxDarkness) {
                    secondMaxDarkness = darkness
                }
            }

            // A valid mark should be significantly darker than the empty bubbles and mostly filled
            if (maxDarkness > 0.3f && (maxDarkness - secondMaxDarkness) > 0.12f && bestOpt != -1) {
                answers.add(bestOpt)
                val vx = qStartX + bestOpt * ansSpacingX
                val vy = qStartY
                val actual = mapVirtualToActual(vx, vy)
                canvas.drawCircle(actual.first, actual.second, 12f, paintGreen)
            } else {
                answers.add(-1) // Blank or invalid (double filled)
            }"""

replacement2 = """            val optionCoords = mutableListOf<Pair<Float, Float>>()
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

content = content.replace(target2, replacement2)

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(content)
print("Updated OmrScanner.kt")
