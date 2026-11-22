const PIN_KEY_PREFIX = 'bioterre_pin_store_v1_';

interface PinRecord {
  salt: string;
  hash: string;
  failedAttempts: number;
  lockedUntilMs: number;
}

const MAX_ATTEMPTS = 5;
const LOCKOUT_MS = 30_000;

function simpleSaltedHash(pin: string, salt: string): string {
  const input = `${salt}:${pin}:bioterre_svt_2026`;
  let h1 = 0x811c9dc5;
  let h2 = 0x9e3779b9;
  for (let i = 0; i < input.length; i++) {
    const ch = input.charCodeAt(i);
    h1 ^= ch;
    h1 = Math.imul(h1, 0x01000193);
    h2 ^= ch + i;
    h2 = Math.imul(h2, 0x85ebca6b);
  }
  return `${(h1 >>> 0).toString(16).padStart(8, '0')}${(h2 >>> 0).toString(16).padStart(8, '0')}`;
}

function getRecord(uid: string): PinRecord | null {
  try {
    const raw = localStorage.getItem(`${PIN_KEY_PREFIX}${uid}`);
    if (!raw) return null;
    return JSON.parse(raw) as PinRecord;
  } catch {
    return null;
  }
}

function saveRecord(uid: string, rec: PinRecord): void {
  try {
    localStorage.setItem(`${PIN_KEY_PREFIX}${uid}`, JSON.stringify(rec));
  } catch {}
}

export function isPinEnabledForUser(uid: string): boolean {
  if (!uid) return false;
  const rec = getRecord(uid);
  return Boolean(rec && rec.hash);
}

export function isValidPinFormat(pin: string): boolean {
  return /^\d{4,6}$/.test(pin);
}

export function setUserPin(uid: string, pin: string): void {
  const salt = Math.random().toString(36).slice(2, 10);
  const hash = simpleSaltedHash(pin, salt);
  saveRecord(uid, {
    salt,
    hash,
    failedAttempts: 0,
    lockedUntilMs: 0,
  });
}

export function clearUserPin(uid: string): void {
  try {
    localStorage.removeItem(`${PIN_KEY_PREFIX}${uid}`);
  } catch {}
}

export function getPinLockoutRemainingSeconds(uid: string): number {
  const rec = getRecord(uid);
  if (!rec) return 0;
  const diff = rec.lockedUntilMs - Date.now();
  return diff > 0 ? Math.ceil(diff / 1000) : 0;
}

export function verifyUserPin(
  uid: string,
  pin: string
): { ok: boolean; remainingAttempts: number; lockedSeconds: number } {
  const rec = getRecord(uid);
  if (!rec) return { ok: true, remainingAttempts: MAX_ATTEMPTS, lockedSeconds: 0 };

  const now = Date.now();
  if (rec.lockedUntilMs > now) {
    return {
      ok: false,
      remainingAttempts: 0,
      lockedSeconds: Math.ceil((rec.lockedUntilMs - now) / 1000),
    };
  }

  const candidate = simpleSaltedHash(pin, rec.salt);
  if (candidate === rec.hash) {
    saveRecord(uid, { ...rec, failedAttempts: 0, lockedUntilMs: 0 });
    return { ok: true, remainingAttempts: MAX_ATTEMPTS, lockedSeconds: 0 };
  }

  const nextFailed = (rec.failedAttempts || 0) + 1;
  if (nextFailed >= MAX_ATTEMPTS) {
    const lockedUntilMs = now + LOCKOUT_MS;
    saveRecord(uid, { ...rec, failedAttempts: 0, lockedUntilMs });
    return { ok: false, remainingAttempts: 0, lockedSeconds: Math.ceil(LOCKOUT_MS / 1000) };
  }

  saveRecord(uid, { ...rec, failedAttempts: nextFailed, lockedUntilMs: 0 });
  return {
    ok: false,
    remainingAttempts: MAX_ATTEMPTS - nextFailed,
    lockedSeconds: 0,
  };
}
