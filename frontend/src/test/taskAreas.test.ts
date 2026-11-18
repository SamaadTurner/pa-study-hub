import { describe, it, expect } from 'vitest'
import { TASK_AREAS, getTaskAreaLabel, weakestTaskArea, describeWeakest } from '@/utils/taskAreas'
import type { TaskAreaScore } from '@/types'

describe('TASK_AREAS', () => {
  it('adds up to the whole exam', () => {
    const total = TASK_AREAS.reduce((sum, area) => sum + area.percent, 0)
    expect(total).toBe(100)
  })

  it('lists each area once', () => {
    const values = TASK_AREAS.map(area => area.value)
    expect(new Set(values).size).toBe(values.length)
  })
})

describe('getTaskAreaLabel', () => {
  it('gives the readable name of an area', () => {
    expect(getTaskAreaLabel('PHARMACEUTICAL_THERAPEUTICS')).toBe('Pharmaceutical therapeutics')
  })

  it('shows an unknown value as it is', () => {
    expect(getTaskAreaLabel('SOMETHING_NEW')).toBe('SOMETHING_NEW')
  })
})

describe('weakestTaskArea', () => {
  const score = (taskArea: TaskAreaScore['taskArea'], correct: number, total: number): TaskAreaScore =>
    ({ taskArea, correct, total, percent: Math.round((correct / total) * 1000) / 10 })

  it('picks the area with the lowest share of right answers', () => {
    const scores = [
      score('MOST_LIKELY_DIAGNOSIS', 8, 10),
      score('PHARMACEUTICAL_THERAPEUTICS', 3, 5),
      score('CLINICAL_INTERVENTION', 4, 6),
    ]
    expect(weakestTaskArea(scores)?.taskArea).toBe('PHARMACEUTICAL_THERAPEUTICS')
  })

  it('skips areas with fewer than 5 questions', () => {
    const scores = [score('BASIC_SCIENCE', 0, 2), score('MOST_LIKELY_DIAGNOSIS', 4, 5)]
    expect(weakestTaskArea(scores)?.taskArea).toBe('MOST_LIKELY_DIAGNOSIS')
  })

  it('gives null when every area is perfect or too small', () => {
    expect(weakestTaskArea([score('BASIC_SCIENCE', 5, 5), score('HEALTH_MAINTENANCE', 1, 3)])).toBeNull()
    expect(weakestTaskArea([])).toBeNull()
  })

  it('keeps the area listed first on a tie', () => {
    const scores = [score('HISTORY_AND_PHYSICAL', 3, 5), score('CLINICAL_INTERVENTION', 3, 5)]
    expect(weakestTaskArea(scores)?.taskArea).toBe('HISTORY_AND_PHYSICAL')
  })
})

describe('describeWeakest', () => {
  it('says how often questions of the area are missed', () => {
    const weakest: TaskAreaScore = { taskArea: 'PHARMACEUTICAL_THERAPEUTICS', correct: 3, total: 5, percent: 60 }
    expect(describeWeakest(weakest)).toBe('You miss pharmaceutical therapeutics questions 40% of the time.')
  })
})
