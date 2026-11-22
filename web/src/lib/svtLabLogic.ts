export function normalizeForSearch(input: string): string {
  return input
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    // Arabic alef/teh marbuta normalization for smooth search
    .replace(/[أإآ]/g, 'ا')
    .replace(/ة/g, 'ه')
    .replace(/ى/g, 'ي')
    .replace(/[\u064B-\u065F]/g, '');
}

const CODON_TABLE: Record<string, string> = {
  UUU: 'Phe', UUC: 'Phe', UUA: 'Leu', UUG: 'Leu',
  CUU: 'Leu', CUC: 'Leu', CUA: 'Leu', CUG: 'Leu',
  AUU: 'Ile', AUC: 'Ile', AUA: 'Ile', AUG: 'Met (Start)',
  GUU: 'Val', GUC: 'Val', GUA: 'Val', GUG: 'Val',
  UCU: 'Ser', UCC: 'Ser', UCA: 'Ser', UCG: 'Ser',
  CCU: 'Pro', CCC: 'Pro', CCA: 'Pro', CCG: 'Pro',
  ACU: 'Thr', ACC: 'Thr', ACA: 'Thr', ACG: 'Thr',
  GCU: 'Ala', GCC: 'Ala', GCA: 'Ala', GCG: 'Ala',
  UAU: 'Tyr', UAC: 'Tyr', UAA: 'STOP', UAG: 'STOP',
  CAU: 'His', CAC: 'His', CAA: 'Gln', CAG: 'Gln',
  AAU: 'Asn', AAC: 'Asn', AAA: 'Lys', AAG: 'Lys',
  GAU: 'Asp', GAC: 'Asp', GAA: 'Glu', GAG: 'Glu',
  UGU: 'Cys', UGC: 'Cys', UGA: 'STOP', UGG: 'Trp',
  CGU: 'Arg', CGC: 'Arg', CGA: 'Arg', CGG: 'Arg',
  AGU: 'Ser', AGC: 'Ser', AGA: 'Arg', AGG: 'Arg',
  GGU: 'Gly', GGC: 'Gly', GGA: 'Gly', GGG: 'Gly',
};

export interface DnaAnalysisResult {
  cleanedDna: string;
  complementaryDna: string;
  mrna: string;
  codons: string[];
  aminoAcids: string[];
  hasStop: boolean;
  gcPercent: number;
}

export function cleanDnaInput(raw: string): string {
  return raw.toUpperCase().replace(/[^ATCG]/g, '');
}

export function analyzeDnaSequence(raw: string): DnaAnalysisResult {
  const cleanedDna = cleanDnaInput(raw);
  const compMap: Record<string, string> = { A: 'T', T: 'A', C: 'G', G: 'C' };
  const mrnaMap: Record<string, string> = { A: 'U', T: 'A', C: 'G', G: 'C' };

  let complementaryDna = '';
  let mrna = '';
  let gcCount = 0;

  for (const base of cleanedDna) {
    complementaryDna += compMap[base] || '';
    mrna += mrnaMap[base] || '';
    if (base === 'G' || base === 'C') gcCount++;
  }

  const codons: string[] = [];
  const aminoAcids: string[] = [];
  let hasStop = false;

  for (let i = 0; i + 2 < mrna.length; i += 3) {
    const codon = mrna.slice(i, i + 3);
    codons.push(codon);
    if (!hasStop) {
      const aa = CODON_TABLE[codon] || '?';
      aminoAcids.push(aa);
      if (aa === 'STOP') hasStop = true;
    }
  }

  const gcPercent = cleanedDna.length > 0 ? Math.round((gcCount * 100) / cleanedDna.length) : 0;

  return {
    cleanedDna,
    complementaryDna,
    mrna,
    codons,
    aminoAcids,
    hasStop,
    gcPercent,
  };
}

export function computeMicroscopeScale(params: {
  eyepieceMagnification: number;
  objectiveMagnification: number;
  observedSizeMm: number;
}): {
  totalMagnification: number;
  realSizeMm: number;
  realSizeMicrometers: number;
} {
  const totalMagnification = Math.max(1, params.eyepieceMagnification * params.objectiveMagnification);
  const realSizeMm = params.observedSizeMm / totalMagnification;
  const realSizeMicrometers = realSizeMm * 1000;
  return { totalMagnification, realSizeMm, realSizeMicrometers };
}

export type AtpPathway = 'respiration' | 'lactic' | 'alcoholic';

export function computeAtpYield(glucoseMoles: number, pathway: AtpPathway): {
  totalAtp: number;
  energyKj: number;
  efficiencyPct: number;
  equation: string;
} {
  const safeMoles = Math.max(0, glucoseMoles);
  const atpPerMol = pathway === 'respiration' ? 38 : 2;
  const totalAtp = Math.round(safeMoles * atpPerMol);
  const energyKj = Math.round(totalAtp * 30.5 * 10) / 10;
  const totalGlucoseEnergy = safeMoles * 2840;
  const efficiencyPct =
    totalGlucoseEnergy > 0 ? Math.round((energyKj * 100) / totalGlucoseEnergy) : 0;
  const equation =
    pathway === 'respiration'
      ? 'C₆H₁₂O₆ + 6 O₂ → 6 CO₂ + 6 H₂O + 38 ATP'
      : pathway === 'lactic'
      ? 'C₆H₁₂O₆ → 2 CH₃-CHOH-COOH + 2 ATP'
      : 'C₆H₁₂O₆ → 2 C₂H₅OH + 2 CO₂ + 2 ATP';
  return { totalAtp, energyKj, efficiencyPct, equation };
}

export type ParentGenotype = 'AA' | 'Aa' | 'aa';

export function computeMonohybridPunnett(p1: ParentGenotype, p2: ParentGenotype): {
  gametes1: [string, string];
  gametes2: [string, string];
  grid: string[][];
  dominantPct: number;
  recessivePct: number;
  genotypeCounts: Record<ParentGenotype, number>;
} {
  const gametes1: [string, string] = [p1[0], p1[1]];
  const gametes2: [string, string] = [p2[0], p2[1]];
  const counts: Record<ParentGenotype, number> = { AA: 0, Aa: 0, aa: 0 };

  const normalizePair = (a: string, b: string): ParentGenotype => {
    if (a === 'A' && b === 'A') return 'AA';
    if (a === 'a' && b === 'a') return 'aa';
    return 'Aa';
  };

  const grid = gametes1.map((g1) =>
    gametes2.map((g2) => {
      const gen = normalizePair(g1, g2);
      counts[gen] += 1;
      return gen;
    })
  );

  const dominantPct = ((counts.AA + counts.Aa) * 100) / 4;
  const recessivePct = (counts.aa * 100) / 4;
  return { gametes1, gametes2, grid, dominantPct, recessivePct, genotypeCounts: counts };
}

export type BloodGroup = 'O-' | 'O+' | 'A-' | 'A+' | 'B-' | 'B+' | 'AB-' | 'AB+';

export function checkBloodCompatibility(donor: BloodGroup, recipient: BloodGroup): {
  compatible: boolean;
  conflictReason: string;
} {
  const donorA = donor.startsWith('A') || donor.startsWith('AB');
  const donorB = donor.startsWith('B') || donor.startsWith('AB');
  const donorRh = donor.endsWith('+');

  const recA = recipient.startsWith('A') || recipient.startsWith('AB');
  const recB = recipient.startsWith('B') || recipient.startsWith('AB');
  const recRh = recipient.endsWith('+');

  const conflicts: string[] = [];
  if (donorA && !recA) conflicts.push('Anti-A');
  if (donorB && !recB) conflicts.push('Anti-B');
  if (donorRh && !recRh) conflicts.push('Anti-Rh (D)');

  return {
    compatible: conflicts.length === 0,
    conflictReason: conflicts.join(', '),
  };
}

export function evaluateWaterQuality(dbo5: number, dco: number, dissolvedO2: number): {
  status: 'excellent' | 'good' | 'moderate' | 'polluted';
  ratioDcoDbo5: string;
} {
  const ratio = dbo5 > 0 ? (dco / dbo5).toFixed(2) : '—';
  let status: 'excellent' | 'good' | 'moderate' | 'polluted';
  if (dbo5 <= 3 && dissolvedO2 >= 7) {
    status = 'excellent';
  } else if (dbo5 <= 6 && dissolvedO2 >= 5) {
    status = 'good';
  } else if (dbo5 <= 12 && dissolvedO2 >= 3) {
    status = 'moderate';
  } else {
    status = 'polluted';
  }
  return { status, ratioDcoDbo5: ratio };
}

export function computeEpicenterDistanceKm(deltaTSeconds: number): number {
  const vp = 6.0;
  const vs = 3.5;
  const safeDt = Math.max(0, deltaTSeconds);
  const dist = safeDt / (1 / vs - 1 / vp);
  return Math.round(dist * 10) / 10;
}
