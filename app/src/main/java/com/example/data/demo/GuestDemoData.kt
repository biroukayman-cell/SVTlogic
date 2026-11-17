package com.example.data.demo

import com.example.data.model.Announcement
import com.example.data.model.CourseResource
import com.example.data.model.Enrollment
import com.example.data.model.PrivateMessage
import com.example.data.model.Quiz
import com.example.data.model.QuizQuestion
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp

object GuestDemoData {
    const val GUEST_USER_ID = "guest_demo_viewer"
    const val GUEST_CLASS_ID = "guest_demo_class"

    val profile = UserProfile(
        id = GUEST_USER_ID,
        userId = GUEST_USER_ID,
        fullName = "Visiteur Démo (Exemple)",
        phoneNumber = "06 00 00 00 00",
        role = "student",
        massarNumber = "DEMO-SVT-2026",
        birthDate = "01/01/2008",
        email = "",
        establishment = "Lycée Exemple SVT",
        teacherStatus = "approved",
        acceptedTerms = true,
        xp = 120L,
        loginStreak = 3L,
        lastLoginDate = "2026-11-16",
        unlockedBadgeIds = listOf("first_quiz", "streak_3")
    )

    val enrollment = Enrollment(
        id = GUEST_USER_ID,
        studentId = GUEST_USER_ID,
        classId = GUEST_CLASS_ID,
        className = "2ème Bac SVT (Aperçu Démo)",
        teacherId = "guest_demo_teacher",
        teacherName = "Prof. Exemple SVT",
        studentName = profile.fullName,
        status = "approved"
    )

    val quizzes: List<Quiz> = listOf(
        Quiz(
            id = "demo_quiz_atp",
            classId = GUEST_CLASS_ID,
            teacherId = "guest_demo_teacher",
            title = "Exemple QCM : Libération d'énergie & ATP",
            description = "Aperçu interactif de démonstration (envoi de réponses désactivé en mode Invité).",
            chapterSvt = "Consommation de la matière organique et flux d'énergie",
            isPublished = true,
            isDownloadable = false,
            questions = listOf(
                QuizQuestion(
                    questionText = "Dans quel compartiment cellulaire se déroule la glycolyse ?",
                    options = listOf(
                        "Le hyaloplasme (cytoplasme)",
                        "La matrice mitochondriale",
                        "Les crêtes mitochondriales",
                        "Le noyau cellulaire"
                    ),
                    correctIndex = 0,
                    explanation = "La glycolyse est une voie anaérobie qui se déroule dans le hyaloplasme."
                ),
                QuizQuestion(
                    questionText = "Quel est le rôle principal du dioxygène dans la chaîne respiratoire ?",
                    options = listOf(
                        "Accepteur final d'électrons et de protons (formant H2O)",
                        "Donneur de phosphate à l'ADP",
                        "Catalyseur de la fermentation lactique",
                        "Transporteur du glucose"
                    ),
                    correctIndex = 0,
                    explanation = "O2 capte les électrons en fin de chaîne pour former H2O."
                )
            )
        ),
        Quiz(
            id = "demo_quiz_immunity",
            classId = GUEST_CLASS_ID,
            teacherId = "guest_demo_teacher",
            title = "Exemple QCM : Réponse immunitaire spécifique",
            description = "Consultation en lecture seule d'un exercice de démonstration.",
            chapterSvt = "Immunologie",
            isPublished = true,
            isDownloadable = false,
            questions = listOf(
                QuizQuestion(
                    questionText = "Quelles cellules produisent les anticorps circulants ?",
                    options = listOf(
                        "Les plasmocytes (issus des lymphocytes B)",
                        "Les hématies",
                        "Les plaquettes sanguines",
                        "Les neurones"
                    ),
                    correctIndex = 0,
                    explanation = "Les lymphocytes B activés se différencient en plasmocytes sécréteurs d'anticorps."
                )
            )
        )
    )

    val resources: List<CourseResource> = listOf(
        CourseResource(
            id = "demo_res_1",
            classId = GUEST_CLASS_ID,
            teacherId = "guest_demo_teacher",
            title = "Exemple Fiche Cours : Cycle de Krebs (PDF)",
            description = "Document d'exemple présenté en aperçu. Le téléchargement et l'ajout de fichiers nécessitent un compte.",
            type = "pdf",
            fileUrl = "",
            durationSeconds = 0L,
            isDownloadable = false
        ),
        CourseResource(
            id = "demo_res_2",
            classId = GUEST_CLASS_ID,
            teacherId = "guest_demo_teacher",
            title = "Exemple Séquence Vidéo : Contraction musculaire",
            description = "Aperçu de suivi vidéo pédagogique SVT.",
            type = "video",
            fileUrl = "",
            durationSeconds = 320L,
            isDownloadable = false
        )
    )

    val announcements: List<Announcement> = listOf(
        Announcement(
            id = "demo_ann_1",
            classId = GUEST_CLASS_ID,
            teacherId = "guest_demo_teacher",
            title = "Exemple d'annonce de classe",
            content = "Ceci est un exemple d'annonce publiée par un professeur. En mode Invité, vous consultez l'interface uniquement sans recevoir de vraies annonces ni répondre.",
            createdAt = Timestamp.now()
        ),
        Announcement(
            id = "demo_ann_2",
            classId = GUEST_CLASS_ID,
            teacherId = "guest_demo_teacher",
            title = "Travaux pratiques Labo SVT",
            content = "Découvrez les outils SVT intégrés dans l'onglet Labo SVT : Glossaire, Cartes Mémo, Simulateur ADN → Protéine et Calculateur Microscope.",
            createdAt = Timestamp.now()
        )
    )

    val messages: List<PrivateMessage> = listOf(
        PrivateMessage(
            id = "demo_msg_1",
            classId = GUEST_CLASS_ID,
            teacherId = "guest_demo_teacher",
            studentId = GUEST_USER_ID,
            studentName = profile.fullName,
            senderId = "guest_demo_teacher",
            senderRole = "teacher",
            content = "Exemple de message privé : en mode Invité, cette conversation est un simple aperçu. Aucun message réel n'est reçu ou envoyé.",
            isRead = true
        )
    )
}
