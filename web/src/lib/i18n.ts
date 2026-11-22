import { AppLanguage, SvtDomain } from '../types/svt';

export const LANG_STORAGE_KEY = 'bioterre_language';

export function loadSavedLanguage(): AppLanguage {
  try {
    const raw = localStorage.getItem(LANG_STORAGE_KEY);
    if (raw === 'fr' || raw === 'ar' || raw === 'en') return raw;
  } catch {}
  return 'fr';
}

export function saveLanguage(lang: AppLanguage): void {
  try {
    localStorage.setItem(LANG_STORAGE_KEY, lang);
  } catch {}
  document.documentElement.lang = lang;
  document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr';
}

export const DOMAIN_LABELS: Record<SvtDomain, Record<AppLanguage, string>> = {
  CELL_GENETICS: {
    fr: 'Génétique & Cellule',
    ar: 'الوراثة والخلية',
    en: 'Genetics & Cell',
  },
  IMMUNITY: {
    fr: 'Immunité',
    ar: 'علم المناعة',
    en: 'Immunity',
  },
  REPRODUCTION: {
    fr: 'Reproduction',
    ar: 'التوالد',
    en: 'Reproduction',
  },
  PHYSIOLOGY_IMMUNITY: {
    fr: 'Physiologie & Métabolisme',
    ar: 'الفيزيولوجيا والاستقلاب',
    en: 'Physiology & Metabolism',
  },
  NERVOUS_MUSCULAR: {
    fr: 'Système nerveux & Muscle',
    ar: 'الجهاز العصبي والعضلي',
    en: 'Nervous & Muscular',
  },
  GEOLOGY: {
    fr: 'Géologie',
    ar: 'الجيولوجيا',
    en: 'Geology',
  },
  ECOLOGY: {
    fr: 'Écologie',
    ar: 'علم البيئة',
    en: 'Ecology',
  },
};

export interface Translations {
  appName: string;
  appSubtitle: string;
  welcomeTitle: string;
  welcomeDesc: string;
  whoAreYou: string;
  quickQrTitle: string;
  quickQrSub: string;
  studentRoleBtn: string;
  studentRoleSub: string;
  teacherRoleBtn: string;
  teacherRoleSub: string;
  guestModeBtn: string;
  guestModeSub: string;
  guestBannerTitle: string;
  guestBannerDesc: string;
  guestExit: string;
  guestBlockedMsg: string;
  changeRole: string;
  signIn: string;
  register: string;
  signOut: string;
  cancel: string;
  save: string;
  close: string;
  delete: string;
  search: string;
  loading: string;
  massarLabel: string;
  massarOrEmailLabel: string;
  massarOptional: string;
  emailProLabel: string;
  recoveryEmailLabel: string;
  taalimHint: string;
  passwordLabel: string;
  confirmPasswordLabel: string;
  fullNameLabel: string;
  phoneOptional: string;
  phoneRequired: string;
  establishmentRequired: string;
  birthDateLabel: string;
  forgotPassword: string;
  forgotPasswordDescStudent: string;
  forgotPasswordDescTeacher: string;
  sendResetLink: string;
  resetEmailSent: string;
  googleSignIn: string;
  teacherApprovalNotice: string;
  firebaseMissingTitle: string;
  firebaseMissingBody: string;

  // Onboarding gates
  setupTitle: string;
  setupSubStudent: string;
  setupSubTeacher: string;
  continueBtn: string;
  qrLockedBadge: string;
  qrLockedNotice: string;
  termsTitle: string;
  termsSubtitle: string;
  termsCheckbox: string;
  termsContinue: string;
  termsPrivacyLink: string;
  teacherPendingTitle: string;
  teacherPendingBody: string;
  teacherRejectedTitle: string;
  teacherRejectedBody: string;
  refreshStatus: string;
  qrJoinTitle: string;
  qrJoinDesc: string;
  qrScanBtn: string;
  qrPickGallery: string;
  qrManualLabel: string;
  qrJoinSubmit: string;
  qrInvalidCode: string;
  qrNotFoundInImage: string;
  pendingTitle: string;
  pendingDesc: string;
  rejectedTitle: string;
  rejectedDesc: string;
  changeClassCode: string;
  deviceLockedTitle: string;
  deviceLockedBody: string;

  // Tabs
  tabExercises: string;
  tabLab: string;
  tabResources: string;
  tabAnnouncements: string;
  tabMessaging: string;
  tabProfile: string;
  tabDashboard: string;
  tabClasses: string;

  // iOS PWA banner
  iosInstallTitle: string;
  iosInstallSteps: string;

  // Notifications
  notificationsTitle: string;
  notificationsEmpty: string;
  markAllRead: string;

  // Exercises / QCM
  todoFilter: string;
  doneFilter: string;
  allFilter: string;
  quizStart: string;
  quizViewResult: string;
  quizPendingTag: string;
  quizCompletedTag: string;
  quizEmpty: string;
  quizNext: string;
  quizPrevious: string;
  quizSubmit: string;
  quizResultTitle: string;
  quizExplanation: string;
  quizCreate: string;
  quizViewResultsBtn: string;

  // Resources
  typeVideo: string;
  typePdf: string;
  typeWord: string;
  typeImage: string;
  resourcesEmpty: string;
  watchVideo: string;
  openDocument: string;
  videoNotStarted: string;
  videoPartial: string;
  videoCompleted: string;
  fileOpened: string;
  fileNotOpened: string;
  downloadFile: string;
  publishResource: string;

  // Announcements & Messaging
  announcementNew: string;
  announcementPublish: string;
  announcementsEmpty: string;
  messagingStudentHeader: string;
  messagingStudentNotice: string;
  messagingTeacherInbox: string;
  messagingInputHint: string;
  messagingSend: string;
  messagingEmpty: string;

  // Profile & PIN & Theme
  personalInfo: string;
  editProfile: string;
  changePhoto: string;
  changePasswordOptional: string;
  badgesTitle: string;
  themeSectionTitle: string;
  themeModeLabel: string;
  themeSystem: string;
  themeLight: string;
  themeDark: string;
  paletteLabel: string;
  paletteEmerald: string;
  paletteOcean: string;
  paletteEarth: string;
  paletteBotanical: string;
  pinTitle: string;
  pinEnabledDesc: string;
  pinDisabledDesc: string;
  pinConfigure: string;
  pinManage: string;
  pinUnlockTitle: string;
  pinUnlockBtn: string;
  pinDisableBtn: string;
  pinWrongAttempts: string;
  pinLockedSeconds: string;

  // Teacher Dashboard & Classes
  dashboardTitle: string;
  dashboardSubtitle: string;
  overviewTab: string;
  resultsTab: string;
  kpiClasses: string;
  kpiStudents: string;
  kpiPending: string;
  kpiQcmAvg: string;
  kpiResourcesVideo: string;
  classBreakdown: string;
  qcmResultsTitle: string;
  qcmResultsDesc: string;
  qcmSearchHint: string;
  sortByPct: string;
  sortByName: string;
  notTaken: string;
  createClassTitle: string;
  pendingEnrollments: string;
  approveAll: string;
  approve: string;
  reject: string;
  approvedStudents: string;
  unlinkDevice: string;
  rosterTitle: string;
  importExcelCsv: string;
  addStudentManual: string;
  printCards: string;
  labDomainsVisibility: string;
  adminTeacherApprovals: string;

  // Legal v2
  legalTitle: string;
  legalBody: string;
  privacyTitle: string;
  privacyBody: string;
  termsModalTitle: string;
  termsBody: string;
}

export const TRANSLATIONS: Record<AppLanguage, Translations> = {
  fr: {
    appName: 'BioTerre SVT',
    appSubtitle: 'Sciences de la Vie et de la Terre',
    welcomeTitle: 'Bienvenue sur BioTerre SVT',
    welcomeDesc:
      'Votre espace pédagogique pour les cours, QCM chronométrés, Labo SVT interactif, suivi vidéo et badges de progression.',
    whoAreYou: 'Qui êtes-vous ?',
    quickQrTitle: 'Rejoindre directement par QR Code',
    quickQrSub: 'Inscription automatique immédiate dans votre classe sans attente',
    studentRoleBtn: 'Je suis un Élève',
    studentRoleSub: 'Connexion avec Numéro Massar ou e-mail @taalim.ma',
    teacherRoleBtn: 'Je suis un Professeur',
    teacherRoleSub: 'Espace enseignant avec e-mail professionnel ou Google',
    guestModeBtn: 'Mode Invité (Aperçu Démo)',
    guestModeSub: 'Découvrir l’interface SVT et le Labo en lecture seule',
    guestBannerTitle: 'Mode Invité • Aperçu Démo',
    guestBannerDesc: 'Lecture seule : aucune sauvegarde ni envoi possible.',
    guestExit: 'Quitter',
    guestBlockedMsg: 'Mode Invité (lecture seule) : inscrivez-vous pour enregistrer votre progression.',
    changeRole: 'Changer de profil',
    signIn: 'Se connecter',
    register: 'Créer un compte',
    signOut: 'Se déconnecter',
    cancel: 'Annuler',
    save: 'Enregistrer',
    close: 'Fermer',
    delete: 'Supprimer',
    search: 'Rechercher...',
    loading: 'Chargement...',
    massarLabel: 'Numéro Massar',
    massarOrEmailLabel: 'Numéro Massar (ou e-mail)',
    massarOptional: 'Numéro Massar (optionnel)',
    emailProLabel: 'E-mail professionnel',
    recoveryEmailLabel: 'E-mail de récupération',
    taalimHint: 'Par défaut : numeromassar@taalim.ma',
    passwordLabel: 'Mot de passe',
    confirmPasswordLabel: 'Confirmer le mot de passe',
    fullNameLabel: 'Nom complet',
    phoneOptional: 'Téléphone (optionnel)',
    phoneRequired: 'Téléphone *',
    establishmentRequired: 'Établissement scolaire *',
    birthDateLabel: 'Date de naissance',
    forgotPassword: 'Mot de passe oublié ?',
    forgotPasswordDescStudent:
      'Saisissez votre e-mail de récupération (ou Massar) pour recevoir un lien, ou demandez un mot de passe provisoire à votre professeur.',
    forgotPasswordDescTeacher:
      'Saisissez votre e-mail professionnel pour recevoir un lien de réinitialisation sécurisé.',
    sendResetLink: 'Envoyer le lien par e-mail',
    resetEmailSent: 'Un e-mail de réinitialisation a été envoyé.',
    googleSignIn: 'Continuer avec Google',
    teacherApprovalNotice:
      'L’accès professeur nécessite la validation de l’administrateur avant la création de classes.',
    firebaseMissingTitle: 'Configuration Firebase Web requise',
    firebaseMissingBody:
      'Ajoutez VITE_FIREBASE_API_KEY et VITE_FIREBASE_APP_ID dans les variables d’environnement pour activer la connexion temps réel. Le Mode Invité et le Labo SVT restent accessibles.',

    setupTitle: 'Finaliser l’inscription',
    setupSubStudent: 'Complétez vos informations pour accéder à votre classe SVT.',
    setupSubTeacher: 'Confirmez votre identité professionnelle pour gérer vos classes.',
    continueBtn: 'Continuer',
    qrLockedBadge: 'Identité QR verrouillée',
    qrLockedNotice:
      'Compte lié par QR Code professeur : le nom complet et le numéro Massar sont verrouillés pour garantir l’intégrité des résultats.',
    termsTitle: 'Accepter les Conditions d’Utilisation (v2)',
    termsSubtitle:
      'Avant d’accéder à votre espace élève, veuillez lire et accepter les conditions d’utilisation et la politique de confidentialité.',
    termsCheckbox: 'J’ai lu et j’accepte les Conditions d’Utilisation et la Politique de Confidentialité (v2)',
    termsContinue: 'Accepter et continuer',
    termsPrivacyLink: 'Consulter la Politique de Confidentialité',
    teacherPendingTitle: 'Compte professeur en attente de validation',
    teacherPendingBody:
      'Votre demande est en attente de validation par l’administrateur pour accéder à votre espace professeur.',
    teacherRejectedTitle: 'Inscription professeur non retenue',
    teacherRejectedBody: 'Votre demande d’inscription professeur a été refusée par l’administrateur.',
    refreshStatus: 'Actualiser mon statut',
    qrJoinTitle: 'Rejoindre votre classe SVT',
    qrJoinDesc: 'Scannez le QR Code fourni par votre professeur ou saisissez le code de la classe.',
    qrScanBtn: 'Scanner le QR Code',
    qrPickGallery: 'Importer une image QR',
    qrManualLabel: 'Ou saisir le code de la classe',
    qrJoinSubmit: 'Valider le code',
    qrInvalidCode: 'Code QR introuvable. Vérifiez auprès de votre professeur.',
    qrNotFoundInImage: 'Aucun QR Code détecté dans cette image.',
    pendingTitle: 'Inscription en attente',
    pendingDesc: 'Votre demande pour rejoindre cette classe a été transmise au professeur pour validation.',
    rejectedTitle: 'Demande refusée',
    rejectedDesc: 'Votre demande d’inscription n’a pas été validée. Scannez un autre code.',
    changeClassCode: 'Scanner un autre QR Code',
    deviceLockedTitle: 'Compte lié à un autre appareil',
    deviceLockedBody:
      'Par sécurité, ce compte élève est lié à un appareil autorisé. Demandez à votre professeur de délier votre appareil depuis la liste de classe.',

    tabExercises: 'Exercices',
    tabLab: 'Labo SVT',
    tabResources: 'Cours',
    tabAnnouncements: 'Annonces',
    tabMessaging: 'Messagerie',
    tabProfile: 'Profil',
    tabDashboard: 'Bord',
    tabClasses: 'Classes',

    iosInstallTitle: 'Installer BioTerre SVT sur iPhone',
    iosInstallSteps: 'Dans Safari : touchez Partager (⎋) puis « Sur l’écran d’accueil ».',

    notificationsTitle: 'Notifications',
    notificationsEmpty: 'Aucune notification.',
    markAllRead: 'Tout marquer comme lu',

    todoFilter: 'À faire',
    doneFilter: 'Terminés',
    allFilter: 'Tous',
    quizStart: 'Commencer',
    quizViewResult: 'Voir la correction',
    quizPendingTag: 'À faire',
    quizCompletedTag: 'Terminé',
    quizEmpty: 'Aucun exercice QCM disponible pour le moment.',
    quizNext: 'Suivant',
    quizPrevious: 'Précédent',
    quizSubmit: 'Valider mes réponses',
    quizResultTitle: 'Résultat & Correction',
    quizExplanation: 'Explication scientifique :',
    quizCreate: 'Créer un QCM',
    quizViewResultsBtn: 'Résultats par élève (%)',

    typeVideo: 'Vidéo',
    typePdf: 'PDF',
    typeWord: 'Word',
    typeImage: 'Image',
    resourcesEmpty: 'Aucun document ou vidéo partagé pour le moment.',
    watchVideo: 'Regarder',
    openDocument: 'Ouvrir',
    videoNotStarted: 'Non vue (0%)',
    videoPartial: 'En cours',
    videoCompleted: 'Vue complètement',
    fileOpened: 'Ouvert ✓',
    fileNotOpened: 'Non ouvert',
    downloadFile: 'Télécharger',
    publishResource: 'Publier un cours',

    announcementNew: 'Nouvelle annonce',
    announcementPublish: 'Publier l’annonce',
    announcementsEmpty: 'Aucune annonce publiée pour cette classe.',
    messagingStudentHeader: 'Messagerie privée avec votre Professeur',
    messagingStudentNotice: 'Canal privé Élève ↔ Professeur. Aucun autre élève ne peut voir vos échanges.',
    messagingTeacherInbox: 'Boîte de réception privée',
    messagingInputHint: 'Écrire un message privé...',
    messagingSend: 'Envoyer',
    messagingEmpty: 'Aucun message pour le moment. Posez votre question en SVT !',

    personalInfo: 'Données personnelles',
    editProfile: 'Modifier le profil',
    changePhoto: 'Changer la photo',
    changePasswordOptional: 'Nouveau mot de passe (optionnel)',
    badgesTitle: 'Badges & Récompenses SVT',
    themeSectionTitle: 'Thème et apparence',
    themeModeLabel: 'Mode d’affichage',
    themeSystem: 'Système',
    themeLight: 'Clair',
    themeDark: 'Sombre',
    paletteLabel: 'Palette de couleurs',
    paletteEmerald: 'Émeraude SVT',
    paletteOcean: 'Océan Minéral',
    paletteEarth: 'Terre & Géologie',
    paletteBotanical: 'Violet Botanique',
    pinTitle: 'Code PIN de sécurité local (4–6 chiffres)',
    pinEnabledDesc: 'Activé — Requis à l’ouverture de cette session sur cet appareil.',
    pinDisabledDesc: 'Désactivé — Ajoutez un code PIN pour protéger votre compte localement.',
    pinConfigure: 'Configurer le PIN',
    pinManage: 'Modifier / Désactiver',
    pinUnlockTitle: 'Sécurité renforcée BioTerre',
    pinUnlockBtn: 'Déverrouiller',
    pinDisableBtn: 'Désactiver le code PIN',
    pinWrongAttempts: 'Code PIN incorrect',
    pinLockedSeconds: 'Trop d’essais. Réessayez dans',

    dashboardTitle: 'Tableau de bord Professeur',
    dashboardSubtitle: 'Pilotage des classes, suivi des élèves et résultats QCM en temps réel',
    overviewTab: 'Vue générale',
    resultsTab: 'Résultats QCM (%)',
    kpiClasses: 'Classes',
    kpiStudents: 'Élèves validés',
    kpiPending: 'en attente',
    kpiQcmAvg: 'Moyenne QCM',
    kpiResourcesVideo: 'Ressources & Vidéo',
    classBreakdown: 'Synthèse par classe',
    qcmResultsTitle: 'Résultats QCM par classe et élève',
    qcmResultsDesc: 'Consultez le détail de chaque exercice et le pourcentage cumulé de chaque élève.',
    qcmSearchHint: 'Rechercher un élève ou un code Massar...',
    sortByPct: 'Tri : % ↓',
    sortByName: 'Tri : Nom A-Z',
    notTaken: 'Non passé',
    createClassTitle: 'Créer une classe SVT',
    pendingEnrollments: 'Demandes en attente',
    approveAll: 'Tout valider',
    approve: 'Valider',
    reject: 'Refuser',
    approvedStudents: 'Élèves inscrits',
    unlinkDevice: 'Délier appareil',
    rosterTitle: 'Liste officielle & Fiches QR',
    importExcelCsv: 'Importer Excel / CSV',
    addStudentManual: 'Ajouter élève',
    printCards: 'Imprimer fiches QR',
    labDomainsVisibility: 'Thèmes Labo SVT visibles',
    adminTeacherApprovals: 'Validation des professeurs (Admin)',

    legalTitle: 'Mentions légales',
    legalBody:
      'Application pédagogique BioTerre SVT (Web PWA & Android).\nAnnée de création : 2026\nCréateur et éditeur : Ayman BIROUK (biroukayman@gmail.com)\nTous droits réservés — Toute copie, modification, usurpation ou redistribution non autorisée est strictement interdite.',
    privacyTitle: 'Politique de Confidentialité (v2)',
    privacyBody:
      'Dernière mise à jour : 2026 (v2)\n\n1. Responsable : BioTerre SVT, créé par Ayman BIROUK (biroukayman@gmail.com).\n2. Données collectées : identité scolaire (nom complet, rôle, numéro Massar verrouillé après inscription QR, e-mail @taalim.ma ou professionnel, établissement), notes QCM chronométrés, pourcentage vidéo, messages privés et photo de profil.\n3. Caméra : utilisée localement uniquement pour scanner le QR Code de classe. Aucune photo n’est stockée sans action de l’utilisateur.\n4. Sécurité : verrouillage QR (nom/Massar), liaison appareil unique, code PIN local et règles Firestore par classe. Aucune publicité ni vente de données.',
    termsModalTitle: 'Conditions d’Utilisation (v2)',
    termsBody:
      'Dernière mise à jour : 2026 (v2)\n\n1. Objet : L’usage de BioTerre SVT vaut acceptation pleine des présentes conditions.\n2. Comptes & Intégrité Massar : Tout élève inscrit par QR Code professeur voit son nom complet et son numéro Massar verrouillés. Toute usurpation d’identité Massar ou tentative de fraude aux QCM est interdite.\n3. Protection anti-piratage : Toute tentative de contournement des règles Firebase, d’extraction massive, d’injection ou de clonage entraîne la suspension immédiate du compte et expose son auteur aux sanctions prévues.\n4. Limitation de responsabilité : Le créateur (Ayman BIROUK) fournit la plateforme à des fins strictement pédagogiques et ne saurait être tenu responsable des usages abusifs ou contenus tiers.\n5. Contact : biroukayman@gmail.com.',
  },
  ar: {
    appName: 'BioTerre SVT',
    appSubtitle: 'علوم الحياة والأرض',
    welcomeTitle: 'مرحباً بكم في BioTerre SVT',
    welcomeDesc:
      'فضاؤكم التربوي للدروس، تمارين QCM المؤقتة، المختبر التفاعلي SVT، تتبع الفيديو وشارات التفوق.',
    whoAreYou: 'من أنت؟',
    quickQrTitle: 'الانضمام مباشرة عبر رمز QR',
    quickQrSub: 'تسجيل تلقائي وفوري داخل قسمك دون انتظار',
    studentRoleBtn: 'أنا تلميذ(ة)',
    studentRoleSub: 'تسجيل الدخول برقم مسار أو البريد @taalim.ma',
    teacherRoleBtn: 'أنا أستاذ(ة)',
    teacherRoleSub: 'فضاء الأستاذ بالبريد المهني أو Google',
    guestModeBtn: 'وضع الضيف (معاينة)',
    guestModeSub: 'اكتشاف واجهة SVT والمختبر للقراءة فقط',
    guestBannerTitle: 'وضع الضيف • معاينة تجريبية',
    guestBannerDesc: 'للقراءة فقط: لا يمكن حفظ النتائج أو إرسال الرسائل.',
    guestExit: 'خروج',
    guestBlockedMsg: 'وضع الضيف للقراءة فقط: سجل الدخول لحفظ تقدمك.',
    changeRole: 'تغيير الصفة',
    signIn: 'تسجيل الدخول',
    register: 'إنشاء حساب',
    signOut: 'تسجيل الخروج',
    cancel: 'إلغاء',
    save: 'حفظ',
    close: 'إغلاق',
    delete: 'حذف',
    search: 'بحث...',
    loading: 'جارٍ التحميل...',
    massarLabel: 'رقم مسار',
    massarOrEmailLabel: 'رقم مسار (أو البريد الإلكتروني)',
    massarOptional: 'رقم مسار (اختياري)',
    emailProLabel: 'البريد المهني',
    recoveryEmailLabel: 'بريد استعادة الحساب',
    taalimHint: 'افتراضياً: numeromassar@taalim.ma',
    passwordLabel: 'كلمة المرور',
    confirmPasswordLabel: 'تأكيد كلمة المرور',
    fullNameLabel: 'الاسم الكامل',
    phoneOptional: 'رقم الهاتف (اختياري)',
    phoneRequired: 'رقم الهاتف *',
    establishmentRequired: 'المؤسسة التعليمية *',
    birthDateLabel: 'تاريخ الازدياد',
    forgotPassword: 'نسيت كلمة المرور؟',
    forgotPasswordDescStudent:
      'أدخل بريد الاستعادة المسجل لإرسال رابط إعادة التعيين، أو اطلب كلمة مرور مؤقتة من أستاذك.',
    forgotPasswordDescTeacher: 'أدخل بريدك المهني لاستلام رابط آمن لتغيير كلمة المرور.',
    sendResetLink: 'إرسال رابط الاستعادة',
    resetEmailSent: 'تم إرسال بريد إعادة تعيين كلمة المرور.',
    googleSignIn: 'المتابعة عبر Google',
    teacherApprovalNotice: 'يتطلب حساب الأستاذ مصادقة المشرف قبل إنشاء الأقسام.',
    firebaseMissingTitle: 'إعدادات Firebase مطلوبة',
    firebaseMissingBody:
      'أضف متغيرات VITE_FIREBASE_API_KEY و VITE_FIREBASE_APP_ID لتفعيل المزامنة السحابية. وضع الضيف والمختبر متاحان.',

    setupTitle: 'إتمام التسجيل',
    setupSubStudent: 'أكمل معلوماتك للالتحاق بقسم علوم الحياة والأرض.',
    setupSubTeacher: 'أكد معلوماتك المهنية لتدبير أقسامك.',
    continueBtn: 'متابعة',
    qrLockedBadge: 'الهوية مقفلة برمز QR',
    qrLockedNotice:
      'تم التسجيل عبر رمز QR الأستاذ: الاسم الكامل ورقم مسار مقفلان ولا يمكن تعديلهما لضمان مصداقية النقط.',
    termsTitle: 'قبول شروط الاستخدام (v2)',
    termsSubtitle: 'قبل الولوج إلى فضاء التلميذ، يرجى قراءة شروط الاستخدام وسياسة الخصوصية والموافقة عليها.',
    termsCheckbox: 'لقد قرأت وأوافق على شروط الاستخدام وسياسة الخصوصية (v2)',
    termsContinue: 'موافق ومتابعة',
    termsPrivacyLink: 'الاطلاع على سياسة الخصوصية',
    teacherPendingTitle: 'حساب الأستاذ في انتظار المصادقة',
    teacherPendingBody: 'طلبك قيد المراجعة من طرف المشرف لتفعيل فضاء الأستاذ.',
    teacherRejectedTitle: 'تم رفض طلب الأستاذ',
    teacherRejectedBody: 'لم تتم الموافقة على طلب تسجيل الأستاذ.',
    refreshStatus: 'تحديث الحالة',
    qrJoinTitle: 'الانضمام إلى قسمك SVT',
    qrJoinDesc: 'امسح رمز QR الخاص بأستاذك أو أدخل رمز القسم يدوياً.',
    qrScanBtn: 'مسح رمز QR',
    qrPickGallery: 'استيراد صورة QR',
    qrManualLabel: 'أو أدخل رمز القسم',
    qrJoinSubmit: 'تأكيد الرمز',
    qrInvalidCode: 'رمز QR غير صحيح. تأكد من أستاذك.',
    qrNotFoundInImage: 'لم يتم العثور على رمز QR في هذه الصورة.',
    pendingTitle: 'التسجيل قيد الانتظار',
    pendingDesc: 'تم إرسال طلب انضمامك إلى الأستاذ للموافقة عليه.',
    rejectedTitle: 'تم رفض الطلب',
    rejectedDesc: 'لم تتم الموافقة على انضمامك. يمكنك مسح رمز آخر.',
    changeClassCode: 'مسح رمز QR آخر',
    deviceLockedTitle: 'الحساب مرتبط بجهاز آخر',
    deviceLockedBody:
      'لأسباب أمنية، هذا الحساب مرتبط بجهاز آخر. اطلب من أستاذك فك ارتباط الجهاز من قائمة القسم.',

    tabExercises: 'التمارين',
    tabLab: 'مختبر SVT',
    tabResources: 'الدروس',
    tabAnnouncements: 'الإعلانات',
    tabMessaging: 'المراسلة',
    tabProfile: 'الملف',
    tabDashboard: 'القيادة',
    tabClasses: 'الأقسام',

    iosInstallTitle: 'تثبيت BioTerre SVT على iPhone',
    iosInstallSteps: 'في Safari: اضغط على مشاركة (⎋) ثم « إضافة إلى الشاشة الرئيسية ».',

    notificationsTitle: 'الإشعارات',
    notificationsEmpty: 'لا توجد إشعارات.',
    markAllRead: 'تحديد الكل كمقروء',

    todoFilter: 'للإنجاز',
    doneFilter: 'منجزة',
    allFilter: 'الكل',
    quizStart: 'ابدأ',
    quizViewResult: 'عرض التصحيح',
    quizPendingTag: 'قيد الإنجاز',
    quizCompletedTag: 'منجز',
    quizEmpty: 'لا توجد تمارين QCM متاحة حالياً.',
    quizNext: 'التالي',
    quizPrevious: 'السابق',
    quizSubmit: 'إرسال الإجابات',
    quizResultTitle: 'النتيجة والتصحيح',
    quizExplanation: 'التفسير العلمي:',
    quizCreate: 'إنشاء QCM',
    quizViewResultsBtn: 'نتائج التلاميذ (%)',

    typeVideo: 'فيديو',
    typePdf: 'PDF',
    typeWord: 'Word',
    typeImage: 'صورة',
    resourcesEmpty: 'لا توجد دروس أو فيديوهات منشورة حالياً.',
    watchVideo: 'مشاهدة',
    openDocument: 'فتح',
    videoNotStarted: 'غير مشاهد (0%)',
    videoPartial: 'قيد المشاهدة',
    videoCompleted: 'مشاهد بالكامل',
    fileOpened: 'تم الفتح ✓',
    fileNotOpened: 'لم يفتح',
    downloadFile: 'تحميل',
    publishResource: 'نشر درس',

    announcementNew: 'إعلان جديد',
    announcementPublish: 'نشر الإعلان',
    announcementsEmpty: 'لا توجد إعلانات في هذا القسم.',
    messagingStudentHeader: 'المراسلة الخاصة مع أستاذك',
    messagingStudentNotice: 'قناة سرية بين التلميذ والأستاذ. لا يمكن لأي تلميذ آخر رؤية محادثتكم.',
    messagingTeacherInbox: 'صندوق الرسائل الخاصة',
    messagingInputHint: 'اكتب رسالة خاصة...',
    messagingSend: 'إرسال',
    messagingEmpty: 'لا توجد رسائل بعد. اطرح سؤالك في SVT!',

    personalInfo: 'المعلومات الشخصية',
    editProfile: 'تعديل الملف',
    changePhoto: 'تغيير الصورة',
    changePasswordOptional: 'كلمة مرور جديدة (اختياري)',
    badgesTitle: 'الشارات والمستوى SVT',
    themeSectionTitle: 'المظهر والألوان',
    themeModeLabel: 'نمط العرض',
    themeSystem: 'النظام',
    themeLight: 'فاتح',
    themeDark: 'داكن',
    paletteLabel: 'لوحة الألوان',
    paletteEmerald: 'زمردي SVT',
    paletteOcean: 'أزرق معدني',
    paletteEarth: 'ترابي وجيولوجي',
    paletteBotanical: 'بنفسجي نباتي',
    pinTitle: 'رمز PIN الأمني المحلي (4-6 أرقام)',
    pinEnabledDesc: 'مفعل — مطلوب عند فتح التطبيق على هذا الجهاز.',
    pinDisabledDesc: 'معطل — أضف رمز PIN لحماية حسابك محلياً.',
    pinConfigure: 'إعداد رمز PIN',
    pinManage: 'تعديل / تعطيل',
    pinUnlockTitle: 'الحماية المعززة BioTerre',
    pinUnlockBtn: 'فتح القفل',
    pinDisableBtn: 'تعطيل رمز PIN',
    pinWrongAttempts: 'رمز PIN غير صحيح',
    pinLockedSeconds: 'محاولات كثيرة. أعد المحاولة بعد',

    dashboardTitle: 'لوحة قيادة الأستاذ',
    dashboardSubtitle: 'تدبير الأقسام، تتبع التلاميذ ونتائج QCM في الوقت الحقيقي',
    overviewTab: 'نظرة عامة',
    resultsTab: 'نتائج QCM (%)',
    kpiClasses: 'الأقسام',
    kpiStudents: 'التلاميذ المقبولون',
    kpiPending: 'في الانتظار',
    kpiQcmAvg: 'معدل QCM',
    kpiResourcesVideo: 'الدروس والفيديو',
    classBreakdown: 'ملخص حسب القسم',
    qcmResultsTitle: 'نتائج QCM حسب القسم والتلميذ',
    qcmResultsDesc: 'تتبع نقطة كل تمرين والنسبة المئوية الإجمالية لكل تلميذ.',
    qcmSearchHint: 'ابحث بالاسم أو رقم مسار...',
    sortByPct: 'ترتيب: النسبة ↓',
    sortByName: 'ترتيب: الاسم',
    notTaken: 'لم ينجز بعد',
    createClassTitle: 'إنشاء قسم SVT',
    pendingEnrollments: 'طلبات الانضمام المعلقة',
    approveAll: 'قبول الكل',
    approve: 'قبول',
    reject: 'رفض',
    approvedStudents: 'التلاميذ المسجلون',
    unlinkDevice: 'فك ارتباط الجهاز',
    rosterTitle: 'اللائحة الرسمية وبطاقات QR',
    importExcelCsv: 'استيراد Excel / CSV',
    addStudentManual: 'إضافة تلميذ',
    printCards: 'طباعة بطاقات QR',
    labDomainsVisibility: 'محاور مختبر SVT الظاهرة',
    adminTeacherApprovals: 'المصادقة على الأساتذة (المشرف)',

    legalTitle: 'بيانات قانونية',
    legalBody:
      'تطبيق BioTerre SVT التعليمي (الويب و Android).\nسنة الإنشاء: 2026\nالمبتكر والناشر: أيمن بيروك (biroukayman@gmail.com)\nجميع الحقوق محفوظة — يمنع أي نسخ أو انتحال أو تعديل غير مصرح به.',
    privacyTitle: 'سياسة الخصوصية وحماية المعطيات (v2)',
    privacyBody:
      'آخر تحديث: 2026 (v2)\n\n1. المسؤول: BioTerre SVT من إنشاء أيمن بيروك (biroukayman@gmail.com).\n2. المعطيات: الاسم الكامل، الصفة، رقم مسار (مقفل بعد مسح QR)، البريد المهني أو @taalim.ma، نتائج QCM، تتبع الفيديو والمراسلة الخاصة.\n3. الكاميرا: تستعمل محلياً لمسح رمز QR فقط دون تخزين أي صور.\n4. الأمان: قفل الهوية عبر QR، ربط جهاز واحد، رمز PIN محلي، ولا توجد أي إعلانات.',
    termsModalTitle: 'شروط الاستخدام (v2)',
    termsBody:
      'آخر تحديث: 2026 (v2)\n\n1. القبول: استخدام التطبيق يعني قبول هذه الشروط بالكامل.\n2. نزاهة مسار: يتم قفل الاسم الكامل ورقم مسار تلقائياً عند التسجيل برمز QR الأستاذ، ويمنع انتحال الهوية.\n3. الحماية من القرصنة: يمنع محاولة اختراق قواعد Firebase أو استخراج البيانات أو الغش، ويعاقب عليه بتوقيف الحساب.\n4. إخلاء المسؤولية: المنصة موجهة لأغراض تعليمية ولا يتحمل المبتكر مسؤولية سوء الاستخدام من طرف المستخدمين.',
  },
  en: {
    appName: 'BioTerre SVT',
    appSubtitle: 'Life and Earth Sciences',
    welcomeTitle: 'Welcome to BioTerre SVT',
    welcomeDesc:
      'Your educational space for lessons, timed QCM quizzes, interactive SVT Lab, video tracking, and achievement badges.',
    whoAreYou: 'Who are you?',
    quickQrTitle: 'Join directly via QR Code',
    quickQrSub: 'Instant automatic class enrollment without waiting',
    studentRoleBtn: 'I am a Student',
    studentRoleSub: 'Sign in with Massar Number or @taalim.ma email',
    teacherRoleBtn: 'I am a Teacher',
    teacherRoleSub: 'Teacher workspace with professional email or Google',
    guestModeBtn: 'Guest Mode (Demo Preview)',
    guestModeSub: 'Explore the SVT interface & Lab in read-only mode',
    guestBannerTitle: 'Guest Mode • Demo Preview',
    guestBannerDesc: 'Read-only mode: saving or sending is disabled.',
    guestExit: 'Exit',
    guestBlockedMsg: 'Guest Mode is read-only: sign in to save your progress.',
    changeRole: 'Change role',
    signIn: 'Sign in',
    register: 'Create account',
    signOut: 'Sign out',
    cancel: 'Cancel',
    save: 'Save',
    close: 'Close',
    delete: 'Delete',
    search: 'Search...',
    loading: 'Loading...',
    massarLabel: 'Massar Number',
    massarOrEmailLabel: 'Massar Number (or email)',
    massarOptional: 'Massar Number (optional)',
    emailProLabel: 'Professional Email',
    recoveryEmailLabel: 'Recovery Email',
    taalimHint: 'Default: massarnumber@taalim.ma',
    passwordLabel: 'Password',
    confirmPasswordLabel: 'Confirm Password',
    fullNameLabel: 'Full Name',
    phoneOptional: 'Phone (optional)',
    phoneRequired: 'Phone *',
    establishmentRequired: 'School / Establishment *',
    birthDateLabel: 'Date of Birth',
    forgotPassword: 'Forgot password?',
    forgotPasswordDescStudent:
      'Enter your recovery email to receive a reset link, or ask your teacher for a new provisional password.',
    forgotPasswordDescTeacher: 'Enter your professional email to receive a secure password reset link.',
    sendResetLink: 'Send Reset Email',
    resetEmailSent: 'Password reset email sent.',
    googleSignIn: 'Continue with Google',
    teacherApprovalNotice:
      'Teacher access requires administrator approval before creating classes.',
    firebaseMissingTitle: 'Firebase Web Configuration Required',
    firebaseMissingBody:
      'Provide VITE_FIREBASE_API_KEY and VITE_FIREBASE_APP_ID environment variables to enable live cloud sync. Guest Mode and SVT Lab remain available.',

    setupTitle: 'Complete Registration',
    setupSubStudent: 'Fill in your details to join your SVT class.',
    setupSubTeacher: 'Confirm your professional profile to manage your classes.',
    continueBtn: 'Continue',
    qrLockedBadge: 'QR Identity Locked',
    qrLockedNotice:
      'Enrolled via teacher QR Code: Full Name and Massar Number are locked to protect grade integrity.',
    termsTitle: 'Accept Terms of Use (v2)',
    termsSubtitle:
      'Before entering your student space, please read and accept the Terms of Use and Privacy Policy.',
    termsCheckbox: 'I have read and accept the Terms of Use and Privacy Policy (v2)',
    termsContinue: 'Accept and Continue',
    termsPrivacyLink: 'Read Privacy Policy',
    teacherPendingTitle: 'Teacher Account Pending Approval',
    teacherPendingBody:
      'Your request is awaiting administrator approval before you can access the teacher workspace.',
    teacherRejectedTitle: 'Teacher Request Declined',
    teacherRejectedBody: 'Your teacher registration request was declined by the administrator.',
    refreshStatus: 'Refresh Status',
    qrJoinTitle: 'Join Your SVT Class',
    qrJoinDesc: 'Scan the QR Code provided by your teacher or enter the class code.',
    qrScanBtn: 'Scan QR Code',
    qrPickGallery: 'Import QR Image',
    qrManualLabel: 'Or enter class code',
    qrJoinSubmit: 'Join Class',
    qrInvalidCode: 'QR Code not found. Please verify with your teacher.',
    qrNotFoundInImage: 'No QR Code detected in this image.',
    pendingTitle: 'Enrollment Pending',
    pendingDesc: 'Your request to join this class has been sent to your teacher for approval.',
    rejectedTitle: 'Request Declined',
    rejectedDesc: 'Your enrollment request was declined. You can scan another class code.',
    changeClassCode: 'Scan another QR Code',
    deviceLockedTitle: 'Account Bound to Another Device',
    deviceLockedBody:
      'For security, this student account is bound to another device. Ask your teacher to unlink your device from the class list.',

    tabExercises: 'Quizzes',
    tabLab: 'SVT Lab',
    tabResources: 'Lessons',
    tabAnnouncements: 'Notices',
    tabMessaging: 'Messages',
    tabProfile: 'Profile',
    tabDashboard: 'Dashboard',
    tabClasses: 'Classes',

    iosInstallTitle: 'Install BioTerre SVT on iPhone',
    iosInstallSteps: 'In Safari: tap Share (⎋) then "Add to Home Screen".',

    notificationsTitle: 'Notifications',
    notificationsEmpty: 'No notifications.',
    markAllRead: 'Mark all read',

    todoFilter: 'To Do',
    doneFilter: 'Done',
    allFilter: 'All',
    quizStart: 'Start',
    quizViewResult: 'View Correction',
    quizPendingTag: 'To Do',
    quizCompletedTag: 'Completed',
    quizEmpty: 'No QCM quizzes available yet.',
    quizNext: 'Next',
    quizPrevious: 'Previous',
    quizSubmit: 'Submit Answers',
    quizResultTitle: 'Score & Correction',
    quizExplanation: 'Scientific explanation:',
    quizCreate: 'Create QCM',
    quizViewResultsBtn: 'Student Results (%)',

    typeVideo: 'Video',
    typePdf: 'PDF',
    typeWord: 'Word',
    typeImage: 'Image',
    resourcesEmpty: 'No lessons or videos shared yet.',
    watchVideo: 'Watch',
    openDocument: 'Open',
    videoNotStarted: 'Not watched (0%)',
    videoPartial: 'In progress',
    videoCompleted: 'Completed',
    fileOpened: 'Opened ✓',
    fileNotOpened: 'Not opened',
    downloadFile: 'Download',
    publishResource: 'Publish Lesson',

    announcementNew: 'New Announcement',
    announcementPublish: 'Publish Announcement',
    announcementsEmpty: 'No announcements published for this class.',
    messagingStudentHeader: 'Private Chat with your Teacher',
    messagingStudentNotice: 'Private Student ↔ Teacher channel. No other student can see your messages.',
    messagingTeacherInbox: 'Private Inbox',
    messagingInputHint: 'Write a private message...',
    messagingSend: 'Send',
    messagingEmpty: 'No messages yet. Ask your SVT question!',

    personalInfo: 'Personal Information',
    editProfile: 'Edit Profile',
    changePhoto: 'Change Photo',
    changePasswordOptional: 'New Password (optional)',
    badgesTitle: 'SVT Badges & XP',
    themeSectionTitle: 'Theme & Appearance',
    themeModeLabel: 'Display Mode',
    themeSystem: 'System',
    themeLight: 'Light',
    themeDark: 'Dark',
    paletteLabel: 'Color Palette',
    paletteEmerald: 'SVT Emerald',
    paletteOcean: 'Mineral Ocean',
    paletteEarth: 'Earth & Geology',
    paletteBotanical: 'Botanical Violet',
    pinTitle: 'Local Security PIN (4–6 digits)',
    pinEnabledDesc: 'Enabled — Required on startup on this device.',
    pinDisabledDesc: 'Disabled — Add a PIN code to lock access on this device.',
    pinConfigure: 'Set up PIN',
    pinManage: 'Change / Disable',
    pinUnlockTitle: 'BioTerre Security Lock',
    pinUnlockBtn: 'Unlock',
    pinDisableBtn: 'Disable PIN',
    pinWrongAttempts: 'Incorrect PIN',
    pinLockedSeconds: 'Too many attempts. Retry in',

    dashboardTitle: 'Teacher Dashboard',
    dashboardSubtitle: 'Manage classes, track students, and inspect real-time QCM scores',
    overviewTab: 'Overview',
    resultsTab: 'QCM Results (%)',
    kpiClasses: 'Classes',
    kpiStudents: 'Approved Students',
    kpiPending: 'pending',
    kpiQcmAvg: 'QCM Average',
    kpiResourcesVideo: 'Lessons & Video',
    classBreakdown: 'Class Summary',
    qcmResultsTitle: 'QCM Results by Class & Student',
    qcmResultsDesc: 'Inspect per-quiz scores and each student’s cumulative overall percentage.',
    qcmSearchHint: 'Search student name or Massar code...',
    sortByPct: 'Sort: % ↓',
    sortByName: 'Sort: Name A-Z',
    notTaken: 'Not taken',
    createClassTitle: 'Create SVT Class',
    pendingEnrollments: 'Pending Enrollments',
    approveAll: 'Approve All',
    approve: 'Approve',
    reject: 'Reject',
    approvedStudents: 'Approved Students',
    unlinkDevice: 'Unlink Device',
    rosterTitle: 'Official Roster & QR Cards',
    importExcelCsv: 'Import Excel / CSV',
    addStudentManual: 'Add Student',
    printCards: 'Print QR Cards',
    labDomainsVisibility: 'Visible SVT Lab Domains',
    adminTeacherApprovals: 'Admin Teacher Approvals',

    legalTitle: 'Legal Notice',
    legalBody:
      'BioTerre SVT Educational Platform (Web PWA & Android).\nCreated in 2026 by Ayman BIROUK (biroukayman@gmail.com).\nAll rights reserved — Unauthorized copying, impersonation, tampering, or redistribution is strictly prohibited.',
    privacyTitle: 'Privacy Policy (v2)',
    privacyBody:
      'Last updated: 2026 (v2)\n\n1. Controller: BioTerre SVT, created by Ayman BIROUK (biroukayman@gmail.com).\n2. Data processed: school identity (full name, role, QR-locked Massar number, @taalim.ma or pro email, establishment), timed QCM results, video completion, private messages, and avatar.\n3. Camera: used strictly locally to scan class QR codes.\n4. Security: QR identity lock, single-device binding, local PIN, strict Firestore class rules, zero ads or tracking.',
    termsModalTitle: 'Terms of Use (v2)',
    termsBody:
      'Last updated: 2026 (v2)\n\n1. Acceptance: Using BioTerre SVT implies full acceptance of these terms.\n2. QR Identity Integrity: Students enrolled via Teacher QR have Full Name and Massar locked. Impersonation or cheating is forbidden.\n3. Anti-Piracy & Creator Protection: Bypassing Firestore rules, scraping, cloning, or tampering results in immediate suspension and legal accountability.\n4. Disclaimer: Provided as-is for educational purposes; creator Ayman BIROUK is not liable for misuse or user-uploaded external content.',
  },
};
