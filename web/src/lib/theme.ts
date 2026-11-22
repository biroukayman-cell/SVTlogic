import { AppPalette, ThemeMode } from '../types/svt';

const MODE_KEY = 'bioterre_theme_mode';
const PALETTE_KEY = 'bioterre_theme_palette';

interface PaletteTokens {
  primary: string;
  onPrimary: string;
  primaryContainer: string;
  onPrimaryContainer: string;
  secondary: string;
  onSecondary: string;
  secondaryContainer: string;
  onSecondaryContainer: string;
  tertiary: string;
  tertiaryContainer: string;
  onTertiaryContainer: string;
  bg: string;
  surface: string;
  surfaceVariant: string;
  onSurface: string;
  onSurfaceVariant: string;
  outline: string;
  metaHex: string;
}

const LIGHT_PALETTES: Record<AppPalette, PaletteTokens> = {
  emerald: {
    primary: '27 122 84',
    onPrimary: '255 255 255',
    primaryContainer: '196 240 218',
    onPrimaryContainer: '4 33 20',
    secondary: '30 107 148',
    onSecondary: '255 255 255',
    secondaryContainer: '205 233 250',
    onSecondaryContainer: '6 32 48',
    tertiary: '198 126 21',
    tertiaryContainer: '255 230 191',
    onTertiaryContainer: '43 23 0',
    bg: '244 250 247',
    surface: '255 255 255',
    surfaceVariant: '226 239 232',
    onSurface: '19 34 27',
    onSurfaceVariant: '66 89 78',
    outline: '114 137 125',
    metaHex: '#1B7A54',
  },
  ocean: {
    primary: '14 96 156',
    onPrimary: '255 255 255',
    primaryContainer: '208 231 255',
    onPrimaryContainer: '0 30 54',
    secondary: '13 124 120',
    onSecondary: '255 255 255',
    secondaryContainer: '197 242 238',
    onSecondaryContainer: '0 32 31',
    tertiary: '198 126 21',
    tertiaryContainer: '255 230 191',
    onTertiaryContainer: '43 23 0',
    bg: '243 248 253',
    surface: '255 255 255',
    surfaceVariant: '223 236 248',
    onSurface: '14 27 42',
    onSurfaceVariant: '58 79 99',
    outline: '108 130 150',
    metaHex: '#0E609C',
  },
  earth: {
    primary: '166 75 33',
    onPrimary: '255 255 255',
    primaryContainer: '255 219 208',
    onPrimaryContainer: '57 12 0',
    secondary: '93 107 47',
    onSecondary: '255 255 255',
    secondaryContainer: '225 241 168',
    onSecondaryContainer: '27 33 0',
    tertiary: '198 126 21',
    tertiaryContainer: '255 230 191',
    onTertiaryContainer: '43 23 0',
    bg: '253 247 243',
    surface: '255 255 255',
    surfaceVariant: '243 229 221',
    onSurface: '38 24 18',
    onSurfaceVariant: '95 72 62',
    outline: '142 117 105',
    metaHex: '#A64B21',
  },
  botanical: {
    primary: '107 63 160',
    onPrimary: '255 255 255',
    primaryContainer: '235 221 255',
    onPrimaryContainer: '36 0 90',
    secondary: '37 122 99',
    onSecondary: '255 255 255',
    secondaryContainer: '198 244 227',
    onSecondaryContainer: '0 33 25',
    tertiary: '198 126 21',
    tertiaryContainer: '255 230 191',
    onTertiaryContainer: '43 23 0',
    bg: '249 246 253',
    surface: '255 255 255',
    surfaceVariant: '236 228 247',
    onSurface: '28 20 41',
    onSurfaceVariant: '78 65 99',
    outline: '126 112 148',
    metaHex: '#6B3FA0',
  },
};

const DARK_PALETTES: Record<AppPalette, PaletteTokens> = {
  emerald: {
    primary: '122 217 173',
    onPrimary: '0 56 35',
    primaryContainer: '12 82 54',
    onPrimaryContainer: '196 240 218',
    secondary: '139 203 242',
    onSecondary: '0 52 77',
    secondaryContainer: '12 77 112',
    onSecondaryContainer: '205 233 250',
    tertiary: '247 188 99',
    tertiaryContainer: '101 62 0',
    onTertiaryContainer: '255 230 191',
    bg: '13 23 18',
    surface: '19 32 26',
    surfaceVariant: '36 54 45',
    onSurface: '222 236 228',
    onSurfaceVariant: '169 191 179',
    outline: '108 132 120',
    metaHex: '#13201A',
  },
  ocean: {
    primary: '140 203 255',
    onPrimary: '0 51 86',
    primaryContainer: '0 74 122',
    onPrimaryContainer: '208 231 255',
    secondary: '125 224 218',
    onSecondary: '0 55 53',
    secondaryContainer: '0 80 77',
    onSecondaryContainer: '197 242 238',
    tertiary: '247 188 99',
    tertiaryContainer: '101 62 0',
    onTertiaryContainer: '255 230 191',
    bg: '12 21 30',
    surface: '18 30 42',
    surfaceVariant: '33 49 66',
    onSurface: '222 234 247',
    onSurfaceVariant: '164 186 206',
    outline: '103 125 148',
    metaHex: '#121E2A',
  },
  earth: {
    primary: '255 181 153',
    onPrimary: '92 26 0',
    primaryContainer: '131 51 14',
    onPrimaryContainer: '255 219 208',
    secondary: '197 213 136',
    onSecondary: '45 54 0',
    secondaryContainer: '67 79 25',
    onSecondaryContainer: '225 241 168',
    tertiary: '247 188 99',
    tertiaryContainer: '101 62 0',
    onTertiaryContainer: '255 230 191',
    bg: '27 18 14',
    surface: '37 26 21',
    surfaceVariant: '57 43 36',
    onSurface: '242 228 220',
    onSurfaceVariant: '204 181 170',
    outline: '146 122 110',
    metaHex: '#251A15',
  },
  botanical: {
    primary: '213 187 255',
    onPrimary: '60 18 112',
    primaryContainer: '83 40 136',
    onPrimaryContainer: '235 221 255',
    secondary: '136 226 196',
    onSecondary: '0 56 42',
    secondaryContainer: '20 82 65',
    onSecondaryContainer: '198 244 227',
    tertiary: '247 188 99',
    tertiaryContainer: '101 62 0',
    onTertiaryContainer: '255 230 191',
    bg: '22 16 30',
    surface: '31 23 42',
    surfaceVariant: '49 38 64',
    onSurface: '235 226 247',
    onSurfaceVariant: '192 178 214',
    outline: '132 118 156',
    metaHex: '#1F172A',
  },
};

export function loadSavedThemeMode(): ThemeMode {
  try {
    const v = localStorage.getItem(MODE_KEY);
    if (v === 'light' || v === 'dark' || v === 'system') return v;
  } catch {}
  return 'system';
}

export function loadSavedPalette(): AppPalette {
  try {
    const v = localStorage.getItem(PALETTE_KEY);
    if (v === 'emerald' || v === 'ocean' || v === 'earth' || v === 'botanical') return v;
  } catch {}
  return 'emerald';
}

export function saveThemePreferences(mode: ThemeMode, palette: AppPalette): void {
  try {
    localStorage.setItem(MODE_KEY, mode);
    localStorage.setItem(PALETTE_KEY, palette);
  } catch {}
}

export function applyThemeToDocument(mode: ThemeMode, palette: AppPalette): boolean {
  const prefersDark =
    typeof window !== 'undefined' &&
    window.matchMedia &&
    window.matchMedia('(prefers-color-scheme: dark)').matches;
  const isDark = mode === 'dark' || (mode === 'system' && prefersDark);
  const tokens = isDark ? DARK_PALETTES[palette] : LIGHT_PALETTES[palette];

  const root = document.documentElement;
  root.classList.toggle('dark', isDark);

  root.style.setProperty('--color-bg', tokens.bg);
  root.style.setProperty('--color-surface', tokens.surface);
  root.style.setProperty('--color-surface-variant', tokens.surfaceVariant);
  root.style.setProperty('--color-on-surface', tokens.onSurface);
  root.style.setProperty('--color-on-surface-variant', tokens.onSurfaceVariant);
  root.style.setProperty('--color-outline', tokens.outline);

  root.style.setProperty('--color-primary', tokens.primary);
  root.style.setProperty('--color-on-primary', tokens.onPrimary);
  root.style.setProperty('--color-primary-container', tokens.primaryContainer);
  root.style.setProperty('--color-on-primary-container', tokens.onPrimaryContainer);

  root.style.setProperty('--color-secondary', tokens.secondary);
  root.style.setProperty('--color-on-secondary', tokens.onSecondary);
  root.style.setProperty('--color-secondary-container', tokens.secondaryContainer);
  root.style.setProperty('--color-on-secondary-container', tokens.onSecondaryContainer);

  root.style.setProperty('--color-tertiary', tokens.tertiary);
  root.style.setProperty('--color-tertiary-container', tokens.tertiaryContainer);
  root.style.setProperty('--color-on-tertiary-container', tokens.onTertiaryContainer);

  const metaTheme = document.querySelector('meta[name="theme-color"]');
  if (metaTheme) {
    metaTheme.setAttribute('content', tokens.metaHex);
  }

  return isDark;
}
