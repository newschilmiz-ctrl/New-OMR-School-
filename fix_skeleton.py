with open("app/src/main/java/com/example/ui/SkeletonEffect.kt", "r") as f:
    lines = f.readlines()

if lines[0].startswith("import androidx.compose.runtime.getValue"):
    lines.pop(0)

pkg_idx = 0
for i, line in enumerate(lines):
    if line.startswith("package "):
        pkg_idx = i
        break

lines.insert(pkg_idx + 1, "import androidx.compose.runtime.getValue\n")

with open("app/src/main/java/com/example/ui/SkeletonEffect.kt", "w") as f:
    f.writelines(lines)
