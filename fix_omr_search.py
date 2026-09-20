import re

with open("app/src/main/java/com/example/util/OmrScanner.kt", "r") as f:
    content = f.read()

# Replace the search bounds in findCenterOfMass calls
old_search_l = """            val actualL = findCenterOfMass(
                bitmap, 
                Math.max(0, roughLx - searchRadius), Math.max(0, roughLy - searchRadius), 
                Math.min(bitmap.width, roughLx + searchRadius), Math.min(bitmap.height, roughLy + searchRadius)
            )"""

new_search_l = """            // Restrict Y search radius to prevent snapping to adjacent marks
            val searchRadiusY = Math.min(searchRadius, (bitmap.height * 0.015f).toInt())
            val actualL = findCenterOfMass(
                bitmap, 
                Math.max(0, roughLx - searchRadius), Math.max(0, roughLy - searchRadiusY), 
                Math.min(bitmap.width, roughLx + searchRadius), Math.min(bitmap.height, roughLy + searchRadiusY)
            )"""

content = content.replace(old_search_l, new_search_l)

old_search_r = """            val actualR = findCenterOfMass(
                bitmap, 
                Math.max(0, roughRx - searchRadius), Math.max(0, roughRy - searchRadius), 
                Math.min(bitmap.width, roughRx + searchRadius), Math.min(bitmap.height, roughRy + searchRadius)
            )"""

new_search_r = """            val actualR = findCenterOfMass(
                bitmap, 
                Math.max(0, roughRx - searchRadius), Math.max(0, roughRy - searchRadiusY), 
                Math.min(bitmap.width, roughRx + searchRadius), Math.min(bitmap.height, roughRy + searchRadiusY)
            )"""

content = content.replace(old_search_r, new_search_r)

with open("app/src/main/java/com/example/util/OmrScanner.kt", "w") as f:
    f.write(content)
