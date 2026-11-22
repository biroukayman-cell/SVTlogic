import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Lock, MessageSquare, Send } from 'lucide-react';
import {
  AppLanguage,
  Enrollment,
  PrivateMessage,
  UserProfile,
} from '../types/svt';
import { TRANSLATIONS } from '../lib/i18n';
import { markMessagesRead, sendPrivateMessage } from '../lib/repository';

interface MessagingTabProps {
  isTeacher: boolean;
  isGuestMode: boolean;
  lang: AppLanguage;
  profile: UserProfile;
  enrollment: Enrollment | null;
  teacherEnrollments: Enrollment[];
  messages: PrivateMessage[];
  onGuestBlocked: () => void;
}

export const MessagingTab: React.FC<MessagingTabProps> = ({
  isTeacher,
  isGuestMode,
  lang,
  profile,
  enrollment,
  teacherEnrollments,
  messages,
  onGuestBlocked,
}) => {
  const tr = TRANSLATIONS[lang];
  const [selectedStudentId, setSelectedStudentId] = useState<string | null>(null);
  const [draft, setDraft] = useState('');
  const [busy, setBusy] = useState(false);
  const bottomRef = useRef<HTMLDivElement | null>(null);

  // Teacher thread list built from approved enrollments + existing messages
  const teacherThreads = useMemo(() => {
    if (!isTeacher) return [];
    const map = new Map<
      string,
      {
        studentId: string;
        studentName: string;
        classId: string;
        unreadCount: number;
        lastMessage?: PrivateMessage;
      }
    >();

    for (const enr of teacherEnrollments.filter((e) => e.status === 'approved')) {
      map.set(enr.studentId, {
        studentId: enr.studentId,
        studentName: enr.studentName || enr.massarNumber || 'Élève',
        classId: enr.classId,
        unreadCount: 0,
      });
    }

    for (const m of messages) {
      const prev = map.get(m.studentId) || {
        studentId: m.studentId,
        studentName: m.studentName || 'Élève',
        classId: m.classId,
        unreadCount: 0,
      };
      if (m.senderRole === 'student' && !m.isRead) {
        prev.unreadCount += 1;
      }
      if (!prev.lastMessage || m.createdAtMillis >= prev.lastMessage.createdAtMillis) {
        prev.lastMessage = m;
      }
      map.set(m.studentId, prev);
    }

    return Array.from(map.values()).sort(
      (a, b) =>
        (b.lastMessage?.createdAtMillis || 0) - (a.lastMessage?.createdAtMillis || 0) ||
        a.studentName.localeCompare(b.studentName)
    );
  }, [isTeacher, teacherEnrollments, messages]);

  const activeThreadId = isTeacher
    ? selectedStudentId || teacherThreads[0]?.studentId || null
    : profile.userId;

  const activeThreadInfo = teacherThreads.find((t) => t.studentId === activeThreadId);

  const activeMessages = useMemo(() => {
    if (!activeThreadId) return [];
    return messages.filter((m) => m.studentId === activeThreadId);
  }, [messages, activeThreadId]);

  useEffect(() => {
    if (isGuestMode) return;
    const unreadIds = activeMessages
      .filter((m) => !m.isRead && m.senderId !== profile.userId)
      .map((m) => m.id);
    if (unreadIds.length > 0) {
      markMessagesRead(unreadIds).catch(() => {});
    }
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [activeMessages.length, activeThreadId, isGuestMode]);

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isGuestMode) {
      onGuestBlocked();
      return;
    }
    if (!draft.trim()) return;

    const classId = isTeacher ? activeThreadInfo?.classId || '' : enrollment?.classId || '';
    const teacherId = isTeacher ? profile.userId : enrollment?.teacherId || '';
    const studentId = isTeacher ? activeThreadId || '' : profile.userId;
    const studentName = isTeacher
      ? activeThreadInfo?.studentName || 'Élève'
      : profile.fullName;

    if (!classId || !teacherId || !studentId) return;

    setBusy(true);
    try {
      await sendPrivateMessage({
        classId,
        teacherId,
        studentId,
        studentName,
        senderId: profile.userId,
        senderRole: profile.role,
        text: draft,
      });
      setDraft('');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="space-y-3">
      <div className="rounded-3xl bg-svt-surface border border-svt-outline/20 p-4 shadow-sm">
        <div className="flex items-center gap-2 font-display text-base font-bold text-svt-primary">
          <Lock className="h-4 w-4" />
          <span>
            {isTeacher
              ? `${tr.messagingTeacherInbox} (${teacherThreads.length})`
              : tr.messagingStudentHeader}
          </span>
        </div>
        <p className="mt-0.5 text-xs text-svt-onSurfaceVariant">{tr.messagingStudentNotice}</p>
      </div>

      {isTeacher && teacherThreads.length > 0 && (
        <div className="flex gap-2 overflow-x-auto pb-1 no-scrollbar">
          {teacherThreads.map((t) => (
            <button
              key={t.studentId}
              type="button"
              onClick={() => setSelectedStudentId(t.studentId)}
              className={`flex min-h-[40px] shrink-0 items-center gap-1.5 rounded-2xl px-3.5 text-xs font-bold transition ${
                activeThreadId === t.studentId
                  ? 'bg-svt-primary text-svt-onPrimary'
                  : 'border border-svt-outline/25 bg-svt-surface text-svt-onSurfaceVariant'
              }`}
            >
              <span>{t.studentName}</span>
              {t.unreadCount > 0 && (
                <span className="rounded-full bg-svt-error px-1.5 py-0.5 text-[10px] text-white">
                  {t.unreadCount}
                </span>
              )}
            </button>
          ))}
        </div>
      )}

      <div className="flex h-[52dvh] min-h-[320px] flex-col overflow-hidden rounded-3xl border border-svt-outline/20 bg-svt-surface shadow-sm">
        <div className="flex-1 space-y-2.5 overflow-y-auto p-4">
          {activeMessages.length === 0 ? (
            <div className="flex h-full flex-col items-center justify-center gap-2 text-center text-xs text-svt-onSurfaceVariant">
              <MessageSquare className="h-8 w-8 opacity-40" />
              <p>{tr.messagingEmpty}</p>
            </div>
          ) : (
            activeMessages.map((m) => {
              const mine = m.senderId === profile.userId;
              return (
                <div
                  key={m.id}
                  className={`flex ${mine ? 'justify-end' : 'justify-start'}`}
                >
                  <div
                    className={`max-w-[82%] rounded-2xl px-3.5 py-2.5 text-sm shadow-sm ${
                      mine
                        ? 'rounded-ee-sm bg-svt-primary text-svt-onPrimary'
                        : 'rounded-es-sm bg-svt-surfaceVariant text-svt-onSurface'
                    }`}
                  >
                    <p className="leading-relaxed whitespace-pre-wrap">{m.text}</p>
                    <div
                      className={`mt-1 text-[10px] ${
                        mine ? 'text-white/75' : 'text-svt-onSurfaceVariant'
                      }`}
                    >
                      {new Date(m.createdAtMillis).toLocaleTimeString([], {
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </div>
                  </div>
                </div>
              );
            })
          )}
          <div ref={bottomRef} />
        </div>

        <form
          onSubmit={handleSend}
          className="flex items-center gap-2 border-t border-svt-outline/20 bg-svt-bg p-2.5"
        >
          <input
            type="text"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            placeholder={
              isGuestMode ? tr.guestBlockedMsg : tr.messagingInputHint
            }
            disabled={isGuestMode || (isTeacher && !activeThreadId)}
            className="min-h-[44px] flex-1 rounded-2xl border border-svt-outline/25 bg-svt-surface px-3.5 text-sm outline-none focus:border-svt-primary disabled:opacity-60"
          />
          <button
            type="submit"
            disabled={busy || isGuestMode || !draft.trim() || (isTeacher && !activeThreadId)}
            className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-svt-primary text-svt-onPrimary disabled:opacity-45"
            aria-label={tr.messagingSend}
          >
            <Send className="h-4 w-4" />
          </button>
        </form>
      </div>
    </div>
  );
};
