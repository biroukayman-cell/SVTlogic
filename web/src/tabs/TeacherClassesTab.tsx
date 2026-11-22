import React, { useMemo, useState } from 'react';
import {
  Check,
  CheckCheck,
  FileSpreadsheet,
  Plus,
  Printer,
  QrCode,
  ShieldCheck,
  Smartphone,
  Trash2,
  UserPlus,
  X,
} from 'lucide-react';
import {
  ALL_SVT_DOMAINS,
  AppLanguage,
  Enrollment,
  RosterStudent,
  SvtClass,
  SvtDomain,
  UserProfile,
} from '../types/svt';
import { DOMAIN_LABELS, TRANSLATIONS } from '../lib/i18n';
import { OWNER_ADMIN_EMAIL, normalizeMassar } from '../lib/authHelpers';
import { parseRosterSpreadsheet, printRosterCredentialCards } from '../lib/fileUtils';
import {
  adminSetTeacherStatus,
  approveAllPendingEnrollments,
  approveOrRejectEnrollment,
  createSvtClass,
  deleteRosterStudent,
  provisionRosterEntries,
  removeEnrollmentAndData,
  unlinkStudentDevice,
  updateClassLabDomains,
} from '../lib/repository';
import { QrDisplayModal } from '../components/QrDisplayModal';

interface TeacherClassesTabProps {
  lang: AppLanguage;
  profile: UserProfile;
  classes: SvtClass[];
  selectedClassId: string | null;
  onSelectClassId: (id: string) => void;
  enrollments: Enrollment[];
  roster: RosterStudent[];
  adminTeachers: UserProfile[];
}

export const TeacherClassesTab: React.FC<TeacherClassesTabProps> = ({
  lang,
  profile,
  classes,
  selectedClassId,
  onSelectClassId,
  enrollments,
  roster,
  adminTeachers,
}) => {
  const tr = TRANSLATIONS[lang];
  const isOwnerAdmin = profile.email.toLowerCase() === OWNER_ADMIN_EMAIL;

  // Create class state
  const [newClassName, setNewClassName] = useState('');
  const [newLevel, setNewLevel] = useState('2BAC SVT');
  const [newYear, setNewYear] = useState('2025-2026');
  const [qrModalClass, setQrModalClass] = useState<SvtClass | null>(null);

  // Manual roster student
  const [manualName, setManualName] = useState('');
  const [manualMassar, setManualMassar] = useState('');
  const [busy, setBusy] = useState(false);
  const [statusMsg, setStatusMsg] = useState<string | null>(null);

  const activeClass = useMemo(
    () => classes.find((c) => c.id === selectedClassId) || classes[0] || null,
    [classes, selectedClassId]
  );

  const pendingEnrollments = useMemo(
    () =>
      enrollments.filter(
        (e) => e.status === 'pending' && (!activeClass || e.classId === activeClass.id)
      ),
    [enrollments, activeClass]
  );

  const approvedEnrollments = useMemo(
    () =>
      enrollments.filter(
        (e) => e.status === 'approved' && (!activeClass || e.classId === activeClass.id)
      ),
    [enrollments, activeClass]
  );

  const classRoster = useMemo(
    () => (activeClass ? roster.filter((r) => r.classId === activeClass.id) : []),
    [roster, activeClass]
  );

  const handleCreateClass = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newClassName.trim().length < 2) return;
    setBusy(true);
    try {
      const created = await createSvtClass({
        teacher: profile,
        name: newClassName,
        level: newLevel,
        academicYear: newYear,
      });
      setNewClassName('');
      onSelectClassId(created.id);
      setQrModalClass(created);
    } finally {
      setBusy(false);
    }
  };

  const handleImportSpreadsheet = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !activeClass) return;
    setBusy(true);
    setStatusMsg(null);
    try {
      const entries = await parseRosterSpreadsheet(file);
      if (entries.length === 0) {
        setStatusMsg('Aucun élève détecté dans ce fichier Excel/CSV.');
        return;
      }
      const count = await provisionRosterEntries({ svtClass: activeClass, entries });
      setStatusMsg(`${count} élève(s) importé(s) et comptes Massar provisionnés ✓`);
    } catch (err: any) {
      setStatusMsg(err?.message || 'Erreur lors de l’import.');
    } finally {
      setBusy(false);
      e.target.value = '';
    }
  };

  const handleAddManualStudent = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeClass) return;
    const cleanMassar = normalizeMassar(manualMassar);
    if (cleanMassar.length < 4 || manualName.trim().length < 2) return;
    setBusy(true);
    try {
      await provisionRosterEntries({
        svtClass: activeClass,
        entries: [{ massarNumber: cleanMassar, fullName: manualName, phoneNumber: '' }],
      });
      setManualName('');
      setManualMassar('');
      setStatusMsg(`Élève ${cleanMassar} ajouté ✓`);
    } finally {
      setBusy(false);
    }
  };

  const handleToggleDomain = async (domain: SvtDomain) => {
    if (!activeClass) return;
    const cur = activeClass.visibleLabDomains;
    const next = cur.includes(domain) ? cur.filter((d) => d !== domain) : [...cur, domain];
    if (next.length === 0) return;
    await updateClassLabDomains(activeClass.id, next);
  };

  return (
    <div className="space-y-4">
      {/* Create Class Form */}
      <form
        onSubmit={handleCreateClass}
        className="space-y-3 rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 shadow-sm"
      >
        <div className="flex items-center gap-2 font-display text-base font-bold text-svt-primary">
          <Plus className="h-4 w-4" />
          <span>{tr.createClassTitle}</span>
        </div>
        <div className="grid gap-2.5 sm:grid-cols-3">
          <input
            type="text"
            required
            value={newClassName}
            onChange={(e) => setNewClassName(e.target.value)}
            placeholder="Nom (ex : 2BAC SVT-1)"
            className="min-h-[44px] rounded-2xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
          />
          <input
            type="text"
            required
            value={newLevel}
            onChange={(e) => setNewLevel(e.target.value)}
            placeholder="Niveau (ex : 2BAC)"
            className="min-h-[44px] rounded-2xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
          />
          <input
            type="text"
            required
            value={newYear}
            onChange={(e) => setNewYear(e.target.value)}
            placeholder="Année (2025-2026)"
            className="min-h-[44px] rounded-2xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
          />
        </div>
        <button
          type="submit"
          disabled={busy}
          className="min-h-[44px] rounded-2xl bg-svt-primary px-5 text-sm font-bold text-svt-onPrimary disabled:opacity-50"
        >
          Créer la classe & générer le QR
        </button>
      </form>

      {/* Class Selector */}
      {classes.length > 0 && (
        <div className="flex gap-2 overflow-x-auto pb-1 no-scrollbar">
          {classes.map((c) => (
            <button
              key={c.id}
              type="button"
              onClick={() => onSelectClassId(c.id)}
              className={`min-h-[40px] shrink-0 rounded-2xl px-4 text-xs font-bold transition ${
                activeClass?.id === c.id
                  ? 'bg-svt-primary text-svt-onPrimary'
                  : 'border border-svt-outline/25 bg-svt-surface'
              }`}
            >
              {c.name}
            </button>
          ))}
        </div>
      )}

      {statusMsg && (
        <div className="rounded-2xl bg-svt-success/15 p-3 text-xs font-bold text-svt-success">
          {statusMsg}
        </div>
      )}

      {activeClass && (
        <>
          {/* Class QR + Visible Lab Domains */}
          <div className="space-y-3 rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <h3 className="font-display text-base font-bold">{activeClass.name}</h3>
                <p className="font-mono text-xs text-svt-secondary">
                  Code QR : {activeClass.qrToken}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setQrModalClass(activeClass)}
                className="flex min-h-[42px] items-center gap-1.5 rounded-2xl bg-svt-primaryContainer px-4 text-xs font-bold text-svt-onPrimaryContainer"
              >
                <QrCode className="h-4 w-4" />
                <span>Afficher QR</span>
              </button>
            </div>

            <div>
              <div className="mb-1.5 text-xs font-bold text-svt-onSurfaceVariant">
                {tr.labDomainsVisibility} :
              </div>
              <div className="flex flex-wrap gap-1.5">
                {ALL_SVT_DOMAINS.map((d) => {
                  const active = activeClass.visibleLabDomains.includes(d);
                  return (
                    <button
                      key={d}
                      type="button"
                      onClick={() => handleToggleDomain(d)}
                      className={`min-h-[34px] rounded-xl px-3 text-[11px] font-bold transition ${
                        active
                          ? 'bg-svt-secondary text-svt-onSecondary'
                          : 'border border-svt-outline/25 bg-svt-bg text-svt-onSurfaceVariant'
                      }`}
                    >
                      {DOMAIN_LABELS[d][lang]}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Pending Enrollments */}
          {pendingEnrollments.length > 0 && (
            <div className="space-y-2.5 rounded-3xl border border-svt-warning/40 bg-svt-warning/10 p-4">
              <div className="flex items-center justify-between">
                <h3 className="font-display text-sm font-bold text-svt-warning">
                  {tr.pendingEnrollments} ({pendingEnrollments.length})
                </h3>
                <button
                  type="button"
                  onClick={() =>
                    approveAllPendingEnrollments(pendingEnrollments, profile.userId)
                  }
                  className="flex min-h-[36px] items-center gap-1 rounded-xl bg-svt-success px-3 text-xs font-bold text-white"
                >
                  <CheckCheck className="h-3.5 w-3.5" />
                  <span>{tr.approveAll}</span>
                </button>
              </div>
              {pendingEnrollments.map((enr) => (
                <div
                  key={enr.studentId}
                  className="flex items-center justify-between rounded-2xl bg-svt-surface p-3 text-xs"
                >
                  <div>
                    <div className="font-bold">{enr.studentName}</div>
                    <div className="text-svt-onSurfaceVariant">{enr.massarNumber}</div>
                  </div>
                  <div className="flex gap-1.5">
                    <button
                      type="button"
                      onClick={() =>
                        approveOrRejectEnrollment(
                          enr.studentId,
                          'approved',
                          profile.userId,
                          enr.className
                        )
                      }
                      className="flex h-9 w-9 items-center justify-center rounded-xl bg-svt-success/15 text-svt-success"
                      title={tr.approve}
                    >
                      <Check className="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() =>
                        approveOrRejectEnrollment(
                          enr.studentId,
                          'rejected',
                          profile.userId,
                          enr.className
                        )
                      }
                      className="flex h-9 w-9 items-center justify-center rounded-xl bg-svt-error/15 text-svt-error"
                      title={tr.reject}
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Approved Students */}
          <div className="space-y-2.5 rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 shadow-sm">
            <h3 className="font-display text-sm font-bold">
              {tr.approvedStudents} ({approvedEnrollments.length})
            </h3>
            {approvedEnrollments.length === 0 ? (
              <p className="text-xs text-svt-onSurfaceVariant">
                Aucun élève inscrit pour le moment. Partagez le QR Code ci-dessus.
              </p>
            ) : (
              <div className="space-y-2">
                {approvedEnrollments.map((enr) => (
                  <div
                    key={enr.studentId}
                    className="flex flex-wrap items-center justify-between gap-2 rounded-2xl border border-svt-outline/15 bg-svt-bg p-3 text-xs"
                  >
                    <div>
                      <div className="font-bold">{enr.studentName}</div>
                      <div className="font-mono text-[11px] text-svt-onSurfaceVariant">
                        {enr.massarNumber || '—'}
                      </div>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <button
                        type="button"
                        onClick={async () => {
                          await unlinkStudentDevice(enr.studentId);
                          setStatusMsg(`Appareil de ${enr.studentName} délié ✓`);
                        }}
                        className="flex min-h-[34px] items-center gap-1 rounded-xl border border-svt-outline/25 px-2.5 text-[11px] font-bold hover:bg-svt-surfaceVariant"
                      >
                        <Smartphone className="h-3.5 w-3.5" />
                        <span>{tr.unlinkDevice}</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          if (confirm(`Retirer ${enr.studentName} de la classe ?`)) {
                            removeEnrollmentAndData(enr);
                          }
                        }}
                        className="flex h-8 w-8 items-center justify-center rounded-xl text-svt-error hover:bg-svt-error/10"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Roster Import + Manual Add + Printable Credential Cards */}
          <div className="space-y-3 rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h3 className="font-display text-sm font-bold">
                {tr.rosterTitle} ({classRoster.length})
              </h3>
              <div className="flex flex-wrap gap-2">
                <label className="flex min-h-[40px] cursor-pointer items-center gap-1.5 rounded-2xl bg-svt-secondaryContainer px-3.5 text-xs font-bold text-svt-onSecondaryContainer">
                  <FileSpreadsheet className="h-4 w-4" />
                  <span>{tr.importExcelCsv}</span>
                  <input
                    type="file"
                    accept=".xlsx,.xls,.csv"
                    className="hidden"
                    onChange={handleImportSpreadsheet}
                  />
                </label>
                {classRoster.length > 0 && (
                  <button
                    type="button"
                    onClick={() =>
                      printRosterCredentialCards(activeClass, classRoster).catch((e) =>
                        alert(e?.message)
                      )
                    }
                    className="flex min-h-[40px] items-center gap-1.5 rounded-2xl bg-svt-primary px-3.5 text-xs font-bold text-svt-onPrimary"
                  >
                    <Printer className="h-4 w-4" />
                    <span>{tr.printCards}</span>
                  </button>
                )}
              </div>
            </div>

            <form onSubmit={handleAddManualStudent} className="grid gap-2 sm:grid-cols-3">
              <input
                type="text"
                required
                value={manualMassar}
                onChange={(e) => setManualMassar(normalizeMassar(e.target.value))}
                placeholder="Massar (ex: G134567890)"
                className="min-h-[42px] rounded-xl border border-svt-outline/30 bg-svt-bg px-3 font-mono text-xs uppercase"
              />
              <input
                type="text"
                required
                value={manualName}
                onChange={(e) => setManualName(e.target.value)}
                placeholder="Nom complet élève"
                className="min-h-[42px] rounded-xl border border-svt-outline/30 bg-svt-bg px-3 text-xs"
              />
              <button
                type="submit"
                disabled={busy}
                className="flex min-h-[42px] items-center justify-center gap-1 rounded-xl bg-svt-primary text-xs font-bold text-svt-onPrimary disabled:opacity-50"
              >
                <UserPlus className="h-4 w-4" />
                <span>{tr.addStudentManual}</span>
              </button>
            </form>

            {classRoster.length > 0 && (
              <div className="max-h-64 space-y-1.5 overflow-y-auto pt-1">
                {classRoster.map((r) => (
                  <div
                    key={r.id}
                    className="flex items-center justify-between rounded-xl border border-svt-outline/15 bg-svt-bg px-3 py-2 text-xs"
                  >
                    <div className="min-w-0">
                      <div className="truncate font-bold">{r.fullName}</div>
                      <div className="font-mono text-[11px] text-svt-onSurfaceVariant">
                        {r.massarNumber} • MDP : <strong>{r.provisionalPassword}</strong>
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => deleteRosterStudent(r.id)}
                      className="p-1.5 text-svt-error"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </>
      )}

      {/* Owner Admin Teacher Approvals */}
      {isOwnerAdmin && (
        <div className="space-y-2.5 rounded-3xl border-2 border-svt-primary/30 bg-svt-primaryContainer/30 p-4">
          <div className="flex items-center gap-2 font-display text-sm font-bold text-svt-primary">
            <ShieldCheck className="h-5 w-5" />
            <span>{tr.adminTeacherApprovals}</span>
          </div>
          {adminTeachers
            .filter((t) => t.email.toLowerCase() !== OWNER_ADMIN_EMAIL)
            .map((t) => (
              <div
                key={t.userId}
                className="flex flex-wrap items-center justify-between gap-2 rounded-2xl bg-svt-surface p-3 text-xs"
              >
                <div>
                  <div className="font-bold">{t.fullName}</div>
                  <div className="text-svt-onSurfaceVariant">
                    {t.email} • {t.establishment} • <strong>{t.teacherStatus}</strong>
                  </div>
                </div>
                <div className="flex gap-1.5">
                  {t.teacherStatus !== 'approved' && (
                    <button
                      type="button"
                      onClick={() => adminSetTeacherStatus(t.userId, 'approved')}
                      className="rounded-xl bg-svt-success px-3 py-1.5 font-bold text-white"
                    >
                      {tr.approve}
                    </button>
                  )}
                  {t.teacherStatus !== 'rejected' && (
                    <button
                      type="button"
                      onClick={() => adminSetTeacherStatus(t.userId, 'rejected')}
                      className="rounded-xl bg-svt-error px-3 py-1.5 font-bold text-white"
                    >
                      {tr.reject}
                    </button>
                  )}
                </div>
              </div>
            ))}
        </div>
      )}

      <QrDisplayModal svtClass={qrModalClass} onClose={() => setQrModalClass(null)} />
    </div>
  );
};
