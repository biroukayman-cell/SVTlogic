import React from 'react';
import { ShieldCheck, X } from 'lucide-react';
import { AppLanguage } from '../types/svt';
import { TRANSLATIONS } from '../lib/i18n';

export type LegalDocType = 'legal' | 'privacy' | 'terms' | null;

export const LegalModal: React.FC<{
  docType: LegalDocType;
  lang: AppLanguage;
  onClose: () => void;
}> = ({ docType, lang, onClose }) => {
  if (!docType) return null;
  const tr = TRANSLATIONS[lang];
  const title =
    docType === 'legal' ? tr.legalTitle : docType === 'privacy' ? tr.privacyTitle : tr.termsModalTitle;
  const body =
    docType === 'legal' ? tr.legalBody : docType === 'privacy' ? tr.privacyBody : tr.termsBody;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/65 p-4 backdrop-blur-sm">
      <div className="flex max-h-[85dvh] w-full max-w-lg flex-col overflow-hidden rounded-3xl bg-svt-surface text-svt-onSurface shadow-2xl">
        <div className="flex items-center justify-between border-b border-svt-outline/20 px-5 py-3.5">
          <div className="flex items-center gap-2 font-display text-base font-bold">
            <ShieldCheck className="h-5 w-5 text-svt-primary" />
            <span>{title}</span>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="flex h-10 w-10 items-center justify-center rounded-full hover:bg-svt-surfaceVariant"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="overflow-y-auto p-5 text-sm leading-relaxed whitespace-pre-line text-svt-onSurfaceVariant">
          {body}
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
