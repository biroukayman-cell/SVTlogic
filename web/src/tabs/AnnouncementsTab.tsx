import React, { useMemo, useState } from 'react';
import { Megaphone, Plus, Trash2 } from 'lucide-react';
import { Announcement, AppLanguage, Enrollment, SvtClass, UserProfile } from '../types/svt';
import { TRANSLATIONS } from '../lib/i18n';
import { deleteAnnouncement, publishAnnouncement } from '../lib/repository';

interface AnnouncementsTabProps {
  isTeacher: boolean;
  lang: AppLanguage;
  profile: UserProfile;
  selectedClassId: string | null;
  teacherClasses: SvtClass[];
  enrollments: Enrollment[];
  announcements: Announcement[];
}

export const AnnouncementsTab: React.FC<AnnouncementsTabProps> = ({
  isTeacher,
  lang,
  profile,
  selectedClassId,
  teacherClasses,
  enrollments,
  announcements,
}) => {
  const tr = TRANSLATIONS[lang];
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [targetClassId, setTargetClassId] = useState(selectedClassId || teacherClasses[0]?.id || '');
  const [busy, setBusy] = useState(false);

  const visible = useMemo(() => {
    if (isTeacher && selectedClassId) {
      return announcements.filter((a) => a.classId === selectedClassId);
    }
    return announcements;
  }, [announcements, isTeacher, selectedClassId]);

  const handlePublish = async (e: React.FormEvent) => {
    e.preventDefault();
    const clsId = targetClassId || selectedClassId || teacherClasses[0]?.id;
    if (!clsId || title.trim().length < 2 || !content.trim()) return;
    setBusy(true);
    try {
      await publishAnnouncement({
        teacherId: profile.userId,
        classId: clsId,
        enrollments,
        title,
        content,
      });
      setTitle('');
      setContent('');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="space-y-4">
      <div className="rounded-3xl bg-gradient-to-r from-svt-tertiary to-svt-primary p-5 text-white shadow-sm">
        <h2 className="font-display text-xl font-extrabold">{tr.tabAnnouncements}</h2>
        <p className="mt-0.5 text-xs text-white/85">Informations & devoirs de la classe</p>
      </div>

      {isTeacher && teacherClasses.length > 0 && (
        <form
          onSubmit={handlePublish}
          className="space-y-2.5 rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 shadow-sm"
        >
          <div className="flex items-center gap-2 font-display text-sm font-bold text-svt-primary">
            <Plus className="h-4 w-4" />
            <span>{tr.announcementNew}</span>
          </div>
          <select
            value={targetClassId || selectedClassId || teacherClasses[0]?.id}
            onChange={(e) => setTargetClassId(e.target.value)}
            className="min-h-[44px] w-full rounded-2xl border border-svt-outline/25 bg-svt-bg px-3 text-sm"
          >
            {teacherClasses.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
          <input
            type="text"
            required
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Titre de l’annonce *"
            className="min-h-[44px] w-full rounded-2xl border border-svt-outline/25 bg-svt-bg px-3 text-sm"
          />
          <textarea
            required
            rows={3}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="Contenu du message pour la classe..."
            className="w-full rounded-2xl border border-svt-outline/25 bg-svt-bg p-3 text-sm"
          />
          <button
            type="submit"
            disabled={busy}
            className="min-h-[44px] rounded-2xl bg-svt-primary px-5 text-sm font-bold text-svt-onPrimary disabled:opacity-50"
          >
            {busy ? tr.loading : tr.announcementPublish}
          </button>
        </form>
      )}

      {visible.length === 0 ? (
        <div className="rounded-3xl border border-dashed border-svt-outline/30 bg-svt-surface p-8 text-center text-sm text-svt-onSurfaceVariant">
          {tr.announcementsEmpty}
        </div>
      ) : (
        <div className="space-y-3">
          {visible.map((ann) => (
            <div
              key={ann.id}
              className="rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 shadow-sm"
            >
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-2.5">
                  <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-svt-tertiaryContainer text-svt-tertiary">
                    <Megaphone className="h-4 w-4" />
                  </div>
                  <div>
                    <h3 className="font-display text-base font-bold">{ann.title}</h3>
                    <p className="text-[11px] text-svt-onSurfaceVariant">
                      {new Date(ann.createdAtMillis).toLocaleString()}
                    </p>
                  </div>
                </div>

                {isTeacher && (
                  <button
                    type="button"
                    onClick={() => {
                      if (confirm(`Supprimer l’annonce « ${ann.title} » ?`)) {
                        deleteAnnouncement(ann.id);
                      }
                    }}
                    className="flex h-9 w-9 items-center justify-center rounded-xl text-svt-error hover:bg-svt-error/10"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                )}
              </div>
              <p className="mt-2.5 text-sm leading-relaxed whitespace-pre-line text-svt-onSurfaceVariant">
                {ann.content}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
