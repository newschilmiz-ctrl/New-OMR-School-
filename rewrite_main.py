with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

text = text.replace("import org.opencv.android.OpenCVLoader", "")
text = text.replace("""        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Unable to load OpenCV!")
        } else {
            Log.d("OpenCV", "OpenCV loaded Successfully!")
        }""", "")
        
with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)
