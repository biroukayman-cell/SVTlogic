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
    IMMUNITY(R.string.svt_domain_immunity),
    REPRODUCTION(R.string.svt_domain_reproduction),
    PHYSIOLOGY_IMMUNITY(R.string.svt_domain_physiology),
    NERVOUS_MUSCULAR(R.string.svt_domain_nervous),
    GEOLOGY(R.string.svt_domain_geology),
    ECOLOGY(R.string.svt_domain_ecology)
}

data class SvtGlossaryEntry(
    val id: String,
    val domain: SvtDomain,
    val term: LocalizedSvtText,
    val definition: LocalizedSvtText,
    val keyHint: LocalizedSvtText,
    val keywords: List<String> = emptyList()
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
            fr = "Réponse immunitaire & Vaccination",
            en = "Immune Response & Vaccination",
            ar = "الاستجابة المناعية والتلقيح"
        ),
        LocalizedSvtText(
            fr = "Reproduction humaine & Cycles hormonaux",
            en = "Human Reproduction & Hormonal Cycles",
            ar = "التوالد البشري والدورات الهرمونية"
        ),
        LocalizedSvtText(
            fr = "Tectonique des plaques & Séismes",
            en = "Plate Tectonics & Earthquakes",
            ar = "تكتونية الصفائح والزلازل"
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
            id = "fact_immunity",
            domain = SvtDomain.IMMUNITY,
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
        ),
        SvtFactOfTheDay(
            id = "fact_reproduction",
            domain = SvtDomain.REPRODUCTION,
            title = LocalizedSvtText(
                fr = "De deux gamètes haploïdes (n) à un organisme diploïde (2n)",
                en = "From two haploid gametes (n) to a diploid organism (2n)",
                ar = "من مشيجين أحاديي الصيغة (n) إلى كائن ثنائي الصيغة (2n)"
            ),
            body = LocalizedSvtText(
                fr = "La méiose divise par deux le nombre de chromosomes, puis la fécondation rétablit le caryotype de l'espèce (2n = 46 chez l'être humain) tout en créant une combinaison génétique unique.",
                en = "Meiosis halves the chromosome count, and fertilization restores the species karyotype (2n = 46 in humans) while generating a unique genetic combination.",
                ar = "يختزل الانقسام الاختزالي عدد الصبغيات إلى النصف، ثم يعيد الإخصاب الصيغة الصبغية للنوع (2n = 46) مع تركيب وراثي فريد."
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
            id = "fact_synapse",
            domain = SvtDomain.NERVOUS_MUSCULAR,
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
        )
    )

    val glossaryEntries: List<SvtGlossaryEntry> = SvtGlossaryCatalogData.allEntries

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
            id = "fc_imm_1",
            domain = SvtDomain.IMMUNITY,
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
            id = "fc_imm_2",
            domain = SvtDomain.IMMUNITY,
            question = LocalizedSvtText(
                fr = "Quelle est la principale différence entre vaccination et sérothérapie ?",
                en = "What is the main difference between vaccination and serotherapy?",
                ar = "ما الفرق الرئيسي بين التلقيح والاستمصال؟"
            ),
            answer = LocalizedSvtText(
                fr = "Vaccin : actif, préventif et durable • Sérum : passif, curatif et temporaire",
                en = "Vaccine: active, preventive, lasting • Serum: passive, curative, temporary",
                ar = "اللقاح: نشيط، وقائي ودائم • المصل: سلبي، علاجي ومؤقت"
            ),
            explanation = LocalizedSvtText(
                fr = "Le vaccin stimule la fabrication de lymphocytes mémoire, alors que le sérum apporte des anticorps déjà prêts.",
                en = "A vaccine stimulates production of memory cells, while serum supplies ready-made antibodies.",
                ar = "يحفز اللقاح إنتاج الذاكرة المناعية، بينما يوفر المصل مضادات أجسام جاهزة."
            )
        ),
        SvtFlashcard(
            id = "fc_imm_3",
            domain = SvtDomain.IMMUNITY,
            question = LocalizedSvtText(
                fr = "Quel type de cellule immunitaire est principalement ciblé par le VIH ?",
                en = "Which immune cell type is primarily targeted by HIV?",
                ar = "ما نوع الخلايا المناعية الذي يستهدفه فيروس VIH أساساً؟"
            ),
            answer = LocalizedSvtText(
                fr = "Les lymphocytes T4 (auxiliaires / CD4+)",
                en = "Helper T4 (CD4+) lymphocytes",
                ar = "اللمفاويات المساعدة T4 (CD4+)"
            ),
            explanation = LocalizedSvtText(
                fr = "Les LT4 coordonnent les réponses humorale et cellulaire : leur destruction paralyse l'immunité spécifique.",
                en = "T4 cells coordinate both humoral and cell-mediated immunity; losing them cripples specific defense.",
                ar = "تنسق LT4 الاستجابتين الخلطية والخلوية، لذا يؤدي تدميرها إلى انهيار المناعة النوعية."
            )
        ),
        SvtFlashcard(
            id = "fc_rep_1",
            domain = SvtDomain.REPRODUCTION,
            question = LocalizedSvtText(
                fr = "Quelle hormone hypophysaire déclenche directement l'ovulation au 14e jour du cycle ?",
                en = "Which pituitary hormone directly triggers ovulation around day 14 of the cycle?",
                ar = "ما الهرمون النخامي الذي يحفز الإباضة مباشرة في اليوم 14 من الدورة؟"
            ),
            answer = LocalizedSvtText(
                fr = "Le pic de LH (Hormone Lutéinisante)",
                en = "The LH (Luteinizing Hormone) surge",
                ar = "ذروة هرمون LH (الهرمون اللوتيئيني)"
            ),
            explanation = LocalizedSvtText(
                fr = "La hausse rapide des œstrogènes exerce un rétrocontrôle positif provoquant un pic de LH qui rompt le follicule mûr.",
                en = "High estrogen levels trigger positive feedback causing an LH surge that ruptures the mature follicle.",
                ar = "يسبب ارتفاع الأستروجين مفعولاً رجعياً إيجابياً يؤدي إلى ذروة LH وانفجار الجريب الناضج."
            )
        ),
        SvtFlashcard(
            id = "fc_rep_2",
            domain = SvtDomain.REPRODUCTION,
            question = LocalizedSvtText(
                fr = "Où a lieu normalement la fécondation chez l'espèce humaine ?",
                en = "Where does fertilization normally occur in humans?",
                ar = "أين يحدث الإخصاب عادةً عند الإنسان؟"
            ),
            answer = LocalizedSvtText(
                fr = "Dans le tiers supérieur de la trompe utérine",
                en = "In the upper third of the Fallopian tube",
                ar = "في الثلث العلوي للخرطوم (قناة فالوب)"
            ),
            explanation = LocalizedSvtText(
                fr = "La cellule-œuf formée migre ensuite tout en se divisant jusqu'à l'utérus pour s'y implanter (nidation).",
                en = "The resulting zygote divides as it travels down to the uterus for implantation.",
                ar = "تهاجر البيضة المخصبة مع انقسامها نحو الرحم لتنغرس في مخاطيته (التعشيش)."
            )
        ),
        SvtFlashcard(
            id = "fc_rep_3",
            domain = SvtDomain.REPRODUCTION,
            question = LocalizedSvtText(
                fr = "Pourquoi les règles n'apparaissent-elles pas au début d'une grossesse ?",
                en = "Why does menstruation stop at the beginning of pregnancy?",
                ar = "لماذا يتوقف الحيض في بداية الحمل؟"
            ),
            answer = LocalizedSvtText(
                fr = "L'hormone HCG maintient le corps jaune, qui continue de sécréter la progestérone",
                en = "hCG maintains the corpus luteum, keeping progesterone levels high",
                ar = "يحافظ هرمون HCG على الجسم الأصفر الذي يستمر في إفراز البروجستيرون"
            ),
            explanation = LocalizedSvtText(
                fr = "La progestérone maintient la muqueuse utérine développée et bloque les contractions du myomètre.",
                en = "Progesterone keeps the uterine endometrium intact and inhibits myometrial contractions.",
                ar = "يحافظ البروجستيرون على نمو مخاطية الرحم ويمنع تقلصات عضلة الرحم."
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
            id = "fc_10",
            domain = SvtDomain.NERVOUS_MUSCULAR,
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
            id = "fc_nerv_2",
            domain = SvtDomain.NERVOUS_MUSCULAR,
            question = LocalizedSvtText(
                fr = "Quel ion déclenche l'exocytose du neurotransmetteur au bouton synaptique ?",
                en = "Which ion triggers neurotransmitter exocytosis at the presynaptic terminal?",
                ar = "ما الأيون الذي يحفز تحرير المبلغ العصبي في الحيز السينابسي؟"
            ),
            answer = LocalizedSvtText("L'ion Calcium (Ca²⁺)", "Calcium ion (Ca²⁺)", "أيون الكالسيوم (Ca²⁺)"),
            explanation = LocalizedSvtText(
                fr = "L'arrivée d'un potentiel d'action ouvre des canaux Ca²⁺ voltage-dépendants, entraînant la fusion des vésicules synaptiques.",
                en = "Action potential arrival opens voltage-gated Ca²⁺ channels, causing synaptic vesicles to fuse.",
                ar = "يؤدي وصول جهد العمل إلى فتح قنوات Ca²⁺ المرتبطة بالجهد والتحام الحويصلات بالغشاء."
            )
        ),
        SvtFlashcard(
            id = "fc_nerv_3",
            domain = SvtDomain.NERVOUS_MUSCULAR,
            question = LocalizedSvtText(
                fr = "Que signifie la loi du « tout ou rien » pour une fibre nerveuse isolée ?",
                en = "What does the \"all-or-none\" law mean for a single nerve fiber?",
                ar = "ماذا يعني قانون «الكل أو اللاشيء» بالنسبة لليف عصبي معزول؟"
            ),
            answer = LocalizedSvtText(
                fr = "Seul un stimulus ≥ seuil déclenche un potentiel d'action d'amplitude maximale et constante (+30 mV)",
                en = "Only a stimulus ≥ threshold triggers an action potential of constant maximal amplitude (+30 mV)",
                ar = "فقط الإهاجة الفعالة (≥ العتبة) تولد جهد عمل بوسع ثابت وأقصى (+30 mV)"
            ),
            explanation = LocalizedSvtText(
                fr = "Sous le seuil (-50 mV), il n'y a qu'une dépolarisation locale. Au-delà, l'intensité est codée en fréquence de PA.",
                en = "Below threshold (-50 mV), only local depolarization occurs. Above threshold, intensity is coded by AP frequency.",
                ar = "دون العتبة يحدث زوال استقطاب محلي فقط، وفوق العتبة تُشفَّر شدة الإهاجة بتردد جهود العمل."
            )
        ),
        SvtFlashcard(
            id = "fc_nerv_4",
            domain = SvtDomain.NERVOUS_MUSCULAR,
            question = LocalizedSvtText(
                fr = "Quel est le rôle des ions Ca²⁺ libérés par le réticulum sarcoplasmique dans le sarcomère ?",
                en = "What is the role of Ca²⁺ ions released by the sarcoplasmic reticulum in the sarcomere?",
                ar = "ما دور أيونات Ca²⁺ المحررة من الشبكة الساركوبلازمية في الساركومير؟"
            ),
            answer = LocalizedSvtText(
                fr = "Ils se fixent sur la troponine et déplacent la tropomyosine pour libérer les sites de l'actine",
                en = "They bind troponin and shift tropomyosin to expose myosin-binding sites on actin",
                ar = "ترتبط بالتروبونين فتزيح التروبوميوزين لتحرير مواقع تثبيت الميوزين على الأكتين"
            ),
            explanation = LocalizedSvtText(
                fr = "Cela permet la formation des ponts d'actomyosine et le glissement des filaments fins vers le centre du sarcomère (raccourcissement de la bande H et de la zone I).",
                en = "This allows actomyosin cross-bridges to form and thin filaments to slide inward, shortening the H-band and I-band.",
                ar = "يسمح ذلك بتشكل قناطر الأكتوميوزين وانزلاق خييطات الأكتين نحو مركز الساركومير (تقلص الشريط I والمنطقة H)."
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
            id = "fc_phys_2",
            domain = SvtDomain.PHYSIOLOGY_IMMUNITY,
            question = LocalizedSvtText(
                fr = "Quelle hormone pancréatique fait baisser la glycémie après un repas ?",
                en = "Which pancreatic hormone lowers blood glucose levels after a meal?",
                ar = "ما الهرمون البنكرياسي الذي يخفض نسبة السكر في الدم بعد الأكل؟"
            ),
            answer = LocalizedSvtText("L'insuline", "Insulin", "الأنسولين"),
            explanation = LocalizedSvtText(
                fr = "Sécrétée par les cellules β des îlots de Langerhans, elle favorise la captation du glucose et sa mise en réserve sous forme de glycogène.",
                en = "Secreted by pancreatic β-cells, it stimulates cellular glucose uptake and glycogen storage.",
                ar = "تفرزه خلايا بيتا في جزر لانغرهانس ويحفز دخول الكليكوز إلى الخلايا وتخزينه على شكل غليكوجين."
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
                fr = "Granite (grenue), Gabbro, Basalte (microlitique), Andésite",
                en = "Granite (coarse-grained), Gabbro, Basalt (microlitic), Andesite",
                ar = "غرانيت، غابرو، بازلت، أنديزيت"
            )
        ),
        SvtRockFamily(
            id = "sedimentary",
            name = LocalizedSvtText("Roches sédimentaires", "Sedimentary Rocks", "صخور رسوبية"),
            origin = LocalizedSvtText(
                fr = "Accumulation, compaction et cimentation (diagenèse) de débris ou précipités chimiques.",
                en = "Deposition, compaction, and cementation (diagenesis) of sediments or mineral precipitates.",
                ar = "تراكم وتماسك وتصخر الرواسب الفتاتية أو الكيميائية أو الإحيائية."
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

    val mindMaps: List<SvtMindMap> = SvtMindMapsData.allMindMaps
}
