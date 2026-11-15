package com.example.core

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
}
