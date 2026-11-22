import {
  ALL_SVT_DOMAINS,
  Announcement,
  CourseResource,
  Enrollment,
  PrivateMessage,
  Quiz,
  QuizAttempt,
  StudentNotification,
  SvtClass,
  UserProfile,
  VideoTracking,
} from '../types/svt';

export const GUEST_CLASS: SvtClass = {
  id: 'demo_class_2bac',
  teacherId: 'demo_teacher_1',
  teacherName: 'Prof. Ayman BIROUK',
  name: '2ème Bac SVT — Groupe A (Démo)',
  academicYear: '2025-2026',
  level: '2BAC SVT',
  qrToken: 'DEMO-2BAC-SVT',
  visibleLabDomains: ALL_SVT_DOMAINS,
  createdAtMillis: Date.now() - 86400_000 * 30,
};

export const GUEST_PROFILE: UserProfile = {
  userId: 'demo_student_1',
  fullName: 'Salma El Mansouri (Invité)',
  phoneNumber: '+212 6 00 00 00 00',
  role: 'student',
  massarNumber: 'G130000000',
  birthDate: '14/05/2008',
  email: 'g130000000@taalim.ma',
  establishment: 'Lycée Ibn Sina (Démo)',
  teacherStatus: 'approved',
  mustChangePassword: false,
  acceptedTerms: true,
  avatarUrl: '',
  xp: 480,
  loginStreak: 4,
  lastLoginDate: '2026-03-10',
  unlockedBadgeIds: ['first_step', 'perfect_score', 'streak_3'],
  boundDeviceId: 'guest_demo_web',
  boundDeviceModel: 'Guest Demo Browser',
  createdAtMillis: Date.now() - 86400_000 * 20,
  updatedAtMillis: Date.now(),
};

export const GUEST_ENROLLMENT: Enrollment = {
  id: 'demo_student_1',
  studentId: 'demo_student_1',
  classId: GUEST_CLASS.id,
  className: GUEST_CLASS.name,
  teacherId: GUEST_CLASS.teacherId,
  teacherName: GUEST_CLASS.teacherName,
  studentName: GUEST_PROFILE.fullName,
  studentPhone: GUEST_PROFILE.phoneNumber,
  massarNumber: GUEST_PROFILE.massarNumber,
  qrToken: GUEST_CLASS.qrToken,
  status: 'approved',
  requestedAtMillis: Date.now() - 86400_000 * 20,
  validatedAtMillis: Date.now() - 86400_000 * 20,
};

export const GUEST_QUIZZES: Quiz[] = [
  {
    id: 'demo_quiz_1',
    classId: GUEST_CLASS.id,
    teacherId: GUEST_CLASS.teacherId,
    title: 'QCM 1 — Consommation de la matière organique & ATP',
    description: 'Évaluez vos connaissances sur la glycolyse, le cycle de Krebs et la fermentation.',
    chapterSvt: 'Physiologie & Métabolisme',
    isDownloadable: true,
    approvedStudentIds: [GUEST_PROFILE.userId],
    createdAtMillis: Date.now() - 86400_000 * 5,
    questions: [
      {
        questionText: 'Où se déroule la glycolyse dans la cellule eucaryote ?',
        options: [
          'Dans la matrice mitochondriale',
          'Dans le hyaloplasme (cytoplasme)',
          'Dans l’espace intermembranaire',
          'Dans le noyau',
        ],
        correctIndex: 1,
        explanation:
          'La glycolyse est une voie anaérobie qui se déroule dans le hyaloplasme et dégrade le glucose en 2 pyruvates.',
      },
      {
        questionText: 'Quel est le bilan net d’ATP de la respiration aérobie pour 1 mole de glucose ?',
        options: ['2 ATP', '12 ATP', '38 ATP', '100 ATP'],
        correctIndex: 2,
        explanation:
          'L’oxydation complète du glucose (glycolyse + Krebs + chaîne respiratoire) fournit environ 38 ATP.',
      },
      {
        questionText: 'Quel ion déclenche directement l’association actine-myosine dans le sarcomère ?',
        options: ['K⁺', 'Ca²⁺', 'Cl⁻', 'Fe²⁺'],
        correctIndex: 1,
        explanation:
          'La libération d’ions Ca²⁺ par le réticulum sarcoplasmique libère les sites de liaison sur l’actine.',
      },
    ],
  },
  {
    id: 'demo_quiz_2',
    classId: GUEST_CLASS.id,
    teacherId: GUEST_CLASS.teacherId,
    title: 'QCM 2 — Information génétique & Mitose',
    description: 'Révision rapide sur la réplication semi-conservative et l’expression des gènes.',
    chapterSvt: 'Génétique & Cellule',
    isDownloadable: true,
    approvedStudentIds: [GUEST_PROFILE.userId],
    createdAtMillis: Date.now() - 86400_000 * 2,
    questions: [
      {
        questionText: 'Quelle base azotée est spécifique à l’ARN (remplace la thymine T) ?',
        options: ['Adénine (A)', 'Guanine (G)', 'Cytosine (C)', 'Uracile (U)'],
        correctIndex: 3,
        explanation: 'L’ARN contient de l’uracile (U) à la place de la thymine (T).',
      },
      {
        questionText: 'Quel est le codon initiateur universel de la traduction ?',
        options: ['UAA', 'AUG', 'UGA', 'CCC'],
        correctIndex: 1,
        explanation: 'AUG code pour la méthionine et marque le début de la traduction.',
      },
    ],
  },
];

export const GUEST_ATTEMPTS: QuizAttempt[] = [
  {
    id: 'demo_attempt_2',
    quizId: 'demo_quiz_2',
    classId: GUEST_CLASS.id,
    studentId: GUEST_PROFILE.userId,
    studentName: GUEST_PROFILE.fullName,
    score: 2,
    maxScore: 2,
    totalDurationSeconds: 28,
    answers: [
      { questionIndex: 0, selectedIndex: 3, isCorrect: true, timeSpentSeconds: 14 },
      { questionIndex: 1, selectedIndex: 1, isCorrect: true, timeSpentSeconds: 14 },
    ],
    submittedAtMillis: Date.now() - 86400_000,
  },
];

export const GUEST_RESOURCES: CourseResource[] = [
  {
    id: 'demo_res_video',
    classId: GUEST_CLASS.id,
    teacherId: GUEST_CLASS.teacherId,
    title: 'Tutoriel BioTerre SVT — Découvrir votre espace élève',
    description: 'Vidéo de démonstration avec suivi automatique de progression sur Safari & Web.',
    type: 'VIDEO',
    fileUrl: '/tutoriel_eleves_bioterre_svt.mp4',
    durationSeconds: 90,
    isDownloadable: false,
    approvedStudentIds: [GUEST_PROFILE.userId],
    createdAtMillis: Date.now() - 86400_000 * 4,
  },
  {
    id: 'demo_res_pdf',
    classId: GUEST_CLASS.id,
    teacherId: GUEST_CLASS.teacherId,
    title: 'Fiche de synthèse — Chaînes de montagnes (Subduction & Collision)',
    description: 'Document de révision (aperçu démonstration).',
    type: 'PDF',
    fileUrl: 'https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf',
    durationSeconds: 0,
    isDownloadable: true,
    approvedStudentIds: [GUEST_PROFILE.userId],
    createdAtMillis: Date.now() - 86400_000 * 3,
  },
];

export const GUEST_VIDEO_TRACKING: VideoTracking[] = [
  {
    id: 'demo_vt_1',
    studentId: GUEST_PROFILE.userId,
    studentName: GUEST_PROFILE.fullName,
    resourceId: 'demo_res_video',
    classId: GUEST_CLASS.id,
    watchedSeconds: 45,
    totalDurationSeconds: 90,
    completionPercentage: 50,
    status: 'partial',
    updatedAtMillis: Date.now() - 3600_000 * 5,
  },
];

export const GUEST_ANNOUNCEMENTS: Announcement[] = [
  {
    id: 'demo_ann_1',
    classId: GUEST_CLASS.id,
    teacherId: GUEST_CLASS.teacherId,
    teacherName: GUEST_CLASS.teacherName,
    title: 'Bienvenue dans l’espace BioTerre SVT (Démo)',
    content:
      'Explorez le QCM chronométré, testez le Labo SVT (Cartes Mentales, Simulateur ADN, Calculateur ATP, Punnett) et découvrez le lecteur vidéo.',
    approvedStudentIds: [GUEST_PROFILE.userId],
    createdAtMillis: Date.now() - 86400_000 * 2,
  },
];

export const GUEST_MESSAGES: PrivateMessage[] = [
  {
    id: 'demo_msg_1',
    classId: GUEST_CLASS.id,
    teacherId: GUEST_CLASS.teacherId,
    studentId: GUEST_PROFILE.userId,
    studentName: GUEST_PROFILE.fullName,
    senderId: GUEST_CLASS.teacherId,
    senderRole: 'teacher',
    text: 'Bonjour Salma ! N’hésite pas à poser tes questions sur le chapitre ATP ou Génétique ici.',
    isRead: true,
    createdAtMillis: Date.now() - 86400_000,
  },
];

export const GUEST_NOTIFICATIONS: StudentNotification[] = [
  {
    id: 'demo_notif_1',
    recipientId: GUEST_PROFILE.userId,
    classId: GUEST_CLASS.id,
    type: 'quiz',
    title: 'Nouveau QCM disponible',
    message: 'QCM 1 — Consommation de la matière organique & ATP est prêt !',
    isRead: false,
    createdAtMillis: Date.now() - 7200_000,
  },
];
