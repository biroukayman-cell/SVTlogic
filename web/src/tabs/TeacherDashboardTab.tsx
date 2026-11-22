import React, { useMemo, useState } from 'react';
import {
  ArrowUpDown,
  BarChart3,
  BookOpen,
  ChevronDown,
  ChevronUp,
  LayoutDashboard,
  Layers,
  Search,
  Users,
} from 'lucide-react';
import {
  Announcement,
  AppLanguage,
  CourseResource,
  Enrollment,
  PrivateMessage,
  Quiz,
  QuizAttempt,
  RosterStudent,
  SvtClass,
  VideoTracking,
} from '../types/svt';
import { TRANSLATIONS } from '../lib/i18n';
import {
  computeDashboardOverview,
  computeStudentQcmResults,
  formatScoreValue,
} from '../lib/teacherAnalytics';

interface TeacherDashboardTabProps {
  lang: AppLanguage;
  selectedSection: 'overview' | 'results';
  onSelectSection: (sec: 'overview' | 'results') => void;
  selectedClassId: string | null;
  classes: SvtClass[];
  enrollments: Enrollment[];
  roster: RosterStudent[];
  quizzes: Quiz[];
  attempts: QuizAttempt[];
  resources: CourseResource[];
  videoTracking: VideoTracking[];
  announcements: Announcement[];
  messages: PrivateMessage[];
}

export const TeacherDashboardTab: React.FC<TeacherDashboardTabProps> = ({
  lang,
  selectedSection,
  onSelectSection,
  selectedClassId,
  classes,
  enrollments,
  roster,
  quizzes,
  attempts,
  resources,
  videoTracking,
  announcements,
  messages,
}) => {
  const tr = TRANSLATIONS[lang];
  const [filterClassId, setFilterClassId] = useState<string | null>(selectedClassId);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortByPct, setSortByPct] = useState(true);
  const [expandedKeys, setExpandedKeys] = useState<Record<string, boolean>>({});

  const overview = useMemo(
    () =>
      computeDashboardOverview({
        classes,
        enrollments,
        quizzes,
        attempts,
        resources,
        videoTrackings: videoTracking,
        announcements,
        messages,
      }),
    [classes, enrollments, quizzes, attempts, resources, videoTracking, announcements, messages]
  );

  const studentResults = useMemo(() => {
    const base = computeStudentQcmResults({
      classes,
      enrollments,
      roster,
      quizzes,
      attempts,
      filterClassId,
    });
    const q = searchQuery.trim().toLowerCase();
    const filtered = q
      ? base.filter(
          (r) =>
            r.studentName.toLowerCase().includes(q) ||
            r.massarNumber.toLowerCase().includes(q) ||
            r.className.toLowerCase().includes(q)
        )
      : base;
    if (sortByPct) {
      return [...filtered].sort(
        (a, b) =>
          b.overallPercentage - a.overallPercentage ||
          a.studentName.localeCompare(b.studentName)
      );
    }
    return [...filtered].sort((a, b) => a.studentName.localeCompare(b.studentName));
  }, [classes, enrollments, roster, quizzes, attempts, filterClassId, searchQuery, sortByPct]);

  return (
    <div className="space-y-4">
      <div className="rounded-3xl bg-gradient-to-r from-svt-primary to-svt-secondary p-5 text-white shadow-sm">
        <h2 className="font-display text-xl font-extrabold">{tr.dashboardTitle}</h2>
        <p className="mt-0.5 text-xs text-white/85">{tr.dashboardSubtitle}</p>
      </div>

      {/* Section Switcher */}
      <div className="flex gap-2">
        <button
          type="button"
          onClick={() => onSelectSection('overview')}
          className={`flex min-h-[42px] items-center gap-1.5 rounded-2xl px-4 text-xs font-bold ${
            selectedSection === 'overview'
              ? 'bg-svt-primary text-svt-onPrimary'
              : 'border border-svt-outline/25 bg-svt-surface'
          }`}
        >
          <LayoutDashboard className="h-4 w-4" />
          <span>{tr.overviewTab}</span>
        </button>
        <button
          type="button"
          onClick={() => onSelectSection('results')}
          className={`flex min-h-[42px] items-center gap-1.5 rounded-2xl px-4 text-xs font-bold ${
            selectedSection === 'results'
              ? 'bg-svt-primary text-svt-onPrimary'
              : 'border border-svt-outline/25 bg-svt-surface'
          }`}
        >
          <BarChart3 className="h-4 w-4" />
          <span>{tr.resultsTab}</span>
        </button>
      </div>

      {selectedSection === 'overview' ? (
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            <KpiCard
              icon={<Layers className="h-5 w-5 text-svt-primary" />}
              title={tr.kpiClasses}
              value={String(overview.totalClasses)}
              subtitle={`${overview.totalAnnouncements} annonces`}
            />
            <KpiCard
              icon={<Users className="h-5 w-5 text-svt-secondary" />}
              title={tr.kpiStudents}
              value={String(overview.totalApprovedStudents)}
              subtitle={`${overview.totalPendingEnrollments} ${tr.kpiPending}`}
            />
            <KpiCard
              icon={<BarChart3 className="h-5 w-5 text-svt-success" />}
              title={tr.kpiQcmAvg}
              value={`${overview.globalAverageQcmPct}%`}
              subtitle={`${overview.totalAttempts} copies • ${overview.totalQuizzes} QCM`}
              onClick={() => onSelectSection('results')}
            />
            <KpiCard
              icon={<BookOpen className="h-5 w-5 text-svt-tertiary" />}
              title={tr.kpiResourcesVideo}
              value={String(overview.totalResources)}
              subtitle={`${overview.videoCompletionRatePct}% vidéo • ${overview.unreadStudentMessages} msg`}
            />
          </div>

          <h3 className="font-display text-base font-bold">{tr.classBreakdown}</h3>
          <div className="grid gap-3 md:grid-cols-2">
            {overview.classSummaries.map((s) => (
              <div
                key={s.svtClass.id}
                className="space-y-2 rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 shadow-sm"
              >
                <div className="flex items-center justify-between gap-2">
                  <div>
                    <div className="font-display text-sm font-bold">{s.svtClass.name}</div>
                    <div className="text-[11px] text-svt-onSurfaceVariant">
                      {s.svtClass.level} • {s.svtClass.academicYear}
                    </div>
                  </div>
                  <span
                    className={`rounded-xl px-2.5 py-1 text-xs font-bold ${
                      s.classAveragePct >= 50
                        ? 'bg-svt-success/15 text-svt-success'
                        : 'bg-svt-warning/15 text-svt-warning'
                    }`}
                  >
                    {s.classAveragePct}%
                  </span>
                </div>
                <div className="h-1.5 w-full overflow-hidden rounded-full bg-svt-surfaceVariant">
                  <div
                    className="h-full rounded-full bg-svt-primary"
                    style={{ width: `${s.classAveragePct}%` }}
                  />
                </div>
                <div className="flex justify-between text-xs text-svt-onSurfaceVariant">
                  <span>{s.approvedStudentsCount} élèves</span>
                  <span>
                    {s.quizzesCount} QCM • {s.attemptsCount} copies • {s.resourcesCount} cours
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : (
        <div className="space-y-3">
          <div className="rounded-3xl bg-svt-primaryContainer/50 p-4 text-xs text-svt-onPrimaryContainer">
            <div className="font-display text-sm font-bold">{tr.qcmResultsTitle}</div>
            <p className="mt-0.5 opacity-85">{tr.qcmResultsDesc}</p>
          </div>

          <div className="flex gap-2 overflow-x-auto no-scrollbar">
            <button
              type="button"
              onClick={() => setFilterClassId(null)}
              className={`min-h-[36px] shrink-0 rounded-xl px-3 text-xs font-bold ${
                filterClassId === null
                  ? 'bg-svt-secondary text-svt-onSecondary'
                  : 'border border-svt-outline/25 bg-svt-surface'
              }`}
            >
              Toutes ({classes.length})
            </button>
            {classes.map((c) => (
              <button
                key={c.id}
                type="button"
                onClick={() => setFilterClassId(c.id)}
                className={`min-h-[36px] shrink-0 rounded-xl px-3 text-xs font-bold ${
                  filterClassId === c.id
                    ? 'bg-svt-secondary text-svt-onSecondary'
                    : 'border border-svt-outline/25 bg-svt-surface'
                }`}
              >
                {c.name}
              </button>
            ))}
          </div>

          <div className="flex gap-2">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute top-3.5 start-3 h-4 w-4 text-svt-onSurfaceVariant" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder={tr.qcmSearchHint}
                className="min-h-[44px] w-full rounded-2xl border border-svt-outline/25 bg-svt-surface ps-9 pe-3 text-xs"
              />
            </div>
            <button
              type="button"
              onClick={() => setSortByPct((s) => !s)}
              className="flex min-h-[44px] shrink-0 items-center gap-1 rounded-2xl border border-svt-outline/25 bg-svt-surface px-3 text-xs font-bold"
            >
              <ArrowUpDown className="h-3.5 w-3.5" />
              <span>{sortByPct ? tr.sortByPct : tr.sortByName}</span>
            </button>
          </div>

          <div className="space-y-2.5">
            {studentResults.map((s) => {
              const key = `${s.classId}_${s.studentId}`;
              const expanded = Boolean(expandedKeys[key]);
              const badgeColor =
                s.quizzesTakenCount === 0
                  ? 'bg-svt-surfaceVariant text-svt-onSurfaceVariant'
                  : s.overallPercentage >= 65
                  ? 'bg-svt-success/15 text-svt-success'
                  : s.overallPercentage >= 45
                  ? 'bg-svt-warning/15 text-svt-warning'
                  : 'bg-svt-error/15 text-svt-error';

              return (
                <div
                  key={key}
                  className="rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 shadow-sm"
                >
                  <button
                    type="button"
                    onClick={() =>
                      setExpandedKeys((prev) => ({ ...prev, [key]: !prev[key] }))
                    }
                    className="flex w-full items-center justify-between gap-2 text-start"
                  >
                    <div className="min-w-0">
                      <div className="truncate font-display text-sm font-bold">
                        {s.studentName}
                      </div>
                      <div className="text-[11px] text-svt-onSurfaceVariant">
                        {[s.className, s.massarNumber].filter(Boolean).join(' • ')}
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className={`rounded-xl px-2.5 py-1 text-xs font-bold ${badgeColor}`}>
                        Total : {s.overallPercentage}%
                      </span>
                      {expanded ? (
                        <ChevronUp className="h-4 w-4" />
                      ) : (
                        <ChevronDown className="h-4 w-4" />
                      )}
                    </div>
                  </button>

                  <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-svt-surfaceVariant">
                    <div
                      className="h-full rounded-full bg-svt-primary"
                      style={{ width: `${s.overallPercentage}%` }}
                    />
                  </div>

                  <div className="mt-1.5 text-[11px] font-semibold text-svt-onSurfaceVariant">
                    {s.quizzesTakenCount} / {s.totalClassQuizzesCount} QCM passés •{' '}
                    {formatScoreValue(s.totalScore)} / {formatScoreValue(s.totalMaxScore)} pts
                  </div>

                  {expanded && s.perQuizResults.length > 0 && (
                    <div className="mt-3 space-y-1.5 border-t border-svt-outline/15 pt-2.5 text-xs">
                      {s.perQuizResults.map((entry) => (
                        <div
                          key={entry.quiz.id}
                          className="flex items-center justify-between gap-2"
                        >
                          <span className="truncate">{entry.quiz.title}</span>
                          {entry.attempt && entry.percentage !== null ? (
                            <span
                              className={`shrink-0 font-bold ${
                                entry.percentage >= 50 ? 'text-svt-success' : 'text-svt-error'
                              }`}
                            >
                              {formatScoreValue(entry.attempt.score)}/
                              {formatScoreValue(entry.attempt.maxScore)} ({entry.percentage}%)
                            </span>
                          ) : (
                            <span className="shrink-0 text-svt-onSurfaceVariant">{tr.notTaken}</span>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};

const KpiCard: React.FC<{
  icon: React.ReactNode;
  title: string;
  value: string;
  subtitle: string;
  onClick?: () => void;
}> = ({ icon, title, value, subtitle, onClick }) => (
  <div
    onClick={onClick}
    className={`rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 shadow-sm ${
      onClick ? 'cursor-pointer hover:border-svt-primary/40' : ''
    }`}
  >
    <div className="flex items-center gap-2 text-xs font-bold text-svt-onSurfaceVariant">
      {icon}
      <span className="truncate">{title}</span>
    </div>
    <div className="mt-1.5 font-display text-2xl font-extrabold">{value}</div>
    <div className="mt-0.5 truncate text-[11px] text-svt-onSurfaceVariant">{subtitle}</div>
  </div>
);
