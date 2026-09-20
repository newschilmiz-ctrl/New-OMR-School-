import re

with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "r") as f:
    text = f.read()

old_ui = """
                                        Text(
                                            text = "Q${index + 1}: ${if (ans == -1) "-" else ('A' + ans)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(4.dp),
                                            color = if (ans == -1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                        )
"""

new_ui = """
                                        val ansText = when (ans) {
                                            -1 -> "BLANK"
                                            -2 -> "MULTIPLE"
                                            else -> ('A' + ans).toString()
                                        }
                                        val ansColor = when (ans) {
                                            -1 -> MaterialTheme.colorScheme.error
                                            -2 -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                        Text(
                                            text = "Q${index + 1}: $ansText",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(4.dp),
                                            color = ansColor
                                        )
"""

text = text.replace(old_ui.strip(), new_ui.strip())
with open("app/src/main/java/com/example/ui/ScanOmrScreen.kt", "w") as f:
    f.write(text)

