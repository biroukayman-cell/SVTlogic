package com.example.core

import com.example.data.model.UserProfile
import com.example.data.svt.SvtDomain
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

    @Test
    fun `resolveVisibleLabDomains parses valid subset and falls back safely`() {
        assertEquals(
            SvtDomain.entries.toSet(),
            SvtLabLogic.resolveVisibleLabDomains(emptyList())
        )
        assertEquals(
            SvtDomain.entries.toSet(),
            SvtLabLogic.resolveVisibleLabDomains(listOf("INVALID_DOMAIN"))
        )
        assertEquals(
            setOf(SvtDomain.GEOLOGY, SvtDomain.ECOLOGY),
            SvtLabLogic.resolveVisibleLabDomains(listOf("GEOLOGY", "ECOLOGY", "UNKNOWN"))
        )
    }

    @Test
    fun `DeviceSessionManager enforces single phone for students only`() {
        val teacherProfile = UserProfile(
            userId = "t1",
            role = "teacher",
            boundDeviceId = "dev_a"
        )
        assertTrue(DeviceSessionManager.isDeviceAllowedForStudent(teacherProfile, "dev_other"))

        val unboundStudent = UserProfile(
            userId = "s1",
            role = "student",
            boundDeviceId = ""
        )
        assertTrue(DeviceSessionManager.isDeviceAllowedForStudent(unboundStudent, "dev_a"))

        val boundStudent = unboundStudent.copy(boundDeviceId = "dev_a", boundDeviceModel = "Phone A")
        assertTrue(DeviceSessionManager.isDeviceAllowedForStudent(boundStudent, "dev_a"))
        assertFalse(DeviceSessionManager.isDeviceAllowedForStudent(boundStudent, "dev_b"))
    }

    @Test
    fun `normalizeSearchText strips French accents and normalizes Arabic forms`() {
        assertEquals("meiose", SvtLabLogic.normalizeSearchText("  Méiose "))
        assertEquals("fecondation", SvtLabLogic.normalizeSearchText("Fécondation"))
        assertEquals("immunite", SvtLabLogic.normalizeSearchText("Immunité"))
        assertEquals("noeud de ranvier", SvtLabLogic.normalizeSearchText("Nœud de Ranvier"))
        assertEquals("الاخصاب", SvtLabLogic.normalizeSearchText("الإخصاب"))
        assertEquals("المناعه", SvtLabLogic.normalizeSearchText("المَنَاعَة"))
    }

    @Test
    fun `isSearchableSvtQuery rejects empty short or numeric-only queries`() {
        assertFalse(SvtLabLogic.isSearchableSvtQuery(""))
        assertFalse(SvtLabLogic.isSearchableSvtQuery("a"))
        assertFalse(SvtLabLogic.isSearchableSvtQuery("12345"))
        assertTrue(SvtLabLogic.isSearchableSvtQuery("adn"))
        assertTrue(SvtLabLogic.isSearchableSvtQuery("anticorps"))
    }

    @Test
    fun `filterGlossaryEntries finds unaccented SVT queries across allowed domains and ignores non-SVT queries`() {
        val allDomains = SvtDomain.entries.toSet()

        val meiosisResults = SvtLabLogic.filterGlossaryEntries(
            entries = SvtKnowledgeCatalog.glossaryEntries,
            rawQuery = "meiose",
            language = AppLanguage.FR,
            allowedDomains = allDomains,
            selectedDomain = SvtDomain.GEOLOGY
        )
        assertTrue(meiosisResults.any { it.id == "meiosis" })

        val immunityResults = SvtLabLogic.filterGlossaryEntries(
            entries = SvtKnowledgeCatalog.glossaryEntries,
            rawQuery = "anticorps",
            language = AppLanguage.FR,
            allowedDomains = allDomains,
            selectedDomain = null
        )
        assertTrue(immunityResults.any { it.domain == SvtDomain.IMMUNITY })

        val reproResults = SvtLabLogic.filterGlossaryEntries(
            entries = SvtKnowledgeCatalog.glossaryEntries,
            rawQuery = "fecondation",
            language = AppLanguage.FR,
            allowedDomains = allDomains,
            selectedDomain = null
        )
        assertTrue(reproResults.any { it.domain == SvtDomain.REPRODUCTION })

        // Respect teacher's allowedDomains restriction
        val restrictedResults = SvtLabLogic.filterGlossaryEntries(
            entries = SvtKnowledgeCatalog.glossaryEntries,
            rawQuery = "fecondation",
            language = AppLanguage.FR,
            allowedDomains = setOf(SvtDomain.GEOLOGY),
            selectedDomain = null
        )
        assertTrue(restrictedResults.isEmpty())

        // Non-SVT terms must not match any glossary entry locally
        assertTrue(
            SvtLabLogic.filterGlossaryEntries(
                entries = SvtKnowledgeCatalog.glossaryEntries,
                rawQuery = "football",
                language = AppLanguage.FR,
                allowedDomains = allDomains,
                selectedDomain = null
            ).isEmpty()
        )
        assertTrue(
            SvtLabLogic.filterGlossaryEntries(
                entries = SvtKnowledgeCatalog.glossaryEntries,
                rawQuery = "recette pizza",
                language = AppLanguage.FR,
                allowedDomains = allDomains,
                selectedDomain = null
            ).isEmpty()
        )
    }

    @Test
    fun `every SvtDomain has glossary entries, flashcards, and fact of the day`() {
        assertTrue(SvtDomain.entries.contains(SvtDomain.IMMUNITY))
        assertTrue(SvtDomain.entries.contains(SvtDomain.REPRODUCTION))
        assertTrue(SvtDomain.entries.contains(SvtDomain.NERVOUS_MUSCULAR))
        assertEquals(7, SvtDomain.entries.size)

        SvtDomain.entries.forEach { domain ->
            assertTrue(
                "Missing glossary entry for $domain",
                SvtKnowledgeCatalog.glossaryEntries.any { it.domain == domain }
            )
            assertTrue(
                "Missing flashcard for $domain",
                SvtKnowledgeCatalog.flashcards.any { it.domain == domain }
            )
            assertTrue(
                "Missing fact of the day for $domain",
                SvtKnowledgeCatalog.factsOfTheDay.any { it.domain == domain }
            )
        }
    }

    @Test
    fun `enriched glossary has at least 36 unique entries and every domain has a mind map`() {
        assertTrue(
            "Expected at least 36 glossary entries, found ${SvtKnowledgeCatalog.glossaryEntries.size}",
            SvtKnowledgeCatalog.glossaryEntries.size >= 36
        )
        assertTrue(
            "Expected at least 8 mind maps, found ${SvtKnowledgeCatalog.mindMaps.size}",
            SvtKnowledgeCatalog.mindMaps.size >= 8
        )
        assertEquals(
            SvtKnowledgeCatalog.mindMaps.size,
            SvtKnowledgeCatalog.mindMaps.map { it.id }.distinct().size
        )

        SvtDomain.entries.forEach { domain ->
            assertTrue(
                "Missing mind map for $domain",
                SvtKnowledgeCatalog.mindMaps.any { it.domain == domain }
            )
        }

        SvtKnowledgeCatalog.mindMaps.forEach { map ->
            assertTrue(map.branches.isNotEmpty())
            AppLanguage.entries.forEach { lang ->
                assertTrue(map.centralConcept.get(lang).isNotBlank())
                assertTrue(map.subtitle.get(lang).isNotBlank())
                assertTrue(map.summary.get(lang).isNotBlank())
            }
        }
    }

    @Test
    fun `computeMetabolicEnergy distinguishes aerobic respiration and fermentations`() {
        val resp = SvtLabLogic.computeMetabolicEnergy(2, SvtLabLogic.MetabolicPathway.AEROBIC_RESPIRATION)
        assertEquals(76, resp.totalAtpMoles)
        assertEquals(12, resp.o2ConsumedMoles)
        assertEquals(12, resp.co2ProducedMoles)
        assertEquals(0, resp.organicByproductMoles)

        val lactic = SvtLabLogic.computeMetabolicEnergy(2, SvtLabLogic.MetabolicPathway.LACTIC_FERMENTATION)
        assertEquals(4, lactic.totalAtpMoles)
        assertEquals(0, lactic.o2ConsumedMoles)
        assertEquals(0, lactic.co2ProducedMoles)
        assertEquals(4, lactic.organicByproductMoles)

        val alcoholic = SvtLabLogic.computeMetabolicEnergy(2, SvtLabLogic.MetabolicPathway.ALCOHOLIC_FERMENTATION)
        assertEquals(4, alcoholic.totalAtpMoles)
        assertEquals(0, alcoholic.o2ConsumedMoles)
        assertEquals(4, alcoholic.co2ProducedMoles)
        assertEquals(4, alcoholic.organicByproductMoles)
    }

    @Test
    fun `computeMonohybridCross produces Mendelian 3 to 1 ratios for Aa x Aa`() {
        val result = SvtLabLogic.computeMonohybridCross(
            SvtLabLogic.MonohybridGenotype.HETERO,
            SvtLabLogic.MonohybridGenotype.HETERO
        )
        assertEquals(25, result.aaPercent)
        assertEquals(50, result.hetPercent)
        assertEquals(25, result.recPercent)
        assertEquals(75, result.dominantPhenotypePercent)
        assertEquals(25, result.recessivePhenotypePercent)
    }

    @Test
    fun `isBloodCompatible enforces ABO and Rh incompatibility rules`() {
        // Universal donor O- works for everyone
        assertTrue(SvtLabLogic.isBloodCompatible(SvtLabLogic.BloodGroupAbo.O, false, SvtLabLogic.BloodGroupAbo.AB, true))
        assertTrue(SvtLabLogic.isBloodCompatible(SvtLabLogic.BloodGroupAbo.O, false, SvtLabLogic.BloodGroupAbo.O, false))
        // Universal recipient AB+ can receive any type
        assertTrue(SvtLabLogic.isBloodCompatible(SvtLabLogic.BloodGroupAbo.A, true, SvtLabLogic.BloodGroupAbo.AB, true))
        assertTrue(SvtLabLogic.isBloodCompatible(SvtLabLogic.BloodGroupAbo.B, false, SvtLabLogic.BloodGroupAbo.AB, true))
        // Cross-group incompatibility & Rh+ into Rh- blocked
        assertFalse(SvtLabLogic.isBloodCompatible(SvtLabLogic.BloodGroupAbo.A, true, SvtLabLogic.BloodGroupAbo.B, true))
        assertFalse(SvtLabLogic.isBloodCompatible(SvtLabLogic.BloodGroupAbo.O, true, SvtLabLogic.BloodGroupAbo.O, false))
    }

    @Test
    fun `computeSeismicEpicenter and evaluateWaterQuality return accurate metrics`() {
        val seismic = SvtLabLogic.computeSeismicEpicenter(deltaTSeconds = 10.0, vpKmPerSec = 6.0, vsKmPerSec = 3.5)
        // d = 10 * (6 * 3.5) / (6 - 3.5) = 210 / 2.5 = 84.0 km
        assertEquals(84.0, seismic.distanceKm, 0.01)

        val excellentWater = SvtLabLogic.evaluateWaterQuality(dbo5MgL = 2.0, dcoMgL = 6.0, dissolvedO2MgL = 9.0)
        assertEquals(SvtLabLogic.WaterQualityClass.EXCELLENT, excellentWater.qualityClass)
        assertEquals(3.0, excellentWater.biodegradabilityRatio, 0.01)

        val pollutedWater = SvtLabLogic.evaluateWaterQuality(dbo5MgL = 25.0, dcoMgL = 80.0, dissolvedO2MgL = 1.8)
        assertEquals(SvtLabLogic.WaterQualityClass.POLLUTED, pollutedWater.qualityClass)
    }
}
