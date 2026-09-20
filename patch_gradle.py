with open("app/build.gradle.kts", "r") as f:
    text = f.read()

if "opencv" not in text:
    text = text.replace("implementation(libs.retrofit)", 'implementation(libs.retrofit)\n  implementation("com.quickbirdstudios:opencv:4.5.3.0")')
    with open("app/build.gradle.kts", "w") as f:
        f.write(text)
    print("Added opencv")
else:
    print("OpenCV already there")
