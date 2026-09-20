import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    text = f.read()

scan_advanced_code = """
    fun scanAdvanced(bitmap: Bitmap, numQuestions: Int, numOptions: Int): ScanResult {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        
        // Find markers (fallback to corners if not found perfectly)
        val tl = findCorner(bitmap, isLeft = true, isTop = true)
        val tr = findCorner(bitmap, isLeft = false, isTop = true)
        val bl = findCorner(bitmap, isLeft = true, isTop = false)
        val br = findCorner(bitmap, isLeft = false, isTop = false)
        
        val a4W = 800f
        val a4H = 1131f
        
        val src = floatArrayOf(
            tl.first, tl.second,
            tr.first, tr.second,
            br.first, br.second,
            bl.first, bl.second
        )
        val dst = floatArrayOf(
            0f, 0f,
            a4W, 0f,
            a4W, a4H,
            0f, a4H
        )
        
        val matrix = android.graphics.Matrix()
        matrix.setPolyToPoly(src, 0, dst, 0, 4)
        
        val warpedBitmap = android.graphics.Bitmap.createBitmap(a4W.toInt(), a4H.toInt(), android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(warpedBitmap)
        canvas.concat(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        
        return scan(warpedBitmap, numQuestions, numOptions)
    }
}
"""

text = text.replace("    }\n}", "    }\n" + scan_advanced_code)

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(text)
