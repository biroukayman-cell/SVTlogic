package com.example.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
class DocxTextExtractorTest {

    @Test
    fun extractFromStream_readsParagraphsFromDocxZip() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:p><w:r><w:t>Chapitre 1 : La mitose</w:t></w:r></w:p>
                <w:p><w:r><w:t>Phase 1</w:t><w:br/><w:t>Prophase</w:t></w:r></w:p>
              </w:body>
            </w:document>
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write(xml)
            zos.closeEntry()
        }

        val extracted = DocxTextExtractor.extractFromStream(ByteArrayInputStream(baos.toByteArray()))
        assertNotNull(extracted)
        assertEquals(
            listOf("Chapitre 1 : La mitose", "Phase 1\nProphase"),
            extracted?.paragraphs
        )
    }

    @Test
    fun extractFromStream_returnsNullWhenNotADocxArchive() {
        val extracted = DocxTextExtractor.extractFromStream(
            ByteArrayInputStream("legacy-binary-doc".toByteArray())
        )
        assertNull(extracted)
    }
}
