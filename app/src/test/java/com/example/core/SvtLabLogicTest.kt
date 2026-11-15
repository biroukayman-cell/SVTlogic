package com.example.core

import com.example.data.svt.SvtKnowledgeCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SvtLabLogicTest {

    @Test
    fun `cleanDnaSequence strips invalid characters and normalizes uppercase`() {
        assertEquals("ATCG", SvtLabLogic.cleanDnaSequence("a t-c_g123!u"))
    }

    @Test
    fun `transcribeAndTranslate computes complementary DNA, mRNA, and stops at STOP codon`() {
        val result = SvtLabLogic.transcribeAndTranslate("TACAAAGTCGGTTTACCTATT")

        assertTrue(result.hasValidBases)
        assertEquals("TACAAAGTCGGTTTACCTATT", result.templateDna)
        assertEquals("ATGTTTCAGCCAAATGGATAA", result.complementaryDna)
        assertEquals("AUGUUUCAGCCAAAUGGAUAA", result.mrnaStrand)
        assertEquals(listOf("AUG", "UUU", "CAG", "CCA", "AAU", "GGA", "UAA"), result.codons)
        assertEquals(listOf("Met", "Phe", "Gln", "Pro", "Asn", "Gly", "STOP"), result.aminoAcids)
        assertEquals("", result.trailingBases)
        assertTrue(result.stoppedEarly)
    }

    @Test
    fun `transcribeAndTranslate captures sickle cell HbA vs HbS point mutation`() {
        val normal = SvtLabLogic.transcribeAndTranslate("CACGTGGACTGAGGACTCCTC")
        val sickle = SvtLabLogic.transcribeAndTranslate("CACGTGGACTGAGGACACCTC")

        assertFalse(normal.stoppedEarly)
        assertFalse(sickle.stoppedEarly)
        // Normal codon 6 is GAG (Glu), sickle-cell codon 6 is GUG (Val)
        assertEquals("Glu", normal.aminoAcids[5])
        assertEquals("Val", sickle.aminoAcids[5])
        assertNotEquals(normal.aminoAcids, sickle.aminoAcids)
    }

    @Test
    fun `transcribeAndTranslate handles incomplete trailing bases`() {
        val result = SvtLabLogic.transcribeAndTranslate("TACAA")
        assertEquals(listOf("AUG"), result.codons)
        assertEquals(listOf("Met"), result.aminoAcids)
        assertEquals("UU", result.trailingBases)
        assertFalse(result.stoppedEarly)
    }

    @Test
    fun `computeMicroscopeMetrics calculates magnification, micrometers, and biological scale hints`() {
        val bloodCell = SvtLabLogic.computeMicroscopeMetrics(
            eyepieceMag = 10,
            objectiveMag = 40,
            observedMillimeters = 4.0
        )
        assertEquals(400, bloodCell.totalMagnification)
        assertEquals(0.01, bloodCell.realMm, 0.0001)
        assertEquals(10.0, bloodCell.realMicrometers, 0.01)
        assertEquals(10000.0, bloodCell.realNanometers, 0.1)
        assertEquals(BiologicalScaleHint.BLOOD_CELL, bloodCell.scaleHint)

        val plantCell = SvtLabLogic.computeMicroscopeMetrics(10, 10, 5.0)
        assertEquals(BiologicalScaleHint.PLANT_CELL, plantCell.scaleHint)

        val bacteria = SvtLabLogic.computeMicroscopeMetrics(10, 100, 2.0)
        assertEquals(BiologicalScaleHint.BACTERIA, bacteria.scaleHint)

        val virus = SvtLabLogic.computeMicroscopeMetrics(15, 100, 0.3)
        assertEquals(BiologicalScaleHint.NANOSCALE_VIRUS, virus.scaleHint)

        val tissue = SvtLabLogic.computeMicroscopeMetrics(5, 4, 5.0)
        assertEquals(BiologicalScaleHint.MACRO_TISSUE, tissue.scaleHint)
    }

    @Test
    fun `knowledge catalog entries have unique IDs and non-blank translations`() {
        assertTrue(SvtKnowledgeCatalog.glossaryEntries.size >= 15)
        assertEquals(
            SvtKnowledgeCatalog.glossaryEntries.size,
            SvtKnowledgeCatalog.glossaryEntries.map { it.id }.distinct().size
        )
        SvtKnowledgeCatalog.glossaryEntries.forEach { entry ->
            AppLanguage.entries.forEach { lang ->
                assertTrue(entry.term.get(lang).isNotBlank())
                assertTrue(entry.definition.get(lang).isNotBlank())
                assertTrue(entry.keyHint.get(lang).isNotBlank())
            }
        }

        assertTrue(SvtKnowledgeCatalog.flashcards.size >= 10)
        SvtKnowledgeCatalog.flashcards.forEach { card ->
            AppLanguage.entries.forEach { lang ->
                assertTrue(card.question.get(lang).isNotBlank())
                assertTrue(card.answer.get(lang).isNotBlank())
                assertTrue(card.explanation.get(lang).isNotBlank())
            }
        }
    }
}
