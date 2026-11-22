import React, { useState } from 'react';
import {
  Clock,
  Lock,
  LogOut,
  QrCode,
  RefreshCw,
  ShieldAlert,
  ShieldCheck,
  Smartphone,
} from 'lucide-react';
import { AppLanguage, Enrollment, UserProfile, UserRole } from '../types/svt';
import { TRANSLATIONS } from '../lib/i18n';
import {
  acceptUserTerms,
  completeProfileSetup,
  joinClassWithQrForSignedInStudent,
  signOutAccount,
} from '../lib/repository';
import { normalizeMassar } from '../lib/authHelpers';
import { QrScannerModal } from '../components/QrScannerModal';
import { LegalDocType, LegalModal } from '../components/LegalModals';

export const ProfileSetupScreen: React.FC<{
  uid: string;
  defaultEmail: string;
  defaultName: string;
  lang: AppLanguage;
}> = ({ uid, defaultEmail, defaultName, lang }) => {
  const tr = TRANSLATIONS[lang];
  const [role, setRole] = useState<UserRole>('teacher');
  const [fullName, setFullName] = useState(defaultName);
  const [phoneNumber, setPhoneNumber] = useState('');
  const [establishment, setEstablishment] = useState('');
  const [massarNumber, setMassarNumber] = useState('');
  const [birthDate, setBirthDate] = useState('');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErr(null);
    if (fullName.trim().length < 2) {
      setErr('Nom complet requis.');
      return;
    }
    if (role === 'teacher' && (phoneNumber.trim().length < 6 || establishment.trim().length < 2)) {
      setErr('Téléphone (6+ chiffres) et établissement scolaire requis pour les professeurs.');
      return;
    }
    setBusy(true);
    try {
      await completeProfileSetup({
        uid,
        role,
        fullName,
        phoneNumber,
        establishment,
        massarNumber,
        birthDate,
        email: defaultEmail,
      });
    } catch (e: any) {
      setErr(e?.message || 'Erreur lors de l’enregistrement');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="min-h-screen-safe flex items-center justify-center bg-svt-bg p-4 pt-safe pb-safe">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-md space-y-4 rounded-3xl border border-svt-outline/20 bg-svt-surface p-6 shadow-md"
      >
        <h1 className="font-display text-xl font-bold">{tr.setupTitle}</h1>
        <p className="text-xs text-svt-onSurfaceVariant">
          {role === 'teacher' ? tr.setupSubTeacher : tr.setupSubStudent}
        </p>

        <div className="flex rounded-2xl bg-svt-surfaceVariant/60 p-1">
          <button
            type="button"
            onClick={() => setRole('student')}
            className={`min-h-[40px] flex-1 rounded-xl text-xs font-bold ${
              role === 'student' ? 'bg-svt-surface text-svt-primary shadow-sm' : ''
            }`}
          >
            {tr.studentRoleBtn}
          </button>
          <button
            type="button"
            onClick={() => setRole('teacher')}
            className={`min-h-[40px] flex-1 rounded-xl text-xs font-bold ${
              role === 'teacher' ? 'bg-svt-surface text-svt-primary shadow-sm' : ''
            }`}
          >
            {tr.teacherRoleBtn}
          </button>
        </div>

        {err && (
          <div className="rounded-xl bg-svt-error/10 p-3 text-xs font-semibold text-svt-error">
            {err}
          </div>
        )}

        <div>
          <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
            {tr.fullNameLabel} *
          </label>
          <input
            type="text"
            required
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 text-sm"
          />
        </div>

        <div>
          <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
            {role === 'teacher' ? tr.phoneRequired : tr.phoneOptional}
          </label>
          <input
            type="tel"
            required={role === 'teacher'}
            value={phoneNumber}
            onChange={(e) => setPhoneNumber(e.target.value)}
            className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 text-sm"
          />
        </div>

        {role === 'teacher' ? (
          <div>
            <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
              {tr.establishmentRequired}
            </label>
            <input
              type="text"
              required
              value={establishment}
              onChange={(e) => setEstablishment(e.target.value)}
              className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 text-sm"
            />
          </div>
        ) : (
          <>
            <div>
              <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                {tr.massarLabel}
              </label>
              <input
                type="text"
                value={massarNumber}
                onChange={(e) => setMassarNumber(normalizeMassar(e.target.value))}
                className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 font-mono text-sm uppercase"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                {tr.birthDateLabel}
              </label>
              <input
                type="text"
                placeholder="JJ/MM/AAAA"
                value={birthDate}
                onChange={(e) => setBirthDate(e.target.value)}
                className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 text-sm"
              />
            </div>
          </>
        )}

        <div className="flex gap-2 pt-2">
          <button
            type="button"
            onClick={() => signOutAccount()}
            className="min-h-[48px] rounded-2xl border border-svt-outline/30 px-4 text-sm font-bold"
          >
            {tr.signOut}
          </button>
          <button
            type="submit"
            disabled={busy}
            className="min-h-[48px] flex-1 rounded-2xl bg-svt-primary text-sm font-bold text-svt-onPrimary"
          >
            {busy ? tr.loading : tr.continueBtn}
          </button>
        </div>
      </form>
    </div>
  );
};

export const TermsAcceptanceScreen: React.FC<{
  profile: UserProfile;
  lang: AppLanguage;
}> = ({ profile, lang }) => {
  const tr = TRANSLATIONS[lang];
  const [checked, setChecked] = useState(false);
  const [busy, setBusy] = useState(false);
  const [legalDoc, setLegalDoc] = useState<LegalDocType>(null);

  const handleAccept = async () => {
    if (!checked) return;
    setBusy(true);
    try {
      await acceptUserTerms(profile.userId);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="min-h-screen-safe flex items-center justify-center bg-svt-bg p-4 pt-safe pb-safe">
      <div className="flex max-h-[88dvh] w-full max-w-lg flex-col rounded-3xl border border-svt-outline/20 bg-svt-surface p-5 shadow-lg">
        <div className="flex items-center gap-2.5 text-svt-primary">
          <ShieldCheck className="h-6 w-6" />
          <h1 className="font-display text-lg font-bold">{tr.termsTitle}</h1>
        </div>
        <p className="mt-1 text-xs text-svt-onSurfaceVariant">
          {profile.fullName} — {tr.termsSubtitle}
        </p>

        <div className="my-4 flex-1 overflow-y-auto rounded-2xl border border-svt-outline/20 bg-svt-bg p-4 text-xs leading-relaxed whitespace-pre-line text-svt-onSurfaceVariant">
          {tr.termsBody}
        </div>

        <button
          type="button"
          onClick={() => setLegalDoc('privacy')}
          className="mb-3 text-start text-xs font-bold text-svt-secondary hover:underline"
        >
          {tr.termsPrivacyLink} →
        </button>

        <label className="flex cursor-pointer items-start gap-2.5 rounded-2xl bg-svt-surfaceVariant/50 p-3 text-xs font-semibold">
          <input
            type="checkbox"
            checked={checked}
            onChange={(e) => setChecked(e.target.checked)}
            className="mt-0.5 h-5 w-5 accent-svt-primary"
          />
          <span>{tr.termsCheckbox}</span>
        </label>

        <div className="mt-4 flex gap-2">
          <button
            type="button"
            onClick={() => signOutAccount()}
            className="min-h-[48px] rounded-2xl border border-svt-outline/30 px-4 text-sm font-bold"
          >
            {tr.signOut}
          </button>
          <button
            type="button"
            disabled={!checked || busy}
            onClick={handleAccept}
            className="min-h-[48px] flex-1 rounded-2xl bg-svt-primary text-sm font-bold text-svt-onPrimary disabled:opacity-50"
          >
            {busy ? tr.loading : tr.termsContinue}
          </button>
        </div>
      </div>

      <LegalModal docType={legalDoc} lang={lang} onClose={() => setLegalDoc(null)} />
    </div>
  );
};

export const TeacherPendingScreen: React.FC<{
  profile: UserProfile;
  lang: AppLanguage;
  onRefresh: () => void;
}> = ({ profile, lang, onRefresh }) => {
  const tr = TRANSLATIONS[lang];
  const rejected = profile.teacherStatus === 'rejected';
  return (
    <div className="min-h-screen-safe flex items-center justify-center bg-svt-bg p-4 pt-safe pb-safe">
      <div className="w-full max-w-md space-y-4 rounded-3xl border border-svt-outline/20 bg-svt-surface p-6 text-center shadow-md">
        <div
          className={`mx-auto flex h-14 w-14 items-center justify-center rounded-2xl ${
            rejected
              ? 'bg-svt-error/15 text-svt-error'
              : 'bg-svt-warning/15 text-svt-warning'
          }`}
        >
          {rejected ? <ShieldAlert className="h-7 w-7" /> : <Clock className="h-7 w-7" />}
        </div>
        <h1 className="font-display text-xl font-bold">
          {rejected ? tr.teacherRejectedTitle : tr.teacherPendingTitle}
        </h1>
        <p className="text-sm text-svt-onSurfaceVariant">
          {profile.fullName} ({profile.establishment}) —{' '}
          {rejected ? tr.teacherRejectedBody : tr.teacherPendingBody}
        </p>
        <div className="flex gap-2 pt-2">
          <button
            type="button"
            onClick={() => signOutAccount()}
            className="flex min-h-[48px] items-center justify-center gap-1.5 rounded-2xl border border-svt-outline/30 px-4 text-sm font-bold"
          >
            <LogOut className="h-4 w-4" />
            <span>{tr.signOut}</span>
          </button>
          <button
            type="button"
            onClick={onRefresh}
            className="flex min-h-[48px] flex-1 items-center justify-center gap-1.5 rounded-2xl bg-svt-primary text-sm font-bold text-svt-onPrimary"
          >
            <RefreshCw className="h-4 w-4" />
            <span>{tr.refreshStatus}</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export const QrJoinClassScreen: React.FC<{
  profile: UserProfile;
  lang: AppLanguage;
}> = ({ profile, lang }) => {
  const tr = TRANSLATIONS[lang];
  const [code, setCode] = useState('');
  const [scannerOpen, setScannerOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const handleJoin = async (rawCode: string) => {
    setErr(null);
    setBusy(true);
    try {
      await joinClassWithQrForSignedInStudent({ profile, rawQr: rawCode });
    } catch (e: any) {
      setErr(e?.message || tr.qrInvalidCode);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="min-h-screen-safe flex items-center justify-center bg-svt-bg p-4 pt-safe pb-safe">
      <div className="w-full max-w-md space-y-4 rounded-3xl border border-svt-outline/20 bg-svt-surface p-6 shadow-md">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-svt-primaryContainer text-svt-primary">
            <QrCode className="h-6 w-6" />
          </div>
          <div>
            <h1 className="font-display text-lg font-bold">{tr.qrJoinTitle}</h1>
            <p className="text-xs text-svt-onSurfaceVariant">{profile.fullName}</p>
          </div>
        </div>
        <p className="text-xs text-svt-onSurfaceVariant">{tr.qrJoinDesc}</p>

        {err && (
          <div className="rounded-xl bg-svt-error/10 p-3 text-xs font-semibold text-svt-error">
            {err}
          </div>
        )}

        <button
          type="button"
          onClick={() => setScannerOpen(true)}
          className="flex min-h-[50px] w-full items-center justify-center gap-2 rounded-2xl bg-svt-primary text-sm font-bold text-svt-onPrimary"
        >
          <QrCode className="h-5 w-5" />
          <span>{tr.qrScanBtn}</span>
        </button>

        <div className="flex gap-2">
          <input
            type="text"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            placeholder="BIOTERRE:SVT-2BAC-A1"
            className="min-h-[48px] flex-1 rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 text-sm"
          />
          <button
            type="button"
            disabled={busy || code.trim().length < 3}
            onClick={() => handleJoin(code)}
            className="min-h-[48px] rounded-2xl bg-svt-secondary px-4 text-sm font-bold text-svt-onSecondary disabled:opacity-50"
          >
            {tr.qrJoinSubmit}
          </button>
        </div>

        <button
          type="button"
          onClick={() => signOutAccount()}
          className="flex min-h-[44px] w-full items-center justify-center gap-1.5 rounded-2xl border border-svt-outline/25 text-xs font-bold text-svt-onSurfaceVariant"
        >
          <LogOut className="h-4 w-4" />
          <span>{tr.signOut}</span>
        </button>
      </div>

      <QrScannerModal
        open={scannerOpen}
        lang={lang}
        onDetected={(raw) => {
          setScannerOpen(false);
          setCode(raw);
          handleJoin(raw);
        }}
        onClose={() => setScannerOpen(false)}
      />
    </div>
  );
};

export const PendingEnrollmentScreen: React.FC<{
  enrollment: Enrollment;
  lang: AppLanguage;
  onScanAnother: () => void;
}> = ({ enrollment, lang, onScanAnother }) => {
  const tr = TRANSLATIONS[lang];
  const rejected = enrollment.status === 'rejected';
  return (
    <div className="min-h-screen-safe flex items-center justify-center bg-svt-bg p-4 pt-safe pb-safe">
      <div className="w-full max-w-md space-y-4 rounded-3xl border border-svt-outline/20 bg-svt-surface p-6 text-center shadow-md">
        <div
          className={`mx-auto flex h-14 w-14 items-center justify-center rounded-2xl ${
            rejected ? 'bg-svt-error/15 text-svt-error' : 'bg-svt-warning/15 text-svt-warning'
          }`}
        >
          {rejected ? <ShieldAlert className="h-7 w-7" /> : <Clock className="h-7 w-7" />}
        </div>
        <h1 className="font-display text-xl font-bold">
          {rejected ? tr.rejectedTitle : tr.pendingTitle}
        </h1>
        <p className="text-sm text-svt-onSurfaceVariant">
          {enrollment.className} ({enrollment.teacherName}) —{' '}
          {rejected ? tr.rejectedDesc : tr.pendingDesc}
        </p>
        <div className="flex gap-2 pt-2">
          <button
            type="button"
            onClick={() => signOutAccount()}
            className="min-h-[48px] rounded-2xl border border-svt-outline/30 px-4 text-sm font-bold"
          >
            {tr.signOut}
          </button>
          <button
            type="button"
            onClick={onScanAnother}
            className="min-h-[48px] flex-1 rounded-2xl bg-svt-primary text-sm font-bold text-svt-onPrimary"
          >
            {tr.changeClassCode}
          </button>
        </div>
      </div>
    </div>
  );
};

export const DeviceLockedScreen: React.FC<{
  profile: UserProfile;
  lang: AppLanguage;
}> = ({ profile, lang }) => {
  const tr = TRANSLATIONS[lang];
  return (
    <div className="min-h-screen-safe flex items-center justify-center bg-svt-bg p-4 pt-safe pb-safe">
      <div className="w-full max-w-md space-y-4 rounded-3xl border border-svt-error/30 bg-svt-surface p-6 text-center shadow-md">
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-svt-error/15 text-svt-error">
          <Smartphone className="h-7 w-7" />
        </div>
        <h1 className="font-display text-xl font-bold">{tr.deviceLockedTitle}</h1>
        <p className="text-sm text-svt-onSurfaceVariant">{tr.deviceLockedBody}</p>
        <div className="inline-flex items-center gap-2 rounded-xl bg-svt-surfaceVariant/60 px-3 py-1.5 text-xs font-semibold">
          <Lock className="h-3.5 w-3.5" />
          <span>{profile.boundDeviceModel || 'Appareil enregistré'}</span>
        </div>
        <div>
          <button
            type="button"
            onClick={() => signOutAccount()}
            className="mt-2 min-h-[48px] w-full rounded-2xl bg-svt-primary text-sm font-bold text-svt-onPrimary"
          >
            {tr.signOut}
          </button>
        </div>
      </div>
    </div>
  );
};
