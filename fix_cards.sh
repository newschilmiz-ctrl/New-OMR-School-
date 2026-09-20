perl -0777 -pi -e 's/RoundedCornerShape\(20\.dp\)/RoundedCornerShape(8.dp)/g' app/src/main/java/com/example/ui/*.kt
perl -0777 -pi -e 's/RoundedCornerShape\(16\.dp\)/RoundedCornerShape(8.dp)/g' app/src/main/java/com/example/ui/*.kt
perl -0777 -pi -e 's/RoundedCornerShape\(24\.dp\)/RoundedCornerShape(12.dp)/g' app/src/main/java/com/example/ui/*.kt
perl -0777 -pi -e 's/RoundedCornerShape\(32\.dp\)/RoundedCornerShape(16.dp)/g' app/src/main/java/com/example/ui/*.kt

perl -0777 -pi -e 's/elevation = CardDefaults\.cardElevation\(defaultElevation = 0\.dp\)/elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)/g' app/src/main/java/com/example/ui/*.kt
