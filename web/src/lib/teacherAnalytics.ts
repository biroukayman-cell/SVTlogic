import {
  Announcement,
  CourseResource,
  Enrollment,
  PrivateMessage,
  Quiz,
  QuizAttempt,
  RosterStudent,
  SvtClass,
  VideoTracking,
} from '../types/svt';

export interface ClassAnalyticsSummary {
  svtClass: SvtClass;
  approvedStudentsCount: number;
  quizzesCount: number;
  attemptsCount: number;
  classAveragePct: number;
  resourcesCount: number;
}

export interface TeacherDashboardOverview {
  totalClasses: number;
  totalApprovedStudents: number;
  totalPendingEnrollments: number;
  totalQuizzes: number;
  totalAttempts: number;
  globalAverageQcmPct: number;
  totalResources: number;
  videoCompletionRatePct: number;
  totalAnnouncements: number;
  unreadStudentMessages: number;
  classSummaries: ClassAnalyticsSummary[];
}

export interface QuizScoreEntry {
  quiz: Quiz;
  attempt: QuizAttempt | null;
  percentage: number | null;
}

export interface StudentQcmResultSummary {
  studentId: string;
  studentName: string;
  massarNumber: string;
  classId: string;
  className: string;
  quizzesTakenCount: number;
  totalClassQuizzesCount: number;
  totalScore: number;
  totalMaxScore: number;
  overallPercentage: number;
  perQuizResults: QuizScoreEntry[];
}

export function computeDashboardOverview(params: {
  classes: SvtClass[];
  enrollments: Enrollment[];
  quizzes: Quiz[];
  attempts: QuizAttempt[];
  resources: CourseResource[];
  videoTrackings: VideoTracking[];
  announcements: Announcement[];
  messages: PrivateMessage[];
}): TeacherDashboardOverview {
  const {
    classes,
    enrollments,
    quizzes,
    attempts,
    resources,
    videoTrackings,
    announcements,
    messages,
  } = params;

  const approvedEnrollments = enrollments.filter((e) => e.status === 'approved');
  const pendingEnrollments = enrollments.filter((e) => e.status === 'pending');

  const computeAvgPct = (list: QuizAttempt[]) => {
    const valid = list.filter((a) => a.maxScore > 0);
    if (valid.length === 0) return 0;
    const sum = valid.reduce((acc, a) => acc + (a.score / a.maxScore) * 100, 0);
    return Math.round(sum / valid.length);
  };

  const globalAverageQcmPct = computeAvgPct(attempts);
  const completedVideos = videoTrackings.filter((t) => t.status === 'completed').length;
  const videoCompletionRatePct =
    videoTrackings.length > 0 ? Math.round((completedVideos * 100) / videoTrackings.length) : 0;

  const unreadStudentMessages = messages.filter(
    (m) => m.senderRole === 'student' && !m.isRead
  ).length;

  const classSummaries: ClassAnalyticsSummary[] = classes.map((cls) => {
    const clsAttempts = attempts.filter((a) => a.classId === cls.id);
    return {
      svtClass: cls,
      approvedStudentsCount: approvedEnrollments.filter((e) => e.classId === cls.id).length,
      quizzesCount: quizzes.filter((q) => q.classId === cls.id).length,
      attemptsCount: clsAttempts.length,
      classAveragePct: computeAvgPct(clsAttempts),
      resourcesCount: resources.filter((r) => r.classId === cls.id).length,
    };
  });

  return {
    totalClasses: classes.length,
    totalApprovedStudents: approvedEnrollments.length,
    totalPendingEnrollments: pendingEnrollments.length,
    totalQuizzes: quizzes.length,
    totalAttempts: attempts.length,
    globalAverageQcmPct,
    totalResources: resources.length,
    videoCompletionRatePct,
    totalAnnouncements: announcements.length,
    unreadStudentMessages,
    classSummaries,
  };
}

export function computeStudentQcmResults(params: {
  classes: SvtClass[];
  enrollments: Enrollment[];
  roster: RosterStudent[];
  quizzes: Quiz[];
  attempts: QuizAttempt[];
  filterClassId: string | null;
}): StudentQcmResultSummary[] {
  const { classes, enrollments, roster, quizzes, attempts, filterClassId } = params;
  const targetClasses = filterClassId
    ? classes.filter((c) => c.id === filterClassId)
    : classes;

  const summaries: StudentQcmResultSummary[] = [];

  for (const cls of targetClasses) {
    const clsQuizzes = quizzes
      .filter((q) => q.classId === cls.id)
      .sort((a, b) => b.createdAtMillis - a.createdAtMillis);

    const clsAttempts = attempts.filter((a) => a.classId === cls.id);
    const approvedEnrollments = enrollments.filter(
      (e) => e.classId === cls.id && e.status === 'approved'
    );
    const clsRoster = roster.filter((r) => r.classId === cls.id);

    const rosterByLinkedId = new Map<string, RosterStudent>();
    const rosterByMassar = new Map<string, RosterStudent>();
    for (const r of clsRoster) {
      if (r.linkedStudentId) rosterByLinkedId.set(r.linkedStudentId, r);
      if (r.massarNumber) rosterByMassar.set(r.massarNumber.toUpperCase(), r);
    }

    const handledStudentIds = new Set<string>();

    const buildStudentSummary = (
      studentId: string,
      studentName: string,
      massarNumber: string
    ) => {
      const studentAttempts = clsAttempts.filter((a) => a.studentId === studentId);
      const attemptByQuiz = new Map<string, QuizAttempt>();
      for (const att of studentAttempts) {
        const prev = attemptByQuiz.get(att.quizId);
        if (!prev || att.submittedAtMillis > prev.submittedAtMillis) {
          attemptByQuiz.set(att.quizId, att);
        }
      }

      let totalScore = 0;
      let totalMaxScore = 0;
      let takenCount = 0;
      const perQuizResults: QuizScoreEntry[] = clsQuizzes.map((q) => {
        const att = attemptByQuiz.get(q.id) || null;
        if (att) {
          takenCount += 1;
          totalScore += att.score;
          totalMaxScore += att.maxScore;
        }
        const pct =
          att && att.maxScore > 0 ? Math.round((att.score * 100) / att.maxScore) : null;
        return { quiz: q, attempt: att, percentage: pct };
      });

      const overallPercentage =
        totalMaxScore > 0 ? Math.round((totalScore * 100) / totalMaxScore) : 0;

      summaries.push({
        studentId,
        studentName,
        massarNumber,
        classId: cls.id,
        className: cls.name,
        quizzesTakenCount: takenCount,
        totalClassQuizzesCount: clsQuizzes.length,
        totalScore,
        totalMaxScore,
        overallPercentage,
        perQuizResults,
      });
    };

    for (const enr of approvedEnrollments) {
      handledStudentIds.add(enr.studentId);
      const rMatch =
        rosterByLinkedId.get(enr.studentId) ||
        (enr.massarNumber ? rosterByMassar.get(enr.massarNumber.toUpperCase()) : undefined);
      const name = enr.studentName || rMatch?.fullName || enr.massarNumber || 'Élève';
      const massar = enr.massarNumber || rMatch?.massarNumber || '';
      buildStudentSummary(enr.studentId, name, massar);
    }

    // Any attempts from students not in approvedEnrollments (legacy/roster)
    for (const att of clsAttempts) {
      if (!handledStudentIds.has(att.studentId)) {
        handledStudentIds.add(att.studentId);
        const rMatch = rosterByLinkedId.get(att.studentId);
        buildStudentSummary(
          att.studentId,
          att.studentName || rMatch?.fullName || 'Élève',
          rMatch?.massarNumber || ''
        );
      }
    }
  }

  return summaries;
}

export function formatScoreValue(value: number): string {
  return Number.isInteger(value) ? String(value) : value.toFixed(1);
}
