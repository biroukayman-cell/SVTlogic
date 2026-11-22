import React, { useEffect, useRef, useState } from 'react';
import jsQR from 'jsqr';
import { Camera, ImagePlus, Keyboard, QrCode, X } from 'lucide-react';
import { AppLanguage } from '../types/svt';
import { TRANSLATIONS } from '../lib/i18n';

interface QrScannerModalProps {
  open: boolean;
  lang: AppLanguage;
  onDetected: (rawCode: string) => void;
  onClose: () => void;
}

export const QrScannerModal: React.FC<QrScannerModalProps> = ({
  open,
  lang,
  onDetected,
  onClose,
}) => {
  const tr = TRANSLATIONS[lang];
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const rafRef = useRef<number | null>(null);

  const [cameraError, setCameraError] = useState<string | null>(null);
  const [imageError, setImageError] = useState<string | null>(null);
  const [manualInput, setManualInput] = useState('');
  const [cameraActive, setCameraActive] = useState(false);

  const stopCamera = () => {
    if (rafRef.current !== null) {
      cancelAnimationFrame(rafRef.current);
      rafRef.current = null;
    }
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((t) => t.stop());
      streamRef.current = null;
    }
    setCameraActive(false);
  };

  useEffect(() => {
    if (!open) {
      stopCamera();
      setCameraError(null);
      setImageError(null);
      return;
    }

    let cancelled = false;
    const startCamera = async () => {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        setCameraError('Caméra non disponible sur ce navigateur. Utilisez l’import photo ou le code manuel.');
        return;
      }
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: { ideal: 'environment' } },
          audio: false,
        });
        if (cancelled) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }
        streamRef.current = stream;
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          videoRef.current.setAttribute('playsinline', 'true');
          await videoRef.current.play();
          setCameraActive(true);
          scanFrame();
        }
      } catch (err: any) {
        setCameraError(
          err?.name === 'NotAllowedError'
            ? 'Autorisation caméra refusée. Activez l’accès dans Safari ou importez une photo QR.'
            : 'Impossible de démarrer la caméra. Utilisez l’import d’image ou le code manuel.'
        );
      }
    };

    const scanFrame = () => {
      if (cancelled) return;
      const video = videoRef.current;
      const canvas = canvasRef.current;
      if (video && canvas && video.readyState >= video.HAVE_ENOUGH_DATA) {
        const w = video.videoWidth;
        const h = video.videoHeight;
        if (w > 0 && h > 0) {
          canvas.width = w;
          canvas.height = h;
          const ctx = canvas.getContext('2d', { willReadFrequently: true });
          if (ctx) {
            ctx.drawImage(video, 0, 0, w, h);
            const imgData = ctx.getImageData(0, 0, w, h);
            const decoded = jsQR(imgData.data, w, h, { inversionAttempts: 'attemptBoth' });
            if (decoded && decoded.data) {
              stopCamera();
              onDetected(decoded.data.trim());
              return;
            }
          }
        }
      }
      rafRef.current = requestAnimationFrame(scanFrame);
    };

    startCamera();
    return () => {
      cancelled = true;
      stopCamera();
    };
  }, [open]);

  const handleImagePick = async (e: React.ChangeEvent<HTMLInputElement>) => {
    setImageError(null);
    const file = e.target.files?.[0];
    if (!file) return;
    const url = URL.createObjectURL(file);
    try {
      const img = await new Promise<HTMLImageElement>((resolve, reject) => {
        const el = new Image();
        el.onload = () => resolve(el);
        el.onerror = () => reject(new Error('Invalid image'));
        el.src = url;
      });
      const maxSide = 1400;
      const scale = Math.min(1, maxSide / Math.max(img.width, img.height));
      const w = Math.max(1, Math.round(img.width * scale));
      const h = Math.max(1, Math.round(img.height * scale));
      const canvas = document.createElement('canvas');
      canvas.width = w;
      canvas.height = h;
      const ctx = canvas.getContext('2d');
      if (!ctx) throw new Error('No canvas');
      ctx.drawImage(img, 0, 0, w, h);
      const imageData = ctx.getImageData(0, 0, w, h);
      const decoded = jsQR(imageData.data, w, h, { inversionAttempts: 'attemptBoth' });
      if (decoded && decoded.data) {
        stopCamera();
        onDetected(decoded.data.trim());
      } else {
        setImageError(tr.qrNotFoundInImage);
      }
    } catch {
      setImageError(tr.qrNotFoundInImage);
    } finally {
      URL.revokeObjectURL(url);
      e.target.value = '';
    }
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-sm">
      <div className="w-full max-w-md overflow-hidden rounded-3xl bg-svt-surface text-svt-onSurface shadow-2xl">
        <div className="flex items-center justify-between border-b border-svt-outline/20 px-5 py-3.5">
          <div className="flex items-center gap-2 font-display text-base font-bold">
            <QrCode className="h-5 w-5 text-svt-primary" />
            <span>{tr.qrScanBtn}</span>
          </div>
          <button
            type="button"
            onClick={() => {
              stopCamera();
              onClose();
            }}
            className="flex h-10 w-10 items-center justify-center rounded-full hover:bg-svt-surfaceVariant"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="space-y-4 p-4">
          <div className="relative aspect-[4/3] w-full overflow-hidden rounded-2xl bg-black">
            <video
              ref={videoRef}
              playsInline
              muted
              autoPlay
              className="h-full w-full object-cover"
            />
            <canvas ref={canvasRef} className="hidden" />
            {cameraActive && (
              <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
                <div className="h-44 w-44 rounded-2xl border-2 border-emerald-400 shadow-[0_0_0_999px_rgba(0,0,0,0.35)]" />
              </div>
            )}
            {cameraError && (
              <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 bg-black/80 p-5 text-center text-xs text-white">
                <Camera className="h-8 w-8 text-emerald-300" />
                <p>{cameraError}</p>
              </div>
            )}
          </div>

          <label className="flex min-h-[48px] cursor-pointer items-center justify-center gap-2 rounded-2xl border border-svt-primary/40 bg-svt-primaryContainer/40 px-4 py-2.5 text-sm font-bold text-svt-primary transition hover:bg-svt-primaryContainer/70">
            <ImagePlus className="h-4 w-4" />
            <span>{tr.qrPickGallery}</span>
            <input type="file" accept="image/*" className="hidden" onChange={handleImagePick} />
          </label>

          {imageError && (
            <p className="rounded-xl bg-svt-error/10 px-3 py-2 text-xs font-semibold text-svt-error">
              {imageError}
            </p>
          )}

          <div className="border-t border-svt-outline/20 pt-3">
            <label className="mb-1.5 flex items-center gap-1.5 text-xs font-semibold text-svt-onSurfaceVariant">
              <Keyboard className="h-3.5 w-3.5" />
              <span>{tr.qrManualLabel}</span>
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                value={manualInput}
                onChange={(e) => setManualInput(e.target.value)}
                placeholder="BIOTERRE:SVT-2BAC-A1"
                className="min-h-[48px] flex-1 rounded-xl border border-svt-outline/30 bg-svt-bg px-3 text-sm outline-none focus:border-svt-primary"
              />
              <button
                type="button"
                disabled={manualInput.trim().length < 3}
                onClick={() => {
                  stopCamera();
                  onDetected(manualInput.trim());
                }}
                className="min-h-[48px] rounded-xl bg-svt-primary px-4 text-sm font-bold text-svt-onPrimary disabled:opacity-50"
              >
                OK
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
