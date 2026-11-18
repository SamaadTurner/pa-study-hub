import type { TaskArea } from '@/types'

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
