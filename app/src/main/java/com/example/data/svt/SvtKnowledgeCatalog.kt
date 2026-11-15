package com.example.data.svt

import androidx.annotation.StringRes
import com.example.R
import com.example.core.AppLanguage

data class LocalizedSvtText(
    val fr: String,
    val en: String,
    val ar: String
) {
    fun get(language: AppLanguage): String = when (language) {
        AppLanguage.FR -> fr
        AppLanguage.EN -> en
        AppLanguage.AR -> ar
    }
}

enum class SvtDomain(@StringRes val labelRes: Int) {
    CELL_GENETICS(R.string.svt_domain_genetics),
    GEOLOGY(R.string.svt_domain_geology),
    PHYSIOLOGY_IMMUNITY(R.string.svt_domain_physiology),
    ECOLOGY(R.string.svt_domain_ecology)
}

data class SvtGlossaryEntry(
    val id: String,
    val domain: SvtDomain,
    val term: LocalizedSvtText,
    val definition: LocalizedSvtText,
    val keyHint: LocalizedSvtText
)

data class SvtFlashcard(
    val id: String,
    val domain: SvtDomain,
    val question: LocalizedSvtText,
    val answer: LocalizedSvtText,
    val explanation: LocalizedSvtText
)

data class SvtFactOfTheDay(
    val id: String,
    val domain: SvtDomain,
    val title: LocalizedSvtText,
    val body: LocalizedSvtText
)

data class SvtGeologicalEra(
    val id: String,
    val name: LocalizedSvtText,
    val span: LocalizedSvtText,
    val highlights: LocalizedSvtText
)

data class SvtRockFamily(
    val id: String,
    val name: LocalizedSvtText,
    val origin: LocalizedSvtText,
    val examples: LocalizedSvtText
)

data class SvtDnaPreset(
    val id: String,
    val label: LocalizedSvtText,
    val templateDna: String
)

object SvtKnowledgeCatalog {

    val chapterPresets: List<LocalizedSvtText> = listOf(
        LocalizedSvtText(
            fr = "Mitose & Méiose",
            en = "Mitosis & Meiosis",
            ar = "الانقسام غير المباشر والاختزالي"
        ),
        LocalizedSvtText(
            fr = "Expression de l'information génétique (ADN → Protéine)",
            en = "Gene Expression (DNA → Protein)",
            ar = "تعبير الخبر الوراثي (ADN → بروتين)"
        ),
        LocalizedSvtText(
            fr = "Tectonique des plaques & Séismes",
            en = "Plate Tectonics & Earthquakes",
            ar = "تكتونية الصفائح والزلازل"
        ),
        LocalizedSvtText(
            fr = "Réponse immunitaire & Anticorps",
            en = "Immune Response & Antibodies",
            ar = "الاستجابة المناعية ومضادات الأجسام"
        ),
        LocalizedSvtText(
            fr = "Photosynthèse & Respiration cellulaire",
            en = "Photosynthesis & Cellular Respiration",
            ar = "التركيب الضوئي والتنفس الخلوي"
        ),
        LocalizedSvtText(
            fr = "Système nerveux & Réflexe myotatique",
            en = "Nervous System & Myotatic Reflex",
            ar = "الجهاز العصبي والانعكاس العضلي"
        )
    )

    val dnaPresets: List<SvtDnaPreset> = listOf(
        SvtDnaPreset(
            id = "insulin_start",
            label = LocalizedSvtText(
                fr = "Séquence standard (AUG → STOP)",
                en = "Standard sequence (AUG → STOP)",
                ar = "متتالية نموذجية (AUG → STOP)"
            ),
            templateDna = "TACAAAGTCGGTTTACCTATT"
        ),
        SvtDnaPreset(
            id = "hemoglobin_normal",
            label = LocalizedSvtText(
                fr = "Allèle HbA (Bêta-globine normale)",
                en = "HbA allele (Normal beta-globin)",
                ar = "الحليل HbA (خضاب دموي عادي)"
            ),
            templateDna = "CACGTGGACTGAGGACTCCTC"
        ),
        SvtDnaPreset(
            id = "hemoglobin_sickle",
            label = LocalizedSvtText(
                fr = "Allèle HbS (Mutation drépanocytose)",
                en = "HbS allele (Sickle-cell mutation)",
                ar = "الحليل HbS (طفرة فقر الدم المنجلي)"
            ),
            templateDna = "CACGTGGACTGAGGACACCTC"
        )
    )

    val factsOfTheDay: List<SvtFactOfTheDay> = listOf(
        SvtFactOfTheDay(
            id = "fact_dna",
            domain = SvtDomain.CELL_GENETICS,
            title = LocalizedSvtText(
                fr = "2 mètres d'ADN dans chaque cellule",
                en = "2 meters of DNA inside every cell",
                ar = "متران من ADN داخل كل خلية"
            ),
            body = LocalizedSvtText(
                fr = "Déplié, l'ADN d'une seule cellule humaine mesure environ 2 mètres, mais il s'enroule autour des histones pour tenir dans un noyau de 6 µm.",
                en = "Uncoiled, the DNA of a single human cell is about 2 meters long, tightly packed around histones inside a 6 µm nucleus.",
                ar = "إذا مُدّد ADN خلية بشرية واحدة فسيصل طوله إلى نحو مترين، لكنه يلتف بدقة حول الهيستونات داخل نواة قطرها 6 ميكرومتر."
            )
        ),
        SvtFactOfTheDay(
            id = "fact_subduction",
            domain = SvtDomain.GEOLOGY,
            title = LocalizedSvtText(
                fr = "Les plaques avancent comme vos ongles",
                en = "Tectonic plates move as fast as fingernails",
                ar = "تتحرك الصفائح بسرعة نمو الأظافر"
            ),
            body = LocalizedSvtText(
                fr = "Les plaques lithosphériques se déplacent de 2 à 10 cm par an : sur des millions d'années, cela ouvre des océans et soulève l'Himalaya.",
                en = "Lithospheric plates drift 2 to 10 cm per year — enough over millions of years to open oceans and raise the Himalayas.",
                ar = "تتحرك الصفائح الصخرية بمعدل 2 إلى 10 سنتيمترات في السنة، وهو ما يكفي عبر ملايين السنين لفتح المحيطات وتشكل السلاسل الجبلية."
            )
        ),
        SvtFactOfTheDay(
            id = "fact_atp",
            domain = SvtDomain.PHYSIOLOGY_IMMUNITY,
            title = LocalizedSvtText(
                fr = "Les mitochondries, centrales énergétiques",
                en = "Mitochondria, cellular powerhouses",
                ar = "الميتوكوندريات، محطات الطاقة الخلوية"
            ),
            body = LocalizedSvtText(
                fr = "Une molécule de glucose oxydée lors de la respiration cellulaire produit jusqu'à 36–38 ATP, contre seulement 2 ATP en fermentation.",
                en = "Complete oxidation of one glucose molecule via cellular respiration yields up to 36–38 ATP, versus only 2 ATP in fermentation.",
                ar = "تُنتج أكسدة جزيئة واحدة من الكليكوز خلال التنفس الخلوي ما يصل إلى 36–38 جزيئة ATP مقابل جزيئتين فقط في التخمر."
            )
        ),
        SvtFactOfTheDay(
            id = "fact_photosynthesis",
            domain = SvtDomain.ECOLOGY,
            title = LocalizedSvtText(
                fr = "L'oxygène provient de la photolyse de l'eau",
                en = "Oxygen comes from water photolysis",
                ar = "الأكسجين ناتج عن التحليل الضوئي للماء"
            ),
            body = LocalizedSvtText(
                fr = "Dans les thylakoïdes du chloroplaste, la lumière casse la molécule H₂O pour libérer O₂, tandis que le CO₂ est fixé dans le stroma.",
                en = "Inside chloroplast thylakoids, light splits H₂O to release O₂, while CO₂ is fixed into sugars in the stroma.",
                ar = "داخل تيلاكويدات البلاستيدة الخضراء، يفكك الضوء جزيئة الماء لتحرير ثنائي الأكسجين بينما يُثبَّت CO₂ في الستروما."
            )
        ),
        SvtFactOfTheDay(
            id = "fact_synapse",
            domain = SvtDomain.PHYSIOLOGY_IMMUNITY,
            title = LocalizedSvtText(
                fr = "L'influx nerveux voyage jusqu'à 120 m/s",
                en = "Nerve impulses travel up to 120 m/s",
                ar = "تنتقل السيالة العصبية بسرعة تصل إلى 120 م/ث"
            ),
            body = LocalizedSvtText(
                fr = "Grâce à la gaine de myéline et à la conduction saltatoire entre les nœuds de Ranvier, le message nerveux parcourt un axone en quelques millisecondes.",
                en = "Thanks to the myelin sheath and saltatory conduction between nodes of Ranvier, signals cross axons in milliseconds.",
                ar = "بفضل غمد النخاعين والتوصيل القافز عبر عقد رانفيي، تعبر السيالة العصبية المحور العصبي في أجزاء من الثانية."
            )
        ),
        SvtFactOfTheDay(
            id = "fact_immunity",
            domain = SvtDomain.PHYSIOLOGY_IMMUNITY,
            title = LocalizedSvtText(
                fr = "La mémoire immunitaire protège durablement",
                en = "Immune memory gives lasting protection",
                ar = "الذاكرة المناعية توفر حماية طويلة الأمد"
            ),
            body = LocalizedSvtText(
                fr = "Lors d'un second contact avec le même antigène, les lymphocytes B et T mémoire déclenchent une réponse plus rapide, plus intense et plus longue.",
                en = "Upon re-exposure to the same antigen, memory B and T cells trigger a faster, stronger, and longer-lasting immune response.",
                ar = "عند الاتصال الثاني بنفس مولد المضاد، تطلق اللمفاويات الذاكرة استجابة أسرع وأقوى وأطول مدة."
            )
        )
    )

    val glossaryEntries: List<SvtGlossaryEntry> = listOf(
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
            )
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
            )
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
            )
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
            )
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
            )
        ),
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
            )
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
            )
        ),
        SvtGlossaryEntry(
            id = "obduction_collision",
            domain = SvtDomain.GEOLOGY,
            term = LocalizedSvtText("Obduction & Collision continentale", "Obduction & Continental Collision", "الطفو والاصطدام القاري"),
            definition = LocalizedSvtText(
                fr = "Chevauchement d'une lithosphère océanique sur un continent (ophiolites), puis affrontement de deux croûtes continentales.",
                en = "Thrusting of oceanic lithosphere onto a continent (ophiolites), followed by collision of two continental crusts.",
                ar = "زحف غلاف صخري محيطي فوق قارة (أوفيوليت) ثم تجابه قشرتين قاريتين وتكون سلاسل الاصطدام."
            ),
            keyHint = LocalizedSvtText(
                fr = "Plis, failles inverses, nappes de charriage et racine crustale",
                en = "Folds, reverse faults, thrust nappes, and crustal root",
                ar = "طيات، فوالق معكوسة، سدائم وجذر قشري"
            )
        ),
        SvtGlossaryEntry(
            id = "seismograph",
            domain = SvtDomain.GEOLOGY,
            term = LocalizedSvtText("Ondes sismiques (P, S, L)", "Seismic Waves (P, S, L)", "الموجات الزلزالية (P, S, L)"),
            definition = LocalizedSvtText(
                fr = "Vibrations propagées depuis le foyer d'un séisme. Les ondes P sont les plus rapides ; les ondes S ne traversent pas les liquides.",
                en = "Vibrations radiating from a quake focus. P-waves are fastest; S-waves cannot travel through liquids.",
                ar = "اهتزازات تنتشر من البؤرة الزلزالية. الموجات P أسرعها، بينما لا تخترق الموجات S الأوساط السائلة."
            ),
            keyHint = LocalizedSvtText(
                fr = "Discontinuités : Moho (30 km), Gutenberg (2900 km), Lehmann (5150 km)",
                en = "Discontinuities: Moho (30 km), Gutenberg (2900 km), Lehmann (5150 km)",
                ar = "الانقطاعات: موهو (30 كلم)، غوتنبرغ (2900 كلم)، ليمان (5150 كلم)"
            )
        ),
        SvtGlossaryEntry(
            id = "synapse",
            domain = SvtDomain.PHYSIOLOGY_IMMUNITY,
            term = LocalizedSvtText("Synapse chimique", "Chemical Synapse", "السينابس الكيميائي"),
            definition = LocalizedSvtText(
                fr = "Zone de jonction entre deux neurones où le potentiel d'action déclenche la libération d'un neurotransmetteur (ex : acétylcholine).",
                en = "Junction between two neurons where an action potential triggers neurotransmitter release across the synaptic cleft.",
                ar = "منطقة ارتباط بين عصبونين حيث يسبب جهد العمل تحرير مبلغ عصبي في الحيز السينابسي."
            ),
            keyHint = LocalizedSvtText(
                fr = "Entrée de Ca²⁺ → Exocytose des vésicules → Récepteurs post-synaptiques",
                en = "Ca²⁺ influx → Vesicle exocytosis → Post-synaptic receptors",
                ar = "دخول أيونات الكالسيوم ← إخراج خلوي للحويصلات ← مستقبلات بعد سينابسية"
            )
        ),
        SvtGlossaryEntry(
            id = "immunity_humoral",
            domain = SvtDomain.PHYSIOLOGY_IMMUNITY,
            term = LocalizedSvtText("Immunité humorale & cellulaire", "Humoral & Cell-Mediated Immunity", "المناعة الخلطية والخلوية"),
            definition = LocalizedSvtText(
                fr = "La voie humorale repose sur les lymphocytes B et les anticorps ; la voie cellulaire implique les lymphocytes T cytotoxiques (LTc).",
                en = "Humoral immunity relies on B cells and antibodies; cell-mediated immunity uses cytotoxic T cells (Tc).",
                ar = "تعتمد المناعة الخلطية على اللمفاويات B ومضادات الأجسام، بينما تعتمد الخلوية على اللمفاويات T القاتلة."
            ),
            keyHint = LocalizedSvtText(
                fr = "LT4 auxiliaires (interleukines) coordonnent les deux réponses spécifiques",
                en = "Helper T4 cells (interleukins) coordinate both specific responses",
                ar = "تنسق اللمفاويات المساعدة LT4 الاستجابتين عبر الأنترلوكينات"
            )
        ),
        SvtGlossaryEntry(
            id = "sarcomere",
            domain = SvtDomain.PHYSIOLOGY_IMMUNITY,
            term = LocalizedSvtText("Sarcomère & Contraction musculaire", "Sarcomere & Muscle Contraction", "الساركومير والتقلص العضلي"),
            definition = LocalizedSvtText(
                fr = "Unité fonctionnelle de la myofibrille délimitée par deux stries Z, où glissent les filaments d'actine et de myosine grâce à l'ATP.",
                en = "Functional unit of a myofibril between two Z-discs where actin and myosin filaments slide using ATP.",
                ar = "الوحدة الوظيفية للييف العضلي بين حزي Z حيث تنزلق خييطات الأكتين والميوزين باستعمال ATP."
            ),
            keyHint = LocalizedSvtText(
                fr = "Ca²⁺ libéré par le réticulum sarcoplasmique + hydrolyse d'ATP",
                en = "Ca²⁺ released from sarcoplasmic reticulum + ATP hydrolysis",
                ar = "تحرير Ca²⁺ من الشبكة الساركوبلازمية + حلمأة جزيئات ATP"
            )
        ),
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
            )
        ),
        SvtGlossaryEntry(
            id = "trophic_chain",
            domain = SvtDomain.ECOLOGY,
            term = LocalizedSvtText("Chaîne trophique & Écosystème", "Food Chain & Ecosystem", "السلسلة الغذائية والحميلة البيئية"),
            definition = LocalizedSvtText(
                fr = "Transfert de matière et d'énergie entre producteurs primaires (autotrophes), consommateurs et décomposeurs d'un biotope.",
                en = "Transfer of matter and energy across primary producers (autotrophs), consumers, and decomposers in a biotope.",
                ar = "انتقال المادة والطاقة بين المنتجين الأوليين والمستهلكين والمحللات داخل وسط بيئي."
            ),
            keyHint = LocalizedSvtText(
                fr = "Environ 10 % seulement de l'énergie passe d'un niveau trophique au suivant",
                en = "Only ~10% of energy transfers from one trophic level to the next",
                ar = "ينتقل نحو 10% فقط من الطاقة من مستوى غذائي إلى الذي يليه"
            )
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
            )
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
            )
        )
    )

    val flashcards: List<SvtFlashcard> = listOf(
        SvtFlashcard(
            id = "fc_1",
            domain = SvtDomain.CELL_GENETICS,
            question = LocalizedSvtText(
                fr = "À quelle phase de la mitose les chromatides sœurs se séparent-elles ?",
                en = "During which phase of mitosis do sister chromatids separate?",
                ar = "في أي طور من الانقسام غير المباشر تنفصل الصبيغيات الشقيقة؟"
            ),
            answer = LocalizedSvtText("L'Anaphase", "Anaphase", "الطور الانفصالي (Anaphase)"),
            explanation = LocalizedSvtText(
                fr = "Le clivage des centromères permet la migration polaire des chromatides vers les pôles opposés de la cellule.",
                en = "Centromere splitting allows chromatids to migrate toward opposite cell poles.",
                ar = "انشطار الجزيئات المركزية يتيح هجرة الصبيغيات نحو قطبي الخلية المتقابلين."
            )
        ),
        SvtFlashcard(
            id = "fc_2",
            domain = SvtDomain.CELL_GENETICS,
            question = LocalizedSvtText(
                fr = "Quelle base azotée remplace la Thymine (T) dans l'ARNm ?",
                en = "Which nitrogenous base replaces Thymine (T) in mRNA?",
                ar = "ما القاعدة الأزوتية التي تعوّض التيمين (T) في ARNm؟"
            ),
            answer = LocalizedSvtText("L'Uracile (U)", "Uracil (U)", "الأوراسيل (U)"),
            explanation = LocalizedSvtText(
                fr = "Dans l'ARN messager, l'Adénine (A) de l'ADN transcrit s'apparie avec l'Uracile (U).",
                en = "In mRNA, Adenine (A) on the DNA template pairs with Uracil (U).",
                ar = "أثناء النسخ، تتكامل قاعدة الأدينين (A) على الخيط المنسوخ مع الأوراسيل (U)."
            )
        ),
        SvtFlashcard(
            id = "fc_3",
            domain = SvtDomain.CELL_GENETICS,
            question = LocalizedSvtText(
                fr = "Quel phénomène en Prophase I de la méiose explique le brassage intrachromosomique ?",
                en = "Which event in Prophase I of meiosis causes intrachromosomal recombination?",
                ar = "ما الظاهرة التي تحدث في التمهيدية I وتحقق التخليط الضمصبغي؟"
            ),
            answer = LocalizedSvtText(
                fr = "Le Crossing-over (enjambement)",
                en = "Crossing-over",
                ar = "العبور الصبغي (Crossing-over)"
            ),
            explanation = LocalizedSvtText(
                fr = "Les chromosomes homologues appariés forment des chiasmas et échangent des segments de chromatides.",
                en = "Paired homologous chromosomes form chiasmata and exchange chromatid segments.",
                ar = "تتبادل الصبغيات المتماثلة أجزاءً من صبيغياتها على مستوى التقاطعات."
            )
        ),
        SvtFlashcard(
            id = "fc_4",
            domain = SvtDomain.CELL_GENETICS,
            question = LocalizedSvtText(
                fr = "Quel est le rôle du codon AUG sur l'ARNm ?",
                en = "What is the role of the AUG codon on mRNA?",
                ar = "ما دور الوحدة الرمزية AUG على جزيئة ARNm؟"
            ),
            answer = LocalizedSvtText(
                fr = "Codon initiateur (Méthionine)",
                en = "Start codon (Methionine)",
                ar = "وحدة البداية (تُرمّز للميثيونين Met)"
            ),
            explanation = LocalizedSvtText(
                fr = "La traduction commence toujours au codon AUG et s'achève aux codons STOP (UAA, UAG ou UGA).",
                en = "Translation begins at AUG and ends when a STOP codon (UAA, UAG, UGA) is reached.",
                ar = "تبدأ الترجمة دائماً عند الكودون AUG وتتوقف عند أحد كودونات التوقف (UAA, UAG, UGA)."
            )
        ),
        SvtFlashcard(
            id = "fc_5",
            domain = SvtDomain.GEOLOGY,
            question = LocalizedSvtText(
                fr = "Quelle discontinuité sépare la croûte terrestre du manteau supérieur ?",
                en = "Which discontinuity separates Earth's crust from the upper mantle?",
                ar = "ما الانقطاع الذي يفصل القشرة الأرضية عن الرداء العلوي؟"
            ),
            answer = LocalizedSvtText("La discontinuité de Moho", "The Moho discontinuity", "انقطاع موهو (Moho)"),
            explanation = LocalizedSvtText(
                fr = "Découverte par Mohorovičić, elle se situe vers 30 km sous les continents et 7–10 km sous les océans.",
                en = "Discovered by Mohorovičić, located around 30 km beneath continents and 7–10 km beneath oceans.",
                ar = "يقع على عمق نحو 30 كلم تحت القارات و7–10 كلم تحت المحيطات، وتتسارع عنده الموجات P."
            )
        ),
        SvtFlashcard(
            id = "fc_6",
            domain = SvtDomain.GEOLOGY,
            question = LocalizedSvtText(
                fr = "Pourquoi les ondes S disparaissent-elles à 2 900 km de profondeur ?",
                en = "Why do S-waves disappear at 2,900 km depth?",
                ar = "لماذا تختفي الموجات S على عمق 2900 كلم؟"
            ),
            answer = LocalizedSvtText(
                fr = "Le noyau externe est liquide",
                en = "The outer core is liquid",
                ar = "لأن النواة الخارجية سائلة"
            ),
            explanation = LocalizedSvtText(
                fr = "Les ondes de cisaillement S ne se propagent pas dans les milieux fluides (discontinuité de Gutenberg).",
                en = "Shear S-waves cannot propagate through fluids (Gutenberg discontinuity).",
                ar = "لا تنتشر موجات القص S في السوائل، مما يدل على الحالة السائلة للنواة الخارجية عند انقطاع غوتنبرغ."
            )
        ),
        SvtFlashcard(
            id = "fc_7",
            domain = SvtDomain.GEOLOGY,
            question = LocalizedSvtText(
                fr = "Quelle roche volcanique caractérise les zones de subduction ?",
                en = "Which volcanic rock characterizes subduction zones?",
                ar = "ما الصخرة البركانية المميزة لمناطق الطمر؟"
            ),
            answer = LocalizedSvtText("L'andésite", "Andesite", "الأنديزيت (Andésite)"),
            explanation = LocalizedSvtText(
                fr = "La déshydratation de la plaque plongeante provoque la fusion partielle du manteau et forme un magma andésitique.",
                en = "Dehydration of the subducting slab triggers partial melting of the mantle wedge, forming andesitic magma.",
                ar = "تحرر الصفيحة المنغرزة الماء مما يؤدي إلى انصهار جزئي للرداء وتكوّن صهارة أنديزيتية."
            )
        ),
        SvtFlashcard(
            id = "fc_8",
            domain = SvtDomain.GEOLOGY,
            question = LocalizedSvtText(
                fr = "Comment reconnaît-on un complexe ophiolitique sur un continent ?",
                en = "What indicates an ophiolite complex on a continent?",
                ar = "بماذا يتميز المركب الأوفيوليتي فوق القارة؟"
            ),
            answer = LocalizedSvtText(
                fr = "Péridotite + Gabbro + Basalte en coussins",
                en = "Peridotite + Gabbro + Pillow basalts",
                ar = "بيريدوتيت + غابرو + بازلت وسائدي"
            ),
            explanation = LocalizedSvtText(
                fr = "C'est un lambeau d'ancienne lithosphère océanique charrié sur le continent lors d'une obduction.",
                en = "It represents a slice of ancient oceanic lithosphere thrust onto a continent during obduction.",
                ar = "هو بقايا غلاف صخري محيطي قديم طفا فوق القارة أثناء ظاهرة الطفو."
            )
        ),
        SvtFlashcard(
            id = "fc_9",
            domain = SvtDomain.PHYSIOLOGY_IMMUNITY,
            question = LocalizedSvtText(
                fr = "Quelles cellules immunitaires sécrètent les anticorps ?",
                en = "Which immune cells secrete antibodies?",
                ar = "ما الخلايا المناعية التي تفرز مضادات الأجسام؟"
            ),
            answer = LocalizedSvtText(
                fr = "Les plasmocytes (issus des lymphocytes B)",
                en = "Plasma cells (derived from B lymphocytes)",
                ar = "البلزميات (الناتجة عن تفريق اللمفاويات B)"
            ),
            explanation = LocalizedSvtText(
                fr = "Après sélection clonale et stimulation par les LT4, les LB se différencient en plasmocytes sécréteurs et LB mémoire.",
                en = "After clonal selection and T4 help, B cells differentiate into antibody-secreting plasma cells and memory B cells.",
                ar = "بعد الانتقاء اللمي والتنشيط بواسطة LT4، تتحول اللمفاويات B إلى بلزميات منتجة لمضادات الأجسام."
            )
        ),
        SvtFlashcard(
            id = "fc_10",
            domain = SvtDomain.PHYSIOLOGY_IMMUNITY,
            question = LocalizedSvtText(
                fr = "Quelle molécule énergétique se fixe sur la tête de myosine lors de la contraction ?",
                en = "Which energy molecule binds to the myosin head during muscle contraction?",
                ar = "ما الجزيئة الطاقية التي ترتبط برأس الميوزين أثناء التقلص العضلي؟"
            ),
            answer = LocalizedSvtText("L'ATP", "ATP", "جزيئة ATP"),
            explanation = LocalizedSvtText(
                fr = "L'hydrolyse de l'ATP en ADP + Pi fournit l'énergie mécanique nécessaire au pivotement des têtes de myosine.",
                en = "Hydrolysis of ATP into ADP + Pi provides the mechanical energy for the myosin power stroke.",
                ar = "توفر حلمأة ATP إلى ADP + Pi الطاقة اللازمة لدوران رؤوس الميوزين وسحب خييطات الأكتين."
            )
        ),
        SvtFlashcard(
            id = "fc_11",
            domain = SvtDomain.PHYSIOLOGY_IMMUNITY,
            question = LocalizedSvtText(
                fr = "Quel organite cellulaire réalise le cycle de Krebs et la phosphorylation oxydative ?",
                en = "Which organelle performs the Krebs cycle and oxidative phosphorylation?",
                ar = "ما العضية الخلوية التي تحتضن دورة كريبس والتفسفر المؤكسد؟"
            ),
            answer = LocalizedSvtText("La mitochondrie", "The mitochondrion", "الميتوكوندريا"),
            explanation = LocalizedSvtText(
                fr = "La glycolyse a lieu dans le hyaloplasme, puis le pyruvate entre dans la matrice et les crêtes mitochondriales.",
                en = "Glycolysis occurs in the cytosol, then pyruvate enters the mitochondrial matrix and cristae.",
                ar = "يحدث انحلال الكليكوز في الجبلة الشفافة، ثم ينفذ حمض البيروفيك إلى الماتريس وأعراف الميتوكوندريا."
            )
        ),
        SvtFlashcard(
            id = "fc_12",
            domain = SvtDomain.ECOLOGY,
            question = LocalizedSvtText(
                fr = "À quoi correspond la DBO₅ lors de l'évaluation de la qualité d'une eau ?",
                en = "What does BOD₅ measure when assessing water quality?",
                ar = "ماذا يقيس مؤشر DBO₅ عند تقييم جودة المياه؟"
            ),
            answer = LocalizedSvtText(
                fr = "La Demande Biochimique en Oxygène sur 5 jours",
                en = "5-day Biochemical Oxygen Demand",
                ar = "الطلب البيوكيميائي للأكسجين خلال 5 أيام"
            ),
            explanation = LocalizedSvtText(
                fr = "Une DBO₅ élevée indique une forte pollution organique consommée par les micro-organismes aérobies.",
                en = "A high BOD₅ indicates heavy organic pollution consumed by aerobic microorganisms.",
                ar = "يدل ارتفاع DBO₅ على تلوث عضوي كبير تستهلكه المتعضيات المجهرية الهوائية."
            )
        )
    )

    val geologicalEras: List<SvtGeologicalEra> = listOf(
        SvtGeologicalEra(
            id = "precambrian",
            name = LocalizedSvtText("Précambrien", "Precambrian", "ما قبل الكمبري"),
            span = LocalizedSvtText("4 560 → 541 Ma", "4,560 → 541 Ma", "4560 ← 541 مليون سنة"),
            highlights = LocalizedSvtText(
                fr = "Apparition de la vie (bactéries, stromatolithes), oxygénation des océans et de l'atmosphère.",
                en = "Origin of life (prokaryotes, stromatolites), oxygenation of oceans and atmosphere.",
                ar = "ظهور الحياة (البكتيريا والستروماتوليت) وتزويد المحيطات والغلاف الجوي بالأكسجين."
            )
        ),
        SvtGeologicalEra(
            id = "paleozoic",
            name = LocalizedSvtText("Paléozoïque (Ère Primaire)", "Paleozoic Era", "الحقب الأول (الباليوزوي)"),
            span = LocalizedSvtText("541 → 252 Ma", "541 → 252 Ma", "541 ← 252 مليون سنة"),
            highlights = LocalizedSvtText(
                fr = "Trilobites, premiers vertébrés, sortie des eaux, forêts du Carbonifère, formation de la Pangée.",
                en = "Trilobites, early vertebrates, land colonization, Carboniferous forests, Pangaea assembly.",
                ar = "ثلاثية الفصوص، الفقاريات الأولى، غزو اليابسة، غابات الفحمي وتشكل قارة بانجيا."
            )
        ),
        SvtGeologicalEra(
            id = "mesozoic",
            name = LocalizedSvtText("Mésozoïque (Ère Secondaire)", "Mesozoic Era", "الحقب الثاني (الميزوزوي)"),
            span = LocalizedSvtText("252 → 66 Ma", "252 → 66 Ma", "252 ← 66 مليون سنة"),
            highlights = LocalizedSvtText(
                fr = "Ammonites, dinosaures, premiers oiseaux et plantes à fleurs, crise Crétacé–Paléogène (66 Ma).",
                en = "Ammonites, dinosaurs, early birds and flowering plants, K–Pg extinction (66 Ma).",
                ar = "الأمونيت، الديناصورات، أوائل الطيور والنباتات الزهرية، أزمة الطباشيري–الثلاثي (66 م.س)."
            )
        ),
        SvtGeologicalEra(
            id = "cenozoic",
            name = LocalizedSvtText("Cénozoïque (Ères Tertiaire & Quaternaire)", "Cenozoic Era", "الحقب الثالث والرابع (السينوزوي)"),
            span = LocalizedSvtText("66 Ma → Actuel", "66 Ma → Present", "66 مليون سنة ← الحاضر"),
            highlights = LocalizedSvtText(
                fr = "Diversification des mammifères, orogenèse alpine/atlasique, apparition de la lignée humaine.",
                en = "Diversification of mammals, Alpine/Atlas mountain building, emergence of hominins.",
                ar = "تنوع الثدييات، تشكل السلاسل الألبية والأطلسية وظهور السلالة البشرية."
            )
        )
    )

    val rockFamilies: List<SvtRockFamily> = listOf(
        SvtRockFamily(
            id = "igneous",
            name = LocalizedSvtText("Roches magmatiques", "Igneous Rocks", "صخور صهارية"),
            origin = LocalizedSvtText(
                fr = "Refroidissement lent en profondeur (plutoniques) ou rapide en surface (volcaniques).",
                en = "Slow cooling at depth (plutonic) or rapid cooling at the surface (volcanic).",
                ar = "تبريد بطيء في العمق (بلوتونية) أو سريع على السطح (بركانية)."
            ),
            examples = LocalizedSvtText(
                fr = "Granite (grenue), Gabbro, Basalte (microlithique), Andésite",
                en = "Granite (phaneritic), Gabbro, Basalt (microlitic), Andesite",
                ar = "غرانيت (محببة)، غابرو، بازلت (ميكروليتية)، أنديزيت"
            )
        ),
        SvtRockFamily(
            id = "sedimentary",
            name = LocalizedSvtText("Roches sédimentaires", "Sedimentary Rocks", "صخور رسوبية"),
            origin = LocalizedSvtText(
                fr = "Érosion, transport, dépôt en strates et diagenèse en surface (contiennent souvent des fossiles).",
                en = "Weathering, transport, layered deposition, and diagenesis (often contain fossils).",
                ar = "حت ونقل وترسب على شكل طبقات وتصخر (تحتوي غالباً على مستحاثات)."
            ),
            examples = LocalizedSvtText(
                fr = "Calcaire, Grès, Argile, Phosphates, Charbon",
                en = "Limestone, Sandstone, Shale/Clay, Phosphates, Coal",
                ar = "كلس، حجر رملي، طين، فوسفاط، فحم حجري"
            )
        ),
        SvtRockFamily(
            id = "metamorphic",
            name = LocalizedSvtText("Roches métamorphiques", "Metamorphic Rocks", "صخور متحولة"),
            origin = LocalizedSvtText(
                fr = "Recristallisation à l'état solide sous forte pression et haute température.",
                en = "Solid-state recrystallization under high pressure and temperature.",
                ar = "إعادة تبلور في الحالة الصلبة تحت ضغط وحرارة مرتفعين."
            ),
            examples = LocalizedSvtText(
                fr = "Schiste, Micaschiste, Gneiss, Marbre, Éclogite",
                en = "Slate/Schist, Micaschist, Gneiss, Marble, Eclogite",
                ar = "شيست، ميكاشيست، غنيس، رخام، إكلوجيت"
            )
        )
    )
}
