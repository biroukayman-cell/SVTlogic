package com.example.core

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.RosterStudent
import com.example.data.model.SvtClass
import java.io.File
import java.io.FileOutputStream

object StudentCredentialsPdfExporter {

    fun exportAndSharePdf(
        context: Context,
        svtClass: SvtClass,
        students: List<RosterStudent>
    ): Result<File> {
        return try {
            require(students.isNotEmpty()) { "No students to export" }

            val document = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 24f
            val cols = 2
            val rows = 3
            val cardsPerPage = cols * rows
            val gap = 14f
            val headerHeight = 46f

            val cardWidth = (pageWidth - margin * 2 - gap * (cols - 1)) / cols
            val cardHeight = (pageHeight - margin * 2 - headerHeight - gap * (rows - 1)) / rows

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(18, 92, 62)
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = 10f
            }
            val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(18, 92, 62)
                style = Paint.Style.STROKE
                strokeWidth = 1.4f
            }
            val cardHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(228, 244, 236)
                style = Paint.Style.FILL
            }
            val cardHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 76, 51)
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(80, 90, 86)
                textSize = 9.5f
            }
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(12, 58, 40)
                textSize = 11.5f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 100, 100)
                textSize = 8.5f
            }

            val totalPages = (students.size + cardsPerPage - 1) / cardsPerPage
            val appName = context.getString(R.string.app_name)
            val classLabel = context.getString(R.string.pdf_card_class, svtClass.name, svtClass.level)
            val massarLabel = context.getString(R.string.pdf_card_massar)
            val passLabel = context.getString(R.string.pdf_card_password)
            val codeLabel = context.getString(R.string.pdf_card_class_code, svtClass.qrToken)
            val footerHint = context.getString(R.string.pdf_card_instruction)

            students.chunked(cardsPerPage).forEachIndexed { pageIndex, pageStudents ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                canvas.drawText(
                    "$appName — ${svtClass.name}",
                    margin,
                    margin + 14f,
                    titlePaint
                )
                canvas.drawText(
                    "${svtClass.level} • ${svtClass.academicYear} • Page ${pageIndex + 1}/$totalPages",
                    margin,
                    margin + 30f,
                    subHeaderPaint
                )

                pageStudents.forEachIndexed { idx, student ->
                    val col = idx % cols
                    val row = idx / cols
                    val left = margin + col * (cardWidth + gap)
                    val top = margin + headerHeight + row * (cardHeight + gap)
                    val right = left + cardWidth
                    val bottom = top + cardHeight

                    val cardRect = RectF(left, top, right, bottom)
                    val headerRect = RectF(left, top, right, top + 28f)
                    canvas.drawRoundRect(headerRect, 8f, 8f, cardHeaderBgPaint)
                    canvas.drawRoundRect(cardRect, 8f, 8f, cardBorderPaint)

                    canvas.drawText(classLabel.take(36), left + 10f, top + 18f, cardHeaderPaint)

                    val textLeft = left + 10f
                    var y = top + 46f
                    canvas.drawText(student.fullName.take(28), textLeft, y, namePaint)
                    y += 18f

                    canvas.drawText(massarLabel, textLeft, y, labelPaint)
                    y += 14f
                    canvas.drawText(student.massarNumber, textLeft, y, valuePaint)
                    y += 18f

                    canvas.drawText(passLabel, textLeft, y, labelPaint)
                    y += 14f
                    canvas.drawText(student.provisionalPassword, textLeft, y, valuePaint)
                    y += 16f

                    canvas.drawText(codeLabel, textLeft, y, notePaint)

                    val qrSize = 86
                    val payload = student.qrPayload.ifBlank {
                        "BIOTERRE:${svtClass.qrToken}|${student.massarNumber}"
                    }
                    val qrBitmap = QrCodeUtils.generateQrBitmap(payload, 256)
                    val qrLeft = (right - qrSize - 10f).toInt()
                    val qrTop = (top + 40f).toInt()
                    val destRect = Rect(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize)
                    canvas.drawBitmap(qrBitmap, null, destRect, null)
                    qrBitmap.recycle()

                    canvas.drawText(footerHint.take(46), textLeft, bottom - 12f, notePaint)
                }

                document.finishPage(page)
            }

            val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
            val safeName = svtClass.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(32)
            val pdfFile = File(exportsDir, "BioTerre_${safeName}_Acces_Eleves.pdf")
            FileOutputStream(pdfFile).use { out ->
                document.writeTo(out)
            }
            document.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$appName — ${svtClass.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(
                shareIntent,
                context.getString(R.string.pdf_export_chooser_title)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

            Result.success(pdfFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
