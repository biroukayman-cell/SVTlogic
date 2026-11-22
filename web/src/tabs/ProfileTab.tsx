import React, { useState } from 'react';
import {
  Award,
  Camera,
  Flame,
  Lock,
  LogOut,
  Palette,
  ShieldCheck,
  User as UserIcon,
} from 'lucide-react';
import {
  AppLanguage,
  AppPalette,
  Enrollment,
  ThemeMode,
  UserProfile,
} from '../types/svt';
import { TRANSLATIONS } from '../lib/i18n';
import { isStudentLockedByQr } from '../lib/authHelpers';
import { computeLevelFromXp, SVT_BADGES } from '../lib/badges';
import { compressImageToDataUrl } from '../lib/fileUtils';
import {
  signOutAccount,
  updateAvatarDataUrl,
  updateStudentProfileData,
} from '../lib/repository';
import {
  clearUserPin,
  isPinEnabledForUser,
  isValidPinFormat,
  setUserPin,
} from '../lib/pinSecurity';
import { LegalDocType, LegalModal } from '../components/LegalModals';

interface ProfileTabProps {
  lang: AppLanguage;
  profile: UserProfile;
  enrollment: Enrollment | null;
  isGuestMode: boolean;
  themeMode: ThemeMode;
  palette: AppPalette;
  onChangeThemeMode: (mode: ThemeMode) => void;
  onChangePalette: (palette: AppPalette) => void;
  onGuestBlocked: () => void;
  onExitGuest: () => void;
}

export const ProfileTab: React.FC<ProfileTabProps> = ({
  lang,
  profile,
  enrollment,
  isGuestMode,
  themeMode,
  palette,
  onChangeThemeMode,
  onChangePalette,
  onGuestBlocked,
  onExitGuest,
}) => {
  const tr = TRANSLATIONS[lang];
  const isTeacher = profile.role === 'teacher';
  const qrLocked = !isTeacher && isStudentLockedByQr(enrollment?.qrToken);

  const [editing, setEditing] = useState(false);
  const [fullName, setFullName] = useState(profile.fullName);
  const [massarNumber, setMassarNumber] = useState(profile.massarNumber);
  const [email, setEmail] = useState(profile.email);
  const [phone, setPhone] = useState(profile.phoneNumber);
  const [birthDate, setBirthDate] = useState(profile.birthDate);
  const [newPassword, setNewPassword] = useState('');
  const [saveMsg, setSaveMsg] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // PIN config dialog
  const [pinModalOpen, setPinModalOpen] = useState(false);
  const [pinEnabled, setPinEnabled] = useState(() => isPinEnabledForUser(profile.userId));
  const [pinInput, setPinInput] = useState('');
  const [pinConfirm, setPinConfirm] = useState('');
  const [pinErr, setPinErr] = useState<string | null>(null);

  const [legalDoc, setLegalDoc] = useState<LegalDocType>(null);

  const levelInfo = computeLevelFromXp(profile.xp, lang);
  const unlockedSet = new Set(profile.unlockedBadgeIds);

  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (isGuestMode) {
      onGuestBlocked();
      return;
    }
    try {
      const dataUrl = await compressImageToDataUrl(file, 480, 180_000);
      await updateAvatarDataUrl(profile.userId, dataUrl);
      setSaveMsg('Photo de profil mise à jour.');
    } catch (err: any) {
      alert(err?.message || 'Erreur photo');
    } finally {
      e.target.value = '';
    }
  };

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isGuestMode) {
      onGuestBlocked();
      return;
    }
    setBusy(true);
    setSaveMsg(null);
    try {
      await updateStudentProfileData({
        profile,
        isQrLocked: qrLocked,
        fullName,
        phoneNumber: phone,
        massarNumber,
        birthDate,
        email,
        newPassword,
      });
      setNewPassword('');
      setEditing(false);
      setSaveMsg('Profil enregistré ✓');
    } catch (err: any) {
      alert(err?.message || 'Erreur de sauvegarde.');
    } finally {
      setBusy(false);
    }
  };

  const handlePinSave = (e: React.FormEvent) => {
    e.preventDefault();
    setPinErr(null);
    if (!isValidPinFormat(pinInput)) {
      setPinErr('Le code PIN doit contenir entre 4 et 6 chiffres.');
      return;
    }
    if (pinInput !== pinConfirm) {
      setPinErr('Les codes PIN ne correspondent pas.');
      return;
    }
    setUserPin(profile.userId, pinInput);
    setPinEnabled(true);
    setPinInput('');
    setPinConfirm('');
    setPinModalOpen(false);
  };

  return (
    <div className="space-y-4">
      {/* Avatar & Identity Card */}
      <div className="rounded-3xl border border-svt-outline/20 bg-svt-surface p-5 shadow-sm">
        <div className="flex flex-wrap items-center gap-4">
          <div className="relative">
            {profile.avatarUrl ? (
              <img
                src={profile.avatarUrl}
                alt={profile.fullName}
                className="h-20 w-20 rounded-3xl object-cover border-2 border-svt-primary/40"
              />
            ) : (
              <div className="flex h-20 w-20 items-center justify-center rounded-3xl bg-svt-primaryContainer text-svt-primary">
                <UserIcon className="h-9 w-9" />
              </div>
            )}
            <label className="absolute -end-1 -bottom-1 flex h-9 w-9 cursor-pointer items-center justify-center rounded-full bg-svt-primary text-svt-onPrimary shadow">
              <Camera className="h-4 w-4" />
              <input
                type="file"
                accept="image/*"
                className="hidden"
                onChange={handleAvatarChange}
              />
            </label>
          </div>

          <div className="min-w-0 flex-1">
            <h2 className="truncate font-display text-xl font-extrabold">{profile.fullName}</h2>
            <p className="text-xs font-semibold text-svt-secondary">
              {isTeacher ? profile.establishment || 'Professeur SVT' : enrollment?.className || 'Élève SVT'}
            </p>
            {profile.massarNumber && (
              <p className="mt-0.5 font-mono text-xs text-svt-onSurfaceVariant">
                Massar : {profile.massarNumber}
              </p>
            )}
            {qrLocked && (
              <div className="mt-1.5 inline-flex items-center gap-1 rounded-full bg-svt-primaryContainer px-2.5 py-0.5 text-[11px] font-bold text-svt-onPrimaryContainer">
                <Lock className="h-3 w-3" />
                <span>{tr.qrLockedBadge}</span>
              </div>
            )}
          </div>
        </div>

        {qrLocked && (
          <p className="mt-3 rounded-2xl bg-svt-surfaceVariant/60 p-3 text-xs text-svt-onSurfaceVariant">
            {tr.qrLockedNotice}
          </p>
        )}

        {saveMsg && (
          <p className="mt-3 rounded-xl bg-svt-success/15 px-3 py-2 text-xs font-bold text-svt-success">
            {saveMsg}
          </p>
        )}

        {!editing ? (
          <div className="mt-4 space-y-1.5 text-xs text-svt-onSurfaceVariant">
            <div>E-mail : {profile.email || '—'}</div>
            <div>Téléphone : {profile.phoneNumber || '—'}</div>
            {!isTeacher && <div>Date de naissance : {profile.birthDate || '—'}</div>}
            <button
              type="button"
              onClick={() => {
                if (isGuestMode) onGuestBlocked();
                else {
                  setFullName(profile.fullName);
                  setMassarNumber(profile.massarNumber);
                  setEmail(profile.email);
                  setPhone(profile.phoneNumber);
                  setBirthDate(profile.birthDate);
                  setEditing(true);
                }
              }}
              className="mt-2 min-h-[42px] rounded-2xl border border-svt-outline/30 px-4 text-xs font-bold text-svt-onSurface hover:bg-svt-surfaceVariant/50"
            >
              {tr.editProfile}
            </button>
          </div>
        ) : (
          <form onSubmit={handleSaveProfile} className="mt-4 space-y-2.5">
            <div>
              <label className="mb-1 block text-[11px] font-bold text-svt-onSurfaceVariant">
                {tr.fullNameLabel} {qrLocked ? '🔒' : ''}
              </label>
              <input
                type="text"
                disabled={qrLocked}
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                className="min-h-[44px] w-full rounded-xl border border-svt-outline/30 bg-svt-bg px-3 text-sm disabled:opacity-60"
              />
            </div>

            {!isTeacher && (
              <div>
                <label className="mb-1 block text-[11px] font-bold text-svt-onSurfaceVariant">
                  {tr.massarLabel} {qrLocked ? '🔒' : ''}
                </label>
                <input
                  type="text"
                  disabled={qrLocked}
                  value={massarNumber}
                  onChange={(e) => setMassarNumber(e.target.value)}
                  className="min-h-[44px] w-full rounded-xl border border-svt-outline/30 bg-svt-bg px-3 font-mono text-sm uppercase disabled:opacity-60"
                />
              </div>
            )}

            <div>
              <label className="mb-1 block text-[11px] font-bold text-svt-onSurfaceVariant">
                {tr.recoveryEmailLabel}
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="numeromassar@taalim.ma"
                className="min-h-[44px] w-full rounded-xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
              />
            </div>

            <div>
              <label className="mb-1 block text-[11px] font-bold text-svt-onSurfaceVariant">
                {tr.phoneOptional}
              </label>
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="min-h-[44px] w-full rounded-xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
              />
            </div>

            {!isTeacher && (
              <div>
                <label className="mb-1 block text-[11px] font-bold text-svt-onSurfaceVariant">
                  {tr.birthDateLabel}
                </label>
                <input
                  type="text"
                  value={birthDate}
                  onChange={(e) => setBirthDate(e.target.value)}
                  placeholder="JJ/MM/AAAA"
                  className="min-h-[44px] w-full rounded-xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
                />
              </div>
            )}

            <div>
              <label className="mb-1 block text-[11px] font-bold text-svt-onSurfaceVariant">
                {tr.changePasswordOptional}
              </label>
              <input
                type="password"
                minLength={6}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="••••••••"
                className="min-h-[44px] w-full rounded-xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
              />
            </div>

            <div className="flex gap-2 pt-1">
              <button
                type="button"
                onClick={() => setEditing(false)}
                className="min-h-[42px] rounded-xl px-4 text-xs font-bold"
              >
                {tr.cancel}
              </button>
              <button
                type="submit"
                disabled={busy}
                className="min-h-[42px] rounded-2xl bg-svt-primary px-5 text-xs font-bold text-svt-onPrimary"
              >
                {tr.save}
              </button>
            </div>
          </form>
        )}
      </div>

      {/* XP Level & Badges (Students) */}
      {!isTeacher && (
        <div className="space-y-3 rounded-3xl border border-svt-outline/20 bg-svt-surface p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 font-display text-base font-bold">
              <Award className="h-5 w-5 text-svt-tertiary" />
              <span>{tr.badgesTitle}</span>
            </div>
            <span className="inline-flex items-center gap-1 rounded-full bg-svt-tertiaryContainer px-3 py-1 text-xs font-bold text-svt-onTertiaryContainer">
              <Flame className="h-3.5 w-3.5" />
              {profile.loginStreak}j
            </span>
          </div>

          <div className="rounded-2xl bg-svt-bg p-3.5">
            <div className="flex items-center justify-between text-xs font-bold">
              <span>
                Niveau {levelInfo.level} • {levelInfo.title}
              </span>
              <span className="text-svt-primary">{profile.xp} XP</span>
            </div>
            <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-svt-surfaceVariant">
              <div
                className="h-full rounded-full bg-svt-primary transition-all"
                style={{ width: `${Math.round(levelInfo.progress * 100)}%` }}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-3">
            {SVT_BADGES.map((b) => {
              const unlocked = unlockedSet.has(b.id);
              return (
                <div
                  key={b.id}
                  className={`rounded-2xl border p-3 text-xs transition ${
                    unlocked
                      ? 'border-svt-primary/35 bg-svt-primaryContainer/40'
                      : 'border-svt-outline/15 bg-svt-bg/60 opacity-60'
                  }`}
                >
                  <div className="text-2xl">{b.emoji}</div>
                  <div className="mt-1 font-display font-bold">{b.title[lang]}</div>
                  <p className="mt-0.5 text-[11px] text-svt-onSurfaceVariant">
                    {b.description[lang]}
                  </p>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Theme & Palette Switcher */}
      <div className="space-y-3 rounded-3xl border border-svt-outline/20 bg-svt-surface p-5 shadow-sm">
        <div className="flex items-center gap-2 font-display text-base font-bold">
          <Palette className="h-5 w-5 text-svt-primary" />
          <span>{tr.themeSectionTitle}</span>
        </div>

        <div>
          <label className="mb-1.5 block text-xs font-bold text-svt-onSurfaceVariant">
            {tr.themeModeLabel}
          </label>
          <div className="grid grid-cols-3 gap-2">
            {(['system', 'light', 'dark'] as ThemeMode[]).map((m) => (
              <button
                key={m}
                type="button"
                onClick={() => onChangeThemeMode(m)}
                className={`min-h-[42px] rounded-2xl text-xs font-bold ${
                  themeMode === m
                    ? 'bg-svt-primary text-svt-onPrimary'
                    : 'border border-svt-outline/25 bg-svt-bg'
                }`}
              >
                {m === 'system' ? tr.themeSystem : m === 'light' ? tr.themeLight : tr.themeDark}
              </button>
            ))}
          </div>
        </div>

        <div>
          <label className="mb-1.5 block text-xs font-bold text-svt-onSurfaceVariant">
            {tr.paletteLabel}
          </label>
          <div className="grid grid-cols-2 gap-2">
            {(
              [
                { id: 'emerald', label: tr.paletteEmerald, hex: '#1B7A54' },
                { id: 'ocean', label: tr.paletteOcean, hex: '#0E609C' },
                { id: 'earth', label: tr.paletteEarth, hex: '#A64B21' },
                { id: 'botanical', label: tr.paletteBotanical, hex: '#6B3FA0' },
              ] as { id: AppPalette; label: string; hex: string }[]
            ).map((p) => (
              <button
                key={p.id}
                type="button"
                onClick={() => onChangePalette(p.id)}
                className={`flex min-h-[44px] items-center gap-2 rounded-2xl border px-3 text-xs font-bold ${
                  palette === p.id
                    ? 'border-svt-primary bg-svt-primaryContainer/50 text-svt-onPrimaryContainer'
                    : 'border-svt-outline/20 bg-svt-bg'
                }`}
              >
                <span
                  className="h-4 w-4 shrink-0 rounded-full"
                  style={{ backgroundColor: p.hex }}
                />
                <span className="truncate">{p.label}</span>
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Security PIN */}
      {!isGuestMode && (
        <div className="space-y-2 rounded-3xl border border-svt-outline/20 bg-svt-surface p-5 shadow-sm">
          <div className="flex items-center gap-2 font-display text-base font-bold">
            <ShieldCheck className="h-5 w-5 text-svt-secondary" />
            <span>{tr.pinTitle}</span>
          </div>
          <p className="text-xs text-svt-onSurfaceVariant">
            {pinEnabled ? tr.pinEnabledDesc : tr.pinDisabledDesc}
          </p>
          <button
            type="button"
            onClick={() => setPinModalOpen(true)}
            className="min-h-[42px] rounded-2xl border border-svt-outline/30 px-4 text-xs font-bold"
          >
            {pinEnabled ? tr.pinManage : tr.pinConfigure}
          </button>
        </div>
      )}

      {/* Legal & Sign Out */}
      <div className="space-y-3 rounded-3xl border border-svt-outline/20 bg-svt-surface p-5 shadow-sm">
        <div className="flex flex-wrap gap-2 text-xs">
          <button
            type="button"
            onClick={() => setLegalDoc('privacy')}
            className="rounded-xl bg-svt-surfaceVariant/60 px-3 py-2 font-bold"
          >
            {tr.privacyTitle}
          </button>
          <button
            type="button"
            onClick={() => setLegalDoc('terms')}
            className="rounded-xl bg-svt-surfaceVariant/60 px-3 py-2 font-bold"
          >
            {tr.termsModalTitle}
          </button>
          <button
            type="button"
            onClick={() => setLegalDoc('legal')}
            className="rounded-xl bg-svt-surfaceVariant/60 px-3 py-2 font-bold"
          >
            {tr.legalTitle}
          </button>
        </div>

        <button
          type="button"
          onClick={() => (isGuestMode ? onExitGuest() : signOutAccount())}
          className="flex min-h-[48px] w-full items-center justify-center gap-2 rounded-2xl bg-svt-error/15 text-sm font-bold text-svt-error hover:bg-svt-error/25"
        >
          <LogOut className="h-4 w-4" />
          <span>{isGuestMode ? tr.guestExit : tr.signOut}</span>
        </button>
      </div>

      {/* PIN Modal */}
      {pinModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/65 p-4 backdrop-blur-sm">
          <form
            onSubmit={handlePinSave}
            className="w-full max-w-sm space-y-3 rounded-3xl bg-svt-surface p-5 text-svt-onSurface shadow-2xl"
          >
            <h3 className="font-display text-lg font-bold">{tr.pinTitle}</h3>
            {pinErr && (
              <p className="rounded-xl bg-svt-error/10 p-2.5 text-xs font-bold text-svt-error">
                {pinErr}
              </p>
            )}
            <input
              type="password"
              inputMode="numeric"
              maxLength={6}
              value={pinInput}
              onChange={(e) => setPinInput(e.target.value.replace(/\D/g, ''))}
              placeholder="Nouveau PIN (4–6 chiffres)"
              className="min-h-[46px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3 text-sm tracking-widest"
            />
            <input
              type="password"
              inputMode="numeric"
              maxLength={6}
              value={pinConfirm}
              onChange={(e) => setPinConfirm(e.target.value.replace(/\D/g, ''))}
              placeholder="Confirmer le code PIN"
              className="min-h-[46px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3 text-sm tracking-widest"
            />
            <div className="flex flex-wrap justify-end gap-2 pt-1">
              {pinEnabled && (
                <button
                  type="button"
                  onClick={() => {
                    clearUserPin(profile.userId);
                    setPinEnabled(false);
                    setPinModalOpen(false);
                  }}
                  className="min-h-[42px] rounded-xl bg-svt-error/15 px-3 text-xs font-bold text-svt-error"
                >
                  {tr.pinDisableBtn}
                </button>
              )}
              <button
                type="button"
                onClick={() => setPinModalOpen(false)}
                className="min-h-[42px] px-3 text-xs font-bold"
              >
                {tr.cancel}
              </button>
              <button
                type="submit"
                className="min-h-[42px] rounded-xl bg-svt-primary px-4 text-xs font-bold text-svt-onPrimary"
              >
                {tr.save}
              </button>
            </div>
          </form>
        </div>
      )}

      <LegalModal docType={legalDoc} lang={lang} onClose={() => setLegalDoc(null)} />
    </div>
  );
};
