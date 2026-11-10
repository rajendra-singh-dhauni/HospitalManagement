package com.mobile360.healthcare.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.mobile360.healthcare.data.local.MedicationEntity
import com.mobile360.healthcare.data.local.PrescriptionView
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class RecordsSavedFile(val where: String, val uri: Uri?, val mime: String)

fun recordsDate(millis: Long, pattern: String = "dd MMM yyyy"): String =
    DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

fun recordsInitials(name: String): String =
    name.removePrefix("Dr. ").split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }

fun recordsSaveToDownloads(context: Context, fileName: String, mime: String, bytes: ByteArray): RecordsSavedFile {
    if (Build.VERSION.SDK_INT >= 29) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create the file")
        resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Could not write the file")
        val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
        resolver.update(uri, done, null, null)
        return RecordsSavedFile("Downloads/$fileName", uri, mime)
    }
    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
    dir.mkdirs()
    val file = File(dir, fileName)
    file.writeBytes(bytes)
    return RecordsSavedFile(file.absolutePath, null, mime)
}

fun recordsViewIntent(file: RecordsSavedFile): Intent? = file.uri?.let {
    Intent(Intent.ACTION_VIEW).setDataAndType(it, file.mime)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
}

fun recordsRxLines(rx: PrescriptionView, meds: List<MedicationEntity>): List<String> {
    val out = mutableListOf<String>()
    out += "CareConnect - Prescription"
    out += "Rx #${rx.rxNumber} (version ${rx.version})"
    out += "Issued: ${recordsDate(rx.issuedMillis)}"
    out += "Patient: ${rx.patientName}"
    out += "Doctor: ${rx.doctorName}, ${rx.doctorSpecialty}"
    out += "${rx.clinic}, ${rx.address}"
    out += ""
    out += "Medications"
    meds.forEach { m ->
        out += "- ${m.name} (${m.category}): ${m.dosage}, ${m.frequency}"
        out += "    ${m.supplyDays} days supply, ${m.refillsLeft} refills left" + if (m.note.isNotBlank()) ", ${m.note}" else ""
    }
    out += ""
    out += "Doctor's advice"
    rx.advice.chunked(80).forEach { out += it }
    return out
}

fun recordsRxText(rx: PrescriptionView, meds: List<MedicationEntity>): String =
    recordsRxLines(rx, meds).joinToString("\n")

fun recordsRxPdf(rx: PrescriptionView, meds: List<MedicationEntity>): ByteArray {
    val doc = PdfDocument()
    val paint = Paint().apply { textSize = 12f }
    val lines = recordsRxLines(rx, meds)
    val perPage = 52
    lines.chunked(perPage).ifEmpty { listOf(emptyList()) }.forEachIndexed { i, chunk ->
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, i + 1).create())
        var y = 56f
        chunk.forEach { line ->
            page.canvas.drawText(line, 40f, y, paint)
            y += 15f
        }
        doc.finishPage(page)
    }
    val out = ByteArrayOutputStream()
    doc.writeTo(out)
    doc.close()
    return out.toByteArray()
}
