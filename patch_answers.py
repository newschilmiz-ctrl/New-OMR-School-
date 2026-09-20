import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    text = f.read()

old_logic = """
            val studentAns = if (maxDarkness > fillThreshold && (maxDarkness - secondMaxDarkness) > marginThreshold && bestOpt != -1) bestOpt else -1
            
            if (studentAns != -1) {
                val cx = qStartX + studentAns * ansSpacingX
                val cy = qStartY
                Imgproc.circle(warpedAnnotated, Point(cx, cy), ansBubbleRadius.toInt(), colorRed, -1)
            }
"""

new_logic = """
            var studentAns = -1 // NOT_ATTEMPTED
            
            if (maxDarkness > fillThreshold) {
                if (secondMaxDarkness > fillThreshold || (maxDarkness - secondMaxDarkness) < marginThreshold) {
                    studentAns = -2 // MULTIPLE_MARKED
                } else {
                    studentAns = bestOpt
                }
            }
            
            if (studentAns >= 0) {
                val cx = qStartX + studentAns * ansSpacingX
                val cy = qStartY
                Imgproc.circle(warpedAnnotated, Point(cx, cy), ansBubbleRadius.toInt(), colorRed, -1)
            } else if (studentAns == -2) {
                // Draw yellow circle for multiple marked
                val cx = qStartX + bestOpt * ansSpacingX
                val cy = qStartY
                Imgproc.circle(warpedAnnotated, Point(cx, cy), ansBubbleRadius.toInt(), Scalar(255.0, 255.0, 0.0, 255.0), 2)
            }
"""

text = text.replace(old_logic.strip(), new_logic.strip())

old_set_logic = """
        val paperSet = if (maxSetDarkness > fillThreshold && (maxSetDarkness - secondMaxSetDarkness) > marginThreshold && bestSetRow != -1) {
            val cx = setStartX
            val cy = setStartY + bestSetRow * setSpacingY
            Imgproc.circle(warpedAnnotated, Point(cx, cy), bubbleRadius.toInt(), colorGreen, -1)
            setSets[bestSetRow] 
        } else {
            "?" // MULTIPLE_MARKED or NOT_ATTEMPTED
        }
"""

new_set_logic = """
        val paperSet = if (maxSetDarkness > fillThreshold) {
            if (secondMaxSetDarkness > fillThreshold || (maxSetDarkness - secondMaxSetDarkness) < marginThreshold) {
                "MULTIPLE"
            } else {
                val cx = setStartX
                val cy = setStartY + bestSetRow * setSpacingY
                Imgproc.circle(warpedAnnotated, Point(cx, cy), bubbleRadius.toInt(), colorGreen, -1)
                setSets[bestSetRow]
            }
        } else {
            "BLANK"
        }
"""

text = text.replace(old_set_logic.strip(), new_set_logic.strip())

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(text)

