import re

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    content = f.read()

target = """    var correct = 0
    var wrong = 0
    var empty = 0
    val statuses = mutableListOf<Int>() // 1 = correct, 0 = wrong, -1 = empty
    for (i in 0 until key!!.numQuestions) {
        val studentAns = if (i < result.answers.size) result.answers[i] else -1
        val correctAns = if (i < correctAnswersList.size) correctAnswersList[i] else -1
        if (studentAns == -1) {
            empty++
            statuses.add(-1)
        } else if (studentAns == correctAns) {
            correct++
            statuses.add(1)
        } else {
            wrong++
            statuses.add(0)
        }
    }"""

replacement = """    var correct = 0
    var wrong = 0
    var empty = 0
    val statuses = mutableListOf<Int>() // 1 = correct, 0 = wrong, -1 = empty
    
    val annotatedBitmap = result.annotatedBitmap
    val canvas = android.graphics.Canvas(annotatedBitmap)
    val paintGreen = android.graphics.Paint().apply { color = android.graphics.Color.GREEN; style = android.graphics.Paint.Style.STROKE; strokeWidth = 4f }
    val paintRed = android.graphics.Paint().apply { color = android.graphics.Color.RED; style = android.graphics.Paint.Style.STROKE; strokeWidth = 4f }
    
    for (i in 0 until key!!.numQuestions) {
        val studentAns = if (i < result.answers.size) result.answers[i] else -1
        val correctAns = if (i < correctAnswersList.size) correctAnswersList[i] else -1
        
        if (i < result.optionCoords.size) {
            val coords = result.optionCoords[i]
            if (studentAns == correctAns && studentAns != -1) {
                if (studentAns < coords.size) {
                    canvas.drawCircle(coords[studentAns].first, coords[studentAns].second, 12f, paintGreen)
                }
            } else {
                if (studentAns != -1 && studentAns < coords.size) {
                    val cx = coords[studentAns].first
                    val cy = coords[studentAns].second
                    canvas.drawCircle(cx, cy, 12f, paintRed)
                    canvas.drawLine(cx - 8f, cy - 8f, cx + 8f, cy + 8f, paintRed)
                    canvas.drawLine(cx + 8f, cy - 8f, cx - 8f, cy + 8f, paintRed)
                }
                if (correctAns != -1 && correctAns < coords.size) {
                    canvas.drawCircle(coords[correctAns].first, coords[correctAns].second, 12f, paintGreen)
                }
            }
        }
        
        if (studentAns == -1) {
            empty++
            statuses.add(-1)
        } else if (studentAns == correctAns) {
            correct++
            statuses.add(1)
        } else {
            wrong++
            statuses.add(0)
        }
    }"""

content = content.replace(target, replacement)
with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "w") as f:
    f.write(content)
print("Updated ResultView evaluation logic")
