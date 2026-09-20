import re

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    text = f.read()

replacement = """
            } else {
                val scannerOptions = GmsDocumentScannerOptions.Builder()
                    .setGalleryImportAllowed(true)
                    .setResultFormats(RESULT_FORMAT_JPEG)
                    .setScannerMode(SCANNER_MODE_FULL)
                    .setPageLimit(1)
                    .build()
                val scanner = GmsDocumentScanning.getClient(scannerOptions)
                
                val scannerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val resultData = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                        val uri = resultData?.pages?.firstOrNull()?.imageUri
                        if (uri != null) {
                            isProcessing = true
                            coroutineScope.launch(Dispatchers.IO) {
                                val stream = context.contentResolver.openInputStream(uri)
                                val bitmap = BitmapFactory.decodeStream(stream)
                                if (bitmap != null) {
                                    val res = OmrScanner.scan(bitmap, numQuestions, numOptions)
                                    withContext(Dispatchers.Main) {
                                        scanResult = res
                                        isProcessing = false
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                                        isProcessing = false
                                    }
                                }
                            }
                        }
                    } else {
                        // User cancelled
                        navController.popBackStack()
                    }
                }

                LaunchedEffect(Unit) {
                    scanner.getStartScanIntent((context as Activity))
                        .addOnSuccessListener { intentSender ->
                            scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Scanner failed to start", Toast.LENGTH_SHORT).show()
                        }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Ready to scan", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = {
                        scanner.getStartScanIntent((context as Activity))
                            .addOnSuccessListener { intentSender ->
                                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                            }
                    }) {
                        Text("Start Scanner")
                    }
                }
            }
"""

text = re.sub(
    r"\} else \{\s*LiveScannerScreen\([\s\S]*?\}\s*\)\s*\}",
    replacement.strip(),
    text
)

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "w") as f:
    f.write(text)

