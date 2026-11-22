import React, { useEffect, useState } from 'react';
import QRCode from 'qrcode';
import { Check, Copy, Download, X } from 'lucide-react';
import { SvtClass } from '../types/svt';
import { buildClassQrPayload } from '../lib/authHelpers';

interface QrDisplayModalProps {
  svtClass: SvtClass | null;
  onClose: () => void;
}

export const QrDisplayModal: React.FC<QrDisplayModalProps> = ({ svtClass, onClose }) => {
  const [dataUrl, setDataUrl] = useState<string>('');
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!svtClass) return;
    const payload = buildClassQrPayload(svtClass.qrToken);
    QRCode.toDataURL(payload, { width: 320, margin: 2 })
      .then(setDataUrl)
      .catch(() => setDataUrl(''));
  }, [svtClass]);

  if (!svtClass) return null;
  const payload = buildClassQrPayload(svtClass.qrToken);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(svtClass.qrToken);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {}
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/65 p-4 backdrop-blur-sm">
      <div className="w-full max-w-sm rounded-3xl bg-svt-surface p-5 text-svt-onSurface shadow-2xl">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="font-display text-lg font-bold">{svtClass.name}</h3>
            <p className="text-xs text-svt-onSurfaceVariant">
              {svtClass.level} • {svtClass.academicYear}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="flex h-10 w-10 items-center justify-center rounded-full hover:bg-svt-surfaceVariant"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="my-5 flex flex-col items-center rounded-2xl bg-white p-4 shadow-inner">
          {dataUrl && <img src={dataUrl} alt={payload} className="h-56 w-56" />}
          <span className="mt-2 font-mono text-sm font-bold text-[#0F4C33]">{payload}</span>
        </div>

        <div className="flex gap-2">
          <button
            type="button"
            onClick={handleCopy}
            className="flex min-h-[48px] flex-1 items-center justify-center gap-2 rounded-2xl border border-svt-outline/30 px-3 text-sm font-bold hover:bg-svt-surfaceVariant"
          >
            {copied ? <Check className="h-4 w-4 text-svt-success" /> : <Copy className="h-4 w-4" />}
            <span>{copied ? 'Copié ✓' : svtClass.qrToken}</span>
          </button>
          {dataUrl && (
            <a
              href={dataUrl}
              download={`QR-${svtClass.qrToken}.png`}
              className="flex min-h-[48px] items-center justify-center gap-2 rounded-2xl bg-svt-primary px-4 text-sm font-bold text-svt-onPrimary"
            >
              <Download className="h-4 w-4" />
              <span>PNG</span>
            </a>
          )}
        </div>
      </div>
    </div>
  );
};
