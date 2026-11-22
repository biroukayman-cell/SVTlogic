import React, { useEffect, useMemo, useState } from 'react';
import {
  BarChart3,
  CheckCircle2,
  Clock,
  CloudOff,
  CloudUpload,
  ImagePlus,
  Play,
  Plus,
  Sparkles,
  Trash2,
  X,
} from 'lucide-react';
import {
  AppLanguage,
  Enrollment,
  Quiz,
  QuizAttempt,
  QuizQuestion,
  SvtClass,
  UserProfile,
} from '../types/svt';
import { DOMAIN_LABELS, TRANSLATIONS } from '../lib/i18n';
import { formatDurationClock } from '../lib/authHelpers';
import { compressImageToDataUrl } from '../lib/fileUtils';
import {
  deleteQuiz,
  publishQuizToClasses,
  submitQuizAttempt,
  toggleQuizDownloadable,
} from '../lib/repository';

interface ExercisesTabProps {
  isTeacher: boolean;
  isGuestMode: boolean;
  lang: AppLanguage;
  profile: UserProfile;
  selectedClassId: string | null;
  teacherClasses: SvtClass[];
  enrollments: Enrollment[];
  quizzes: Quiz[];
  attempts: QuizAttempt[];
  completedVideosCount: number;
  openedResourcesCount: number;
  sentMessagesCount: number;
  onGuestBlocked: () => void;
  onOpenQcmResults?: () => void;
}

export const ExercisesTab: React.FC<ExercisesTabProps> = ({
  isTeacher,
  isGuestMode,
  lang,
  profile,
  selectedClassId,
  teacherClasses,
  enrollments,
  quizzes,
  attempts,
  completedVideosCount,
  openedResourcesCount,
  sentMessagesCount,
  onGuestBlocked,
  onOpenQcmResults,
}) => {
  const tr = TRANSLATIONS[lang];
  const [filter, setFilter] = useState<'all' | 'todo' | 'done'>('all');
  const [playingQuiz, setPlayingQuiz] = useState<Quiz | null>(null);
  const [reviewAttempt, setReviewAttempt] = useState<{ quiz: Quiz | null; attempt: QuizAttempt } | null>(null);
  const [statsQuiz, setStatsQuiz] = useState<Quiz | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [unlockedToast, setUnlockedToast] = useState<string[]>([]);

  const attemptByQuiz = useMemo(() => {
    const map = new Map<string, QuizAttempt>();
    for (const a of attempts) {
      if (!isTeacher && a.studentId !== profile.userId) continue;
      if (!map.has(a.quizId)) map.set(a.quizId, a);
    }
    return map;
  }, [attempts, isTeacher, profile.userId]);

  const visibleQuizzes = useMemo(() => {
    const base =
      isTeacher && selectedClassId
        ? quizzes.filter((q) => q.classId === selectedClassId)
        : quizzes;
    if (isTeacher || filter === 'all') return base;
    return base.filter((q) => {
      const hasAtt = attemptByQuiz.has(q.id);
      return filter === 'done' ? hasAtt : !hasAtt;
    });
  }, [quizzes, isTeacher, selectedClassId, filter, attemptByQuiz]);

  return (
    <div className="space-y-4">
      {/* Header Banner */}
      <div className="rounded-3xl bg-gradient-to-r from-svt-primary to-svt-secondary p-5 text-white shadow-sm">
        <h2 className="font-display text-xl font-extrabold">{tr.tabExercises}</h2>
        <p className="mt-0.5 text-xs text-white/85">{tr.appSubtitle}</p>
      </div>

      {unlockedToast.length > 0 && (
        <div className="flex items-center justify-between rounded-2xl border border-svt-tertiary/40 bg-svt-tertiaryContainer p-3.5 text-svt-onTertiaryContainer shadow-sm">
          <div className="flex items-center gap-2 text-xs font-bold">
            <Sparkles className="h-4 w-4 text-svt-tertiary" />
            <span>Badge(s) SVT débloqué(s) : {unlockedToast.join(', ')}</span>
          </div>
          <button type="button" onClick={() => setUnlockedToast([])} className="p-1">
            <X className="h-4 w-4" />
          </button>
        </div>
      )}

      {/* Teacher Actions or Student Filter Chips */}
      {isTeacher ? (
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            disabled={teacherClasses.length === 0}
            onClick={() => setCreateOpen(true)}
            className="flex min-h-[44px] items-center gap-1.5 rounded-2xl bg-svt-primary px-4 text-sm font-bold text-svt-onPrimary shadow-sm disabled:opacity-50"
          >
            <Plus className="h-4 w-4" />
            <span>{tr.quizCreate}</span>
          </button>
          {onOpenQcmResults && (
            <button
              type="button"
              onClick={onOpenQcmResults}
              className="flex min-h-[44px] items-center gap-1.5 rounded-2xl border border-svt-outline/30 bg-svt-surface px-4 text-sm font-bold hover:bg-svt-surfaceVariant/50"
            >
              <BarChart3 className="h-4 w-4 text-svt-secondary" />
              <span>{tr.quizViewResultsBtn}</span>
            </button>
          )}
        </div>
      ) : (
        <div className="flex gap-2 overflow-x-auto no-scrollbar">
          {(['all', 'todo', 'done'] as const).map((f) => (
            <button
              key={f}
              type="button"
              onClick={() => setFilter(f)}
              className={`min-h-[40px] shrink-0 rounded-2xl px-4 text-xs font-bold transition ${
                filter === f
                  ? 'bg-svt-primary text-svt-onPrimary'
                  : 'border border-svt-outline/25 bg-svt-surface text-svt-onSurfaceVariant'
              }`}
            >
              {f === 'all' ? tr.allFilter : f === 'todo' ? tr.todoFilter : tr.doneFilter}
            </button>
          ))}
        </div>
      )}

      {/* Quiz List */}
      {visibleQuizzes.length === 0 ? (
        <div className="rounded-3xl border border-dashed border-svt-outline/30 bg-svt-surface p-8 text-center text-sm text-svt-onSurfaceVariant">
          {tr.quizEmpty}
        </div>
      ) : (
        <div className="grid gap-3 md:grid-cols-2">
          {visibleQuizzes.map((quiz) => {
            const att = !isTeacher ? attemptByQuiz.get(quiz.id) : undefined;
            const quizAttempts = isTeacher ? attempts.filter((a) => a.quizId === quiz.id) : [];

            return (
              <div
                key={quiz.id}
                className="flex flex-col justify-between rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 shadow-sm"
              >
                <div>
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <span className="text-[11px] font-bold uppercase tracking-wide text-svt-secondary">
                        {quiz.chapterSvt}
                      </span>
                      <h3 className="font-display text-base font-bold leading-snug">{quiz.title}</h3>
                    </div>

                    {isTeacher ? (
                      <button
                        type="button"
                        onClick={() => {
                          if (confirm(`Supprimer le QCM « ${quiz.title} » ?`)) {
                            deleteQuiz(quiz.id);
                          }
                        }}
                        className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl text-svt-error hover:bg-svt-error/10"
                        title={tr.delete}
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    ) : (
                      <span
                        className={`shrink-0 rounded-full px-2.5 py-1 text-[11px] font-bold ${
                          att
                            ? 'bg-svt-success/15 text-svt-success'
                            : 'bg-svt-primaryContainer text-svt-onPrimaryContainer'
                        }`}
                      >
                        {att ? `${att.score}/${att.maxScore}` : tr.quizPendingTag}
                      </span>
                    )}
                  </div>

                  {quiz.description && (
                    <p className="mt-1.5 text-xs leading-relaxed text-svt-onSurfaceVariant">
                      {quiz.description}
                    </p>
                  )}
                </div>

                <div className="mt-4 flex flex-wrap items-center justify-between gap-2 border-t border-svt-outline/15 pt-3">
                  <span className="text-xs font-semibold text-svt-onSurfaceVariant">
                    {quiz.questions.length} QCM
                  </span>

                  {isTeacher ? (
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => toggleQuizDownloadable(quiz)}
                        className="flex min-h-[38px] items-center gap-1 rounded-xl border border-svt-outline/25 px-3 text-xs font-bold"
                      >
                        {quiz.isDownloadable ? (
                          <CloudUpload className="h-3.5 w-3.5 text-svt-success" />
                        ) : (
                          <CloudOff className="h-3.5 w-3.5" />
                        )}
                        <span>{quiz.isDownloadable ? 'Hors-ligne ON' : 'En ligne'}</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => setStatsQuiz(quiz)}
                        className="flex min-h-[38px] items-center gap-1 rounded-xl bg-svt-secondaryContainer px-3 text-xs font-bold text-svt-onSecondaryContainer"
                      >
                        <BarChart3 className="h-3.5 w-3.5" />
                        <span>({quizAttempts.length})</span>
                      </button>
                    </div>
                  ) : att ? (
                    <button
                      type="button"
                      onClick={() => setReviewAttempt({ quiz, attempt: att })}
                      className="flex min-h-[42px] items-center gap-1.5 rounded-2xl border border-svt-primary/40 bg-svt-primaryContainer/40 px-4 text-xs font-bold text-svt-primary"
                    >
                      <CheckCircle2 className="h-4 w-4" />
                      <span>{tr.quizViewResult}</span>
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setPlayingQuiz(quiz)}
                      className="flex min-h-[42px] items-center gap-1.5 rounded-2xl bg-svt-primary px-4 text-xs font-bold text-svt-onPrimary shadow-sm"
                    >
                      <Play className="h-4 w-4" />
                      <span>{tr.quizStart}</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Timed QCM Player Modal */}
      {playingQuiz && (
        <TimedQuizPlayerModal
          quiz={playingQuiz}
          lang={lang}
          isGuestMode={isGuestMode}
          onClose={() => setPlayingQuiz(null)}
          onSubmit={async (selected, perQ, total) => {
            const q = playingQuiz;
            setPlayingQuiz(null);
            if (isGuestMode) {
              onGuestBlocked();
              return;
            }
            try {
              const res = await submitQuizAttempt({
                quiz: q,
                profile,
                selectedAnswers: selected,
                perQuestionSeconds: perQ,
                totalSeconds: total,
                previousAttemptsCount: attempts.length,
                completedVideosCount,
                openedResourcesCount,
                sentMessagesCount,
              });
              if (res.newBadges.length > 0) setUnlockedToast(res.newBadges);
              setReviewAttempt({ quiz: q, attempt: res.attempt });
            } catch (e: any) {
              alert(e?.message || 'Erreur lors de l’envoi du QCM.');
            }
          }}
        />
      )}

      {/* Attempt Review Modal */}
      {reviewAttempt && (
        <QuizResultModal
          quiz={reviewAttempt.quiz}
          attempt={reviewAttempt.attempt}
          lang={lang}
          onClose={() => setReviewAttempt(null)}
        />
      )}

      {/* Teacher Quiz Stats Modal */}
      {statsQuiz && (
        <TeacherQuizStatsModal
          quiz={statsQuiz}
          attempts={attempts.filter((a) => a.quizId === statsQuiz.id)}
          onClose={() => setStatsQuiz(null)}
        />
      )}

      {/* Teacher Create Quiz Modal */}
      {createOpen && (
        <CreateQuizModal
          lang={lang}
          classes={teacherClasses}
          defaultClassId={selectedClassId}
          onClose={() => setCreateOpen(false)}
          onPublish={async (payload) => {
            await publishQuizToClasses({
              teacherId: profile.userId,
              enrollments,
              ...payload,
            });
            setCreateOpen(false);
          }}
        />
      )}
    </div>
  );
};

const TimedQuizPlayerModal: React.FC<{
  quiz: Quiz;
  lang: AppLanguage;
  isGuestMode: boolean;
  onClose: () => void;
  onSubmit: (selected: number[], perQuestionSecs: number[], totalSecs: number) => void;
}> = ({ quiz, lang, isGuestMode, onClose, onSubmit }) => {
  const tr = TRANSLATIONS[lang];
  const questions = quiz.questions;
  const [idx, setIdx] = useState(0);
  const [selected, setSelected] = useState<number[]>(() => Array(questions.length).fill(-1));
  const [perQSecs, setPerQSecs] = useState<number[]>(() => Array(questions.length).fill(0));
  const [totalSecs, setTotalSecs] = useState(0);
  const [questionElapsedMs, setQuestionElapsedMs] = useState(0);

  useEffect(() => {
    setQuestionElapsedMs((perQSecs[idx] || 0) * 1000);
    let ticks = 0;
    const timer = setInterval(() => {
      setQuestionElapsedMs((ms) => ms + 100);
      ticks += 1;
      if (ticks % 10 === 0) {
        setTotalSecs((t) => t + 1);
        setPerQSecs((prev) => {
          const copy = [...prev];
          copy[idx] = (copy[idx] || 0) + 1;
          return copy;
        });
      }
    }, 100);
    return () => clearInterval(timer);
  }, [idx]);

  const q = questions[idx];
  if (!q) return null;

  const progressPct = Math.round(((idx + 1) * 100) / Math.max(1, questions.length));
  const paceFraction = Math.min(1, questionElapsedMs / 45_000);
  const paceColor =
    paceFraction < 0.6 ? '#1B7A54' : paceFraction < 0.85 ? '#D97706' : '#D14343';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-3 backdrop-blur-sm">
      <div className="flex max-h-[92dvh] w-full max-w-lg flex-col overflow-hidden rounded-3xl bg-svt-surface text-svt-onSurface shadow-2xl">
        {/* Top Timer & Progress */}
        <div className="space-y-2.5 border-b border-svt-outline/20 p-4">
          <div className="flex items-center justify-between">
            <span className="font-display text-base font-bold">
              Question {idx + 1} / {questions.length}
            </span>
            <button
              type="button"
              onClick={onClose}
              className="flex h-9 w-9 items-center justify-center rounded-full hover:bg-svt-surfaceVariant"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          <div className="flex items-center justify-between text-xs font-bold">
            <span className="inline-flex items-center gap-1 rounded-xl bg-svt-secondaryContainer px-2.5 py-1 text-svt-onSecondaryContainer">
              <Clock className="h-3.5 w-3.5" />
              <span>{formatDurationClock(perQSecs[idx] || 0)}</span>
            </span>
            <span className="rounded-xl bg-svt-primaryContainer px-2.5 py-1 text-svt-onPrimaryContainer">
              Total : {formatDurationClock(totalSecs)}
            </span>
          </div>

          <div className="h-1.5 w-full overflow-hidden rounded-full bg-svt-surfaceVariant">
            <div
              className="h-full rounded-full bg-svt-primary transition-all duration-300"
              style={{ width: `${progressPct}%` }}
            />
          </div>

          {/* Animated Pace Bar (Green -> Orange -> Red) */}
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-svt-surfaceVariant/70">
            <div
              className="h-full rounded-full transition-colors duration-300"
              style={{
                width: `${Math.round(paceFraction * 100)}%`,
                backgroundColor: paceColor,
              }}
            />
          </div>
        </div>

        {/* Question Body */}
        <div key={idx} className="animate-qcm-in flex-1 space-y-3 overflow-y-auto p-4">
          {q.imageUrl && (
            <img
              src={q.imageUrl}
              alt="Illustration QCM"
              className="max-h-52 w-full rounded-2xl border border-svt-outline/20 object-contain bg-svt-bg"
            />
          )}
          <h3 className="font-display text-base font-bold leading-snug">{q.questionText}</h3>

          <div className="space-y-2">
            {q.options.map((opt, optIdx) => {
              const isSel = selected[idx] === optIdx;
              return (
                <button
                  key={optIdx}
                  type="button"
                  onClick={() => {
                    const copy = [...selected];
                    copy[idx] = optIdx;
                    setSelected(copy);
                  }}
                  className={`flex min-h-[50px] w-full items-center gap-3 rounded-2xl border p-3.5 text-start text-sm transition ${
                    isSel
                      ? 'border-svt-primary bg-svt-primaryContainer/70 font-bold text-svt-onPrimaryContainer'
                      : 'border-svt-outline/20 bg-svt-bg hover:border-svt-primary/40'
                  }`}
                >
                  <span
                    className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-full border-2 ${
                      isSel ? 'border-svt-primary bg-svt-primary' : 'border-svt-outline/50'
                    }`}
                  >
                    {isSel && <span className="h-2 w-2 rounded-full bg-white" />}
                  </span>
                  <span>{opt}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Footer Controls */}
        <div className="flex items-center justify-between gap-2 border-t border-svt-outline/20 p-4">
          <button
            type="button"
            disabled={idx === 0}
            onClick={() => setIdx((i) => Math.max(0, i - 1))}
            className="min-h-[46px] rounded-2xl border border-svt-outline/30 px-4 text-sm font-bold disabled:opacity-40"
          >
            {tr.quizPrevious}
          </button>
          {idx < questions.length - 1 ? (
            <button
              type="button"
              disabled={!isGuestMode && selected[idx] === -1}
              onClick={() => setIdx((i) => i + 1)}
              className="min-h-[46px] flex-1 rounded-2xl bg-svt-primary px-5 text-sm font-bold text-svt-onPrimary disabled:opacity-45"
            >
              {tr.quizNext}
            </button>
          ) : (
            <button
              type="button"
              disabled={!isGuestMode && selected[idx] === -1}
              onClick={() => onSubmit(selected, perQSecs, totalSecs)}
              className="min-h-[46px] flex-1 rounded-2xl bg-svt-success px-5 text-sm font-bold text-white disabled:opacity-45"
            >
              {tr.quizSubmit}
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

const QuizResultModal: React.FC<{
  quiz: Quiz | null;
  attempt: QuizAttempt;
  lang: AppLanguage;
  onClose: () => void;
}> = ({ quiz, attempt, lang, onClose }) => {
  const tr = TRANSLATIONS[lang];
  const pct =
    attempt.maxScore > 0 ? Math.round((attempt.score * 100) / attempt.maxScore) : 0;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-3 backdrop-blur-sm">
      <div className="flex max-h-[90dvh] w-full max-w-lg flex-col overflow-hidden rounded-3xl bg-svt-surface text-svt-onSurface shadow-2xl">
        <div className="flex items-center justify-between border-b border-svt-outline/20 px-5 py-4">
          <div>
            <h3 className="font-display text-lg font-bold">{tr.quizResultTitle}</h3>
            <p className="text-xs text-svt-onSurfaceVariant">
              {attempt.score} / {attempt.maxScore} ({pct}%) •{' '}
              {formatDurationClock(attempt.totalDurationSeconds)}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="flex h-9 w-9 items-center justify-center rounded-full hover:bg-svt-surfaceVariant"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex-1 space-y-3 overflow-y-auto p-4">
          {quiz?.questions.map((q, idx) => {
            const ans = attempt.answers.find((a) => a.questionIndex === idx);
            const ok = ans?.isCorrect;
            return (
              <div
                key={idx}
                className={`rounded-2xl border p-3.5 text-xs ${
                  ok
                    ? 'border-svt-success/35 bg-svt-success/10'
                    : 'border-svt-error/35 bg-svt-error/10'
                }`}
              >
                <div className="font-display text-sm font-bold">
                  {idx + 1}. {q.questionText}
                </div>
                {q.imageUrl && (
                  <img
                    src={q.imageUrl}
                    alt="Question"
                    className="my-2 max-h-40 rounded-xl object-contain"
                  />
                )}
                <div className="mt-1.5">
                  Réponse choisie :{' '}
                  <strong>
                    {ans && ans.selectedIndex >= 0 ? q.options[ans.selectedIndex] : '—'}
                  </strong>
                </div>
                {!ok && (
                  <div className="mt-0.5 text-svt-success">
                    Bonne réponse : <strong>{q.options[q.correctIndex]}</strong>
                  </div>
                )}
                {q.explanation && (
                  <div className="mt-2 rounded-xl bg-svt-surface/80 p-2.5 text-svt-onSurfaceVariant">
                    <strong>{tr.quizExplanation}</strong> {q.explanation}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        <div className="border-t border-svt-outline/20 p-3.5 text-end">
          <button
            type="button"
            onClick={onClose}
            className="min-h-[44px] rounded-2xl bg-svt-primary px-6 text-sm font-bold text-svt-onPrimary"
          >
            {tr.close}
          </button>
        </div>
      </div>
    </div>
  );
};

const TeacherQuizStatsModal: React.FC<{
  quiz: Quiz;
  attempts: QuizAttempt[];
  onClose: () => void;
}> = ({ quiz, attempts, onClose }) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/65 p-4 backdrop-blur-sm">
      <div className="flex max-h-[85dvh] w-full max-w-md flex-col overflow-hidden rounded-3xl bg-svt-surface text-svt-onSurface shadow-2xl">
        <div className="flex items-center justify-between border-b border-svt-outline/20 px-5 py-4">
          <div>
            <h3 className="font-display text-base font-bold">{quiz.title}</h3>
            <p className="text-xs text-svt-onSurfaceVariant">{attempts.length} copies soumises</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="flex h-9 w-9 items-center justify-center rounded-full hover:bg-svt-surfaceVariant"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex-1 space-y-2 overflow-y-auto p-4">
          {attempts.length === 0 ? (
            <p className="py-6 text-center text-sm text-svt-onSurfaceVariant">
              Aucun élève n’a encore terminé ce QCM.
            </p>
          ) : (
            attempts.map((a) => {
              const pct = a.maxScore > 0 ? Math.round((a.score * 100) / a.maxScore) : 0;
              return (
                <div
                  key={a.id}
                  className="flex items-center justify-between rounded-2xl border border-svt-outline/20 bg-svt-bg p-3 text-xs"
                >
                  <div>
                    <div className="font-bold">{a.studentName}</div>
                    <div className="text-svt-onSurfaceVariant">
                      Temps : {formatDurationClock(a.totalDurationSeconds)}
                    </div>
                  </div>
                  <span
                    className={`rounded-xl px-2.5 py-1 font-bold ${
                      pct >= 50
                        ? 'bg-svt-success/15 text-svt-success'
                        : 'bg-svt-error/15 text-svt-error'
                    }`}
                  >
                    {a.score}/{a.maxScore} ({pct}%)
                  </span>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};

const CreateQuizModal: React.FC<{
  lang: AppLanguage;
  classes: SvtClass[];
  defaultClassId: string | null;
  onClose: () => void;
  onPublish: (payload: {
    classIds: string[];
    title: string;
    description: string;
    chapterSvt: string;
    questions: QuizQuestion[];
    isDownloadable: boolean;
  }) => Promise<void>;
}> = ({ lang, classes, defaultClassId, onClose, onPublish }) => {
  const tr = TRANSLATIONS[lang];
  const [selectedClassIds, setSelectedClassIds] = useState<string[]>(() =>
    defaultClassId ? [defaultClassId] : classes[0] ? [classes[0].id] : []
  );
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [chapterSvt, setChapterSvt] = useState(DOMAIN_LABELS.CELL_GENETICS[lang]);
  const [isDownloadable, setIsDownloadable] = useState(true);
  const [questions, setQuestions] = useState<QuizQuestion[]>([
    {
      questionText: '',
      options: ['', '', '', ''],
      correctIndex: 0,
      explanation: '',
    },
  ]);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const toggleClass = (cid: string) => {
    setSelectedClassIds((prev) =>
      prev.includes(cid) ? prev.filter((x) => x !== cid) : [...prev, cid]
    );
  };

  const handleImageUpload = async (qIdx: number, file?: File) => {
    if (!file) return;
    try {
      const dataUrl = await compressImageToDataUrl(file, 900, 180_000);
      setQuestions((prev) =>
        prev.map((item, i) => (i === qIdx ? { ...item, imageUrl: dataUrl } : item))
      );
    } catch {
      setErr('Impossible de charger l’image sélectionnée.');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErr(null);
    if (selectedClassIds.length === 0) {
      setErr('Sélectionnez au moins une classe.');
      return;
    }
    if (title.trim().length < 2) {
      setErr('Titre du QCM requis.');
      return;
    }
    const validQuestions = questions.filter(
      (q) => q.questionText.trim() && q.options.filter((o) => o.trim()).length >= 2
    );
    if (validQuestions.length === 0) {
      setErr('Ajoutez au moins 1 question complète avec 2 choix minimum.');
      return;
    }
    setBusy(true);
    try {
      await onPublish({
        classIds: selectedClassIds,
        title,
        description,
        chapterSvt,
        questions: validQuestions,
        isDownloadable,
      });
    } catch (e: any) {
      setErr(e?.message || 'Erreur lors de la publication.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-3 backdrop-blur-sm">
      <form
        onSubmit={handleSubmit}
        className="flex max-h-[92dvh] w-full max-w-2xl flex-col overflow-hidden rounded-3xl bg-svt-surface text-svt-onSurface shadow-2xl"
      >
        <div className="flex items-center justify-between border-b border-svt-outline/20 px-5 py-4">
          <h3 className="font-display text-lg font-bold">{tr.quizCreate}</h3>
          <button
            type="button"
            onClick={onClose}
            className="flex h-9 w-9 items-center justify-center rounded-full hover:bg-svt-surfaceVariant"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex-1 space-y-4 overflow-y-auto p-5">
          {err && (
            <div className="rounded-xl bg-svt-error/10 p-3 text-xs font-semibold text-svt-error">
              {err}
            </div>
          )}

          <div>
            <label className="mb-1.5 block text-xs font-bold text-svt-onSurfaceVariant">
              Classes cibles *
            </label>
            <div className="flex flex-wrap gap-2">
              {classes.map((c) => {
                const active = selectedClassIds.includes(c.id);
                return (
                  <button
                    key={c.id}
                    type="button"
                    onClick={() => toggleClass(c.id)}
                    className={`min-h-[38px] rounded-xl px-3 text-xs font-bold transition ${
                      active
                        ? 'bg-svt-primary text-svt-onPrimary'
                        : 'border border-svt-outline/25 bg-svt-bg'
                    }`}
                  >
                    {c.name}
                  </button>
                );
              })}
            </div>
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                Titre *
              </label>
              <input
                type="text"
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Ex : QCM 1 — Cycle de Krebs"
                className="min-h-[46px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                Chapitre SVT *
              </label>
              <input
                type="text"
                required
                value={chapterSvt}
                onChange={(e) => setChapterSvt(e.target.value)}
                className="min-h-[46px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
              />
            </div>
          </div>

          <div>
            <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
              Description (optionnel)
            </label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="min-h-[44px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
            />
          </div>

          <label className="flex items-center gap-2 text-xs font-semibold">
            <input
              type="checkbox"
              checked={isDownloadable}
              onChange={(e) => setIsDownloadable(e.target.checked)}
              className="h-4 w-4 accent-svt-primary"
            />
            <span>Autoriser l’accès hors-ligne</span>
          </label>

          {/* Questions */}
          <div className="space-y-3">
            {questions.map((q, qIdx) => (
              <div
                key={qIdx}
                className="space-y-2.5 rounded-2xl border border-svt-outline/25 bg-svt-bg p-3.5"
              >
                <div className="flex items-center justify-between">
                  <span className="font-display text-sm font-bold text-svt-primary">
                    Question {qIdx + 1}
                  </span>
                  {questions.length > 1 && (
                    <button
                      type="button"
                      onClick={() => setQuestions((prev) => prev.filter((_, i) => i !== qIdx))}
                      className="text-xs font-bold text-svt-error"
                    >
                      Supprimer
                    </button>
                  )}
                </div>

                <input
                  type="text"
                  value={q.questionText}
                  onChange={(e) =>
                    setQuestions((prev) =>
                      prev.map((item, i) =>
                        i === qIdx ? { ...item, questionText: e.target.value } : item
                      )
                    )
                  }
                  placeholder="Énoncé de la question..."
                  className="min-h-[44px] w-full rounded-xl border border-svt-outline/25 bg-svt-surface px-3 text-sm"
                />

                <div className="flex flex-wrap items-center gap-2">
                  <label className="inline-flex cursor-pointer items-center gap-1.5 rounded-xl bg-svt-secondaryContainer px-3 py-1.5 text-xs font-bold text-svt-onSecondaryContainer">
                    <ImagePlus className="h-3.5 w-3.5" />
                    <span>{q.imageUrl ? 'Changer schéma' : 'Ajouter schéma/image'}</span>
                    <input
                      type="file"
                      accept="image/*"
                      className="hidden"
                      onChange={(e) => handleImageUpload(qIdx, e.target.files?.[0])}
                    />
                  </label>
                  {q.imageUrl && (
                    <button
                      type="button"
                      onClick={() =>
                        setQuestions((prev) =>
                          prev.map((item, i) =>
                            i === qIdx ? { ...item, imageUrl: undefined } : item
                          )
                        )
                      }
                      className="text-xs font-bold text-svt-error"
                    >
                      Retirer image
                    </button>
                  )}
                </div>

                {q.imageUrl && (
                  <img
                    src={q.imageUrl}
                    alt="Aperçu"
                    className="max-h-36 rounded-xl border border-svt-outline/20 object-contain"
                  />
                )}

                <div className="grid gap-2 sm:grid-cols-2">
                  {q.options.map((opt, optIdx) => (
                    <div key={optIdx} className="flex items-center gap-2">
                      <input
                        type="radio"
                        name={`correct_${qIdx}`}
                        checked={q.correctIndex === optIdx}
                        onChange={() =>
                          setQuestions((prev) =>
                            prev.map((item, i) =>
                              i === qIdx ? { ...item, correctIndex: optIdx } : item
                            )
                          )
                        }
                        className="h-4 w-4 accent-svt-success"
                      />
                      <input
                        type="text"
                        value={opt}
                        onChange={(e) =>
                          setQuestions((prev) =>
                            prev.map((item, i) => {
                              if (i !== qIdx) return item;
                              const nextOpts = [...item.options];
                              nextOpts[optIdx] = e.target.value;
                              return { ...item, options: nextOpts };
                            })
                          )
                        }
                        placeholder={`Choix ${optIdx + 1}`}
                        className="min-h-[40px] flex-1 rounded-xl border border-svt-outline/20 bg-svt-surface px-2.5 text-xs"
                      />
                    </div>
                  ))}
                </div>

                <input
                  type="text"
                  value={q.explanation}
                  onChange={(e) =>
                    setQuestions((prev) =>
                      prev.map((item, i) =>
                        i === qIdx ? { ...item, explanation: e.target.value } : item
                      )
                    )
                  }
                  placeholder="Explication scientifique (affichée après validation)"
                  className="min-h-[40px] w-full rounded-xl border border-svt-outline/20 bg-svt-surface px-3 text-xs"
                />
              </div>
            ))}

            {questions.length < 20 && (
              <button
                type="button"
                onClick={() =>
                  setQuestions((prev) => [
                    ...prev,
                    {
                      questionText: '',
                      options: ['', '', '', ''],
                      correctIndex: 0,
                      explanation: '',
                    },
                  ])
                }
                className="flex min-h-[44px] w-full items-center justify-center gap-1.5 rounded-2xl border border-dashed border-svt-primary/40 text-xs font-bold text-svt-primary"
              >
                <Plus className="h-4 w-4" />
                <span>Ajouter une question</span>
              </button>
            )}
          </div>
        </div>

        <div className="flex justify-end gap-2 border-t border-svt-outline/20 p-4">
          <button
            type="button"
            onClick={onClose}
            className="min-h-[44px] rounded-xl px-4 text-sm font-bold text-svt-onSurfaceVariant"
          >
            {tr.cancel}
          </button>
          <button
            type="submit"
            disabled={busy}
            className="min-h-[44px] rounded-2xl bg-svt-primary px-6 text-sm font-bold text-svt-onPrimary disabled:opacity-50"
          >
            {busy ? tr.loading : 'Publier le QCM'}
          </button>
        </div>
      </form>
    </div>
  );
};
