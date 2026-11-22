import JSZip from 'jszip';
import * as XLSX from 'xlsx';
import QRCode from 'qrcode';
import { ParsedRosterEntry, ResourceType, RosterStudent, SvtClass } from '../types/svt';
import { buildStudentQrPayload, normalizeMassar } from './authHelpers';

export const CHUNK_SIZE_BYTES = 600 * 1024;
export const MAX_UPLOAD_BYTES = 20 * 1024 * 1024;
export const CHUNK_URL_PREFIX = 'bioterre-chunk://firestore/';

export function isChunkedResourceUrl(url: string): boolean {
  return url.startsWith(CHUNK_URL_PREFIX);
}

export function parseChunkResourceUrl(url: string): { resourceId: string; ext: string } | null {
  if (!isChunkedResourceUrl(url)) return null;
  const body = url.slice(CHUNK_URL_PREFIX.length);
  const [resourceId, query] = body.split('?');
  if (!resourceId) return null;
  const params = new URLSearchParams(query || '');
  return {
    resourceId,
    ext: (params.get('ext') || 'bin').toLowerCase(),
  };
}

export function getMimeTypeForResource(type: ResourceType, ext: string): string {
  const cleanExt = ext.toLowerCase().replace(/^\./, '');
  if (type === 'PDF' || cleanExt === 'pdf') return 'application/pdf';
  if (type === 'VIDEO') {
    if (cleanExt === 'webm') return 'video/webm';
    if (cleanExt === 'mov') return 'video/quicktime';
    return 'video/mp4';
  }
  if (type === 'IMAGE') {
    if (cleanExt === 'png') return 'image/png';
    if (cleanExt === 'webp') return 'image/webp';
    return 'image/jpeg';
  }
  if (cleanExt === 'docx') {
    return 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
  }
  if (cleanExt === 'doc') return 'application/msword';
  return 'application/octet-stream';
}

export async function compressImageToDataUrl(
  file: File,
  maxDimension = 960,
  maxChars = 220_000
): Promise<string> {
  const imgUrl = URL.createObjectURL(file);
  try {
    const img = await new Promise<HTMLImageElement>((resolve, reject) => {
      const el = new Image();
      el.onload = () => resolve(el);
      el.onerror = () => reject(new Error('Invalid image'));
      el.src = imgUrl;
    });

    let { width, height } = img;
    if (width > maxDimension || height > maxDimension) {
      const ratio = Math.min(maxDimension / width, maxDimension / height);
      width = Math.max(1, Math.round(width * ratio));
      height = Math.max(1, Math.round(height * ratio));
    }

    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    if (!ctx) throw new Error('Canvas unavailable');
    ctx.drawImage(img, 0, 0, width, height);

    let quality = 0.82;
    let dataUrl = canvas.toDataURL('image/jpeg', quality);
    while (dataUrl.length > maxChars && quality > 0.3) {
      quality -= 0.12;
      dataUrl = canvas.toDataURL('image/jpeg', quality);
    }
    if (dataUrl.length > maxChars) {
      // Scale down further if needed
      const scale = 0.7;
      canvas.width = Math.max(1, Math.round(width * scale));
      canvas.height = Math.max(1, Math.round(height * scale));
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
      dataUrl = canvas.toDataURL('image/jpeg', 0.65);
    }
    return dataUrl;
  } finally {
    URL.revokeObjectURL(imgUrl);
  }
}

export async function extractDocxText(buffer: ArrayBuffer): Promise<string> {
  const zip = await JSZip.loadAsync(buffer);
  const docFile = zip.file('word/document.xml');
  if (!docFile) {
    throw new Error('Document Word (.docx) invalide ou format .doc ancien non supporté.');
  }
  const xmlText = await docFile.async('string');
  const parser = new DOMParser();
  const xmlDoc = parser.parseFromString(xmlText, 'application/xml');
  const paragraphs = Array.from(xmlDoc.getElementsByTagName('w:p'));
  const lines: string[] = [];
  for (const p of paragraphs) {
    const runs = Array.from(p.getElementsByTagName('w:t'));
    const line = runs.map((t) => t.textContent || '').join('').trim();
    if (line) lines.push(line);
  }
  return lines.join('\n\n');
}

export async function parseRosterSpreadsheet(file: File): Promise<ParsedRosterEntry[]> {
  const buf = await file.arrayBuffer();
  const workbook = XLSX.read(buf, { type: 'array' });
  const sheetName = workbook.SheetNames[0];
  if (!sheetName) return [];
  const sheet = workbook.Sheets[sheetName];
  const rows = XLSX.utils.sheet_to_json<(string | number | null)[]>(sheet, { header: 1 });
  if (rows.length === 0) return [];

  let headerRowIdx = -1;
  let massarCol = -1;
  let nameCol = -1;
  let firstNameCol = -1;
  let phoneCol = -1;

  for (let r = 0; r < Math.min(rows.length, 15); r++) {
    const row = rows[r] || [];
    for (let c = 0; c < row.length; c++) {
      const cell = String(row[c] ?? '').trim().toLowerCase();
      if (!cell) continue;
      if (cell.includes('massar') || cell.includes('cne') || cell.includes('مسار') || cell.includes('code')) {
        massarCol = c;
      } else if (cell.includes('prénom') || cell.includes('prenom') || cell.includes('first')) {
        firstNameCol = c;
      } else if (
        cell.includes('nom') ||
        cell.includes('name') ||
        cell.includes('élève') ||
        cell.includes('eleve') ||
        cell.includes('الاسم')
      ) {
        nameCol = c;
      } else if (cell.includes('tel') || cell.includes('phone') || cell.includes('gsm') || cell.includes('الهاتف')) {
        phoneCol = c;
      }
    }
    if (massarCol !== -1 || nameCol !== -1) {
      headerRowIdx = r;
      break;
    }
  }

  const startIdx = headerRowIdx >= 0 ? headerRowIdx + 1 : 0;
  if (massarCol === -1) massarCol = 0;
  if (nameCol === -1) nameCol = 1;

  const entries: ParsedRosterEntry[] = [];
  const seen = new Set<string>();

  for (let r = startIdx; r < rows.length; r++) {
    const row = rows[r] || [];
    const rawMassar = String(row[massarCol] ?? '').trim();
    const cleanMassar = normalizeMassar(rawMassar);
    if (cleanMassar.length < 4) continue;
    if (seen.has(cleanMassar)) continue;

    const last = String(row[nameCol] ?? '').trim();
    const first = firstNameCol >= 0 ? String(row[firstNameCol] ?? '').trim() : '';
    const fullName = [first, last].filter(Boolean).join(' ').trim() || cleanMassar;
    const phone = phoneCol >= 0 ? String(row[phoneCol] ?? '').trim() : '';

    seen.add(cleanMassar);
    entries.push({ massarNumber: cleanMassar, fullName, phoneNumber: phone });
  }

  return entries;
}

export async function printRosterCredentialCards(
  svtClass: SvtClass,
  students: RosterStudent[]
): Promise<void> {
  if (students.length === 0) return;

  const cardsHtml = await Promise.all(
    students.map(async (s) => {
      const payload = buildStudentQrPayload(svtClass.qrToken, s.massarNumber);
      const qrDataUrl = await QRCode.toDataURL(payload, { width: 160, margin: 1 });
      const taalim = `${s.massarNumber.toLowerCase()}@taalim.ma`;
      return `
        <div class="card">
          <div class="card-head">
            <span>BioTerre SVT • ${escapeHtml(svtClass.name)}</span>
            <span>${escapeHtml(svtClass.level)}</span>
          </div>
          <div class="card-body">
            <div class="info">
              <div class="student-name">${escapeHtml(s.fullName)}</div>
              <div class="row"><span class="lbl">Massar :</span><span class="val">${escapeHtml(s.massarNumber)}</span></div>
              <div class="row"><span class="lbl">Mot de passe :</span><span class="val">${escapeHtml(s.provisionalPassword)}</span></div>
              <div class="row"><span class="lbl">E-mail défaut :</span><span class="val-sm">${escapeHtml(taalim)}</span></div>
              <div class="row"><span class="lbl">Code classe :</span><span class="val-sm">${escapeHtml(svtClass.qrToken)}</span></div>
            </div>
            <img src="${qrDataUrl}" alt="QR ${escapeHtml(s.massarNumber)}" class="qr" />
          </div>
          <div class="card-foot">Scannez ce QR Code dans BioTerre SVT (Web ou Android) pour vous connecter.</div>
        </div>
      `;
    })
  );

  const win = window.open('', '_blank', 'width=900,height=700');
  if (!win) {
    throw new Error('Popup bloquée par le navigateur. Autorisez les popups pour imprimer les fiches.');
  }

  win.document.write(`<!doctype html>
  <html lang="fr">
    <head>
      <meta charset="utf-8" />
      <title>Fiches Élèves QR — ${escapeHtml(svtClass.name)}</title>
      <style>
        body { font-family: system-ui, -apple-system, sans-serif; margin: 16px; color: #13221b; }
        .top-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
        .print-btn { background: #1B7A54; color: #fff; border: none; padding: 10px 18px; border-radius: 8px; font-weight: 700; cursor: pointer; }
        .grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
        .card { border: 1.5px solid #1B7A54; border-radius: 10px; overflow: hidden; break-inside: avoid; }
        .card-head { background: #E4F4EC; color: #0F4C33; font-size: 11px; font-weight: 700; padding: 6px 10px; display: flex; justify-content: space-between; }
        .card-body { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 10px; }
        .info { flex: 1; min-width: 0; }
        .student-name { font-size: 14px; font-weight: 800; margin-bottom: 6px; }
        .row { font-size: 11px; margin-bottom: 3px; }
        .lbl { color: #505a56; }
        .val { font-family: ui-monospace, monospace; font-weight: 700; color: #0c3a28; }
        .val-sm { font-family: ui-monospace, monospace; font-size: 10px; color: #1E6B94; }
        .qr { width: 92px; height: 92px; flex-shrink: 0; }
        .card-foot { border-top: 1px dashed #c4e2d3; font-size: 9px; color: #505a56; padding: 5px 10px; }
        @media print {
          .top-bar { display: none; }
          body { margin: 8mm; }
        }
      </style>
    </head>
    <body>
      <div class="top-bar">
        <div>
          <strong>BioTerre SVT — ${escapeHtml(svtClass.name)}</strong> (${students.length} élèves)
        </div>
        <button class="print-btn" onclick="window.print()">Imprimer / Enregistrer en PDF</button>
      </div>
      <div class="grid">${cardsHtml.join('')}</div>
    </body>
  </html>`);
  win.document.close();
}

function escapeHtml(raw: string): string {
  return raw
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
