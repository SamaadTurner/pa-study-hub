import { describe, it, expect } from 'vitest'
import { TASK_AREAS, getTaskAreaLabel } from '@/utils/taskAreas'

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
