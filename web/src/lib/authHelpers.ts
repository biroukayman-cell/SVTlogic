import { ParsedQrPayload } from '../types/svt';

export const OWNER_ADMIN_EMAIL = 'biroukayman@gmail.com';
const DEVICE_ID_STORAGE_KEY = 'bioterre_web_device_id';
const PIN_HASH_PREFIX = 'bioterre_pin_v1_';

export function normalizeMassar(raw: string): string {
  return raw
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9_-]/g, '');
}

export function massarToAuthEmail(rawMassar: string): string {
  const clean = normalizeMassar(rawMassar);
  return `massar.${clean.toLowerCase()}@student.bioterre.svt`;
}

export function defaultStudentTaalimEmail(rawMassar: string): string {
  const clean = normalizeMassar(rawMassar);
  if (!clean || clean.startsWith('QR')) return '';
  return `${clean.toLowerCase()}@taalim.ma`;
}

export function generateProvisionalPassword(massar: string): string {
  const clean = normalizeMassar(massar);
  const suffix = clean.length >= 4 ? clean.slice(-4) : clean.padEnd(4, '0');
  const randomDigits = Math.floor(1000 + Math.random() * 9000);
  return `SVT-${suffix}-${randomDigits}`;
}

export function generateQrToken(className: string): string {
  const prefix = className
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 12);
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let rand = '';
  for (let i = 0; i < 6; i++) {
    rand += chars[Math.floor(Math.random() * chars.length)];
  }
  const base = prefix.length >= 2 ? prefix : 'SVT';
  return `${base}-${rand}`.replace(/[^A-Za-z0-9_-]/g, '').slice(0, 48);
}

export function buildClassQrPayload(qrToken: string): string {
  return `BIOTERRE:${qrToken.trim()}`;
}

export function buildStudentQrPayload(qrToken: string, massar: string): string {
  const cleanMassar = normalizeMassar(massar);
  if (!cleanMassar) return buildClassQrPayload(qrToken);
  return `BIOTERRE:${qrToken.trim()}|${cleanMassar}`;
}

export function parseQrPayload(raw: string): ParsedQrPayload | null {
  const trimmed = raw.trim();
  if (!trimmed) return null;

  const withoutPrefix = trimmed.toUpperCase().startsWith('BIOTERRE:')
    ? trimmed.slice('BIOTERRE:'.length).trim()
    : trimmed;

  if (withoutPrefix.includes('|')) {
    const parts = withoutPrefix.split('|');
    const token = (parts[0] || '').trim().replace(/[^A-Za-z0-9_-]/g, '');
    const massar = normalizeMassar(parts[1] || '');
    if (token.length < 3) return null;
    return { classToken: token, massarNumber: massar };
  }

  const token = withoutPrefix.replace(/[^A-Za-z0-9_-]/g, '');
  if (token.length < 3) return null;
  return { classToken: token, massarNumber: '' };
}

export function getOrCreateWebDeviceId(): string {
  try {
    const existing = localStorage.getItem(DEVICE_ID_STORAGE_KEY);
    if (existing && /^[a-zA-Z0-9_-]+$/.test(existing)) {
      return existing;
    }
    const isIos =
      typeof navigator !== 'undefined' &&
      (/iPad|iPhone|iPod/.test(navigator.userAgent) ||
        (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1));
    const prefix = isIos ? 'ios_web_' : 'web_';
    const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let rand = '';
    if (typeof crypto !== 'undefined' && crypto.getRandomValues) {
      const bytes = new Uint8Array(18);
      crypto.getRandomValues(bytes);
      for (let i = 0; i < bytes.length; i++) {
        rand += chars[bytes[i] % chars.length];
      }
    } else {
      for (let i = 0; i < 18; i++) {
        rand += chars[Math.floor(Math.random() * chars.length)];
      }
    }
    const id = `${prefix}${rand}`;
    localStorage.setItem(DEVICE_ID_STORAGE_KEY, id);
    return id;
  } catch {
    return `web_session_${Math.random().toString(36).slice(2, 12)}`;
  }
}

export function getWebDeviceLabel(): string {
  if (typeof navigator === 'undefined') return 'Web Browser';
  const ua = navigator.userAgent || '';
  if (/iPhone/i.test(ua)) return 'iPhone Safari Web';
  if (/iPad/i.test(ua) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)) {
    return 'iPad Safari Web';
  }
  if (/Android/i.test(ua)) return 'Android Web';
  if (/Mac/i.test(ua)) return 'macOS Web';
  if (/Windows/i.test(ua)) return 'Windows Web';
  return 'Web PWA';
}

export function isStudentLockedByQr(qrToken?: string | null): boolean {
  return Boolean(qrToken && qrToken.trim().length > 0);
}

export function todayIsoDate(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function formatDurationClock(totalSeconds: number): string {
  const safe = Math.max(0, Math.floor(totalSeconds));
  const m = Math.floor(safe / 60);
  const s = safe % 60;
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

export { PIN_HASH_PREFIX };
