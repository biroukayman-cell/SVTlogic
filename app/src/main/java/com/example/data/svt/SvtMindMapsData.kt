package com.example.data.svt

data class SvtMindMapBranch(
    val title: LocalizedSvtText,
    val nodes: List<LocalizedSvtText>,
    val keyFormulaOrRule: LocalizedSvtText? = null
)

data class SvtMindMap(
    val id: String,
    val domain: SvtDomain,
    val centralConcept: LocalizedSvtText,
    val subtitle: LocalizedSvtText,
    val summary: LocalizedSvtText,
    val branches: List<SvtMindMapBranch>
)

object SvtMindMapsData {
    val allMindMaps: List<SvtMindMap> = listOf(
        SvtMindMap(
            id = "map_gene_expression",
            domain = SvtDomain.CELL_GENETICS,
            centralConcept = LocalizedSvtText(
                fr = "Information Génétique & Synthèse des Protéines",
                en = "Genetic Information & Protein Synthesis",
                ar = "الخبر الوراثي وتركيب البروتينات"
            ),
            subtitle = LocalizedSvtText(
                fr = "Du chromosome à la protéine fonctionnelle (ADN → ARNm → Polypeptide)",
                en = "From chromosome to functional protein (DNA → mRNA → Polypeptide)",
                ar = "من الصبغي إلى البروتين الوظيفي (ADN → ARNm → عديد الببتيد)"
            ),
            summary = LocalizedSvtText(
                fr = "Comprendre comment l'ADN se réplique, s'exprime dans la cellule et maintient ou diversifie les caractères héréditaires.",
                en = "Understand how DNA replicates, expresses traits in cells, and preserves or diversifies hereditary characters.",
                ar = "فهم كيفية تضاعف الـ ADN وتعبيره عن الصفات الوراثية والحفاظ عليها أو تنويعها."
            ),
            branches = listOf(
                SvtMindMapBranch(
                    title = LocalizedSvtText("1. Structure & Réplication", "1. Structure & Replication", "1. البنية والتضاعف"),
                    nodes = listOf(
                        LocalizedSvtText("Double hélice complémentaire : A=T, C≡G", "Complementary double helix: A=T, C≡G", "لولب مزدوج متكامل: A=T و C≡G"),
                        LocalizedSvtText("Phase S : réplication semi-conservative (ADN polymérase)", "S phase: semi-conservative replication (DNA polymerase)", "طور S: تضاعف نصف محافظ (ADN بوليميراز)"),
                        LocalizedSvtText("Mitose (2n → 2 × 2n) : conservation du caryotype", "Mitosis (2n → 2 × 2n): karyotype conservation", "انقسام غير مباشر (2n → 2×2n): حفظ الصيغة الصبغية")
                    ),
                    keyFormulaOrRule = LocalizedSvtText("Cycle cellulaire : G₁ → S → G₂ → M", "Cell cycle: G₁ → S → G₂ → M", "الدورة الخلوية: G₁ → S → G₂ → M")
                ),
                SvtMindMapBranch(
                    title = LocalizedSvtText("2. Transcription (Noyau)", "2. Transcription (Nucleus)", "2. النسخ (النواة)"),
                    nodes = listOf(
                        LocalizedSvtText("ARN polymérase copie le brin transcrit (3'→5')", "RNA polymerase copies template strand (3'→5')", "ARN بوليميراز ينسخ اللولب المنسوخ"),
                        LocalizedSvtText("Thymine (T) remplacée par Uracile (U)", "Thymine (T) replaced by Uracil (U)", "استبدال التيمين (T) بالأوراسيل (U)"),
                        LocalizedSvtText("ARNm quitte le noyau par les pores nucléaires", "mRNA exits nucleus via nuclear pores", "يغادر ARNm النواة عبر الثقوب النووية")
                    )
                ),
                SvtMindMapBranch(
                    title = LocalizedSvtText("3. Traduction (Cytoplasme)", "3. Translation (Cytoplasm)", "3. الترجمة (السيتوبلازم)"),
                    nodes = listOf(
                        LocalizedSvtText("Initiation : codon AUG (Méthionine) + ribosome", "Initiation: AUG start codon (Methionine) + ribosome", "البداية: وحدة AUG (ميثيونين) + الريبوزوم"),
                        LocalizedSvtText("Élongation : ARNt apporte les acides aminés spécifiques", "Elongation: tRNA delivers specific amino acids", "الاستطالة: ينقل ARNt الأحماض الأمينية النوعية"),
                        LocalizedSvtText("Terminaison : codons STOP (UAA, UAG, UGA)", "Termination: STOP codons (UAA, UAG, UGA)", "النهاية: وحدات التوقف (UAA, UAG, UGA)")
                    ),
                    keyFormulaOrRule = LocalizedSvtText("Gène → ARNm → Protéine → Caractère", "Gene → mRNA → Protein → Trait", "مورثة ← ARNm ← بروتين ← صفة")
                ),
                SvtMindMapBranch(
                    title = LocalizedSvtText("4. Mutations & Diversité", "4. Mutations & Diversity", "4. الطفرات والتنوع"),
                    nodes = listOf(
                        LocalizedSvtText("Substitution, insertion ou délétion de nucléotides", "Nucleotide substitution, insertion, or deletion", "استبدال أو إضافة أو ضياع نيكليوتيدات"),
                        LocalizedSvtText("Méiose (2n → 4 cellules n) + brassages intrachromosomique & interchromosomique", "Meiosis (2n → 4 haploid cells n) + crossing-over & independent assortment", "الانقسام الاختزالي + التخليط الضمصبغي والبيصبغي")
                    )
                )
            )
        ),
        SvtMindMap(
            id = "map_mendel_genetics",
            domain = SvtDomain.CELL_GENETICS,
            centralConcept = LocalizedSvtText(
                fr = "Lois Statistiques de Transmission Héréditaire",
                en = "Statistical Laws of Inheritance",
                ar = "القوانين الإحصائية لانتقال الصفات الوراثية"
            ),
            subtitle = LocalizedSvtText(
                fr = "Monohybridisme, Dihybridisme & Hérédité liée au sexe",
                en = "Monohybrid, Dihybrid & Sex-Linked Inheritance",
                ar = "الهجونة الأحادية، الثنائية والوراثة المرتبطة بالجنس"
            ),
            summary = LocalizedSvtText(
                fr = "Synthèse des proportions phénotypiques F₁ / F₂ et des tests-cross utilisés en génétique formelle.",
                en = "Summary of F₁ / F₂ phenotypic ratios and test-cross rules in classical genetics.",
                ar = "ملخص لنسب المظاهر الخارجية في F₁ و F₂ والتزاوج الاختباري في علم الوراثة."
            ),
            branches = listOf(
                SvtMindMapBranch(
                    title = LocalizedSvtText("Monohybridisme (1 gène)", "Monohybridism (1 gene)", "الهجونة الأحادية (مورثة واحدة)"),
                    nodes = listOf(
                        LocalizedSvtText("Dominance absolue F₂ : 3/4 [A] : 1/4 [a]", "Complete dominance F₂: 3/4 [A] : 1/4 [a]", "سيادة تامة في F₂: 3/4 [A] و 1/4 [a]"),
                        LocalizedSvtText("Codominance F₂ : 1/4 [A] : 2/4 [AB] : 1/4 [B]", "Codominance F₂: 1/4 [A] : 2/4 [AB] : 1/4 [B]", "تساوي السيادة في F₂: 1/4 : 2/4 : 1/4"),
                        LocalizedSvtText("Gène létal : 2/3 [A] : 1/3 [a]", "Lethal allele: 2/3 [A] : 1/3 [a]", "حليل مميت: 2/3 [A] و 1/3 [a]")
                    )
                ),
                SvtMindMapBranch(
                    title = LocalizedSvtText("Dihybridisme (2 gènes)", "Dihybridism (2 genes)", "الهجونة الثنائية (مورثتان)"),
                    nodes = listOf(
                        LocalizedSvtText("Gènes indépendants (F₂) : proportions 9 : 3 : 3 : 1", "Independent genes (F₂): 9 : 3 : 3 : 1 ratio", "مورثتان مستقلتان: نسب 9 : 3 : 3 : 1"),
                        LocalizedSvtText("Back-cross indépendant : 1/4 : 1/4 : 1/4 : 1/4", "Independent test-cross: 25% each phenotype", "تزاوج اختباري مستقل: 25% لكل مظهر"),
                        LocalizedSvtText("Gènes liés (Linkage) : types parentaux > types recombinés", "Linked genes: parental phenotypes > recombinant phenotypes", "مورثتان مرتبطتان: المظاهر الأبوية > المظاهر الجديدة التركيب")
                    )
                ),
                SvtMindMapBranch(
                    title = LocalizedSvtText("Hérédité humaine & Sexe", "Human & Sex-Linked Genetics", "الوراثة البشرية والجنس"),
                    nodes = listOf(
                        LocalizedSvtText("Chromosomes sexuels : XX (femelle) / XY (mâle)", "Sex chromosomes: XX (female) / XY (male)", "الصبغيات الجنسية: XX (أنثى) / XY (ذكر)"),
                        LocalizedSvtText("Allèle lié à X : croisement réciproque non identique", "X-linked trait: reciprocal crosses yield different ratios", "مورثة محمولة على X: التزاوج العكسي لا يعطي نفس النتائج")
                    )
                )
            )
        ),
        SvtMindMap(
            id = "map_immunity",
            domain = SvtDomain.IMMUNITY,
            centralConcept = LocalizedSvtText(
                fr = "Système Immunitaire : Défense de l'Organisme",
                en = "Immune System: Defense of the Body",
                ar = "الجهاز المناعي: الدفاع عن الجسم"
            ),
            subtitle = LocalizedSvtText(
                fr = "Soi / Non-soi, Immunité innée & Immunité adaptative (Humorale / Cellulaire)",
                en = "Self / Non-Self, Innate & Adaptive Immunity (Humoral / Cellular)",
                ar = "الذاتي وغير الذاتي، المناعة الطبيعية والمناعة النوعية"
            ),
            summary = LocalizedSvtText(
                fr = "Vue d'ensemble des barrières, cellules effectrices et coopération cellulaire.",
                en = "Overview of barriers, effector cells, and cellular cooperation.",
                ar = "نظرة شاملة على الحواجز الطبيعية والخلايا المناعية والتعاون الخلوي."
            ),
            branches = listOf(
                SvtMindMapBranch(
                    title = LocalizedSvtText("1. Soi & Non-Soi", "1. Self & Non-Self", "1. الذاتي وغير الذاتي"),
                    nodes = listOf(
                        LocalizedSvtText("Marqueurs CMH (HLA) à la surface des cellules nucléées", "MHC (HLA) markers on nucleated cells", "واسمات CMH على سطح الخلايا المنواة"),
                        LocalizedSvtText("Antigène : toute molécule reconnue étrangère", "Antigen: any molecule recognized as foreign", "مولد المضاد: كل عنصر غريب عن الجسم")
                    )
                ),
                SvtMindMapBranch(
                    title = LocalizedSvtText("2. Immunité Innée (Non spécifique)", "2. Innate Immunity (Non-specific)", "2. المناعة الطبيعية (غير النوعية)"),
                    nodes = listOf(
                        LocalizedSvtText("Barrières naturelles : peau, muqueuses, pH acide", "Natural barriers: skin, mucosa, acidic secretions", "حواجز طبيعية: الجلد، الأغشية المخاطية، الحموضة"),
                        LocalizedSvtText("Réaction inflammatoire (rougeur, chaleur, œdème, douleur)", "Inflammatory response (redness, heat, swelling, pain)", "الاستجابة الالتهابية (احمرار، حرارة، انتفاخ، ألم)"),
                        LocalizedSvtText("Phagocytose : adhésion → ingestion → digestion → rejet", "Phagocytosis: attachment → ingestion → digestion → exocytosis", "البلعمة: التثبيت ← الابتلاع ← الهضم ← الطرح")
                    )
                ),
                SvtMindMapBranch(
                    title = LocalizedSvtText("3. Immunité Adaptative (Spécifique)", "3. Adaptive Immunity (Specific)", "3. المناعة المكتسبة (النوعية)"),
                    nodes = listOf(
                        LocalizedSvtText("Voie humorale : LB → Plasmocytes → Anticorps (Ig)", "Humoral pathway: B cells → Plasma cells → Antibodies", "مسلك خلطي: لمفاويات B ← بلزميات ← مضادات الأجسام"),
                        LocalizedSvtText("Voie cellulaire : LT8 → LTc détruit cellules infectées/cancéreuses", "Cellular pathway: CD8 T → CTL lyses infected cells", "مسلك خلوي: لمفاويات T8 ← T قاتلة تدمر الخلايا المصابة"),
                        LocalizedSvtText("Coopération via LT4 (T helper) & interleukines + Mémoire immunitaire", "Cooperation via CD4 T helper & interleukins + Immune memory", "تعاون مناعي عبر LT4 والأنترلوكينات + ذاكرة مناعية")
                    )
                ),
                SvtMindMapBranch(
                    title = LocalizedSvtText("4. Vaccination & Dysfonctionnements", "4. Vaccination & Disorders", "4. التلقيح واضطرابات المناعة"),
                    nodes = listOf(
                        LocalizedSvtText("Vaccination (préventive, mémoire) vs Sérothérapie (curative, immédiate)", "Vaccination (preventive memory) vs Serotherapy (immediate passive antibodies)", "التلقيح (وقائي/ذاكرة) مقابل الاستمصال (علاجي/فوري)"),
                        LocalizedSvtText("Allergie (IgE, histamine) • SIDA (VIH détruit les LT4)", "Allergy (IgE, histamine) • AIDS (HIV destroys CD4 T cells)", "الحساسية (IgE) • السيدا (فيروس VIH يدمر LT4)")
                    )
                )
            )
        ),
        SvtMindMap(
            id = "map_reproduction",
            domain = SvtDomain.REPRODUCTION,
            centralConcept = LocalizedSvtText(
                fr = "Reproduction Humaine & Régulation Hormonale",
                en = "Human Reproduction & Hormonal Regulation",
                ar = "التوالد البشري والتنظيم الهرموني"
            ),
            subtitle = LocalizedSvtText(
                fr = "Gamétogenèse, Axe Hypothalamo-Hypophysaire & Fécondation",
                en = "Gametogenesis, Hypothalamic-Pituitary Axis & Fertilization",
                ar = "تشكل الأمشاج، المحور السريري النخامي والإخصاب"
            ),
            summary = LocalizedSvtText(
                fr = "Liens entre hormones cérébrales (GnRH, FSH, LH), gonades et cycles féminins.",
                en = "Coordination between brain hormones (GnRH, FSH, LH), gonads, and female cycles.",
                ar = "العلاقة بين الهرمونات الدماغية والغدد التناسلية والدورتين المبيضية والرحمية."
            ),
            branches = listOf(
                SvtMindMapBranch(
                    title = LocalizedSvtText("Axe Hypothalamo-Hypophysaire", "Hypothalamic-Pituitary Axis", "المركب السريري النخامي"),
                    nodes = listOf(
                        LocalizedSvtText("Hypothalamus sécrète GnRH de façon pulsatile", "Hypothalamus secretes pulsatile GnRH", "تحت السرير البصري يفرز GnRH بشكل نبضي"),
                        LocalizedSvtText("Antéhypophyse libère FSH (follicules/Sertoli) & LH (ovulation/Leydig)", "Anterior pituitary releases FSH & LH", "الغدة النخامية تفرز FSH و LH")
                    )
                ),
                SvtMindMapBranch(
                    title = LocalizedSvtText("Cycle Féminin (28 jours)", "Female Cycle (28 days)", "الدورة الأنثوية (28 يوماً)"),
                    nodes = listOf(
                        LocalizedSvtText("J1–J14 Phase folliculaire : œstrogènes croissent ; pic de LH → Ovulation J14", "Day 1–14 Follicular phase: estrogen rise → LH surge → Ovulation Day 14", "الطور الجريبي: ارتفاع الأستروجينات ← ذروة LH ← الإباضة اليوم 14"),
                        LocalizedSvtText("J14–J28 Phase lutéale : corps jaune sécrète progestérone (dentelle utérine)", "Day 14–28 Luteal phase: corpus luteum secretes progesterone", "الطور الجسفروني: الجسم الأصفر يفرز الجسفرون لتجهيز الرحم")
                    ),
                    keyFormulaOrRule = LocalizedSvtText("Rétrocontrôle (–) puis (+) juste avant l'ovulation", "Negative feedback, switching to (+) just before ovulation", "تغذية راجعة سلبية ثم إيجابية قبيل الإباضة")
                ),
                SvtMindMapBranch(
                    title = LocalizedSvtText("Fécondation & Nidation", "Fertilization & Implantation", "الإخصاب والتعشيش"),
                    nodes = listOf(
                        LocalizedSvtText("Réaction acrosomique + caryogamie → cellule-œuf diploïde (2n)", "Acrosome reaction + karyogamy → diploid zygote (2n)", "التفاعل الأكروزومي + التحام النواتين ← بيضة ملقحة (2n)"),
                        LocalizedSvtText("Embryon sécrète HCG qui maintient le corps jaune actif", "Embryo secretes hCG maintaining corpus luteum", "يفرز المضغة هرمون HCG للحفاظ على الجسم الأصفر")
                    )
                )
            )
        ),
        SvtMindMap(
            id = "map_atp_metabolism",
            domain = SvtDomain.PHYSIOLOGY_IMMUNITY,
            centralConcept = LocalizedSvtText(
                fr = "Consommation de la Matière Organique & Flux d'ATP",
                en = "Organic Matter Consumption & ATP Energy Flow",
                ar = "استهلاك المادة العضوية وتدفق الطاقة (ATP)"
            ),
            subtitle = LocalizedSvtText(
                fr = "Respiration Cellulaire (38 ATP) vs Fermentations (2 ATP)",
                en = "Cellular Respiration (38 ATP) vs Fermentations (2 ATP)",
                ar = "التنفس الخلوي (38 ATP) مقابل التخمر (2 ATP)"
            ),
            summary = LocalizedSvtText(
                fr = "Les deux voies métaboliques permettant de régénérer l'ATP indispensable aux activités cellulaires.",
                en = "Both metabolic pathways that regenerate ATP required for cellular work.",
                ar = "المسلكان الاستقلابيان لتجديد الـ ATP اللازم لأنشطة الخلية."
            ),
            branches = listOf(
                SvtMindMapBranch(
                    title = LocalizedSvtText("1. Glycolyse (Hyaloplasme)", "1. Glycolysis (Cytosol)", "1. انحلال الكليكوز (الجبلة الشفافة)"),
                    nodes = listOf(
                        LocalizedSvtText("Commune à la respiration et à la fermentation (anaérobie)", "Common to both respiration and fermentation (anaerobic)", "مرحلة مشتركة ولا تتطلب أكسجين O₂"),
                        LocalizedSvtText("1 Glucose (C₆) → 2 Acides pyruviques (C₃) + 2 ATP + 2 NADH,H⁺", "1 Glucose (C₆) → 2 Pyruvates (C₃) + 2 ATP + 2 NADH", "1 كليكوز ← 2 حمض البيروفيك + 2 ATP + 2 NADH,H⁺")
                    )
                ),
                SvtMindMapBranch(
                    title = LocalizedSvtText("2. Respiration (Mitochondrie, +O₂)", "2. Respiration (Mitochondria, +O₂)", "2. التنفس (الميتوكندري، بوجود O₂)"),
                    nodes = listOf(
                        LocalizedSvtText("Matrice : Cycle de Krebs → décarboxylations (CO₂) + NADH,H⁺ + FADH₂", "Matrix: Krebs cycle → CO₂ release + NADH + FADH₂", "الماتريس: دورة كريبس ← طرح CO₂ وإنتاج النواقل المختزلة"),
                        LocalizedSvtText("Membrane interne : Chaîne respiratoire + Sphères pédonculées (ATP synthase)", "Inner membrane: Electron transport chain + ATP synthase", "الغشاء الداخلي: السلسلة التنفسية + الكرات ذات شمراخ (ATP)"),
                        LocalizedSvtText("O₂ = accepteur final d'électrons → formation d'H₂O", "O₂ is final electron acceptor → forms H₂O", "الأكسجين متقبل نهائي للإلكترونات ← تكوّن الماء")
                    ),
                    keyFormulaOrRule = LocalizedSvtText("Bilan : 1 Glucose + 6 O₂ → 6 CO₂ + 6 H₂O + 38 ATP", "Yield: 1 Glucose + 6 O₂ → 6 CO₂ + 6 H₂O + 38 ATP", "الحصيلة الطاقية: 38 ATP لكل جزيئة كليكوز")
                ),
                SvtMindMapBranch(
                    title = LocalizedSvtText("3. Fermentation (Hyaloplasme, sans O₂)", "3. Fermentation (Cytosol, no O₂)", "3. التخمر (بدون O₂)"),
                    nodes = listOf(
                        LocalizedSvtText("Fermentation lactique : Pyruvate → Acide lactique + 2 ATP", "Lactic fermentation: Pyruvate → Lactic acid + 2 ATP", "تخمر لبني: حمض لبني + 2 ATP (دون طرح CO₂)"),
                        LocalizedSvtText("Fermentation alcoolique : Pyruvate → Éthanol + CO₂ + 2 ATP", "Alcoholic fermentation: Pyruvate → Ethanol + CO₂ + 2 ATP", "تخمر كحولي: إيثانول + CO₂ + 2 ATP"),
                        LocalizedSvtText("Dégradation partielle : les résidus contiennent encore beaucoup d'énergie", "Incomplete breakdown: byproducts still store chemical energy", "هدم جزئي: الحثالة العضوية ما زالت غنية بالطاقة")
                    )
                )
            )
        ),
        SvtMindMap(
            id = "map_nervous_muscle",
            domain = SvtDomain.NERVOUS_MUSCULAR,
            centralConcept = LocalizedSvtText(
                fr = "Influx Nerveux & Contraction Musculaire",
                en = "Nerve Impulse & Muscle Contraction",
                ar = "السيالة العصبية والتقلص العضلي"
            ),
            subtitle = LocalizedSvtText(
                fr = "Potentiel d'action, Synapse chimique & Glissement Actine-Myosine",
                en = "Action Potential, Chemical Synapse & Actin-Myosin Sliding",
                ar = "جهد العمل، السينابس الكيميائي وانزلاق خييطات الأكتين والميوزين"
            ),
            summary = LocalizedSvtText(
                fr = "Comment un message électrique traverse le neurone, franchit une synapse et déclenche le raccourcissement du sarcomère.",
                en = "How electrical signals travel along neurons, cross chemical synapses, and power sarcomere contraction.",
                ar = "كيف تنتقل السيالة عبر الليف العصبي والسينابس لتُحدث تقلص الساركومير."
            ),
            branches = listOf(
                SvtMindMapBranch(
                    title = LocalizedSvtText("1. Message Nerveux", "1. Nerve Signal", "1. السيالة العصبية"),
                    nodes = listOf(
                        LocalizedSvtText("Potentiel de repos (-70 mV) maintenu par pompe Na⁺/K⁺", "Resting potential (-70 mV) via Na⁺/K⁺ pump", "جهد الكمون (-70 mV) عبر مضخة Na⁺/K⁺"),
                        LocalizedSvtText("Potentiel d'action : dépolarisation (Na⁺ entrant) puis repolarisation (K⁺ sortant)", "Action potential: depolarization (Na⁺ influx) then repolarization (K⁺ efflux)", "جهد العمل: إزالة الاستقطاب (دخول Na⁺) ثم إعادة الاستقطاب (خروج K⁺)"),
                        LocalizedSvtText("Codage en fréquence de PA proportionnelle à l'intensité du stimulus", "Frequency modulation encodes stimulus intensity", "تشفير شدة الإهاجة عبر تردد جهود العمل")
                    )
                ),
                SvtMindMapBranch(
                    title = LocalizedSvtText("2. Synapse Chimique", "2. Chemical Synapse", "2. السينابس الكيميائي"),
                    nodes = listOf(
                        LocalizedSvtText("Arrivée PA → entrée de Ca²⁺ → exocytose du neurotransmetteur (ACh)", "AP arrival → Ca²⁺ entry → neurotransmitter exocytosis", "وصول جهد العمل ← دخول Ca²⁺ ← طرح المبلغ العصبي (ACh)"),
                        LocalizedSvtText("Fixation sur récepteurs postsynaptiques → PPSE ou PPSI", "Postsynaptic receptor binding → EPSP or IPSP", "التثبت على المستقبلات البعد سينابسية ← PPSE أو PPSI")
                    )
                ),
                SvtMindMapBranch(
                    title = LocalizedSvtText("3. Contraction du Sarcomère", "3. Sarcomere Contraction", "3. تقلص الساركومير"),
                    nodes = listOf(
                        LocalizedSvtText("Libération de Ca²⁺ par le réticulum sarcoplasmique", "Ca²⁺ release from sarcoplasmic reticulum", "تحرير أيونات Ca²⁺ من الشبكة الساركوبلازمية"),
                        LocalizedSvtText("Formation des ponts actine-myosine + hydrolyse d'ATP", "Actin-myosin cross-bridges + ATP hydrolysis", "تشكّل الجسور بين الأكتين والميوزين + حلمأة ATP"),
                        LocalizedSvtText("Raccourcissement des bandes I et zone H ; bande A constante", "Shortening of I-bands & H-zone; A-band stays constant", "تقلص الأشرطة الفاتحة I والمنطقة H وثبات الشريط الداكن A")
                    )
                )
            )
        ),
        SvtMindMap(
            id = "map_geology_orogeny",
            domain = SvtDomain.GEOLOGY,
            centralConcept = LocalizedSvtText(
                fr = "Chaînes de Montagnes & Métamorphisme",
                en = "Mountain Building & Metamorphism",
                ar = "السلاسل الجبلية والتحول الصخري"
            ),
            subtitle = LocalizedSvtText(
                fr = "Subduction, Obduction, Collision & Anatexie",
                en = "Subduction, Obduction, Collision & Anatexis",
                ar = "التمر، الطفو، الاصطدام والانصهار الجزئي"
            ),
            summary = LocalizedSvtText(
                fr = "Les phénomènes géologiques accompagnant la convergence des plaques lithosphériques.",
                en = "Geological phenomena occurring along convergent plate boundaries.",
                ar = "الظواهر الجيولوجية المصاحبة لتقارب الصفائح الصخرية."
            ),
            branches = listOf(
                SvtMindMapBranch(
                    title = LocalizedSvtText("1. Subduction", "1. Subduction", "1. سلاسل التمر"),
                    nodes = listOf(
                        LocalizedSvtText("Plongement de la lithosphère océanique dense sous une autre plaque", "Dense oceanic lithosphere sinks beneath overriding plate", "انغراز الصفيحة المحيطية الأكثر كثافة تحت صفيحة أخرى"),
                        LocalizedSvtText("Marqueurs : fosse océanique, plan de Wadati-Benioff, volcanisme andésitique", "Markers: deep trench, Wadati-Benioff zone, andesitic volcanism", "المميزات: حفرة محيطية، مستوى بينيوف، بركانية أنديزيتية")
                    )
                ),
                SvtMindMapBranch(
                    title = LocalizedSvtText("2. Obduction & Collision", "2. Obduction & Collision", "2. الطفو والاصطدام"),
                    nodes = listOf(
                        LocalizedSvtText("Obduction : complexe ophiolitique charrié sur le continent (ex : Oman)", "Obduction: ophiolite sequence thrust onto continent", "الطفو: زحف مركب الأوفيوليت فوق القارة"),
                        LocalizedSvtText("Collision : affrontement de 2 continents → plis, failles inverses, racine crustale", "Collision: 2 continents collide → folds, reverse faults, deep crustal root", "الاصطدام: تجابه قارتين ← طيات، فوالق معكوسة وجذر قشري")
                    )
                ),
                SvtMindMapBranch(
                    title = LocalizedSvtText("3. Métamorphisme & Granitisation", "3. Metamorphism & Granitization", "3. التحول والغرانيتية"),
                    nodes = listOf(
                        LocalizedSvtText("Série métamorphique : Schiste → Micaschiste → Gneiss", "Metamorphic sequence: Slate → Schist → Gneiss", "المتتالية التحولية: شيست ← ميكاشيست ← غنيس"),
                        LocalizedSvtText("Anatexie → Migmatite puis Granite d'anatexie (plutonique)", "Partial melting → Migmatite → Anatectic granite", "انصهار جزئي ← ميغماتيت ثم غرانيت أناتيكتي")
                    )
                )
            )
        ),
        SvtMindMap(
            id = "map_ecology_matter_pollution",
            domain = SvtDomain.ECOLOGY,
            centralConcept = LocalizedSvtText(
                fr = "Écosystèmes, Matière & Environnement",
                en = "Ecosystems, Matter Flow & Pollution",
                ar = "الحميلات البيئية، تدفق المادة والتلوث"
            ),
            subtitle = LocalizedSvtText(
                fr = "Réseaux trophiques, Eutrophisation, Bioaccumulation & DBO₅",
                en = "Food Webs, Eutrophication, Bioaccumulation & BOD₅",
                ar = "الشبكات الغذائية، التخصب، التراكم الإحيائي ومؤشرات الماء"
            ),
            summary = LocalizedSvtText(
                fr = "Comprendre l'équilibre des écosystèmes naturels et l'impact des activités humaines.",
                en = "Understand ecosystem balance and human impacts on water, soil, and climate.",
                ar = "فهم توازن الأنظمة البيئية وتأثير الأنشطة البشرية على الماء والتربة والمناخ."
            ),
            branches = listOf(
                SvtMindMapBranch(
                    title = LocalizedSvtText("Productivité & Pyramides", "Productivity & Pyramids", "الإنتاجية والأهرام البيئية"),
                    nodes = listOf(
                        LocalizedSvtText("Producteurs primaires (chlorophylliens) fixent le carbone minéral", "Primary producers fix inorganic carbon via photosynthesis", "المنتجون الأوليون يثبتون الكربون عبر التركيب الضوئي"),
                        LocalizedSvtText("Rendement écologique ~10% d'un niveau trophique au suivant", "~10% energy transfer efficiency between trophic levels", "مردودية طاقية تقارب 10% بين مستويين غذائيين")
                    )
                ),
                SvtMindMapBranch(
                    title = LocalizedSvtText("Pollutions des Eaux & Sols", "Water & Soil Pollution", "تلوث المياه والتربة"),
                    nodes = listOf(
                        LocalizedSvtText("Excès de Nitrates/Phosphates → Prolifération d'algues → Eutrophisation (chute O₂)", "Excess Nitrates/Phosphates → Algal bloom → Eutrophication (O₂ drop)", "فائض النترات والفوسفاط ← تكاثر الطحالب ← التخصب ونقص O₂"),
                        LocalizedSvtText("Indicateurs de qualité : DBO₅, DCO, IBGN (invertébrés benthiques)", "Quality indicators: BOD₅, COD, benthic biotic index", "مؤشرات الجودة: DBO₅ و DCO والمؤشر الإحيائي IBGN"),
                        LocalizedSvtText("Bioaccumulation : métaux lourds & pesticides concentrés dans la chaîne", "Bioaccumulation: heavy metals & pesticides magnify up food chains", "التراكم الإحيائي للمعادن الثقيلة والمبيدات عبر السلسلة")
                    )
                )
            )
        )
    )
}
