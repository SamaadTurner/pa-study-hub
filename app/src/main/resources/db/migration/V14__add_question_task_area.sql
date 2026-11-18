-- The PANCE blueprint sorts questions two ways: by organ system (the category
-- column) and by the task the question tests, such as making a diagnosis or
-- choosing a drug. This adds the task, tags the 30 starter questions from V3,
-- and records whether an exam was picked to follow the blueprint's task mix.
--
-- V3 gave the questions random ids, so they are found by the start of their text.

ALTER TABLE questions ADD COLUMN task_area VARCHAR(40);

-- Cardiology
UPDATE questions SET task_area = 'PHARMACEUTICAL_THERAPEUTICS' WHERE stem LIKE 'Which of the following best describes the treatment of stable angina%';
UPDATE questions SET task_area = 'MOST_LIKELY_DIAGNOSIS'       WHERE stem LIKE 'A patient presents with sudden onset of severe tearing chest pain%';
UPDATE questions SET task_area = 'PHARMACEUTICAL_THERAPEUTICS' WHERE stem LIKE 'Which diuretic is most appropriate for a patient with CHF%';
UPDATE questions SET task_area = 'DIAGNOSTIC_STUDIES'          WHERE stem LIKE 'Which ECG finding is MOST consistent with right ventricular hypertrophy%';
UPDATE questions SET task_area = 'MOST_LIKELY_DIAGNOSIS'       WHERE stem LIKE 'A 45-year-old woman is found to have a systolic murmur%';
UPDATE questions SET task_area = 'CLINICAL_INTERVENTION'       WHERE stem LIKE 'What is the first-line treatment for hemodynamically unstable ventricular tachycardia%';
UPDATE questions SET task_area = 'PHARMACEUTICAL_THERAPEUTICS' WHERE stem LIKE 'A patient with atrial fibrillation develops rapid ventricular rate%';
UPDATE questions SET task_area = 'BASIC_SCIENCE'               WHERE stem LIKE 'Which of the following is the most common cause of mitral stenosis%';
UPDATE questions SET task_area = 'CLINICAL_INTERVENTION'       WHERE stem LIKE 'A patient is started on lisinopril%';
UPDATE questions SET task_area = 'HISTORY_AND_PHYSICAL'        WHERE stem LIKE 'Which of the following best describes a third heart sound%';

-- Pharmacology
UPDATE questions SET task_area = 'CLINICAL_INTERVENTION'       WHERE stem LIKE 'A patient taking lithium develops polyuria%';
UPDATE questions SET task_area = 'PHARMACEUTICAL_THERAPEUTICS' WHERE stem LIKE 'Which of the following is the mechanism of action of metformin%';
UPDATE questions SET task_area = 'PHARMACEUTICAL_THERAPEUTICS' WHERE stem LIKE 'A 28-year-old woman in her first trimester of pregnancy is found to have a DVT%';
UPDATE questions SET task_area = 'MOST_LIKELY_DIAGNOSIS'       WHERE stem LIKE 'A patient taking fluoxetine for depression is started on tramadol%';
UPDATE questions SET task_area = 'PHARMACEUTICAL_THERAPEUTICS' WHERE stem LIKE 'Which of the following is the reversal agent for heparin%';
UPDATE questions SET task_area = 'BASIC_SCIENCE'               WHERE stem LIKE 'A patient on isoniazid (INH) for latent TB develops peripheral neuropathy%';
UPDATE questions SET task_area = 'PHARMACEUTICAL_THERAPEUTICS' WHERE stem LIKE 'Which adverse effect is unique to clozapine%';
UPDATE questions SET task_area = 'PHARMACEUTICAL_THERAPEUTICS' WHERE stem LIKE 'A patient is started on amiodarone for atrial fibrillation%';
UPDATE questions SET task_area = 'PHARMACEUTICAL_THERAPEUTICS' WHERE stem LIKE 'Which medication should be avoided in a patient with a sulfa allergy%';
UPDATE questions SET task_area = 'PHARMACEUTICAL_THERAPEUTICS' WHERE stem LIKE 'A patient taking warfarin starts a course of fluconazole%';

-- Pulmonology
UPDATE questions SET task_area = 'CLINICAL_INTERVENTION'       WHERE stem LIKE 'A patient with asthma presents to the ED with severe bronchospasm%';
UPDATE questions SET task_area = 'DIAGNOSTIC_STUDIES'          WHERE stem LIKE 'What spirometry pattern is consistent with restrictive lung disease%';
UPDATE questions SET task_area = 'MOST_LIKELY_DIAGNOSIS'       WHERE stem LIKE 'A 40-year-old non-smoker presents with progressive dyspnea%';
UPDATE questions SET task_area = 'DIAGNOSTIC_STUDIES'          WHERE stem LIKE 'Light''s criteria are used to classify pleural effusions%';
UPDATE questions SET task_area = 'BASIC_SCIENCE'               WHERE stem LIKE 'Which of the following is the most common organism responsible for community-acquired pneumonia%';
UPDATE questions SET task_area = 'DIAGNOSTIC_STUDIES'          WHERE stem LIKE 'A patient develops sudden onset pleuritic chest pain and dyspnea after a long flight%';
UPDATE questions SET task_area = 'MOST_LIKELY_DIAGNOSIS'       WHERE stem LIKE 'What is the Berlin definition criteria for ARDS%';
UPDATE questions SET task_area = 'MOST_LIKELY_DIAGNOSIS'       WHERE stem LIKE 'A 35-year-old African American woman presents with bilateral hilar lymphadenopathy%';
UPDATE questions SET task_area = 'HEALTH_MAINTENANCE'          WHERE stem LIKE 'Obstructive sleep apnea (OSA) is most strongly associated with which complication%';
UPDATE questions SET task_area = 'CLINICAL_INTERVENTION'       WHERE stem LIKE 'A tall, thin 22-year-old male presents with sudden onset right-sided chest pain%';

-- Every question has to have a task from now on. If a question above was missed
-- this fails, and the whole migration is rolled back instead of half applied.
ALTER TABLE questions ALTER COLUMN task_area SET NOT NULL;

CREATE INDEX idx_questions_task_area ON questions (task_area) WHERE is_active = TRUE;

ALTER TABLE exam_sessions ADD COLUMN follows_blueprint BOOLEAN NOT NULL DEFAULT FALSE;
