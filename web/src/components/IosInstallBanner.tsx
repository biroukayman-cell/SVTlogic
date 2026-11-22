import React, { useEffect, useState } from 'react';
import { Share, PlusSquare, X } from 'lucide-react';
import { AppLanguage } from '../types/svt';
import { TRANSLATIONS } from '../lib/i18n';

const DISMISS_KEY = 'bioterre_ios_install_dismissed';

export const IosInstallBanner: React.FC<{ lang: AppLanguage }> = ({ lang }) => {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    try {
      if (localStorage.getItem(DISMISS_KEY) === '1') return;
      const ua = navigator.userAgent || '';
      const isIos =
        /iPhone|iPad|iPod/i.test(ua) ||
        (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
      const isStandalone =
        Boolean((window.navigator as any).standalone) ||
        window.matchMedia('(display-mode: standalone)').matches;
      const isSafari = /Safari/i.test(ua) && !/CriOS|FxiOS|EdgiOS/i.test(ua);
      if (isIos && isSafari && !isStandalone) {
        setVisible(true);
      }
    } catch {}
  }, []);

  if (!visible) return null;
  const tr = TRANSLATIONS[lang];

  const dismiss = () => {
    setVisible(false);
    try {
      localStorage.setItem(DISMISS_KEY, '1');
    } catch {}
  };

  return (
    <div className="mx-3 mt-2 rounded-2xl border border-svt-primary/25 bg-svt-primaryContainer/70 px-3.5 py-2.5 text-svt-onPrimaryContainer shadow-sm backdrop-blur">
      <div className="flex items-start gap-2.5">
        <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-svt-primary text-svt-onPrimary">
          <PlusSquare className="h-4 w-4" />
        </div>
        <div className="min-w-0 flex-1 text-xs leading-relaxed">
          <div className="font-display text-sm font-bold">{tr.iosInstallTitle}</div>
          <div className="mt-0.5 flex flex-wrap items-center gap-1 opacity-90">
            <Share className="inline h-3.5 w-3.5 shrink-0" />
            <span>{tr.iosInstallSteps}</span>
          </div>
        </div>
        <button
          type="button"
          onClick={dismiss}
          aria-label={tr.close}
          className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full hover:bg-black/10"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
};
