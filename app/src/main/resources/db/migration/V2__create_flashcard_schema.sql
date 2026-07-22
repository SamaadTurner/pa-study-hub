-- ============================================================
-- Flashcard Service Schema
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- -------------------------------------------------------
-- Decks
-- -------------------------------------------------------
CREATE TABLE decks (
    id           UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id      UUID        NOT NULL,
    title        VARCHAR(100) NOT NULL,
    description  VARCHAR(500),
    category     VARCHAR(50)  NOT NULL,
    is_public    BOOLEAN      NOT NULL DEFAULT FALSE,
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_decks_user_id       ON decks (user_id)   WHERE is_deleted = FALSE;
CREATE INDEX idx_decks_category      ON decks (category)  WHERE is_deleted = FALSE;
CREATE INDEX idx_decks_public        ON decks (is_public, updated_at DESC) WHERE is_deleted = FALSE;
CREATE INDEX idx_decks_updated_at    ON decks (updated_at DESC);

-- -------------------------------------------------------
-- Cards
-- -------------------------------------------------------
CREATE TABLE cards (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    deck_id    UUID         NOT NULL REFERENCES decks(id) ON DELETE CASCADE,
    front      VARCHAR(2000) NOT NULL,
    back       VARCHAR(5000) NOT NULL,
    hint       VARCHAR(500),
    image_url  VARCHAR(1000),
    tags       TEXT,                           -- comma-separated
    is_deleted BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cards_deck_id ON cards (deck_id) WHERE is_deleted = FALSE;

-- -------------------------------------------------------
-- Review Schedules  (one row per user × card)
-- -------------------------------------------------------
CREATE TABLE review_schedules (
    id               UUID    NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    card_id          UUID    NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    user_id          UUID    NOT NULL,
    ease_factor      DOUBLE PRECISION NOT NULL DEFAULT 2.5,
    interval         INT     NOT NULL DEFAULT 0,
    repetitions      INT     NOT NULL DEFAULT 0,
    next_review_date DATE,
    last_reviewed_at TIMESTAMP,
    last_quality     INT,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_review_user_card UNIQUE (user_id, card_id)
);

CREATE INDEX idx_review_card_user       ON review_schedules (card_id, user_id);
CREATE INDEX idx_review_next_date       ON review_schedules (next_review_date) WHERE next_review_date IS NOT NULL;
CREATE INDEX idx_review_deck_user_date  ON review_schedules (user_id, next_review_date);

-- -------------------------------------------------------
-- Seed data — three public decks with real PA school content
-- (user_id = 00000000-0000-0000-0000-000000000001 = Jocelyn's account seed ID)
-- -------------------------------------------------------
DO $$
DECLARE
    v_user_id   UUID := '00000000-0000-0000-0000-000000000001';
    v_cardio_id UUID;
    v_pharma_id UUID;
    v_anatomy_id UUID;
BEGIN

-- -------------------------------------------------------
-- Deck 1: Cardiology Essentials
-- -------------------------------------------------------
INSERT INTO decks (id, user_id, title, description, category, is_public)
VALUES (gen_random_uuid(), v_user_id,
        'Cardiology Essentials',
        'High-yield cardiology concepts for the PANCE — murmurs, ECG patterns, heart failure, ACS',
        'CARDIOLOGY', TRUE)
RETURNING id INTO v_cardio_id;

INSERT INTO cards (deck_id, front, back, hint, tags) VALUES
(v_cardio_id,
 'What are the classic signs of cardiac tamponade?',
 'Beck''s Triad: (1) Hypotension, (2) Distended jugular veins (JVD), (3) Muffled heart sounds. Also: pulsus paradoxus (>10 mmHg drop in SBP with inspiration), tachycardia, and narrowed pulse pressure. Diagnosis: echo. Treatment: pericardiocentesis.',
 'Think: Beck''s Triad',
 'tamponade,pericardium,emergency'),

(v_cardio_id,
 'A patient presents with crushing chest pain radiating to the left arm and diaphoresis. ECG shows ST elevation in leads II, III, aVF. Which coronary artery is occluded?',
 'Right Coronary Artery (RCA). Inferior MI (ST elevation in II, III, aVF) is most commonly due to RCA occlusion. Check right-sided leads (V3R–V4R) for right ventricular involvement. Avoid nitrates and aggressive diuresis if RV infarct present — requires volume.',
 'Inferior = RCA',
 'MI,STEMI,RCA,inferior'),

(v_cardio_id,
 'What is the Framingham criteria for congestive heart failure diagnosis?',
 'Requires 2 major criteria OR 1 major + 2 minor criteria. MAJOR: PND, orthopnea, elevated JVP, S3 gallop, pulmonary rales, cardiomegaly on CXR, acute pulmonary edema, weight loss >4.5 kg in 5 days with treatment. MINOR: bilateral ankle edema, nocturnal cough, dyspnea on exertion, hepatomegaly, pleural effusion, tachycardia, weight loss >4.5 kg in 5 days.',
 'Major vs Minor criteria',
 'heart-failure,CHF,diagnosis'),

(v_cardio_id,
 'Differentiate systolic vs diastolic heart failure by ejection fraction and typical presentation.',
 'Systolic HF (HFrEF): EF < 40%. Dilated, poorly contracting ventricle. Causes: ischemia, cardiomyopathy. Tx: ACE-I/ARB, beta-blocker, SGLT2-I, diuretics. Diastolic HF (HFpEF): EF ≥ 50%. Stiff, non-compliant ventricle. Causes: HTN, diabetes, obesity. Tx: control HR, BP, fluid balance.',
 'rEF = reduced, pEF = preserved',
 'heart-failure,EF,systolic,diastolic'),

(v_cardio_id,
 'Which murmur increases with Valsalva maneuver and decreases with squatting?',
 'Hypertrophic Obstructive Cardiomyopathy (HOCM). Valsalva ↓ preload → obstruction worsens → murmur louder. Squatting ↑ preload and afterload → obstruction lessens → murmur softer. Also: MVP murmur click moves earlier with Valsalva. Mnemonic: HOCM and MVP become louder when "less blood in the heart" (Valsalva, standing).',
 'Valsalva = ↓ preload',
 'murmur,HOCM,Valsalva,hypertrophic'),

(v_cardio_id,
 'What is the CHADS2-VASc score and what score warrants anticoagulation in A-fib?',
 'CHA₂DS₂-VASc: C=CHF(1), H=HTN(1), A₂=Age≥75(2), D=Diabetes(1), S₂=Stroke/TIA(2), V=Vascular disease(1), A=Age 65–74(1), Sc=Sex category female(1). Score ≥ 2 in males or ≥ 3 in females → anticoagulation recommended. Preferred agents: DOACs (rivaroxaban, apixaban, dabigatran) over warfarin unless valvular A-fib.',
 'Each letter = risk factor for stroke',
 'A-fib,anticoagulation,stroke,CHADS2VASc'),

(v_cardio_id,
 'Name the four findings on ECG that indicate hyperkalemia and at what potassium levels they appear.',
 'K+ 5.5–6.5: Peaked (tented) T waves (earliest finding). K+ 6.5–7.5: Prolonged PR interval, widened QRS. K+ 7.5–8.0: Sine wave pattern (P waves disappear, QRS merges with T). K+ > 8.0: Ventricular fibrillation, asystole. Treatment: calcium gluconate (membrane stabilization), insulin + dextrose, sodium bicarbonate, kayexalate, dialysis.',
 'Peaked T waves are FIRST',
 'hyperkalemia,ECG,potassium,electrolytes'),

(v_cardio_id,
 'What drug is first-line for rate control in atrial fibrillation with reduced ejection fraction (HFrEF)?',
 'Beta-blockers (specifically carvedilol, metoprolol succinate, or bisoprolol) are first-line. Digoxin can be added if rate control is insufficient. Non-dihydropyridine CCBs (diltiazem, verapamil) are CONTRAINDICATED in HFrEF — they are negative inotropes and worsen systolic function.',
 'CCBs contraindicated in HFrEF',
 'A-fib,rate-control,HFrEF,beta-blocker'),

(v_cardio_id,
 'A patient in the ER has a wide-complex tachycardia at 180 bpm. They are hemodynamically stable. What is your management approach?',
 'Assume ventricular tachycardia (VT) until proven otherwise. If stable: IV amiodarone (150 mg over 10 min, then 1 mg/min) or procainamide. If unstable (hypotension, chest pain, AMS): synchronized cardioversion immediately. Do NOT use adenosine or verapamil (can cause VF if it is VT). Get 12-lead ECG. Check electrolytes (K, Mg).',
 'Wide complex = VT until proven otherwise',
 'VT,tachycardia,emergency,cardioversion'),

(v_cardio_id,
 'What is the most common cause of secondary hypertension in young women aged 18–35?',
 'Oral contraceptive use (estrogen-containing). Estrogen ↑ angiotensinogen → ↑ angiotensin II → HTN. Other causes in young women: fibromuscular dysplasia (renal artery stenosis — bruit over renal artery, beaded appearance on angiography), hyperaldosteronism. Renal artery stenosis in young men → atherosclerosis.',
 'Think OCPs first in young women',
 'hypertension,secondary,OCP,young-women');

-- -------------------------------------------------------
-- Deck 2: Pharmacology High-Yield
-- -------------------------------------------------------
INSERT INTO decks (id, user_id, title, description, category, is_public)
VALUES (gen_random_uuid(), v_user_id,
        'Pharmacology High-Yield',
        'Top-tested drug mechanisms, side effects, and contraindications for the PANCE',
        'PHARMACOLOGY', TRUE)
RETURNING id INTO v_pharma_id;

INSERT INTO cards (deck_id, front, back, hint, tags) VALUES
(v_pharma_id,
 'What are the classic side effects of ACE inhibitors, and when are they contraindicated?',
 'Side effects: (1) Dry cough (↑ bradykinin — switch to ARB). (2) Angioedema (rare but life-threatening — history of ACE-I angioedema is absolute contraindication). (3) Hyperkalemia. (4) Acute kidney injury (especially in renal artery stenosis). Contraindications: pregnancy (category D/X — causes fetal renal dysgenesis), bilateral renal artery stenosis, prior ACE-I angioedema.',
 'Cough = bradykinin buildup',
 'ACE-inhibitor,side-effects,contraindications'),

(v_pharma_id,
 'A patient on metformin is scheduled for contrast CT scan. What is the concern and what should be done?',
 'Concern: Contrast-induced nephropathy can cause acute kidney injury → metformin accumulates → lactic acidosis. Management: Hold metformin 48 hours before contrast if eGFR < 60, or hold at the time of contrast and restart 48 hours after if renal function is stable. If emergent scan needed, give IV fluids and monitor renal function closely.',
 'Contrast + metformin → lactic acidosis risk',
 'metformin,contrast,AKI,diabetes'),

(v_pharma_id,
 'What is the mechanism of action of statins, and what are their major side effects?',
 'MOA: Inhibit HMG-CoA reductase → ↓ cholesterol synthesis → ↑ LDL receptors on hepatocytes → ↓ LDL. Side effects: (1) Myopathy/rhabdomyolysis (check CK if muscle pain — discontinue if CK >10x ULN). (2) Hepatotoxicity (monitor LFTs). (3) Teratogenic — contraindicated in pregnancy. Drug interactions: fibrates + statins ↑ myopathy risk. Avoid grapefruit (CYP3A4 inhibition ↑ levels).',
 'HMG-CoA reductase inhibitors',
 'statin,HMG-CoA,myopathy,cholesterol'),

(v_pharma_id,
 'What antibiotics are associated with C. difficile colitis, and what is the treatment?',
 'Highest risk: Clindamycin, fluoroquinolones (ciprofloxacin, levofloxacin), ampicillin/amoxicillin, cephalosporins. Mechanism: disruption of normal colonic flora → C. diff overgrowth → exotoxin A and B → colitis. Treatment: (1) Mild-moderate: oral vancomycin 125 mg QID × 10 days OR fidaxomicin. (2) Severe: oral vancomycin 500 mg QID ± IV metronidazole. (3) Fulminant: colectomy. Stop offending antibiotic. Avoid anti-motility agents.',
 'Clindamycin = classic culprit',
 'C-diff,antibiotics,colitis,vancomycin'),

(v_pharma_id,
 'Name the mechanism of action and key clinical uses for each class of beta-blockers (selective vs non-selective).',
 'Selective (β1): metoprolol, atenolol, bisoprolol — preferred in asthma/COPD, peripheral vascular disease. Non-selective (β1+β2): propranolol, nadolol, carvedilol (also α1) — used in portal HTN (propranolol), essential tremor (propranolol), thyroid storm. Carvedilol: α1+β1+β2 — preferred in HFrEF. Beta-blockers are contraindicated in decompensated HF, high-degree AV block, cocaine-induced MI (use phentolamine).',
 'Selective = β1 only',
 'beta-blocker,selective,cardiology,pharmacology'),

(v_pharma_id,
 'What is serotonin syndrome vs. neuroleptic malignant syndrome? How do you differentiate them?',
 'Serotonin syndrome: ↑ serotonergic activity. Triad: altered mental status, autonomic instability, neuromuscular abnormalities (hyperreflexia, clonus, myoclonus). Onset: rapid (hours). Causes: SSRIs + MAOIs, linezolid, tramadol, triptans. Tx: cyproheptadine (serotonin antagonist), benzos, supportive. NMS: dopamine blockade. Triad: hyperthermia, muscle rigidity (lead-pipe), autonomic instability. Onset: days-weeks. Cause: antipsychotics. Tx: dantrolene, bromocriptine. Key differentiator: Clonus/hyperreflexia = serotonin syndrome; Lead-pipe rigidity = NMS.',
 'Clonus = serotonin, Rigidity = NMS',
 'serotonin-syndrome,NMS,antipsychotics,SSRI'),

(v_pharma_id,
 'A patient on warfarin presents with a supratherapeutic INR of 8 but no bleeding. What is your management?',
 'INR 4–10, no bleeding: Hold warfarin 1–2 doses. If high bleeding risk: add oral vitamin K 1–2.5 mg. INR > 10, no bleeding: Hold warfarin + oral vitamin K 2.5–5 mg, recheck INR in 24h. Serious/life-threatening bleeding: 4-factor PCC (prothrombin complex concentrate) + IV vitamin K 10 mg slow infusion. Fresh frozen plasma (FFP) is second-line. Do NOT use FFP alone for urgent reversal — PCC works faster.',
 'Bleeding = PCC first, not FFP',
 'warfarin,INR,reversal,anticoagulation'),

(v_pharma_id,
 'What are the contraindications to NSAIDs and what renal complication can they cause?',
 'Contraindications: peptic ulcer disease, CKD (eGFR < 30), heart failure (fluid retention, ↓ GFR), aspirin-exacerbated respiratory disease (AERD/Samter''s triad), third trimester pregnancy (premature closure of ductus arteriosus). Renal complications: (1) Prerenal AKI (↓ prostaglandins → vasoconstriction of afferent arteriole → ↓ GFR). (2) Interstitial nephritis. (3) Papillary necrosis with chronic use. Use acetaminophen as alternative in these patients.',
 'NSAIDs block prostaglandins → ↓ renal blood flow',
 'NSAIDs,contraindications,renal,CKD'),

(v_pharma_id,
 'What is the mechanism by which aminoglycosides cause nephrotoxicity and ototoxicity?',
 'Nephrotoxicity: Aminoglycosides accumulate in proximal tubular cells → free radical generation → tubular cell necrosis → non-oliguric ATN. Risk factors: prolonged use, pre-existing CKD, dehydration, concurrent nephrotoxins. Monitor: BUN/Cr, drug levels (trough). Ototoxicity: Drug accumulates in cochlear hair cells (irreversible sensorineural hearing loss) and vestibular cells. Monitor: audiometry. Also: gentamicin causes vestibulotoxicity more than cochleotoxicity. "GNARLY" toxicities: Gentamicin=Nephro+Oto.',
 'Trough levels predict nephrotoxicity',
 'aminoglycoside,nephrotoxicity,ototoxicity,ATN'),

(v_pharma_id,
 'Name three classes of medications that can cause a drug-induced lupus (DIL) and the antibody most specific for it.',
 'Classic DIL drugs (mnemonic HIPPIE): Hydralazine, Isoniazid, Procainamide, Phenytoin, Infliximab/biologics, d-penicillamine (E). Also: quinidine, methyldopa, minocycline. Anti-histone antibodies are most associated with DIL (95% sensitive but not specific — also in SLE). Anti-dsDNA and anti-Smith antibodies are specific for SLE but NOT DIL. Drug-induced lupus: renal and CNS involvement rare (unlike SLE). Resolves when drug stopped.',
 'Anti-histone = drug-induced lupus',
 'drug-induced-lupus,anti-histone,pharmacology,autoimmune');

-- -------------------------------------------------------
-- Deck 3: Anatomy & Physiology Essentials
-- -------------------------------------------------------
INSERT INTO decks (id, user_id, title, description, category, is_public)
VALUES (gen_random_uuid(), v_user_id,
        'Anatomy & Physiology Essentials',
        'Key anatomical relationships, nerve injuries, and physiological concepts tested on the PANCE',
        'ANATOMY', TRUE)
RETURNING id INTO v_anatomy_id;

INSERT INTO cards (deck_id, front, back, hint, tags) VALUES
(v_anatomy_id,
 'What muscle and nerve are damaged in a "wrist drop" injury?',
 'Wrist drop = radial nerve injury (C5–T1, posterior cord of brachial plexus). Affected muscles: extensor carpi radialis, extensor carpi ulnaris, extensor digitorum — loss of wrist and finger extension. Classic mechanism: "Saturday night palsy" (compression in spiral groove of humerus). Also occurs with humeral shaft fracture. Test: Ask patient to extend wrist against resistance.',
 'Radial nerve = wrist and finger extension',
 'radial-nerve,wrist-drop,nerve-injury,anatomy'),

(v_anatomy_id,
 'What is the clinical presentation of an ulnar nerve injury at the wrist?',
 'Ulnar nerve (C8–T1) injury at the wrist: "Claw hand" deformity (ring and little fingers — 4th and 5th digits). Loss of: intrinsic hand muscles (interossei, hypothenar muscles, medial two lumbricals), adductor pollicis. Froment''s sign: patient flexes IP joint of thumb to hold paper (compensating for lost adductor pollicis). Ulnar nerve at elbow injury: also loses medial forearm sensation and flexor digitorum profundus to digits 4–5.',
 'Ulnar = claw hand (ring + little fingers)',
 'ulnar-nerve,claw-hand,nerve-injury,anatomy'),

(v_anatomy_id,
 'What structure passes through the carpal tunnel, and what is compressed in carpal tunnel syndrome?',
 'Carpal tunnel contents: median nerve (C6–T1) + 9 flexor tendons (FPL, 4 FDS, 4 FDP). The ulnar nerve and artery pass through Guyon''s canal (NOT carpal tunnel). CTS compresses the median nerve → symptoms: pain/tingling in first 3.5 fingers (thumb, index, middle, radial half of ring), thenar wasting. Tests: Phalen''s (wrist flexion 60s) and Tinel''s (tapping over carpal tunnel). Tx: splinting, corticosteroid injection, surgical release.',
 'Median nerve = first 3.5 fingers',
 'carpal-tunnel,median-nerve,anatomy,CTS'),

(v_anatomy_id,
 'Describe the relationship between the ureters and uterine artery — why is this surgically important?',
 'The ureter passes UNDER the uterine artery ("water under the bridge"). Mnemonic: "The water (ureter) flows under the bridge (uterine artery)." Clinical significance: During hysterectomy, the uterine artery is ligated. The ureter runs just 1–2 cm lateral to the cervix at this point. Inadvertent ligation or transection of the ureter is the most common urologic complication of hysterectomy (0.5–1%).',
 'Water under the bridge',
 'ureter,uterine-artery,anatomy,surgery,gynecology'),

(v_anatomy_id,
 'What cranial nerves carry parasympathetic fibers, and what organs do they innervate?',
 'Cranial nerves with parasympathetic fibers (CN III, VII, IX, X): CN III (oculomotor) → ciliary ganglion → pupil constriction (miosis) and lens accommodation. CN VII (facial) → pterygopalatine ganglion → lacrimal and nasal glands; submandibular ganglion → submandibular and sublingual glands. CN IX (glossopharyngeal) → otic ganglion → parotid gland. CN X (vagus) → intrinsic ganglia in thoracic and abdominal viscera. Mnemonic: S3AD3M (SIII → ciliary; S3VII → lacrimal, submandibular; SSIX → parotid; SX → viscera).',
 'CN III, VII, IX, X carry parasympathetics',
 'cranial-nerves,parasympathetic,anatomy,autonomic'),

(v_anatomy_id,
 'What is the "unhappy triad" of the knee and how does the mechanism of injury occur?',
 'Unhappy triad (O''Donoghue''s triad): Anterior cruciate ligament (ACL) + medial collateral ligament (MCL) + medial meniscus tears. Mechanism: Valgus stress to planted foot (lateral blow to knee while foot is planted) — common in football "clipping" injuries. The medial compartment structures are tethered to each other, making combined injury common. Exam: Valgus stress test (MCL), anterior drawer/Lachman (ACL), McMurray (meniscus). MRI is gold standard.',
 'Valgus stress = medial side tears',
 'knee,ACL,MCL,meniscus,unhappy-triad'),

(v_anatomy_id,
 'Where does the thoracic duct empty, and what type of lymph does it carry?',
 'The thoracic duct drains into the left subclavian vein at its junction with the left internal jugular vein (left venous angle). It drains MOST of the body''s lymph: the entire left side of the body + right lower extremity + abdominal viscera (chyle — lymph rich in fat from lacteals). The RIGHT lymphatic duct drains: right arm, right side of head/neck, right thorax → right venous angle. Chylothorax (milky pleural fluid high in triglycerides) occurs with thoracic duct injury.',
 'Thoracic duct = LEFT venous angle',
 'thoracic-duct,lymphatics,anatomy,chylothorax'),

(v_anatomy_id,
 'What is the physiological basis for the Frank-Starling law and how does it apply clinically?',
 'Frank-Starling Law: As end-diastolic volume (preload) increases, stroke volume increases — up to a point. Mechanism: ↑ preload → ↑ sarcomere stretch → more cross-bridge formation (optimal actin-myosin overlap) → ↑ contractility and stroke volume. Clinical applications: (1) IV fluids increase preload → ↑ CO in hypovolemia. (2) In severe heart failure, the curve shifts down and right — the heart is operating on the flat/descending limb, unable to further increase SV with more preload. (3) Basis for using diuretics in acute HF (reduce preload).',
 'Stretch → more cross-bridge = more force',
 'Frank-Starling,preload,stroke-volume,physiology'),

(v_anatomy_id,
 'What are the boundaries of the femoral triangle and what structures pass through it?',
 'Boundaries: Superiorly = inguinal ligament. Laterally = medial border of sartorius. Medially = medial border of adductor longus. Floor = iliopsoas + pectineus + adductor longus. Roof = fascia lata + skin. Contents (lateral to medial — mnemonic NAVEL): Nerve (femoral nerve, L2–L4), Artery (femoral artery), Vein (femoral vein), Empty space (femoral canal), Lymphatics (femoral canal + lymph nodes). The femoral hernia enters through the femoral canal — more common in women. Pulsatile groin mass = femoral artery aneurysm.',
 'NAVEL: Nerve, Artery, Vein, Empty, Lymphatics',
 'femoral-triangle,anatomy,NAVEL,hernia'),

(v_anatomy_id,
 'Describe the layers of the abdominal wall (outside to inside) and the significance of the arcuate line.',
 'Layers (outside → in): Skin → Subcutaneous fat (Camper''s fascia) → Scarpa''s fascia → External oblique muscle/aponeurosis → Internal oblique muscle → Transversus abdominis → Transversalis fascia → Extraperitoneal fat → Peritoneum. Rectus sheath: Above arcuate line (halfway between umbilicus and pubis): anterior = EO + IO aponeuroses; posterior = IO + TA aponeuroses. Below arcuate line: ALL aponeuroses go anterior → rectus muscle has NO posterior sheath (only transversalis fascia). This is why incisions below the arcuate line may enter the peritoneum more easily.',
 'Below arcuate line = no posterior rectus sheath',
 'abdominal-wall,arcuate-line,anatomy,layers');

END $$;
