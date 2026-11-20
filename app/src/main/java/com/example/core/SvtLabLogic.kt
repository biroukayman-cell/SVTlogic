package com.example.core

import com.example.data.svt.SvtDomain
import com.example.data.svt.SvtGlossaryEntry
import java.text.Normalizer
import java.util.Locale

data class DnaSynthesisResult(
    val templateDna: String,
    val complementaryDna: String,
    val mrnaStrand: String,
    val codons: List<String>,
    val aminoAcids: List<String>,
    val trailingBases: String,
    val stoppedEarly: Boolean
) {
    val hasValidBases: Boolean
        get() = templateDna.isNotEmpty()
}

enum class BiologicalScaleHint {
    NANOSCALE_VIRUS,
    BACTERIA,
    BLOOD_CELL,
    PLANT_CELL,
    MACRO_TISSUE
}

data class MicroscopeResult(
    val totalMagnification: Int,
    val observedMm: Double,
    val realMm: Double,
    val realMicrometers: Double,
    val realNanometers: Double,
    val scaleHint: BiologicalScaleHint
)

object SvtLabLogic {

    private val combiningMarksRegex = Regex("\\p{Mn}+")
    private val nonWordRegex = Regex("[^\\p{L}\\p{Nd}\\s]+")
    private val multiSpaceRegex = Regex("\\s+")

    private val codonTable: Map<String, String> = mapOf(
        "UUU" to "Phe", "UUC" to "Phe",
        "UUA" to "Leu", "UUG" to "Leu", "CUU" to "Leu", "CUC" to "Leu", "CUA" to "Leu", "CUG" to "Leu",
        "AUU" to "Ile", "AUC" to "Ile", "AUA" to "Ile",
        "AUG" to "Met",
        "GUU" to "Val", "GUC" to "Val", "GUA" to "Val", "GUG" to "Val",
        "UCU" to "Ser", "UCC" to "Ser", "UCA" to "Ser", "UCG" to "Ser", "AGU" to "Ser", "AGC" to "Ser",
        "CCU" to "Pro", "CCC" to "Pro", "CCA" to "Pro", "CCG" to "Pro",
        "ACU" to "Thr", "ACC" to "Thr", "ACA" to "Thr", "ACG" to "Thr",
        "GCU" to "Ala", "GCC" to "Ala", "GCA" to "Ala", "GCG" to "Ala",
        "UAU" to "Tyr", "UAC" to "Tyr",
        "UAA" to "STOP", "UAG" to "STOP", "UGA" to "STOP",
        "CAU" to "His", "CAC" to "His",
        "CAA" to "Gln", "CAG" to "Gln",
        "AAU" to "Asn", "AAC" to "Asn",
        "AAA" to "Lys", "AAG" to "Lys",
        "GAU" to "Asp", "GAC" to "Asp",
        "GAA" to "Glu", "GAG" to "Glu",
        "UGU" to "Cys", "UGC" to "Cys",
        "UGG" to "Trp",
        "CGU" to "Arg", "CGC" to "Arg", "CGA" to "Arg", "CGG" to "Arg", "AGA" to "Arg", "AGG" to "Arg",
        "GGU" to "Gly", "GGC" to "Gly", "GGA" to "Gly", "GGG" to "Gly"
    )

    fun cleanDnaSequence(raw: String): String {
        return raw.uppercase(Locale.US).filter { it == 'A' || it == 'T' || it == 'C' || it == 'G' }
    }

    fun transcribeAndTranslate(rawTranscribedDna: String): DnaSynthesisResult {
        val template = cleanDnaSequence(rawTranscribedDna)
        if (template.isEmpty()) {
            return DnaSynthesisResult(
                templateDna = "",
                complementaryDna = "",
                mrnaStrand = "",
                codons = emptyList(),
                aminoAcids = emptyList(),
                trailingBases = "",
                stoppedEarly = false
            )
        }

        val compDnaBuilder = StringBuilder(template.length)
        val mrnaBuilder = StringBuilder(template.length)
        for (base in template) {
            when (base) {
                'A' -> {
                    compDnaBuilder.append('T')
                    mrnaBuilder.append('U')
                }
                'T' -> {
                    compDnaBuilder.append('A')
                    mrnaBuilder.append('A')
                }
                'C' -> {
                    compDnaBuilder.append('G')
                    mrnaBuilder.append('G')
                }
                'G' -> {
                    compDnaBuilder.append('C')
                    mrnaBuilder.append('C')
                }
            }
        }

        val mrna = mrnaBuilder.toString()
        val completeTripletCount = mrna.length / 3
        val codons = (0 until completeTripletCount).map { idx ->
            mrna.substring(idx * 3, idx * 3 + 3)
        }
        val trailing = mrna.substring(completeTripletCount * 3)

        val aminoAcids = mutableListOf<String>()
        var stoppedEarly = false
        for (codon in codons) {
            val aa = codonTable[codon] ?: "?"
            aminoAcids.add(aa)
            if (aa == "STOP") {
                stoppedEarly = true
                break
            }
        }

        return DnaSynthesisResult(
            templateDna = template,
            complementaryDna = compDnaBuilder.toString(),
            mrnaStrand = mrna,
            codons = codons,
            aminoAcids = aminoAcids,
            trailingBases = trailing,
            stoppedEarly = stoppedEarly
        )
    }

    fun computeMicroscopeMetrics(
        eyepieceMag: Int,
        objectiveMag: Int,
        observedMillimeters: Double
    ): MicroscopeResult {
        val safeEyepiece = eyepieceMag.coerceAtLeast(1)
        val safeObjective = objectiveMag.coerceAtLeast(1)
        val totalMag = safeEyepiece * safeObjective
        val safeObs = observedMillimeters.coerceAtLeast(0.0)
        val realMm = safeObs / totalMag.toDouble()
        val realUm = realMm * 1000.0
        val realNm = realUm * 1000.0

        val hint = when {
            realUm < 0.5 -> BiologicalScaleHint.NANOSCALE_VIRUS
            realUm < 5.0 -> BiologicalScaleHint.BACTERIA
            realUm < 25.0 -> BiologicalScaleHint.BLOOD_CELL
            realUm < 150.0 -> BiologicalScaleHint.PLANT_CELL
            else -> BiologicalScaleHint.MACRO_TISSUE
        }

        return MicroscopeResult(
            totalMagnification = totalMag,
            observedMm = safeObs,
            realMm = realMm,
            realMicrometers = realUm,
            realNanometers = realNm,
            scaleHint = hint
        )
    }

    fun resolveVisibleLabDomains(domainNames: List<String>): Set<SvtDomain> {
        if (domainNames.isEmpty()) return SvtDomain.entries.toSet()
        val parsed = domainNames.mapNotNull { key ->
            SvtDomain.entries.firstOrNull { it.name == key }
        }.toSet()
        return parsed.ifEmpty { SvtDomain.entries.toSet() }
    }

    fun normalizeSearchText(raw: String): String {
        if (raw.isBlank()) return ""
        val expanded = raw.trim()
            .lowercase(Locale.ROOT)
            .replace("œ", "oe")
            .replace("æ", "ae")
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ى", "ي")
            .replace("ة", "ه")
            .replace("ـ", "")
        val decomposed = Normalizer.normalize(expanded, Normalizer.Form.NFD)
        val stripped = combiningMarksRegex.replace(decomposed, "")
        val cleaned = nonWordRegex.replace(stripped, " ")
        return multiSpaceRegex.replace(cleaned, " ").trim()
    }

    fun isSearchableSvtQuery(rawQuery: String): Boolean {
        val norm = normalizeSearchText(rawQuery)
        if (norm.length < 2) return false
        return norm.any { it.isLetter() }
    }

    fun filterGlossaryEntries(
        entries: List<SvtGlossaryEntry>,
        rawQuery: String,
        language: AppLanguage,
        allowedDomains: Set<SvtDomain>,
        selectedDomain: SvtDomain?
    ): List<SvtGlossaryEntry> {
        val allowed = entries.filter { it.domain in allowedDomains }
        val normQuery = normalizeSearchText(rawQuery)
        if (normQuery.isEmpty()) {
            return if (selectedDomain == null) allowed
            else allowed.filter { it.domain == selectedDomain }
        }

        val queryTokens = normQuery.split(' ').filter { it.length >= 3 }
        val scored = allowed.mapNotNull { entry ->
            val primaryTerm = normalizeSearchText(entry.term.get(language))
            val allTerms = listOf(
                primaryTerm,
                normalizeSearchText(entry.term.fr),
                normalizeSearchText(entry.term.en),
                normalizeSearchText(entry.term.ar)
            )
            val normKeywords = entry.keywords.map { normalizeSearchText(it) }
            val searchableCandidates = (allTerms + normKeywords).filter { it.isNotEmpty() }

            val directExact = searchableCandidates.any { it == normQuery }
            val prefixMatch = searchableCandidates.any { it.startsWith(normQuery) }
            val containsMatch = searchableCandidates.any { it.contains(normQuery) }
            val tokenMatch = queryTokens.isNotEmpty() && searchableCandidates.any { cand ->
                cand.length >= 3 && (queryTokens.any { token -> cand == token || (token.length >= 4 && cand.contains(token)) })
            }

            if (!directExact && !prefixMatch && !containsMatch && !tokenMatch) {
                null
            } else {
                var score = when {
                    directExact -> 100
                    prefixMatch -> 80
                    containsMatch -> 60
                    else -> 40
                }
                if (selectedDomain != null && entry.domain == selectedDomain) {
                    score += 10
                }
                entry to score
            }
        }

        return scored.sortedByDescending { it.second }.map { it.first }
    }

    // ---------------------------------------------------------
    // Extra Interactive Labo SVT Simulators Logic
    // ---------------------------------------------------------
    enum class MetabolicPathway {
        AEROBIC_RESPIRATION,
        LACTIC_FERMENTATION,
        ALCOHOLIC_FERMENTATION
    }

    data class MetabolicEnergyResult(
        val glucoseMoles: Int,
        val pathway: MetabolicPathway,
        val totalAtpMoles: Int,
        val o2ConsumedMoles: Int,
        val co2ProducedMoles: Int,
        val organicByproductMoles: Int,
        val yieldPercentage: Double
    )

    fun computeMetabolicEnergy(
        glucoseMoles: Int,
        pathway: MetabolicPathway
    ): MetabolicEnergyResult {
        val safeMoles = glucoseMoles.coerceIn(1, 20)
        return when (pathway) {
            MetabolicPathway.AEROBIC_RESPIRATION -> MetabolicEnergyResult(
                glucoseMoles = safeMoles,
                pathway = pathway,
                totalAtpMoles = safeMoles * 38,
                o2ConsumedMoles = safeMoles * 6,
                co2ProducedMoles = safeMoles * 6,
                organicByproductMoles = 0,
                yieldPercentage = 40.5
            )
            MetabolicPathway.LACTIC_FERMENTATION -> MetabolicEnergyResult(
                glucoseMoles = safeMoles,
                pathway = pathway,
                totalAtpMoles = safeMoles * 2,
                o2ConsumedMoles = 0,
                co2ProducedMoles = 0,
                organicByproductMoles = safeMoles * 2,
                yieldPercentage = 2.1
            )
            MetabolicPathway.ALCOHOLIC_FERMENTATION -> MetabolicEnergyResult(
                glucoseMoles = safeMoles,
                pathway = pathway,
                totalAtpMoles = safeMoles * 2,
                o2ConsumedMoles = 0,
                co2ProducedMoles = safeMoles * 2,
                organicByproductMoles = safeMoles * 2,
                yieldPercentage = 2.1
            )
        }
    }

    enum class MonohybridGenotype(val display: String, val gametes: Pair<String, String>) {
        HOMO_DOM("AA", "A" to "A"),
        HETERO("Aa", "A" to "a"),
        HOMO_REC("aa", "a" to "a")
    }

    data class GeneticCrossResult(
        val parent1: MonohybridGenotype,
        val parent2: MonohybridGenotype,
        val grid: List<List<String>>,
        val aaPercent: Int,
        val hetPercent: Int,
        val recPercent: Int,
        val dominantPhenotypePercent: Int,
        val recessivePhenotypePercent: Int
    )

    fun computeMonohybridCross(
        parent1: MonohybridGenotype,
        parent2: MonohybridGenotype
    ): GeneticCrossResult {
        val g1 = listOf(parent1.gametes.first, parent1.gametes.second)
        val g2 = listOf(parent2.gametes.first, parent2.gametes.second)

        fun combine(a: String, b: String): String {
            return when {
                a == "A" && b == "A" -> "AA"
                a == "a" && b == "a" -> "aa"
                else -> "Aa"
            }
        }

        val grid = g1.map { a1 -> g2.map { a2 -> combine(a1, a2) } }
        val flat = grid.flatten()
        val aa = flat.count { it == "AA" } * 25
        val het = flat.count { it == "Aa" } * 25
        val rec = flat.count { it == "aa" } * 25

        return GeneticCrossResult(
            parent1 = parent1,
            parent2 = parent2,
            grid = grid,
            aaPercent = aa,
            hetPercent = het,
            recPercent = rec,
            dominantPhenotypePercent = aa + het,
            recessivePhenotypePercent = rec
        )
    }

    enum class BloodGroupAbo(val label: String, val antigens: Set<Char>) {
        O("O", emptySet()),
        A("A", setOf('A')),
        B("B", setOf('B')),
        AB("AB", setOf('A', 'B'))
    }

    fun isBloodCompatible(
        donorAbo: BloodGroupAbo,
        donorRhPositive: Boolean,
        recipientAbo: BloodGroupAbo,
        recipientRhPositive: Boolean
    ): Boolean {
        val aboOk = recipientAbo.antigens.containsAll(donorAbo.antigens)
        val rhOk = !donorRhPositive || recipientRhPositive
        return aboOk && rhOk
    }

    data class SeismicWaveResult(
        val deltaTSeconds: Double,
        val vpKmPerSec: Double,
        val vsKmPerSec: Double,
        val distanceKm: Double,
        val estimatedCrustDepthHint: BiologicalScaleHint? = null
    )

    fun computeSeismicEpicenter(
        deltaTSeconds: Double,
        vpKmPerSec: Double = 6.0,
        vsKmPerSec: Double = 3.5
    ): SeismicWaveResult {
        val safeDelta = deltaTSeconds.coerceIn(0.0, 600.0)
        val safeVp = vpKmPerSec.coerceAtLeast(1.5)
        val safeVs = vsKmPerSec.coerceIn(0.5, safeVp - 0.1)
        val dist = safeDelta * (safeVp * safeVs) / (safeVp - safeVs)
        return SeismicWaveResult(
            deltaTSeconds = safeDelta,
            vpKmPerSec = safeVp,
            vsKmPerSec = safeVs,
            distanceKm = dist
        )
    }

    enum class WaterQualityClass {
        EXCELLENT,
        GOOD,
        MODERATE,
        POLLUTED
    }

    data class WaterQualityResult(
        val dbo5MgL: Double,
        val dcoMgL: Double,
        val dissolvedO2MgL: Double,
        val biodegradabilityRatio: Double,
        val qualityClass: WaterQualityClass
    )

    fun evaluateWaterQuality(
        dbo5MgL: Double,
        dcoMgL: Double,
        dissolvedO2MgL: Double
    ): WaterQualityResult {
        val safeDbo5 = dbo5MgL.coerceAtLeast(0.1)
        val safeDco = dcoMgL.coerceAtLeast(safeDbo5)
        val safeO2 = dissolvedO2MgL.coerceIn(0.0, 20.0)
        val ratio = safeDco / safeDbo5

        val quality = when {
            safeDbo5 <= 3.0 && safeO2 >= 7.0 -> WaterQualityClass.EXCELLENT
            safeDbo5 <= 6.0 && safeO2 >= 5.0 -> WaterQualityClass.GOOD
            safeDbo5 <= 12.0 && safeO2 >= 3.0 -> WaterQualityClass.MODERATE
            else -> WaterQualityClass.POLLUTED
        }
        return WaterQualityResult(
            dbo5MgL = safeDbo5,
            dcoMgL = safeDco,
            dissolvedO2MgL = safeO2,
            biodegradabilityRatio = ratio,
            qualityClass = quality
        )
    }
}
