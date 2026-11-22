import { AppLanguage, SvtDomain } from '../types/svt';

export interface LocalizedText {
  fr: string;
  en: string;
  ar: string;
}

export interface GlossaryEntry {
  id: string;
  domain: SvtDomain;
  term: LocalizedText;
  definition: LocalizedText;
  keyHint: LocalizedText;
  keywords: string[];
}

export interface FlashcardItem {
  id: string;
  domain: SvtDomain;
  question: LocalizedText;
  answer: LocalizedText;
}

export interface MindMapBranch {
  title: LocalizedText;
  items: LocalizedText[];
  colorHex: string;
}

export interface SvtMindMap {
  id: string;
  domain: SvtDomain;
  title: LocalizedText;
  centralConcept: LocalizedText;
  branches: MindMapBranch[];
}

export interface GeologicalEra {
  name: LocalizedText;
  periodMa: string;
  keyEvents: LocalizedText;
}

export interface RockFamily {
  family: LocalizedText;
  origin: LocalizedText;
  examples: string[];
}

export const DNA_PRESETS: { label: LocalizedText; sequence: string }[] = [
  {
    label: { fr: 'Fragment Hémoglobine (Normal)', ar: 'جزء الهيموغلوبين (عادي)', en: 'Hemoglobin Fragment (Normal)' },
    sequence: 'TACCTCAAGGAGTTTATC',
  },
  {
    label: { fr: 'Mutation Drépanocytose (HbS)', ar: 'طفرة فقر الدم المنجلي', en: 'Sickle Cell Mutation (HbS)' },
    sequence: 'TACCACAAGGAGTTTATC',
  },
  {
    label: { fr: 'Gène Insuline (Début)', ar: 'مورثة الأنسولين (بداية)', en: 'Insulin Gene (Start)' },
    sequence: 'TACAACGGTCGTCCAACT',
  },
  {
    label: { fr: 'Mutation Non-Sens (Stop)', ar: 'طفرة معوقِفة (Stop)', en: 'Nonsense Mutation (Stop)' },
    sequence: 'TACATTGAGGTCCAGACT',
  },
];

export const GLOSSARY_ENTRIES: GlossaryEntry[] = [
  {
    id: 'dna_replication',
    domain: 'CELL_GENETICS',
    term: { fr: 'Réplication de l’ADN (Semi-conservative)', en: 'DNA Replication (Semi-conservative)', ar: 'تضاعف الـ ADN (نصف محافظ)' },
    definition: {
      fr: 'Synthèse de deux molécules d’ADN identiques pendant la phase S de l’interphase grâce à l’ADN polymérase ; chaque molécule fille conserve un brin parental.',
      en: 'Synthesis of two identical DNA molecules during S-phase via DNA polymerase; each daughter duplex retains one parental strand.',
      ar: 'تكون جزيئتي ADN متطابقتين خلال مرحلة السكون S بواسطة أنزيم ADN بوليميراز مع احتفاظ كل جزيئة بلولب أصلي.',
    },
    keyHint: { fr: 'Sens 5′ → 3′ • Fourche de réplication', en: '5′ → 3′ direction • Replication fork', ar: 'منحى ′5 ← ′3 • عيون التضاعف' },
    keywords: ['adn', 'replication', 'phase s', 'helicase', 'dna polymerase', 'تضاعف'],
  },
  {
    id: 'mitosis',
    domain: 'CELL_GENETICS',
    term: { fr: 'Mitose (Prophase, Métaphase, Anaphase, Télophase)', en: 'Mitosis (PMAT)', ar: 'الانقسام غير المباشر' },
    definition: {
      fr: 'Division cellulaire conforme donnant deux cellules filles génétiquement identiques (2n) à la cellule mère.',
      en: 'Equational cell division producing two genetically identical diploid daughter cells (2n).',
      ar: 'انقسام خلوي مطابق يعطي خليتين بنتين متطابقتين وراثياً (2n) للخلية الأم.',
    },
    keyHint: { fr: 'Plaque équatoriale en métaphase • Clivage du centromère en anaphase', en: 'Equatorial plate at metaphase • Centromere split at anaphase', ar: 'صفيحة استوائية في الطور الاستوائي وانشطار الجزيء المركزي في الانفصالي' },
    keywords: ['mitose', 'prophase', 'metaphase', 'anaphase', 'telophase', 'الانقسام'],
  },
  {
    id: 'meiosis',
    domain: 'CELL_GENETICS',
    term: { fr: 'Méiose (Réductionnelle & Équationnelle)', en: 'Meiosis', ar: 'الانقسام الاختزالي' },
    definition: {
      fr: 'Succession de deux divisions produisant 4 gamètes haploïdes (n) à partir d’une cellule diploïde (2n), assurant le brassage génétique.',
      en: 'Two successive divisions yielding 4 haploid gametes (n) from one diploid cell (2n), generating genetic diversity.',
      ar: 'انقسامان متتاليان ينتجان 4 أمشاج أحادية الصيغة الصبغية (n) مع تخليط وراثي.',
    },
    keyHint: { fr: 'Crossing-over en Prophase I • Brassage interchromosomique en Anaphase I', en: 'Crossing-over in Prophase I • Independent assortment in Anaphase I', ar: 'عبور صبغي في التمهيدية I وتخليط بيصبغي في الانفصالية I' },
    keywords: ['meiose', 'gamete', 'haploid', 'crossing over', 'brassage', 'الانقسام الاختزالي'],
  },
  {
    id: 'transcription_translation',
    domain: 'CELL_GENETICS',
    term: { fr: 'Transcription & Traduction', en: 'Transcription & Translation', ar: 'النسخ والترجمة' },
    definition: {
      fr: 'L’ARN polymérase synthétise l’ARNm dans le noyau (transcription), puis les ribosomes traduisent les codons en chaîne polypeptidique dans le cytoplasme.',
      en: 'RNA polymerase synthesizes mRNA in the nucleus (transcription), then ribosomes translate codons into a polypeptide in the cytoplasm.',
      ar: 'يتم نسخ الـ ARNm في النواة ثم تترجم الريبوزومات الوحدات الرمزية إلى متعدد بيبتيد في السيتوبلازم.',
    },
    keyHint: { fr: 'A=U, C≡G • Codon initiateur AUG (Met) • Codons Stop UAA/UAG/UGA', en: 'A=U, C≡G • Start AUG • Stop UAA/UAG/UGA', ar: 'البدء AUG • التوقف UAA/UAG/UGA' },
    keywords: ['arnm', 'transcription', 'traduction', 'ribosome', 'codon', 'النسخ', 'الترجمة'],
  },
  {
    id: 'mutation',
    domain: 'CELL_GENETICS',
    term: { fr: 'Mutation génétique (Substitution, Délétion, Insertion)', en: 'Genetic Mutation', ar: 'الطفرة الوراثية' },
    definition: {
      fr: 'Modification aléatoire et héréditaire de la séquence nucléotidique de l’ADN créant de nouveaux allèles (silencieuse, faux-sens ou non-sens).',
      en: 'Random heritable change in DNA nucleotide sequence creating new alleles (silent, missense, or nonsense).',
      ar: 'تغير مفاجئ في تسلسل نيوكليوتيدات الـ ADN يؤدي لظهور حليلات جديدة.',
    },
    keyHint: { fr: 'Délétion/Insertion = décalage du cadre de lecture', en: 'Deletion/Insertion causes frameshift', ar: 'الضياع أو الإضافة يحدثان إزاحة لإطار القراءة' },
    keywords: ['mutation', 'allele', 'substitution', 'deletion', 'الطفرة'],
  },
  {
    id: 'innate_immunity',
    domain: 'IMMUNITY',
    term: { fr: 'Immunité non spécifique (Phagocytose)', en: 'Innate Immunity & Phagocytosis', ar: 'المناعة غير النوعية (البلعمة)' },
    definition: {
      fr: 'Réponse rapide immédiate via barrières naturelles, réaction inflammatoire et phagocytose (adhésion, ingestion, digestion, rejet).',
      en: 'Immediate first-line defense via physical barriers, inflammation, and phagocytosis.',
      ar: 'استجابة فورية وموحدة عبر الحواجز الطبيعية والالتهاب وظاهرة البلعمة.',
    },
    keyHint: { fr: '4 signes : rougeur, chaleur, gonflement, douleur', en: '4 signs: redness, heat, swelling, pain', ar: 'علامات الالتهاب: احمرار، حرارة، انتفاخ، ألم' },
    keywords: ['phagocytose', 'inflammation', 'macrophage', 'البلعمة', 'الالتهاب'],
  },
  {
    id: 'adaptive_humoral',
    domain: 'IMMUNITY',
    term: { fr: 'Immunité humorale (Lymphocytes B & Anticorps)', en: 'Humoral Immunity (B Cells & Antibodies)', ar: 'المناعة الخلطية (LB ومضادات الأجسام)' },
    definition: {
      fr: 'Les lymphocytes B sélectionnés se différencient en plasmocytes sécréteurs d’anticorps spécifiques neutralisant les antigènes extracellulaires.',
      en: 'Activated B lymphocytes differentiate into plasma cells secreting specific antibodies that form immune complexes.',
      ar: 'تتحول اللمفاويات B إلى بلزميات مفرزة لمضادات أجسام نوعية لتكوين المركب المنيع.',
    },
    keyHint: { fr: 'Complexe immun → Facilite la phagocytose par opsonisation', en: 'Immune complex facilitates phagocytosis', ar: 'المركب المنيع يسهل البلعمة' },
    keywords: ['anticorps', 'lymphocyte b', 'plasmocyte', 'complexe immun', 'الخلطية', 'مضادات الأجسام'],
  },
  {
    id: 'adaptive_cellular',
    domain: 'IMMUNITY',
    term: { fr: 'Immunité cellulaire (LTc & Perforine)', en: 'Cell-Mediated Immunity (Tc Cells)', ar: 'المناعة الخلوية (LTc والبرفورين)' },
    definition: {
      fr: 'Les lymphocytes T cytotoxiques (LT8 → LTc) détruisent les cellules infectées ou cancéreuses grâce à la perforine et aux granzymes.',
      en: 'Cytotoxic T cells recognize MHC-I antigens and destroy infected or tumor cells via perforin.',
      ar: 'تدمر اللمفاويات T القاتلة الخلايا المعفنة أو السرطانية عبر إفراز البرفورين.',
    },
    keyHint: { fr: 'Nécessite LT4 (auxiliaires) qui sécrètent les interleukines (IL-2)', en: 'Requires Helper T4 cells releasing IL-2', ar: 'تحفز بواسطة اللمفاويات المساعدة LT4 والأنتيرلوكينات' },
    keywords: ['ltc', 'lt4', 'perforine', 'cmh', 'المناعة الخلوية'],
  },
  {
    id: 'vaccination_serotherapy',
    domain: 'IMMUNITY',
    term: { fr: 'Vaccination vs Sérothérapie', en: 'Vaccination vs Serotherapy', ar: 'التلقيح والاستمصال' },
    definition: {
      fr: 'La vaccination est préventive et active (mémoire immunitaire durable) ; la sérothérapie est curative et passive (injection directe d’anticorps).',
      en: 'Vaccination is active and preventive (long memory); serotherapy is passive and immediate (ready-made antibodies).',
      ar: 'التلقيح وقائي ونشيط يكسب ذاكرة مناعية؛ الاستمصال علاجي وفوري ومؤقت.',
    },
    keyHint: { fr: 'Réponse secondaire beaucoup plus rapide et intense', en: 'Secondary memory response is faster & stronger', ar: 'الاستجابة الثانوية أسرع وأقوى' },
    keywords: ['vaccin', 'serotherapie', 'memoire immunitaire', 'التلقيح', 'الاستمصال'],
  },
  {
    id: 'spermatogenesis_oogenesis',
    domain: 'REPRODUCTION',
    term: { fr: 'Gamétogenèse (Spermatogenèse & Ovogenèse)', en: 'Gametogenesis', ar: 'تشكل الأمشاج' },
    definition: {
      fr: 'Formation continue des spermatozoïdes dans les tubes séminifères et formation cyclique de l’ovocyte II bloqué en métaphase II jusqu’à la fécondation.',
      en: 'Continuous sperm formation in seminiferous tubules vs cyclic oocyte development arrested at metaphase II.',
      ar: 'تشكل مستمر للحيوانات المنوية بالأنابيب المنوية وتشكل دوري للبويضة.',
    },
    keyHint: { fr: 'Cellules de Leydig → Testostérone • Cellules de Sertoli → Nutrition', en: 'Leydig → Testosterone • Sertoli → Support', ar: 'خلايا لايديغ تفرز التستوستيرون' },
    keywords: ['spermatogenese', 'ovogenese', 'leydig', 'testosterone', 'الأمشاج'],
  },
  {
    id: 'menstrual_cycle',
    domain: 'REPRODUCTION',
    term: { fr: 'Cycle ovarien & Hormonologie (FSH / LH / Œstrogènes / Progestérone)', en: 'Ovarian & Uterine Cycle', ar: 'الدورة المبيضية والرحمية' },
    definition: {
      fr: 'Phase folliculaire (FSH, œstradiol), pic ovulatoire de LH à J14, puis phase lutéale (progestérone préparant l’endomètre à la nidation).',
      en: 'Follicular phase (FSH, estradiol), Day-14 LH surge triggering ovulation, then luteal progesterone supporting implantation.',
      ar: 'الطور الجريبي ثم ذروة LH للإباضة في اليوم 14، يليه الطور الجسفروني لتهيئة بطانة الرحم.',
    },
    keyHint: { fr: 'Pic d’œstradiol (> seuil) → Rétrocontrôle positif sur LH', en: 'High estradiol spike triggers positive feedback LH surge', ar: 'ارتفاع الأستراديول يحدث مفعولاً رجعياً إيجابياً على LH' },
    keywords: ['lh', 'fsh', 'ovulation', 'progesterone', 'oestrogene', 'الإباضة'],
  },
  {
    id: 'atp_respiration',
    domain: 'PHYSIOLOGY_IMMUNITY',
    term: { fr: 'Respiration cellulaire & Cycle de Krebs', en: 'Cellular Respiration & Krebs Cycle', ar: 'التنفس الخلوي وحلقة كريبس' },
    definition: {
      fr: 'Dégradation complète du glucose (glycolyse + cycle de Krebs + chaîne respiratoire mitochondriale) produisant 38 ATP avec consommation d’O₂.',
      en: 'Complete oxidation of glucose across glycolysis, Krebs cycle, and oxidative phosphorylation yielding ~38 ATP.',
      ar: 'هدم كلي للكليكوز في الجبلة الشفافة ثم الميتوكوندري لإنتاج 38 ATP.',
    },
    keyHint: { fr: 'Glycolyse (hyaloplasme) = 2 pyruvate + 2 ATP + 2 NADH,H⁺', en: 'Glycolysis (cytosol) yields 2 pyruvate + 2 ATP', ar: 'انحلال الكليكوز بالجبلة الشفافة يعطي حمض البيروفيك' },
    keywords: ['atp', 'mitochondrie', 'krebs', 'glycolyse', 'respiration', 'التنفس', 'الميتوكوندري'],
  },
  {
    id: 'fermentation',
    domain: 'PHYSIOLOGY_IMMUNITY',
    term: { fr: 'Fermentation lactique & alcoolique', en: 'Lactic & Alcoholic Fermentation', ar: 'التخمر اللبني والكحولي' },
    definition: {
      fr: 'Dégradation incomplète du glucose en milieu anaérobie (hyaloplasme uniquement) produisant seulement 2 ATP par mole de glucose.',
      en: 'Incomplete anaerobic breakdown of glucose in cytosol producing only 2 ATP.',
      ar: 'هدم جزئي للكليكوز في غياب ثنائي الأكسجين ينتج فقط 2 ATP.',
    },
    keyHint: { fr: 'Forte énergie chimique perdue dans le déchet organique (lactate/éthanol)', en: 'Most energy remains trapped in lactate or ethanol', ar: 'تبقى أغلب الطاقة كامنة في الحمض اللبني أو الإيثانول' },
    keywords: ['fermentation', 'lactique', 'alcoolique', 'anaerobie', 'التخمر'],
  },
  {
    id: 'photosynthesis',
    domain: 'PHYSIOLOGY_IMMUNITY',
    term: { fr: 'Photosynthèse (Phase claire & Cycle de Calvin)', en: 'Photosynthesis (Light & Calvin Cycle)', ar: 'التركيب الضوئي' },
    definition: {
      fr: 'Conversion de l’énergie lumineuse en ATP/NADPH dans les thylakoïdes (photolyse de l’eau), suivie de la fixation du CO₂ dans le stroma.',
      en: 'Light energy captured in thylakoids produces ATP/NADPH and O₂, driving CO₂ fixation in the stroma.',
      ar: 'تحويل الطاقة الضوئية في التيلاكويدات ثم اختزال CO₂ إلى مادة عضوية في الستروما.',
    },
    keyHint: { fr: '6 CO₂ + 6 H₂O + Lumière → C₆H₁₂O₆ + 6 O₂', en: '6 CO₂ + 6 H₂O + Light → C₆H₁₂O₆ + 6 O₂', ar: '6 CO₂ + 6 H₂O + ضوء ← C₆H₁₂O₆ + 6 O₂' },
    keywords: ['photosynthese', 'chlorophylle', 'chloroplaste', 'calvin', 'التركيب الضوئي'],
  },
  {
    id: 'action_potential',
    domain: 'NERVOUS_MUSCULAR',
    term: { fr: 'Potentiel d’action & Synapse', en: 'Action Potential & Synapse', ar: 'جهد العمل والسينابس' },
    definition: {
      fr: 'Inversion brève de la polarité membranaire (dépolarisation Na⁺ puis repolarisation K⁺) transmise le long de l’axone puis par neurotransmetteurs.',
      en: 'Brief membrane depolarization (Na⁺ influx) and repolarization (K⁺ efflux) propagating to chemical synapses.',
      ar: 'انعكاس سريع للاستقطاب الغشائي ينتقل عبر المحورة ثم بواسطة مبلغات عصبية في السينابس.',
    },
    keyHint: { fr: 'Repos : -70 mV • Loi du tout ou rien • Exocytose via Ca²⁺', en: 'Resting -70 mV • All-or-none • Ca²⁺ triggers vesicle release', ar: 'جهد الكمون -70mV • قانون الكل أو العدم' },
    keywords: ['neurone', 'synapse', 'potentiel action', 'axone', 'جهد العمل', 'السينابس'],
  },
  {
    id: 'sarcomere_contraction',
    domain: 'NERVOUS_MUSCULAR',
    term: { fr: 'Contraction musculaire (Sarcomère & Ca²⁺ / ATP)', en: 'Muscle Contraction (Sarcomere)', ar: 'التقلص العضلي (الساركومير)' },
    definition: {
      fr: 'Glissement des myofilaments fins d’actine entre les filaments épais de myosine grâce au Ca²⁺ et à l’hydrolyse de l’ATP.',
      en: 'Sliding of actin thin filaments over myosin thick filaments powered by Ca²⁺ release and ATP hydrolysis.',
      ar: 'انزلاق خييطات الأكتين بين خييطات الميوزين بتدخل أيونات الكالسيوم وحلمأة ATP.',
    },
    keyHint: { fr: 'Raccourcissement des stries Z et bande I • Bande A reste constante', en: 'Z-lines & I-band shorten • A-band stays constant', ar: 'تقارب حزتي Z وتقلص المنطقة الفاتحة I' },
    keywords: ['muscle', 'sarcomere', 'actine', 'myosine', 'calcium', 'الساركومير', 'التقلص العضلي'],
  },
  {
    id: 'plate_tectonics_subduction',
    domain: 'GEOLOGY',
    term: { fr: 'Subduction, Obduction & Collision (Chaînes de montagnes)', en: 'Subduction, Obduction & Collision', ar: 'الطمر والطفو والاصطدام' },
    definition: {
      fr: 'Enfouissement d’une lithosphère océanique dense (subduction → andésite, métamorphisme HP-BT) ou affrontement continental (collision → plis, failles inverses).',
      en: 'Sinking of dense oceanic lithosphere (subduction) or continental collision forming fold mountains and nappes.',
      ar: 'انغراز الغلاف الصخري المحيطي الأكثر كثافة (طمر) أو تصادم صفيحتين قاريتين (اصطدام).',
    },
    keyHint: { fr: 'Ophiolite = témoin d’une ancienne croûte océanique', en: 'Ophiolites record ancient oceanic crust', ar: 'الأفيوليت شاهد على قشرة محيطية قديمة' },
    keywords: ['subduction', 'collision', 'obduction', 'ophiolite', 'الطمر', 'الاصطدام'],
  },
  {
    id: 'metamorphism_anatexis',
    domain: 'GEOLOGY',
    term: { fr: 'Métamorphisme & Granitisation (Anatexie)', en: 'Metamorphism & Anatexis', ar: 'التحول والكرانيتية' },
    definition: {
      fr: 'Transformation minéralogique à l’état solide sous P/T croissants (schiste → micaschiste → gneiss → migmatite → granite d’anatexie).',
      en: 'Solid-state mineral transformation under rising P/T leading eventually to partial melting (migmatite / anatexis).',
      ar: 'تحول الصخور في الحالة الصلبة تحت تأثير الضغط والحرارة وصولاً للانصهار الجزئي.',
    },
    keyHint: { fr: 'Foliation = marqueur du métamorphisme régional', en: 'Foliation marks regional metamorphism', ar: 'التوريق يميز التحول الإقليمي' },
    keywords: ['metamorphisme', 'gneiss', 'migmatite', 'granite', 'anatexie', 'التحول'],
  },
  {
    id: 'trophic_chain',
    domain: 'ECOLOGY',
    term: { fr: 'Chaîne trophique & Écosystème', en: 'Food Chain & Ecosystem', ar: 'السلسلة الغذائية والحميلة البيئية' },
    definition: {
      fr: 'Système formé par un biotope et sa biocénose où s’effectue un flux d’énergie et un cycle de matière (producteurs → consommateurs → décomposeurs).',
      en: 'System of biotope and biocenosis transferring matter and energy across trophic levels.',
      ar: 'مجموع مكون من المحيا والعشيرة الإحيائية حيث تنتقل المادة والطاقة بين الحلقات الغذائية.',
    },
    keyHint: { fr: '~10% seulement de l’énergie passe au niveau trophique suivant', en: 'Only ~10% energy transfers per trophic level', ar: 'ينتقل نحو 10% فقط من الطاقة للمستوى الموالي' },
    keywords: ['ecosysteme', 'chaine trophique', 'biotope', 'biocenose', 'الحميلة البيئية', 'البيئة'],
  },
  {
    id: 'eutrophication_pollution',
    domain: 'ECOLOGY',
    term: { fr: 'Eutrophisation & Qualité de l’eau (DBO₅ / DCO)', en: 'Eutrophication & Water Quality', ar: 'التخثث وجودة المياه' },
    definition: {
      fr: 'Excès de nitrates/phosphates provoquant une prolifération algale en surface puis l’asphyxie du milieu par baisse d’O₂ dissous.',
      en: 'Excess nitrates/phosphates triggering algal blooms followed by severe oxygen depletion.',
      ar: 'تراكم النترات والفوسفاط يؤدي لتكاثر الطحالب ونضوب ثنائي الأكسجين المذاب.',
    },
    keyHint: { fr: 'DBO₅ élevée + O₂ dissous faible = pollution organique sévère', en: 'High BOD₅ + low dissolved O₂ = severe pollution', ar: 'ارتفاع DBO5 وانخفاض O2 يدلان على تلوث عضوي' },
    keywords: ['eutrophisation', 'dbo5', 'dco', 'pollution', 'eau', 'التخثث'],
  },
];

export const FLASHCARDS: FlashcardItem[] = [
  {
    id: 'fc_1',
    domain: 'CELL_GENETICS',
    question: { fr: 'Combien de molécules d’ADN obtient-on après 1 cycle de réplication ?', en: 'How many DNA molecules result from 1 replication cycle?', ar: 'كم عدد جزيئات الـ ADN الناتجة بعد دورة تضاعف واحدة؟' },
    answer: { fr: '2 molécules identiques, chacune formée d’un brin ancien et d’un brin néosynthétisé (modèle semi-conservatif).', en: '2 identical duplexes, each with one parental strand and one newly synthesized strand.', ar: 'جزيئتان متطابقتان، كل واحدة مكونة من لولب أصلي ولولب جديد (نصف محافظ).' },
  },
  {
    id: 'fc_2',
    domain: 'CELL_GENETICS',
    question: { fr: 'Quel est le rôle des deux types de brassage lors de la méiose ?', en: 'What are the two types of genetic shuffling during meiosis?', ar: 'ما هما نوعا التخليط الصبغي خلال الانقسام الاختزالي؟' },
    answer: { fr: '1) Brassage intrachromosomique (crossing-over en Prophase I). 2) Brassage interchromosomique (ségrégation indépendante en Anaphase I).', en: '1) Intrachromosomal (crossing-over in Prophase I). 2) Interchromosomal (independent assortment in Anaphase I).', ar: '1) التخليط الضمصبغي (العبور في التمهيدية I). 2) التخليط البيصبغي (الافتراق المستقل في الانفصالية I).' },
  },
  {
    id: 'fc_3',
    domain: 'PHYSIOLOGY_IMMUNITY',
    question: { fr: 'Quelle différence de bilan ATP entre respiration et fermentation ?', en: 'What is the ATP yield difference between respiration and fermentation?', ar: 'ما الفرق في الحصيلة الطاقية بين التنفس والتخمر؟' },
    answer: { fr: 'Respiration aérobie : 38 ATP / mole de glucose. Fermentation (lactique ou alcoolique) : 2 ATP seulement.', en: 'Aerobic respiration: 38 ATP/mol glucose. Fermentation: only 2 ATP.', ar: 'التنفس: 38 ATP لكل مول كليكوز. التخمر: 2 ATP فقط.' },
  },
  {
    id: 'fc_4',
    domain: 'IMMUNITY',
    question: { fr: 'Quel est le rôle central des lymphocytes T4 (auxiliaires) ?', en: 'What is the central role of Helper T4 lymphocytes?', ar: 'ما الدور المحوري للمفاويات المساعدة LT4؟' },
    answer: { fr: 'Ils sécrètent les interleukines (IL-2) indispensables à l’activation et à la multiplication des LB et des LT8.', en: 'They secrete interleukins (IL-2) required to activate and clone B cells and cytotoxic T8 cells.', ar: 'تفرز الأنتيرلوكينات الضرورية لتنشيط وتكاثر اللمفاويات B و LT8.' },
  },
  {
    id: 'fc_5',
    domain: 'NERVOUS_MUSCULAR',
    question: { fr: 'Quels ions déclenchent l’exocytose des vésicules synaptiques et la contraction du sarcomère ?', en: 'Which ion triggers both synaptic vesicle exocytosis and sarcomere contraction?', ar: 'ما الأيون الذي يحفز تحرير المبلغ العصبي وتقلص الساركومير؟' },
    answer: { fr: 'Les ions Ca²⁺ ! Ils permettent la libération du neurotransmetteur et exposent les sites d’actine à la myosine.', en: 'Ca²⁺ ions! They trigger neurotransmitter release and unblock actin-myosin binding sites.', ar: 'أيونات الكالسيوم Ca²⁺! تحفز طرح المبلغ العصبي وارتباط الميوزين بالأكتين.' },
  },
  {
    id: 'fc_6',
    domain: 'REPRODUCTION',
    question: { fr: 'Quelle hormone déclenche l’ovulation au 14e jour du cycle ?', en: 'Which hormone triggers ovulation on Day 14?', ar: 'ما الهرمون المسؤول عن حدوث الإباضة في اليوم 14؟' },
    answer: { fr: 'Le pic de LH (hormone lutéinisante), provoqué par un rétrocontrôle positif d’un taux élevé d’œstradiol.', en: 'The LH surge, triggered by positive feedback from peak estradiol levels.', ar: 'ذروة هرمون LH الناتجة عن المفعول الرجعي الإيجابي لارتفاع الأستراديول.' },
  },
  {
    id: 'fc_7',
    domain: 'GEOLOGY',
    question: { fr: 'Qu’est-ce qu’un complexe ophiolitique ?', en: 'What does an ophiolite complex indicate?', ar: 'على ماذا يدل المركب الأفيوليتي في السلاسل الجبلية؟' },
    answer: { fr: 'Un lambeau de lithosphère océanique charrié sur la croûte continentale (obduction), témoin d’un ancien océan disparu.', en: 'A slice of oceanic lithosphere thrust onto a continent (obduction), proving a closed ocean basin.', ar: 'جزء من الغلاف الصخري المحيطي طفا فوق القارة ويشهد على انغلاق محيط قديم.' },
  },
  {
    id: 'fc_8',
    domain: 'ECOLOGY',
    question: { fr: 'Comment calcule-t-on la distance d’un séisme à l’épicentre ?', en: 'How do you estimate earthquake epicenter distance?', ar: 'كيف نحسب المسافة الفاصلة عن بؤرة الزلزال؟' },
    answer: { fr: 'D = Δt / (1/Vs − 1/Vp), où Δt est le décalage d’arrivée entre ondes S et ondes P.', en: 'D = Δt / (1/Vs − 1/Vp), using the arrival delay between S-waves and P-waves.', ar: 'D = Δt / (1/Vs − 1/Vp) اعتماداً على فارق وصول الموجات S و P.' },
  },
];

export const SVT_MIND_MAPS: SvtMindMap[] = [
  {
    id: 'mm_genetics',
    domain: 'CELL_GENETICS',
    title: { fr: 'Expression de l’information génétique', en: 'Gene Expression & Division', ar: 'تعبير الخبر الوراثي والانقسام' },
    centralConcept: { fr: 'ADN → ARNm → Protéine', en: 'DNA → mRNA → Protein', ar: 'ADN ← ARNm ← بروتين' },
    branches: [
      {
        colorHex: '#1B7A54',
        title: { fr: 'Réplication (Noyau)', en: 'Replication', ar: 'التضاعف' },
        items: [
          { fr: 'Phase S • Modèle semi-conservatif', en: 'S-phase • Semi-conservative', ar: 'الطور S • نموذج نصف محافظ' },
          { fr: 'ADN polymérase 5′ → 3′', en: 'DNA polymerase 5′ → 3′', ar: 'أنزيم ADN بوليميراز ′5 ← ′3' },
        ],
      },
      {
        colorHex: '#1E6B94',
        title: { fr: 'Transcription & Traduction', en: 'Transcription & Translation', ar: 'النسخ والترجمة' },
        items: [
          { fr: 'ARN polymérase copie le brin transcrit (T → U)', en: 'RNA pol builds mRNA (T → U)', ar: 'نسخ اللولب المنسوخ (T ← U)' },
          { fr: 'Ribosome lit codon AUG → STOP (UAA/UAG/UGA)', en: 'Ribosome reads AUG → STOP', ar: 'الريبوزوم يترجم الوحدات الرمزية' },
        ],
      },
      {
        colorHex: '#C67E15',
        title: { fr: 'Méiose & Diversité', en: 'Meiosis Diversity', ar: 'التخليط الوراثي' },
        items: [
          { fr: 'Prophase I : Crossing-over intrachromosomique', en: 'Prophase I: Crossing-over', ar: 'التمهيدية I: العبور الصبغي' },
          { fr: 'Anaphase I : Ségrégation aléatoire', en: 'Anaphase I: Independent assortment', ar: 'الانفصالية I: التخليط البيصبغي' },
        ],
      },
    ],
  },
  {
    id: 'mm_physiology',
    domain: 'PHYSIOLOGY_IMMUNITY',
    title: { fr: 'Flux d’énergie & ATP cellulaire', en: 'Cellular Energy & ATP', ar: 'تحرير الطاقة الكامنة وتقلص العضلة' },
    centralConcept: { fr: 'Glucose → Pyruvate → ATP', en: 'Glucose → Pyruvate → ATP', ar: 'كليكوز ← حمض البيروفيك ← ATP' },
    branches: [
      {
        colorHex: '#1B7A54',
        title: { fr: 'Respiration aérobie (38 ATP)', en: 'Aerobic Respiration (38 ATP)', ar: 'التنفس الخلوي (38 ATP)' },
        items: [
          { fr: 'Glycolyse (hyaloplasme) : 2 ATP', en: 'Glycolysis (cytosol): 2 ATP', ar: 'انحلال الكليكوز: 2 ATP' },
          { fr: 'Cycle de Krebs + Phosphorylation oxydative (mitochondrie) : 36 ATP', en: 'Krebs + ETC (mitochondria): 36 ATP', ar: 'حلقة كريبس والتفسفر المؤكسد: 36 ATP' },
        ],
      },
      {
        colorHex: '#D97706',
        title: { fr: 'Fermentation anaérobie (2 ATP)', en: 'Anaerobic Fermentation (2 ATP)', ar: 'التخمر (2 ATP)' },
        items: [
          { fr: 'Lactique (muscle) → Acide lactique', en: 'Lactic (muscle) → Lactate', ar: 'التخمر اللبني في العضلة' },
          { fr: 'Alcoolique (levures) → Éthanol + CO₂', en: 'Alcoholic (yeast) → Ethanol + CO₂', ar: 'التخمر الكحولي في الخميرة' },
        ],
      },
    ],
  },
  {
    id: 'mm_immunity',
    domain: 'IMMUNITY',
    title: { fr: 'Défenses immunitaires', en: 'Immune System Pathways', ar: 'الاستجابات المناعية' },
    centralConcept: { fr: 'Soi vs Non-Soi (CMH)', en: 'Self vs Non-Self (MHC)', ar: 'الذاتي وغير الذاتي (CMH)' },
    branches: [
      {
        colorHex: '#0E609C',
        title: { fr: 'Non spécifique (Rapide)', en: 'Innate Defense', ar: 'غير نوعية (فورية)' },
        items: [
          { fr: 'Barrières + Réaction inflammatoire + Phagocytose', en: 'Barriers + Inflammation + Phagocytosis', ar: 'حواجز طبيعية + التهاب + بلعمة' },
        ],
      },
      {
        colorHex: '#6B3FA0',
        title: { fr: 'Spécifique Humorale & Cellulaire', en: 'Adaptive Defense', ar: 'مناعة نوعية' },
        items: [
          { fr: 'LT4 (IL-2) → Active LB (Anticorps) & LT8 → LTc (Perforine)', en: 'Helper T4 activates B (Antibodies) & Tc (Perforin)', ar: 'LT4 تنشط LB (مضادات الأجسام) و LTc (البرفورين)' },
        ],
      },
    ],
  },
  {
    id: 'mm_nervous',
    domain: 'NERVOUS_MUSCULAR',
    title: { fr: 'Communication nerveuse & Muscle', en: 'Neural & Muscular Control', ar: 'التواصل العصبي والتقلص العضلي' },
    centralConcept: { fr: 'Stimulus → Influx → Contraction', en: 'Stimulus → Impulse → Contraction', ar: 'إهاجة ← سيالة عصبية ← تقلص' },
    branches: [
      {
        colorHex: '#1E6B94',
        title: { fr: 'Influx & Synapse', en: 'Action Potential & Synapse', ar: 'جهد العمل والسينابس' },
        items: [
          { fr: 'Repos -70 mV • Dépolarisation Na⁺ • Repolarisation K⁺', en: 'Resting -70 mV • Na⁺ in / K⁺ out', ar: 'جهد كمون -70mV ثم إزالة وإعادة الاستقطاب' },
          { fr: 'Synapse chimique : Ca²⁺ → Exocytose Acétylcholine', en: 'Ca²⁺ triggers Acetylcholine release', ar: 'دخول الكالسيوم يحرر الأستيل كولين' },
        ],
      },
      {
        colorHex: '#A64B21',
        title: { fr: 'Sarcomère', en: 'Sarcomere Mechanics', ar: 'آلية الساركومير' },
        items: [
          { fr: 'Ca²⁺ + ATP → Glissement Actine / Myosine', en: 'Ca²⁺ + ATP → Actin slides over Myosin', ar: 'انزلاق الأكتين والميوزين بوجود Ca²⁺ و ATP' },
        ],
      },
    ],
  },
  {
    id: 'mm_reproduction',
    domain: 'REPRODUCTION',
    title: { fr: 'Régulation hormonale sexuelle', en: 'Reproductive Hormonal Control', ar: 'التنظيم الهرموني للتوالد' },
    centralConcept: { fr: 'Hypothalamus → Hypophyse → Gonades', en: 'Hypothalamus → Pituitary → Gonads', ar: 'الوطاء ← النخامية ← المناسل' },
    branches: [
      {
        colorHex: '#6B3FA0',
        title: { fr: 'Chez la femme (Cyclique)', en: 'Female Cyclic Axis', ar: 'عند المرأة (دوري)' },
        items: [
          { fr: 'GnRH → FSH/LH → Œstradiol (J1-J14) → Pic LH (Ovulation J14)', en: 'FSH grows follicle → Estradiol spike triggers LH ovulation', ar: 'FSH لنمو الجريب ثم ذروة LH للإباضة' },
          { fr: 'Corps jaune → Progestérone (prépare la muqueuse utérine)', en: 'Corpus luteum secretes progesterone', ar: 'الجسم الأصفر يفرز الجسفرون' },
        ],
      },
      {
        colorHex: '#1E6B94',
        title: { fr: 'Chez l’homme (Continu)', en: 'Male Continuous Axis', ar: 'عند الرجل (مستمر)' },
        items: [
          { fr: 'LH → Leydig (Testostérone) • FSH → Sertoli (Spermatogenèse)', en: 'LH → Leydig (Testosterone) • FSH → Sertoli', ar: 'LH يحفز لايديغ و FSH يحفز سيرتولي' },
        ],
      },
    ],
  },
  {
    id: 'mm_geology',
    domain: 'GEOLOGY',
    title: { fr: 'Tectonique des plaques & Chaînes récentes', en: 'Plate Tectonics & Orogeny', ar: 'تكتونية الصفائح والسلاسل الجبلية' },
    centralConcept: { fr: 'Subduction • Obduction • Collision', en: 'Subduction • Obduction • Collision', ar: 'طمر • طفو • اصطدام' },
    branches: [
      {
        colorHex: '#A64B21',
        title: { fr: 'Marqueurs géologiques', en: 'Geological Markers', ar: 'المميزات الصخرية والبنيوية' },
        items: [
          { fr: 'Andésite + Fosse + Plan de Wadati-Benioff = Subduction', en: 'Andesite + Trench + Benioff = Subduction', ar: 'الأنديزيت ومستوى واداتي بينيوف = الطمر' },
          { fr: 'Ophiolites charriées = Obduction • Migmatite = Anatexie', en: 'Ophiolites = Obduction • Migmatite = Anatexis', ar: 'الأفيوليت = الطفو • الميغماتيت = الانصهار الجزئي' },
        ],
      },
    ],
  },
  {
    id: 'mm_ecology',
    domain: 'ECOLOGY',
    title: { fr: 'Écosystèmes & Impact humain', en: 'Ecosystems & Human Impact', ar: 'الحميلات البيئية والتلوث' },
    centralConcept: { fr: 'Matière cyclique & Flux d’énergie', en: 'Matter Cycle & Energy Flow', ar: 'دورة المادة وتدفق الطاقة' },
    branches: [
      {
        colorHex: '#1B7A54',
        title: { fr: 'Équilibre & Mesures', en: 'Balance & Indicators', ar: 'التوازن ومؤشرات الجودة' },
        items: [
          { fr: 'Producteurs → Consommateurs → Décomposeurs', en: 'Producers → Consumers → Decomposers', ar: 'المنتجون ← المستهلكون ← المحللات' },
          { fr: 'DBO₅, DCO, IBGN mesurent la pollution aquatique', en: 'BOD₅, COD, IBGN evaluate water pollution', ar: 'مؤشرات DBO5 و DCO تقيس تلوث المياه' },
        ],
      },
    ],
  },
];

export const GEOLOGICAL_ERAS: GeologicalEra[] = [
  {
    name: { fr: 'Cénozoïque (Tertiaire – Quaternaire)', en: 'Cenozoic', ar: 'الحقب الحديث (الرابع والثالث)' },
    periodMa: '66 Ma → 0 Ma',
    keyEvents: { fr: 'Expansion des mammifères, orogenèse alpine & rifaine, apparition humaine.', en: 'Mammal diversification, Alpine-Rif mountain building, hominins.', ar: 'تنوع الثدييات، تشكل جبال الألب والريف، ظهور الإنسان.' },
  },
  {
    name: { fr: 'Mésozoïque (Secondaire)', en: 'Mesozoic', ar: 'الحقب الثاني' },
    periodMa: '252 Ma → 66 Ma',
    keyEvents: { fr: 'Fragmentation de la Pangée, ammonites, dinosaures et plantes à fleurs.', en: 'Pangaea breakup, ammonites, dinosaurs, flowering plants.', ar: 'انقسام القارة العظمى، الأمونيت، الديناصورات.' },
  },
  {
    name: { fr: 'Paléozoïque (Primaire)', en: 'Paleozoic', ar: 'الحقب الأول' },
    periodMa: '541 Ma → 252 Ma',
    keyEvents: { fr: 'Trilobites, premières plantes vasculaires, sortie des eaux, chaîne hercynienne.', en: 'Trilobites, land colonization, Hercynian mountains.', ar: 'ثلاثيات الفصوص، غزو اليابسة، السلسلة الهرسينية.' },
  },
  {
    name: { fr: 'Précambrien', en: 'Precambrian', ar: 'ما قبل الكمبري' },
    periodMa: '4600 Ma → 541 Ma',
    keyEvents: { fr: 'Formation de la Terre, cyanobactéries (stromatolites) et enrichissement en O₂.', en: 'Earth formation, cyanobacteria, oxygenation of atmosphere.', ar: 'تشكل الأرض، ظهور البكتيريا الزرقاء والأكسجين.' },
  },
];

export const ROCK_FAMILIES: RockFamily[] = [
  {
    family: { fr: 'Roches Magmatiques', en: 'Igneous Rocks', ar: 'الصخور الصهارية' },
    origin: { fr: 'Refroidissement du magma en profondeur (granite) ou en surface (basalte, andésite).', en: 'Cooling of magma underground (granite) or at surface (basalt, andesite).', ar: 'تصلب الصهارة في العمق (غرانيت) أو على السطح (بازلت، أنديزيت).' },
    examples: ['Granite', 'Basalte', 'Andésite', 'Gabbro'],
  },
  {
    family: { fr: 'Roches Sédimentaires', en: 'Sedimentary Rocks', ar: 'الصخور الرسوبية' },
    origin: { fr: 'Érosion, transport, dépôt en strates et diagenèse (fossiles fréquents).', en: 'Weathering, transport, layered deposition and diagenesis.', ar: 'التعرية، النقل، الترسب في طبقات والتصخر (غنية بالمستحاثات).' },
    examples: ['Calcaire', 'Grès', 'Argile', 'Phosphate'],
  },
  {
    family: { fr: 'Roches Métamorphiques', en: 'Metamorphic Rocks', ar: 'الصخور المتحولة' },
    origin: { fr: 'Transformation à l’état solide sous haute pression et température (schistosité / foliation).', en: 'Solid-state recrystallization under high P & T (foliation).', ar: 'تحول في الحالة الصلبة تحت ضغط وحرارة مرتفعين (شيستية وتوريق).' },
    examples: ['Schiste', 'Micaschiste', 'Gneiss', 'Éclogite'],
  },
];

export function pickText(text: LocalizedText, lang: AppLanguage): string {
  return text[lang] || text.fr;
}
