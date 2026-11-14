package com.example.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExcelRosterParserTest {

    @Test
    fun parseStream_xlsxWithHeadersAndSharedStrings_returnsStudents() {
        val xlsxBytes = buildMinimalXlsx(
            sharedStrings = listOf(
                "Nom complet",
                "Code Massar",
                "Salma El Amrani",
                "K135792468",
                "Youssef Benali",
                "d987654321"
            ),
            rows = listOf(
                listOf("0", "1"),
                listOf("2", "3"),
                listOf("4", "5")
            )
        )

        val result = ExcelRosterParser.parseStream(ByteArrayInputStream(xlsxBytes))
        assertTrue("Expected Success, got $result", result is ExcelParseResult.Success)
        val students = (result as ExcelParseResult.Success).students
        assertEquals(2, students.size)
        assertEquals("Salma El Amrani", students[0].fullName)
        assertEquals("K135792468", students[0].massarNumber)
        assertFalse(students[0].generatedMassar)
        assertEquals("Youssef Benali", students[1].fullName)
        assertEquals("D987654321", students[1].massarNumber)
    }

    @Test
    fun parseStream_semicolonCsvWithoutMassar_generatesNormalizedMassarIds() {
        val csv = """
            Nom complet
            Amina Tazi
            Karim Idrissi
        """.trimIndent()

        val result = ExcelRosterParser.parseStream(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))
        assertTrue(result is ExcelParseResult.Success)
        val students = (result as ExcelParseResult.Success).students
        assertEquals(2, students.size)
        assertEquals("Amina Tazi", students[0].fullName)
        assertTrue(students[0].generatedMassar)
        assertTrue(students[0].massarNumber.startsWith("M"))
        assertEquals("Karim Idrissi", students[1].fullName)
        assertTrue(students[1].generatedMassar)
    }

    @Test
    fun parseStream_semicolonCsvWithMassarAndSplitNames_parsesCorrectly() {
        val csv = """
            Massar;Nom;Prénom
            R123456789;Berrada;Nizar
            J555666777;Chraibi;Hajar
        """.trimIndent()

        val result = ExcelRosterParser.parseStream(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))
        assertTrue(result is ExcelParseResult.Success)
        val students = (result as ExcelParseResult.Success).students
        assertEquals(2, students.size)
        assertEquals("Nizar Berrada", students[0].fullName)
        assertEquals("R123456789", students[0].massarNumber)
        assertEquals("Hajar Chraibi", students[1].fullName)
        assertEquals("J555666777", students[1].massarNumber)
    }

    @Test
    fun parseStream_htmlTableXls_parsesStudents() {
        val html = """
            <html><body><table>
              <tr><th>Nom complet</th><th>Massar</th></tr>
              <tr><td>Laila Mansouri</td><td>G444555666</td></tr>
            </table></body></html>
        """.trimIndent()

        val result = ExcelRosterParser.parseStream(ByteArrayInputStream(html.toByteArray(Charsets.UTF_8)))
        assertTrue(result is ExcelParseResult.Success)
        val students = (result as ExcelParseResult.Success).students
        assertEquals(1, students.size)
        assertEquals("Laila Mansouri", students[0].fullName)
        assertEquals("G444555666", students[0].massarNumber)
    }

    @Test
    fun parseStream_legacyBinaryXls_returnsUnsupportedError() {
        val oleHeader = byteArrayOf(
            0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte(),
            0xA1.toByte(), 0xB1.toByte(), 0x1A.toByte(), 0xE1.toByte()
        )
        val result = ExcelRosterParser.parseStream(ByteArrayInputStream(oleHeader))
        assertEquals(
            ExcelParseResult.Error(ExcelParseErrorReason.BINARY_XLS_UNSUPPORTED),
            result
        )
    }

    private fun buildMinimalXlsx(sharedStrings: List<String>, rows: List<List<String>>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zos ->
            zos.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            val sstXml = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
                sharedStrings.forEach { text ->
                    append("<si><t>").append(text).append("</t></si>")
                }
                append("</sst>")
            }
            zos.write(sstXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            val sheetXml = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
                rows.forEachIndexed { rIdx, cols ->
                    val rowNum = rIdx + 1
                    append("<row r=\"$rowNum\">")
                    cols.forEachIndexed { cIdx, sIdx ->
                        val colLetter = ('A' + cIdx)
                        append("<c r=\"$colLetter$rowNum\" t=\"s\"><v>$sIdx</v></c>")
                    }
                    append("</row>")
                }
                append("</sheetData></worksheet>")
            }
            zos.write(sheetXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        return out.toByteArray()
    }
}
