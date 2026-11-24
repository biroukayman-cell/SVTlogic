package com.example.core

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

import java.io.ByteArrayOutputStream

data class ExtractedWordDocument(
    val paragraphs: List<String>
)

object DocxTextExtractor {

    private const val MAX_ARCHIVE_BYTES = 15 * 1024 * 1024
    private const val MAX_XML_ENTRY_BYTES = 8 * 1024 * 1024
    private const val MAX_ZIP_ENTRIES = 250

    fun extractFromFile(file: File): ExtractedWordDocument? {
        if (!file.exists() || file.length() <= 0L || file.length() > MAX_ARCHIVE_BYTES) return null
        return runCatching {
            FileInputStream(file).use { fis ->
                extractFromStream(fis)
            }
        }.getOrNull()
    }

    fun extractFromStream(input: InputStream): ExtractedWordDocument? {
        val bytes = readBoundedBytes(input, MAX_ARCHIVE_BYTES) ?: return null
        if (bytes.isEmpty()) return null

        val documentXml = extractEntryFromZip(bytes, "word/document.xml") ?: return null
        val paragraphs = parseDocumentXml(documentXml)
        return ExtractedWordDocument(paragraphs = paragraphs)
    }

    private fun extractEntryFromZip(zipBytes: ByteArray, entryName: String): ByteArray? {
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entriesCount = 0
            var entry = zis.nextEntry
            while (entry != null && entriesCount < MAX_ZIP_ENTRIES) {
                entriesCount++
                if (entry.name.equals(entryName, ignoreCase = true)) {
                    return readBoundedBytes(zis, MAX_XML_ENTRY_BYTES)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return null
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

    private fun parseDocumentXml(xmlBytes: ByteArray): List<String> {
        val factory = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = false
        }
        val parser = factory.newPullParser().apply {
            setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
        }

        val paragraphs = mutableListOf<String>()
        val currentParagraph = StringBuilder()
        var inParagraph = false
        var inText = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name?.substringAfter(':')
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (tagName) {
                        "p" -> {
                            inParagraph = true
                            currentParagraph.clear()
                        }
                        "t", "delText" -> inText = true
                        "tab" -> if (inParagraph) currentParagraph.append('\t')
                        "br", "cr" -> if (inParagraph) currentParagraph.append('\n')
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inParagraph && inText) {
                        currentParagraph.append(parser.text.orEmpty())
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (tagName) {
                        "t", "delText" -> inText = false
                        "p" -> {
                            val line = currentParagraph.toString().trim()
                            if (line.isNotEmpty()) {
                                paragraphs.add(line)
                            }
                            inParagraph = false
                            currentParagraph.clear()
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return paragraphs
    }
}
