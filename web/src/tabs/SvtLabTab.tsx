import React, { useMemo, useState } from 'react';
import {
  Activity,
  BookOpen,
  ChevronLeft,
  ChevronRight,
  Dna,
  Droplets,
  FlaskConical,
  Layers,
  Microscope,
  Network,
  RotateCw,
  Search,
} from 'lucide-react';
import { ALL_SVT_DOMAINS, AppLanguage, SvtDomain } from '../types/svt';
import { DOMAIN_LABELS, TRANSLATIONS } from '../lib/i18n';
import {
  DNA_PRESETS,
  FLASHCARDS,
  GEOLOGICAL_ERAS,
  GLOSSARY_ENTRIES,
  pickText,
  ROCK_FAMILIES,
  SVT_MIND_MAPS,
} from '../lib/svtLabData';
import {
  analyzeDnaSequence,
  AtpPathway,
  BloodGroup,
  checkBloodCompatibility,
  cleanDnaInput,
  computeAtpYield,
  computeEpicenterDistanceKm,
  computeMicroscopeScale,
  computeMonohybridPunnett,
  evaluateWaterQuality,
  normalizeForSearch,
  ParentGenotype,
} from '../lib/svtLabLogic';

type LabSection =
  | 'glossary'
  | 'mindmaps'
  | 'flashcards'
  | 'dna'
  | 'microscope_geo'
  | 'extra_tools';

interface SvtLabTabProps {
  lang: AppLanguage;
  visibleDomains?: SvtDomain[];
}

export const SvtLabTab: React.FC<SvtLabTabProps> = ({ lang, visibleDomains }) => {
  const tr = TRANSLATIONS[lang];
  const allowedDomains =
    visibleDomains && visibleDomains.length > 0 ? visibleDomains : ALL_SVT_DOMAINS;

  const [section, setSection] = useState<LabSection>('glossary');
  const [domainFilter, setDomainFilter] = useState<SvtDomain | 'ALL'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Flashcards state
  const [cardIndex, setCardIndex] = useState(0);
  const [flipped, setFlipped] = useState(false);

  // DNA Simulator state
  const [dnaInput, setDnaInput] = useState(DNA_PRESETS[0].sequence);

  // Microscope & Geology state
  const [eyepiece, setEyepiece] = useState(10);
  const [objective, setObjective] = useState(40);
  const [observedMm, setObservedMm] = useState(2.5);

  // Extra SVT Tools state
  const [extraToolTab, setExtraToolTab] = useState<'atp' | 'genetics' | 'ecology'>('atp');
  const [glucoseMol, setGlucoseMol] = useState(1);
  const [atpPathway, setAtpPathway] = useState<AtpPathway>('respiration');
  const [parent1, setParent1] = useState<ParentGenotype>('Aa');
  const [parent2, setParent2] = useState<ParentGenotype>('Aa');
  const [donor, setDonor] = useState<BloodGroup>('O-');
  const [recipient, setRecipient] = useState<BloodGroup>('A+');
  const [dbo5, setDbo5] = useState(4.5);
  const [dco, setDco] = useState(18);
  const [dissolvedO2, setDissolvedO2] = useState(6.2);
  const [deltaT, setDeltaT] = useState(12.5);

  const filteredGlossary = useMemo(() => {
    const q = normalizeForSearch(searchQuery);
    return GLOSSARY_ENTRIES.filter((entry) => {
      if (!allowedDomains.includes(entry.domain)) return false;
      if (domainFilter !== 'ALL' && entry.domain !== domainFilter) return false;
      if (!q) return true;
      const haystack = [
        entry.term.fr,
        entry.term.ar,
        entry.term.en,
        entry.definition.fr,
        entry.definition.ar,
        entry.definition.en,
        ...entry.keywords,
      ]
        .map(normalizeForSearch)
        .join(' ');
      return haystack.includes(q);
    });
  }, [allowedDomains, domainFilter, searchQuery]);

  const filteredMindMaps = useMemo(
    () => SVT_MIND_MAPS.filter((m) => allowedDomains.includes(m.domain)),
    [allowedDomains]
  );

  const filteredFlashcards = useMemo(
    () => FLASHCARDS.filter((c) => allowedDomains.includes(c.domain)),
    [allowedDomains]
  );

  const safeCardIdx = filteredFlashcards.length > 0 ? cardIndex % filteredFlashcards.length : 0;
  const activeCard = filteredFlashcards[safeCardIdx];

  const dnaResult = useMemo(() => analyzeDnaSequence(dnaInput), [dnaInput]);
  const microscopeResult = useMemo(
    () =>
      computeMicroscopeScale({
        eyepieceMagnification: eyepiece,
        objectiveMagnification: objective,
        observedSizeMm: observedMm,
      }),
    [eyepiece, objective, observedMm]
  );
  const atpResult = useMemo(() => computeAtpYield(glucoseMol, atpPathway), [glucoseMol, atpPathway]);
  const punnettResult = useMemo(() => computeMonohybridPunnett(parent1, parent2), [parent1, parent2]);
  const bloodResult = useMemo(() => checkBloodCompatibility(donor, recipient), [donor, recipient]);
  const waterResult = useMemo(
    () => evaluateWaterQuality(dbo5, dco, dissolvedO2),
    [dbo5, dco, dissolvedO2]
  );
  const epicenterKm = useMemo(() => computeEpicenterDistanceKm(deltaT), [deltaT]);

  const navItems: { id: LabSection; label: string; icon: React.ReactNode }[] = [
    { id: 'glossary', label: lang === 'ar' ? 'المعجم' : lang === 'en' ? 'Glossary' : 'Glossaire', icon: <BookOpen className="h-4 w-4" /> },
    { id: 'mindmaps', label: lang === 'ar' ? 'خرائط ذهنية' : lang === 'en' ? 'Mind Maps' : 'Cartes Mentales', icon: <Network className="h-4 w-4" /> },
    { id: 'flashcards', label: lang === 'ar' ? 'بطاقات الحفظ' : lang === 'en' ? 'Flashcards' : 'Cartes Mémo', icon: <Layers className="h-4 w-4" /> },
    { id: 'dna', label: 'ADN → Protéine', icon: <Dna className="h-4 w-4" /> },
    { id: 'microscope_geo', label: lang === 'ar' ? 'مجهر وجيولوجيا' : lang === 'en' ? 'Microscope & Geo' : 'Microscope & Géol', icon: <Microscope className="h-4 w-4" /> },
    { id: 'extra_tools', label: lang === 'ar' ? 'أدوات SVT' : lang === 'en' ? 'SVT Tools' : 'Outils SVT', icon: <FlaskConical className="h-4 w-4" /> },
  ];

  return (
    <div className="space-y-4">
      <div className="rounded-3xl bg-gradient-to-r from-svt-primary to-svt-secondary p-5 text-white shadow-sm">
        <h2 className="font-display text-xl font-extrabold">{tr.tabLab}</h2>
        <p className="mt-0.5 text-xs text-white/85">
          {allowedDomains.length} / 7 {DOMAIN_LABELS.CELL_GENETICS[lang].split(' ')[0]} • FR / AR / EN
        </p>
      </div>

      {/* Horizontal Sub-tabs */}
      <div className="flex gap-2 overflow-x-auto pb-1 no-scrollbar">
        {navItems.map((item) => (
          <button
            key={item.id}
            type="button"
            onClick={() => setSection(item.id)}
            className={`flex min-h-[42px] shrink-0 items-center gap-1.5 rounded-2xl px-3.5 text-xs font-bold transition ${
              section === item.id
                ? 'bg-svt-primary text-svt-onPrimary shadow-sm'
                : 'border border-svt-outline/25 bg-svt-surface text-svt-onSurfaceVariant'
            }`}
          >
            {item.icon}
            <span>{item.label}</span>
          </button>
        ))}
      </div>

      {/* 1. GLOSSARY */}
      {section === 'glossary' && (
        <div className="space-y-3">
          <div className="relative">
            <Search className="pointer-events-none absolute top-3.5 start-3.5 h-4 w-4 text-svt-onSurfaceVariant" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder={tr.search}
              className="min-h-[46px] w-full rounded-2xl border border-svt-outline/25 bg-svt-surface ps-10 pe-4 text-sm outline-none focus:border-svt-primary"
            />
          </div>

          <div className="flex gap-1.5 overflow-x-auto no-scrollbar">
            <button
              type="button"
              onClick={() => setDomainFilter('ALL')}
              className={`min-h-[36px] shrink-0 rounded-xl px-3 text-xs font-bold ${
                domainFilter === 'ALL'
                  ? 'bg-svt-secondary text-svt-onSecondary'
                  : 'bg-svt-surfaceVariant/60 text-svt-onSurfaceVariant'
              }`}
            >
              {tr.allFilter}
            </button>
            {allowedDomains.map((d) => (
              <button
                key={d}
                type="button"
                onClick={() => setDomainFilter(d)}
                className={`min-h-[36px] shrink-0 rounded-xl px-3 text-xs font-bold ${
                  domainFilter === d
                    ? 'bg-svt-secondary text-svt-onSecondary'
                    : 'bg-svt-surfaceVariant/60 text-svt-onSurfaceVariant'
                }`}
              >
                {DOMAIN_LABELS[d][lang]}
              </button>
            ))}
          </div>

          <div className="grid gap-3 md:grid-cols-2">
            {filteredGlossary.map((entry) => (
              <div
                key={entry.id}
                className="rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 shadow-sm"
              >
                <span className="text-[10px] font-bold uppercase tracking-wider text-svt-secondary">
                  {DOMAIN_LABELS[entry.domain][lang]}
                </span>
                <h3 className="mt-0.5 font-display text-base font-bold">
                  {pickText(entry.term, lang)}
                </h3>
                <p className="mt-1.5 text-xs leading-relaxed text-svt-onSurfaceVariant">
                  {pickText(entry.definition, lang)}
                </p>
                <div className="mt-2.5 rounded-xl bg-svt-primaryContainer/50 px-3 py-1.5 text-[11px] font-bold text-svt-onPrimaryContainer">
                  💡 {pickText(entry.keyHint, lang)}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 2. MIND MAPS */}
      {section === 'mindmaps' && (
        <div className="grid gap-3 md:grid-cols-2">
          {filteredMindMaps.map((mm) => (
            <div
              key={mm.id}
              className="space-y-3 rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 shadow-sm"
            >
              <div>
                <span className="text-[10px] font-bold uppercase text-svt-secondary">
                  {DOMAIN_LABELS[mm.domain][lang]}
                </span>
                <h3 className="font-display text-base font-bold">{pickText(mm.title, lang)}</h3>
              </div>

              <div className="rounded-2xl bg-svt-primaryContainer/60 px-4 py-2.5 text-center font-display text-sm font-extrabold text-svt-onPrimaryContainer">
                {pickText(mm.centralConcept, lang)}
              </div>

              <div className="space-y-2">
                {mm.branches.map((br, i) => (
                  <div
                    key={i}
                    className="rounded-2xl border border-svt-outline/15 bg-svt-bg p-3"
                    style={{ borderInlineStartWidth: '4px', borderInlineStartColor: br.colorHex }}
                  >
                    <div className="font-display text-xs font-bold" style={{ color: br.colorHex }}>
                      {pickText(br.title, lang)}
                    </div>
                    <ul className="mt-1 space-y-1 text-xs text-svt-onSurfaceVariant">
                      {br.items.map((it, j) => (
                        <li key={j}>• {pickText(it, lang)}</li>
                      ))}
                    </ul>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* 3. FLASHCARDS */}
      {section === 'flashcards' && activeCard && (
        <div className="mx-auto max-w-md space-y-4">
          <div className="text-center text-xs font-bold text-svt-onSurfaceVariant">
            {safeCardIdx + 1} / {filteredFlashcards.length} •{' '}
            {DOMAIN_LABELS[activeCard.domain][lang]}
          </div>

          <div
            onClick={() => setFlipped((f) => !f)}
            className="perspective-1000 cursor-pointer"
          >
            <div
              className={`transform-style-3d relative min-h-[240px] w-full transition-transform duration-500 ${
                flipped ? 'rotate-y-180' : ''
              }`}
            >
              {/* Front */}
              <div className="backface-hidden absolute inset-0 flex flex-col items-center justify-center rounded-3xl border-2 border-svt-primary/35 bg-svt-surface p-6 text-center shadow-md">
                <span className="mb-2 rounded-full bg-svt-primaryContainer px-3 py-0.5 text-[11px] font-bold text-svt-onPrimaryContainer">
                  Question
                </span>
                <p className="font-display text-lg font-bold leading-snug">
                  {pickText(activeCard.question, lang)}
                </p>
                <span className="mt-4 inline-flex items-center gap-1 text-xs text-svt-onSurfaceVariant">
                  <RotateCw className="h-3.5 w-3.5" /> Toucher pour retourner
                </span>
              </div>

              {/* Back */}
              <div className="backface-hidden rotate-y-180 absolute inset-0 flex flex-col items-center justify-center rounded-3xl border-2 border-svt-secondary/40 bg-svt-secondaryContainer p-6 text-center text-svt-onSecondaryContainer shadow-md">
                <span className="mb-2 rounded-full bg-white/60 px-3 py-0.5 text-[11px] font-bold">
                  Réponse
                </span>
                <p className="text-sm font-semibold leading-relaxed">
                  {pickText(activeCard.answer, lang)}
                </p>
              </div>
            </div>
          </div>

          <div className="flex items-center justify-between gap-3">
            <button
              type="button"
              onClick={() => {
                setFlipped(false);
                setCardIndex(
                  (i) => (i - 1 + filteredFlashcards.length) % filteredFlashcards.length
                );
              }}
              className="flex min-h-[48px] flex-1 items-center justify-center gap-1 rounded-2xl border border-svt-outline/25 bg-svt-surface font-bold"
            >
              <ChevronLeft className="h-5 w-5" />
            </button>
            <button
              type="button"
              onClick={() => setFlipped((f) => !f)}
              className="flex min-h-[48px] items-center justify-center gap-1.5 rounded-2xl bg-svt-primary px-5 text-sm font-bold text-svt-onPrimary"
            >
              <RotateCw className="h-4 w-4" />
              <span>Flip</span>
            </button>
            <button
              type="button"
              onClick={() => {
                setFlipped(false);
                setCardIndex((i) => (i + 1) % filteredFlashcards.length);
              }}
              className="flex min-h-[48px] flex-1 items-center justify-center gap-1 rounded-2xl border border-svt-outline/25 bg-svt-surface font-bold"
            >
              <ChevronRight className="h-5 w-5" />
            </button>
          </div>
        </div>
      )}

      {/* 4. DNA -> mRNA -> PROTEIN SIMULATOR */}
      {section === 'dna' && (
        <div className="space-y-4 rounded-3xl border border-svt-outline/20 bg-svt-surface p-5 shadow-sm">
          <div className="flex flex-wrap gap-2">
            {DNA_PRESETS.map((p, i) => (
              <button
                key={i}
                type="button"
                onClick={() => setDnaInput(p.sequence)}
                className="min-h-[36px] rounded-xl bg-svt-primaryContainer/60 px-3 text-xs font-bold text-svt-onPrimaryContainer hover:bg-svt-primaryContainer"
              >
                {pickText(p.label, lang)}
              </button>
            ))}
          </div>

          <div>
            <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
              Brin transcrit ADN (3′ → 5′) — Bases A, T, C, G :
            </label>
            <input
              type="text"
              value={dnaInput}
              onChange={(e) => setDnaInput(cleanDnaInput(e.target.value))}
              className="min-h-[48px] w-full rounded-2xl border border-svt-outline/30 bg-svt-bg px-3.5 font-mono text-sm font-bold tracking-widest uppercase"
            />
          </div>

          <div className="space-y-2.5 rounded-2xl bg-svt-bg p-4 text-xs">
            <div>
              <span className="font-bold text-svt-onSurfaceVariant">Brin complémentaire : </span>
              <span className="break-all font-mono font-bold text-svt-secondary">
                {dnaResult.complementaryDna || '—'}
              </span>
            </div>
            <div>
              <span className="font-bold text-svt-onSurfaceVariant">ARNm transcrit (5′ → 3′) : </span>
              <span className="break-all font-mono font-bold text-svt-primary">
                {dnaResult.codons.join(' · ') || dnaResult.mrna || '—'}
              </span>
            </div>
            <div>
              <span className="font-bold text-svt-onSurfaceVariant">Polypeptide traduit : </span>
              <div className="mt-1.5 flex flex-wrap gap-1.5">
                {dnaResult.aminoAcids.map((aa, i) => (
                  <span
                    key={i}
                    className={`rounded-xl px-2.5 py-1 font-mono text-xs font-bold ${
                      aa === 'STOP'
                        ? 'bg-svt-error/15 text-svt-error'
                        : 'bg-svt-primaryContainer text-svt-onPrimaryContainer'
                    }`}
                  >
                    {aa}
                  </span>
                ))}
              </div>
            </div>
            <div className="pt-1 text-[11px] font-semibold text-svt-onSurfaceVariant">
              Taux G+C : {dnaResult.gcPercent}% • {dnaResult.cleanedDna.length} nucléotides
            </div>
          </div>
        </div>
      )}

      {/* 5. MICROSCOPE & GEOLOGY */}
      {section === 'microscope_geo' && (
        <div className="space-y-4">
          <div className="space-y-3 rounded-3xl border border-svt-outline/20 bg-svt-surface p-5 shadow-sm">
            <h3 className="font-display text-base font-bold">
              Calculateur Microscope (Grandissement & Taille réelle)
            </h3>
            <div className="grid gap-3 sm:grid-cols-3">
              <div>
                <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                  Oculaire (×)
                </label>
                <input
                  type="number"
                  min={1}
                  value={eyepiece}
                  onChange={(e) => setEyepiece(Number(e.target.value) || 10)}
                  className="min-h-[44px] w-full rounded-xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
                />
              </div>
              <div>
                <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                  Objectif (×)
                </label>
                <input
                  type="number"
                  min={1}
                  value={objective}
                  onChange={(e) => setObjective(Number(e.target.value) || 40)}
                  className="min-h-[44px] w-full rounded-xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
                />
              </div>
              <div>
                <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                  Taille observée (mm)
                </label>
                <input
                  type="number"
                  step="0.1"
                  min={0}
                  value={observedMm}
                  onChange={(e) => setObservedMm(Number(e.target.value) || 0)}
                  className="min-h-[44px] w-full rounded-xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
                />
              </div>
            </div>
            <div className="rounded-2xl bg-svt-primaryContainer/55 p-3.5 text-xs font-bold text-svt-onPrimaryContainer">
              Grossissement total : ×{microscopeResult.totalMagnification} • Taille réelle :{' '}
              {microscopeResult.realSizeMicrometers.toFixed(2)} µm (
              {microscopeResult.realSizeMm.toFixed(4)} mm)
            </div>
          </div>

          <div className="grid gap-3 md:grid-cols-2">
            <div className="space-y-2.5 rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 shadow-sm">
              <h3 className="font-display text-base font-bold">Échelle des temps géologiques</h3>
              {GEOLOGICAL_ERAS.map((era, i) => (
                <div key={i} className="rounded-2xl bg-svt-bg p-3 text-xs">
                  <div className="flex items-center justify-between font-bold text-svt-primary">
                    <span>{pickText(era.name, lang)}</span>
                    <span className="font-mono text-[11px]">{era.periodMa}</span>
                  </div>
                  <p className="mt-1 text-svt-onSurfaceVariant">{pickText(era.keyEvents, lang)}</p>
                </div>
              ))}
            </div>

            <div className="space-y-2.5 rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 shadow-sm">
              <h3 className="font-display text-base font-bold">3 Grandes Familles de Roches</h3>
              {ROCK_FAMILIES.map((rk, i) => (
                <div key={i} className="rounded-2xl bg-svt-bg p-3 text-xs">
                  <div className="font-bold text-svt-secondary">{pickText(rk.family, lang)}</div>
                  <p className="mt-1 text-svt-onSurfaceVariant">{pickText(rk.origin, lang)}</p>
                  <div className="mt-1.5 flex flex-wrap gap-1">
                    {rk.examples.map((ex) => (
                      <span
                        key={ex}
                        className="rounded-lg bg-svt-surfaceVariant px-2 py-0.5 text-[11px] font-semibold"
                      >
                        {ex}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* 6. EXTRA SVT TOOLS (ATP, Genetics & ABO, Ecology & Seismic) */}
      {section === 'extra_tools' && (
        <div className="space-y-3">
          <div className="flex gap-2 overflow-x-auto no-scrollbar">
            <button
              type="button"
              onClick={() => setExtraToolTab('atp')}
              className={`min-h-[38px] shrink-0 rounded-xl px-3.5 text-xs font-bold ${
                extraToolTab === 'atp'
                  ? 'bg-svt-secondary text-svt-onSecondary'
                  : 'bg-svt-surface border border-svt-outline/20'
              }`}
            >
              ⚡ Métabolisme ATP
            </button>
            <button
              type="button"
              onClick={() => setExtraToolTab('genetics')}
              className={`min-h-[38px] shrink-0 rounded-xl px-3.5 text-xs font-bold ${
                extraToolTab === 'genetics'
                  ? 'bg-svt-secondary text-svt-onSecondary'
                  : 'bg-svt-surface border border-svt-outline/20'
              }`}
            >
              🧬 Punnett & Groupes ABO
            </button>
            <button
              type="button"
              onClick={() => setExtraToolTab('ecology')}
              className={`min-h-[38px] shrink-0 rounded-xl px-3.5 text-xs font-bold ${
                extraToolTab === 'ecology'
                  ? 'bg-svt-secondary text-svt-onSecondary'
                  : 'bg-svt-surface border border-svt-outline/20'
              }`}
            >
              🌍 Eau & Épicentre
            </button>
          </div>

          {extraToolTab === 'atp' && (
            <div className="space-y-3 rounded-3xl border border-svt-outline/20 bg-svt-surface p-5 shadow-sm">
              <h3 className="font-display text-base font-bold">Calculateur énergétique cellulaire (ATP)</h3>
              <div className="grid gap-3 sm:grid-cols-2">
                <div>
                  <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                    Quantité de glucose (mol)
                  </label>
                  <input
                    type="number"
                    min={0.5}
                    step={0.5}
                    value={glucoseMol}
                    onChange={(e) => setGlucoseMol(Number(e.target.value) || 0)}
                    className="min-h-[44px] w-full rounded-xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                    Voie métabolique
                  </label>
                  <select
                    value={atpPathway}
                    onChange={(e) => setAtpPathway(e.target.value as AtpPathway)}
                    className="min-h-[44px] w-full rounded-xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
                  >
                    <option value="respiration">Respiration aérobie (38 ATP)</option>
                    <option value="lactic">Fermentation lactique (2 ATP)</option>
                    <option value="alcoholic">Fermentation alcoolique (2 ATP)</option>
                  </select>
                </div>
              </div>
              <div className="rounded-2xl bg-svt-primaryContainer/50 p-4 text-xs space-y-1">
                <div className="font-mono font-bold text-svt-primary">{atpResult.equation}</div>
                <div className="font-display text-base font-extrabold">
                  Production totale : {atpResult.totalAtp} ATP
                </div>
                <div className="text-svt-onSurfaceVariant">
                  Rendement énergétique estimé : {atpResult.efficiencyPct}% ({atpResult.energyKj} kJ)
                </div>
              </div>
            </div>
          )}

          {extraToolTab === 'genetics' && (
            <div className="grid gap-3 md:grid-cols-2">
              <div className="space-y-3 rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 shadow-sm">
                <h3 className="font-display text-base font-bold">Échiquier de Punnett (Monohybridisme)</h3>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="mb-1 block text-xs font-bold">Parent 1</label>
                    <select
                      value={parent1}
                      onChange={(e) => setParent1(e.target.value as ParentGenotype)}
                      className="min-h-[42px] w-full rounded-xl border border-svt-outline/30 bg-svt-bg px-3 text-sm font-bold"
                    >
                      <option value="AA">AA</option>
                      <option value="Aa">Aa</option>
                      <option value="aa">aa</option>
                    </select>
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-bold">Parent 2</label>
                    <select
                      value={parent2}
                      onChange={(e) => setParent2(e.target.value as ParentGenotype)}
                      className="min-h-[42px] w-full rounded-xl border border-svt-outline/30 bg-svt-bg px-3 text-sm font-bold"
                    >
                      <option value="AA">AA</option>
                      <option value="Aa">Aa</option>
                      <option value="aa">aa</option>
                    </select>
                  </div>
                </div>
                <table className="w-full border-collapse text-center font-mono text-xs">
                  <thead>
                    <tr>
                      <th className="p-2" />
                      {punnettResult.gametes2.map((g, i) => (
                        <th key={i} className="rounded-lg bg-svt-surfaceVariant p-2">
                          {g}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {punnettResult.grid.map((row, rIdx) => (
                      <tr key={rIdx}>
                        <th className="rounded-lg bg-svt-surfaceVariant p-2">
                          {punnettResult.gametes1[rIdx]}
                        </th>
                        {row.map((cell, cIdx) => (
                          <td
                            key={cIdx}
                            className="border border-svt-outline/20 p-2 font-bold text-svt-primary"
                          >
                            {cell}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
                <div className="text-xs font-bold text-svt-onSurfaceVariant">
                  [A] dominant : {punnettResult.dominantPct}% • [a] récessif : {punnettResult.recessivePct}%
                </div>
              </div>

              <div className="space-y-3 rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 shadow-sm">
                <h3 className="font-display text-base font-bold">Compatibilité Transfusionnelle (ABO/Rh)</h3>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="mb-1 block text-xs font-bold">Donneur</label>
                    <select
                      value={donor}
                      onChange={(e) => setDonor(e.target.value as BloodGroup)}
                      className="min-h-[42px] w-full rounded-xl border border-svt-outline/30 bg-svt-bg px-3 text-sm font-bold"
                    >
                      {(['O-', 'O+', 'A-', 'A+', 'B-', 'B+', 'AB-', 'AB+'] as BloodGroup[]).map((g) => (
                        <option key={g} value={g}>{g}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-bold">Receveur</label>
                    <select
                      value={recipient}
                      onChange={(e) => setRecipient(e.target.value as BloodGroup)}
                      className="min-h-[42px] w-full rounded-xl border border-svt-outline/30 bg-svt-bg px-3 text-sm font-bold"
                    >
                      {(['O-', 'O+', 'A-', 'A+', 'B-', 'B+', 'AB-', 'AB+'] as BloodGroup[]).map((g) => (
                        <option key={g} value={g}>{g}</option>
                      ))}
                    </select>
                  </div>
                </div>
                <div
                  className={`rounded-2xl p-3.5 text-xs font-bold ${
                    bloodResult.compatible
                      ? 'bg-svt-success/15 text-svt-success'
                      : 'bg-svt-error/15 text-svt-error'
                  }`}
                >
                  {bloodResult.compatible
                    ? '✓ Transfusion compatible — Aucune agglutination'
                    : `✕ Incompatible — Agglutination (${bloodResult.conflictReason})`}
                </div>
              </div>
            </div>
          )}

          {extraToolTab === 'ecology' && (
            <div className="grid gap-3 md:grid-cols-2">
              <div className="space-y-3 rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 shadow-sm">
                <h3 className="flex items-center gap-1.5 font-display text-base font-bold">
                  <Droplets className="h-4 w-4 text-svt-secondary" />
                  <span>Qualité de l’eau (DBO₅ / DCO / O₂)</span>
                </h3>
                <div className="grid grid-cols-3 gap-2">
                  <div>
                    <label className="mb-1 block text-[11px] font-bold">DBO₅ (mg/L)</label>
                    <input
                      type="number"
                      step="0.5"
                      value={dbo5}
                      onChange={(e) => setDbo5(Number(e.target.value) || 0)}
                      className="min-h-[42px] w-full rounded-xl border border-svt-outline/30 bg-svt-bg px-2.5 text-xs"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-[11px] font-bold">DCO (mg/L)</label>
                    <input
                      type="number"
                      step="0.5"
                      value={dco}
                      onChange={(e) => setDco(Number(e.target.value) || 0)}
                      className="min-h-[42px] w-full rounded-xl border border-svt-outline/30 bg-svt-bg px-2.5 text-xs"
                    />
                  </div>
                  <div>
                    <label className="mb-1 block text-[11px] font-bold">O₂ dissous</label>
                    <input
                      type="number"
                      step="0.5"
                      value={dissolvedO2}
                      onChange={(e) => setDissolvedO2(Number(e.target.value) || 0)}
                      className="min-h-[42px] w-full rounded-xl border border-svt-outline/30 bg-svt-bg px-2.5 text-xs"
                    />
                  </div>
                </div>
                <div className="rounded-2xl bg-svt-secondaryContainer/60 p-3 text-xs font-bold text-svt-onSecondaryContainer">
                  Indice : {waterResult.status.toUpperCase()} • Rapport DCO/DBO₅ = {waterResult.ratioDcoDbo5}
                </div>
              </div>

              <div className="space-y-3 rounded-3xl border border-svt-outline/20 bg-svt-surface p-4 shadow-sm">
                <h3 className="flex items-center gap-1.5 font-display text-base font-bold">
                  <Activity className="h-4 w-4 text-svt-tertiary" />
                  <span>Distance à l’épicentre (Ondes P et S)</span>
                </h3>
                <div>
                  <label className="mb-1 block text-xs font-bold text-svt-onSurfaceVariant">
                    Retard Δt (ts - tp) en secondes
                  </label>
                  <input
                    type="number"
                    step="0.5"
                    min={0}
                    value={deltaT}
                    onChange={(e) => setDeltaT(Number(e.target.value) || 0)}
                    className="min-h-[44px] w-full rounded-xl border border-svt-outline/30 bg-svt-bg px-3 text-sm"
                  />
                </div>
                <div className="rounded-2xl bg-svt-tertiaryContainer/60 p-3 text-xs font-bold text-svt-onTertiaryContainer">
                  Distance estimée : {epicenterKm} km (Vp = 6,0 km/s • Vs = 3,5 km/s)
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
