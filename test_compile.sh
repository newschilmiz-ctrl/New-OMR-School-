gradle compileDebugKotlin > test_output.txt 2>&1
cat test_output.txt | grep -C 3 "e: "
