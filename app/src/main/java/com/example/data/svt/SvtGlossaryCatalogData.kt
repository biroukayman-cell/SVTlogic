package com.example.data.svt

internal object SvtGlossaryCatalogData {

    val allEntries: List<SvtGlossaryEntry> = listOf(
        // --- Génétique & Cellule ---
        SvtGlossaryEntry(
            id = "mitosis",
            domain = SvtDomain.CELL_GENETICS,
            term = LocalizedSvtText("Mitose", "Mitosis", "الانقسام غير المباشر"),
            definition = LocalizedSvtText(
                fr = "Division cellulaire conforme donnant deux cellules filles génétiquement identiques à la cellule mère (2n → 2n).",
                en = "Equational cell division producing two daughter cells genetically identical to the parent cell (2n → 2n).",
                ar = "انقسام خلوي مطابق ينتج عنه خليتان بنتان متطابقتان وراثياً مع الخلية الأم (2n → 2n)."
            ),
            keyHint = LocalizedSvtText(
                fr = "4 phases : Prophase, Métaphase, Anaphase, Télophase (PMAT)",
                en = "4 phases: Prophase, Metaphase, Anaphase, Telophase (PMAT)",
                ar = "4 أطوار: التمهيدي، الاستوائي، الانفصالي، النهائي"
            ),
            keywords = listOf("mitose", "division cellulaire", "pmat", "prophase", "metaphase", "anaphase", "telophase", "mitosis")
        ),
        SvtGlossaryEntry(
            id = "meiosis",
            domain = SvtDomain.CELL_GENETICS,
            term = LocalizedSvtText("Méiose", "Meiosis", "الانقسام الاختزالي"),
            definition = LocalizedSvtText(
                fr = "Succession de deux divisions produisant 4 cellules haploïdes (gamètes, n chromosomes) et assurant le brassage génétique.",
                en = "Two consecutive divisions yielding 4 haploid gametes (n chromosomes) while driving genetic recombination.",
                ar = "تتالي انقسامين يعطي 4 خلايا أحادية الصيغة الصبغية (أمشاج n) ويحقق التخليط الوراثي."
            ),
            keyHint = LocalizedSvtText(
                fr = "Crossing-over en Prophase I • Brassage interchromosomique en Anaphase I",
                en = "Crossing-over in Prophase I • Independent assortment in Anaphase I",
                ar = "العبور الصبغي في التمهيدية I • التخليط البيصبغي في الانفصالية I"
            ),
            keywords = listOf("meiose", "meiosis", "crossing-over", "brassage", "haploide", "الانقسام الاختزالي")
        ),
        SvtGlossaryEntry(
            id = "dna",
            domain = SvtDomain.CELL_GENETICS,
            term = LocalizedSvtText("ADN (Acide désoxyribonucléique)", "DNA (Deoxyribonucleic Acid)", "ADN (الحمض النووي الريبوزي منقوص الأكسجين)"),
            definition = LocalizedSvtText(
                fr = "Molécule en double hélice support de l'information génétique, constituée de nucléotides complémentaires (A=T et C≡G).",
                en = "Double-helix molecule carrying genetic information, built from complementary base pairs (A=T and C≡G).",
                ar = "جزيئة مزدوجة اللولب حاملة للخبر الوراثي، مكونة من نيكليوتيدات متكاملة (A=T و C≡G)."
            ),
            keyHint = LocalizedSvtText(
                fr = "Réplication semi-conservative en phase S (ADN polymérase)",
                en = "Semi-conservative replication during S phase (DNA polymerase)",
                ar = "تضاعف نصف محافظ خلال الطور S بواسطة أنزيم ADN بوليميراز"
            ),
            keywords = listOf("adn", "dna", "nucleotide", "replication", "double helice", "الحمض النووي")
        ),
        SvtGlossaryEntry(
            id = "transcription",
            domain = SvtDomain.CELL_GENETICS,
            term = LocalizedSvtText("Transcription & Traduction", "Transcription & Translation", "النسخ والترجمة"),
            definition = LocalizedSvtText(
                fr = "La transcription copie un gène d'ADN en ARNm dans le noyau ; la traduction assemble les acides aminés au ribosome.",
                en = "Transcription copies a DNA gene into mRNA in the nucleus; translation assembles amino acids at the ribosome.",
                ar = "ينسخ النسخ المورثة من ADN إلى ARNm داخل النواة، بينما تترجم الريبوزومات الكودونات إلى أحماض أمينية."
            ),
            keyHint = LocalizedSvtText(
                fr = "Codon initiateur : AUG (Met) • Codons stop : UAA, UAG, UGA",
                en = "Start codon: AUG (Met) • Stop codons: UAA, UAG, UGA",
                ar = "وحدة البداية: AUG (Met) • وحدات التوقف: UAA, UAG, UGA"
            ),
            keywords = listOf("transcription", "traduction", "arnm", "mrna", "ribosome", "codon", "proteine", "النسخ", "الترجمة")
        ),
        SvtGlossaryEntry(
            id = "allele",
            domain = SvtDomain.CELL_GENETICS,
            term = LocalizedSvtText("Allèle, Génotype & Phénotype", "Allele, Genotype & Phenotype", "الحليل، النمط الوراثي والمظهر الخارجي"),
            definition = LocalizedSvtText(
                fr = "Un allèle est une version d'un gène. Le génotype désigne l'ensemble des allèles ; le phénotype représente les caractères observables.",
                en = "An allele is a gene variant. Genotype is the set of alleles; phenotype is the set of observable traits.",
                ar = "الحليل هو شكل من أشكال المورثة. النمط الوراثي هو مجموع الحليلات، والمظهر الخارجي هو الصفات الملاحظة."
            ),
            keyHint = LocalizedSvtText(
                fr = "Homozygote (A//A) • Hétérozygote (A//a) • Codominance ou dominance",
                en = "Homozygous (AA) • Heterozygous (Aa) • Dominance or codominance",
                ar = "متشابه الاقتران • مختلف الاقتران • سيادة تامة أو تساوي السيادة"
            ),
            keywords = listOf("allele", "genotype", "phenotype", "gene", "homozygote", "heterozygote", "الحليل", "المورثة")
        ),
        SvtGlossaryEntry(
            id = "chromosome",
            domain = SvtDomain.CELL_GENETICS,
            term = LocalizedSvtText("Chromosome & Caryotype", "Chromosome & Karyotype", "الصبغي والخريطة الصبغية"),
            definition = LocalizedSvtText(
                fr = "Structure nucléaire constituée d'ADN condensé portant les gènes. Le caryotype représente l'ensemble ordonné des chromosomes d'une cellule (2n = 46 chez l'humain).",
                en = "Nuclear structure made of condensed DNA carrying genes. A karyotype displays a cell's full ordered chromosome set (2n = 46 in humans).",
                ar = "بنية نووية تتكون من ADN متكثف يحمل المورثات، وتمثل الخريطة الصبغية مجموع صبغيات الخلية مرتبة (2n = 46 عند الإنسان)."
            ),
            keyHint = LocalizedSvtText(
                fr = "22 paires d'autosomes + 1 paire de chromosomes sexuels (XX ou XY)",
                en = "22 pairs of autosomes + 1 pair of sex chromosomes (XX or XY)",
                ar = "22 زوجاً من الصبغيات اللاجنسية + زوج جنسي واحد (XX أو XY)"
            ),
            keywords = listOf("chromosome", "caryotype", "karyotype", "autosome", "chromatide", "الصبغي", "الخريطة الصبغية")
        ),
        SvtGlossaryEntry(
            id = "enzyme",
            domain = SvtDomain.CELL_GENETICS,
            term = LocalizedSvtText("Enzyme & Site actif", "Enzyme & Active Site", "الأنزيم والموقع النشيط"),
            definition = LocalizedSvtText(
                fr = "Biocatalyseur protéique qui accélère une réaction chimique spécifique en abaissant son énergie d'activation sans être consommé.",
                en = "Protein biocatalyst that speeds up a specific chemical reaction by lowering activation energy without being consumed.",
                ar = "وسيط حيوي بروتيني يسرّع تفاعلاً كيميائياً نوعياً دون أن يُستهلك."
            ),
            keyHint = LocalizedSvtText(
                fr = "Spécificité de substrat (complexe E–S) • Sensible au pH et à la température",
                en = "Substrate specificity (E–S complex) • Sensitive to pH and temperature",
                ar = "نوعية الركيزة (مركب أنزيم–ركيزة) • يتأثر بالحرارة ودرجة الحموضة pH"
            ),
            keywords = listOf("enzyme", "catalyseur", "site actif", "substrat", "الأنزيم", "الموقع النشيط")
        ),

        // --- Immunité ---
        SvtGlossaryEntry(
            id = "antigen_self",
            domain = SvtDomain.IMMUNITY,
            term = LocalizedSvtText("Antigène & Soi / Non-soi", "Antigen & Self / Non-self", "مولد المضاد والذاتي / غير الذاتي"),
            definition = LocalizedSvtText(
                fr = "Un antigène est toute molécule étrangère reconnue par le système immunitaire (non-soi) et capable de déclencher une réponse immunitaire spécifique.",
                en = "An antigen is any foreign molecule recognized by the immune system (non-self) that triggers a specific immune response.",
                ar = "مولد المضاد هو كل جزيئة أجنبية (غير ذاتية) يتعرف عليها الجهاز المناعي وتثير استجابة مناعية نوعية."
            ),
            keyHint = LocalizedSvtText(
                fr = "Marqueurs du soi : système CMH (HLA) et marqueurs ABO / Rhésus",
                en = "Self markers: MHC (HLA) complex and ABO / Rhesus blood antigens",
                ar = "واسمات الذاتي: مركب التلاؤم النسيجي CMH والفصائل الدموية ABO"
            ),
            keywords = listOf("antigene", "antigen", "soi", "non-soi", "cmh", "hla", "immuno", "مولد المضاد", "الذاتي")
        ),
        SvtGlossaryEntry(
            id = "phagocytosis",
            domain = SvtDomain.IMMUNITY,
            term = LocalizedSvtText("Phagocytose", "Phagocytosis", "البلعمة"),
            definition = LocalizedSvtText(
                fr = "Mécanisme de l'immunité innée (non spécifique) par lequel des cellules phagocytaires (macrophages, granulocytes) englobent et digèrent les micro-organismes.",
                en = "Innate (non-specific) immune mechanism by which phagocytes (macrophages, neutrophils) engulf and digest pathogens.",
                ar = "آلية مناعية طبيعية غير نوعية تبتلع بواسطتها البلعميات (البلاعم والعدلات) الجراثيم وتهضمها."
            ),
            keyHint = LocalizedSvtText(
                fr = "4 étapes : Adhésion → Ingestion → Digestion (lysosomes) → Rejet des débris",
                en = "4 steps: Adherence → Ingestion → Digestion (lysosomes) → Exocytosis of debris",
                ar = "4 مراحل: التثبيت ← الابتلاع ← الهضم ← طرح الحطام"
            ),
            keywords = listOf("phagocytose", "phagocyte", "macrophage", "granulocyte", "immunite innee", "البلعمة")
        ),
        SvtGlossaryEntry(
            id = "antibodies",
            domain = SvtDomain.IMMUNITY,
            term = LocalizedSvtText("Anticorps (Immunoglobulines)", "Antibodies (Immunoglobulins)", "مضادات الأجسام"),
            definition = LocalizedSvtText(
                fr = "Protéines en forme de Y sécrétées par les plasmocytes (issus des lymphocytes B), capables de se lier spécifiquement à un antigène pour former un complexe immun et le neutraliser.",
                en = "Y-shaped proteins secreted by plasma cells (derived from B lymphocytes) that bind specifically to an antigen to form an immune complex and neutralize it.",
                ar = "بروتينات على شكل Y تفرزها البلزميات (الناتجة عن اللمفاويات B) وترتبط نوعياً بمولد المضاد لتكوين مركب منيع وإبطال مفعوله."
            ),
            keyHint = LocalizedSvtText(
                fr = "Complexe immun → Neutralisation + facilitation de la phagocytose",
                en = "Immune complex → Neutralization + opsonization for phagocytosis",
                ar = "المركب المنيع ← إبطال المفعول وتسهيل البلعمة"
            ),
            keywords = listOf("anticorps", "antibody", "immunoglobuline", "plasmocyte", "complexe immun", "مضادات الأجسام", "البلزميات")
        ),
        SvtGlossaryEntry(
            id = "immunity_humoral",
            domain = SvtDomain.IMMUNITY,
            term = LocalizedSvtText("Immunité humorale & cellulaire", "Humoral & Cell-Mediated Immunity", "المناعة الخلطية والخلوية"),
            definition = LocalizedSvtText(
                fr = "La voie humorale repose sur les lymphocytes B et les anticorps circulants ; la voie cellulaire implique les lymphocytes T cytotoxiques (LTc) détruisant les cellules infectées.",
                en = "Humoral immunity relies on B cells and circulating antibodies; cell-mediated immunity uses cytotoxic T cells (Tc) to destroy infected host cells.",
                ar = "تعتمد المناعة الخلطية على اللمفاويات B ومضادات الأجسام، بينما تعتمد الخلوية على اللمفاويات T القاتلة لتدمير الخلايا المعفنة."
            ),
            keyHint = LocalizedSvtText(
                fr = "LT4 auxiliaires (interleukines) coordonnent les deux réponses spécifiques",
                en = "Helper T4 cells (interleukins) coordinate both specific responses",
                ar = "تنسق اللمفاويات المساعدة LT4 الاستجابتين عبر الأنترلوكينات"
            ),
            keywords = listOf("immunite", "immunity", "humorale", "cellulaire", "lymphocyte", "lt4", "ltc", "lb", "المناعة", "الخلطية", "الخلوية")
        ),
        SvtGlossaryEntry(
            id = "vaccination_serotherapy",
            domain = SvtDomain.IMMUNITY,
            term = LocalizedSvtText("Vaccination & Sérothérapie", "Vaccination & Serotherapy", "التلقيح والاستمصال"),
            definition = LocalizedSvtText(
                fr = "La vaccination est une immunisation active préventive qui crée une mémoire immunitaire durable ; la sérothérapie apporte immédiatement des anticorps prêts pour une protection curative temporaire.",
                en = "Vaccination provides active, preventive immunization creating lasting immune memory; serotherapy delivers ready-made antibodies for immediate, short-term treatment.",
                ar = "التلقيح تمنيع نشيط وقائي يُكسب ذاكرة مناعية طويلة الأمد، بينما الاستمصال يمنح مضادات أجسام جاهزة لحماية فورية ومؤقتة."
            ),
            keyHint = LocalizedSvtText(
                fr = "Vaccin = Préventif, lent, durable • Sérum = Curatif, immédiat, passager",
                en = "Vaccine = Preventive, slow onset, long-lasting • Serum = Curative, immediate, temporary",
                ar = "اللقاح = وقائي ودائم • المصل = علاجي وفوري ومؤقت"
            ),
            keywords = listOf("vaccin", "vaccination", "serotherapie", "serum", "memoire immunitaire", "التلقيح", "الاستمصال")
        ),
        SvtGlossaryEntry(
            id = "allergy_hiv",
            domain = SvtDomain.IMMUNITY,
            term = LocalizedSvtText("Allergie & SIDA (VIH)", "Allergy & AIDS (HIV)", "الأرجية والسيدا (VIH)"),
            definition = LocalizedSvtText(
                fr = "L'allergie est une réponse immunitaire excessive dirigée contre un allergène bénin (IgE et histamine) ; le VIH infecte et détruit les lymphocytes T4, provoquant un déficit immunitaire acquis (SIDA).",
                en = "Allergy is an exaggerated immune reaction to a harmless allergen (IgE and histamine); HIV targets and destroys T4 helper cells, causing acquired immunodeficiency (AIDS).",
                ar = "الأرجية استجابة مناعية مفرطة تجاه مؤرج غير ضار، بينما يصيب فيروس VIH اللمفاويات T4 ويدمرها مما يسبب فقدان المناعة المكتسب (السيدا)."
            ),
            keyHint = LocalizedSvtText(
                fr = "Allergie : mastocytes + histamine • VIH : chute progressive des LT4",
                en = "Allergy: mast cells + histamine • HIV: progressive drop in helper T4 cells",
                ar = "الأرجية: الخلايا البدينة والهستامين • VIH: تدمير تدريجي للخلايا LT4"
            ),
            keywords = listOf("allergie", "allergy", "allergene", "histamine", "vih", "hiv", "sida", "aids", "الأرجية", "السيدا")
        ),

        // --- Reproduction ---
        SvtGlossaryEntry(
            id = "fertilization",
            domain = SvtDomain.REPRODUCTION,
            term = LocalizedSvtText("Fécondation", "Fertilization", "الإخصاب"),
            definition = LocalizedSvtText(
                fr = "Fusion d'un gamète mâle haploïde (spermatozoïde, n) et d'un gamète femelle haploïde (ovule, n) formant une cellule-œuf diploïde (zygote, 2n).",
                en = "Fusion of a haploid male gamete (sperm, n) with a haploid female gamete (ovum, n) to form a diploid zygote (2n).",
                ar = "اندماج مشيج ذكري أحادي الصيغة الصبغية (حيوان منوي n) مع مشيج أنثوي (بويضة n) لتكوين بيضة ثنائية الصيغة الصبغية (2n)."
            ),
            keyHint = LocalizedSvtText(
                fr = "Restaure la diploïdie (2n) • Chez l'humain, a lieu dans le tiers supérieur de la trompe",
                en = "Restores diploidy (2n) • In humans, occurs in the upper third of the Fallopian tube",
                ar = "يعيد الصيغة الصبغية الثنائية 2n ويحدث عند الإنسان في الثلث العلوي للخرطوم"
            ),
            keywords = listOf("fecondation", "fertilization", "zygote", "cellule oeuf", "reproduction", "الإخصاب", "التوالد")
        ),
        SvtGlossaryEntry(
            id = "gametogenesis",
            domain = SvtDomain.REPRODUCTION,
            term = LocalizedSvtText("Gamètes & Gamétogenèse", "Gametes & Gametogenesis", "الأمشاج وتشكل الأمشاج"),
            definition = LocalizedSvtText(
                fr = "Formation des cellules reproductrices haploïdes (n) par méiose dans les gonades : spermatogenèse dans les testicules et ovogenèse dans les ovaires.",
                en = "Formation of haploid reproductive cells (n) via meiosis in the gonads: spermatogenesis in the testes and oogenesis in the ovaries.",
                ar = "تشكل الخلايا التناسلية أحادية الصيغة الصبغية (n) عن طريق الانقسام الاختزالي: تشكل النطاف في الخصيتين وتشكل البويضات في المبيضين."
            ),
            keyHint = LocalizedSvtText(
                fr = "Spermatogenèse : 4 spermatozoïdes/méiose • Ovogenèse : 1 ovocyte + globules polaires",
                en = "Spermatogenesis: 4 sperm per meiosis • Oogenesis: 1 ovum + polar bodies",
                ar = "تشكل النطاف: 4 أمشاج • تشكل البويضات: بويضة واحدة وكريات قطبية"
            ),
            keywords = listOf("gamete", "gametogenese", "spermatogenese", "ovogenese", "spermatozoide", "ovule", "ovocyte", "الأمشاج")
        ),
        SvtGlossaryEntry(
            id = "ovarian_cycle",
            domain = SvtDomain.REPRODUCTION,
            term = LocalizedSvtText("Cycle ovarien & Ovulation", "Ovarian Cycle & Ovulation", "الدورة المبيضية والإباضة"),
            definition = LocalizedSvtText(
                fr = "Succession mensuelle (~28 jours) comprenant la phase folliculaire (maturation d'un follicule), l'ovulation au 14e jour déclenchée par le pic de LH, puis la phase lutéale (corps jaune).",
                en = "Monthly (~28-day) cycle consisting of the follicular phase (follicle maturation), ovulation around day 14 triggered by an LH surge, and the luteal phase (corpus luteum).",
                ar = "دورة شهرية (~28 يوماً) تتضمن الطور الجريبي، الإباضة في اليوم 14 بتحفيز من ذروة هرمون LH، ثم الطور اللوتيئيني (الجسم الأصفر)."
            ),
            keyHint = LocalizedSvtText(
                fr = "J1–J14 : Œstrogènes • J14 : Pic de LH → Ovulation • J14–J28 : Progestérone",
                en = "Days 1–14: Estrogen • Day 14: LH surge → Ovulation • Days 14–28: Progesterone",
                ar = "يوم 1–14: أستروجين • يوم 14: ذروة LH والإباضة • يوم 14–28: بروجستيرون"
            ),
            keywords = listOf("ovulation", "cycle ovarien", "follicule", "corps jaune", "lh", "fsh", "oestrogene", "progesterone", "الإباضة", "المبيض")
        ),
        SvtGlossaryEntry(
            id = "uterine_cycle",
            domain = SvtDomain.REPRODUCTION,
            term = LocalizedSvtText("Cycle utérin & Nidation", "Uterine Cycle & Implantation", "الدورة الرحمية والتعشيش"),
            definition = LocalizedSvtText(
                fr = "Préparation cyclique de la muqueuse utérine (endomètre) à recevoir l'embryon. En cas de fécondation, l'embryon s'implante dans l'endomètre (nidation) ; sinon surviennent les règles.",
                en = "Cyclic preparation of the uterine lining (endometrium) to receive an embryo. Upon fertilization, the embryo implants (implantation); otherwise menstruation occurs.",
                ar = "تهيئة دورية لمخاطية الرحم لاستقبال الجنين. عند حدوث الإخصاب ينغرس الجنين في الرحم (التعشيش)، وإلا يحدث الحيض."
            ),
            keyHint = LocalizedSvtText(
                fr = "Règles (J1–J5) → Prolifération (Œstrogènes) → Dentelle utérine (Progestérone)",
                en = "Menses (D1–5) → Proliferation (Estrogen) → Secretory phase (Progesterone)",
                ar = "الحيض ← التكاثر (أستروجين) ← التخريم الرحمي (بروجستيرون)"
            ),
            keywords = listOf("nidation", "implantation", "cycle uterin", "endometre", "menstruation", "regles", "uterus", "التعشيش", "الرحم")
        ),
        SvtGlossaryEntry(
            id = "placenta_gestation",
            domain = SvtDomain.REPRODUCTION,
            term = LocalizedSvtText("Placenta & Gestation", "Placenta & Pregnancy", "المشيمة والحمل"),
            definition = LocalizedSvtText(
                fr = "Organe temporaire d'échanges entre le sang maternel et le sang fœtal (nutriments, O₂, CO₂, déchets) sans mélange direct des sangs, assurant aussi une fonction endocrine (HCG, progestérone).",
                en = "Temporary exchange organ between maternal and fetal blood (nutrients, O₂, CO₂, wastes) without direct blood mixing, also producing hormones (hCG, progesterone).",
                ar = "عضو تبادل مؤقت بين دم الأم ودم الجنين (مغذيات، O₂، CO₂ وفضلات) دون اختلاط الدم، كما يفرز هرمونات الحمل."
            ),
            keyHint = LocalizedSvtText(
                fr = "Cordon ombilical relie le fœtus au placenta • Barrière sélective protectrice",
                en = "Umbilical cord links fetus to placenta • Selective protective barrier",
                ar = "يربط الحبل السري الجنين بالمشيمة التي تشكل حاجزاً انتقائياً"
            ),
            keywords = listOf("placenta", "gestation", "grossesse", "foetus", "cordon ombilical", "hcg", "المشيمة", "الحمل")
        ),
        SvtGlossaryEntry(
            id = "plant_reproduction",
            domain = SvtDomain.REPRODUCTION,
            term = LocalizedSvtText("Pollinisation & Reproduction végétale", "Pollination & Plant Reproduction", "الأبر والتوالد عند النباتات"),
            definition = LocalizedSvtText(
                fr = "Transport des grains de pollen des étamines vers le pistil, suivi chez les Angiospermes d'une double fécondation donnant l'embryon (2n) et l'albumen nourricier (3n) de la graine.",
                en = "Transfer of pollen grains from stamens to pistil, followed in angiosperms by double fertilization forming the diploid embryo (2n) and triploid endosperm (3n) inside the seed.",
                ar = "انتقال حبوب اللقاح من الأسدية نحو المدقة، يليه عند كاسيات البذور إخصاب مضاعف يعطي الجنين (2n) والسويداء المدخرة (3n)."
            ),
            keyHint = LocalizedSvtText(
                fr = "Fleur → Pollinisation → Double fécondation → Graine + Fruit",
                en = "Flower → Pollination → Double fertilization → Seed + Fruit",
                ar = "زهرة ← أبر ← إخصاب مضاعف ← بذرة وثمرة"
            ),
            keywords = listOf("pollinisation", "pollen", "double fecondation", "graine", "angiosperme", "fleur", "reproduction vegetale", "الأبر")
        ),

        // --- Physiologie & Métabolisme ---
        SvtGlossaryEntry(
            id = "cellular_respiration",
            domain = SvtDomain.PHYSIOLOGY_IMMUNITY,
            term = LocalizedSvtText("Respiration cellulaire & ATP", "Cellular Respiration & ATP", "التنفس الخلوي وجزيئة ATP"),
            definition = LocalizedSvtText(
                fr = "Dégradation complète du glucose en présence de O₂ dans le hyaloplasme et la mitochondrie, libérant 36 à 38 molécules d'ATP utilisables par la cellule (C₆H₁₂O₆ + 6 O₂ → 6 CO₂ + 6 H₂O + ATP).",
                en = "Complete breakdown of glucose with O₂ in the cytosol and mitochondria, yielding 36–38 ATP molecules for cellular work (C₆H₁₂O₆ + 6 O₂ → 6 CO₂ + 6 H₂O + ATP).",
                ar = "هدم كلي للكليكوز بوجود O₂ في الجبلة الشفافة والميتوكوندريا، محرراً 36 إلى 38 جزيئة ATP وCO₂ والماء."
            ),
            keyHint = LocalizedSvtText(
                fr = "Glycolyse (hyaloplasme) → Cycle de Krebs (matrice) → Chaîne respiratoire (crêtes)",
                en = "Glycolysis (cytosol) → Krebs cycle (matrix) → Electron transport chain (cristae)",
                ar = "انحلال الكليكوز ← دورة كريبس ← السلسلة التنفسية في الأعراف"
            ),
            keywords = listOf("respiration", "atp", "glucose", "mitochondrie", "glycolyse", "krebs", "metabolisme", "التنفس الخلوي")
        ),
        SvtGlossaryEntry(
            id = "fermentation",
            domain = SvtDomain.PHYSIOLOGY_IMMUNITY,
            term = LocalizedSvtText("Fermentation lactique & alcoolique", "Lactic & Alcoholic Fermentation", "التخمر اللبني والكحولي"),
            definition = LocalizedSvtText(
                fr = "Dégradation partielle du glucose en milieu anaérobie (sans O₂) dans le hyaloplasme, produisant seulement 2 ATP et un résidu organique énergétique (acide lactique ou éthanol + CO₂).",
                en = "Partial anaerobic (oxygen-free) breakdown of glucose in the cytosol yielding only 2 ATP plus energy-rich organic byproducts (lactic acid or ethanol + CO₂).",
                ar = "هدم جزئي للكليكوز في غياب O₂ داخل الجبلة الشفافة، ينتج 2 ATP فقط مع حثالة عضوية (حمض لبني أو كحول إيثيلي)."
            ),
            keyHint = LocalizedSvtText(
                fr = "Muscle en effort intense : fermentation lactique • Levures sans O₂ : alcoolique",
                en = "Hard-working muscle: lactic fermentation • Yeast without O₂: alcoholic",
                ar = "العضلة عند المجهود القوي: تخمر لبني • الخميرة: تخمر كحولي"
            ),
            keywords = listOf("fermentation", "anaerobie", "lactique", "alcoolique", "hyaloplasme", "التخمر")
        ),
        SvtGlossaryEntry(
            id = "glycemia",
            domain = SvtDomain.PHYSIOLOGY_IMMUNITY,
            term = LocalizedSvtText("Glycémie, Insuline & Glucagon", "Glycemia, Insulin & Glucagon", "تحلون الدم والأنسولين والغلوكاغون"),
            definition = LocalizedSvtText(
                fr = "Régulation hormonale du taux de glucose sanguin (~1 g/L) par le pancréas : l'insuline (cellules β) abaisse la glycémie ; le glucagon (cellules α) l'élève par glycogénolyse hépatique.",
                en = "Hormonal regulation of blood glucose (~1 g/L) by the pancreas: insulin (β cells) lowers blood sugar; glucagon (α cells) raises it via liver glycogen breakdown.",
                ar = "تنظيم هرموني لنسبة السكر في الدم (~1 غ/ل) بواسطة البنكرياس: يخفض الأنسولين تحلون الدم بينما يرفعه الغلوكاغون."
            ),
            keyHint = LocalizedSvtText(
                fr = "Insuline = hypoglycémiante (stockage) • Glucagon = hyperglycémiant (libération)",
                en = "Insulin = glucose storage (lowers blood sugar) • Glucagon = glucose release",
                ar = "الأنسولين مخفض لتحلون الدم • الغلوكاغون رافع لتحلون الدم"
            ),
            keywords = listOf("glycemie", "insuline", "glucagon", "pancreas", "diabete", "glycogene", "تحلون الدم", "الأنسولين")
        ),
        SvtGlossaryEntry(
            id = "nephron",
            domain = SvtDomain.PHYSIOLOGY_IMMUNITY,
            term = LocalizedSvtText("Néphron & Filtration rénale", "Nephron & Kidney Filtration", "النفرون والترشيح الكلوي"),
            definition = LocalizedSvtText(
                fr = "Unité structurale et fonctionnelle du rein qui filtre le plasma sanguin au niveau du glomérule, réabsorbe les substances utiles (glucose, eau, sels) et forme l'urine définitive.",
                en = "Structural and functional unit of the kidney that filters blood plasma at the glomerulus, reabsorbs vital solutes (glucose, water, ions), and produces urine.",
                ar = "الوحدة الوظيفية للكلية التي ترشح البلازما في الكبيبة وتعيد امتصاص المواد النافعة (كليكوز، ماء، أملاح) لتكوين البول."
            ),
            keyHint = LocalizedSvtText(
                fr = "Filtration glomérulaire → Réabsorption tubulaire → Sécrétion",
                en = "Glomerular filtration → Tubular reabsorption → Secretion",
                ar = "الترشيح الكبيبي ← إعادة الامتصاص ← الإفراز الأنبوبي"
            ),
            keywords = listOf("nephron", "rein", "urine", "glomerule", "filtration", "reabsorption", "النفرون", "الكلية")
        ),

        // --- Système nerveux & Muscle ---
        SvtGlossaryEntry(
            id = "synapse",
            domain = SvtDomain.NERVOUS_MUSCULAR,
            term = LocalizedSvtText("Synapse chimique", "Chemical Synapse", "السينابس الكيميائي"),
            definition = LocalizedSvtText(
                fr = "Zone de jonction entre deux neurones où le potentiel d'action déclenche la libération d'un neurotransmetteur (ex : acétylcholine) dans la fente synaptique.",
                en = "Junction between two neurons where an action potential triggers neurotransmitter release (e.g. acetylcholine) across the synaptic cleft.",
                ar = "منطقة ارتباط بين عصبونين حيث يسبب جهد العمل تحرير مبلغ عصبي (مثل الأسيتيل كولين) في الحيز السينابسي."
            ),
            keyHint = LocalizedSvtText(
                fr = "Entrée de Ca²⁺ → Exocytose des vésicules → Récepteurs post-synaptiques",
                en = "Ca²⁺ influx → Vesicle exocytosis → Post-synaptic receptors",
                ar = "دخول أيونات الكالسيوم ← إخراج خلوي للحويصلات ← مستقبلات بعد سينابسية"
            ),
            keywords = listOf("synapse", "neurone", "neurotransmetteur", "acetylcholine", "السينابس", "العصبون")
        ),
        SvtGlossaryEntry(
            id = "action_potential",
            domain = SvtDomain.NERVOUS_MUSCULAR,
            term = LocalizedSvtText("Potentiel d'action & Influx nerveux", "Action Potential & Nerve Impulse", "جهد العمل والسيالة العصبية"),
            definition = LocalizedSvtText(
                fr = "Variation brève et inversée du potentiel membranaire d'un neurone (-70 mV → +30 mV) obéissant à la loi du tout ou rien, qui se propage sans atténuation le long de l'axone.",
                en = "Brief, rapid reversal of neuronal membrane voltage (-70 mV → +30 mV) following the all-or-none law that propagates unattenuated along the axon.",
                ar = "تغير سريع ومنعكس لجهد الغشاء العصبي (-70mV ← +30mV) يخضع لقانون الكل أو العدم وينتشر دون خمود على طول المحور العصبي."
            ),
            keyHint = LocalizedSvtText(
                fr = "Dépolarisation (entrée Na⁺) → Repolarisation (sortie K⁺) → Hyperpolarisation",
                en = "Depolarization (Na⁺ influx) → Repolarization (K⁺ efflux) → Hyperpolarization",
                ar = "إزالة الاستقطاب (دخول Na⁺) ← إعادة الاستقطاب (خروج K⁺)"
            ),
            keywords = listOf("potentiel d action", "influx nerveux", "axone", "myeline", "depolarisation", "جهد العمل", "السيالة العصبية")
        ),
        SvtGlossaryEntry(
            id = "myotatic_reflex",
            domain = SvtDomain.NERVOUS_MUSCULAR,
            term = LocalizedSvtText("Réflexe myotatique & Arc réflexe", "Myotatic Reflex & Reflex Arc", "الانعكاس العضلي وقوس الانعكاس"),
            definition = LocalizedSvtText(
                fr = "Contraction involontaire d'un muscle en réponse à son propre étirement, assurant le maintien de la posture via un circuit nerveux médullaire (récepteur → neurone sensitif → motoneurone → muscle).",
                en = "Involuntary muscle contraction in response to its own stretching, maintaining posture via a spinal neural circuit (receptor → sensory neuron → motor neuron → muscle).",
                ar = "تقلص لاإرادي لعضلة استجابةً لتمددها، يحافظ على التوازن عبر مسار عصبي شوكي (مستقبل ← عصبون حسي ← عصبون حركي ← عضلة)."
            ),
            keyHint = LocalizedSvtText(
                fr = "Fuseau neuromusculaire → Moelle épinière (monosynaptique) → Plaque motrice",
                en = "Muscle spindle → Spinal cord (monosynaptic) → Neuromuscular junction",
                ar = "المغزل العصبي العضلي ← النخاع الشوكي ← الصفيحة المحركة"
            ),
            keywords = listOf("reflexe", "myotatique", "arc reflexe", "moelle epiniere", "motoneurone", "الانعكاس العضلي")
        ),
        SvtGlossaryEntry(
            id = "sarcomere",
            domain = SvtDomain.NERVOUS_MUSCULAR,
            term = LocalizedSvtText("Sarcomère & Contraction musculaire", "Sarcomere & Muscle Contraction", "الساركومير والتقلص العضلي"),
            definition = LocalizedSvtText(
                fr = "Unité fonctionnelle de la myofibrille délimitée par deux stries Z, où glissent les filaments d'actine et de myosine grâce à l'ATP et aux ions Ca²⁺.",
                en = "Functional unit of a myofibril between two Z-discs where actin and myosin filaments slide past each other using ATP and Ca²⁺.",
                ar = "الوحدة الوظيفية للييف العضلي بين حزي Z حيث تنزلق خييطات الأكتين والميوزين باستعمال ATP وأيونات Ca²⁺."
            ),
            keyHint = LocalizedSvtText(
                fr = "Ca²⁺ libéré par le réticulum sarcoplasmique + hydrolyse d'ATP",
                en = "Ca²⁺ released from sarcoplasmic reticulum + ATP hydrolysis",
                ar = "تحرير Ca²⁺ من الشبكة الساركوبلازمية + حلمأة جزيئات ATP"
            ),
            keywords = listOf("sarcomere", "muscle", "contraction", "actine", "myosine", "myofibrille", "الساركومير", "التقلص العضلي")
        ),

        // --- Géologie ---
        SvtGlossaryEntry(
            id = "subduction",
            domain = SvtDomain.GEOLOGY,
            term = LocalizedSvtText("Subduction", "Subduction", "الطمر"),
            definition = LocalizedSvtText(
                fr = "Enfoncement d'une plaque lithosphérique océanique dense dans l'asthénosphère sous une plaque moins dense.",
                en = "Sinking of a denser oceanic lithospheric plate into the asthenosphere beneath a less dense plate.",
                ar = "انغراز صفيحة صخرية محيطية أكثر كثافة تحت صفيحة أقل كثافة داخل الأستينوسفير."
            ),
            keyHint = LocalizedSvtText(
                fr = "Fosse océanique, plan de Wadati-Benioff, volcanisme andésitique",
                en = "Oceanic trench, Wadati-Benioff zone, andesitic volcanism",
                ar = "حفرة محيطية، مستوى بينيوف، بركانية أنديزيتية"
            ),
            keywords = listOf("subduction", "plaque", "tectonique", "andesite", "benioff", "الطمر", "الصفائح")
        ),
        SvtGlossaryEntry(
            id = "metamorphism",
            domain = SvtDomain.GEOLOGY,
            term = LocalizedSvtText("Métamorphisme & Anatexie", "Metamorphism & Anatexis", "التحول والأناتيكسي"),
            definition = LocalizedSvtText(
                fr = "Transformation minéralogique et structurale d'une roche à l'état solide sous l'effet de la pression et de la température.",
                en = "Solid-state mineralogical and textural transformation of rocks caused by increased pressure and temperature.",
                ar = "تغير معادن وبنية الصخور في الحالة الصلبة تحت تأثير ارتفاع الضغط ودرجة الحرارة."
            ),
            keyHint = LocalizedSvtText(
                fr = "Argile → Schiste → Micaschiste → Gneiss → Migmatite (fusion partielle)",
                en = "Clay → Slate/Schist → Micaschist → Gneiss → Migmatite (partial melting)",
                ar = "طين → شيست → ميكاشيست → غنيس → ميغماتيت (انصهار جزئي)"
            ),
            keywords = listOf("metamorphisme", "anatexie", "gneiss", "schiste", "migmatite", "roche", "التحول")
        ),
        SvtGlossaryEntry(
            id = "obduction_collision",
            domain = SvtDomain.GEOLOGY,
            term = LocalizedSvtText("Obduction & Collision continentale", "Obduction & Continental Collision", "الطفو والاصطدام القاري"),
            definition = LocalizedSvtText(
                fr = "Chevauchement d'une lithosphère océanique sur un continent (ophiolites), puis affrontement de deux croûtes continentales formant une chaîne de montagnes.",
                en = "Thrusting of oceanic lithosphere onto a continent (ophiolites), followed by collision of two continental crusts building mountain belts.",
                ar = "زحف غلاف صخري محيطي فوق قارة (أوفيوليت) ثم تجابه قشرتين قاريتين وتكون سلاسل الاصطدام."
            ),
            keyHint = LocalizedSvtText(
                fr = "Plis, failles inverses, nappes de charriage et racine crustale",
                en = "Folds, reverse faults, thrust nappes, and crustal root",
                ar = "طيات، فوالق معكوسة، سدائم وجذر قشري"
            ),
            keywords = listOf("obduction", "collision", "ophiolite", "orogenese", "montagne", "faille", "الطفو", "الاصطدام")
        ),
        SvtGlossaryEntry(
            id = "seismograph",
            domain = SvtDomain.GEOLOGY,
            term = LocalizedSvtText("Ondes sismiques (P, S, L) & Moho", "Seismic Waves (P, S, L) & Moho", "الموجات الزلزالية (P, S, L) وانقطاع موهو"),
            definition = LocalizedSvtText(
                fr = "Vibrations propagées depuis le foyer d'un séisme. Les ondes P sont les plus rapides ; les ondes S ne traversent pas les liquides ; leurs vitesses révèlent les discontinuités (Moho, Gutenberg).",
                en = "Vibrations radiating from a quake focus. P-waves are fastest; S-waves cannot travel through liquids; wave speeds reveal Earth's discontinuities (Moho, Gutenberg).",
                ar = "اهتزازات تنتشر من البؤرة الزلزالية. الموجات P أسرعها، بينما لا تخترق الموجات S الأوساط السائلة وتكشف سرعاتها الانقطاعات (موهو، غوتنبرغ)."
            ),
            keyHint = LocalizedSvtText(
                fr = "Discontinuités : Moho (30 km), Gutenberg (2900 km), Lehmann (5150 km)",
                en = "Discontinuities: Moho (30 km), Gutenberg (2900 km), Lehmann (5150 km)",
                ar = "الانقطاعات: موهو (30 كلم)، غوتنبرغ (2900 كلم)، ليمان (5150 كلم)"
            ),
            keywords = listOf("seisme", "ondes sismiques", "moho", "gutenberg", "lehmann", "seismograph", "الزلزال", "موهو")
        ),

        // --- Écologie ---
        SvtGlossaryEntry(
            id = "photosynthesis_entry",
            domain = SvtDomain.ECOLOGY,
            term = LocalizedSvtText("Photosynthèse", "Photosynthesis", "التركيب الضوئي"),
            definition = LocalizedSvtText(
                fr = "Synthèse de matière organique (glucose) par les végétaux chlorophylliens à partir de CO₂, d'H₂O et d'énergie lumineuse.",
                en = "Synthesis of organic matter (glucose) by chlorophyllous plants from CO₂, H₂O, and light energy.",
                ar = "إنتاج المادة العضوية (الكليكوز) لدى النباتات اليخضورية انطلاقاً من CO₂ والماء والطاقة الضوئية."
            ),
            keyHint = LocalizedSvtText(
                fr = "6 CO₂ + 6 H₂O + Lumière → C₆H₁₂O₆ + 6 O₂",
                en = "6 CO₂ + 6 H₂O + Light → C₆H₁₂O₆ + 6 O₂",
                ar = "6 CO₂ + 6 H₂O + طاقة ضوئية ← C₆H₁₂O₆ + 6 O₂"
            ),
            keywords = listOf("photosynthese", "chlorophylle", "chloroplaste", "photosynthesis", "التركيب الضوئي")
        ),
        SvtGlossaryEntry(
            id = "trophic_chain",
            domain = SvtDomain.ECOLOGY,
            term = LocalizedSvtText("Chaîne trophique & Écosystème", "Food Chain & Ecosystem", "السلسلة الغذائية والحميلة البيئية"),
            definition = LocalizedSvtText(
                fr = "Ensemble formé par un biotope et sa biocénose, où s'effectue un transfert de matière et d'énergie entre producteurs, consommateurs et décomposeurs.",
                en = "System formed by a biotope and biocenosis involving matter and energy transfer across producers, consumers, and decomposers.",
                ar = "مجموع مكون من المحيا والعشيرة الإحيائية حيث تنتقل المادة والطاقة بين المنتجين والمستهلكين والمحللات."
            ),
            keyHint = LocalizedSvtText(
                fr = "Environ 10 % seulement de l'énergie passe d'un niveau trophique au suivant",
                en = "Only ~10% of energy transfers from one trophic level to the next",
                ar = "ينتقل نحو 10% فقط من الطاقة من مستوى غذائي إلى الذي يليه"
            ),
            keywords = listOf("ecosysteme", "chaine trophique", "biotope", "biocenose", "producteur", "ecologie", "الحميلة البيئية", "البيئة")
        ),
        SvtGlossaryEntry(
            id = "carbon_cycle",
            domain = SvtDomain.ECOLOGY,
            term = LocalizedSvtText("Cycle du carbone & Effet de serre", "Carbon Cycle & Greenhouse Effect", "دورة الكربون والاحتباس الحراري"),
            definition = LocalizedSvtText(
                fr = "Échanges continus de carbone entre atmosphère, biosphère, hydrosphère et lithosphère régulés par la photosynthèse et la respiration.",
                en = "Continuous carbon exchange between atmosphere, biosphere, hydrosphere, and lithosphere driven by photosynthesis and respiration.",
                ar = "تبادلات مستمرة للكربون بين الغلاف الجوي والمحيط الحيوي والمائي والصخري عبر التركيب الضوئي والتنفس."
            ),
            keyHint = LocalizedSvtText(
                fr = "Puits de carbone : océans, forêts, roches carbonatées",
                en = "Carbon sinks: oceans, forests, carbonate rocks",
                ar = "خزانات الكربون: المحيطات، الغابات والصخور الكلسية"
            ),
            keywords = listOf("carbone", "effet de serre", "co2", "climat", "eutrophisation", "دورة الكربون", "الاحتباس الحراري")
        )
    )
}
