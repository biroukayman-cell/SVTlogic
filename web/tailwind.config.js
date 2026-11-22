/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      fontFamily: {
        display: ['Outfit', 'Noto Sans Arabic', 'system-ui', 'sans-serif'],
        sans: ['Nunito', 'Noto Sans Arabic', 'system-ui', 'sans-serif'],
      },
      colors: {
        svt: {
          bg: 'rgb(var(--color-bg) / <alpha-value>)',
          surface: 'rgb(var(--color-surface) / <alpha-value>)',
          surfaceVariant: 'rgb(var(--color-surface-variant) / <alpha-value>)',
          onSurface: 'rgb(var(--color-on-surface) / <alpha-value>)',
          onSurfaceVariant: 'rgb(var(--color-on-surface-variant) / <alpha-value>)',
          outline: 'rgb(var(--color-outline) / <alpha-value>)',
          primary: 'rgb(var(--color-primary) / <alpha-value>)',
          onPrimary: 'rgb(var(--color-on-primary) / <alpha-value>)',
          primaryContainer: 'rgb(var(--color-primary-container) / <alpha-value>)',
          onPrimaryContainer: 'rgb(var(--color-on-primary-container) / <alpha-value>)',
          secondary: 'rgb(var(--color-secondary) / <alpha-value>)',
          onSecondary: 'rgb(var(--color-on-secondary) / <alpha-value>)',
          secondaryContainer: 'rgb(var(--color-secondary-container) / <alpha-value>)',
          onSecondaryContainer: 'rgb(var(--color-on-secondary-container) / <alpha-value>)',
          tertiary: 'rgb(var(--color-tertiary) / <alpha-value>)',
          tertiaryContainer: 'rgb(var(--color-tertiary-container) / <alpha-value>)',
          onTertiaryContainer: 'rgb(var(--color-on-tertiary-container) / <alpha-value>)',
          success: '#198754',
          warning: '#D97706',
          error: '#D14343',
        },
      },
    },
  },
  plugins: [],
};
