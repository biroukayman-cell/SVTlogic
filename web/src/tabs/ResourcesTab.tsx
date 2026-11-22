import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  BarChart3,
  CheckCircle2,
  Download,
  FileText,
  Film,
  Image as ImageIcon,
  Plus,
  Trash2,
  Upload,
  X,
  ZoomIn,
  ZoomOut,
} from 'lucide-react';
import {
  AppLanguage,
  CourseResource,
  Enrollment,
  ResourceType,
  SvtClass,
  UserProfile,
  VideoTracking,
} from '../types/svt';
import { TRANSLATIONS } from '../lib/i18n';
import { extractDocxText } from '../lib/fileUtils';
import {
  deleteResource,
  markResourceOpened,
  publishResource,
  resolveResourceBlobUrl,
  updateVideoTrackingProgress,
} from '../lib/repository';

interface ResourcesTabProps {
  isTeacher: boolean;
  isGuestMode: boolean;
  lang: AppLanguage;
  profile: UserProfile;
  selectedClassId: string | null;
  teacherClasses: SvtClass[];
  enrollments: Enrollment[];
  resources: CourseResource[];
  videoTracking: VideoTracking[];
  onGuestBlocked: () => void;
}

export const ResourcesTab: React.FC<ResourcesTabProps> = ({
  isTeacher,
  isGuestMode,
  lang,
  profile,
  selectedClassId,
  teacherClasses,
  enrollments,
  resources,
  videoTracking,
  onGuestBlocked,
}) => {
  const tr = TRANSLATIONS[lang];
  const [typeFilter, setTypeFilter] = useState<'ALL' | ResourceType>('ALL');
  const [activeResource, setActiveResource] = useState<CourseResource | null>(null);
  const [trackingModalRes, setTrackingModalRes] = useState<CourseResource | null>(null);
  const [publishOpen, setPublishOpen] = useState(false);

  const visibleResources = useMemo(() => {
    const byClass =
      isTeacher && selectedClassId
        ? resources.filter((r) => r.classId === selectedClassId)
        : resources;
    if (typeFilter === 'ALL') return byClass;
    return byClass.filter((r) => r.type === typeFilter);
  }, [resources, isTeacher, selectedClassId, typeFilter]);

  const myTrackingByRes = useMemo(() => {
    const map = new Map<string, VideoTracking>();
    for (const vt of videoTracking) {
      if (vt.studentId === profile.userId) map.set(vt.resourceId, vt);
    }
    return map;
  }, [videoTracking, profile.userId]);

  return (
    <div className="space-y-4">
      <div className="rounded-3xl bg-gradient-to-r from-svt-secondary to-svt-primary p-5 text-white shadow-sm">
        <h2 className="font-display text-xl font-extrabold">{tr.tabResources}</h2>
        <p className="mt-0.5 text-xs text-white/85">Vidéos MP4 (suivi ≥90%), PDF, Word & Schémas</p>
      </div>

      {isTeacher && (
        <button
          type="button"
          disabled={teacherClasses.length === 0}
          onClick={() => setPublishOpen(true)}
          className="flex min-h-[44px] items-center gap-1.5 rounded-2xl bg-svt-primary px-4 text-sm font-bold text-svt-onPrimary shadow-sm disabled:opacity-50"
        >
          <Plus className="h-4 w-4" />
          <span>{tr.publishResource}</span>
        </button>
      )}

      {/* Filter Pills */}
      <div className="flex gap-2 overflow-x-auto no-scrollbar">
        {(['ALL', 'VIDEO', 'PDF', 'WORD', 'IMAGE'] as const).map((t) => (
          <button
            key={t}
            type="button"
            onClick={() => setTypeFilter(t)}
            className={`min-h-[38px] shrink-0 rounded-2xl px-4 text-xs font-bold transition ${
              typeFilter === t
                ? 'bg-svt-primary text-svt-onPrimary'
                : 'border border-svt-outline/25 bg-svt-surface text-svt-onSurfaceVariant'
            }`}
          >
            {t === 'ALL'
              ? tr.allFilter
              : t === 'VIDEO'
              ? tr.typeVideo
              : t === 'PDF'
              ? tr.typePdf
              : t === 'WORD'
              ? tr.typeWord
              : tr.typeImage}
          </button>
        ))}
      </div>

      {visibleResources.length === 0 ? (
        <div className="rounded-3xl border border-dashed border-svt-outline/30 bg-svt-surface p-8 text-center text-sm text-svt-onSurfaceVariant">
          {tr.resourcesEmpty}
        </div>
      ) : (
        <div className="grid gap-3 md:grid-cols-2">
          {visibleResources.map((res) => {
            const vt = !isTeacher ? myTrackingByRes.get(res.id) : undefined;
            const resTrackings = isTeacher
              ? videoTracking.filter((v) => v.resourceId === res.id)
              : [];

            return (
              <div
                key={res.id}
                className="flex flex-col justify-between rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 shadow-sm"
              >
                <div>
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-svt-primaryContainer text-svt-primary">
                        {res.type === 'VIDEO' ? (
                          <Film className="h-4 w-4" />
                        ) : res.type === 'IMAGE' ? (
                          <ImageIcon className="h-4 w-4" />
                        ) : (
                          <FileText className="h-4 w-4" />
                        )}
                      </span>
                      <div>
                        <span className="text-[10px] font-bold uppercase text-svt-secondary">
                          {res.type}
                        </span>
                        <h3 className="font-display text-sm font-bold leading-snug">{res.title}</h3>
                      </div>
                    </div>

                    {isTeacher && (
                      <button
                        type="button"
                        onClick={() => {
                          if (confirm(`Supprimer « ${res.title} » ?`)) {
                            deleteResource(res.id);
                          }
                        }}
                        className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl text-svt-error hover:bg-svt-error/10"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    )}
                  </div>

                  {res.description && (
                    <p className="mt-2 text-xs text-svt-onSurfaceVariant">{res.description}</p>
                  )}
                </div>

                <div className="mt-4 flex flex-wrap items-center justify-between gap-2 border-t border-svt-outline/15 pt-3">
                  {!isTeacher ? (
                    <span
                      className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-[11px] font-bold ${
                        vt?.status === 'completed'
                          ? 'bg-svt-success/15 text-svt-success'
                          : vt?.status === 'partial'
                          ? 'bg-svt-warning/15 text-svt-warning'
                          : 'bg-svt-surfaceVariant/70 text-svt-onSurfaceVariant'
                      }`}
                    >
                      {vt?.status === 'completed' && <CheckCircle2 className="h-3.5 w-3.5" />}
                      <span>
                        {res.type === 'VIDEO'
                          ? vt
                            ? `${vt.completionPercentage}%`
                            : tr.videoNotStarted
                          : vt?.status === 'completed'
                          ? tr.fileOpened
                          : tr.fileNotOpened}
                      </span>
                    </span>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setTrackingModalRes(res)}
                      className="flex min-h-[36px] items-center gap-1 rounded-xl bg-svt-secondaryContainer px-3 text-xs font-bold text-svt-onSecondaryContainer"
                    >
                      <BarChart3 className="h-3.5 w-3.5" />
                      <span>Suivi ({resTrackings.length})</span>
                    </button>
                  )}

                  <button
                    type="button"
                    onClick={() => setActiveResource(res)}
                    className="min-h-[40px] rounded-2xl bg-svt-primary px-4 text-xs font-bold text-svt-onPrimary"
                  >
                    {res.type === 'VIDEO' ? tr.watchVideo : tr.openDocument}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {activeResource && (
        <ResourceViewerModal
          resource={activeResource}
          profile={profile}
          isTeacher={isTeacher}
          isGuestMode={isGuestMode}
          existingTracking={myTrackingByRes.get(activeResource.id)}
          onGuestBlocked={onGuestBlocked}
          onClose={() => setActiveResource(null)}
        />
      )}

      {trackingModalRes && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/65 p-4 backdrop-blur-sm">
          <div className="flex max-h-[85dvh] w-full max-w-md flex-col overflow-hidden rounded-3xl bg-svt-surface text-svt-onSurface shadow-2xl">
            <div className="flex items-center justify-between border-b border-svt-outline/20 px-5 py-4">
              <h3 className="font-display text-base font-bold">{trackingModalRes.title}</h3>
              <button
                type="button"
                onClick={() => setTrackingModalRes(null)}
                className="flex h-9 w-9 items-center justify-center rounded-full hover:bg-svt-surfaceVariant"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <div className="flex-1 space-y-2 overflow-y-auto p-4">
              {videoTracking
                .filter((v) => v.resourceId === trackingModalRes.id)
                .map((v) => (
                  <div
                    key={v.id}
                    className="flex items-center justify-between rounded-2xl border border-svt-outline/20 bg-svt-bg p-3 text-xs"
                  >
                    <span className="font-bold">{v.studentName}</span>
                    <span
                      className={`rounded-xl px-2.5 py-1 font-bold ${
                        v.status === 'completed'
                          ? 'bg-svt-success/15 text-svt-success'
                          : 'bg-svt-warning/15 text-svt-warning'
                      }`}
                    >
                      {v.completionPercentage}% ({v.status})
                    </span>
                  </div>
                ))}
              {videoTracking.filter((v) => v.resourceId === trackingModalRes.id).length === 0 && (
                <p className="py-6 text-center text-sm text-svt-onSurfaceVariant">
                  Aucune activité enregistrée pour le moment.
                </p>
              )}
            </div>
          </div>
        </div>
      )}

      {publishOpen && (
        <PublishResourceModal
          classes={teacherClasses}
          defaultClassId={selectedClassId}
          teacherId={profile.userId}
          enrollments={enrollments}
          onClose={() => setPublishOpen(false)}
        />
      )}
    </div>
  );
};

const ResourceViewerModal: React.FC<{
  resource: CourseResource;
  profile: UserProfile;
  isTeacher: boolean;
  isGuestMode: boolean;
  existingTracking?: VideoTracking;
  onGuestBlocked: () => void;
  onClose: () => void;
}> = ({ resource, profile, isTeacher, isGuestMode, existingTracking, onClose }) => {
  const [resolvedUrl, setResolvedUrl] = useState<string>('');
  const [docxText, setDocxText] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [zoom, setZoom] = useState(1);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const lastSavedRef = useRef(0);

  useEffect(() => {
    let createdObjectUrl: string | null = null;
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setErr(null);
      try {
        const res = await resolveResourceBlobUrl(resource);
        if (cancelled) {
          if (res.isObjectUrl) URL.revokeObjectURL(res.url);
          return;
        }
        if (res.isObjectUrl) createdObjectUrl = res.url;
        setResolvedUrl(res.url);

        if (resource.type === 'WORD') {
          let buf = res.buffer;
          if (!buf && res.url.startsWith('http')) {
            const resp = await fetch(res.url);
            buf = await resp.arrayBuffer();
          }
          if (buf) {
            const text = await extractDocxText(buf);
            setDocxText(text || 'Document Word vide.');
          }
        }

        if (!isTeacher && !isGuestMode && resource.type !== 'VIDEO') {
          markResourceOpened(resource, profile, existingTracking).catch(() => {});
        }
      } catch (e: any) {
        if (!cancelled) setErr(e?.message || 'Impossible de charger cette ressource.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    load();
    return () => {
      cancelled = true;
      if (createdObjectUrl) URL.revokeObjectURL(createdObjectUrl);
    };
  }, [resource.id]);

  const syncVideoProgress = (force = false) => {
    if (isTeacher || isGuestMode) return;
    const vid = videoRef.current;
    if (!vid) return;
    const now = Date.now();
    if (!force && now - lastSavedRef.current < 4500) return;
    lastSavedRef.current = now;
    const dur = Number.isFinite(vid.duration) && vid.duration > 0 ? vid.duration : resource.durationSeconds || 1;
    updateVideoTrackingProgress({
      resource,
      profile,
      currentSeconds: vid.currentTime,
      durationSeconds: dur,
      existing: existingTracking,
    }).catch(() => {});
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-3 backdrop-blur-sm">
      <div className="flex max-h-[92dvh] w-full max-w-3xl flex-col overflow-hidden rounded-3xl bg-svt-surface text-svt-onSurface shadow-2xl">
        <div className="flex items-center justify-between border-b border-svt-outline/20 px-5 py-3.5">
          <div className="min-w-0">
            <h3 className="truncate font-display text-base font-bold">{resource.title}</h3>
            <p className="text-[11px] text-svt-onSurfaceVariant">{resource.type}</p>
          </div>
          <div className="flex items-center gap-2">
            {resolvedUrl && (resource.isDownloadable || isTeacher) && (
              <a
                href={resolvedUrl}
                download={resource.title}
                target="_blank"
                rel="noreferrer"
                className="flex h-9 items-center gap-1 rounded-xl bg-svt-primaryContainer px-3 text-xs font-bold text-svt-onPrimaryContainer"
              >
                <Download className="h-3.5 w-3.5" />
                <span>Télécharger</span>
              </a>
            )}
            <button
              type="button"
              onClick={() => {
                syncVideoProgress(true);
                onClose();
              }}
              className="flex h-9 w-9 items-center justify-center rounded-full hover:bg-svt-surfaceVariant"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-auto p-4">
          {loading && <div className="py-12 text-center text-sm">Chargement du support...</div>}
          {err && (
            <div className="rounded-2xl bg-svt-error/10 p-4 text-center text-xs font-bold text-svt-error">
              {err}
            </div>
          )}

          {!loading && !err && resource.type === 'VIDEO' && (
            <video
              ref={videoRef}
              src={resolvedUrl}
              controls
              playsInline
              className="max-h-[65dvh] w-full rounded-2xl bg-black"
              onTimeUpdate={() => syncVideoProgress(false)}
              onPause={() => syncVideoProgress(true)}
              onSeeked={() => syncVideoProgress(true)}
              onEnded={() => syncVideoProgress(true)}
            />
          )}

          {!loading && !err && resource.type === 'PDF' && (
            <div className="space-y-3">
              <iframe
                src={resolvedUrl}
                title={resource.title}
                className="h-[65dvh] w-full rounded-2xl border border-svt-outline/20 bg-white"
              />
              <div className="text-center">
                <a
                  href={resolvedUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl bg-svt-secondary px-4 text-xs font-bold text-svt-onSecondary"
                >
                  Ouvrir le PDF dans un nouvel onglet
                </a>
              </div>
            </div>
          )}

          {!loading && !err && resource.type === 'IMAGE' && (
            <div className="space-y-3 text-center">
              <div className="flex justify-center gap-2">
                <button
                  type="button"
                  onClick={() => setZoom((z) => Math.max(0.5, z - 0.25))}
                  className="rounded-xl border border-svt-outline/25 px-3 py-1.5 text-xs font-bold"
                >
                  <ZoomOut className="inline h-4 w-4" />
                </button>
                <button
                  type="button"
                  onClick={() => setZoom(1)}
                  className="rounded-xl border border-svt-outline/25 px-3 py-1.5 text-xs font-bold"
                >
                  {Math.round(zoom * 100)}%
                </button>
                <button
                  type="button"
                  onClick={() => setZoom((z) => Math.min(3, z + 0.25))}
                  className="rounded-xl border border-svt-outline/25 px-3 py-1.5 text-xs font-bold"
                >
                  <ZoomIn className="inline h-4 w-4" />
                </button>
              </div>
              <div className="overflow-auto rounded-2xl bg-svt-bg p-2">
                <img
                  src={resolvedUrl}
                  alt={resource.title}
                  className="mx-auto transition-transform"
                  style={{ transform: `scale(${zoom})`, transformOrigin: 'center top' }}
                />
              </div>
            </div>
          )}

          {!loading && !err && resource.type === 'WORD' && (
            <div className="rounded-2xl border border-svt-outline/20 bg-svt-bg p-4 text-sm leading-relaxed whitespace-pre-line">
              {docxText || 'Aperçu Word indisponible. Utilisez Télécharger.'}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

const PublishResourceModal: React.FC<{
  classes: SvtClass[];
  defaultClassId: string | null;
  teacherId: string;
  enrollments: Enrollment[];
  onClose: () => void;
}> = ({ classes, defaultClassId, teacherId, enrollments, onClose }) => {
  const [classId, setClassId] = useState(defaultClassId || classes[0]?.id || '');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [type, setType] = useState<ResourceType>('PDF');
  const [externalUrl, setExternalUrl] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [durationSeconds, setDurationSeconds] = useState(0);
  const [isDownloadable, setIsDownloadable] = useState(true);
  const [progress, setProgress] = useState(0);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErr(null);
    if (!classId || title.trim().length < 2) {
      setErr('Sélectionnez une classe et saisissez un titre.');
      return;
    }
    setBusy(true);
    try {
      await publishResource({
        teacherId,
        classId,
        enrollments,
        title,
        description,
        type,
        externalUrl,
        file,
        durationSeconds,
        isDownloadable,
        onProgress: setProgress,
      });
      onClose();
    } catch (e: any) {
      setErr(e?.message || 'Erreur lors de la publication.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4 backdrop-blur-sm">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-md space-y-3.5 rounded-3xl bg-svt-surface p-5 text-svt-onSurface shadow-2xl"
      >
        <div className="flex items-center justify-between">
          <h3 className="font-display text-lg font-bold">Publier un support de cours</h3>
          <button type="button" onClick={onClose} className="p-1">
            <X className="h-5 w-5" />
          </button>
        </div>

        {err && (
          <div className="rounded-xl bg-svt-error/10 p-3 text-xs font-semibold text-svt-error">
            {err}
          </div>
        )}

        <select
          value={classId}
          onChange={(e) => setClassId(e.target.value)}
          className="min-h-[46px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
        >
          {classes.map((c) => (
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
          placeholder="Titre du cours ou vidéo *"
          className="min-h-[46px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
        />

        <input
          type="text"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Description (optionnel)"
          className="min-h-[44px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
        />

        <div className="grid grid-cols-4 gap-1.5">
          {(['PDF', 'VIDEO', 'WORD', 'IMAGE'] as ResourceType[]).map((t) => (
            <button
              key={t}
              type="button"
              onClick={() => setType(t)}
              className={`min-h-[38px] rounded-xl text-xs font-bold ${
                type === t
                  ? 'bg-svt-primary text-svt-onPrimary'
                  : 'border border-svt-outline/25 bg-svt-bg'
              }`}
            >
              {t}
            </button>
          ))}
        </div>

        {type === 'VIDEO' && (
          <input
            type="number"
            min={0}
            value={durationSeconds}
            onChange={(e) => setDurationSeconds(Number(e.target.value) || 0)}
            placeholder="Durée vidéo (secondes)"
            className="min-h-[44px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
          />
        )}

        <input
          type="url"
          value={externalUrl}
          onChange={(e) => setExternalUrl(e.target.value)}
          placeholder="Lien HTTPS (ou choisissez un fichier ci-dessous)"
          className="min-h-[44px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
        />

        <label className="flex min-h-[46px] cursor-pointer items-center justify-center gap-2 rounded-2xl border border-dashed border-svt-primary/50 bg-svt-primaryContainer/30 px-3 text-xs font-bold text-svt-primary">
          <Upload className="h-4 w-4" />
          <span>{file ? `${file.name} (${Math.round(file.size / 1024)} Ko)` : 'Choisir un fichier (max 20 Mo)'}</span>
          <input
            type="file"
            className="hidden"
            onChange={(e) => setFile(e.target.files?.[0] || null)}
          />
        </label>

        <label className="flex items-center gap-2 text-xs font-semibold">
          <input
            type="checkbox"
            checked={isDownloadable}
            onChange={(e) => setIsDownloadable(e.target.checked)}
            className="h-4 w-4 accent-svt-primary"
          />
          <span>Autoriser le téléchargement hors-ligne</span>
        </label>

        {busy && progress > 0 && (
          <div className="text-xs font-bold text-svt-primary">Envoi en cours : {progress}%</div>
        )}

        <div className="flex justify-end gap-2 pt-1">
          <button type="button" onClick={onClose} className="min-h-[44px] px-4 text-sm font-bold">
            Annuler
          </button>
          <button
            type="submit"
            disabled={busy}
            className="min-h-[44px] rounded-2xl bg-svt-primary px-5 text-sm font-bold text-svt-onPrimary disabled:opacity-50"
          >
            {busy ? 'Envoi...' : 'Publier'}
          </button>
        </div>
      </form>
    </div>
  );
};
