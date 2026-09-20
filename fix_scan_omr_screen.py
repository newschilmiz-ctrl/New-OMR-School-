import re

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    text = f.read()

# Replace ML Kit imports with LiveScanner usage logic
import_old = """
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
"""
text = text.replace(import_old.strip(), "")

logic_old = """
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
                                    val res = OmrScanner.scan(bitmap, numQuestions, numOptions, exam?.templateType ?: "Standard")
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
                    }
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Ready to scan", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = {
                        scanner.getStartScanIntent((context as Activity))
                            .addOnSuccessListener { intentSender ->
                                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Scanner not available", Toast.LENGTH_SHORT).show()
                            }
                    }) {
                        Text("Start Scanner")
                    }
                }
"""

logic_new = """
                var startLiveScanner by remember { mutableStateOf(false) }
                if (startLiveScanner) {
                    LiveScanner(
                        numQuestions = numQuestions,
                        numOptions = numOptions,
                        onScanSuccess = { res ->
                            scanResult = res
                            startLiveScanner = false
                        },
                        onCancel = {
                            startLiveScanner = false
                        }
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Ready to scan", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = {
                            startLiveScanner = true
                        }) {
                            Text("Open Live Scanner")
                        }
                    }
                }
"""
text = text.replace(logic_old.strip(), logic_new.strip())

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "w") as f:
    f.write(text)

