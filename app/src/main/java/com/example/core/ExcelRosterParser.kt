package com.example.core

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.Normalizer
import java.util.Locale
import java.util.zip.ZipInputStream

data class ImportedStudentRow(
    val fullName: String,
    val massarNumber: String,
    val generatedMassar: Boolean = false
)

sealed class ExcelParseResult {
    data class Success(val students: List<ImportedStudentRow>) : ExcelParseResult()
    data class Error(val reason: ExcelParseErrorReason) : ExcelParseResult()
}

enum class ExcelParseErrorReason {
    BINARY_XLS_UNSUPPORTED,
    EMPTY_OR_NO_STUDENTS,
    READ_FAILED
}

object ExcelRosterParser {

    private const val MAX_FILE_BYTES = 15 * 1024 * 1024
    private const val MAX_XML_ENTRY_BYTES = 8 * 1024 * 1024
    private const val MAX_ZIP_ENTRIES = 250

    fun parseUri(context: Context, uri: Uri): ExcelParseResult {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                parseStream(stream)
            } ?: ExcelParseResult.Error(ExcelParseErrorReason.READ_FAILED)
        } catch (_: Exception) {
            ExcelParseResult.Error(ExcelParseErrorReason.READ_FAILED)
        }
    }

    fun parseStream(inputStream: InputStream): ExcelParseResult {
        return try {
            val bytes = readBoundedBytes(inputStream, MAX_FILE_BYTES)
                ?: return ExcelParseResult.Error(ExcelParseErrorReason.READ_FAILED)
            if (bytes.isEmpty()) {
                return ExcelParseResult.Error(ExcelParseErrorReason.EMPTY_OR_NO_STUDENTS)
            }

            // Detect legacy OLE2 binary .xls (D0 CF 11 E0)
            if (bytes.size >= 4 &&
                bytes[0] == 0xD0.toByte() &&
                bytes[1] == 0xCF.toByte() &&
                bytes[2] == 0x11.toByte() &&
                bytes[3] == 0xE0.toByte()
            ) {
                return ExcelParseResult.Error(ExcelParseErrorReason.BINARY_XLS_UNSUPPORTED)
            }

            val rows: List<List<String>> = when {
                // PKZip magic bytes -> .xlsx
                bytes.size >= 4 &&
                    bytes[0] == 0x50.toByte() &&
                    bytes[1] == 0x4B.toByte() &&
                    bytes[2] == 0x03.toByte() &&
                    bytes[3] == 0x04.toByte() -> parseXlsx(bytes)

                else -> {
                    val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
                    val trimmedLower = text.trimStart().lowercase(Locale.US)
                    when {
                        trimmedLower.contains("<workbook") || trimmedLower.contains("<ss:workbook") ->
                            parseSpreadsheetMl(text)
                        trimmedLower.contains("<table") || trimmedLower.contains("<html") ->
                            parseHtmlTable(text)
                        else -> parseDelimitedText(text)
                    }
                }
            }

            val students = extractStudentsFromGrid(rows)
            if (students.isEmpty()) {
                ExcelParseResult.Error(ExcelParseErrorReason.EMPTY_OR_NO_STUDENTS)
            } else {
                ExcelParseResult.Success(students)
            }
        } catch (_: Exception) {
            ExcelParseResult.Error(ExcelParseErrorReason.READ_FAILED)
        }
    }

    private fun parseXlsx(bytes: ByteArray): List<List<String>> {
        var sharedStringsXml: ByteArray? = null
        val sheetEntries = linkedMapOf<String, ByteArray>()

        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entryCount = 0
            var entry = zis.nextEntry
            while (entry != null && entryCount < MAX_ZIP_ENTRIES) {
                entryCount++
                val name = entry.name.lowercase(Locale.US)
                if (!entry.isDirectory) {
                    when {
                        name == "xl/sharedstrings.xml" -> {
                            sharedStringsXml = readBoundedBytes(zis, MAX_XML_ENTRY_BYTES)
                                ?: throw IllegalStateException("XLSX sharedStrings exceeds size limit")
                        }
                        name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml") -> {
                            sheetEntries[name] = readBoundedBytes(zis, MAX_XML_ENTRY_BYTES)
                                ?: throw IllegalStateException("XLSX worksheet exceeds size limit")
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val sharedStrings = sharedStringsXml?.let { parseSharedStrings(it) } ?: emptyList()
        val firstSheet = sheetEntries["xl/worksheets/sheet1.xml"]
            ?: sheetEntries.entries.minByOrNull { it.key }?.value
            ?: return emptyList()

        return parseWorksheetXml(firstSheet, sharedStrings)
    }

    private fun readBoundedBytes(input: InputStream, maxBytes: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            total += read
            if (total > maxBytes) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun parseSharedStrings(xmlBytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

        var inSi = false
        var inText = false
        val currentSi = StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tag = localTag(parser.name)
                    if (tag == "si") {
                        inSi = true
                        currentSi.setLength(0)
                    } else if (inSi && tag == "t") {
                        inText = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inSi && inText) {
                        currentSi.append(parser.text ?: "")
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = localTag(parser.name)
                    if (tag == "t") {
                        inText = false
                    } else if (tag == "si") {
                        strings.add(currentSi.toString().trim())
                        inSi = false
                    }
                }
            }
            eventType = parser.next()
        }
        return strings
    }

    private fun parseWorksheetXml(xmlBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

        var currentRow = mutableMapOf<Int, String>()
        var currentColIndex = -1
        var currentCellType = ""
        var inCell = false
        var inValue = false
        var inInlineText = false
        val cellBuffer = StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (localTag(parser.name)) {
                        "row" -> {
                            currentRow = mutableMapOf()
                            currentColIndex = -1
                        }
                        "c" -> {
                            inCell = true
                            val ref = parser.getAttributeValue(null, "r")
                            currentColIndex = if (!ref.isNullOrBlank()) {
                                columnLettersToIndex(ref)
                            } else {
                                currentColIndex + 1
                            }
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                            cellBuffer.setLength(0)
                        }
                        "v" -> if (inCell) inValue = true
                        "t" -> if (inCell && currentCellType == "inlineStr") inInlineText = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inValue || inInlineText) {
                        cellBuffer.append(parser.text ?: "")
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (localTag(parser.name)) {
                        "v" -> inValue = false
                        "t" -> inInlineText = false
                        "c" -> {
                            val raw = cellBuffer.toString().trim()
                            val value = if (currentCellType == "s") {
                                val idx = raw.toIntOrNull()
                                if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else ""
                            } else {
                                raw
                            }
                            if (currentColIndex >= 0 && value.isNotBlank()) {
                                currentRow[currentColIndex] = value
                            }
                            inCell = false
                        }
                        "row" -> {
                            if (currentRow.isNotEmpty()) {
                                val maxCol = currentRow.keys.maxOrNull() ?: 0
                                val dense = MutableList(maxCol + 1) { idx -> currentRow[idx].orEmpty() }
                                rows.add(dense)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }

    private fun parseSpreadsheetMl(xmlText: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlText.toByteArray(Charsets.UTF_8)), "UTF-8")

        var currentRow = mutableListOf<String>()
        var inRow = false
        var inData = false
        val buffer = StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (localTag(parser.name).lowercase(Locale.US)) {
                        "row" -> {
                            inRow = true
                            currentRow = mutableListOf()
                        }
                        "data" -> {
                            if (inRow) {
                                inData = true
                                buffer.setLength(0)
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inData) buffer.append(parser.text ?: "")
                }
                XmlPullParser.END_TAG -> {
                    when (localTag(parser.name).lowercase(Locale.US)) {
                        "data" -> {
                            if (inData) {
                                currentRow.add(buffer.toString().trim())
                                inData = false
                            }
                        }
                        "row" -> {
                            if (currentRow.any { it.isNotBlank() }) {
                                rows.add(currentRow)
                            }
                            inRow = false
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }

    private fun parseHtmlTable(html: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val trRegex = Regex("(?is)<tr[^>]*>(.*?)</tr>")
        val cellRegex = Regex("(?is)<t[dh][^>]*>(.*?)</t[dh]>")
        val tagStripRegex = Regex("(?is)<[^>]+>")
        for (trMatch in trRegex.findAll(html)) {
            val cells = cellRegex.findAll(trMatch.groupValues[1])
                .map { m ->
                    tagStripRegex.replace(m.groupValues[1], "")
                        .replace("&nbsp;", " ")
                        .replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .trim()
                }
                .toList()
            if (cells.any { it.isNotBlank() }) {
                rows.add(cells)
            }
        }
        return rows
    }

    private fun parseDelimitedText(text: String): List<List<String>> {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val sample = lines.first()
        val delimiter = when {
            sample.count { it == ';' } >= sample.count { it == ',' } && sample.contains(';') -> ';'
            sample.contains('\t') -> '\t'
            else -> ','
        }
        return lines.map { splitCsvLine(it, delimiter) }
    }

    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    current.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (ch == delimiter && !inQuotes) {
                result.add(current.toString().trim())
                current.setLength(0)
            } else {
                current.append(ch)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    private fun extractStudentsFromGrid(rows: List<List<String>>): List<ImportedStudentRow> {
        val nonEmptyRows = rows.filter { row -> row.any { it.isNotBlank() } }
        if (nonEmptyRows.isEmpty()) return emptyList()

        var headerRowIndex = -1
        var massarCol = -1
        var fullNameCol = -1
        var lastNameCol = -1
        var firstNameCol = -1

        val searchLimit = minOf(nonEmptyRows.size, 10)
        for (r in 0 until searchLimit) {
            val row = nonEmptyRows[r]
            var foundMassar = -1
            var foundFull = -1
            var foundLast = -1
            var foundFirst = -1

            row.forEachIndexed { idx, rawCell ->
                val norm = normalizeHeader(rawCell)
                if (norm.isBlank()) return@forEachIndexed
                when {
                    foundMassar == -1 && (
                        norm.contains("massar") ||
                            norm == "cne" ||
                            norm == "code" ||
                            norm.contains("identifiant") ||
                            norm.contains("مسار")
                        ) -> foundMassar = idx

                    foundFull == -1 && (
                        norm.contains("nomcomplet") ||
                            norm.contains("fullname") ||
                            norm.contains("nomprenom") ||
                            norm.contains("eleve") ||
                            norm.contains("etudiant") ||
                            norm.contains("student") ||
                            norm.contains("الاسمالكامل") ||
                            norm.contains("التلميذ")
                        ) -> foundFull = idx

                    foundFirst == -1 && (
                        norm == "prenom" ||
                            norm == "firstname" ||
                            norm.contains("الاسمالشخصي")
                        ) -> foundFirst = idx

                    foundLast == -1 && (
                        norm == "nom" ||
                            norm == "lastname" ||
                            norm.contains("الاسمالعايلي") ||
                            norm.contains("النسب")
                        ) -> foundLast = idx
                }
            }

            if (foundMassar != -1 || foundFull != -1 || (foundLast != -1 && foundFirst != -1)) {
                headerRowIndex = r
                massarCol = foundMassar
                fullNameCol = foundFull
                lastNameCol = foundLast
                firstNameCol = foundFirst
                break
            }
        }

        val dataRows = if (headerRowIndex >= 0) {
            nonEmptyRows.drop(headerRowIndex + 1)
        } else {
            nonEmptyRows
        }

        val seenMassar = linkedSetOf<String>()
        val results = mutableListOf<ImportedStudentRow>()
        var autoSeq = 1

        for (row in dataRows) {
            val rawName = when {
                fullNameCol in row.indices && row[fullNameCol].isNotBlank() -> row[fullNameCol].trim()
                firstNameCol in row.indices || lastNameCol in row.indices -> {
                    val first = row.getOrNull(firstNameCol).orEmpty().trim()
                    val last = row.getOrNull(lastNameCol).orEmpty().trim()
                    listOf(first, last).filter { it.isNotBlank() }.joinToString(" ")
                }
                headerRowIndex == -1 -> inferNameFromUnheadedRow(row)
                else -> row.firstOrNull { it.isNotBlank() && !looksLikeMassar(it) }.orEmpty().trim()
            }

            if (rawName.length < 2) continue

            val rawMassar = when {
                massarCol in row.indices && row[massarCol].isNotBlank() -> row[massarCol]
                headerRowIndex == -1 -> row.firstOrNull { looksLikeMassar(it) }.orEmpty()
                else -> ""
            }

            var cleanMassar = AuthSessionDraft.normalizeMassar(rawMassar)
            var generated = false
            if (cleanMassar.length < 4) {
                val slug = normalizeHeader(rawName).uppercase(Locale.US).take(4).ifBlank { "ELV" }
                cleanMassar = "M${slug}${autoSeq.toString().padStart(3, '0')}"
                autoSeq++
                generated = true
            }

            if (seenMassar.add(cleanMassar)) {
                results.add(
                    ImportedStudentRow(
                        fullName = rawName,
                        massarNumber = cleanMassar,
                        generatedMassar = generated
                    )
                )
            }
        }
        return results
    }

    private fun inferNameFromUnheadedRow(row: List<String>): String {
        val nonBlank = row.map { it.trim() }.filter { it.isNotBlank() }
        if (nonBlank.isEmpty()) return ""
        val nonMassar = nonBlank.filterNot { looksLikeMassar(it) }
        return if (nonMassar.size >= 2) {
            nonMassar.take(2).joinToString(" ")
        } else {
            nonMassar.firstOrNull() ?: ""
        }
    }

    private fun looksLikeMassar(value: String): Boolean {
        val clean = value.trim()
        return clean.matches(Regex("^[A-Za-z]\\d{6,12}$")) ||
            clean.matches(Regex("^[A-Za-z0-9_-]{7,16}$")) && clean.any { it.isDigit() }
    }

    private fun columnLettersToIndex(cellRef: String): Int {
        var idx = 0
        for (ch in cellRef) {
            if (ch.isLetter()) {
                idx = idx * 26 + (ch.uppercaseChar() - 'A' + 1)
            } else {
                break
            }
        }
        return (idx - 1).coerceAtLeast(0)
    }

    private fun localTag(name: String?): String {
        val n = name ?: return ""
        return n.substringAfter(':')
    }

    private fun normalizeHeader(raw: String): String {
        val decomposed = Normalizer.normalize(raw.trim().lowercase(Locale.US), Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[\\s_°'\"-]+"), "")
    }
}
