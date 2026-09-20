with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

if "OpenCVLoader" not in text:
    text = text.replace("import android.util.Log", "import android.util.Log\nimport org.opencv.android.OpenCVLoader")
    
    init_code = """        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Unable to load OpenCV!")
        } else {
            Log.d("OpenCV", "OpenCV loaded Successfully!")
        }
        enableEdgeToEdge()"""
    text = text.replace("enableEdgeToEdge()", init_code)
    with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
        f.write(text)
    print("Added OpenCVLoader to MainActivity")
