package com.mobile360.healthcare.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.mobile360.healthcare.data.local.MedicationEntity
import com.mobile360.healthcare.data.local.PrescriptionView
import java.io.ByteArrayOutputStream
import java.io.File

// Builds the prescription PDF locally and saves it to Downloads via MediaStore.Downloads.
fun rxDetailSavePdf(context: Context, rx: PrescriptionView, meds: List<MedicationEntity>): RecordsSavedFile {
    val doc = PdfDocument()
    val paint = Paint().apply { textSize = 12f }
    val lines = recordsRxLines(rx, meds)
    lines.chunked(52).ifEmpty { listOf(emptyList()) }.forEachIndexed { i, chunk ->
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, i + 1).create())
        var y = 56f
        chunk.forEach { line -> page.canvas.drawText(line, 40f, y, paint); y += 15f }
        doc.finishPage(page)
    }
    val out = ByteArrayOutputStream()
    doc.writeTo(out)
    doc.close()
    val bytes = out.toByteArray()
    val name = "prescription_${rx.rxNumber}.pdf"
    val mime = "application/pdf"
    if (Build.VERSION.SDK_INT >= 29) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("Could not create the file")
        resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Could not write the file")
        resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        return RecordsSavedFile("Downloads/$name", uri, mime)
    }
    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
    dir.mkdirs()
    val file = File(dir, name)
    file.writeBytes(bytes)
    return RecordsSavedFile(file.absolutePath, null, mime)
}
