import React, { useEffect, useState } from 'react';
import {
  AlertTriangle,
  ArrowLeft,
  Eye,
  GraduationCap,
  KeyRound,
  QrCode,
  ShieldCheck,
  Sparkles,
  UserCheck,
} from 'lucide-react';
import { AppLanguage, UserRole } from '../types/svt';
import { TRANSLATIONS } from '../lib/i18n';
import {
  defaultStudentTaalimEmail,
  normalizeMassar,
  parseQrPayload,
} from '../lib/authHelpers';
import { isFirebaseConfigured } from '../lib/firebase';
import {
  registerStudentWithQr,
  registerTeacher,
  sendPasswordReset,
  signInStudent,
  signInTeacher,
  signInWithGoogleTeacher,
} from '../lib/repository';
import { QrScannerModal } from '../components/QrScannerModal';
import { LegalDocType, LegalModal } from '../components/LegalModals';
import { IosInstallBanner } from '../components/IosInstallBanner';

type AuthView = 'role' | 'qr_quick' | 'student' | 'teacher';

interface AuthGatewayProps {
  lang: AppLanguage;
  onChangeLang: (lang: AppLanguage) => void;
  onEnterGuestMode: () => void;
}

export const AuthGateway: React.FC<AuthGatewayProps> = ({
  lang,
  onChangeLang,
  onEnterGuestMode,
}) => {
  const tr = TRANSLATIONS[lang];
  const [view, setView] = useState<AuthView>('role');
  const [teacherRegisterMode, setTeacherRegisterMode] = useState(false);

  // Student login fields
  const [studentIdentifier, setStudentIdentifier] = useState('');
  const [studentPassword, setStudentPassword] = useState('');

  // Quick QR join fields
  const [qrInput, setQrInput] = useState('');
  const [qrFullName, setQrFullName] = useState('');
  const [qrMassar, setQrMassar] = useState('');
  const [qrRecoveryEmail, setQrRecoveryEmail] = useState('');
  const [qrRecoveryTouched, setQrRecoveryTouched] = useState(false);
  const [qrPassword, setQrPassword] = useState('');
  const [qrPhone, setQrPhone] = useState('');
  const [scannerOpen, setScannerOpen] = useState(false);

  // Teacher fields
  const [teacherEmail, setTeacherEmail] = useState('');
  const [teacherPassword, setTeacherPassword] = useState('');
  const [teacherConfirmPassword, setTeacherConfirmPassword] = useState('');
  const [teacherFullName, setTeacherFullName] = useState('');
  const [teacherPhone, setTeacherPhone] = useState('');
  const [teacherEstablishment, setTeacherEstablishment] = useState('');

  // Modals & status
  const [busy, setBusy] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [forgotOpen, setForgotOpen] = useState<UserRole | null>(null);
  const [forgotInput, setForgotInput] = useState('');
  const [forgotSuccess, setForgotSuccess] = useState<string | null>(null);
  const [legalDoc, setLegalDoc] = useState<LegalDocType>(null);

  // Auto-prefill recovery email as <massar>@taalim.ma
  useEffect(() => {
    if (!qrRecoveryTouched) {
      setQrRecoveryEmail(defaultStudentTaalimEmail(qrMassar));
    }
  }, [qrMassar, qrRecoveryTouched]);

  const handleQrDetected = (raw: string) => {
    setScannerOpen(false);
    setQrInput(raw);
    const parsed = parseQrPayload(raw);
    if (parsed && parsed.massarNumber) {
      setQrMassar(parsed.massarNumber);
    }
    setErrorMsg(null);
  };

  const handleStudentSignIn = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    if (!studentIdentifier.trim() || !studentPassword) {
      setErrorMsg('Saisissez votre numéro Massar (ou e-mail) et votre mot de passe.');
      return;
    }
    setBusy(true);
    try {
      await signInStudent(studentIdentifier, studentPassword);
    } catch (err: any) {
      setErrorMsg(formatAuthError(err));
    } finally {
      setBusy(false);
    }
  };

  const handleQrQuickRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    if (!qrInput.trim()) {
      setErrorMsg('Scannez le QR Code du professeur ou saisissez le code classe.');
      return;
    }
    if (qrFullName.trim().length < 2) {
      setErrorMsg('Veuillez saisir votre nom complet (min. 2 caractères).');
      return;
    }
    if (qrPassword.length < 6) {
      setErrorMsg('Le mot de passe doit contenir au moins 6 caractères.');
      return;
    }
    setBusy(true);
    try {
      await registerStudentWithQr({
        qrInput,
        fullName: qrFullName,
        massarInput: qrMassar,
        password: qrPassword,
        recoveryEmail: qrRecoveryEmail,
        phoneNumber: qrPhone,
      });
    } catch (err: any) {
      setErrorMsg(formatAuthError(err));
    } finally {
      setBusy(false);
    }
  };

  const handleTeacherSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    if (!teacherEmail.includes('@')) {
      setErrorMsg('Veuillez saisir une adresse e-mail professionnelle valide.');
      return;
    }
    if (teacherPassword.length < 6) {
      setErrorMsg('Le mot de passe doit contenir au moins 6 caractères.');
      return;
    }
    if (teacherRegisterMode) {
      if (teacherPassword !== teacherConfirmPassword) {
        setErrorMsg('Les mots de passe ne correspondent pas.');
        return;
      }
      if (teacherFullName.trim().length < 2) {
        setErrorMsg('Nom complet requis.');
        return;
      }
      if (teacherPhone.trim().length < 6) {
        setErrorMsg('Numéro de téléphone valide requis (min. 6 caractères).');
        return;
      }
      if (teacherEstablishment.trim().length < 2) {
        setErrorMsg('Nom de l’établissement requis.');
        return;
      }
    }

    setBusy(true);
    try {
      if (teacherRegisterMode) {
        await registerTeacher({
          email: teacherEmail,
          password: teacherPassword,
          fullName: teacherFullName,
          phoneNumber: teacherPhone,
          establishment: teacherEstablishment,
        });
      } else {
        await signInTeacher(teacherEmail, teacherPassword);
      }
    } catch (err: any) {
      setErrorMsg(formatAuthError(err));
    } finally {
      setBusy(false);
    }
  };

  const handleGoogleTeacher = async () => {
    setErrorMsg(null);
    setBusy(true);
    try {
      await signInWithGoogleTeacher({
        fullName: teacherFullName,
        phoneNumber: teacherPhone,
        establishment: teacherEstablishment,
      });
    } catch (err: any) {
      setErrorMsg(formatAuthError(err));
    } finally {
      setBusy(false);
    }
  };

  const handleForgotSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setForgotSuccess(null);
    setErrorMsg(null);
    if (!forgotInput.trim()) return;
    setBusy(true);
    try {
      await sendPasswordReset(forgotInput);
      setForgotSuccess(tr.resetEmailSent);
    } catch (err: any) {
      setErrorMsg(formatAuthError(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="min-h-screen-safe flex flex-col bg-svt-bg pt-safe pb-safe text-svt-onSurface">
      {/* Top Language Bar */}
      <header className="mx-auto flex w-full max-w-2xl items-center justify-between px-4 pt-3 pb-1">
        <div className="flex items-center gap-2.5">
          <img
            src="/icons/icon-192.png"
            alt="BioTerre SVT"
            className="h-10 w-10 rounded-xl object-cover shadow-sm"
          />
          <div>
            <div className="font-display text-base font-extrabold text-svt-primary">
              {tr.appName}
            </div>
            <div className="text-[11px] font-semibold text-svt-onSurfaceVariant">
              {tr.appSubtitle}
            </div>
          </div>
        </div>

        <div className="flex items-center gap-1 rounded-2xl border border-svt-outline/20 bg-svt-surface p-1 shadow-sm">
          {(['fr', 'ar', 'en'] as AppLanguage[]).map((code) => (
            <button
              key={code}
              type="button"
              onClick={() => onChangeLang(code)}
              className={`min-h-[34px] rounded-xl px-2.5 text-xs font-bold uppercase transition ${
                lang === code
                  ? 'bg-svt-primary text-svt-onPrimary shadow-sm'
                  : 'text-svt-onSurfaceVariant hover:bg-svt-surfaceVariant/60'
              }`}
            >
              {code}
            </button>
          ))}
        </div>
      </header>

      <IosInstallBanner lang={lang} />

      <main className="mx-auto flex w-full max-w-lg flex-1 flex-col justify-center px-4 py-5">
        {!isFirebaseConfigured && (
          <div className="mb-4 rounded-2xl border border-svt-warning/40 bg-svt-warning/10 p-3.5 text-xs">
            <div className="flex items-center gap-2 font-bold text-svt-warning">
              <AlertTriangle className="h-4 w-4 shrink-0" />
              <span>{tr.firebaseMissingTitle}</span>
            </div>
            <p className="mt-1 text-svt-onSurfaceVariant">{tr.firebaseMissingBody}</p>
          </div>
        )}

        {errorMsg && (
          <div className="mb-4 rounded-2xl border border-svt-error/35 bg-svt-error/10 p-3 text-xs font-semibold text-svt-error">
            {errorMsg}
          </div>
        )}

        {view === 'role' && (
          <div className="space-y-4">
            <div className="rounded-3xl bg-gradient-to-br from-svt-primary to-svt-secondary p-6 text-white shadow-lg">
              <div className="inline-flex items-center gap-1.5 rounded-full bg-white/15 px-3 py-1 text-[11px] font-bold">
                <Sparkles className="h-3.5 w-3.5" />
                <span>PWA • iOS Safari & Desktop</span>
              </div>
              <h1 className="mt-3 font-display text-2xl font-extrabold leading-tight">
                {tr.welcomeTitle}
              </h1>
              <p className="mt-1.5 text-sm leading-relaxed text-white/90">{tr.welcomeDesc}</p>
            </div>

            <div className="space-y-2.5">
              <p className="px-1 text-xs font-bold uppercase tracking-wider text-svt-onSurfaceVariant">
                {tr.whoAreYou}
              </p>

              {/* 1. Join directly by QR Code */}
              <button
                type="button"
                disabled={!isFirebaseConfigured}
                onClick={() => {
                  setErrorMsg(null);
                  setView('qr_quick');
                }}
                className="flex min-h-[72px] w-full items-center gap-3.5 rounded-3xl border-2 border-svt-primary/40 bg-svt-primaryContainer/50 p-4 text-start transition hover:bg-svt-primaryContainer/80 disabled:opacity-50"
              >
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-svt-primary text-svt-onPrimary shadow-sm">
                  <QrCode className="h-6 w-6" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="font-display text-base font-bold text-svt-onPrimaryContainer">
                    {tr.quickQrTitle}
                  </div>
                  <div className="text-xs text-svt-onPrimaryContainer/80">{tr.quickQrSub}</div>
                </div>
              </button>

              {/* 2. Student */}
              <button
                type="button"
                disabled={!isFirebaseConfigured}
                onClick={() => {
                  setErrorMsg(null);
                  setView('student');
                }}
                className="flex min-h-[70px] w-full items-center gap-3.5 rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 text-start shadow-sm transition hover:border-svt-primary/40 disabled:opacity-50"
              >
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-svt-secondaryContainer text-svt-secondary">
                  <GraduationCap className="h-6 w-6" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="font-display text-base font-bold">{tr.studentRoleBtn}</div>
                  <div className="text-xs text-svt-onSurfaceVariant">{tr.studentRoleSub}</div>
                </div>
              </button>

              {/* 3. Teacher */}
              <button
                type="button"
                disabled={!isFirebaseConfigured}
                onClick={() => {
                  setErrorMsg(null);
                  setView('teacher');
                }}
                className="flex min-h-[70px] w-full items-center gap-3.5 rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 text-start shadow-sm transition hover:border-svt-primary/40 disabled:opacity-50"
              >
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-svt-tertiaryContainer text-svt-tertiary">
                  <UserCheck className="h-6 w-6" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="font-display text-base font-bold">{tr.teacherRoleBtn}</div>
                  <div className="text-xs text-svt-onSurfaceVariant">{tr.teacherRoleSub}</div>
                </div>
              </button>

              {/* 4. Guest Demo Mode */}
              <button
                type="button"
                onClick={onEnterGuestMode}
                className="flex min-h-[64px] w-full items-center gap-3.5 rounded-3xl border border-dashed border-svt-outline/40 bg-svt-surfaceVariant/40 p-4 text-start transition hover:bg-svt-surfaceVariant/70"
              >
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-svt-surface text-svt-primary">
                  <Eye className="h-5 w-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="font-display text-sm font-bold">{tr.guestModeBtn}</div>
                  <div className="text-xs text-svt-onSurfaceVariant">{tr.guestModeSub}</div>
                </div>
              </button>
            </div>
          </div>
        )}

        {view === 'qr_quick' && (
          <form
            onSubmit={handleQrQuickRegister}
            className="space-y-3.5 rounded-3xl border border-svt-outline/20 bg-svt-surface p-5 shadow-sm"
          >
            <button
              type="button"
              onClick={() => setView('role')}
              className="inline-flex items-center gap-1.5 text-xs font-bold text-svt-primary"
            >
              <ArrowLeft className="h-4 w-4" />
              <span>{tr.changeRole}</span>
            </button>

            <h2 className="font-display text-xl font-bold">{tr.quickQrTitle}</h2>
            <p className="text-xs text-svt-onSurfaceVariant">{tr.quickQrSub}</p>

            <button
              type="button"
              onClick={() => setScannerOpen(true)}
              className="flex min-h-[48px] w-full items-center justify-center gap-2 rounded-2xl bg-svt-primaryContainer px-4 text-sm font-bold text-svt-onPrimaryContainer"
            >
              <QrCode className="h-5 w-5" />
              <span>{tr.qrScanBtn}</span>
            </button>

            <div>
              <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                {tr.qrManualLabel}
              </label>
              <input
                type="text"
                value={qrInput}
                onChange={(e) => {
                  setQrInput(e.target.value);
                  const parsed = parseQrPayload(e.target.value);
                  if (parsed?.massarNumber) setQrMassar(parsed.massarNumber);
                }}
                placeholder="BIOTERRE:SVT-2BAC-A1 ou SVT-2BAC-A1|G134567890"
                className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 text-sm outline-none focus:border-svt-primary"
              />
            </div>

            <div>
              <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                {tr.fullNameLabel} *
              </label>
              <input
                type="text"
                required
                value={qrFullName}
                onChange={(e) => setQrFullName(e.target.value)}
                placeholder="Ex : Salma El Mansouri"
                className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 text-sm outline-none focus:border-svt-primary"
              />
            </div>

            <div>
              <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                {tr.massarOptional}
              </label>
              <input
                type="text"
                value={qrMassar}
                onChange={(e) => setQrMassar(normalizeMassar(e.target.value))}
                placeholder="Ex : G134567890"
                className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 font-mono text-sm uppercase outline-none focus:border-svt-primary"
              />
            </div>

            <div>
              <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                {tr.recoveryEmailLabel}
              </label>
              <input
                type="email"
                value={qrRecoveryEmail}
                onChange={(e) => {
                  setQrRecoveryTouched(true);
                  setQrRecoveryEmail(e.target.value);
                }}
                placeholder="g134567890@taalim.ma"
                className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 text-sm outline-none focus:border-svt-primary"
              />
              <span className="mt-1 block text-[11px] text-svt-onSurfaceVariant">
                {tr.taalimHint}
              </span>
            </div>

            <div>
              <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                {tr.phoneOptional}
              </label>
              <input
                type="tel"
                value={qrPhone}
                onChange={(e) => setQrPhone(e.target.value)}
                placeholder="06 00 00 00 00"
                className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 text-sm outline-none focus:border-svt-primary"
              />
            </div>

            <div>
              <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                {tr.passwordLabel} *
              </label>
              <input
                type="password"
                required
                minLength={6}
                value={qrPassword}
                onChange={(e) => setQrPassword(e.target.value)}
                placeholder="••••••••"
                className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 text-sm outline-none focus:border-svt-primary"
              />
            </div>

            <button
              type="submit"
              disabled={busy}
              className="min-h-[50px] w-full rounded-2xl bg-svt-primary text-sm font-bold text-svt-onPrimary shadow-sm disabled:opacity-50"
            >
              {busy ? tr.loading : tr.register}
            </button>
          </form>
        )}

        {view === 'student' && (
          <form
            onSubmit={handleStudentSignIn}
            className="space-y-4 rounded-3xl border border-svt-outline/20 bg-svt-surface p-5 shadow-sm"
          >
            <button
              type="button"
              onClick={() => setView('role')}
              className="inline-flex items-center gap-1.5 text-xs font-bold text-svt-primary"
            >
              <ArrowLeft className="h-4 w-4" />
              <span>{tr.changeRole}</span>
            </button>

            <div>
              <h2 className="font-display text-xl font-bold">{tr.studentRoleBtn}</h2>
              <p className="mt-0.5 text-xs text-svt-onSurfaceVariant">{tr.studentRoleSub}</p>
            </div>

            <div>
              <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                {tr.massarOrEmailLabel}
              </label>
              <input
                type="text"
                required
                value={studentIdentifier}
                onChange={(e) => setStudentIdentifier(e.target.value)}
                placeholder="G134567890 ou eleve@taalim.ma"
                className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 text-sm outline-none focus:border-svt-primary"
              />
            </div>

            <div>
              <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                {tr.passwordLabel}
              </label>
              <input
                type="password"
                required
                value={studentPassword}
                onChange={(e) => setStudentPassword(e.target.value)}
                placeholder="••••••••"
                className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 text-sm outline-none focus:border-svt-primary"
              />
            </div>

            <div className="flex items-center justify-between">
              <button
                type="button"
                onClick={() => {
                  setForgotInput(studentIdentifier);
                  setForgotSuccess(null);
                  setForgotOpen('student');
                }}
                className="text-xs font-bold text-svt-secondary hover:underline"
              >
                {tr.forgotPassword}
              </button>
              <button
                type="button"
                onClick={() => setView('qr_quick')}
                className="text-xs font-bold text-svt-primary hover:underline"
              >
                + QR Code
              </button>
            </div>

            <button
              type="submit"
              disabled={busy}
              className="min-h-[50px] w-full rounded-2xl bg-svt-primary text-sm font-bold text-svt-onPrimary shadow-sm disabled:opacity-50"
            >
              {busy ? tr.loading : tr.signIn}
            </button>
          </form>
        )}

        {view === 'teacher' && (
          <div className="space-y-4 rounded-3xl border border-svt-outline/20 bg-svt-surface p-5 shadow-sm">
            <button
              type="button"
              onClick={() => setView('role')}
              className="inline-flex items-center gap-1.5 text-xs font-bold text-svt-primary"
            >
              <ArrowLeft className="h-4 w-4" />
              <span>{tr.changeRole}</span>
            </button>

            <div className="flex rounded-2xl bg-svt-surfaceVariant/60 p-1">
              <button
                type="button"
                onClick={() => setTeacherRegisterMode(false)}
                className={`min-h-[40px] flex-1 rounded-xl text-xs font-bold transition ${
                  !teacherRegisterMode ? 'bg-svt-surface text-svt-primary shadow-sm' : ''
                }`}
              >
                {tr.signIn}
              </button>
              <button
                type="button"
                onClick={() => setTeacherRegisterMode(true)}
                className={`min-h-[40px] flex-1 rounded-xl text-xs font-bold transition ${
                  teacherRegisterMode ? 'bg-svt-surface text-svt-primary shadow-sm' : ''
                }`}
              >
                {tr.register}
              </button>
            </div>

            {teacherRegisterMode && (
              <div className="rounded-2xl bg-svt-tertiaryContainer/60 p-3 text-xs text-svt-onTertiaryContainer">
                {tr.teacherApprovalNotice}
              </div>
            )}

            <form onSubmit={handleTeacherSubmit} className="space-y-3">
              {teacherRegisterMode && (
                <>
                  <div>
                    <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                      {tr.fullNameLabel} *
                    </label>
                    <input
                      type="text"
                      required
                      value={teacherFullName}
                      onChange={(e) => setTeacherFullName(e.target.value)}
                      placeholder="Prof. Nom Prénom"
                      className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 text-sm outline-none focus:border-svt-primary"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                      {tr.phoneRequired}
                    </label>
                    <input
                      type="tel"
                      required
                      value={teacherPhone}
                      onChange={(e) => setTeacherPhone(e.target.value)}
                      placeholder="+212 6 00 00 00 00"
                      className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 text-sm outline-none focus:border-svt-primary"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                      {tr.establishmentRequired}
                    </label>
                    <input
                      type="text"
                      required
                      value={teacherEstablishment}
                      onChange={(e) => setTeacherEstablishment(e.target.value)}
                      placeholder="Lycée Qualifiant Ibn Sina"
                      className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 text-sm outline-none focus:border-svt-primary"
                    />
                  </div>
                </>
              )}

              <div>
                <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                  {tr.emailProLabel}
                </label>
                <input
                  type="email"
                  required
                  value={teacherEmail}
                  onChange={(e) => setTeacherEmail(e.target.value)}
                  placeholder="nom.prenom@ecole.ma"
                  className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 text-sm outline-none focus:border-svt-primary"
                />
              </div>

              <div>
                <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                  {tr.passwordLabel}
                </label>
                <input
                  type="password"
                  required
                  minLength={6}
                  value={teacherPassword}
                  onChange={(e) => setTeacherPassword(e.target.value)}
                  placeholder="••••••••"
                  className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 text-sm outline-none focus:border-svt-primary"
                />
              </div>

              {teacherRegisterMode && (
                <div>
                  <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                    {tr.confirmPasswordLabel}
                  </label>
                  <input
                    type="password"
                    required
                    minLength={6}
                    value={teacherConfirmPassword}
                    onChange={(e) => setTeacherConfirmPassword(e.target.value)}
                    placeholder="••••••••"
                    className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 text-sm outline-none focus:border-svt-primary"
                  />
                </div>
              )}

              {!teacherRegisterMode && (
                <div className="text-end">
                  <button
                    type="button"
                    onClick={() => {
                      setForgotInput(teacherEmail);
                      setForgotSuccess(null);
                      setForgotOpen('teacher');
                    }}
                    className="text-xs font-bold text-svt-secondary hover:underline"
                  >
                    {tr.forgotPassword}
                  </button>
                </div>
              )}

              <button
                type="submit"
                disabled={busy}
                className="min-h-[50px] w-full rounded-2xl bg-svt-primary text-sm font-bold text-svt-onPrimary shadow-sm disabled:opacity-50"
              >
                {busy ? tr.loading : teacherRegisterMode ? tr.register : tr.signIn}
              </button>
            </form>

            <div className="relative flex items-center py-1">
              <div className="flex-grow border-t border-svt-outline/20" />
              <span className="mx-3 text-[11px] font-bold text-svt-onSurfaceVariant">OU</span>
              <div className="flex-grow border-t border-svt-outline/20" />
            </div>

            <button
              type="button"
              disabled={busy}
              onClick={handleGoogleTeacher}
              className="flex min-h-[48px] w-full items-center justify-center gap-2 rounded-2xl border border-svt-outline/30 bg-svt-bg text-sm font-bold transition hover:bg-svt-surfaceVariant/60"
            >
              <KeyRound className="h-4 w-4 text-svt-secondary" />
              <span>{tr.googleSignIn}</span>
            </button>
          </div>
        )}

        {/* Legal Footer */}
        <footer className="mt-6 flex flex-wrap items-center justify-center gap-3 text-xs text-svt-onSurfaceVariant">
          <button
            type="button"
            onClick={() => setLegalDoc('privacy')}
            className="inline-flex items-center gap-1 hover:text-svt-primary hover:underline"
          >
            <ShieldCheck className="h-3.5 w-3.5" />
            <span>{tr.privacyTitle}</span>
          </button>
          <span>•</span>
          <button
            type="button"
            onClick={() => setLegalDoc('terms')}
            className="hover:text-svt-primary hover:underline"
          >
            {tr.termsModalTitle}
          </button>
          <span>•</span>
          <button
            type="button"
            onClick={() => setLegalDoc('legal')}
            className="hover:text-svt-primary hover:underline"
          >
            {tr.legalTitle}
          </button>
        </footer>
      </main>

      <QrScannerModal
        open={scannerOpen}
        lang={lang}
        onDetected={handleQrDetected}
        onClose={() => setScannerOpen(false)}
      />

      {/* Forgot Password Modal */}
      {forgotOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/65 p-4 backdrop-blur-sm">
          <form
            onSubmit={handleForgotSubmit}
            className="w-full max-w-sm space-y-3 rounded-3xl bg-svt-surface p-5 text-svt-onSurface shadow-2xl"
          >
            <h3 className="font-display text-lg font-bold">{tr.forgotPassword}</h3>
            <p className="text-xs leading-relaxed text-svt-onSurfaceVariant">
              {forgotOpen === 'student'
                ? tr.forgotPasswordDescStudent
                : tr.forgotPasswordDescTeacher}
            </p>
            <input
              type="text"
              required
              value={forgotInput}
              onChange={(e) => setForgotInput(e.target.value)}
              placeholder={
                forgotOpen === 'student' ? 'G134567890 ou eleve@taalim.ma' : 'nom@ecole.ma'
              }
              className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 text-sm outline-none focus:border-svt-primary"
            />
            {forgotSuccess && (
              <p className="rounded-xl bg-svt-success/15 p-2.5 text-xs font-semibold text-svt-success">
                {forgotSuccess}
              </p>
            )}
            <div className="flex justify-end gap-2 pt-1">
              <button
                type="button"
                onClick={() => setForgotOpen(null)}
                className="min-h-[44px] rounded-xl px-4 text-sm font-bold text-svt-onSurfaceVariant"
              >
                {tr.close}
              </button>
              <button
                type="submit"
                disabled={busy}
                className="min-h-[44px] rounded-xl bg-svt-primary px-4 text-sm font-bold text-svt-onPrimary"
              >
                {tr.sendResetLink}
              </button>
            </div>
          </form>
        </div>
      )}

      <LegalModal docType={legalDoc} lang={lang} onClose={() => setLegalDoc(null)} />
    </div>
  );
};

function formatAuthError(err: any): string {
  const code = err?.code || '';
  if (
    code === 'auth/invalid-credential' ||
    code === 'auth/wrong-password' ||
    code === 'auth/user-not-found'
  ) {
    return 'Numéro Massar / e-mail ou mot de passe incorrect.';
  }
  if (code === 'auth/email-already-in-use') {
    return 'Ce compte existe déjà. Passez sur Se connecter.';
  }
  if (code === 'auth/weak-password') {
    return 'Mot de passe trop faible (minimum 6 caractères).';
  }
  if (code === 'auth/popup-closed-by-user') {
    return 'Connexion Google annulée.';
  }
  return err?.message || 'Erreur lors de la connexion.';
}
