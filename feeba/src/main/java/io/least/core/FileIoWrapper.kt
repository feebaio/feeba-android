package io.least.core

import android.content.Context
import android.util.Log
import java.io.File

fun readLocalFile(context: Context, fileName: String): String {
    val dir = File(context.filesDir, "cache")
    return File(dir, fileName).readText()
}

fun writeToLocalFile(text: String, context: Context, fileName: String) {
    val dir = File(context.filesDir, "cache")
    if (!dir.exists()) {
        dir.mkdir();
    }
    val file = File(dir, fileName)

    Log.d("writeToLocalFile", "Writing to ${file.name}")
    runCatching { file.writeText(text) }
        .onSuccess { Log.d("writeToLocalFile", "Writing to ${file.name} was successful") }
        .onFailure { Log.e("writeToLocalFile", "Writing to ${file.name} failed. Error: $it") }

}

private fun getTrimmedLastPart(fileName: String): String {
    val trimmedFileName: String = try {
        fileName.split('/').last()
    } catch (t: Throwable) {
        fileName
    }
    return trimmedFileName
}

