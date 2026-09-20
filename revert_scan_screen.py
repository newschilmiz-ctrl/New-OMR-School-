import re

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    content = f.read()

target = """                            val answerKeyList = key?.answers ?: emptyList()
                            OmrScanner.scan(softwareBitmap, numQuestions, numOptions, answerKeyList)"""
replacement = """                            val scanResultObj = OmrScanner.scan(softwareBitmap, numQuestions, numOptions)
                            // We return it for now. Evaluation will happen in ResultView or later if we refactor.
                            scanResultObj"""

content = content.replace(target, replacement)
with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "w") as f:
    f.write(content)
print("Reverted ScanOmrScreen.kt")
