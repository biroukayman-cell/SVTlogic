import React, { useEffect, useState } from 'react';
import { Delete, LockKeyhole, LogOut } from 'lucide-react';
import { AppLanguage } from '../types/svt';
import { TRANSLATIONS } from '../lib/i18n';
import { getPinLockoutRemainingSeconds, verifyUserPin } from '../lib/pinSecurity';

interface PinLockOverlayProps {
  uid: string;
  userName: string;
  lang: AppLanguage;
  onUnlocked: () => void;
  onSignOut: () => void;
}

export const PinLockOverlay: React.FC<PinLockOverlayProps> = ({
  uid,
  userName,
  lang,
  onUnlocked,
  onSignOut,
}) => {
  const tr = TRANSLATIONS[lang];
  const [pin, setPin] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [lockSeconds, setLockSeconds] = useState(() => getPinLockoutRemainingSeconds(uid));

  useEffect(() => {
    if (lockSeconds <= 0) return;
    const t = setInterval(() => {
      const rem = getPinLockoutRemainingSeconds(uid);
      setLockSeconds(rem);
      if (rem <= 0) setErrorMsg(null);
    }, 1000);
    return () => clearInterval(t);
  }, [lockSeconds, uid]);

  const handleDigit = (d: string) => {
    if (lockSeconds > 0 || pin.length >= 6) return;
    setErrorMsg(null);
    setPin((prev) => prev + d);
  };

  const handleSubmit = () => {
    if (lockSeconds > 0 || pin.length < 4) return;
    const res = verifyUserPin(uid, pin);
    if (res.ok) {
      setPin('');
      onUnlocked();
    } else if (res.lockedSeconds > 0) {
      setLockSeconds(res.lockedSeconds);
      setPin('');
      setErrorMsg(`${tr.pinLockedSeconds} ${res.lockedSeconds}s`);
    } else {
      setPin('');
      setErrorMsg(`${tr.pinWrongAttempts} (${res.remainingAttempts})`);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-svt-bg p-6 text-svt-onSurface">
      <div className="w-full max-w-xs rounded-3xl bg-svt-surface p-6 text-center shadow-xl">
        <div className="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-2xl bg-svt-primaryContainer text-svt-primary">
          <LockKeyhole className="h-7 w-7" />
        </div>
        <h2 className="font-display text-xl font-bold">{tr.pinUnlockTitle}</h2>
        <p className="mt-1 text-xs text-svt-onSurfaceVariant">{userName}</p>

        <div className="my-5 flex justify-center gap-2.5">
          {Array.from({ length: 6 }).map((_, idx) => (
            <div
              key={idx}
              className={`h-3.5 w-3.5 rounded-full border transition ${
                idx < pin.length
                  ? 'border-svt-primary bg-svt-primary scale-110'
                  : 'border-svt-outline/40 bg-transparent'
              }`}
            />
          ))}
        </div>

        {(errorMsg || lockSeconds > 0) && (
          <p className="mb-3 rounded-xl bg-svt-error/10 px-3 py-1.5 text-xs font-semibold text-svt-error">
            {lockSeconds > 0 ? `${tr.pinLockedSeconds} ${lockSeconds}s` : errorMsg}
          </p>
        )}

        <div className="grid grid-cols-3 gap-2.5">
          {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((digit) => (
            <button
              key={digit}
              type="button"
              disabled={lockSeconds > 0}
              onClick={() => handleDigit(digit)}
              className="flex h-12 items-center justify-center rounded-2xl bg-svt-surfaceVariant/70 font-display text-lg font-bold hover:bg-svt-primaryContainer active:scale-95"
            >
              {digit}
            </button>
          ))}
          <button
            type="button"
            onClick={onSignOut}
            title={tr.signOut}
            className="flex h-12 items-center justify-center rounded-2xl text-svt-error hover:bg-svt-error/10"
          >
            <LogOut className="h-5 w-5" />
          </button>
          <button
            type="button"
            disabled={lockSeconds > 0}
            onClick={() => handleDigit('0')}
            className="flex h-12 items-center justify-center rounded-2xl bg-svt-surfaceVariant/70 font-display text-lg font-bold hover:bg-svt-primaryContainer active:scale-95"
          >
            0
          </button>
          <button
            type="button"
            onClick={() => setPin((p) => p.slice(0, -1))}
            className="flex h-12 items-center justify-center rounded-2xl bg-svt-surfaceVariant/50 hover:bg-svt-surfaceVariant"
          >
            <Delete className="h-5 w-5" />
          </button>
        </div>

        <button
          type="button"
          disabled={pin.length < 4 || lockSeconds > 0}
          onClick={handleSubmit}
          className="mt-4 min-h-[48px] w-full rounded-2xl bg-svt-primary font-bold text-svt-onPrimary disabled:opacity-50"
        >
          {tr.pinUnlockBtn}
        </button>
      </div>
    </div>
  );
};
