import { AppLanguage } from '../types/svt';

export interface SvtBadgeDefinition {
  id: string;
  emoji: string;
  xpBonus: number;
  title: Record<AppLanguage, string>;
  description: Record<AppLanguage, string>;
}

export const SVT_BADGES: SvtBadgeDefinition[] = [
  {
    id: 'first_step',
    emoji: '🌱',
    xpBonus: 50,
    title: { fr: 'Première Graine SVT', ar: 'أول خطوة علمية', en: 'First SVT Seed' },
    description: {
      fr: 'Terminer votre premier QCM chronométré.',
      ar: 'إتمام أول تمرين QCM مؤقت بنجاح.',
      en: 'Complete your first timed QCM.',
    },
  },
  {
    id: 'perfect_score',
    emoji: '🧬',
    xpBonus: 120,
    title: { fr: 'Génie Génétique (100%)', ar: 'تفوق كامل (100%)', en: 'Genetic Master (100%)' },
    description: {
      fr: 'Obtenir 100% de bonnes réponses à un QCM.',
      ar: 'الحصول على 100% من الإجابات الصحيحة في تمرين.',
      en: 'Achieve 100% correct answers in a quiz.',
    },
  },
  {
    id: 'quiz_master_5',
    emoji: '🔬',
    xpBonus: 100,
    title: { fr: 'Explorateur Cellulaire', ar: 'مستكشف الخلايا', en: 'Cell Explorer' },
    description: {
      fr: 'Réussir au moins 5 exercices QCM.',
      ar: 'إنجاز 5 تمارين QCM على الأقل.',
      en: 'Complete at least 5 QCM quizzes.',
    },
  },
  {
    id: 'quiz_master_15',
    emoji: '🏆',
    xpBonus: 250,
    title: { fr: 'Maître des Sciences', ar: 'خبير علوم الحياة والأرض', en: 'SVT Grandmaster' },
    description: {
      fr: 'Réussir 15 exercices QCM en classe.',
      ar: 'إنجاز 15 تمرين QCM بنجاح.',
      en: 'Complete 15 QCM exercises in class.',
    },
  },
  {
    id: 'speed_scholar',
    emoji: '⚡',
    xpBonus: 90,
    title: { fr: 'Réflexe Synaptique', ar: 'رد فعل عصبي سريع', en: 'Synaptic Reflex' },
    description: {
      fr: 'Terminer un QCM en moins de 15s par question avec ≥ 70%.',
      ar: 'إنهاء تمرين بأقل من 15 ثانية للسؤال وبنسبة ≥ 70%.',
      en: 'Finish a quiz averaging under 15s/question with ≥ 70%.',
    },
  },
  {
    id: 'video_watcher',
    emoji: '🎬',
    xpBonus: 100,
    title: { fr: 'Observateur Attentif', ar: 'متابع مجتهد', en: 'Attentive Observer' },
    description: {
      fr: 'Regarder au moins 3 vidéos de cours jusqu’à 90%+.',
      ar: 'مشاهدة 3 فيديوهات تعليمية بنسبة 90% فأكثر.',
      en: 'Complete 3 course videos to 90%+.',
    },
  },
  {
    id: 'curious_reader',
    emoji: '📚',
    xpBonus: 80,
    title: { fr: 'Chercheur Documentaire', ar: 'باحث وثائقي', en: 'Document Researcher' },
    description: {
      fr: 'Consulter au moins 5 supports de cours.',
      ar: 'مراجعة 5 وثائق ودروس على الأقل.',
      en: 'Explore at least 5 course resources.',
    },
  },
  {
    id: 'svt_communicator',
    emoji: '💬',
    xpBonus: 60,
    title: { fr: 'Esprit Scientifique', ar: 'تواصل علمي نشيط', en: 'Scientific Communicator' },
    description: {
      fr: 'Poser une question à votre professeur en messagerie privée.',
      ar: 'طرح سؤال على أستاذك عبر المراسلة الخاصة.',
      en: 'Send a private question to your teacher.',
    },
  },
  {
    id: 'streak_3',
    emoji: '🔥',
    xpBonus: 80,
    title: { fr: 'Régularité (3 jours)', ar: 'مواظبة (3 أيام)', en: '3-Day Streak' },
    description: {
      fr: 'Maintenir une série active de 3 jours.',
      ar: 'الحفاظ على سلسلة حضور لمدة 3 أيام متتالية.',
      en: 'Maintain an active 3-day learning streak.',
    },
  },
  {
    id: 'streak_7',
    emoji: '🌍',
    xpBonus: 200,
    title: { fr: 'Passionné BioTerre (7j)', ar: 'شغوف بالعلوم (7 أيام)', en: 'BioTerre Champion (7d)' },
    description: {
      fr: 'Maintenir une série active de 7 jours.',
      ar: 'الحفاظ على سلسلة حضور لمدة 7 أيام متتالية.',
      en: 'Maintain an active 7-day learning streak.',
    },
  },
];

export function computeLevelFromXp(xp: number, lang: AppLanguage): {
  level: number;
  title: string;
  currentLevelXp: number;
  nextLevelXp: number;
  progress: number;
} {
  const level = Math.max(1, Math.floor(xp / 200) + 1);
  const currentLevelXp = (level - 1) * 200;
  const nextLevelXp = level * 200;
  const progress = Math.min(1, Math.max(0, (xp - currentLevelXp) / 200));

  const titles: Record<AppLanguage, string[]> = {
    fr: ['Apprenti BioTerre', 'Explorateur SVT', 'Analyste Cellulaire', 'Chercheur Confirmé', 'Expert BioTerre'],
    ar: ['مبتدئ في العلوم', 'مستكشف البيئة والحياة', 'محلل خلوي', 'باحث متمكن', 'خبير BioTerre'],
    en: ['BioTerre Apprentice', 'SVT Explorer', 'Cell Analyst', 'Senior Researcher', 'BioTerre Expert'],
  };

  const list = titles[lang];
  const title = list[Math.min(level - 1, list.length - 1)];
  return { level, title, currentLevelXp, nextLevelXp, progress };
}

export function evaluateBadgeUnlocks(params: {
  existingBadges: string[];
  attemptsCount: number;
  latestScore: number;
  latestMaxScore: number;
  latestTotalSeconds: number;
  latestQuestionsCount: number;
  completedVideosCount: number;
  openedResourcesCount: number;
  sentMessagesCount: number;
  loginStreak: number;
}): { newBadgeIds: string[]; bonusXp: number } {
  const existing = new Set(params.existingBadges);
  const newlyUnlocked: string[] = [];

  const check = (id: string, condition: boolean) => {
    if (condition && !existing.has(id)) {
      newlyUnlocked.push(id);
      existing.add(id);
    }
  };

  check('first_step', params.attemptsCount >= 1);
  check('perfect_score', params.latestMaxScore > 0 && params.latestScore >= params.latestMaxScore);
  check('quiz_master_5', params.attemptsCount >= 5);
  check('quiz_master_15', params.attemptsCount >= 15);
  const avgSec =
    params.latestQuestionsCount > 0 ? params.latestTotalSeconds / params.latestQuestionsCount : 999;
  const pct = params.latestMaxScore > 0 ? params.latestScore / params.latestMaxScore : 0;
  check('speed_scholar', params.attemptsCount >= 1 && avgSec < 15 && pct >= 0.7);
  check('video_watcher', params.completedVideosCount >= 3);
  check('curious_reader', params.openedResourcesCount >= 5);
  check('svt_communicator', params.sentMessagesCount >= 1);
  check('streak_3', params.loginStreak >= 3);
  check('streak_7', params.loginStreak >= 7);

  const bonusXp = newlyUnlocked.reduce((sum, id) => {
    const def = SVT_BADGES.find((b) => b.id === id);
    return sum + (def ? def.xpBonus : 0);
  }, 0);

  return { newBadgeIds: newlyUnlocked, bonusXp };
}
