package com.example.core

import kotlin.math.roundToInt

object ExternalAiAnalysisHelper {

    fun buildClassAnalysisPrompt(
        input: ClassAiAnalysisInput,
        language: AppLanguage
    ): String {
        val headerInstruction = when (language) {
            AppLanguage.FR -> """
                Tu es un conseiller pédagogique expert en Sciences de la Vie et de la Terre (SVT).
                Analyse les résultats complets de la classe ci-dessous et rédige en français clair et structuré :
                1. Une synthèse globale du niveau et de la participation de la classe.
                2. Les points forts identifiés (chapitres, notions et compétences maîtrisés).
                3. Les lacunes et difficultés principales (notions mal comprises, questions les plus échouées).
                4. Des pistes de remédiation SVT concrètes à appliquer en classe.
                (Ne mentionne aucun nom d'élève ; appuie-toi uniquement sur les données fournies.)
            """.trimIndent()
            AppLanguage.AR -> """
                أنت مستشار تربوي خبير في علوم الحياة والأرض (SVT).
                حلّل النتائج الشاملة للقسم أدناه واكتب باللغة العربية الفصحى التربوية تقريراً منظماً يتضمن:
                1. ملخصاً عاماً لمستوى القسم ونسبة المشاركة.
                2. نقاط القوة المحددة (الدروس والمفاهيم المتقنة).
                3. الثغرات والصعوبات الرئيسية (الأسئلة والمفاهيم التي تكررت فيها الأخطاء).
                4. خطة معالجة تربوية عملية داخل الفصل.
                (اعتمد فقط على المعطيات الرقمية المقدمة دون اختراع أسماء أو نتائج.)
            """.trimIndent()
            AppLanguage.EN -> """
                You are an expert Life and Earth Sciences (SVT) pedagogical advisor.
                Analyze the complete class performance data below and provide a clear, structured report in English with:
                1. An overall summary of class level and participation.
                2. Identified strengths (well-mastered chapters and concepts).
                3. Key learning gaps and difficulties (most-missed concepts/questions).
                4. Concrete SVT classroom remediation actions.
                (Rely strictly on the metrics provided without inventing names or extra scores.)
            """.trimIndent()
        }

        return buildString {
            appendLine(headerInstruction)
            appendLine()
            appendLine(formatSingleClassBlock(input, language))
        }.trim()
    }

    fun buildAllClassesAnalysisPrompt(
        inputs: List<ClassAiAnalysisInput>,
        language: AppLanguage
    ): String {
        if (inputs.size == 1) {
            return buildClassAnalysisPrompt(inputs.first(), language)
        }
        val headerInstruction = when (language) {
            AppLanguage.FR -> """
                Tu es un conseiller pédagogique expert en Sciences de la Vie et de la Terre (SVT).
                Voici les résultats détaillés de l'ensemble de mes classes (${inputs.size} classes).
                Rédige en français clair et structuré :
                1. Une vue comparative globale entre les classes (points communs, écarts de réussite et participation).
                2. Pour chaque classe : ses points forts, ses lacunes prioritaires et les chapitres/questions à retravailler.
                3. Un plan de remédiation SVT priorisé et adaptable par classe.
                (Appuie-toi strictement sur les données chiffrées fournies.)
            """.trimIndent()
            AppLanguage.AR -> """
                أنت مستشار تربوي خبير في علوم الحياة والأرض (SVT).
                إليك النتائج المفصلة لجميع أقسامي (${inputs.size} أقسام).
                اكتب باللغة العربية الفصحى التربوية تحليلاً منظماً يتضمن:
                1. مقارنة شاملة بين الأقسام (المستوى العام، المشاركة، الفوارق).
                2. لكل قسم: نقاط القوة، الثغرات ذات الأولوية، والأسئلة والدروس التي تحتاج دعماً.
                3. خطة معالجة تربوية عملية ومرتبة حسب الأولوية لكل قسم.
                (اعتمد حصرياً على المعطيات المقدمة.)
            """.trimIndent()
            AppLanguage.EN -> """
                You are an expert Life and Earth Sciences (SVT) pedagogical advisor.
                Below are the detailed results for all of my classes (${inputs.size} classes).
                Provide a clear, structured analysis in English covering:
                1. A global comparison across classes (mastery, participation, gaps).
                2. For each class: specific strengths, priority gaps, and weak chapters/questions.
                3. A prioritized SVT remediation plan tailored to each class.
                (Rely strictly on the provided metrics.)
            """.trimIndent()
        }

        val totalStudents = inputs.sumOf { it.totalStudentsCount }
        val activeStudents = inputs.sumOf { it.activeQcmStudentsCount }
        val totalOk = inputs.sumOf { it.totalSucceededQuestions }
        val totalKo = inputs.sumOf { it.totalFailedQuestions }
        val totalAns = totalOk + totalKo
        val globalQRate = if (totalAns > 0) ((totalOk.toDouble() / totalAns) * 100.0).roundToInt() else 0
        val avgAcrossClasses = if (inputs.isEmpty()) 0 else inputs.map { it.classAveragePct }.average().roundToInt()

        return buildString {
            appendLine(headerInstruction)
            appendLine()
            appendLine("=== RÉSUMÉ GLOBAL (${inputs.size} classes) ===")
            appendLine("- Élèves suivis : $totalStudents (Actifs QCM : $activeStudents)")
            appendLine("- Moyenne QCM inter-classes : $avgAcrossClasses%")
            appendLine("- Réponses aux questions : $totalOk réussies / $totalKo échouées ($globalQRate% réussite)")
            appendLine()
            inputs.forEachIndexed { idx, input ->
                appendLine("=== CLASSE ${idx + 1}/${inputs.size} ===")
                appendLine(formatSingleClassBlock(input, language))
                appendLine()
            }
        }.trim()
    }

    fun buildQuizFrequentErrorsPrompt(
        input: QuizErrorSummaryInput,
        language: AppLanguage
    ): String {
        val headerInstruction = when (language) {
            AppLanguage.FR -> """
                Tu es un conseiller pédagogique expert en Sciences de la Vie et de la Terre (SVT).
                Analyse les résultats question par question de ce QCM et rédige en français une synthèse structurée comprenant :
                1. Une synthèse globale des performances sur ce QCM.
                2. Les erreurs les plus fréquentes commises par les élèves (questions et mauvais choix récurrents).
                3. Les causes scientifiques ou confusions probables expliquant ces erreurs.
                4. Des pistes de remédiation et d'explication ciblées pour le prochain cours.
                (Appuie-toi strictement sur les données fournies ci-dessous.)
            """.trimIndent()
            AppLanguage.AR -> """
                أنت مستشار تربوي خبير في علوم الحياة والأرض (SVT).
                حلّل نتائج هذا الاختبار سؤالاً بسؤال واكتب باللغة العربية الفصحى التربوية ملخصاً منظماً يتضمن:
                1. خلاصة عامة لأداء التلاميذ في هذا الاختبار.
                2. الأخطاء الأكثر شيوعاً (الأسئلة والخيارات الخاطئة المتكررة).
                3. الأسباب العلمية أو الالتباسات المحتملة وراء هذه الأخطاء.
                4. سبل معالجة وشروحاً موجهة للحصة القادمة.
                (اعتمد حصرياً على المعطيات المقدمة أدناه.)
            """.trimIndent()
            AppLanguage.EN -> """
                You are an expert Life and Earth Sciences (SVT) pedagogical advisor.
                Analyze the question-by-question results of this quiz and write a structured synthesis in English covering:
                1. An overall summary of student performance on this quiz.
                2. The most frequent student errors (specific questions and recurring wrong choices).
                3. Likely scientific misconceptions explaining those errors.
                4. Targeted remediation and review explanations for the next lesson.
                (Rely strictly on the metrics provided below.)
            """.trimIndent()
        }

        val totalAnswers = input.totalSucceededAnswers + input.totalFailedAnswers
        val errorRatePct = if (totalAnswers > 0) {
            ((input.totalFailedAnswers.toDouble() / totalAnswers.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(0, 100)
        } else 0

        return buildString {
            appendLine(headerInstruction)
            appendLine()
            appendLine("QCM : ${input.quizTitle}")
            appendLine("Classe : ${input.className.ifBlank { "Classe SVT" }} (${input.level.ifBlank { "SVT" }})")
            appendLine("Chapitre SVT : ${input.chapterSvt.ifBlank { "SVT général" }}")
            appendLine("Nombre de questions : ${input.totalQuestions}")
            appendLine("Copies enregistrées : ${input.attemptsCount}")
            appendLine("Moyenne du QCM : ${input.averagePct}% • Taux d'erreur global : $errorRatePct% (${input.totalSucceededAnswers} réussies / ${input.totalFailedAnswers} échouées)")
            appendLine("Durée moyenne : ${input.averageDurationSeconds}s")
            appendLine()
            appendLine("Détail par question (trié par taux d'erreur décroissant) :")
            if (input.questionErrorMetrics.isEmpty()) {
                appendLine("- Aucune donnée question par question disponible.")
            } else {
                input.questionErrorMetrics.forEach { q ->
                    val wrongSummary = if (q.commonWrongOptions.isEmpty()) {
                        "aucune erreur"
                    } else {
                        q.commonWrongOptions.take(3).joinToString("; ") { opt ->
                            "\"${opt.optionText}\" (${opt.count}x)"
                        }
                    }
                    appendLine(
                        "- Q${q.questionNumber} (${q.errorRatePct}% d'erreur : ${q.incorrectCount} fausses / ${q.totalResponses} réponses, temps moy. ${q.avgTimeSpentSeconds}s) : \"${q.questionText}\""
                    )
                    appendLine("  • Bonne réponse : \"${q.correctOptionText}\"")
                    appendLine("  • Choix erronés fréquents : $wrongSummary")
                    if (q.explanation.isNotBlank()) {
                        appendLine("  • Notion attendue : ${q.explanation}")
                    }
                }
            }
        }.trim()
    }

    private fun formatSingleClassBlock(
        input: ClassAiAnalysisInput,
        language: AppLanguage
    ): String = buildString {
        val yearLabel = input.academicYear.ifBlank { "-" }
        val levelLabel = input.level.ifBlank { "SVT" }
        appendLine("Classe : ${input.className} | Niveau : $levelLabel | Année : $yearLabel")
        appendLine("Effectif suivi : ${input.totalStudentsCount} élève(s) (Actifs QCM : ${input.activeQcmStudentsCount}, Sans copie : ${input.inactiveStudentsCount})")
        appendLine("Moyenne QCM de la classe : ${input.classAveragePct}%")
        appendLine("Questions traitées : ${input.totalSucceededQuestions} réussies / ${input.totalFailedQuestions} échouées (${input.questionSuccessRatePct}% de réussite)")
        appendLine("Répartition des scores : >=70% : ${input.strongStudentsCount} | 50-69% : ${input.averageStudentsCount} | <50% : ${input.strugglingStudentsCount}")
        appendLine("Ressources de cours : ${input.totalResourcesCount} document(s)/vidéo(s) | Complétion vidéo : ${input.videoCompletionRatePct}%")
        appendLine()
        appendLine("Résultats par QCM et chapitre :")
        if (input.quizMetrics.isEmpty()) {
            appendLine("- Aucun QCM publié pour cette classe.")
        } else {
            input.quizMetrics.forEach { q ->
                val ch = q.chapterSvt.ifBlank { "SVT" }
                appendLine("- '${q.quizTitle}' [$ch] : ${q.attemptsCount} copie(s), moyenne ${q.averagePct}%, ${q.succeededQuestions} réussies / ${q.failedQuestions} échouées")
            }
        }
        appendLine()
        appendLine("Questions les mieux maîtrisées :")
        if (input.strongestQuestions.isEmpty()) {
            appendLine("- Aucune donnée enregistrée.")
        } else {
            input.strongestQuestions.forEach { q ->
                val ch = q.chapterSvt.ifBlank { q.quizTitle }
                appendLine("- [$ch] \"${q.questionText}\" -> ${q.successRatePct}% de réussite (${q.succeededCount}✓ / ${q.failedCount}✗)")
            }
        }
        appendLine()
        appendLine("Questions les plus échouées :")
        if (input.weakestQuestions.isEmpty()) {
            appendLine("- Aucune question échouée enregistrée.")
        } else {
            input.weakestQuestions.forEach { q ->
                val ch = q.chapterSvt.ifBlank { q.quizTitle }
                appendLine("- [$ch] \"${q.questionText}\" -> ${q.successRatePct}% de réussite (${q.succeededCount}✓ / ${q.failedCount}✗)")
            }
        }
    }.trim()
}
