package com.example.core

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.R
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AiPdfSection(
    val title: String,
    val bodyLines: List<String>,
    val accentColorRgb: Triple<Int, Int, Int>
)

data class AiPdfDownloadResult(
    val fileName: String,
    val uri: Uri,
    val savedToDownloads: Boolean
)

object AiAnalysisPdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_X = 36f
    private const val MARGIN_TOP = 36f
    private const val MARGIN_BOTTOM = 42f

    fun buildSuggestedFileName(prefix: String, subjectName: String): String {
        val cleanSubject = subjectName
            .trim()
            .replace(Regex("[^a-zA-Z0-9_-]+"), "_")
            .trim('_')
            .take(36)
            .ifBlank { "SVT" }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return "${prefix}_${cleanSubject}_$stamp.pdf"
    }

    fun buildExternalAnalysisSections(
        context: Context,
        metricsTitle: String,
        metricsLines: List<String>,
        pastedAiAnalysis: String,
        promptText: String
    ): List<AiPdfSection> = buildList {
        if (metricsLines.isNotEmpty()) {
            add(
                AiPdfSection(
                    title = metricsTitle,
                    bodyLines = metricsLines,
                    accentColorRgb = Triple(18, 92, 62)
                )
            )
        }
        val cleanAnalysis = pastedAiAnalysis.trim()
        if (cleanAnalysis.isNotBlank()) {
            val paragraphs = cleanAnalysis
                .split(Regex("\\n+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            add(
                AiPdfSection(
                    title = context.getString(R.string.teacher_ai_pdf_external_analysis_section),
                    bodyLines = paragraphs,
                    accentColorRgb = Triple(22, 128, 61)
                )
            )
        } else if (promptText.isNotBlank()) {
            val promptLines = promptText
                .split(Regex("\\n+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            add(
                AiPdfSection(
                    title = context.getString(R.string.teacher_ai_pdf_prompt_section),
                    bodyLines = promptLines,
                    accentColorRgb = Triple(180, 108, 12)
                )
            )
        }
    }

    fun exportToDownloadsAndGetUri(
        context: Context,
        fileName: String,
        documentTitle: String,
        subtitle: String,
        sections: List<AiPdfSection>
    ): Result<AiPdfDownloadResult> {
        return try {
            require(sections.isNotEmpty()) { "No analysis content to export" }
            val appName = context.getString(R.string.app_name)
            val generatedAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        "${Environment.DIRECTORY_DOWNLOADS}/BioTerre_SVT"
                    )
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val itemUri = resolver.insert(collection, values)
                if (itemUri != null) {
                    try {
                        resolver.openOutputStream(itemUri)?.use { out ->
                            writePdfToStream(
                                outputStream = out,
                                appName = appName,
                                documentTitle = documentTitle,
                                subtitle = subtitle,
                                generatedAt = generatedAt,
                                sections = sections
                            )
                        } ?: throw IllegalStateException("Cannot open MediaStore output stream")

                        val completeValues = ContentValues().apply {
                            put(MediaStore.Downloads.IS_PENDING, 0)
                        }
                        resolver.update(itemUri, completeValues, null, null)
                        return Result.success(
                            AiPdfDownloadResult(
                                fileName = fileName,
                                uri = itemUri,
                                savedToDownloads = true
                            )
                        )
                    } catch (e: Exception) {
                        runCatching { resolver.delete(itemUri, null, null) }
                        throw e
                    }
                }
            }

            // Fallback for Android 7-9 or OEMs where MediaStore.Downloads returned null
            val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
            val pdfFile = File(exportsDir, fileName)
            FileOutputStream(pdfFile).use { out ->
                writePdfToStream(
                    outputStream = out,
                    appName = appName,
                    documentTitle = documentTitle,
                    subtitle = subtitle,
                    generatedAt = generatedAt,
                    sections = sections
                )
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            Result.success(
                AiPdfDownloadResult(
                    fileName = fileName,
                    uri = uri,
                    savedToDownloads = false
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun writePdfToUri(
        context: Context,
        targetUri: Uri,
        documentTitle: String,
        subtitle: String,
        sections: List<AiPdfSection>
    ): Result<Unit> {
        return try {
            require(sections.isNotEmpty()) { "No analysis content to export" }
            val appName = context.getString(R.string.app_name)
            val generatedAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                writePdfToStream(
                    outputStream = out,
                    appName = appName,
                    documentTitle = documentTitle,
                    subtitle = subtitle,
                    generatedAt = generatedAt,
                    sections = sections
                )
            } ?: throw IllegalStateException("Unable to open target PDF stream")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openOrSharePdf(context: Context, result: AiPdfDownloadResult, title: String) {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(result.uri, "application/pdf")
            clipData = ClipData.newRawUri(title, result.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val canView = viewIntent.resolveActivity(context.packageManager) != null
        if (canView) {
            val launched = runCatching {
                context.startActivity(viewIntent)
            }.isSuccess
            if (launched) return
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, result.uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            clipData = ClipData.newRawUri(title, result.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(
            shareIntent,
            context.getString(R.string.teacher_ai_pdf_share_chooser)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(chooser) }
    }

    internal fun writePdfToStream(
        outputStream: OutputStream,
        appName: String,
        documentTitle: String,
        subtitle: String,
        generatedAt: String,
        sections: List<AiPdfSection>
    ) {
        val document = PdfDocument()
        val contentWidth = (PAGE_WIDTH - MARGIN_X * 2).toInt()
        val maxContentBottom = PAGE_HEIGHT - MARGIN_BOTTOM

        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(228, 244, 236)
            style = Paint.Style.FILL
        }
        val headerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(18, 92, 62)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(18, 92, 62)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val docTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 35, 28)
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(75, 85, 82)
            textSize = 10f
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 34, 32)
            textSize = 10.5f
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(110, 115, 112)
            textSize = 8.5f
        }

        var pageNumber = 1
        var currentPage = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        )
        var canvas = currentPage.canvas
        canvas.drawColor(Color.WHITE)

        // Draw first-page header banner
        val titleLayout = buildTextLayout(documentTitle, docTitlePaint, contentWidth - 24)
        val subtitleText = listOfNotNull(
            subtitle.takeIf { it.isNotBlank() },
            generatedAt
        ).joinToString(" • ")
        val subtitleLayout = buildTextLayout(subtitleText, subtitlePaint, contentWidth - 24)
        val headerHeight = 28f + titleLayout.height + subtitleLayout.height + 16f
        val headerRect = RectF(MARGIN_X, MARGIN_TOP, PAGE_WIDTH - MARGIN_X, MARGIN_TOP + headerHeight)
        canvas.drawRoundRect(headerRect, 10f, 10f, headerBgPaint)
        canvas.drawRoundRect(headerRect, 10f, 10f, headerBorderPaint)

        canvas.drawText(appName.uppercase(Locale.getDefault()), MARGIN_X + 12f, MARGIN_TOP + 18f, brandPaint)
        canvas.save()
        canvas.translate(MARGIN_X + 12f, MARGIN_TOP + 26f)
        titleLayout.draw(canvas)
        canvas.translate(0f, titleLayout.height + 4f)
        subtitleLayout.draw(canvas)
        canvas.restore()

        var cursorY = MARGIN_TOP + headerHeight + 18f

        fun startNextPage() {
            drawPageFooter(canvas, appName, pageNumber, footerPaint)
            document.finishPage(currentPage)
            pageNumber += 1
            currentPage = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            canvas = currentPage.canvas
            canvas.drawColor(Color.WHITE)
            cursorY = MARGIN_TOP
        }

        for (section in sections) {
            val (r, g, b) = section.accentColorRgb
            val sectionTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(r, g, b)
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val accentBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(r, g, b)
                style = Paint.Style.FILL
            }
            val sectionBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(18, r, g, b)
                style = Paint.Style.FILL
            }

            val sectionTitleLayout = buildTextLayout(section.title, sectionTitlePaint, contentWidth - 20)
            if (cursorY + sectionTitleLayout.height + 28f > maxContentBottom) {
                startNextPage()
            }

            // Section heading strip
            val stripHeight = sectionTitleLayout.height + 10f
            val stripRect = RectF(MARGIN_X, cursorY, PAGE_WIDTH - MARGIN_X, cursorY + stripHeight)
            canvas.drawRoundRect(stripRect, 6f, 6f, sectionBgPaint)
            canvas.drawRect(MARGIN_X, cursorY + 2f, MARGIN_X + 4f, cursorY + stripHeight - 2f, accentBarPaint)
            canvas.save()
            canvas.translate(MARGIN_X + 12f, cursorY + 5f)
            sectionTitleLayout.draw(canvas)
            canvas.restore()
            cursorY += stripHeight + 8f

            for (paragraph in section.bodyLines) {
                if (paragraph.isBlank()) continue
                val paraLayout = buildTextLayout(paragraph, bodyPaint, contentWidth - 16)
                if (cursorY + paraLayout.height + 6f > maxContentBottom) {
                    startNextPage()
                }
                canvas.save()
                canvas.translate(MARGIN_X + 8f, cursorY)
                paraLayout.draw(canvas)
                canvas.restore()
                cursorY += paraLayout.height + 6f
            }

            cursorY += 10f
        }

        drawPageFooter(canvas, appName, pageNumber, footerPaint)
        document.finishPage(currentPage)

        try {
            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    private fun drawPageFooter(
        canvas: android.graphics.Canvas,
        appName: String,
        pageNumber: Int,
        paint: Paint
    ) {
        val y = PAGE_HEIGHT - 18f
        canvas.drawText("$appName — Page $pageNumber", MARGIN_X, y, paint)
    }

    private fun buildTextLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        val safeWidth = width.coerceAtLeast(100)
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, safeWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1.15f)
            .setIncludePad(false)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .build()
    }
}
