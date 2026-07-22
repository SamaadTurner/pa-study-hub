-- ============================================================
-- Exam Service Schema
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- -------------------------------------------------------
-- Questions
-- -------------------------------------------------------
CREATE TABLE questions (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    stem                VARCHAR(3000) NOT NULL,
    clinical_vignette   VARCHAR(1000),
    category            VARCHAR(50)  NOT NULL,
    difficulty          VARCHAR(20)  NOT NULL,
    explanation         VARCHAR(2000) NOT NULL,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_questions_category   ON questions (category)  WHERE is_active = TRUE;
CREATE INDEX idx_questions_difficulty ON questions (difficulty) WHERE is_active = TRUE;

-- -------------------------------------------------------
-- Answer Options (4–5 per question)
-- -------------------------------------------------------
CREATE TABLE answer_options (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    question_id  UUID         NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    text         VARCHAR(1000) NOT NULL,
    is_correct   BOOLEAN      NOT NULL DEFAULT FALSE,
    order_index  INT          NOT NULL
);

CREATE INDEX idx_answer_options_question_id ON answer_options (question_id);

-- -------------------------------------------------------
-- Exam Sessions
-- -------------------------------------------------------
CREATE TABLE exam_sessions (
    id                  UUID    NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id             UUID    NOT NULL,
    question_count      INT     NOT NULL,
    time_limit_minutes  INT     NOT NULL DEFAULT 0,
    category_filter     VARCHAR(50),
    difficulty_filter   VARCHAR(20),
    status              VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    score               INT,
    score_percent       DOUBLE PRECISION,
    started_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMP,
    duration_seconds    INT
);

CREATE INDEX idx_exam_sessions_user_id     ON exam_sessions (user_id);
CREATE INDEX idx_exam_sessions_started_at  ON exam_sessions (started_at DESC);
CREATE INDEX idx_exam_sessions_user_started ON exam_sessions (user_id, started_at DESC);

-- -------------------------------------------------------
-- Exam Answers
-- -------------------------------------------------------
CREATE TABLE exam_answers (
    id                  UUID    NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    exam_session_id     UUID    NOT NULL REFERENCES exam_sessions(id) ON DELETE CASCADE,
    question_id         UUID    NOT NULL REFERENCES questions(id),
    selected_option_id  UUID    REFERENCES answer_options(id),
    is_correct          BOOLEAN NOT NULL DEFAULT FALSE,
    time_spent_seconds  INT,
    answered_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_exam_answers_session_id  ON exam_answers (exam_session_id);
CREATE INDEX idx_exam_answers_question_id ON exam_answers (question_id);
CREATE UNIQUE INDEX idx_exam_answers_session_question ON exam_answers (exam_session_id, question_id);

-- ============================================================
-- Seed Data: 30 PANCE-style Questions
-- (10 Cardiology, 10 Pharmacology, 10 Pulmonology)
-- ============================================================
DO $$
DECLARE
    q_id UUID;
BEGIN

-- ===========================
-- CARDIOLOGY (10 questions)
-- ===========================

-- Q1
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'Which of the following best describes the treatment of stable angina?',
'A 58-year-old man with hypertension and hyperlipidemia presents with reproducible chest pain on exertion that resolves with rest. ECG is normal at rest.',
'CARDIOLOGY', 'MEDIUM',
'Stable angina is treated with sublingual nitroglycerin for acute episodes and long-acting nitrates, beta-blockers, or calcium channel blockers for prevention. Beta-blockers are first-line for patients with concurrent hypertension or prior MI. Aspirin is used for anti-platelet effect. Risk factor modification (statins, BP control) is essential.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Sublingual nitroglycerin for acute episodes + beta-blocker for prevention', TRUE, 0),
(q_id, 'Immediate cardiac catheterization', FALSE, 1),
(q_id, 'IV heparin and aspirin', FALSE, 2),
(q_id, 'Emergent percutaneous coronary intervention (PCI)', FALSE, 3);

-- Q2
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'A patient presents with sudden onset of severe tearing chest pain radiating to the back. BP is 180/100 in the right arm and 140/85 in the left arm. CXR shows widened mediastinum. What is the most likely diagnosis?',
NULL,
'CARDIOLOGY', 'HARD',
'Aortic dissection presents with sudden onset tearing/ripping chest pain radiating to the back. The blood pressure differential between arms (>20 mmHg) is classic. Widened mediastinum on CXR supports the diagnosis. CT angiography is the gold standard. Type A (ascending aorta) requires emergency surgery. Type B (descending) is managed medically with BP control (IV labetalol or nitroprusside). Do NOT anticoagulate.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Aortic dissection', TRUE, 0),
(q_id, 'STEMI', FALSE, 1),
(q_id, 'Pulmonary embolism', FALSE, 2),
(q_id, 'Esophageal rupture', FALSE, 3);

-- Q3
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'Which diuretic is most appropriate for a patient with CHF and normal renal function?',
'A 72-year-old woman with known congestive heart failure presents with 2+ pitting edema to the knees and shortness of breath at rest. eGFR is 65.',
'CARDIOLOGY', 'EASY',
'Loop diuretics (furosemide, bumetanide, torsemide) are first-line for fluid overload in CHF. They work in the thick ascending loop of Henle to inhibit Na-K-2Cl cotransporter. Thiazides are less potent and lose effectiveness at eGFR < 30. Spironolactone (aldosterone antagonist) is adjunct in HFrEF but is not the primary diuretic for acute fluid overload. Acetazolamide is a carbonic anhydrase inhibitor used in metabolic alkalosis and glaucoma.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Furosemide (loop diuretic)', TRUE, 0),
(q_id, 'Hydrochlorothiazide', FALSE, 1),
(q_id, 'Spironolactone alone', FALSE, 2),
(q_id, 'Acetazolamide', FALSE, 3);

-- Q4
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'Which ECG finding is MOST consistent with right ventricular hypertrophy?',
NULL,
'CARDIOLOGY', 'MEDIUM',
'Right ventricular hypertrophy (RVH) on ECG: R > S in V1 (dominant R wave in V1), right axis deviation (>+90°), and S wave persistence in V5-V6. Causes: pulmonary hypertension, pulmonic stenosis, cor pulmonale from COPD. Contrast with LVH: tall R waves in V5-V6 and deep S waves in V1-V2 (Sokolow-Lyon criteria: S in V1 + R in V5 > 35 mm), left axis deviation.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Dominant R wave in V1 with right axis deviation', TRUE, 0),
(q_id, 'Tall R waves in V5-V6 with left axis deviation', FALSE, 1),
(q_id, 'Delta waves with short PR interval', FALSE, 2),
(q_id, 'Peaked T waves in precordial leads', FALSE, 3);

-- Q5
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'A 45-year-old woman is found to have a systolic murmur that increases with standing and decreases with squatting. Which condition is most likely?',
NULL,
'CARDIOLOGY', 'HARD',
'HOCM (Hypertrophic Obstructive Cardiomyopathy) is the classic murmur that worsens with maneuvers that decrease preload (standing, Valsalva) and improves with maneuvers that increase preload (squatting, passive leg raise). This is because decreased preload narrows the outflow tract and worsens obstruction. MVP click also moves earlier with Valsalva. Aortic stenosis and MR both decrease with standing.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Hypertrophic obstructive cardiomyopathy (HOCM)', TRUE, 0),
(q_id, 'Aortic stenosis', FALSE, 1),
(q_id, 'Mitral regurgitation', FALSE, 2),
(q_id, 'Ventricular septal defect', FALSE, 3);

-- Q6
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'What is the first-line treatment for hemodynamically unstable ventricular tachycardia?',
'A 67-year-old man with prior MI presents with palpitations and lightheadedness. BP is 70/40. Monitor shows a wide-complex tachycardia at 190 bpm.',
'CARDIOLOGY', 'EASY',
'Hemodynamically unstable VT requires immediate synchronized cardioversion. Do not delay for medications. After cardioversion, amiodarone infusion is used to maintain sinus rhythm. Lidocaine is an alternative antiarrhythmic. If the patient is pulseless, defibrillation (unsynchronized) is used, not synchronized cardioversion. Adenosine is used only for stable SVT, not wide-complex tachycardia.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Immediate synchronized cardioversion', TRUE, 0),
(q_id, 'IV adenosine 6 mg push', FALSE, 1),
(q_id, 'Oral beta-blocker and observation', FALSE, 2),
(q_id, 'IV verapamil', FALSE, 3);

-- Q7
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'A patient with atrial fibrillation develops rapid ventricular rate. Which of the following is CONTRAINDICATED if the patient has WPW syndrome?',
NULL,
'CARDIOLOGY', 'HARD',
'In WPW with A-fib, AV node blockers (adenosine, verapamil, digoxin, beta-blockers) are CONTRAINDICATED. These agents block the AV node but leave the accessory pathway (Bundle of Kent) unblocked, potentially causing all impulses to conduct via the accessory pathway at very high rates → ventricular fibrillation. Treatment: IV procainamide or ibutilide, or urgent cardioversion. Ablation is the definitive treatment.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'IV adenosine', TRUE, 0),
(q_id, 'IV procainamide', FALSE, 1),
(q_id, 'Synchronized cardioversion', FALSE, 2),
(q_id, 'IV ibutilide', FALSE, 3);

-- Q8
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'Which of the following is the most common cause of mitral stenosis?',
NULL,
'CARDIOLOGY', 'EASY',
'Rheumatic heart disease is the most common cause of mitral stenosis worldwide, caused by group A streptococcal pharyngitis triggering an autoimmune response that damages the mitral valve leaflets and causes leaflet fusion, thickening, and calcification. The opening snap followed by a low-pitched diastolic rumble at the apex is the classic exam finding. Rare causes: congenital, carcinoid syndrome, systemic lupus.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Rheumatic heart disease', TRUE, 0),
(q_id, 'Mitral valve prolapse', FALSE, 1),
(q_id, 'Marfan syndrome', FALSE, 2),
(q_id, 'Infective endocarditis', FALSE, 3);

-- Q9
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'A patient is started on lisinopril. Two days later she returns complaining of throat swelling and difficulty swallowing with no urticaria. What is the most appropriate next step?',
NULL,
'CARDIOLOGY', 'MEDIUM',
'ACE inhibitor-induced angioedema is a serious adverse effect caused by bradykinin accumulation (ACE normally degrades bradykinin). It is not IgE-mediated (no urticaria, no relation to dose). Treatment: discontinue ACE inhibitor immediately, consider FFP, icatibant (bradykinin B2 receptor antagonist), or C1 esterase inhibitor. Do NOT rechallenge or switch to another ACE inhibitor. Switch to ARB (which does not affect bradykinin) — but note ARBs have a small risk of cross-reactivity.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Discontinue lisinopril immediately and monitor airway', TRUE, 0),
(q_id, 'Reduce the dose of lisinopril', FALSE, 1),
(q_id, 'Add cetirizine (antihistamine) and continue lisinopril', FALSE, 2),
(q_id, 'Switch to a different ACE inhibitor', FALSE, 3);

-- Q10
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'Which of the following best describes a third heart sound (S3)?',
NULL,
'CARDIOLOGY', 'MEDIUM',
'S3 (ventricular gallop) occurs in early diastole when blood rushes into a volume-overloaded, dilated, non-compliant ventricle. Heard best at apex with bell of stethoscope, patient in left lateral decubitus. PATHOLOGIC in adults > 40 years (indicates CHF, dilated cardiomyopathy). Physiologic in children and young adults (< 40) and pregnant women. S4 is a late diastolic sound caused by atrial kick into a stiff (non-compliant) ventricle — associated with HTN, LVH, HFpEF.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Early diastolic sound from rapid ventricular filling in a volume-overloaded heart', TRUE, 0),
(q_id, 'Late diastolic sound from atrial contraction against a stiff ventricle', FALSE, 1),
(q_id, 'Systolic ejection click from bicuspid aortic valve', FALSE, 2),
(q_id, 'Mid-systolic click from mitral valve prolapse', FALSE, 3);

-- ===========================
-- PHARMACOLOGY (10 questions)
-- ===========================

-- Q11
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'A patient taking lithium develops polyuria, polydipsia, and a serum lithium level of 1.8 mEq/L. What should be done?',
NULL,
'PHARMACOLOGY', 'MEDIUM',
'Lithium toxicity: therapeutic range 0.6–1.2 mEq/L (up to 1.5 for acute mania). Level >1.5 = early toxicity (nausea, tremor, ataxia). Level >2.0 = severe (seizures, cardiac arrhythmias, coma). Treatment: hold lithium, IV normal saline (lithium follows sodium), hemodialysis if severe (level >4.0 or severe neurological symptoms). NSAIDs, thiazides, and ACE inhibitors increase lithium levels. Avoid dehydration and low-sodium diets.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Hold lithium, hydrate with IV normal saline, recheck level', TRUE, 0),
(q_id, 'Continue lithium at same dose and add a diuretic', FALSE, 1),
(q_id, 'Switch immediately to valproate', FALSE, 2),
(q_id, 'Add an NSAID for symptoms', FALSE, 3);

-- Q12
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'Which of the following is the mechanism of action of metformin in type 2 diabetes?',
NULL,
'PHARMACOLOGY', 'EASY',
'Metformin primarily activates AMPK → inhibits hepatic gluconeogenesis (decreasing glucose output from the liver), which accounts for most of its glucose-lowering effect. It also improves peripheral insulin sensitivity and delays GI glucose absorption. It does NOT cause hypoglycemia (not an insulin secretagogue). Contraindicated in eGFR < 30 and holds for contrast dye. Weight neutral to modest weight loss. Does NOT cause lactic acidosis in normal renal function at therapeutic doses.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Inhibits hepatic gluconeogenesis via AMPK activation', TRUE, 0),
(q_id, 'Stimulates pancreatic insulin secretion', FALSE, 1),
(q_id, 'Blocks intestinal alpha-glucosidases', FALSE, 2),
(q_id, 'Inhibits renal glucose reabsorption via SGLT2', FALSE, 3);

-- Q13
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'A 28-year-old woman in her first trimester of pregnancy is found to have a DVT. Which anticoagulant is most appropriate?',
NULL,
'PHARMACOLOGY', 'MEDIUM',
'Low molecular weight heparin (LMWH, e.g., enoxaparin) is the anticoagulant of choice in pregnancy. It does not cross the placenta and is safe. Warfarin is CONTRAINDICATED in the first trimester (crosses placenta, causes warfarin embryopathy: nasal hypoplasia, stippled epiphyses) and should be avoided near term (neonatal bleeding). DOACs (rivaroxaban, apixaban) are CONTRAINDICATED in pregnancy — inadequate safety data, cross the placenta. Unfractionated heparin is acceptable but less convenient than LMWH.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Low molecular weight heparin (enoxaparin)', TRUE, 0),
(q_id, 'Warfarin', FALSE, 1),
(q_id, 'Rivaroxaban (DOAC)', FALSE, 2),
(q_id, 'Aspirin alone', FALSE, 3);

-- Q14
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'A patient taking fluoxetine for depression is started on tramadol for pain. Two days later they develop agitation, diaphoresis, and clonus. What is the most likely diagnosis?',
NULL,
'PHARMACOLOGY', 'HARD',
'Serotonin syndrome is caused by excess serotonergic activity, commonly from combining two serotonergic agents. Fluoxetine (SSRI) + tramadol (also a serotonin reuptake inhibitor + weak opioid) = serotonin syndrome. Triad: altered mental status, autonomic instability (hyperthermia, diaphoresis, tachycardia), neuromuscular findings (clonus, hyperreflexia, myoclonus). Treatment: discontinue offending drugs, cyproheptadine (serotonin antagonist), benzos for agitation, cooling, supportive care.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Serotonin syndrome', TRUE, 0),
(q_id, 'Neuroleptic malignant syndrome', FALSE, 1),
(q_id, 'Opioid overdose', FALSE, 2),
(q_id, 'Anticholinergic toxicity', FALSE, 3);

-- Q15
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'Which of the following is the reversal agent for heparin?',
NULL,
'PHARMACOLOGY', 'EASY',
'Protamine sulfate reverses heparin (both unfractionated and partially reverses LMWH). It is a positively charged protein derived from fish sperm that binds the negatively charged heparin and neutralizes its anticoagulant activity. Reversal of warfarin: vitamin K ± 4-factor PCC or FFP. Reversal of dabigatran (DOAC): idarucizumab. Reversal of factor Xa inhibitors (rivaroxaban, apixaban): andexanet alfa.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Protamine sulfate', TRUE, 0),
(q_id, 'Vitamin K', FALSE, 1),
(q_id, 'Idarucizumab', FALSE, 2),
(q_id, 'Naloxone', FALSE, 3);

-- Q16
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'A patient on isoniazid (INH) for latent TB develops peripheral neuropathy. What is the underlying mechanism?',
NULL,
'PHARMACOLOGY', 'MEDIUM',
'INH causes peripheral neuropathy by interfering with pyridoxine (vitamin B6) metabolism. INH competes with pyridoxal phosphate, the active form of B6, leading to functional B6 deficiency. Prevention: supplement pyridoxine 25–50 mg/day for all patients on INH. Other INH toxicities: hepatotoxicity (most serious — monitor LFTs, hold if ALT > 3x ULN with symptoms), lupus-like syndrome (anti-histone Ab). Fast acetylators metabolize INH more quickly and may need dose adjustment.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Functional vitamin B6 (pyridoxine) deficiency', TRUE, 0),
(q_id, 'Direct axonal toxicity from drug accumulation', FALSE, 1),
(q_id, 'Vitamin B12 depletion', FALSE, 2),
(q_id, 'Copper chelation causing demyelination', FALSE, 3);

-- Q17
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'Which adverse effect is unique to clozapine compared to other antipsychotics?',
NULL,
'PHARMACOLOGY', 'HARD',
'Agranulocytosis (life-threatening neutropenia) is the unique and most dangerous side effect of clozapine, occurring in 1–2% of patients. REMS program requires ANC monitoring: baseline, weekly for 6 months, biweekly for 6 months, then monthly. Hold clozapine if ANC < 1500. Clozapine also causes seizures (dose-dependent), myocarditis/cardiomyopathy, drooling, metabolic syndrome, and sedation. It is used only for treatment-resistant schizophrenia or suicidality in schizophrenia.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Agranulocytosis', TRUE, 0),
(q_id, 'Tardive dyskinesia', FALSE, 1),
(q_id, 'QT prolongation', FALSE, 2),
(q_id, 'Neuroleptic malignant syndrome', FALSE, 3);

-- Q18
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'A patient is started on amiodarone for atrial fibrillation. Which organ system requires long-term monitoring?',
NULL,
'PHARMACOLOGY', 'MEDIUM',
'Amiodarone has multiple organ toxicities requiring long-term monitoring. Mnemonic "PALE": Pulmonary toxicity (interstitial pneumonitis/fibrosis — most serious; monitor CXR and PFTs), Appearance (blue-gray skin discoloration from iodine deposition, photosensitivity), Liver toxicity (elevated LFTs, hepatitis; monitor LFTs), Eyes (corneal microdeposits — nearly universal but rarely symptomatic; optic neuropathy rare). Also: thyroid dysfunction (contains iodine — both hyper and hypothyroidism; monitor TFTs every 6 months), bradycardia, prolonged QT.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Thyroid, pulmonary, hepatic, and ocular monitoring required', TRUE, 0),
(q_id, 'Renal function only', FALSE, 1),
(q_id, 'CBC for bone marrow suppression monthly', FALSE, 2),
(q_id, 'No monitoring required for long-term use', FALSE, 3);

-- Q19
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'Which medication should be avoided in a patient with a sulfa allergy who requires a diuretic?',
NULL,
'PHARMACOLOGY', 'MEDIUM',
'Sulfonamide diuretics include: thiazides (hydrochlorothiazide, chlorthalidone), loop diuretics (furosemide, bumetanide, torsemide), and carbonic anhydrase inhibitors (acetazolamide). These contain a sulfonamide moiety and theoretically cross-react with sulfa antibiotics (trimethoprim-sulfamethoxazole). Cross-reactivity is debated and clinically rare, but in documented sulfa allergy, use caution or alternative (ethacrynic acid is the only loop diuretic without a sulfonamide group). Spironolactone and eplerenone have no sulfonamide group.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Furosemide (contains sulfonamide group)', TRUE, 0),
(q_id, 'Spironolactone', FALSE, 1),
(q_id, 'Eplerenone', FALSE, 2),
(q_id, 'Triamterene', FALSE, 3);

-- Q20
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'A patient taking warfarin starts a course of fluconazole for a vaginal yeast infection. What adjustment is needed?',
NULL,
'PHARMACOLOGY', 'HARD',
'Fluconazole is a strong CYP2C9 inhibitor. Warfarin (S-warfarin, the more potent isomer) is primarily metabolized by CYP2C9. Inhibiting CYP2C9 → decreased warfarin metabolism → increased warfarin levels → supratherapeutic INR → bleeding risk. Action: check INR 3–5 days after starting fluconazole, anticipate dose reduction needed, and counsel patient on bleeding signs. Other strong CYP2C9 inhibitors: amiodarone, metronidazole, trimethoprim-sulfamethoxazole (common warfarin interactions).')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Expect INR to increase — reduce warfarin dose and monitor closely', TRUE, 0),
(q_id, 'No adjustment needed — fluconazole does not affect warfarin', FALSE, 1),
(q_id, 'Expect INR to decrease — increase warfarin dose', FALSE, 2),
(q_id, 'Hold warfarin entirely during fluconazole course', FALSE, 3);

-- ===========================
-- PULMONOLOGY (10 questions)
-- ===========================

-- Q21
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'A patient with asthma presents to the ED with severe bronchospasm not responding to albuterol. What is the next best treatment?',
'O2 sat 88%, RR 28, using accessory muscles, unable to complete sentences.',
'PULMONOLOGY', 'MEDIUM',
'Severe acute asthma exacerbation not responding to SABA (albuterol): add ipratropium (anticholinergic bronchodilator), systemic corticosteroids (IV methylprednisolone or oral prednisone — critical to reduce inflammation), continuous albuterol nebulization, magnesium sulfate (bronchodilator, inhibits smooth muscle contraction via calcium antagonism — useful in severe cases), heliox (heliox-driven nebs to reduce airway resistance). Intubation is last resort due to high risk of pneumothorax and auto-PEEP.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'IV methylprednisolone + ipratropium + magnesium sulfate', TRUE, 0),
(q_id, 'Immediate intubation', FALSE, 1),
(q_id, 'Oral prednisone and discharge', FALSE, 2),
(q_id, 'IV aminophylline infusion', FALSE, 3);

-- Q22
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'What spirometry pattern is consistent with restrictive lung disease?',
NULL,
'PULMONOLOGY', 'EASY',
'Restrictive lung disease: FVC decreased, FEV1 decreased, FEV1/FVC ratio NORMAL or elevated (>0.70). TLC decreased. Causes: pulmonary fibrosis, sarcoidosis, obesity, pleural effusion, neuromuscular disease. Obstructive lung disease (COPD, asthma): FEV1 decreased, FVC normal or decreased, FEV1/FVC ratio DECREASED (<0.70). TLC normal or increased (air trapping in COPD). Mixed pattern: both FVC and FEV1/FVC decreased.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Decreased FVC, decreased FEV1, normal or elevated FEV1/FVC ratio', TRUE, 0),
(q_id, 'Decreased FEV1, normal FVC, decreased FEV1/FVC ratio', FALSE, 1),
(q_id, 'Normal FVC, normal FEV1, normal FEV1/FVC ratio', FALSE, 2),
(q_id, 'Increased TLC with air trapping', FALSE, 3);

-- Q23
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'A 40-year-old non-smoker presents with progressive dyspnea and basilar emphysema on CT. What diagnosis should be considered?',
NULL,
'PULMONOLOGY', 'HARD',
'Alpha-1 antitrypsin deficiency (AATD) should be suspected in: (1) COPD/emphysema in patients < 45 years, (2) non-smokers or minimal smokers with emphysema, (3) basilar-predominant emphysema (unusual — typical COPD is upper-lobe), (4) concurrent liver disease (cirrhosis from misfolded protein accumulation in hepatocytes). Diagnosis: serum AAT level (low), genotyping (PiZZ most severe). Treatment: bronchodilators, augmentation therapy (IV pooled AAT).')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Alpha-1 antitrypsin deficiency', TRUE, 0),
(q_id, 'Cystic fibrosis', FALSE, 1),
(q_id, 'Hypersensitivity pneumonitis', FALSE, 2),
(q_id, 'Usual interstitial pneumonia (UIP)', FALSE, 3);

-- Q24
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'Light''s criteria are used to classify pleural effusions. An exudate is characterized by which of the following?',
NULL,
'PULMONOLOGY', 'MEDIUM',
'Light''s criteria (any one = exudate): (1) Pleural fluid protein / serum protein > 0.5, (2) Pleural fluid LDH / serum LDH > 0.6, (3) Pleural fluid LDH > 2/3 upper limit of normal for serum LDH. Transudate causes: CHF, cirrhosis, nephrotic syndrome (low protein state). Exudate causes: infection (parapneumonic effusion), malignancy, PE (can be either), TB, pancreatitis, lupus. Bloody effusion: malignancy, trauma, PE, TB.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Pleural fluid protein/serum protein > 0.5', TRUE, 0),
(q_id, 'Pleural fluid protein/serum protein < 0.5', FALSE, 1),
(q_id, 'Pleural fluid LDH < 200 IU/L', FALSE, 2),
(q_id, 'Glucose equal to serum glucose', FALSE, 3);

-- Q25
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'Which of the following is the most common organism responsible for community-acquired pneumonia?',
NULL,
'PULMONOLOGY', 'EASY',
'Streptococcus pneumoniae (pneumococcus) is the most common cause of community-acquired pneumonia (CAP) in all age groups. Classic presentation: sudden onset, high fever, productive cough with rust-colored sputum, pleuritic chest pain. CXR: lobar consolidation. Treatment: amoxicillin-clavulanate or respiratory fluoroquinolone (outpatient), beta-lactam + macrolide or respiratory fluoroquinolone (inpatient). Atypical organisms (Mycoplasma, Chlamydophila, Legionella) require macrolide or doxycycline coverage.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Streptococcus pneumoniae', TRUE, 0),
(q_id, 'Haemophilus influenzae', FALSE, 1),
(q_id, 'Mycoplasma pneumoniae', FALSE, 2),
(q_id, 'Staphylococcus aureus', FALSE, 3);

-- Q26
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'A patient develops sudden onset pleuritic chest pain and dyspnea after a long flight. D-dimer is elevated. What is the most appropriate next step in a hemodynamically stable patient with intermediate pretest probability?',
NULL,
'PULMONOLOGY', 'MEDIUM',
'Pulmonary embolism workup: Calculate clinical probability (Wells or Geneva score). If intermediate probability and D-dimer elevated: CT pulmonary angiography (CTPA) is the gold standard. If hemodynamically unstable: immediate anticoagulation (if no contraindication) or thrombolysis/embolectomy. V/Q scan is used when CTPA is contraindicated (contrast allergy, CKD). Treatment of PE: therapeutic anticoagulation (LMWH bridge to warfarin, or DOAC directly). Massive PE: IV tPA (thrombolysis) or surgical embolectomy.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'CT pulmonary angiography (CTPA)', TRUE, 0),
(q_id, 'Immediate thrombolysis', FALSE, 1),
(q_id, 'Lower extremity Doppler ultrasound only', FALSE, 2),
(q_id, 'Echocardiogram and discharge', FALSE, 3);

-- Q27
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'What is the Berlin definition criteria for ARDS?',
NULL,
'PULMONOLOGY', 'HARD',
'ARDS Berlin Definition (2012): (1) Timing: within 1 week of a known clinical insult, (2) Bilateral opacities on CXR/CT not fully explained by effusions, lobar collapse, or nodules, (3) Respiratory failure not fully explained by cardiac failure or fluid overload (echo if no risk factor), (4) PaO2/FiO2 ratio: Mild 200–300 (PEEP ≥5), Moderate 100–200 (PEEP ≥5), Severe <100 (PEEP ≥5). Treatment: lung-protective ventilation (low tidal volume 6 mL/kg IBW), PEEP titration, prone positioning for severe ARDS, neuromuscular blockade, avoid fluid overload.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Acute onset, bilateral infiltrates, PaO2/FiO2 < 300 with PEEP >= 5, not explained by cardiac failure', TRUE, 0),
(q_id, 'Unilateral infiltrate with PaO2/FiO2 < 200', FALSE, 1),
(q_id, 'Bilateral infiltrates + PCWP > 18 mmHg', FALSE, 2),
(q_id, 'Chronic bilateral infiltrates with FEV1/FVC < 0.70', FALSE, 3);

-- Q28
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'A 35-year-old African American woman presents with bilateral hilar lymphadenopathy, erythema nodosum, and arthralgias. What is the most likely diagnosis?',
NULL,
'PULMONOLOGY', 'MEDIUM',
'Sarcoidosis classic presentation: young African American woman (though can affect any race), bilateral hilar lymphadenopathy (BHL), erythema nodosum + arthralgias (Lofgren syndrome — good prognosis). Other findings: uveitis, parotid enlargement, hypercalcemia (activated macrophages produce 1,25-dihydroxyvitamin D), elevated ACE (not diagnostic but used to monitor disease). Diagnosis: biopsy of accessible tissue showing non-caseating granulomas. Exclude TB and fungal infections. Treatment: oral corticosteroids if symptomatic.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Sarcoidosis', TRUE, 0),
(q_id, 'Tuberculosis', FALSE, 1),
(q_id, 'Lymphoma', FALSE, 2),
(q_id, 'Pulmonary histoplasmosis', FALSE, 3);

-- Q29
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'Obstructive sleep apnea (OSA) is most strongly associated with which complication if left untreated?',
NULL,
'PULMONOLOGY', 'MEDIUM',
'OSA is associated with: systemic hypertension (most common association — nocturnal hypoxemia triggers sympathetic activation), pulmonary hypertension (chronic hypoxic pulmonary vasoconstriction), atrial fibrillation, and right heart failure (cor pulmonale). Also associated with: type 2 diabetes, metabolic syndrome, increased stroke and MI risk, impaired cognitive function. Treatment: CPAP is first-line. Weight loss, position therapy, and surgery (uvulopalatopharyngoplasty) are alternatives.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Systemic hypertension', TRUE, 0),
(q_id, 'Megaloblastic anemia', FALSE, 1),
(q_id, 'Hypothyroidism', FALSE, 2),
(q_id, 'Spontaneous pneumothorax', FALSE, 3);

-- Q30
INSERT INTO questions (id, stem, clinical_vignette, category, difficulty, explanation)
VALUES (gen_random_uuid(),
'A tall, thin 22-year-old male presents with sudden onset right-sided chest pain and dyspnea. CXR shows a 30% right pneumothorax. Which treatment is most appropriate?',
NULL,
'PULMONOLOGY', 'MEDIUM',
'Spontaneous pneumothorax in a young, tall, thin male (classic demographics — rupture of apical blebs): Small (< 20%, no dyspnea): supplemental O2 and observation. Large (> 20%) or symptomatic: needle aspiration (14-16G needle in 2nd intercostal space, midclavicular line) OR chest tube placement (4th-5th ICS, anterior axillary line). Tension pneumothorax (tracheal deviation, absent breath sounds, hypotension, JVD): immediate needle decompression at 2nd ICS MCL — do NOT wait for CXR. This patient has a 30% pneumothorax with dyspnea → chest tube/aspiration indicated.')
RETURNING id INTO q_id;
INSERT INTO answer_options (question_id, text, is_correct, order_index) VALUES
(q_id, 'Chest tube insertion or needle aspiration', TRUE, 0),
(q_id, 'Observation and supplemental O2 only', FALSE, 1),
(q_id, 'Immediate VATS surgery', FALSE, 2),
(q_id, 'Bilateral chest tube insertion prophylactically', FALSE, 3);

END $$;
