import React, { useEffect, useMemo, useState } from 'react';
import { onAuthStateChanged, User } from 'firebase/auth';
import {
  Bell,
  BookOpen,
  CheckCircle2,
  FlaskConical,
  LayoutDashboard,
  Layers,
  Megaphone,
  MessageSquare,
  User as UserIcon,
} from 'lucide-react';
import {
  ALL_SVT_DOMAINS,
  Announcement,
  AppLanguage,
  AppPalette,
  CourseResource,
  Enrollment,
  PrivateMessage,
  Quiz,
  QuizAttempt,
  RosterStudent,
  StudentNotification,
  StudentTab,
  SvtClass,
  TeacherTab,
  ThemeMode,
  UserProfile,
  VideoTracking,
} from './types/svt';
import { auth, isFirebaseConfigured } from './lib/firebase';
import { getOrCreateWebDeviceId, OWNER_ADMIN_EMAIL } from './lib/authHelpers';
import { loadSavedLanguage, saveLanguage, TRANSLATIONS } from './lib/i18n';
import {
  applyThemeToDocument,
  loadSavedPalette,
  loadSavedThemeMode,
  saveThemePreferences,
} from './lib/theme';
import { isPinEnabledForUser } from './lib/pinSecurity';
import {
  GUEST_ANNOUNCEMENTS,
  GUEST_ATTEMPTS,
  GUEST_CLASS,
  GUEST_ENROLLMENT,
  GUEST_MESSAGES,
  GUEST_NOTIFICATIONS,
  GUEST_PROFILE,
  GUEST_QUIZZES,
  GUEST_RESOURCES,
  GUEST_VIDEO_TRACKING,
} from './lib/guestDemoData';
import {
  bindCurrentWebDeviceIfEmpty,
  markAllNotificationsRead,
  subscribeAdminTeacherRequests,
  subscribeAnnouncements,
  subscribeClassQuizzes,
  subscribePrivateMessages,
  subscribeQuizAttempts,
  subscribeResources,
  subscribeStudentClass,
  subscribeStudentEnrollment,
  subscribeStudentNotifications,
  subscribeTeacherClasses,
  subscribeTeacherEnrollments,
  subscribeTeacherRoster,
  subscribeUserProfile,
  subscribeVideoTracking,
} from './lib/repository';
import { AuthGateway } from './screens/AuthGateway';
import {
  DeviceLockedScreen,
  PendingEnrollmentScreen,
  ProfileSetupScreen,
  QrJoinClassScreen,
  TeacherPendingScreen,
  TermsAcceptanceScreen,
} from './screens/OnboardingGates';
import { IosInstallBanner } from './components/IosInstallBanner';
import { NotificationDrawer } from './components/NotificationDrawer';
import { PinLockOverlay } from './components/PinLockOverlay';
import { ExercisesTab } from './tabs/ExercisesTab';
import { SvtLabTab } from './tabs/SvtLabTab';
import { ResourcesTab } from './tabs/ResourcesTab';
import { AnnouncementsTab } from './tabs/AnnouncementsTab';
import { MessagingTab } from './tabs/MessagingTab';
import { ProfileTab } from './tabs/ProfileTab';
import { TeacherDashboardTab } from './tabs/TeacherDashboardTab';
import { TeacherClassesTab } from './tabs/TeacherClassesTab';

export const App: React.FC = () => {
  const [lang, setLang] = useState<AppLanguage>(() => {
    const saved = loadSavedLanguage();
    saveLanguage(saved);
    return saved;
  });
  const [themeMode, setThemeMode] = useState<ThemeMode>(() => loadSavedThemeMode());
  const [palette, setPalette] = useState<AppPalette>(() => loadSavedPalette());

  const [authReady, setAuthReady] = useState(!isFirebaseConfigured);
  const [fbUser, setFbUser] = useState<User | null>(null);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [profileLoaded, setProfileLoaded] = useState(false);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [syncError, setSyncError] = useState<string | null>(null);
  const [retryTick, setRetryTick] = useState(0);

  const [isGuestMode, setIsGuestMode] = useState(false);
  const [guestBannerToast, setGuestBannerToast] = useState<string | null>(null);
  const [pinUnlocked, setPinUnlocked] = useState(false);
  const [scanOtherClass, setScanOtherClass] = useState(false);

  // Live Firestore states
  const [enrollment, setEnrollment] = useState<Enrollment | null>(null);
  const [enrollmentLoaded, setEnrollmentLoaded] = useState(false);
  const [studentClass, setStudentClass] = useState<SvtClass | null>(null);

  const [teacherClasses, setTeacherClasses] = useState<SvtClass[]>([]);
  const [selectedClassId, setSelectedClassId] = useState<string | null>(null);
  const [teacherEnrollments, setTeacherEnrollments] = useState<Enrollment[]>([]);
  const [teacherRoster, setTeacherRoster] = useState<RosterStudent[]>([]);
  const [adminTeachers, setAdminTeachers] = useState<UserProfile[]>([]);

  const [quizzes, setQuizzes] = useState<Quiz[]>([]);
  const [attempts, setAttempts] = useState<QuizAttempt[]>([]);
  const [resources, setResources] = useState<CourseResource[]>([]);
  const [videoTracking, setVideoTracking] = useState<VideoTracking[]>([]);
  const [announcements, setAnnouncements] = useState<Announcement[]>([]);
  const [messages, setMessages] = useState<PrivateMessage[]>([]);
  const [notifications, setNotifications] = useState<StudentNotification[]>([]);

  // Navigation
  const [studentTab, setStudentTab] = useState<StudentTab>('exercises');
  const [teacherTab, setTeacherTab] = useState<TeacherTab>('dashboard');
  const [dashboardSection, setDashboardSection] = useState<'overview' | 'results'>('overview');
  const [notifOpen, setNotifOpen] = useState(false);

  const tr = TRANSLATIONS[lang];

  // Apply theme & listen to system color scheme
  useEffect(() => {
    applyThemeToDocument(themeMode, palette);
    saveThemePreferences(themeMode, palette);
    const mq = window.matchMedia?.('(prefers-color-scheme: dark)');
    if (!mq || themeMode !== 'system') return;
    const listener = () => applyThemeToDocument('system', palette);
    mq.addEventListener?.('change', listener);
    return () => mq.removeEventListener?.('change', listener);
  }, [themeMode, palette]);

  // Language persistence
  const handleChangeLang = (next: AppLanguage) => {
    setLang(next);
    saveLanguage(next);
  };

  // Firebase Auth listener
  useEffect(() => {
    if (!auth) {
      setAuthReady(true);
      return;
    }
    return onAuthStateChanged(auth, (user) => {
      setFbUser(user);
      setAuthReady(true);
      setPinUnlocked(false);
      setScanOtherClass(false);
      if (!user) {
        setProfile(null);
        setProfileLoaded(false);
        setProfileError(null);
        setSyncError(null);
        setEnrollment(null);
        setEnrollmentLoaded(false);
        setStudentClass(null);
        setTeacherClasses([]);
        setQuizzes([]);
        setAttempts([]);
        setResources([]);
        setVideoTracking([]);
        setAnnouncements([]);
        setMessages([]);
        setNotifications([]);
      }
    });
  }, []);

  const reportSyncError = (label: string) => (err: Error) => {
    console.error(`Firestore listener error (${label}):`, err);
    setSyncError((prev) => prev || `${label} : ${err.message}`);
  };

  // Subscribe User Profile
  useEffect(() => {
    if (!fbUser || isGuestMode) return;
    setProfileLoaded(false);
    setProfileError(null);
    return subscribeUserProfile(
      fbUser.uid,
      (p) => {
        setProfile(p);
        setProfileLoaded(true);
        setProfileError(null);
        if (p && p.role === 'student' && !p.boundDeviceId) {
          bindCurrentWebDeviceIfEmpty(p).catch(() => {});
        }
      },
      (err) => {
        console.error('Profile listener error:', err);
        setProfileError(err.message);
        setProfileLoaded(true);
      }
    );
  }, [fbUser?.uid, isGuestMode, retryTick]);

  // Subscribe Enrollment (Student) or Classes/Enrollments/Roster (Teacher)
  useEffect(() => {
    if (!profile || isGuestMode) return;
    setSyncError(null);
    if (profile.role === 'student') {
      setEnrollmentLoaded(false);
      return subscribeStudentEnrollment(
        profile.userId,
        (enr) => {
          setEnrollment(enr);
          setEnrollmentLoaded(true);
        },
        reportSyncError('enrollment')
      );
    } else if (profile.teacherStatus === 'approved') {
      const unsubCls = subscribeTeacherClasses(
        profile.userId,
        (cls) => {
          setTeacherClasses(cls);
          setSelectedClassId((prev) =>
            prev && cls.some((c) => c.id === prev) ? prev : cls[0]?.id || null
          );
        },
        reportSyncError('classes')
      );
      const unsubEnr = subscribeTeacherEnrollments(
        profile.userId,
        setTeacherEnrollments,
        reportSyncError('enrollments')
      );
      const unsubRos = subscribeTeacherRoster(
        profile.userId,
        setTeacherRoster,
        reportSyncError('class_rosters')
      );
      const unsubAdmin =
        profile.email.toLowerCase() === OWNER_ADMIN_EMAIL
          ? subscribeAdminTeacherRequests(setAdminTeachers, reportSyncError('users'))
          : () => {};
      return () => {
        unsubCls();
        unsubEnr();
        unsubRos();
        unsubAdmin();
      };
    }
  }, [profile?.userId, profile?.role, profile?.teacherStatus, isGuestMode, retryTick]);

  // Subscribe student class doc for visibleLabDomains
  useEffect(() => {
    if (!profile || profile.role !== 'student' || !enrollment || enrollment.status !== 'approved' || isGuestMode) {
      return;
    }
    return subscribeStudentClass(
      enrollment.classId,
      setStudentClass,
      reportSyncError('student_class')
    );
  }, [profile?.userId, enrollment?.classId, enrollment?.status, isGuestMode, retryTick]);

  // Subscribe Class Content (Quizzes, Attempts, Resources, VideoTracking, Announcements, Messages, Notifications)
  useEffect(() => {
    if (!profile || isGuestMode) return;
    if (profile.role === 'student') {
      if (!enrollment || enrollment.status !== 'approved') return;
      const u1 = subscribeClassQuizzes(
        { role: 'student', userId: profile.userId, classId: enrollment.classId },
        setQuizzes,
        reportSyncError('quizzes')
      );
      const u2 = subscribeQuizAttempts(
        { role: 'student', userId: profile.userId },
        setAttempts,
        reportSyncError('quiz_attempts')
      );
      const u3 = subscribeResources(
        { role: 'student', userId: profile.userId, classId: enrollment.classId },
        setResources,
        reportSyncError('resources')
      );
      const u4 = subscribeVideoTracking(
        { role: 'student', userId: profile.userId },
        setVideoTracking,
        reportSyncError('video_tracking')
      );
      const u5 = subscribeAnnouncements(
        { role: 'student', userId: profile.userId, classId: enrollment.classId },
        setAnnouncements,
        reportSyncError('announcements')
      );
      const u6 = subscribePrivateMessages(
        { role: 'student', userId: profile.userId },
        setMessages,
        reportSyncError('private_messages')
      );
      const u7 = subscribeStudentNotifications(
        profile.userId,
        setNotifications,
        reportSyncError('notifications')
      );
      return () => {
        u1();
        u2();
        u3();
        u4();
        u5();
        u6();
        u7();
      };
    } else if (profile.teacherStatus === 'approved') {
      const u1 = subscribeClassQuizzes(
        { role: 'teacher', userId: profile.userId },
        setQuizzes,
        reportSyncError('quizzes')
      );
      const u2 = subscribeQuizAttempts(
        { role: 'teacher', userId: profile.userId },
        setAttempts,
        reportSyncError('quiz_attempts')
      );
      const u3 = subscribeResources(
        { role: 'teacher', userId: profile.userId },
        setResources,
        reportSyncError('resources')
      );
      const u4 = subscribeVideoTracking(
        { role: 'teacher', userId: profile.userId },
        setVideoTracking,
        reportSyncError('video_tracking')
      );
      const u5 = subscribeAnnouncements(
        { role: 'teacher', userId: profile.userId },
        setAnnouncements,
        reportSyncError('announcements')
      );
      const u6 = subscribePrivateMessages(
        { role: 'teacher', userId: profile.userId },
        setMessages,
        reportSyncError('private_messages')
      );
      return () => {
        u1();
        u2();
        u3();
        u4();
        u5();
        u6();
      };
    }
  }, [
    profile?.userId,
    profile?.role,
    profile?.teacherStatus,
    enrollment?.classId,
    enrollment?.status,
    isGuestMode,
    retryTick,
  ]);

  const showGuestBlocked = () => {
    setGuestBannerToast(tr.guestBlockedMsg);
    setTimeout(() => setGuestBannerToast(null), 3200);
  };

  // Effective active data (Live vs Guest Demo)
  const effectiveProfile = isGuestMode ? GUEST_PROFILE : profile;
  const effectiveEnrollment = isGuestMode ? GUEST_ENROLLMENT : enrollment;
  const effectiveQuizzes = isGuestMode ? GUEST_QUIZZES : quizzes;
  const effectiveAttempts = isGuestMode ? GUEST_ATTEMPTS : attempts;
  const effectiveResources = isGuestMode ? GUEST_RESOURCES : resources;
  const effectiveTracking = isGuestMode ? GUEST_VIDEO_TRACKING : videoTracking;
  const effectiveAnnouncements = isGuestMode ? GUEST_ANNOUNCEMENTS : announcements;
  const effectiveMessages = isGuestMode ? GUEST_MESSAGES : messages;
  const effectiveNotifications = isGuestMode ? GUEST_NOTIFICATIONS : notifications;

  const visibleLabDomains = isGuestMode
    ? GUEST_CLASS.visibleLabDomains
    : effectiveProfile?.role === 'teacher'
    ? ALL_SVT_DOMAINS
    : studentClass?.visibleLabDomains || ALL_SVT_DOMAINS;

  const unreadNotifCount = effectiveNotifications.filter((n) => !n.isRead).length;
  const unreadMsgCount = useMemo(() => {
    if (!effectiveProfile) return 0;
    return effectiveMessages.filter(
      (m) => !m.isRead && m.senderId !== effectiveProfile.userId
    ).length;
  }, [effectiveMessages, effectiveProfile]);

  const completedVideosCount = useMemo(
    () => effectiveTracking.filter((v) => v.status === 'completed').length,
    [effectiveTracking]
  );

  // Loading Screen
  if (!authReady) {
    return (
      <div className="min-h-screen-safe flex items-center justify-center bg-svt-bg text-svt-primary font-display font-bold">
        {tr.loading}
      </div>
    );
  }

  // Unauthenticated Gateway
  if (!isGuestMode && !fbUser) {
    return (
      <AuthGateway
        lang={lang}
        onChangeLang={handleChangeLang}
        onEnterGuestMode={() => {
          setIsGuestMode(true);
          setStudentTab('exercises');
        }}
      />
    );
  }

  // Signed in -> Wait for user profile
  if (!isGuestMode && fbUser && !profileLoaded) {
    return (
      <div className="min-h-screen-safe flex items-center justify-center bg-svt-bg text-svt-primary font-display font-bold">
        {tr.loading}
      </div>
    );
  }

  // Profile Read Failure Guard (never send existing users to ProfileSetupScreen on error)
  if (!isGuestMode && fbUser && profileError && !profile) {
    return (
      <div className="min-h-screen-safe flex items-center justify-center bg-svt-bg p-4 pt-safe pb-safe">
        <div className="w-full max-w-md space-y-4 rounded-3xl border border-svt-error/30 bg-svt-surface p-6 text-center shadow-md">
          <h1 className="font-display text-lg font-bold text-svt-error">
            Impossible de charger votre profil
          </h1>
          <p className="text-xs text-svt-onSurfaceVariant">
            Vos données dans le cloud n’ont pas été supprimées, mais la lecture Firestore a échoué :
          </p>
          <div className="rounded-xl bg-svt-error/10 p-3 text-xs font-semibold text-svt-error">
            {profileError}
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => auth?.signOut()}
              className="min-h-[46px] flex-1 rounded-2xl border border-svt-outline/30 text-xs font-bold"
            >
              {tr.signOut}
            </button>
            <button
              type="button"
              onClick={() => setRetryTick((t) => t + 1)}
              className="min-h-[46px] flex-1 rounded-2xl bg-svt-primary text-xs font-bold text-svt-onPrimary"
            >
              Réessayer
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Profile Setup Gate (e.g. Google Sign-In first time)
  if (!isGuestMode && fbUser && !profile) {
    return (
      <ProfileSetupScreen
        uid={fbUser.uid}
        defaultEmail={fbUser.email || ''}
        defaultName={fbUser.displayName || ''}
        lang={lang}
      />
    );
  }

  if (!effectiveProfile) return null;

  // Live user safety & onboarding gates
  if (!isGuestMode) {
    if (effectiveProfile.role === 'teacher' && effectiveProfile.teacherStatus !== 'approved') {
      return (
        <TeacherPendingScreen
          profile={effectiveProfile}
          lang={lang}
          onRefresh={() => fbUser?.reload()}
        />
      );
    }

    if (effectiveProfile.role === 'student' && !effectiveProfile.acceptedTerms) {
      return <TermsAcceptanceScreen profile={effectiveProfile} lang={lang} />;
    }

    const currentDeviceId = getOrCreateWebDeviceId();
    if (
      effectiveProfile.role === 'student' &&
      effectiveProfile.boundDeviceId &&
      effectiveProfile.boundDeviceId !== currentDeviceId
    ) {
      return <DeviceLockedScreen profile={effectiveProfile} lang={lang} />;
    }

    if (effectiveProfile.role === 'student') {
      if (!enrollmentLoaded) {
        return (
          <div className="min-h-screen-safe flex items-center justify-center bg-svt-bg text-svt-primary font-display font-bold">
            {tr.loading}
          </div>
        );
      }
      if (!enrollment || scanOtherClass) {
        return <QrJoinClassScreen profile={effectiveProfile} lang={lang} />;
      }
      if (enrollment.status !== 'approved') {
        return (
          <PendingEnrollmentScreen
            enrollment={enrollment}
            lang={lang}
            onScanAnother={() => setScanOtherClass(true)}
          />
        );
      }
    }

    if (isPinEnabledForUser(effectiveProfile.userId) && !pinUnlocked) {
      return (
        <PinLockOverlay
          uid={effectiveProfile.userId}
          userName={effectiveProfile.fullName}
          lang={lang}
          onUnlocked={() => setPinUnlocked(true)}
          onSignOut={() => auth?.signOut()}
        />
      );
    }
  }

  const isTeacher = !isGuestMode && effectiveProfile.role === 'teacher';

  const studentNav: { id: StudentTab; label: string; icon: React.ReactNode; badge?: number }[] = [
    { id: 'exercises', label: tr.tabExercises, icon: <CheckCircle2 className="h-5 w-5" /> },
    { id: 'svt_lab', label: tr.tabLab, icon: <FlaskConical className="h-5 w-5" /> },
    { id: 'resources', label: tr.tabResources, icon: <BookOpen className="h-5 w-5" /> },
    { id: 'announcements', label: tr.tabAnnouncements, icon: <Megaphone className="h-5 w-5" /> },
    {
      id: 'messaging',
      label: tr.tabMessaging,
      icon: <MessageSquare className="h-5 w-5" />,
      badge: unreadMsgCount,
    },
    { id: 'profile', label: tr.tabProfile, icon: <UserIcon className="h-5 w-5" /> },
  ];

  const teacherNav: { id: TeacherTab; label: string; icon: React.ReactNode; badge?: number }[] = [
    { id: 'dashboard', label: tr.tabDashboard, icon: <LayoutDashboard className="h-5 w-5" /> },
    { id: 'exercises', label: tr.tabExercises, icon: <CheckCircle2 className="h-5 w-5" /> },
    { id: 'resources', label: tr.tabResources, icon: <BookOpen className="h-5 w-5" /> },
    { id: 'announcements', label: tr.tabAnnouncements, icon: <Megaphone className="h-5 w-5" /> },
    {
      id: 'messaging',
      label: tr.tabMessaging,
      icon: <MessageSquare className="h-5 w-5" />,
      badge: unreadMsgCount,
    },
    { id: 'classes', label: tr.tabClasses, icon: <Layers className="h-5 w-5" /> },
    { id: 'profile', label: tr.tabProfile, icon: <UserIcon className="h-5 w-5" /> },
  ];

  return (
    <div className="min-h-screen-safe flex flex-col bg-svt-bg text-svt-onSurface">
      {!isGuestMode && syncError && (
        <div className="bg-svt-error/15 px-4 py-2 text-xs font-semibold text-svt-error flex items-center justify-between gap-2">
          <span>Erreur de synchronisation Firestore : {syncError}</span>
          <button
            type="button"
            onClick={() => {
              setSyncError(null);
              setRetryTick((t) => t + 1);
            }}
            className="shrink-0 rounded-xl bg-svt-error px-3 py-1 text-[11px] font-bold text-white"
          >
            Réessayer
          </button>
        </div>
      )}
      {/* Top Bar with iOS Safe Area */}
      <header className="sticky top-0 z-30 border-b border-svt-outline/15 bg-svt-surface/90 pt-safe backdrop-blur-md">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-2 px-3 py-2.5 sm:px-5">
          <div className="flex min-w-0 items-center gap-2.5">
            <img
              src="/icons/icon-192.png"
              alt="BioTerre SVT"
              className="h-9 w-9 shrink-0 rounded-xl object-cover shadow-sm"
            />
            <div className="min-w-0">
              <div className="truncate font-display text-sm font-extrabold text-svt-primary">
                {tr.appName}
              </div>
              <div className="truncate text-[11px] font-semibold text-svt-onSurfaceVariant">
                {isGuestMode
                  ? GUEST_CLASS.name
                  : isTeacher
                  ? teacherClasses.find((c) => c.id === selectedClassId)?.name || 'Espace Professeur'
                  : effectiveEnrollment?.className || tr.appSubtitle}
              </div>
            </div>
          </div>

          <div className="flex items-center gap-1.5">
            {isTeacher && teacherClasses.length > 1 && (
              <select
                value={selectedClassId || ''}
                onChange={(e) => setSelectedClassId(e.target.value)}
                className="hidden max-w-[160px] rounded-xl border border-svt-outline/25 bg-svt-bg px-2.5 py-1.5 text-xs font-bold sm:block"
              >
                {teacherClasses.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            )}

            {/* FR / AR / EN Switcher */}
            <div className="flex items-center rounded-xl border border-svt-outline/20 bg-svt-bg p-0.5">
              {(['fr', 'ar', 'en'] as AppLanguage[]).map((code) => (
                <button
                  key={code}
                  type="button"
                  onClick={() => handleChangeLang(code)}
                  className={`min-h-[30px] rounded-lg px-2 text-[11px] font-bold uppercase ${
                    lang === code ? 'bg-svt-primary text-svt-onPrimary' : 'text-svt-onSurfaceVariant'
                  }`}
                >
                  {code}
                </button>
              ))}
            </div>

            {/* Notifications Bell */}
            {!isTeacher && (
              <button
                type="button"
                onClick={() => setNotifOpen(true)}
                aria-label={tr.notificationsTitle}
                className="relative flex h-10 w-10 items-center justify-center rounded-xl hover:bg-svt-surfaceVariant/70"
              >
                <Bell className="h-5 w-5" />
                {unreadNotifCount > 0 && (
                  <span className="absolute top-1.5 end-1.5 flex h-4 min-w-[16px] items-center justify-center rounded-full bg-svt-error px-1 text-[10px] font-bold text-white">
                    {unreadNotifCount}
                  </span>
                )}
              </button>
            )}
          </div>
        </div>

        {/* Guest Demo Banner */}
        {isGuestMode && (
          <div className="border-t border-svt-tertiary/30 bg-svt-tertiaryContainer/80 px-4 py-2 text-xs text-svt-onTertiaryContainer">
            <div className="mx-auto flex max-w-6xl items-center justify-between gap-2">
              <div className="truncate">
                <strong>{tr.guestBannerTitle}</strong> — {tr.guestBannerDesc}
              </div>
              <button
                type="button"
                onClick={() => setIsGuestMode(false)}
                className="shrink-0 rounded-xl bg-svt-tertiary px-3 py-1 text-xs font-bold text-white"
              >
                {tr.guestExit}
              </button>
            </div>
          </div>
        )}
      </header>

      <IosInstallBanner lang={lang} />

      {guestBannerToast && (
        <div className="fixed top-20 left-1/2 z-50 -translate-x-1/2 rounded-2xl bg-svt-onSurface px-4 py-2.5 text-xs font-bold text-svt-surface shadow-lg">
          {guestBannerToast}
        </div>
      )}

      {/* Body with Desktop Navigation Rail + Main Content */}
      <div className="mx-auto flex w-full max-w-6xl flex-1">
        {/* Desktop Rail */}
        <aside className="hidden w-56 shrink-0 flex-col gap-1 border-e border-svt-outline/15 p-3 md:flex">
          {(isTeacher ? teacherNav : studentNav).map((item) => {
            const active = isTeacher ? teacherTab === item.id : studentTab === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() =>
                  isTeacher
                    ? setTeacherTab(item.id as TeacherTab)
                    : setStudentTab(item.id as StudentTab)
                }
                className={`flex min-h-[46px] items-center justify-between rounded-2xl px-3.5 text-sm font-bold transition ${
                  active
                    ? 'bg-svt-primaryContainer text-svt-onPrimaryContainer'
                    : 'text-svt-onSurfaceVariant hover:bg-svt-surfaceVariant/50'
                }`}
              >
                <span className="flex items-center gap-2.5">
                  {item.icon}
                  <span>{item.label}</span>
                </span>
                {item.badge ? (
                  <span className="rounded-full bg-svt-error px-2 py-0.5 text-[10px] font-bold text-white">
                    {item.badge}
                  </span>
                ) : null}
              </button>
            );
          })}
        </aside>

        {/* Active Tab Content */}
        <main className="flex-1 px-3 py-4 pb-24 sm:px-5 md:pb-8">
          {isTeacher ? (
            <>
              {teacherTab === 'dashboard' && (
                <TeacherDashboardTab
                  lang={lang}
                  selectedSection={dashboardSection}
                  onSelectSection={setDashboardSection}
                  selectedClassId={selectedClassId}
                  classes={teacherClasses}
                  enrollments={teacherEnrollments}
                  roster={teacherRoster}
                  quizzes={effectiveQuizzes}
                  attempts={effectiveAttempts}
                  resources={effectiveResources}
                  videoTracking={effectiveTracking}
                  announcements={effectiveAnnouncements}
                  messages={effectiveMessages}
                />
              )}
              {teacherTab === 'exercises' && (
                <ExercisesTab
                  isTeacher
                  isGuestMode={false}
                  lang={lang}
                  profile={effectiveProfile}
                  selectedClassId={selectedClassId}
                  teacherClasses={teacherClasses}
                  enrollments={teacherEnrollments}
                  quizzes={effectiveQuizzes}
                  attempts={effectiveAttempts}
                  completedVideosCount={completedVideosCount}
                  openedResourcesCount={completedVideosCount}
                  sentMessagesCount={0}
                  onGuestBlocked={showGuestBlocked}
                  onOpenQcmResults={() => {
                    setDashboardSection('results');
                    setTeacherTab('dashboard');
                  }}
                />
              )}
              {teacherTab === 'resources' && (
                <ResourcesTab
                  isTeacher
                  isGuestMode={false}
                  lang={lang}
                  profile={effectiveProfile}
                  selectedClassId={selectedClassId}
                  teacherClasses={teacherClasses}
                  enrollments={teacherEnrollments}
                  resources={effectiveResources}
                  videoTracking={effectiveTracking}
                  onGuestBlocked={showGuestBlocked}
                />
              )}
              {teacherTab === 'announcements' && (
                <AnnouncementsTab
                  isTeacher
                  lang={lang}
                  profile={effectiveProfile}
                  selectedClassId={selectedClassId}
                  teacherClasses={teacherClasses}
                  enrollments={teacherEnrollments}
                  announcements={effectiveAnnouncements}
                />
              )}
              {teacherTab === 'messaging' && (
                <MessagingTab
                  isTeacher
                  isGuestMode={false}
                  lang={lang}
                  profile={effectiveProfile}
                  enrollment={null}
                  teacherEnrollments={teacherEnrollments}
                  messages={effectiveMessages}
                  onGuestBlocked={showGuestBlocked}
                />
              )}
              {teacherTab === 'classes' && (
                <TeacherClassesTab
                  lang={lang}
                  profile={effectiveProfile}
                  classes={teacherClasses}
                  selectedClassId={selectedClassId}
                  onSelectClassId={setSelectedClassId}
                  enrollments={teacherEnrollments}
                  roster={teacherRoster}
                  adminTeachers={adminTeachers}
                />
              )}
              {teacherTab === 'profile' && (
                <ProfileTab
                  lang={lang}
                  profile={effectiveProfile}
                  enrollment={null}
                  isGuestMode={false}
                  themeMode={themeMode}
                  palette={palette}
                  onChangeThemeMode={setThemeMode}
                  onChangePalette={setPalette}
                  onGuestBlocked={showGuestBlocked}
                  onExitGuest={() => setIsGuestMode(false)}
                />
              )}
            </>
          ) : (
            <>
              {studentTab === 'exercises' && (
                <ExercisesTab
                  isTeacher={false}
                  isGuestMode={isGuestMode}
                  lang={lang}
                  profile={effectiveProfile}
                  selectedClassId={null}
                  teacherClasses={[]}
                  enrollments={[]}
                  quizzes={effectiveQuizzes}
                  attempts={effectiveAttempts}
                  completedVideosCount={completedVideosCount}
                  openedResourcesCount={completedVideosCount}
                  sentMessagesCount={
                    effectiveMessages.filter((m) => m.senderId === effectiveProfile.userId).length
                  }
                  onGuestBlocked={showGuestBlocked}
                />
              )}
              {studentTab === 'svt_lab' && (
                <SvtLabTab lang={lang} visibleDomains={visibleLabDomains} />
              )}
              {studentTab === 'resources' && (
                <ResourcesTab
                  isTeacher={false}
                  isGuestMode={isGuestMode}
                  lang={lang}
                  profile={effectiveProfile}
                  selectedClassId={null}
                  teacherClasses={[]}
                  enrollments={[]}
                  resources={effectiveResources}
                  videoTracking={effectiveTracking}
                  onGuestBlocked={showGuestBlocked}
                />
              )}
              {studentTab === 'announcements' && (
                <AnnouncementsTab
                  isTeacher={false}
                  lang={lang}
                  profile={effectiveProfile}
                  selectedClassId={null}
                  teacherClasses={[]}
                  enrollments={[]}
                  announcements={effectiveAnnouncements}
                />
              )}
              {studentTab === 'messaging' && (
                <MessagingTab
                  isTeacher={false}
                  isGuestMode={isGuestMode}
                  lang={lang}
                  profile={effectiveProfile}
                  enrollment={effectiveEnrollment}
                  teacherEnrollments={[]}
                  messages={effectiveMessages}
                  onGuestBlocked={showGuestBlocked}
                />
              )}
              {studentTab === 'profile' && (
                <ProfileTab
                  lang={lang}
                  profile={effectiveProfile}
                  enrollment={effectiveEnrollment}
                  isGuestMode={isGuestMode}
                  themeMode={themeMode}
                  palette={palette}
                  onChangeThemeMode={setThemeMode}
                  onChangePalette={setPalette}
                  onGuestBlocked={showGuestBlocked}
                  onExitGuest={() => setIsGuestMode(false)}
                />
              )}
            </>
          )}
        </main>
      </div>

      {/* Mobile Bottom Navigation Bar with Safe Area Bottom */}
      <nav className="fixed inset-x-0 bottom-0 z-30 border-t border-svt-outline/20 bg-svt-surface/95 pb-safe backdrop-blur-md md:hidden">
        <div className="flex items-center justify-around px-1 py-1">
          {(isTeacher ? teacherNav : studentNav).map((item) => {
            const active = isTeacher ? teacherTab === item.id : studentTab === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() =>
                  isTeacher
                    ? setTeacherTab(item.id as TeacherTab)
                    : setStudentTab(item.id as StudentTab)
                }
                className={`relative flex min-h-[52px] flex-1 flex-col items-center justify-center gap-0.5 rounded-2xl px-1 text-[10px] font-bold transition ${
                  active ? 'text-svt-primary' : 'text-svt-onSurfaceVariant'
                }`}
              >
                <span
                  className={`rounded-2xl px-3 py-1 ${
                    active ? 'bg-svt-primaryContainer text-svt-onPrimaryContainer' : ''
                  }`}
                >
                  {item.icon}
                </span>
                <span className="truncate max-w-[60px]">{item.label}</span>
                {item.badge ? (
                  <span className="absolute top-1 end-2 flex h-4 min-w-[16px] items-center justify-center rounded-full bg-svt-error px-1 text-[9px] text-white">
                    {item.badge}
                  </span>
                ) : null}
              </button>
            );
          })}
        </div>
      </nav>

      <NotificationDrawer
        open={notifOpen}
        lang={lang}
        notifications={effectiveNotifications}
        onMarkAllRead={() => {
          if (!isGuestMode) markAllNotificationsRead(effectiveNotifications).catch(() => {});
        }}
        onClose={() => setNotifOpen(false)}
      />
    </div>
  );
};

export default App;
