import type { TaskArea, TaskAreaScore } from '@/types'

// The task areas of the 2025 PANCE blueprint, in the order NCCPA lists them,
// with each one's share of the exam. Same numbers as TaskArea.java on the backend.
export const TASK_AREAS: { value: TaskArea; label: string; percent: number }[] = [
  { value: 'HISTORY_AND_PHYSICAL',        label: 'History & physical exam',   percent: 16 },
  { value: 'DIAGNOSTIC_STUDIES',          label: 'Diagnostic & lab studies',  percent: 10 },
  { value: 'MOST_LIKELY_DIAGNOSIS',       label: 'Most likely diagnosis',     percent: 18 },
  { value: 'HEALTH_MAINTENANCE',          label: 'Health maintenance',        percent: 11 },
  { value: 'CLINICAL_INTERVENTION',       label: 'Clinical intervention',     percent: 16 },
  { value: 'PHARMACEUTICAL_THERAPEUTICS', label: 'Pharmaceutical therapeutics', percent: 15 },
  { value: 'BASIC_SCIENCE',               label: 'Basic science',             percent: 8 },
  { value: 'PROFESSIONAL_PRACTICE',       label: 'Professional practice',     percent: 6 },
]

export function getTaskAreaLabel(area: string): string {
  const found = TASK_AREAS.find(a => a.value === area)
  // An unknown value is shown as it came, so a new backend area still shows up
  return found ? found.label : area
}

// With only a couple of questions in an area, one wrong answer looks like a big
// weakness. An area needs at least this many questions before it can be picked.
export const MIN_QUESTIONS_FOR_WEAKEST = 5

// The task area with the lowest share of right answers, among the areas with enough
// questions and at least one miss. Null when no area qualifies. On a tie the area
// listed first (blueprint order) wins.
export function weakestTaskArea(
  scores: TaskAreaScore[],
  minQuestions = MIN_QUESTIONS_FOR_WEAKEST,
): TaskAreaScore | null {
  let weakest: TaskAreaScore | null = null
  for (const score of scores) {
    if (score.total < minQuestions || score.correct === score.total) continue
    if (weakest === null || score.percent < weakest.percent) {
      weakest = score
    }
  }
  return weakest
}

// "You miss pharmaceutical therapeutics questions 40% of the time."
export function describeWeakest(score: TaskAreaScore): string {
  const missed = Math.round(((score.total - score.correct) / score.total) * 100)
  return `You miss ${getTaskAreaLabel(score.taskArea).toLowerCase()} questions ${missed}% of the time.`
}
