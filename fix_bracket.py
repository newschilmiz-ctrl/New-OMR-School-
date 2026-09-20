import re

with open("app/src/main/java/com/example/ui/StudentsScreen.kt", "r") as f:
    content = f.read()

parts = content.split("@Composable\nfun SkeletonStudentCard")
if len(parts) == 2:
    lines = parts[0].rstrip().split('\n')
    lines.append('}')
    parts[0] = '\n'.join(lines) + '\n\n'

    with open("app/src/main/java/com/example/ui/StudentsScreen.kt", "w") as f:
        f.write(parts[0] + "@Composable\nfun SkeletonStudentCard" + parts[1])
    print("Fixed!")
